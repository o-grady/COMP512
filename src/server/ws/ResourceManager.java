/** 
 * Simplified version from CSE 593, University of Washington.
 *
 * A Distributed System in Java using Web Services.
 * 
 * Failures should be reported via the return value.  For example, 
 * if an operation fails, you should return either false (boolean), 
 * or some error code like -1 (int).
 *
 * If there is a boolean return value and you're not sure how it 
 * would be used in your implementation, ignore it.  I used boolean
 * return values in the interface generously to allow flexibility in 
 * implementation.  But don't forget to return true when the operation
 * has succeeded.
 */

package server.ws;

import java.util.*;

public interface ResourceManager {
    
    // Flight operations //
    
    /* Add seats to a flight.  
     * In general, this will be used to create a new flight, but it should be 
     * possible to add seats to an existing flight.  Adding to an existing 
     * flight should overwrite the current price of the available seats.
     *
     * @return success.
     */
    public boolean addFlight(int id, int flightNumber, int numSeats, int flightPrice); 

    /**
     * Delete the entire flight.
     * This implies deletion of this flight and all its seats.  If there is a 
     * reservation on the flight, then the flight cannot be deleted.
     *
     * @return success.
     */   
    public boolean deleteFlight(int id, int flightNumber); 

    /* Return the number of empty seats in this flight. */
    public int queryFlight(int id, int flightNumber); 

    /* Return the price of a seat on this flight. */
    public int queryFlightPrice(int id, int flightNumber); 


    // Car operations //

    /* Add cars to a location.  
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     */    
    public boolean addCars(int id, String location, int numCars, int carPrice); 
    
    /* Delete all cars from a location.
     * It should not succeed if there are reservations for this location.
     */
    public boolean deleteCars(int id, String location); 

    /* Return the number of cars available at this location. */    
    public int queryCars(int id, String location); 

    /* Return the price of a car at this location. */    
    public int queryCarsPrice(int id, String location); 


    // Room operations //
    
    /* Add rooms to a location.  
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     */
    
    public boolean addRooms(int id, String location, int numRooms, int roomPrice); 			    

    /* Delete all rooms from a location.
     * It should not succeed if there are reservations for this location.
     */    
    public boolean deleteRooms(int id, String location); 

    /* Return the number of rooms available at this location. */    
    public int queryRooms(int id, String location); 

    /* Return the price of a room at this location. */    
    public int queryRoomsPrice(int id, String location); 


    // Customer operations //
        
    /* Create a new customer and return their unique identifier. */    
    public int newCustomer(int id); 
    
    /* Create a new customer with the provided identifier. */    
    public boolean newCustomerId(int id, int customerNumber);

    /* Remove this customer and all their associated reservations. */    
    public boolean deleteCustomer(int id, int customerNumber); 

    /* Return a bill. */    
    public String queryCustomerInfo(int id, int customerNumber); 

    /* Reserve a seat on this flight. */    
    public boolean reserveFlight(int id, int customerNumber, int flightNumber); 

    /* Reserve a car at this location. */    
    public boolean reserveCar(int id, int customerNumber, String location); 

    /* Reserve a room at this location. */    
    public boolean reserveRoom(int id, int customerNumber, String location); 


    /* Reserve an itinerary. */    
    public boolean reserveItinerary(int id, int customerNumber, Vector flightNumbers, 
                                    String location, boolean car, boolean room);
    			
}
