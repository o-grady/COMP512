package server;

import shared.IRequestHandler;
import shared.RequestDescriptor;
import shared.RequestType;
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
		if(request.requestType == RequestType.TWOPHASECOMMITVOTERESP){
			if(tm.getStartupVoteResponsesNeeded().contains(request.transactionID)){
				tm.getStartupVoteResponsesNeeded().remove(request.transactionID);
				if(!tm.getStartupVoteResponsesNeeded().isEmpty()){
					//Send request for an element
					System.out.println("TMRequestHandler: Requesting vote result, hung on startup");
					return new ResponseDescriptor(ResponseType.WAITINGFORVOTES, (int) tm.getStartupVoteResponsesNeeded().toArray()[0]);
				}
			}
		}
		if(!tm.getStartupVoteResponsesNeeded().isEmpty()){
			//Send request for an element
			System.out.println("TMRequestHandler: Requesting vote result, hung on startup");
			return new ResponseDescriptor(ResponseType.WAITINGFORVOTES, (int) tm.getStartupVoteResponsesNeeded().toArray()[0]);
		}
		try{
			switch (request.requestType) {
			case NEWFLIGHT:
				System.out.println("TMRequestHandler: NEWFLIGHT received");
				boolResponse = tm.addFlight(request.transactionID,
						request.flightNumber, request.numSeats, request.price);
				break;
			case NEWCAR:
				System.out.println("TMRequestHandler: NEWCAR received");
				boolResponse = tm.addCars(request.transactionID,
						request.location, request.numCars, request.price);
				break;
			case NEWROOM:
				System.out.println("TMRequestHandler: NEWROOM received");
				boolResponse = tm.addRooms(request.transactionID,
						request.location, request.numRooms, request.price);
				break;
			case NEWCUSTOMER:
				System.out.println("TMRequestHandler: NEWCUSTOMER received");
				intResponse = tm.newCustomer(request.transactionID);
				break;
			case NEWCUSTOMERID:
				System.out.println("TMRequestHandler: NEWCUSTOMERID received");
				boolResponse = tm.newCustomerId(request.transactionID,
						request.customerNumber);
				break;
			case DELETEFLIGHT:
				System.out.println("TMRequestHandler: DELETEFLIGHT received");
				boolResponse = tm.deleteFlight(request.transactionID,
						request.flightNumber);
				break;
			case DELETECAR:
				System.out.println("TMRequestHandler: DELETECAR received");
				boolResponse = tm.deleteCars(request.transactionID,
						request.location);
				break;
			case DELETEROOM:
				System.out.println("TMRequestHandler: DELETEROOM received");
				boolResponse = tm.deleteRooms(request.transactionID,
						request.location);
				break;
			case DELETECUSTOMER:
				System.out.println("TMRequestHandler: DELETECUSTOMER received");
				boolResponse = tm.deleteCustomer(request.transactionID,
						request.customerNumber);
				break;
			case QUERYFLIGHT:
				System.out.println("TMRequestHandler: QUERYFLIGHT received");
				intResponse = tm.queryFlight(request.transactionID,
						request.flightNumber);
				break;
			case QUERYCAR:
				System.out.println("TMRequestHandler: QUERYCAR received");
				intResponse = tm.queryCars(request.transactionID,
						request.location);
				break;
			case QUERYROOM:
				System.out.println("TMRequestHandler: QUERYROOM received");
				intResponse = tm.queryRooms(request.transactionID,
						request.location);
				break;
			case QUERYCUSTOMER:
				System.out.println("TMRequestHandler: QUERYCUSTOMER received");
				// Pretty sure its this method
				stringResponse = tm.queryCustomerInfo(request.transactionID,
						request.customerNumber);
				break;
			case QUERYFLIGHTPRICE:
				System.out.println("TMRequestHandler: QUERYFLIGHTPRICE received");
				intResponse = tm.queryFlightPrice(request.transactionID,
						request.flightNumber);
				break;
			case QUERYCARPRICE:
				System.out.println("TMRequestHandler: QUERYCARPRICE received");
				intResponse = tm.queryCarsPrice(request.transactionID,
						request.location);
				break;
			case QUERYROOMPRICE:
				System.out.println("TMRequestHandler: QUERYROOMPRICE received");
				intResponse = tm.queryRoomsPrice(request.transactionID,
						request.location);
				break;
			case RESERVEFLIGHT:
				System.out.println("TMRequestHandler: RESERVEFLIGHT received");
				boolResponse = tm.reserveFlight(request.transactionID,
						request.customerNumber, request.flightNumber);
				break;
			case RESERVECAR:
				System.out.println("TMRequestHandler: RESERVECAR received");
				boolResponse = tm.reserveCar(request.transactionID,
						request.customerNumber, request.location);
				break;
			case RESERVEROOM:
				System.out.println("TMRequestHandler: RESERVEROOM received");
				boolResponse = tm.reserveRoom(request.transactionID,
						request.customerNumber, request.location);
				break;
			case ITINERARY:
				System.out.println("TMRequestHandler: ITINERARY received");
				// This one doesn't work. Fix when messages are passed as JSON
				// aResourceManager.reserveItinerary(request.id, request[2],
				// Integer.parseInt(request[3]), Integer.parseInt(request[4]));
				// outToClient.writeBytes("ITINERARY received");
				break;
			case TWOPHASECOMMITVOTERESP:
	            System.out.println("TMRequestHandler: TWOPHASECOMMITVOTERESP received");
	            if(request.canCommit){
	            	boolResponse = tm.twoPhaseCommitTransaction(request.transactionID);
	            }else{
	            	//call modified abort
	            	boolResponse = tm.twoPhaseAbortTransaction(request.transactionID);
	            }
	            break;
		    case ABORT:
	            System.out.println("TMRequestHandler: ABORT received");
	            boolResponse =  tm.abortTransaction(request.transactionID);
	            break;
		    case SHUTDOWN:
	            System.out.println("TMRequestHandler: SHUTDOWN received");
	            boolResponse = tm.abortAllActiveTransactions();
	            //shutdown if abortAll worked
	            if(boolResponse){
	            	System.exit(0);
	            }	            
	            break;
		    case ENLIST:
		    	System.out.println("TMRequestHandler: ENLIST received");
		    	intResponse = tm.enlist(request.transactionID);
		    	break;
			case ABORTALL:
				System.out.println("TMRequestHandler: ABORTALL received");
		    	boolResponse = tm.abortAllActiveTransactions();		    	
				break;
			case PREPARE:
				System.out.println("TMRequestHandler: PREPARE received");
		    	boolResponse = tm.prepare(request.transactionID);		    	
				break;
			case SELFDESTRUCT: 
				System.exit(1);
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
		} catch (TransactionBlockingException e) {
			responseType = ResponseType.ERROR;
			stringResponse = "Transaction is blocking in commit phase";
		} catch (NotWaitingForVoteResultException e) {
			responseType = ResponseType.ERROR;
			stringResponse = "Transaction was given a vote response and was not waiting for one";
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
