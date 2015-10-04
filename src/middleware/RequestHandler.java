package middleware;

import server.RMRequestHandler;
import server.ResourceManagerImpl;
import shared.IRequestHandler;
import shared.RequestDescriptor;
import shared.RequestType;
import shared.ResponseDescriptor;

public class RequestHandler implements IRequestHandler {

	private ConnectionManager cm;
	private RMRequestHandler rh;
	
	public RequestHandler(ConnectionManager cm) {
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
		return null;
	}
}
