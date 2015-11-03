package server;

import java.util.Vector;

public interface TransactionManager {
	public int startTransaction();
	
	public boolean commitTransaction(int transactionID);
	
	public boolean abortTransaction(int transactionID);

	public boolean abortAllActiveTransactions(); 

	//RM methods, with additional transactionID. These methods will call the RM methods and aquire the correct locks
    public boolean addFlight(int id, int flightNumber, int numSeats, int flightPrice, int transactionID); 


    public boolean deleteFlight(int id, int flightNumber, int transactionID); 

    /* Return the number of empty seats in this flight. */
    public int queryFlight(int id, int flightNumber, int transactionID); 

    /* Return the price of a seat on this flight. */
    public int queryFlightPrice(int id, int flightNumber, int transactionID); 


    // Car operations //

    /* Add cars to a location.  
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     */    
    public boolean addCars(int id, String location, int numCars, int carPrice, int transactionID); 
    
    /* Delete all cars from a location.
     * It should not succeed if there are reservations for this location.
     */
    public boolean deleteCars(int id, String location, int transactionID); 

    /* Return the number of cars available at this location. */    
    public int queryCars(int id, String location, int transactionID); 

    /* Return the price of a car at this location. */    
    public int queryCarsPrice(int id, String location, int transactionID); 


    // Room operations //
    
    /* Add rooms to a location.  
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     */
    
    public boolean addRooms(int id, String location, int numRooms, int roomPrice, int transactionID);

    /* Delete all rooms from a location.
     * It should not succeed if there are reservations for this location.
     */    
    public boolean deleteRooms(int id, String location, int transactionID); 

    /* Return the number of rooms available at this location. */    
    public int queryRooms(int id, String location, int transactionID); 

    /* Return the price of a room at this location. */    
    public int queryRoomsPrice(int id, String location, int transactionID); 


    // Customer operations //
        
    /* Create a new customer and return their unique identifier. */    
    public int newCustomer(int id, int transactionID); 
    
    /* Create a new customer with the provided identifier. */    
    public boolean newCustomerId(int id, int customerNumber, int transactionID);

    /* Remove this customer and all their associated reservations. */    
    public boolean deleteCustomer(int id, int customerNumber, int transactionID); 

    /* Return a bill. */    
    public String queryCustomerInfo(int id, int customerNumber, int transactionID); 

    /* Reserve a seat on this flight. */    
    public boolean reserveFlight(int id, int customerNumber, int flightNumber, int transactionID); 

    /* Reserve a car at this location. */    
    public boolean reserveCar(int id, int customerNumber, String location, int transactionID); 

    /* Reserve a room at this location. */    
    public boolean reserveRoom(int id, int customerNumber, String location, int transactionID);



}
