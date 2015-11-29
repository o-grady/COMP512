package server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import shared.CommitLogger;
import shared.CommitLoggerImpl;
import shared.LogType;
import shared.LockManager.DeadlockException;
import shared.LockManager.LockManager;

public class TransactionManagerImpl implements TransactionManager {
	private static final String DIR_SUFFIX = "-transactions";
	private static final String COMMITED_STATE_PREFIX = "commitedTxn";
	private static final String OLD_STATE_PREFIX = "oldState_txn";
	public static final int RESERVED_ID = 4567654;
	
	public ResourceManager rm;
	//lock data using following scheme: (flight|car|room|customer) + ID
	private LockManager lm;
    private ActiveTransactionThread activeTransactions;
	private String transactionLocation;
	private Set<Integer> transactionsIn2PC;
	private Set<Integer> startupVoteResponsesNeeded;
	private CommitLogger logger;
	public TransactionManagerImpl(ResourceManager rm, LockManager lm, int serverID){
		this.rm = rm;
		this.lm = lm;
		this.activeTransactions = new ActiveTransactionThread(this);
		this.transactionLocation = System.getProperty("user.dir") + File.separator + serverID + DIR_SUFFIX;
		this.transactionsIn2PC = new HashSet<Integer>();
		this.startupVoteResponsesNeeded = new HashSet<Integer>();
		File txnFolder = Paths.get(this.transactionLocation).toFile();
		
		//Make folder if it does not exist
		txnFolder.mkdirs();
		logger = new CommitLoggerImpl(this.transactionLocation + File.separator + "log.txt");
		int mostRecentCommit = logger.mostRecentlyCommittedTransaction();
		if (mostRecentCommit > 0) {
			System.out.println("Setting contents to commit number " + mostRecentCommit);
			rm.readOldStateFromFile(COMMITED_STATE_PREFIX + mostRecentCommit, transactionLocation);
		}
		
		// Clean up leftover files
		File[] filesInTxnFolder =  txnFolder.listFiles();
		for(int i = 0 ; i < filesInTxnFolder.length ; i++){
			if(filesInTxnFolder[i].isFile()) {
				String fileName = filesInTxnFolder[i].getName(); 
				if (fileName.startsWith(COMMITED_STATE_PREFIX)) {
					String noPrefix = filesInTxnFolder[i].getName().substring(COMMITED_STATE_PREFIX.length());
					int txnNumber = Integer.parseInt(noPrefix);
					if(txnNumber != mostRecentCommit) {
						try {
							Files.deleteIfExists(filesInTxnFolder[i].toPath());
						} catch (IOException e1) {
							System.out.println("WARN: Could not delete " + filesInTxnFolder[i].toPath());
						}
					}
				}
				if (fileName.startsWith(OLD_STATE_PREFIX)) {
					try {
						Files.deleteIfExists(filesInTxnFolder[i].toPath());
					} catch (IOException e1) {
						System.out.println("WARN: Could not delete " + filesInTxnFolder[i].toPath());
					}
				}
			}
		}
		this.activeTransactions.start();
		int largestTxn = logger.largestTransactionInLog();
		for(int i = 0 ; i <= largestTxn ; i++ ){
			if(logger.hasLog(LogType.STARTED, i)){
				if(logger.hasLog(LogType.YESVOTESENT, i)){
					if(!logger.hasLog(LogType.COMMITTED, i) && !logger.hasLog(LogType.ABORTED, i)){
						System.out.println("Transaction " + i + " added to startupVoteResponsesNeeded");
						activeTransactions.add(i);
						activeTransactions.hangTransaction(i);
						transactionsIn2PC.add(i);
						startupVoteResponsesNeeded.add(i);
					}
				}else{
					//Add abort log, dont need to actually abort as state is not altered,
					//Shouldn't need to notify middleware, next time it asks about txn it will get an Aborted response. 
					logger.log(LogType.ABORTED, i);
				}
			}
		}
	}
	@Override
	public Set<Integer> getStartupVoteResponsesNeeded(){
		return this.startupVoteResponsesNeeded;
	}
	private void exceptionIfTransactionIsBlockingOrInactive(int transactionID) throws TransactionBlockingException, TransactionNotActiveException {
		if(this.transactionsIn2PC.contains(transactionID)){
			throw new TransactionBlockingException();
		}
		if(!this.activeTransactions.contains(transactionID)){
			throw new TransactionNotActiveException();
		}
	}

	
	@Override
	public synchronized int enlist(int id) throws AbortedTransactionException {
		if (activeTransactions.contains(id)) {
			activeTransactions.signalTransaction(id);
		} else if(logger.hasLog(LogType.ABORTED, id)) {
			throw new AbortedTransactionException();
			//dont enlist a previously aborted transaction
		} else {
			String fileName = OLD_STATE_PREFIX + id;
			rm.writeDataToFile(fileName, transactionLocation);
			activeTransactions.add(id);
			logger.log(LogType.STARTED, id);
		}
		return id;
	}

