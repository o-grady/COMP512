package middleware;

import shared.RequestDescriptor;
import shared.RequestType;
import shared.ResponseDescriptor;

public class RequestHandler {

	private ConnectionManager cm;
	
	public RequestHandler(ConnectionManager cm) {
		this.cm = cm;
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
			
			if (cm.modeIsConnected(mode)) {
				return cm.getConnection(mode).sendRequest(request);
			} else {
				throw new Exception("Server not connected");
			}
		}
		catch(Exception ex) {
			return new ResponseDescriptor("Error: " + ex.getMessage());
		}
	}
	
	private ResponseDescriptor reserveItinerary(RequestDescriptor request) {
		return null;
	}
}
