package server;

import java.util.HashSet;
import java.util.Set;

import shared.LockManager.DeadlockException;
import shared.LockManager.LockManager;

public class TransactionManagerImpl implements TransactionManager {
	
	ResourceManager rm;
	//lock data using following scheme: (flight|car|room|customer) + ID
	LockManager lm;
    Set<Integer> activeTransactions;
	int transactionCounter;
	
	public TransactionManagerImpl(ResourceManager rm, LockManager lm){
		this.rm = rm;
		this.lm = lm;
		activeTransactions = new HashSet<Integer>();
		transactionCounter = 0;
	}
	
	@Override
	public int startTransaction() {
		transactionCounter++;
		activeTransactions.add(transactionCounter);
		return transactionCounter;
	}

	@Override
	public boolean commitTransaction(int transactionID) {
		if(!activeTransactions.contains(transactionID)){
			//can't perform actions on non-active transaction
			return false;
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
			lm.Lock(transactionID, "flight"+id, LockManager.WRITE);
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
			lm.Lock(transactionID, "flight"+id, LockManager.WRITE);
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
			lm.Lock(transactionID, "flight"+id, LockManager.READ);
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
			lm.Lock(transactionID, "flight"+id, LockManager.READ);
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
			lm.Lock(transactionID, "car"+id, LockManager.WRITE);
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
			lm.Lock(transactionID, "car"+id, LockManager.WRITE);
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
			lm.Lock(transactionID, "car"+id, LockManager.READ);
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
			lm.Lock(transactionID, "car"+id, LockManager.READ);
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
			lm.Lock(transactionID, "room"+id, LockManager.WRITE);
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
			lm.Lock(transactionID, "room"+id, LockManager.WRITE);
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
			lm.Lock(transactionID, "room"+id, LockManager.READ);
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
			lm.Lock(transactionID, "room"+id, LockManager.READ);
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
			lm.Lock(transactionID, "customer"+id, LockManager.WRITE);
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
			lm.Lock(transactionID, "customer"+id, LockManager.WRITE);
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
			lm.Lock(transactionID, "customer"+id, LockManager.WRITE);
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
			lm.Lock(transactionID, "customer"+id, LockManager.READ);
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
