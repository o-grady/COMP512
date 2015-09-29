package shared;

import java.io.Serializable;

public class RequestDescriptor implements Serializable {

	private static final long serialVersionUID = -1943991785815079042L;
	public RequestType requestType;
	
    public int id;
    public int flightNumber;
    public int numSeats;
    public boolean room;
    public boolean car;
    public int price;
    public int numRooms;
    public int numCars;
    public String location;
    public int customerNumber;
	
	public RequestDescriptor(RequestType requestType) {
		this.requestType = requestType;
	}
}