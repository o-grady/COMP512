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
		String responseMessage = "";
		switch (request.requestType) {
		case NEWFLIGHT:
			System.out.println("NEWFLIGHT received");
			responseMessage = Boolean.toString(rm.addFlight(request.id,
					request.flightNumber, request.numSeats, request.price));
			break;
		case NEWCAR:
			System.out.println("NEWCAR received");
			responseMessage = Boolean.toString(rm.addCars(request.id,
					request.location, request.numCars, request.price));
			break;
		case NEWROOM:
			System.out.println("NEWROOM received");
			responseMessage = Boolean.toString(rm.addRooms(request.id,
					request.location, request.numRooms, request.price));
			break;
		case NEWCUSTOMER:
			System.out.println("NEWCUSTOMER received");
			responseMessage = Integer.toString(rm.newCustomer(request.id));
			break;
		case NEWCUSTOMERID:
			System.out.println("NEWCUSTOMERID received");
			responseMessage = Boolean.toString(rm.newCustomerId(request.id,
					request.customerNumber));
			break;
		case DELETEFLIGHT:
			System.out.println("DELETEFLIGHT received");
			responseMessage = Boolean.toString(rm.deleteFlight(request.id,
					request.flightNumber));
			break;
		case DELETECAR:
			System.out.println("DELETECAR received");
			responseMessage = Boolean.toString(rm.deleteCars(request.id,
					request.location));
			break;
		case DELETEROOM:
			System.out.println("DELETEROOM received");
			responseMessage = Boolean.toString(rm.deleteRooms(request.id,
					request.location));
			break;
		case DELETECUSTOMER:
			System.out.println("DELETECUSTOMER received");
			responseMessage = Boolean.toString(rm.deleteCustomer(request.id,
					request.customerNumber));
			break;
		case QUERYFLIGHT:
			System.out.println("QUERYFLIGHT received");
			responseMessage = Integer.toString(rm.queryFlight(request.id,
					request.flightNumber));
			break;
		case QUERYCAR:
			System.out.println("QUERYCAR received");
			responseMessage = Integer.toString(rm.queryCars(request.id,
					request.location));
			break;
		case QUERYROOM:
			System.out.println("QUERYROOM received");
			responseMessage = Integer.toString(rm.queryRooms(request.id,
					request.location));
			break;
		case QUERYCUSTOMER:
			System.out.println("QUERYCUSTOMER received");
			// Pretty sure its this method
			responseMessage = rm.queryCustomerInfo(request.id,
					request.customerNumber);
			break;
		case QUERYFLIGHTPRICE:
			System.out.println("QUERYFLIGHTPRICE received");
			responseMessage = Integer.toString(rm.queryFlightPrice(request.id,
					request.flightNumber));
			break;
		case QUERYCARPRICE:
			System.out.println("QUERYCARPRICE received");
			responseMessage = Integer.toString(rm.queryCarsPrice(request.id,
					request.location));
			break;
		case QUERYROOMPRICE:
			System.out.println("QUERYROOMPRICE received");
			responseMessage = Integer.toString(rm.queryRoomsPrice(request.id,
					request.location));
			break;
		case RESERVEFLIGHT:
			System.out.println("RESERVEFLIGHT received");
			responseMessage = Boolean.toString(rm.reserveFlight(request.id,
					request.customerNumber, request.flightNumber));
			break;
		case RESERVECAR:
			System.out.println("RESERVECAR received");
			responseMessage = Boolean.toString(rm.reserveCar(request.id,
					request.customerNumber, request.location));
			break;
		case RESERVEROOM:
			System.out.println("RESERVEROOM received");
			responseMessage = Boolean.toString(rm.reserveRoom(request.id,
					request.customerNumber, request.location));
			break;
		case ITINERARY:
			System.out.println("ITINERARY received");
			// This one doesn't work. Fix when messages are passed as JSON
			// aResourceManager.reserveItinerary(request.id, request[2],
			// Integer.parseInt(request[3]), Integer.parseInt(request[4]));
			// outToClient.writeBytes("ITINERARY received");
			break;
		}

		return new ResponseDescriptor(responseMessage);
	}

}
