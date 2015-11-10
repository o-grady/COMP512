// -------------------------------// Adapted from Kevin T. Manley// CSE 593// -------------------------------package server;public class Flight extends ReservableItem {    /**	 * 	 */	private static final long serialVersionUID = 1824468416698686502L;	public Flight(int flightNumber, int numSeats, int flightPrice) {        super(Integer.toString(flightNumber), numSeats, flightPrice);    }    public String getKey() {        return Flight.getKey(Integer.parseInt(getLocation()));    }    public static String getKey(int flightNumber) {        String s = "flight-" + flightNumber;        return s.toLowerCase();    }}