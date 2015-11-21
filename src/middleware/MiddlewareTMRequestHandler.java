package middleware;

import server.AbortedTransactionException;
import server.TMRequestHandler;
import server.TransactionManager;
import server.TransactionNotActiveException;
import shared.IRequestHandler;
import shared.RequestDescriptor;
import shared.RequestType;
import shared.ResponseDescriptor;
import shared.ResponseType;
import shared.ServerConnection;

public class MiddlewareTMRequestHandler implements IRequestHandler {
	private ConnectionManager cm;
	private TransactionManager tm;
	private TMRequestHandler rh;
	private MiddlewareActiveTransactionThread activeTxns; 
	
	public MiddlewareTMRequestHandler(ConnectionManager cm, TransactionManager tm, TMRequestHandler rh) {
		this.cm = cm;
		this.tm = tm;
		this.rh = rh;
		this.activeTxns = new MiddlewareActiveTransactionThread();
		this.activeTxns.start();
	}

	@Override
	public ResponseDescriptor handleRequest(RequestDescriptor request) {
		int intResponse = -1;
		boolean boolResponse = false;
		String stringResponse = null;
		ServerMode mode = null;
		ResponseType responseType = null;
		int transactionID = request.transactionID;
		try{
			if (activeTxns.contains(transactionID)) {
				this.signalRMKeepAlive(transactionID);
				activeTxns.get(transactionID).lastActive = System.currentTimeMillis();
			} else if (request.requestType != RequestType.ABORTALL && 
					request.requestType != RequestType.SHUTDOWN && 
					request.requestType != RequestType.STARTTXN) {
				throw new TransactionNotActiveException();
			}
			if (request.requestType == RequestType.ITINERARY) {
				return reserveItinerary(request);
			}
			
			switch (request.requestType) {
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
	            System.out.println("STARTTXN received");
	            intResponse = tm.startTransaction();
	            activeTxns.add(intResponse);
	            break;
		    case COMMIT:
		    	//TODO: This needs to implement 2PC instead of 1 phase like this
	            System.out.println("COMMIT received");
	            boolResponse = tm.commitTransaction(transactionID);
	            
	            boolResponse = boolResponse && this.sendRequestToStarted(request);

	            activeTxns.remove(transactionID);
	            break;
		    case ABORT:
	            System.out.println("ABORT received");
	            boolResponse = tm.abortTransaction(transactionID);
	            
	            boolResponse = boolResponse && this.sendRequestToStarted(request);
	            
	            activeTxns.remove(transactionID);
	            break;
		    case SHUTDOWN:
	            System.out.println("SHUTDOWN received");
	            boolResponse = tm.abortAllActiveTransactions();
				for( ServerConnection connection : cm.getAllConnections()){
					try {
						connection.sendRequest(request);
					} catch (java.net.SocketException ex) {
						// eat the exception, this is expected as we are shutting them down
					} catch (Exception ex) {
						// This is unexpected, set boolresponse to false...
						boolResponse = false;
					}
				}
	            //shutdown if abortAll worked
	            if(boolResponse){
	            	System.exit(0);
	            } else {
	            	System.exit(-1);
	            }
	            
	            break;
		    case ENLIST:
		    	// This is used to signal the local TM ONLY, other TMs are enlisted on an as needed basis
		    	System.out.println("ENLIST received");
		    	this.tm.enlist(transactionID);
			default:
				break;
			}
			
			// handle customer related requests
			if (mode == ServerMode.CUSTOMER) {
				if (request.requestType == RequestType.QUERYCUSTOMER){
					return this.queryCustomer(request);
				} else {
					// run the customer request on the local RM
					ResponseDescriptor rd = rh.handleRequest(request);
					// check for abort condition
					if (rd.responseType == ResponseType.ABORT || rd.responseType == ResponseType.ERROR) {
						this.abortTransaction(transactionID);
						return rd;
					} else {
						// enlist the RMs if not already started
						this.checkAndEnlistAll(transactionID);
						// bounce the request out to all RMs
						for(ServerConnection connection : cm.getAllConnections()) {
							rd = connection.sendRequest(request);
							// check for abort condition
							if (rd.responseType == ResponseType.ABORT || rd.responseType == ResponseType.ERROR) {
								this.abortTransaction(transactionID);
								return rd;
							}
						}
					}
					
					return rd;
				}
			} else if (cm.modeIsConnected(mode)) {
				this.checkAndEnlist(mode, transactionID);
				ResponseDescriptor rd = cm.getConnection(mode).sendRequest(request); 
				if (rd.responseType == ResponseType.ABORT || rd.responseType == ResponseType.ERROR) {
					this.abortTransaction(transactionID);
					return rd;
				}
				return rd;
			} else if (mode != null) {
				throw new Exception("Server not connected");
			}
			
		//Transaction problems are returned in stringResponse
		} catch (TransactionNotActiveException e){
			responseType = ResponseType.ERROR;
			stringResponse = "TransactionNotActive";
		} catch (AbortedTransactionException e) {
			responseType = ResponseType.ABORT;
			stringResponse = "AbortedTransaction";
		} catch (Exception e) {
			responseType = ResponseType.ERROR;
			stringResponse = e.getMessage();
			e.printStackTrace();
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
	
	private void abortTransaction(int transactionID) {
		RequestDescriptor req = new RequestDescriptor(RequestType.ABORT);
		req.transactionID = transactionID;
		this.handleRequest(req);
	}
	
	private boolean sendRequestToStarted(RequestDescriptor request) throws Exception {
		boolean boolResponse = true;
		
        for (ServerMode sm : ServerMode.values()) {
            if (activeTxns.get(request.transactionID).hasStarted.get(sm)) {
            	ServerConnection sc = cm.getConnection(sm);
            	ResponseDescriptor rd = sc.sendRequest(request);
            	if (rd.responseType == ResponseType.BOOLEAN) {
            		boolResponse = boolResponse && (boolean) rd.data; 
            	} else if (rd.responseType == ResponseType.ABORT || rd.responseType == ResponseType.ERROR) {
            		boolResponse = false;
            	}
            	
            }
        }
		return boolResponse;
	}
	
	private void checkAndEnlist(ServerMode mode, int transactionID) {
		ServerConnection sc = cm.getConnection(mode);
		RequestDescriptor req = new RequestDescriptor(RequestType.ENLIST);
		req.transactionID = transactionID;
		try {
			if (!activeTxns.get(transactionID).hasStarted.get(mode)) {
				sc.sendRequest(req);
				activeTxns.get(transactionID).hasStarted.put(mode, true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void checkAndEnlistAll(int transactionID) {
		RequestDescriptor req = new RequestDescriptor(RequestType.ENLIST);
		req.transactionID = transactionID;
		try {
	        for (ServerMode mode : ServerMode.values()) {
	            if (mode != ServerMode.CUSTOMER && !activeTxns.get(transactionID).hasStarted.get(mode)) {
	            	ServerConnection sc = cm.getConnection(mode);
	            	sc.sendRequest(req);
	            	activeTxns.get(transactionID).hasStarted.put(mode, true);
	            }
	        }
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private void signalRMKeepAlive(int transactionID) {
		RequestDescriptor req = new RequestDescriptor(RequestType.ENLIST);
		req.transactionID = transactionID;
		try {
			this.tm.enlist(transactionID);
			this.sendRequestToStarted(req);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private ResponseDescriptor queryCustomer(RequestDescriptor request) throws Exception{
		String custInfo = "";
		String startLine = null;
		String endLine = null;
		for( ServerConnection connection : cm.getAllConnections() ){
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
		ResponseDescriptor res = new ResponseDescriptor();
		//return if no flights
		if(request.flightNumbers == null || request.flightNumbers.isEmpty()){
			res.data = false;
			res.additionalMessage = "No flight numbers requested";
			return res;
		}
		RequestDescriptor req2 = null;
		int customerNumber = request.customerNumber;
		String location = request.location;
		boolean car = request.car;
		boolean room = request.room;
		int transactionID = request.transactionID;

		//Book flights, room and car
		for (int i = 0 ; i < request.flightNumbers.size() ; i++){
			req2 = new RequestDescriptor(RequestType.RESERVEFLIGHT);
			req2.transactionID = transactionID;
			req2.customerNumber = customerNumber;
			req2.flightNumber = request.flightNumbers.elementAt(i);
			ResponseDescriptor rd = this.handleRequest(req2);
			if (rd.responseType == ResponseType.BOOLEAN && !(boolean)rd.data) {
				res.data = false;
				res.additionalMessage = "Problem booking " + req2.flightNumber;
				RequestDescriptor req = new RequestDescriptor(RequestType.ABORT);
				req.transactionID = transactionID;
				this.handleRequest(req);
				return res;
			}
			if (rd.responseType == ResponseType.ERROR || rd.responseType == ResponseType.ABORT) {
				System.out.println("Aborting Transaction " + transactionID);
				RequestDescriptor req = new RequestDescriptor(RequestType.ABORT);
				req.transactionID = transactionID;
				this.handleRequest(req);

				return rd;
			}
			
		}
		if(car){
			req2 = new RequestDescriptor(RequestType.RESERVECAR);
			req2.transactionID = transactionID;
			req2.customerNumber = customerNumber;
			req2.location = location;
			ResponseDescriptor rd = this.handleRequest(req2);
			if (rd.responseType == ResponseType.BOOLEAN && !(boolean)rd.data) {
				res.data = false;
				res.additionalMessage = "Problem booking car at " + location;
				RequestDescriptor req = new RequestDescriptor(RequestType.ABORT);
				req.transactionID = transactionID;
				this.handleRequest(req);
				return res;
			}
			if (rd.responseType == ResponseType.ERROR || rd.responseType == ResponseType.ABORT) {
				System.out.println("Aborting Transaction " + transactionID);
				RequestDescriptor req = new RequestDescriptor(RequestType.ABORT);
				req.transactionID = transactionID;
				this.handleRequest(req);

				return rd;
			}
		}
		if(room){
			req2 = new RequestDescriptor(RequestType.RESERVEROOM);
			req2.transactionID = transactionID;
			req2.customerNumber = customerNumber;
			req2.location = location;
			ResponseDescriptor rd = this.handleRequest(req2);
			if (rd.responseType == ResponseType.BOOLEAN && !(boolean)rd.data) {
				res.data = false;
				res.additionalMessage = "Problem booking room at " + location;
				RequestDescriptor req = new RequestDescriptor(RequestType.ABORT);
				req.transactionID = transactionID;
				this.handleRequest(req);
				return res;
			}
			if (rd.responseType == ResponseType.ERROR || rd.responseType == ResponseType.ABORT) {
				System.out.println("Aborting Transaction " + transactionID);
				RequestDescriptor req = new RequestDescriptor(RequestType.ABORT);
				req.transactionID = transactionID;
				this.handleRequest(req);

				return rd;
			}
		}
		res.responseType = ResponseType.BOOLEAN;
		res.data = true;
		res.additionalMessage = "Success!";
		return res;
	}
}
