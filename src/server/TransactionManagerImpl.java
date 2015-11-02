package server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import shared.LockManager.DeadlockException;
import shared.LockManager.LockManager;

public class TransactionManagerImpl implements TransactionManager {
	public ResourceManager rm;
	//lock data using following scheme: (flight|car|room|customer) + ID
	LockManager lm;
    Set<Integer> activeTransactions;
	int transactionCounter;
	String transactionLocation;
	
	public static void main(String args[]){
		TransactionManager tm = new TransactionManagerImpl(new ResourceManagerImpl(), new LockManager(), 1);
		int txnID1 = tm.startTransaction();
		int txnID2 = tm.startTransaction();
		System.out.println("Query Cars txn1:" + tm.queryCars(1, "1", txnID1));
		System.out.println("Query Cars txn2:" + tm.queryCars(1, "2", txnID2));
		System.out.println("Add Cars txn1:" + tm.addCars(1, "1", 1, 1, txnID1));
		System.out.println("Add Cars txn2:" + tm.addCars(1, "2", 1, 1, txnID2));
		System.out.println("Commit txn1:" + tm.commitTransaction(txnID1));
		System.out.println("Commit txn2:" + tm.commitTransaction(txnID2));
	}
	public TransactionManagerImpl(ResourceManager rm, LockManager lm, int serverID){
		this.rm = rm;
		this.lm = lm;
		this.activeTransactions = new HashSet<Integer>();
		this.transactionCounter = 0;
		this.transactionLocation = System.getProperty("user.dir") + File.separator + serverID +  "-transactions";
		Paths.get(this.transactionLocation).toFile().mkdirs();
		//TODO: Read the most recent commit in folder (if it exists) and use that as the RM_Hashtable
	}
	
	@Override
	public int startTransaction() {
		transactionCounter++;
		String fileName = "" + transactionCounter;
		rm.writeDataToFile(fileName, transactionLocation);
		activeTransactions.add(transactionCounter);
		return transactionCounter;
	}

