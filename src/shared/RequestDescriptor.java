package shared;

import java.io.Serializable;
import java.util.Vector;

import middleware.ServerMode;

public class RequestDescriptor implements Serializable {

	private static final long serialVersionUID = -1943991785815079042L;
	public final RequestType requestType;
	
    public int transactionID;
    public int flightNumber;
    public int numSeats;
    public boolean room;
    public boolean car;
    public int price;
    public int numRooms;
    public int numCars;
    public String location;
    public int customerNumber;
    public ServerMode serverToCrash;
    public boolean canCommit;

    public Vector<Integer> flightNumbers;
	
	public RequestDescriptor(RequestType requestType) {
		this.requestType = requestType;
		this.transactionID = this.flightNumber = this.numSeats = this.price = this.numRooms = this.numCars = this.customerNumber = -1;
		this.room = this.car = false;
		this.location = null;
		this.flightNumbers = null;
		this.serverToCrash = null;
		this.canCommit = false;
	}
	public String toString(){
		return "transactionID = " + this.transactionID + "\n"
				+ "flightNumber = " + this.flightNumber + "\n"
				+ "numSeats = " + this.numSeats + "\n"
				+ "room = " + this.room + "\n"
				+ "car = " + this.car + "\n"
				+ "price = " + this.price + "\n"
				+ "numRooms = " + this.numRooms + "\n"
				+ "numCars = " + this.numCars + "\n"
				+ "location = " + this.location + "\n"
				+ "customerNumber = " + this.customerNumber + "\n"
				+ "serverToCrash = " + this.serverToCrash;
	}
}