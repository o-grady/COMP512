package server;

import shared.IRequestHandler;
import shared.RequestDescriptor;
import shared.ResponseDescriptor;

public class TMRequestHandler implements IRequestHandler {

	private TransactionManager tm;

	public TMRequestHandler(TransactionManager tm) {
		this.tm = tm;
	}

	public ResponseDescriptor handleRequest(RequestDescriptor request) {
		int intResponse = -1;
		boolean boolResponse = false;
		String stringResponse = null;
		int transactionID = request.transactionID;
		switch (request.requestType) {
		case NEWFLIGHT:
			System.out.println("NEWFLIGHT received");
			boolResponse = tm.addFlight(request.id,
					request.flightNumber, request.numSeats, request.price, transactionID);
			break;
		case NEWCAR:
			System.out.println("NEWCAR received");
			boolResponse = tm.addCars(request.id,
					request.location, request.numCars, request.price, transactionID);
			break;
		case NEWROOM:
			System.out.println("NEWROOM received");
			boolResponse = tm.addRooms(request.id,
					request.location, request.numRooms, request.price, transactionID);
			break;
		case NEWCUSTOMER:
			System.out.println("NEWCUSTOMER received");
			intResponse = tm.newCustomer(request.id, transactionID);
			break;
		case NEWCUSTOMERID:
			System.out.println("NEWCUSTOMERID received");
			boolResponse = tm.newCustomerId(request.id,
					request.customerNumber, transactionID);
			break;
		case DELETEFLIGHT:
			System.out.println("DELETEFLIGHT received");
			boolResponse = tm.deleteFlight(request.id,
					request.flightNumber, transactionID);
			break;
		case DELETECAR:
			System.out.println("DELETECAR received");
			boolResponse = tm.deleteCars(request.id,
					request.location, transactionID);
			break;
		case DELETEROOM:
			System.out.println("DELETEROOM received");
			boolResponse = tm.deleteRooms(request.id,
					request.location, transactionID);
			break;
		case DELETECUSTOMER:
			System.out.println("DELETECUSTOMER received");
			boolResponse = tm.deleteCustomer(request.id,
					request.customerNumber, transactionID);
			break;
		case QUERYFLIGHT:
			System.out.println("QUERYFLIGHT received");
			intResponse = tm.queryFlight(request.id,
					request.flightNumber, transactionID);
			break;
		case QUERYCAR:
			System.out.println("QUERYCAR received");
			intResponse = tm.queryCars(request.id,
					request.location, transactionID);
			break;
		case QUERYROOM:
			System.out.println("QUERYROOM received");
			intResponse = tm.queryRooms(request.id,
					request.location, transactionID);
			break;
		case QUERYCUSTOMER:
			System.out.println("QUERYCUSTOMER received");
			// Pretty sure its this method
			stringResponse = tm.queryCustomerInfo(request.id,
					request.customerNumber, transactionID);
			break;
		case QUERYFLIGHTPRICE:
			System.out.println("QUERYFLIGHTPRICE received");
			intResponse = tm.queryFlightPrice(request.id,
					request.flightNumber, transactionID);
			break;
		case QUERYCARPRICE:
			System.out.println("QUERYCARPRICE received");
			intResponse = tm.queryCarsPrice(request.id,
					request.location, transactionID);
			break;
		case QUERYROOMPRICE:
			System.out.println("QUERYROOMPRICE received");
			intResponse = tm.queryRoomsPrice(request.id,
					request.location, transactionID);
			break;
		case RESERVEFLIGHT:
			System.out.println("RESERVEFLIGHT received");
			boolResponse = tm.reserveFlight(request.id,
					request.customerNumber, request.flightNumber, transactionID);
			break;
		case RESERVECAR:
			System.out.println("RESERVECAR received");
			boolResponse = tm.reserveCar(request.id,
					request.customerNumber, request.location, transactionID);
			break;
		case RESERVEROOM:
			System.out.println("RESERVEROOM received");
			boolResponse = tm.reserveRoom(request.id,
					request.customerNumber, request.location, transactionID);
			break;
		case ITINERARY:
			System.out.println("ITINERARY received");
			// This one doesn't work. Fix when messages are passed as JSON
			// aResourceManager.reserveItinerary(request.id, request[2],
			// Integer.parseInt(request[3]), Integer.parseInt(request[4]));
			// outToClient.writeBytes("ITINERARY received");
			break;
		case STARTTXN:
            System.out.println("STARTTXN received");
            intResponse = tm.startTransaction();
            break;
	    case COMMIT:
            System.out.println("COMMIT received");
            boolResponse =  tm.commitTransaction(transactionID);
            break;
	    case ABORT:
            System.out.println("ABORT received");
            boolResponse =  tm.abortTransaction(transactionID);
            break;
	    case SHUTDOWN:
            System.out.println("SHUTDOWN received");
            //TODO: Implement this
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