	@Override
	public boolean commitTransaction(int transactionID) {
		if(!activeTransactions.contains(transactionID)){
			//can't perform actions on non-active transaction
			return false;
		}
		Path p = Paths.get(transactionLocation, ""+transactionID);
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
	public boolean abortTransaction(int transactionID) {
		if(!activeTransactions.contains(transactionID)){
			//can't perform actions on non-active transaction
			return false;
		}
		//TODO: Revert all changes made in transaction so far, need to save old state somewhere as well to do this
		rm.readOldStateFromFile(""+transactionID, transactionLocation);
		lm.UnlockAll(transactionID);
		activeTransactions.remove(transactionID);
		return true;
	}

	@Override
	public boolean addFlight(int id, int flightNumber, int numSeats, int flightPrice, int transactionID) {
		if(!activeTransactions.contains(transactionID)){
			//can't perform actions on non-active transaction
			return false;
		}
		try {
			lm.Lock(transactionID, Flight.getKey(flightNumber), LockManager.WRITE);
			return rm.addFlight(id, flightNumber, numSeats, flightPrice);
		} catch (DeadlockException e) {
			System.out.println("addFlight failed, could not aquire write lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean deleteFlight(int id, int flightNumber, int transactionID) {
		if(!activeTransactions.contains(transactionID)){
			//can't perform actions on non-active transaction
			return false;
		}
		try {
			lm.Lock(transactionID, Flight.getKey(flightNumber), LockManager.WRITE);
			return rm.deleteFlight(id, flightNumber);
		} catch (DeadlockException e) {
			System.out.println("deleteFlight failed, could not aquire write lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public int queryFlight(int id, int flightNumber, int transactionID) {
		if(!activeTransactions.contains(transactionID)){
			//can't perform actions on non-active transaction
			return -1;
		}
		try {
			lm.Lock(transactionID, Flight.getKey(flightNumber), LockManager.READ);
			return rm.queryFlight(id, flightNumber);
		} catch (DeadlockException e) {
			System.out.println("queryFlight failed, could not aquire read lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			e.printStackTrace();
		}
		return -1;
	}

	@Override
	public int queryFlightPrice(int id, int flightNumber, int transactionID) {
		if(!activeTransactions.contains(transactionID)){
			//can't perform actions on non-active transaction
			return -1;
		}
		try {
			lm.Lock(transactionID, Flight.getKey(flightNumber), LockManager.READ);
			return rm.queryFlightPrice(id, flightNumber);
		} catch (DeadlockException e) {
			System.out.println("queryFlightPrice failed, could not aquire read lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			e.printStackTrace();
		}
		return -1;
	}

	@Override
	public boolean addCars(int id, String location, int numCars, int carPrice, int transactionID) {
		if(!activeTransactions.contains(transactionID)){
			//can't perform actions on non-active transaction
			return false;
		}
		try {
			lm.Lock(transactionID, Car.getKey(location), LockManager.WRITE);
			return rm.addCars(id, location, numCars, carPrice);
		} catch (DeadlockException e) {
			System.out.println("addCars failed, could not aquire write lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean deleteCars(int id, String location, int transactionID) {
		if(!activeTransactions.contains(transactionID)){
			//can't perform actions on non-active transaction
			return false;
		}
		try {
			lm.Lock(transactionID, Car.getKey(location), LockManager.WRITE);
			return rm.deleteCars(id, location);
		} catch (DeadlockException e) {
			System.out.println("deleteCars failed, could not aquire write lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public int queryCars(int id, String location, int transactionID) {
		if(!activeTransactions.contains(transactionID)){
			//can't perform actions on non-active transaction
			return -1;
		}
		try {
			lm.Lock(transactionID, Car.getKey(location), LockManager.READ);
			return rm.queryCars(id, location);
		} catch (DeadlockException e) {
			System.out.println("queryCars failed, could not aquire read lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			e.printStackTrace();
		}
		return -1;
	}

	@Override
	public int queryCarsPrice(int id, String location, int transactionID) {
		if(!activeTransactions.contains(transactionID)){
			//can't perform actions on non-active transaction
			return -1;
		}
		try {
			lm.Lock(transactionID, Car.getKey(location), LockManager.READ);
			return rm.queryCarsPrice(id, location);
		} catch (DeadlockException e) {
			System.out.println("queryCarsPrice failed, could not aquire read lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			e.printStackTrace();
		}
		return -1;
	}

	@Override
	public boolean addRooms(int id, String location, int numRooms, int roomPrice, int transactionID) {
		if(!activeTransactions.contains(transactionID)){
			//can't perform actions on non-active transaction
			return false;
		}
		try {
			lm.Lock(transactionID, Room.getKey(location), LockManager.WRITE);
			return rm.addRooms(id, location, numRooms, roomPrice);
		} catch (DeadlockException e) {
			System.out.println("addRooms failed, could not aquire write lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean deleteRooms(int id, String location, int transactionID) {
		if(!activeTransactions.contains(transactionID)){
			//can't perform actions on non-active transaction
			return false;
		}
		try {
			lm.Lock(transactionID, Room.getKey(location), LockManager.WRITE);
			return rm.deleteRooms(id, location);
		} catch (DeadlockException e) {
			System.out.println("deleteRooms failed, could not aquire write lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public int queryRooms(int id, String location, int transactionID) {
		if(!activeTransactions.contains(transactionID)){
			//can't perform actions on non-active transaction
			return -1;
		}
		try {
			lm.Lock(transactionID, Room.getKey(location), LockManager.READ);
			return rm.queryRooms(id, location);
		} catch (DeadlockException e) {
			System.out.println("queryRooms failed, could not aquire write lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			e.printStackTrace();
		}
		return -1;
	}

	@Override
	public int queryRoomsPrice(int id, String location, int transactionID) {
		if(!activeTransactions.contains(transactionID)){
			//can't perform actions on non-active transaction
			return -1;
		}
		try {
			lm.Lock(transactionID, Room.getKey(location), LockManager.READ);
			return rm.queryRoomsPrice(id, location);
		} catch (DeadlockException e) {
			System.out.println("queryRoomPrice failed, could not aquire write lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			e.printStackTrace();
		}
		return -1;
	}

	@Override
	public int newCustomer(int id, int transactionID) {
		if(!activeTransactions.contains(transactionID)){
			//can't perform actions on non-active transaction
			return -1;
		}
		try {
			//No RMHashtable key associated with new customer. Instead lock string "newcustomer"
			lm.Lock(transactionID, "newcustomer", LockManager.WRITE);
			return rm.newCustomer(id);
		} catch (DeadlockException e) {
			System.out.println("newCustomer failed, could not aquire write lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			e.printStackTrace();
		}
		return -1;
	}

	@Override
	public boolean newCustomerId(int id, int customerNumber, int transactionID) {
		if(!activeTransactions.contains(transactionID)){
			//can't perform actions on non-active transaction
			return false;
		}
		try {
			//No RMHashtable key associated with new customer. Instead lock string "newcustomer"
			lm.Lock(transactionID, "newcustomer", LockManager.WRITE);
			return rm.newCustomerId(id, customerNumber);
		} catch (DeadlockException e) {
			System.out.println("newCustomerID failed, could not aquire write lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean deleteCustomer(int id, int customerNumber, int transactionID) {
		if(!activeTransactions.contains(transactionID)){
			//can't perform actions on non-active transaction
			return false;
		}
		try {
			lm.Lock(transactionID, Customer.getKey(customerNumber), LockManager.WRITE);
			return rm.deleteCustomer(id, customerNumber);
		} catch (DeadlockException e) {
			System.out.println("deleteCustomer failed, could not aquire write lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public String queryCustomerInfo(int id, int customerNumber, int transactionID) {
		if(!activeTransactions.contains(transactionID)){
			//can't perform actions on non-active transaction
			return null;
		}
		try {
			lm.Lock(transactionID, Customer.getKey(customerNumber), LockManager.READ);
			return rm.queryCustomerInfo(id, customerNumber);
		} catch (DeadlockException e) {
			System.out.println("queryCustomerInfo failed, could not aquire read lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean reserveFlight(int id, int customerNumber, int flightNumber, int transactionID) {
		if(!activeTransactions.contains(transactionID)){
			//can't perform actions on non-active transaction
			return false;
		}
		try {
			lm.Lock(transactionID, "flight"+id, LockManager.WRITE);
			return rm.reserveFlight(id, customerNumber, flightNumber);
		} catch (DeadlockException e) {
			System.out.println("reserveFlight failed, could not aquire write lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean reserveCar(int id, int customerNumber, String location, int transactionID) {
		if(!activeTransactions.contains(transactionID)){
			//can't perform actions on non-active transaction
			return false;
		}
		try {
			lm.Lock(transactionID, "car"+id, LockManager.WRITE);
			return rm.reserveCar(id, customerNumber, location);
		} catch (DeadlockException e) {
			System.out.println("reserveCar failed, could not aquire write lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean reserveRoom(int id, int customerNumber, String location, int transactionID) {
		if(!activeTransactions.contains(transactionID)){
			//can't perform actions on non-active transaction
			return false;
		}
		try {
			lm.Lock(transactionID, "room"+id, LockManager.WRITE);
			return rm.reserveRoom(id, customerNumber, location);
		} catch (DeadlockException e) {
			System.out.println("reserveRoom failed, could not aquire write lock for id "+ id+ " in transaction "+ transactionID);
			System.out.println("Aborting transaction "+ transactionID);
			this.abortTransaction(transactionID);
			e.printStackTrace();
		}
		return false;
	}

}
