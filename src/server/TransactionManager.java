package server;

public interface TransactionManager {
	public int startTransaction();
	
	public int enlist(int transactionID);
	
	public boolean twoPhaseCommitTransaction(int transactionID) throws NotWaitingForVoteResultException;
	
	public boolean abortTransaction(int transactionID) throws TransactionNotActiveException, TransactionBlockingException;

	public boolean abortAllActiveTransactions() throws TransactionBlockingException; 

	//RM methods, with additional transactionID. These methods will call the RM methods and aquire the correct locks
    public boolean addFlight(int transactionID, int flightNumber, int numSeats, int flightPrice) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException; 


    public boolean deleteFlight(int transactionID, int flightNumber) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException; 

    /* Return the number of empty seats in this flight. */
    public int queryFlight(int transactionID, int flightNumber) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException; 

    /* Return the price of a seat on this flight. */
    public int queryFlightPrice(int transactionID, int flightNumber) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException; 


    // Car operations //

    /* Add cars to a location.  
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     */    
    public boolean addCars(int transactionID, String location, int numCars, int carPrice) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException; 
    
    /* Delete all cars from a location.
     * It should not succeed if there are reservations for this location.
     */
    public boolean deleteCars(int transactionID, String location) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException; 

    /* Return the number of cars available at this location. */    
    public int queryCars(int transactionID, String location) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException; 

    /* Return the price of a car at this location. */    
    public int queryCarsPrice(int transactionID, String location) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException; 


    // Room operations //
    
    /* Add rooms to a location.  
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     */
    
    public boolean addRooms(int transactionID, String location, int numRooms, int roomPrice) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException;

    /* Delete all rooms from a location.
     * It should not succeed if there are reservations for this location.
     */    
    public boolean deleteRooms(int transactionID, String location) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException; 

    /* Return the number of rooms available at this location. */    
    public int queryRooms(int transactionID, String location) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException; 

    /* Return the price of a room at this location. */    
    public int queryRoomsPrice(int transactionID, String location) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException; 


    // Customer operations //
        
    /* Create a new customer and return their unique identifier. */    
    public int newCustomer(int transactionID) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException; 
    
    /* Create a new customer with the provided identifier. */    
    public boolean newCustomerId(int transactionID, int customerNumber) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException;

    /* Remove this customer and all their associated reservations. */    
    public boolean deleteCustomer(int transactionID, int customerNumber) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException; 

    /* Return a bill. */    
    public String queryCustomerInfo(int transactionID, int customerNumber) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException; 

    /* Reserve a seat on this flight. */    
    public boolean reserveFlight(int transactionID, int customerNumber, int flightNumber) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException; 

    /* Reserve a car at this location. */    
    public boolean reserveCar(int transactionID, int customerNumber, String location) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException; 

    /* Reserve a room at this location. */    
    public boolean reserveRoom(int transactionID, int customerNumber, String location) throws AbortedTransactionException, TransactionNotActiveException, TransactionBlockingException;
    
    /* Returns true if transaction can be committed */
	boolean prepare(int transactionID) throws TransactionNotActiveException, TransactionBlockingException;

	boolean twoPhaseAbortTransaction(int transactionID) throws NotWaitingForVoteResultException;



}
