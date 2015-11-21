package server;

import shared.IRequestHandler;
import shared.RequestDescriptor;
import shared.ResponseDescriptor;
import shared.ResponseType;

public class TMRequestHandler implements IRequestHandler {

	private TransactionManager tm;

	public TMRequestHandler(TransactionManager tm) {
		this.tm = tm;
	}

	public ResponseDescriptor handleRequest(RequestDescriptor request) {
		int intResponse = -1;
		boolean boolResponse = false;
		String stringResponse = null;
		ResponseType responseType = null;
		try{
			switch (request.requestType) {
			case NEWFLIGHT:
				System.out.println("NEWFLIGHT received");
				boolResponse = tm.addFlight(request.transactionID,
						request.flightNumber, request.numSeats, request.price);
				break;
			case NEWCAR:
				System.out.println("NEWCAR received");
				boolResponse = tm.addCars(request.transactionID,
						request.location, request.numCars, request.price);
				break;
			case NEWROOM:
				System.out.println("NEWROOM received");
				boolResponse = tm.addRooms(request.transactionID,
						request.location, request.numRooms, request.price);
				break;
			case NEWCUSTOMER:
				System.out.println("NEWCUSTOMER received");
				intResponse = tm.newCustomer(request.transactionID);
				break;
			case NEWCUSTOMERID:
				System.out.println("NEWCUSTOMERID received");
				boolResponse = tm.newCustomerId(request.transactionID,
						request.customerNumber);
				break;
			case DELETEFLIGHT:
				System.out.println("DELETEFLIGHT received");
				boolResponse = tm.deleteFlight(request.transactionID,
						request.flightNumber);
				break;
			case DELETECAR:
				System.out.println("DELETECAR received");
				boolResponse = tm.deleteCars(request.transactionID,
						request.location);
				break;
			case DELETEROOM:
				System.out.println("DELETEROOM received");
				boolResponse = tm.deleteRooms(request.transactionID,
						request.location);
				break;
			case DELETECUSTOMER:
				System.out.println("DELETECUSTOMER received");
				boolResponse = tm.deleteCustomer(request.transactionID,
						request.customerNumber);
				break;
			case QUERYFLIGHT:
				System.out.println("QUERYFLIGHT received");
				intResponse = tm.queryFlight(request.transactionID,
						request.flightNumber);
				break;
			case QUERYCAR:
				System.out.println("QUERYCAR received");
				intResponse = tm.queryCars(request.transactionID,
						request.location);
				break;
			case QUERYROOM:
				System.out.println("QUERYROOM received");
				intResponse = tm.queryRooms(request.transactionID,
						request.location);
				break;
			case QUERYCUSTOMER:
				System.out.println("QUERYCUSTOMER received");
				// Pretty sure its this method
				stringResponse = tm.queryCustomerInfo(request.transactionID,
						request.customerNumber);
				break;
			case QUERYFLIGHTPRICE:
				System.out.println("QUERYFLIGHTPRICE received");
				intResponse = tm.queryFlightPrice(request.transactionID,
						request.flightNumber);
				break;
			case QUERYCARPRICE:
				System.out.println("QUERYCARPRICE received");
				intResponse = tm.queryCarsPrice(request.transactionID,
						request.location);
				break;
			case QUERYROOMPRICE:
				System.out.println("QUERYROOMPRICE received");
				intResponse = tm.queryRoomsPrice(request.transactionID,
						request.location);
				break;
			case RESERVEFLIGHT:
				System.out.println("RESERVEFLIGHT received");
				boolResponse = tm.reserveFlight(request.transactionID,
						request.customerNumber, request.flightNumber);
				break;
			case RESERVECAR:
				System.out.println("RESERVECAR received");
				boolResponse = tm.reserveCar(request.transactionID,
						request.customerNumber, request.location);
				break;
			case RESERVEROOM:
				System.out.println("RESERVEROOM received");
				boolResponse = tm.reserveRoom(request.transactionID,
						request.customerNumber, request.location);
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
	            boolResponse =  tm.commitTransaction(request.transactionID);
	            break;
		    case ABORT:
	            System.out.println("ABORT received");
	            boolResponse =  tm.abortTransaction(request.transactionID);
	            break;
		    case SHUTDOWN:
	            System.out.println("SHUTDOWN received");
	            boolResponse = tm.abortAllActiveTransactions();
	            //shutdown if abortAll worked
	            if(boolResponse){
	            	System.exit(0);
	            }	            
	            break;
		    case ENLIST:
		    	System.out.println("ENLIST received");
		    	intResponse = tm.enlist(request.transactionID);
		    	break;
			case ABORTALL:
				System.out.println("ABORTALL received");
		    	boolResponse = tm.abortAllActiveTransactions();		    	
				break;
			case PREPARE:
				System.out.println("PREPARE received");
		    	boolResponse = tm.prepare(request.transactionID);		    	
				break;
			default:
				break;
			}
		//Transaction problems are returned in stringResponse
		} catch (TransactionNotActiveException e){
			responseType = ResponseType.ERROR;
			stringResponse = "TransactionNotActive";
		} catch (AbortedTransactionException e) {
			responseType = ResponseType.ABORT;
			stringResponse = "AbortedTransaction";
		}
		if (responseType != null) {
			return new ResponseDescriptor(responseType, stringResponse);
		}
		else if(stringResponse != null){
			return new ResponseDescriptor(stringResponse);
		}else if(intResponse != -1){
			return new ResponseDescriptor(intResponse);
		}else{
			return new ResponseDescriptor(boolResponse);
		}

	}

}
