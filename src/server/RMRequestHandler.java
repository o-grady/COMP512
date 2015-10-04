package server;

import shared.RequestDescriptor;
import shared.ResponseDescriptor;

public class RMRequestHandler {

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
			boolResponse = rm.addFlight(request.id,
					request.flightNumber, request.numSeats, request.price);
			break;
		case NEWCAR:
			System.out.println("NEWCAR received");
			boolResponse = rm.addCars(request.id,
					request.location, request.numCars, request.price);
			break;
		case NEWROOM:
			System.out.println("NEWROOM received");
			boolResponse = rm.addRooms(request.id,
					request.location, request.numRooms, request.price);
			break;
		case NEWCUSTOMER:
			System.out.println("NEWCUSTOMER received");
			intResponse = rm.newCustomer(request.id);
			break;
		case NEWCUSTOMERID:
			System.out.println("NEWCUSTOMERID received");
			boolResponse = rm.newCustomerId(request.id,
					request.customerNumber);
			break;
		case DELETEFLIGHT:
			System.out.println("DELETEFLIGHT received");
			boolResponse = rm.deleteFlight(request.id,
					request.flightNumber);
			break;
		case DELETECAR:
			System.out.println("DELETECAR received");
			boolResponse = rm.deleteCars(request.id,
					request.location);
			break;
		case DELETEROOM:
			System.out.println("DELETEROOM received");
			boolResponse = rm.deleteRooms(request.id,
					request.location);
			break;
		case DELETECUSTOMER:
			System.out.println("DELETECUSTOMER received");
			boolResponse = rm.deleteCustomer(request.id,
					request.customerNumber);
			break;
		case QUERYFLIGHT:
			System.out.println("QUERYFLIGHT received");
			intResponse = rm.queryFlight(request.id,
					request.flightNumber);
			break;
		case QUERYCAR:
			System.out.println("QUERYCAR received");
			intResponse = rm.queryCars(request.id,
					request.location);
			break;
		case QUERYROOM:
			System.out.println("QUERYROOM received");
			intResponse = rm.queryRooms(request.id,
					request.location);
			break;
		case QUERYCUSTOMER:
			System.out.println("QUERYCUSTOMER received");
			// Pretty sure its this method
			stringResponse = rm.queryCustomerInfo(request.id,
					request.customerNumber);
			break;
		case QUERYFLIGHTPRICE:
			System.out.println("QUERYFLIGHTPRICE received");
			intResponse = rm.queryFlightPrice(request.id,
					request.flightNumber);
			break;
		case QUERYCARPRICE:
			System.out.println("QUERYCARPRICE received");
			intResponse = rm.queryCarsPrice(request.id,
					request.location);
			break;
		case QUERYROOMPRICE:
			System.out.println("QUERYROOMPRICE received");
			intResponse = rm.queryRoomsPrice(request.id,
					request.location);
			break;
		case RESERVEFLIGHT:
			System.out.println("RESERVEFLIGHT received");
			boolResponse = rm.reserveFlight(request.id,
					request.customerNumber, request.flightNumber);
			break;
		case RESERVECAR:
			System.out.println("RESERVECAR received");
			boolResponse = rm.reserveCar(request.id,
					request.customerNumber, request.location);
			break;
		case RESERVEROOM:
			System.out.println("RESERVEROOM received");
			boolResponse = rm.reserveRoom(request.id,
					request.customerNumber, request.location);
			break;
		case ITINERARY:
			System.out.println("ITINERARY received");
			// This one doesn't work. Fix when messages are passed as JSON
			// aResourceManager.reserveItinerary(request.id, request[2],
			// Integer.parseInt(request[3]), Integer.parseInt(request[4]));
			// outToClient.writeBytes("ITINERARY received");
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
