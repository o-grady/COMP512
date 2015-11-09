package server;

import shared.IRequestHandler;
import shared.RequestDescriptor;
import shared.ResponseDescriptor;

public class RMRequestHandler implements IRequestHandler {

	private ResourceManager rm;

	public RMRequestHandler(ResourceManager rm) {
		this.rm = rm;
	}

	public ResponseDescriptor handleRequest(RequestDescriptor request) {
		int intResponse = -1;
		boolean boolResponse = false;
		String stringResponse = null;
		switch (request.requestType) {
		case NEWFLIGHT:
			System.out.println("NEWFLIGHT received");
			boolResponse = rm.addFlight(request.transactionID,
					request.flightNumber, request.numSeats, request.price);
			break;
		case NEWCAR:
			System.out.println("NEWCAR received");
			boolResponse = rm.addCars(request.transactionID,
					request.location, request.numCars, request.price);
			break;
		case NEWROOM:
			System.out.println("NEWROOM received");
			boolResponse = rm.addRooms(request.transactionID,
					request.location, request.numRooms, request.price);
			break;
		case NEWCUSTOMER:
			System.out.println("NEWCUSTOMER received");
			intResponse = rm.newCustomer(request.transactionID);
			break;
		case NEWCUSTOMERID:
			System.out.println("NEWCUSTOMERID received");
			boolResponse = rm.newCustomerId(request.transactionID,
					request.customerNumber);
			break;
		case DELETEFLIGHT:
			System.out.println("DELETEFLIGHT received");
			boolResponse = rm.deleteFlight(request.transactionID,
					request.flightNumber);
			break;
		case DELETECAR:
			System.out.println("DELETECAR received");
			boolResponse = rm.deleteCars(request.transactionID,
					request.location);
			break;
		case DELETEROOM:
			System.out.println("DELETEROOM received");
			boolResponse = rm.deleteRooms(request.transactionID,
					request.location);
			break;
		case DELETECUSTOMER:
			System.out.println("DELETECUSTOMER received");
			boolResponse = rm.deleteCustomer(request.transactionID,
					request.customerNumber);
			break;
		case QUERYFLIGHT:
			System.out.println("QUERYFLIGHT received");
			intResponse = rm.queryFlight(request.transactionID,
					request.flightNumber);
			break;
		case QUERYCAR:
			System.out.println("QUERYCAR received");
			intResponse = rm.queryCars(request.transactionID,
					request.location);
			break;
		case QUERYROOM:
			System.out.println("QUERYROOM received");
			intResponse = rm.queryRooms(request.transactionID,
					request.location);
			break;
		case QUERYCUSTOMER:
			System.out.println("QUERYCUSTOMER received");
			// Pretty sure its this method
			stringResponse = rm.queryCustomerInfo(request.transactionID,
					request.customerNumber);
			break;
		case QUERYFLIGHTPRICE:
			System.out.println("QUERYFLIGHTPRICE received");
			intResponse = rm.queryFlightPrice(request.transactionID,
					request.flightNumber);
			break;
		case QUERYCARPRICE:
			System.out.println("QUERYCARPRICE received");
			intResponse = rm.queryCarsPrice(request.transactionID,
					request.location);
			break;
		case QUERYROOMPRICE:
			System.out.println("QUERYROOMPRICE received");
			intResponse = rm.queryRoomsPrice(request.transactionID,
					request.location);
			break;
		case RESERVEFLIGHT:
			System.out.println("RESERVEFLIGHT received");
			boolResponse = rm.reserveFlight(request.transactionID,
					request.customerNumber, request.flightNumber);
			break;
		case RESERVECAR:
			System.out.println("RESERVECAR received");
			boolResponse = rm.reserveCar(request.transactionID,
					request.customerNumber, request.location);
			break;
		case RESERVEROOM:
			System.out.println("RESERVEROOM received");
			boolResponse = rm.reserveRoom(request.transactionID,
					request.customerNumber, request.location);
			break;
		default:
			System.out.println("Received unhandled signal at RMRequestHandler: " + request.requestType.toString());
			break;
		}
		if(stringResponse != null){
			return new ResponseDescriptor(stringResponse);
		}else if(intResponse != -1){
			return new ResponseDescriptor(intResponse);
		}else{
			return new ResponseDescriptor(boolResponse);
		}

	}

}