	@Override
	public synchronized boolean prepare(int transactionID) throws TransactionBlockingException, TransactionNotActiveException{
		//Not sure that these exceptions should be thrown, because TMRequestHandler will catch and send String resp, want
		//to send a boolean even if transaction is dead.
		//TODO: need a log of 2PCStarted,txnid and 2PCVoteMade,txnid so that duplicate prepare, commit and aborts are ignored.
		System.out.println("Starting prepare");
		if(!activeTransactions.contains(transactionID)){
			System.out.println("Transaction not active");
			throw new TransactionNotActiveException();
		}
		if(this.transactionsIn2PC.contains(transactionID)){
			//Already has been prepared
			throw new TransactionBlockingException();
		}
		activeTransactions.signalTransaction(transactionID);
		if (lm.isTransactionWaiting(transactionID)){
			System.out.println("Transaction waiting");
			try {
				this.abortTransaction(transactionID);
				System.out.println("Aborted Transaction");
			} catch (TransactionNotActiveException e) {
				//TODO: don't think anything needs to be done here
			}
			return false;
		}
		activeTransactions.hangTransaction(transactionID);
		this.transactionsIn2PC.add(transactionID);
		logger.log(LogType.YESVOTESENT, transactionID);
		return true; 
	}
	@Override
	public synchronized boolean twoPhaseCommitTransaction(int transactionID) throws NotWaitingForVoteResultException{
		//do not commit if transaction is not waiting for vote result
		if(!this.transactionsIn2PC.contains(transactionID)){
			throw new NotWaitingForVoteResultException();
		}
		logger.log(LogType.COMMITTED, transactionID);
		//write new commit
		if(!rm.writeDataToFile(COMMITED_STATE_PREFIX + transactionID, transactionLocation)){
			//TODO: not sure if this should happen, after getting a YES vote commit needs to be written, maybe some other type of error other than abort?
			//abortTransaction(transactionID);
			//throw new AbortedTransactionException();
			System.out.println("ERROR: could not write commit state to stable storage");
		};
		
		//Delete old committed transactions
		File txnFolder = Paths.get(this.transactionLocation).toFile();
		File[] filesInTxnFolder =  txnFolder.listFiles();
		for(int i = 0 ; i < filesInTxnFolder.length ; i++){
			if(filesInTxnFolder[i].isFile() && filesInTxnFolder[i].getName().startsWith(COMMITED_STATE_PREFIX)) {
				String noPrefix = filesInTxnFolder[i].getName().substring(COMMITED_STATE_PREFIX.length());
				int txnNumber = Integer.parseInt(noPrefix);
				if(txnNumber != transactionID) {
					try {
						Files.deleteIfExists(filesInTxnFolder[i].toPath());
					} catch (IOException e1) {
						System.out.println("WARN: Could not delete " + filesInTxnFolder[i].toPath());
					}
				}
			}
		}

		//Delete old state
		Path p = Paths.get(transactionLocation, OLD_STATE_PREFIX + transactionID);
		try {
			Files.delete(p);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Calling UnlockAll on " + transactionID);
		activeTransactions.unhangTransaction(transactionID);
		transactionsIn2PC.remove(transactionID);
		activeTransactions.remove(transactionID);
		lm.UnlockAll(transactionID);
		return true;
	}
	
	@Override
	public synchronized boolean twoPhaseAbortTransaction(int transactionID) throws NotWaitingForVoteResultException{
		//do not abort if transaction is not waiting for vote result
		if(!this.transactionsIn2PC.contains(transactionID)){
			throw new NotWaitingForVoteResultException();
		}
		logger.log(LogType.ABORTED, transactionID);
		int mostRecentCommit = logger.mostRecentCommitSinceTransactionStart(transactionID);
		if(mostRecentCommit ==  -1){
			rm.readOldStateFromFile(OLD_STATE_PREFIX + transactionID, transactionLocation);
		}else{
			// TODO: confirm with Grady
			rm.readOldStateFromFile(COMMITED_STATE_PREFIX + mostRecentCommit, transactionLocation);
		}
		Path p = Paths.get(transactionLocation, OLD_STATE_PREFIX + transactionID);
		//Remove Old State
		try {
			Files.delete(p);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Calling UnlockAll on " + transactionID);
		activeTransactions.remove(transactionID);
		lm.UnlockAll(transactionID);
		return true;
	}
	@Override
	public synchronized boolean abortTransaction(int transactionID) throws TransactionNotActiveException, TransactionBlockingException {
		exceptionIfTransactionIsBlockingOrInactive(transactionID);
		logger.log(LogType.ABORTED, transactionID);
		int mostRecentCommit = logger.mostRecentCommitSinceTransactionStart(transactionID);
		if(mostRecentCommit ==  -1){
			rm.readOldStateFromFile(OLD_STATE_PREFIX + transactionID, transactionLocation);
		}else{
			// TODO: confirm with Grady
			rm.readOldStateFromFile(COMMITED_STATE_PREFIX + mostRecentCommit, transactionLocation);
		}
		Path p = Paths.get(transactionLocation, OLD_STATE_PREFIX + transactionID);
		//Remove Old State
		try {
			Files.delete(p);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Calling UnlockAll on " + transactionID);
		activeTransactions.remove(transactionID);
		lm.UnlockAll(transactionID);
		return true;
	}
	
	@Override
	public boolean abortAllActiveTransactions() {
		Set<Integer> activeTransactionsCopy = activeTransactions.getAllActiveTransactions();
		for( int transactionID : activeTransactionsCopy){
			try {
				this.abortTransaction(transactionID);
			} catch (TransactionNotActiveException e) {
				//This should never hit, only aborting transactions in active set
			} catch (TransactionBlockingException e) {
				//Cant abort transactions waiting for vote result
			}
		}
		return true;
	}
	
	@Override
	public boolean addFlight(int transactionID, int flightNumber, int numSeats, int flightPrice) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException {
		exceptionIfTransactionIsBlockingOrInactive(transactionID);
		activeTransactions.signalTransaction(transactionID);
		try {
			lm.Lock(transactionID, Flight.getKey(flightNumber), LockManager.WRITE);
			return rm.addFlight(transactionID, flightNumber, numSeats, flightPrice);
		} catch (DeadlockException e) {
			System.out.println("addFlight failed, could not aquire write lock for id "+ transactionID + " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}

	@Override
	public boolean deleteFlight(int transactionID, int flightNumber) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException {
		exceptionIfTransactionIsBlockingOrInactive(transactionID);
		activeTransactions.signalTransaction(transactionID);
		try {
			lm.Lock(transactionID, Flight.getKey(flightNumber), LockManager.WRITE);
			return rm.deleteFlight(transactionID, flightNumber);
		} catch (DeadlockException e) {
			System.out.println("deleteFlight failed, could not aquire write lock for id "+ transactionID + " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}

	@Override
	public int queryFlight(int transactionID, int flightNumber) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException {
		exceptionIfTransactionIsBlockingOrInactive(transactionID);
		activeTransactions.signalTransaction(transactionID);
		try {
			lm.Lock(transactionID, Flight.getKey(flightNumber), LockManager.READ);
			return rm.queryFlight(transactionID, flightNumber);
		} catch (DeadlockException e) {
			System.out.println("queryFlight failed, could not aquire read lock for id "+ transactionID + " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}

	@Override
	public int queryFlightPrice(int transactionID, int flightNumber) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException {
		exceptionIfTransactionIsBlockingOrInactive(transactionID);
		activeTransactions.signalTransaction(transactionID);
		try {
			lm.Lock(transactionID, Flight.getKey(flightNumber), LockManager.READ);
			return rm.queryFlightPrice(transactionID, flightNumber);
		} catch (DeadlockException e) {
			System.out.println("queryFlightPrice failed, could not aquire read lock for id "+ transactionID + " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}

	@Override
	public boolean addCars(int transactionID, String location, int numCars, int carPrice) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException {
		exceptionIfTransactionIsBlockingOrInactive(transactionID);
		activeTransactions.signalTransaction(transactionID);
		try {
			lm.Lock(transactionID, Car.getKey(location), LockManager.WRITE);
			return rm.addCars(transactionID, location, numCars, carPrice);
		} catch (DeadlockException e) {
			System.out.println("addCars failed, could not aquire write lock for id "+ transactionID + " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}

	@Override
	public boolean deleteCars(int transactionID, String location) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException {
		exceptionIfTransactionIsBlockingOrInactive(transactionID);
		activeTransactions.signalTransaction(transactionID);
		try {
			lm.Lock(transactionID, Car.getKey(location), LockManager.WRITE);
			return rm.deleteCars(transactionID, location);
		} catch (DeadlockException e) {
			System.out.println("deleteCars failed, could not aquire write lock for id "+ transactionID + " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}

	@Override
	public int queryCars(int transactionID, String location) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException {
		exceptionIfTransactionIsBlockingOrInactive(transactionID);
		activeTransactions.signalTransaction(transactionID);
		try {
			lm.Lock(transactionID, Car.getKey(location), LockManager.READ);
			return rm.queryCars(transactionID, location);
		} catch (DeadlockException e) {
			System.out.println("queryCars failed, could not aquire read lock for id "+ transactionID + " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}

	@Override
	public int queryCarsPrice(int transactionID, String location) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException {
		exceptionIfTransactionIsBlockingOrInactive(transactionID);
		activeTransactions.signalTransaction(transactionID);
		try {
			lm.Lock(transactionID, Car.getKey(location), LockManager.READ);
			return rm.queryCarsPrice(transactionID, location);
		} catch (DeadlockException e) {
			System.out.println("queryCarsPrice failed, could not aquire read lock for id "+ transactionID + " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}

	@Override
	public boolean addRooms(int transactionID, String location, int numRooms, int roomPrice) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException {
		exceptionIfTransactionIsBlockingOrInactive(transactionID);
		activeTransactions.signalTransaction(transactionID);
		try {
			lm.Lock(transactionID, Room.getKey(location), LockManager.WRITE);
			return rm.addRooms(transactionID, location, numRooms, roomPrice);
		} catch (DeadlockException e) {
			System.out.println("addRooms failed, could not aquire write lock for id "+ transactionID + " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}

	@Override
	public boolean deleteRooms(int transactionID, String location) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException {
		exceptionIfTransactionIsBlockingOrInactive(transactionID);
		activeTransactions.signalTransaction(transactionID);
		try {
			lm.Lock(transactionID, Room.getKey(location), LockManager.WRITE);
			return rm.deleteRooms(transactionID, location);
		} catch (DeadlockException e) {
			System.out.println("deleteRooms failed, could not aquire write lock for id "+ transactionID + " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}

	@Override
	public int queryRooms(int transactionID, String location) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException {
		exceptionIfTransactionIsBlockingOrInactive(transactionID);
		activeTransactions.signalTransaction(transactionID);
		try {
			lm.Lock(transactionID, Room.getKey(location), LockManager.READ);
			return rm.queryRooms(transactionID, location);
		} catch (DeadlockException e) {
			System.out.println("queryRooms failed, could not aquire write lock for id "+ transactionID + " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}

	@Override
	public int queryRoomsPrice(int transactionID, String location) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException {
		exceptionIfTransactionIsBlockingOrInactive(transactionID);
		activeTransactions.signalTransaction(transactionID);
		try {
			lm.Lock(transactionID, Room.getKey(location), LockManager.READ);
			return rm.queryRoomsPrice(transactionID, location);
		} catch (DeadlockException e) {
			System.out.println("queryRoomPrice failed, could not aquire write lock for id "+ transactionID + " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}

	@Override
	public int newCustomer(int transactionID) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException {
		exceptionIfTransactionIsBlockingOrInactive(transactionID);
		activeTransactions.signalTransaction(transactionID);
		try {
			//No RMHashtable key associated with new customer. Instead lock string "newcustomer"
			lm.Lock(transactionID, "newcustomer", LockManager.WRITE);
			return rm.newCustomer(transactionID);
		} catch (DeadlockException e) {
			System.out.println("newCustomer failed, could not aquire write lock for id "+ transactionID + " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}

	@Override
	public boolean newCustomerId(int transactionID, int customerNumber) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException {
		exceptionIfTransactionIsBlockingOrInactive(transactionID);
		activeTransactions.signalTransaction(transactionID);
		try {
			//No RMHashtable key associated with new customer. Instead lock string "newcustomer"
			lm.Lock(transactionID, "newcustomer", LockManager.WRITE);
			return rm.newCustomerId(transactionID, customerNumber);
		} catch (DeadlockException e) {
			System.out.println("newCustomerID failed, could not aquire write lock for id "+ transactionID + " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}

	@Override
	public boolean deleteCustomer(int transactionID, int customerNumber) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException {
		exceptionIfTransactionIsBlockingOrInactive(transactionID);
		activeTransactions.signalTransaction(transactionID);
		try {
			lm.Lock(transactionID, Customer.getKey(customerNumber), LockManager.WRITE);
			return rm.deleteCustomer(transactionID, customerNumber);
		} catch (DeadlockException e) {
			System.out.println("deleteCustomer failed, could not aquire write lock for id "+ transactionID + " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}

	@Override
	public String queryCustomerInfo(int transactionID, int customerNumber) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException {
		exceptionIfTransactionIsBlockingOrInactive(transactionID);
		activeTransactions.signalTransaction(transactionID);
		try {
			lm.Lock(transactionID, Customer.getKey(customerNumber), LockManager.READ);
			return rm.queryCustomerInfo(transactionID, customerNumber);
		} catch (DeadlockException e) {
			System.out.println("queryCustomerInfo failed, could not aquire read lock for id "+ transactionID + " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}

	@Override
	public boolean reserveFlight(int transactionID, int customerNumber, int flightNumber) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException {
		exceptionIfTransactionIsBlockingOrInactive(transactionID);
		activeTransactions.signalTransaction(transactionID);
		try {
			lm.Lock(transactionID, Flight.getKey(flightNumber), LockManager.WRITE);
			lm.Lock(transactionID, Customer.getKey(customerNumber), LockManager.WRITE);
			return rm.reserveFlight(transactionID, customerNumber, flightNumber);
		} catch (DeadlockException e) {
			System.out.println("reserveFlight failed, could not aquire write lock for id "+ transactionID + " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}

	@Override
	public boolean reserveCar(int transactionID, int customerNumber, String location) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException {
		exceptionIfTransactionIsBlockingOrInactive(transactionID);
		activeTransactions.signalTransaction(transactionID);
		try {
			lm.Lock(transactionID, Car.getKey(location), LockManager.WRITE);
			lm.Lock(transactionID, Customer.getKey(customerNumber), LockManager.WRITE);
			return rm.reserveCar(transactionID, customerNumber, location);
		} catch (DeadlockException e) {
			System.out.println("reserveCar failed, could not aquire write lock for id "+ transactionID + " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}

	@Override
	public boolean reserveRoom(int transactionID, int customerNumber, String location) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException {
		exceptionIfTransactionIsBlockingOrInactive(transactionID);
		activeTransactions.signalTransaction(transactionID);
		try {
			lm.Lock(transactionID, Room.getKey(location), LockManager.WRITE);

			lm.Lock(transactionID, Customer.getKey(customerNumber), LockManager.WRITE);
			return rm.reserveRoom(transactionID, customerNumber, location);
		} catch (DeadlockException e) {
			System.out.println("reserveRoom failed, could not aquire write lock for id "+ transactionID + " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}
}
