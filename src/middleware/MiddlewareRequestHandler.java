package middleware;

import server.RMRequestHandler;
import server.ResourceManagerImpl;
import shared.IRequestHandler;
import shared.RequestDescriptor;
import shared.RequestType;
import shared.ResponseDescriptor;
import shared.ServerConnection;

public class MiddlewareRequestHandler implements IRequestHandler {

	private ConnectionManager cm;
	private RMRequestHandler rh;
	
	public MiddlewareRequestHandler(ConnectionManager cm) {
		this.cm = cm;
		this.rh = new RMRequestHandler(new ResourceManagerImpl());
	}
	
	public ResponseDescriptor handleRequest(RequestDescriptor request) {
		try {
			ServerMode mode = null;
			if (request.requestType == RequestType.ITINERARY) {
				return reserveItinerary(request);
			}
			
			switch(request.requestType) {
			case DELETECAR:
			case NEWCAR:
			case QUERYCAR:
			case QUERYCARPRICE:
			case RESERVECAR:
				mode = ServerMode.CAR;
				break;
			case DELETEFLIGHT:
			case NEWFLIGHT:
			case QUERYFLIGHT:
			case QUERYFLIGHTPRICE:
			case RESERVEFLIGHT:
				mode = ServerMode.PLANE;
				break;
			case DELETEROOM:
			case NEWROOM:
			case QUERYROOM:
			case QUERYROOMPRICE:
			case RESERVEROOM:
				mode = ServerMode.ROOM;
				break;
			case DELETECUSTOMER:
			case NEWCUSTOMER:
			case NEWCUSTOMERID:
			case QUERYCUSTOMER:
				mode = ServerMode.CUSTOMER;
				break;
			default:
				break;
			}
			
			if (mode == ServerMode.CUSTOMER) {
				for( ServerConnection connection : cm.getAllConnections()){
					connection.sendRequest(request);
				}
				return rh.handleRequest(request);
			}
			else if (cm.modeIsConnected(mode)) {
				return cm.getConnection(mode).sendRequest(request);
			} else {
				throw new Exception("Server not connected");
			}
		}
		catch(Exception ex) {
			return new ResponseDescriptor("Error: " + ex.getClass().getName() + ", " + ex.getMessage());
		}
	}
	
	private ResponseDescriptor reserveItinerary(RequestDescriptor request) {
		int id = request.id;
		int customerNumber = request.customerNumber;
		String location = request.location;
		boolean car = request.car;
		boolean room = request.room;
		RequestDescriptor req2 = null;
		ResponseDescriptor res = new ResponseDescriptor();
		//return if no flights
		if(request.flightNumbers == null){
			res.booleanResponse = false;
			res.additionalMessage = "No flight numbers requested";
			return res;
		}
		//Query all flights, return if any are full
		for (int i = 0 ; i < request.flightNumbers.size() ; i++){
			req2 = new RequestDescriptor(RequestType.QUERYFLIGHT);
			req2.id = id;
			req2.flightNumber = request.flightNumbers.elementAt(i);
			if( this.handleRequest(req2).intResponse <= 0){
				res.booleanResponse = false;
				res.additionalMessage = "Flight " + req2.flightNumber + " is full";
				return res;
			}
		}
		//Query room and car if selected
		if(room){
			req2 = new RequestDescriptor(RequestType.QUERYROOM);
			req2.id = id;
			req2.location = location;
			if(this.handleRequest(req2).intResponse <= 0){
				res.booleanResponse = false;
				res.additionalMessage = "No car available at " + location;
				return res;
			}
		}
		if(car){
			req2 = new RequestDescriptor(RequestType.QUERYCAR);
			req2.id = id;
			req2.location = location;
			if(this.handleRequest(req2).intResponse <= 0){
				res.booleanResponse = false;
				res.additionalMessage = "No room available at " + location;
				return res;
			}
		}
		//Book flights, room and car
		for (int i = 0 ; i < request.flightNumbers.size() ; i++){
			req2 = new RequestDescriptor(RequestType.RESERVEFLIGHT);
			req2.id = id;
			req2.customerNumber = customerNumber;
			req2.flightNumber = request.flightNumbers.elementAt(i);
			if( !this.handleRequest(req2).booleanResponse){
				res.booleanResponse = false;
				res.additionalMessage = "Problem booking " + req2.flightNumber;
				return res;
			}
			
		}
		if(car){
			req2 = new RequestDescriptor(RequestType.RESERVECAR);
			req2.id = id;
			req2.customerNumber = customerNumber;
			req2.location = location;
			if(!this.handleRequest(req2).booleanResponse){
				res.booleanResponse = false;
				res.additionalMessage = "Problem booking car at " + location;
				return res;
			}
		}
		if(room){
			req2 = new RequestDescriptor(RequestType.RESERVEROOM);
			req2.id = id;
			req2.customerNumber = customerNumber;
			req2.location = location;
			if(this.handleRequest(req2).booleanResponse){
				res.booleanResponse = false;
				res.additionalMessage = "Problem booking room at " + location;
				return res;
			}
		}
		res.booleanResponse = true;
		res.additionalMessage = "Success!";
		return res;
	}
}