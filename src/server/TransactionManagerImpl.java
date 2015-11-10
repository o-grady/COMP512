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
	public ResourceManager rm;
	//lock data using following scheme: (flight|car|room|customer) + ID
	private LockManager lm;
    private ActiveTransactionThread activeTransactions;
	private int transactionCounter;
	private String transactionLocation;
		
	public static void main(String args[]){
		
		TransactionManager tm = new TransactionManagerImpl(new ResourceManagerImpl(), new LockManager(), 1, true);
		int txnID1 = tm.startTransaction();
		int txnID2 = tm.startTransaction();
		try {
			System.out.println("Query Cars txn1:" + tm.queryCars(1, "1", txnID1));
			System.out.println("Query Cars txn2:" + tm.queryCars(1, "2", txnID2));
			System.out.println("Add Cars txn1:" + tm.addCars(1, "1", 1, 1, txnID1));
			System.out.println("Add Cars txn2:" + tm.addCars(1, "2", 1, 1, txnID2));
			System.out.println("Commit txn1:" + tm.commitTransaction(txnID1));
			System.out.println("Commit txn2:" + tm.commitTransaction(txnID2));
			int txnID3 = tm.startTransaction();
			int txnID4 = tm.startTransaction();
			System.out.println("New Customer txn3:" + tm.newCustomer(1, txnID3));
			System.out.println("New Customer txn4:" + tm.newCustomer(1, txnID4));
			System.out.println("Commit txn3:" + tm.commitTransaction(txnID3));
			System.out.println("Commit txn4:" + tm.commitTransaction(txnID4));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public TransactionManagerImpl(ResourceManager rm, LockManager lm, int serverID, boolean TTLHere){
		this.rm = rm;
		this.lm = lm;
		this.activeTransactions = new ActiveTransactionThread(this);
		this.transactionLocation = System.getProperty("user.dir") + File.separator + serverID +  "-transactions";
		File txnFolder = Paths.get(this.transactionLocation).toFile();
		//Make folder if it does not exist
		txnFolder.mkdirs();
		this.transactionCounter = getMostRecentCommitNumber();
		System.out.println("TransactionCounter initialized to " + transactionCounter);
		if (transactionCounter > 0) {
			rm.readOldStateFromFile("commitedTxn"+transactionCounter, transactionLocation);
		}
		if (TTLHere) {
			this.activeTransactions.start();
		}
	}

	private int getMostRecentCommitNumber(){
		int largestTxn = 0;
		File txnFolder = Paths.get(this.transactionLocation).toFile();
		//Make folder if it does not exist
		File[] filesInTxnFolder =  txnFolder.listFiles();
		//Find the most recent commit
		for(int i = 0 ; i < filesInTxnFolder.length ; i++){
			if(filesInTxnFolder[i].isFile()){
				if(filesInTxnFolder[i].getName().startsWith("commitedTxn")){
					String noPrefix = filesInTxnFolder[i].getName().substring("commitedTxn".length());
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
		String fileName = "oldState_txn" + transactionCounter;
		rm.writeDataToFile(fileName, transactionLocation);
		activeTransactions.add(transactionCounter);
		return transactionCounter;
	}
	
	@Override
	public int enlist(int id) {
		if (activeTransactions.contains(id)) {
			activeTransactions.signalTransaction(id);
		} else {
			String fileName = "oldState_txn" + id;
			rm.writeDataToFile(fileName, transactionLocation);
			activeTransactions.add(id);
		}
		return id;
	}

	@Override
	public synchronized boolean commitTransaction(int transactionID) throws AbortedTransactionException, TransactionNotActiveException{
		activeTransactions.signalTransaction(transactionID);
		//write new commit
		if(!rm.writeDataToFile("commitedTxn"+transactionID, transactionLocation)){
			abortTransaction(transactionID);
			throw new AbortedTransactionException();
		};
		//Delete old commited transaction
		Path p = Paths.get(transactionLocation, "commitedTxn"+(transactionID - 1));
		try {
			Files.deleteIfExists(p);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		//Delete old state
		p = Paths.get(transactionLocation, "oldState_txn"+transactionID);
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
			rm.readOldStateFromFile("oldState_txn"+transactionID, transactionLocation);
		}else{
			rm.readOldStateFromFile("commitedTxn", transactionLocation);
		}
		Path p = Paths.get(transactionLocation, "oldState_txn"+transactionID);
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
	public boolean addFlight(int id, int flightNumber, int numSeats, int flightPrice, int transactionID) throws AbortedTransactionException, TransactionNotActiveException {
		activeTransactions.signalTransaction(transactionID);
		try {
			lm.Lock(transactionID, Flight.getKey(flightNumber), LockManager.WRITE);
			return rm.addFlight(id, flightNumber, numSeats, flightPrice);
		} catch (DeadlockException e) {
			System.out.println("addFlight failed, could not aquire write lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}

	@Override
	public boolean deleteFlight(int id, int flightNumber, int transactionID) throws AbortedTransactionException, TransactionNotActiveException {
		activeTransactions.signalTransaction(transactionID);
		try {
			lm.Lock(transactionID, Flight.getKey(flightNumber), LockManager.WRITE);
			return rm.deleteFlight(id, flightNumber);
		} catch (DeadlockException e) {
			System.out.println("deleteFlight failed, could not aquire write lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}

	@Override
	public int queryFlight(int id, int flightNumber, int transactionID) throws AbortedTransactionException, TransactionNotActiveException {
		activeTransactions.signalTransaction(transactionID);
		try {
			lm.Lock(transactionID, Flight.getKey(flightNumber), LockManager.READ);
			return rm.queryFlight(id, flightNumber);
		} catch (DeadlockException e) {
			System.out.println("queryFlight failed, could not aquire read lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}

	@Override
	public int queryFlightPrice(int id, int flightNumber, int transactionID) throws AbortedTransactionException, TransactionNotActiveException {
		activeTransactions.signalTransaction(transactionID);
		try {
			lm.Lock(transactionID, Flight.getKey(flightNumber), LockManager.READ);
			return rm.queryFlightPrice(id, flightNumber);
		} catch (DeadlockException e) {
			System.out.println("queryFlightPrice failed, could not aquire read lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}

	@Override
	public boolean addCars(int id, String location, int numCars, int carPrice, int transactionID) throws AbortedTransactionException, TransactionNotActiveException {
		activeTransactions.signalTransaction(transactionID);
		try {
			lm.Lock(transactionID, Car.getKey(location), LockManager.WRITE);
			return rm.addCars(id, location, numCars, carPrice);
		} catch (DeadlockException e) {
			System.out.println("addCars failed, could not aquire write lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}

	@Override
	public boolean deleteCars(int id, String location, int transactionID) throws AbortedTransactionException, TransactionNotActiveException {
		activeTransactions.signalTransaction(transactionID);
		try {
			lm.Lock(transactionID, Car.getKey(location), LockManager.WRITE);
			return rm.deleteCars(id, location);
		} catch (DeadlockException e) {
			System.out.println("deleteCars failed, could not aquire write lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}

	@Override
	public int queryCars(int id, String location, int transactionID) throws AbortedTransactionException, TransactionNotActiveException {
		activeTransactions.signalTransaction(transactionID);
		try {
			lm.Lock(transactionID, Car.getKey(location), LockManager.READ);
			return rm.queryCars(id, location);
		} catch (DeadlockException e) {
			System.out.println("queryCars failed, could not aquire read lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}

	@Override
	public int queryCarsPrice(int id, String location, int transactionID) throws AbortedTransactionException, TransactionNotActiveException {
		activeTransactions.signalTransaction(transactionID);
		try {
			lm.Lock(transactionID, Car.getKey(location), LockManager.READ);
			return rm.queryCarsPrice(id, location);
		} catch (DeadlockException e) {
			System.out.println("queryCarsPrice failed, could not aquire read lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}

	@Override
	public boolean addRooms(int id, String location, int numRooms, int roomPrice, int transactionID) throws AbortedTransactionException, TransactionNotActiveException {
		activeTransactions.signalTransaction(transactionID);
		try {
			lm.Lock(transactionID, Room.getKey(location), LockManager.WRITE);
			return rm.addRooms(id, location, numRooms, roomPrice);
		} catch (DeadlockException e) {
			System.out.println("addRooms failed, could not aquire write lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}

	@Override
	public boolean deleteRooms(int id, String location, int transactionID) throws AbortedTransactionException, TransactionNotActiveException {
		activeTransactions.signalTransaction(transactionID);
		try {
			lm.Lock(transactionID, Room.getKey(location), LockManager.WRITE);
			return rm.deleteRooms(id, location);
		} catch (DeadlockException e) {
			System.out.println("deleteRooms failed, could not aquire write lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}

	@Override
	public int queryRooms(int id, String location, int transactionID) throws AbortedTransactionException, TransactionNotActiveException {
		activeTransactions.signalTransaction(transactionID);
		try {
			lm.Lock(transactionID, Room.getKey(location), LockManager.READ);
			return rm.queryRooms(id, location);
		} catch (DeadlockException e) {
			System.out.println("queryRooms failed, could not aquire write lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}

	@Override
	public int queryRoomsPrice(int id, String location, int transactionID) throws AbortedTransactionException, TransactionNotActiveException {
		activeTransactions.signalTransaction(transactionID);
		try {
			lm.Lock(transactionID, Room.getKey(location), LockManager.READ);
			return rm.queryRoomsPrice(id, location);
		} catch (DeadlockException e) {
			System.out.println("queryRoomPrice failed, could not aquire write lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}

	@Override
	public int newCustomer(int id, int transactionID) throws AbortedTransactionException, TransactionNotActiveException {
		activeTransactions.signalTransaction(transactionID);
		try {
			//No RMHashtable key associated with new customer. Instead lock string "newcustomer"
			lm.Lock(transactionID, "newcustomer", LockManager.WRITE);
			return rm.newCustomer(id);
		} catch (DeadlockException e) {
			System.out.println("newCustomer failed, could not aquire write lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}

	@Override
	public boolean newCustomerId(int id, int customerNumber, int transactionID) throws AbortedTransactionException, TransactionNotActiveException {
		activeTransactions.signalTransaction(transactionID);
		try {
			//No RMHashtable key associated with new customer. Instead lock string "newcustomer"
			lm.Lock(transactionID, "newcustomer", LockManager.WRITE);
			return rm.newCustomerId(id, customerNumber);
		} catch (DeadlockException e) {
			System.out.println("newCustomerID failed, could not aquire write lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}

	@Override
	public boolean deleteCustomer(int id, int customerNumber, int transactionID) throws AbortedTransactionException, TransactionNotActiveException {
		activeTransactions.signalTransaction(transactionID);
		try {
			lm.Lock(transactionID, Customer.getKey(customerNumber), LockManager.WRITE);
			return rm.deleteCustomer(id, customerNumber);
		} catch (DeadlockException e) {
			System.out.println("deleteCustomer failed, could not aquire write lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}

	@Override
	public String queryCustomerInfo(int id, int customerNumber, int transactionID) throws AbortedTransactionException, TransactionNotActiveException {
		activeTransactions.signalTransaction(transactionID);
		try {
			lm.Lock(transactionID, Customer.getKey(customerNumber), LockManager.READ);
			return rm.queryCustomerInfo(id, customerNumber);
		} catch (DeadlockException e) {
			System.out.println("queryCustomerInfo failed, could not aquire read lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}

	@Override
	public boolean reserveFlight(int id, int customerNumber, int flightNumber, int transactionID) throws AbortedTransactionException, TransactionNotActiveException {
		activeTransactions.signalTransaction(transactionID);
		try {
			lm.Lock(transactionID, Flight.getKey(flightNumber), LockManager.WRITE);
			lm.Lock(transactionID, Customer.getKey(customerNumber), LockManager.WRITE);
			return rm.reserveFlight(id, customerNumber, flightNumber);
		} catch (DeadlockException e) {
			System.out.println("reserveFlight failed, could not aquire write lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}

	@Override
	public boolean reserveCar(int id, int customerNumber, String location, int transactionID) throws AbortedTransactionException, TransactionNotActiveException {
		activeTransactions.signalTransaction(transactionID);
		try {
			lm.Lock(transactionID, Car.getKey(location), LockManager.WRITE);
			lm.Lock(transactionID, Customer.getKey(customerNumber), LockManager.WRITE);
			return rm.reserveCar(id, customerNumber, location);
		} catch (DeadlockException e) {
			System.out.println("reserveCar failed, could not aquire write lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}

	@Override
	public boolean reserveRoom(int id, int customerNumber, String location, int transactionID) throws AbortedTransactionException, TransactionNotActiveException {
		activeTransactions.signalTransaction(transactionID);
		try {
			lm.Lock(transactionID, Room.getKey(location), LockManager.WRITE);
			lm.Lock(transactionID, Customer.getKey(customerNumber), LockManager.WRITE);
			return rm.reserveRoom(id, customerNumber, location);
		} catch (DeadlockException e) {
			System.out.println("reserveRoom failed, could not aquire write lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			throw new AbortedTransactionException();
		}
	}
}
