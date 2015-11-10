package middleware;

import server.RMRequestHandler;
import server.ResourceManagerImpl;
import shared.IRequestHandler;
import shared.RequestDescriptor;
import shared.RequestType;
import shared.ResponseDescriptor;
import shared.ResponseType;
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
				mode = ServerMode.FLIGHT;
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
			case STARTTXN:
			case COMMIT:
			case ABORT:
			case SHUTDOWN:
				mode = ServerMode.CUSTOMER;
				break;
			default:
				break;
			}
			
			if (mode == ServerMode.CUSTOMER) {
				if (request.requestType == RequestType.QUERYCUSTOMER){
					return this.queryCustomer(request);
				}else{
					for( ServerConnection connection : cm.getAllConnections()){
						connection.sendRequest(request);
					}
					return rh.handleRequest(request);
				}
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
	private ResponseDescriptor queryCustomer(RequestDescriptor request) throws Exception{
		String custInfo = "";
		String startLine = null;
		String endLine = null;
		for( ServerConnection connection : cm.getAllConnections()){
			ResponseDescriptor rd = connection.sendRequest(request);
			String data = (String) rd.data;
			if (rd.responseType == ResponseType.ERROR || rd.responseType == ResponseType.ABORT) {
				throw new Exception("Error. Data: " + (String) data + ", Message: " + rd.additionalMessage);
			}
			String[] lines = data.split("\n");
			for (int i = 1 ; i < lines.length - 1 ; i++){
				custInfo += lines[i] + "\n";
			}
			if(startLine == null){
				startLine = lines[0];
				endLine = lines[lines.length - 1];
			}
		}
		custInfo = startLine + "\n" + custInfo + endLine;
		
		return new ResponseDescriptor(custInfo);
	}
	private ResponseDescriptor reserveItinerary(RequestDescriptor request) {
		int transactionID = request.transactionID;
		int customerNumber = request.customerNumber;
		String location = request.location;
		boolean car = request.car;
		boolean room = request.room;
		RequestDescriptor req2 = null;
		ResponseDescriptor res = new ResponseDescriptor();
		//return if no flights
		if(request.flightNumbers == null || request.flightNumbers.isEmpty()){
			res.data = false;
			res.additionalMessage = "No flight numbers requested";
			return res;
		}
		//Query all flights, return if any are full
		for (int i = 0 ; i < request.flightNumbers.size() ; i++){
			req2 = new RequestDescriptor(RequestType.QUERYFLIGHT);
			req2.transactionID = transactionID;
			req2.flightNumber = request.flightNumbers.elementAt(i);
			ResponseDescriptor rd = this.handleRequest(req2);
			if( rd.responseType == ResponseType.INTEGER && (int)rd.data <= 0) {
				res.data = false;
				res.additionalMessage = "Flight " + req2.flightNumber + " is full";
				return res;
			}
		}
		//Query room and car if selected
		if(room){
			req2 = new RequestDescriptor(RequestType.QUERYROOM);
			req2.transactionID = transactionID;
			req2.location = location;
			ResponseDescriptor rd = this.handleRequest(req2);
			if( rd.responseType == ResponseType.INTEGER && (int)rd.data <= 0) {
				res.data = false;
				res.additionalMessage = "No car available at " + location;
				return res;
			}
		}
		if(car){
			req2 = new RequestDescriptor(RequestType.QUERYCAR);
			req2.transactionID = transactionID;
			req2.location = location;
			ResponseDescriptor rd = this.handleRequest(req2);
			if( rd.responseType == ResponseType.INTEGER && (int)rd.data <= 0) {
				res.data = false;
				res.additionalMessage = "No room available at " + location;
				return res;
			}
		}
		//Book flights, room and car
		for (int i = 0 ; i < request.flightNumbers.size() ; i++){
			req2 = new RequestDescriptor(RequestType.RESERVEFLIGHT);
			req2.transactionID = transactionID;
			req2.customerNumber = customerNumber;
			req2.flightNumber = request.flightNumbers.elementAt(i);
			ResponseDescriptor rd = this.handleRequest(req2);
			if( rd.responseType == ResponseType.BOOLEAN && !(boolean)rd.data) {
				res.data = false;
				res.additionalMessage = "Problem booking " + req2.flightNumber;
				return res;
			}
			
		}
		if(car){
			req2 = new RequestDescriptor(RequestType.RESERVECAR);
			req2.transactionID = transactionID;
			req2.customerNumber = customerNumber;
			req2.location = location;
			ResponseDescriptor rd = this.handleRequest(req2);
			if( rd.responseType == ResponseType.BOOLEAN && !(boolean)rd.data) {
				res.data = false;
				res.additionalMessage = "Problem booking car at " + location;
				return res;
			}
		}
		if(room){
			req2 = new RequestDescriptor(RequestType.RESERVEROOM);
			req2.transactionID = transactionID;
			req2.customerNumber = customerNumber;
			req2.location = location;
			ResponseDescriptor rd = this.handleRequest(req2);
			if( rd.responseType == ResponseType.BOOLEAN && !(boolean)rd.data) {
				res.data = false;
				res.additionalMessage = "Problem booking room at " + location;
				return res;
			}
		}
		res.data = true;
		res.additionalMessage = "Success!";
		return res;
	}
}
