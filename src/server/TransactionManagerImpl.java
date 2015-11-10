package server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

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
	private int transactionCounter;
	private String transactionLocation;
	
	public TransactionManagerImpl(ResourceManager rm, LockManager lm, int serverID){
		this.rm = rm;
		this.lm = lm;
		this.activeTransactions = new ActiveTransactionThread(this);
		this.transactionLocation = System.getProperty("user.dir") + File.separator + serverID + DIR_SUFFIX;
		File txnFolder = Paths.get(this.transactionLocation).toFile();
		
		//Make folder if it does not exist
		txnFolder.mkdirs();
		this.transactionCounter = getMostRecentCommitNumber();
		System.out.println("TransactionCounter initialized to " + transactionCounter);
		if (transactionCounter > 0) {
			rm.readOldStateFromFile(COMMITED_STATE_PREFIX + transactionCounter, transactionLocation);
		}
		
		// Clean up leftover files
		File[] filesInTxnFolder =  txnFolder.listFiles();
		for(int i = 0 ; i < filesInTxnFolder.length ; i++){
			if(filesInTxnFolder[i].isFile()) {
				String fileName = filesInTxnFolder[i].getName(); 
				if (fileName.startsWith(COMMITED_STATE_PREFIX)) {
					String noPrefix = filesInTxnFolder[i].getName().substring(COMMITED_STATE_PREFIX.length());
					int txnNumber = Integer.parseInt(noPrefix);
					if(txnNumber != this.transactionCounter) {
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
	}

	private int getMostRecentCommitNumber(){
		int largestTxn = 0;
		File txnFolder = Paths.get(this.transactionLocation).toFile();
		//Make folder if it does not exist
		File[] filesInTxnFolder =  txnFolder.listFiles();
		//Find the most recent commit
		for(int i = 0 ; i < filesInTxnFolder.length ; i++){
			if(filesInTxnFolder[i].isFile()){
				if(filesInTxnFolder[i].getName().startsWith(COMMITED_STATE_PREFIX)){
					String noPrefix = filesInTxnFolder[i].getName().substring(COMMITED_STATE_PREFIX.length());
					int txnNumber = Integer.parseInt(noPrefix);
					if( largestTxn < txnNumber){
						largestTxn = txnNumber;
					}
				}
			}
		}
		return largestTxn;
	}
	
	@Override
	public synchronized int startTransaction() {
		transactionCounter++;
		String fileName = OLD_STATE_PREFIX + transactionCounter;
		rm.writeDataToFile(fileName, transactionLocation);
		activeTransactions.add(transactionCounter);
		return transactionCounter;
	}
	
	@Override
	public int enlist(int id) {
		if (activeTransactions.contains(id)) {
			activeTransactions.signalTransaction(id);
		} else {
			String fileName = OLD_STATE_PREFIX + id;
			rm.writeDataToFile(fileName, transactionLocation);
			activeTransactions.add(id);
		}
		return id;
	}

	@Override
	public synchronized boolean commitTransaction(int transactionID) throws AbortedTransactionException, TransactionNotActiveException{
		activeTransactions.signalTransaction(transactionID);
		//write new commit
		if(!rm.writeDataToFile(COMMITED_STATE_PREFIX + transactionID, transactionLocation)){
			abortTransaction(transactionID);
			throw new AbortedTransactionException();
		};
		
		//Delete old committed transactions
		File txnFolder = Paths.get(this.transactionLocation).toFile();
		File[] filesInTxnFolder =  txnFolder.listFiles();
		for(int i = 0 ; i < filesInTxnFolder.length ; i++){
			if(filesInTxnFolder[i].isFile() && filesInTxnFolder[i].getName().startsWith(COMMITED_STATE_PREFIX)) {
				String noPrefix = filesInTxnFolder[i].getName().substring(COMMITED_STATE_PREFIX.length());
				int txnNumber = Integer.parseInt(noPrefix);
				if(txnNumber < transactionID) {
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
		lm.UnlockAll(transactionID);
		activeTransactions.remove(transactionID);
		return true;
	}
	
	@Override
	public synchronized boolean abortTransaction(int transactionID) throws TransactionNotActiveException {
		activeTransactions.signalTransaction(transactionID);
		int mostRecentCommit = getMostRecentCommitNumber();
		if(transactionID > mostRecentCommit){
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
		lm.UnlockAll(transactionID);
		activeTransactions.remove(transactionID);
		return true;
	}
	
	@Override
	public boolean abortAllActiveTransactions(){
		Set<Integer> activeTransactionsCopy = activeTransactions.getAllActiveTransactions();
		for( int transactionID : activeTransactionsCopy){
			try {
				this.abortTransaction(transactionID);
			} catch (TransactionNotActiveException e) {
				//This should never hit, only aborting transactions in active set
			}
		}
		return true;
	}
	
	@Override
	public boolean addFlight(int transactionID, int flightNumber, int numSeats, int flightPrice) throws AbortedTransactionException, TransactionNotActiveException {
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
	public boolean deleteFlight(int transactionID, int flightNumber) throws AbortedTransactionException, TransactionNotActiveException {
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
	public int queryFlight(int transactionID, int flightNumber) throws AbortedTransactionException, TransactionNotActiveException {
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
	public int queryFlightPrice(int transactionID, int flightNumber) throws AbortedTransactionException, TransactionNotActiveException {
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
	public boolean addCars(int transactionID, String location, int numCars, int carPrice) throws AbortedTransactionException, TransactionNotActiveException {
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
	public boolean deleteCars(int transactionID, String location) throws AbortedTransactionException, TransactionNotActiveException {
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
	public int queryCars(int transactionID, String location) throws AbortedTransactionException, TransactionNotActiveException {
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
	public int queryCarsPrice(int transactionID, String location) throws AbortedTransactionException, TransactionNotActiveException {
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
	public boolean addRooms(int transactionID, String location, int numRooms, int roomPrice) throws AbortedTransactionException, TransactionNotActiveException {
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
	public boolean deleteRooms(int transactionID, String location) throws AbortedTransactionException, TransactionNotActiveException {
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
	public int queryRooms(int transactionID, String location) throws AbortedTransactionException, TransactionNotActiveException {
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
	public int queryRoomsPrice(int transactionID, String location) throws AbortedTransactionException, TransactionNotActiveException {
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
	public int newCustomer(int transactionID) throws AbortedTransactionException, TransactionNotActiveException {
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
	public boolean newCustomerId(int transactionID, int customerNumber) throws AbortedTransactionException, TransactionNotActiveException {
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
	public boolean deleteCustomer(int transactionID, int customerNumber) throws AbortedTransactionException, TransactionNotActiveException {
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
	public String queryCustomerInfo(int transactionID, int customerNumber) throws AbortedTransactionException, TransactionNotActiveException {
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
	public boolean reserveFlight(int transactionID, int customerNumber, int flightNumber) throws AbortedTransactionException, TransactionNotActiveException {
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
	public boolean reserveCar(int transactionID, int customerNumber, String location) throws AbortedTransactionException, TransactionNotActiveException {
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
	public boolean reserveRoom(int transactionID, int customerNumber, String location) throws AbortedTransactionException, TransactionNotActiveException {
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
