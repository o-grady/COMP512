package server;

public class TransactionManagerImpl implements TransactionManager {

	@Override
	public int startTransaction() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean commitTransaction(int transactionID) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean abortTransaction(int transactionID) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean addFlight(int id, int flightNumber, int numSeats, int flightPrice) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteFlight(int id, int flightNumber, int transactionID) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int queryFlight(int id, int flightNumber, int transactionID) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int queryFlightPrice(int id, int flightNumber, int transactionID) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean addCars(int id, String location, int numCars, int carPrice, int transactionID) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteCars(int id, String location, int transactionID) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int queryCars(int id, String location, int transactionID) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int queryCarsPrice(int id, String location, int transactionID) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean addRooms(int id, String location, int numRooms, int roomPrice, int transactionID) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteRooms(int id, String location, int transactionID) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int queryRooms(int id, String location, int transactionID) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int queryRoomsPrice(int id, String location, int transactionID) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int newCustomer(int id, int transactionID) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean newCustomerId(int id, int customerNumber, int transactionID) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteCustomer(int id, int customerNumber, int transactionID) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String queryCustomerInfo(int id, int customerNumber, int transactionID) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean reserveFlight(int id, int customerNumber, int flightNumber, int transactionID) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean reserveCar(int id, int customerNumber, String location, int transactionID) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean reserveRoom(int id, int customerNumber, String location, int transactionID) {
		// TODO Auto-generated method stub
		return false;
	}

}
