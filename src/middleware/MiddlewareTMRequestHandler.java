package middleware;

import java.util.HashMap;
import java.util.Map;

import server.AbortedTransactionException;
import server.TMRequestHandler;
import server.TransactionManager;
import server.TransactionNotActiveException;
import shared.IRequestHandler;
import shared.RequestDescriptor;
import shared.RequestType;
import shared.ResponseDescriptor;
import shared.ServerConnection;

public class MiddlewareTMRequestHandler implements IRequestHandler {

	class TransactionDescriptor {
		public boolean roomStarted = false;
		public boolean carStarted = false;
		public boolean flightStarted = false;
	}
	
	private ConnectionManager cm;
	private TransactionManager tm;
	private TMRequestHandler rh;
	private Map<Integer, TransactionDescriptor> activeTxns;
	
	public MiddlewareTMRequestHandler(ConnectionManager cm, TransactionManager tm, TMRequestHandler rh) {
		this.cm = cm;
		this.tm = tm;
		this.rh = rh;
		this.activeTxns = new HashMap<Integer, TransactionDescriptor>();
	}

	@Override
	public ResponseDescriptor handleRequest(RequestDescriptor request) {
		int intResponse = -1;
		boolean boolResponse = false;
		String stringResponse = null;
		ServerMode mode = null;
		int transactionID = request.transactionID;
		try{
			if (!activeTxns.containsKey(transactionID) && 
					request.requestType != RequestType.ABORTALL && 
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
			case STARTTXN:
	            System.out.println("STARTTXN received");
	            intResponse = tm.startTransaction();
	            activeTxns.put(intResponse, new TransactionDescriptor());
	            break;
		    case COMMIT:
	            System.out.println("COMMIT received");
	            boolResponse = tm.commitTransaction(transactionID);
	            if (activeTxns.get(transactionID).carStarted) {
	            	ServerConnection sc = cm.getConnection(ServerMode.CAR);
	            	ResponseDescriptor rd = sc.sendRequest(request);
	            	boolResponse = boolResponse && rd.booleanResponse; 
	            }
	            if (activeTxns.get(transactionID).flightStarted) {
	            	ServerConnection sc = cm.getConnection(ServerMode.PLANE);
	            	ResponseDescriptor rd = sc.sendRequest(request);
	            	boolResponse = boolResponse && rd.booleanResponse; 
	            }
	            if (activeTxns.get(transactionID).roomStarted) {
	            	ServerConnection sc = cm.getConnection(ServerMode.ROOM);
	            	ResponseDescriptor rd = sc.sendRequest(request);
	            	boolResponse = boolResponse && rd.booleanResponse; 
	            }
	            activeTxns.remove(transactionID);
	            break;
		    case ABORT:
	            System.out.println("ABORT received");
	            boolResponse = tm.abortTransaction(transactionID);
	            if (activeTxns.get(transactionID).carStarted) {
	            	ServerConnection sc = cm.getConnection(ServerMode.CAR);
	            	ResponseDescriptor rd = sc.sendRequest(request);
	            	boolResponse = boolResponse && rd.booleanResponse; 
	            }
	            if (activeTxns.get(transactionID).flightStarted) {
	            	ServerConnection sc = cm.getConnection(ServerMode.PLANE);
	            	ResponseDescriptor rd = sc.sendRequest(request);
	            	boolResponse = boolResponse && rd.booleanResponse; 
	            }
	            if (activeTxns.get(transactionID).roomStarted) {
	            	ServerConnection sc = cm.getConnection(ServerMode.ROOM);
	            	ResponseDescriptor rd = sc.sendRequest(request);
	            	boolResponse = boolResponse && rd.booleanResponse; 
	            }
	            activeTxns.remove(transactionID);
	            break;
		    case SHUTDOWN:
	            System.out.println("SHUTDOWN received");
	            boolResponse = tm.abortAllActiveTransactions();
				for( ServerConnection connection : cm.getAllConnections()){
					try {
						connection.sendRequest(request);
					} catch (java.net.SocketException ex) {
						// eat the exception, this is expected we are shutting them down
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
			default:
				break;
			}
			
			if (mode == ServerMode.CUSTOMER) {
				if (request.requestType == RequestType.QUERYCUSTOMER){
					return this.queryCustomer(request);
				} else {
					ResponseDescriptor rd = rh.handleRequest(request);
					// enlist the RM if not already started
					this.checkAndEnlistAll(transactionID);
					// check for abort condition
					if (rd.stringResponse != null && (rd.stringResponse.equals("TransactionNotActive") || rd.stringResponse.equals("AbortedTransaction"))) {
						System.out.println("Aborting Transaction " + transactionID);
						RequestDescriptor req = new RequestDescriptor(RequestType.ABORT);
						req.transactionID = transactionID;
						return this.handleRequest(req);
					} else {
						for( ServerConnection connection : cm.getAllConnections()){
							rd = connection.sendRequest(request);
							// check for abort condition
							if (rd.stringResponse != null && (rd.stringResponse.equals("TransactionNotActive") || rd.stringResponse.equals("AbortedTransaction"))) {
								System.out.println("Aborting Transaction " + transactionID);
								RequestDescriptor req = new RequestDescriptor(RequestType.ABORT);
								req.transactionID = transactionID;
								return this.handleRequest(req);
							}
						}
					}
					
					return rh.handleRequest(request);
				}
			} else if (cm.modeIsConnected(mode)) {
				this.checkAndEnlist(mode, transactionID);
				ResponseDescriptor rd = cm.getConnection(mode).sendRequest(request); 
				if (rd.stringResponse != null && (rd.stringResponse.equals("TransactionNotActive") || rd.stringResponse.equals("AbortedTransaction"))) {
					System.out.println("Aborting Transaction " + transactionID);
					RequestDescriptor req = new RequestDescriptor(RequestType.ABORT);
					req.transactionID = transactionID;
					this.handleRequest(req);
				}
				return rd;
			} else if (mode != null) {
				throw new Exception("Server not connected");
			}
			
		//Transaction problems are returned in stringResponse
		} catch (TransactionNotActiveException e){
			stringResponse = "TransactionNotActive";
		} catch (AbortedTransactionException e) {
			stringResponse = "AbortedTransaction";
		} catch (Exception e) {
			stringResponse = e.getMessage();
			e.printStackTrace();
		}
		if(stringResponse != null){
			return new ResponseDescriptor(stringResponse);
		}else if(intResponse != -1){
			return new ResponseDescriptor(intResponse);
		}else{
			return new ResponseDescriptor(boolResponse);
		}
	}
	
	private void checkAndEnlist(ServerMode mode, int transactionID) {
		ServerConnection sc = cm.getConnection(mode);
		RequestDescriptor req = new RequestDescriptor(RequestType.ENLIST);
		req.transactionID = transactionID;
		try {
			if (mode == ServerMode.CAR && !activeTxns.get(transactionID).carStarted) {
				sc.sendRequest(req);
				activeTxns.get(transactionID).carStarted = true;
			}
			if (mode == ServerMode.PLANE && !activeTxns.get(transactionID).flightStarted) {
				sc.sendRequest(req);
				activeTxns.get(transactionID).flightStarted = true;
			}
			if (mode == ServerMode.ROOM && !activeTxns.get(transactionID).roomStarted) {
				sc.sendRequest(req);
				activeTxns.get(transactionID).roomStarted = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void checkAndEnlistAll(int transactionID) {
		RequestDescriptor req = new RequestDescriptor(RequestType.ENLIST);
		req.transactionID = transactionID;
		try {
			if (!activeTxns.get(transactionID).carStarted) {
				ServerConnection sc = cm.getConnection(ServerMode.CAR);
				sc.sendRequest(req);
				activeTxns.get(transactionID).carStarted = true;
			}
			if (!activeTxns.get(transactionID).flightStarted) {
				ServerConnection sc = cm.getConnection(ServerMode.PLANE);
				sc.sendRequest(req);
				activeTxns.get(transactionID).flightStarted = true;
			}
			if (!activeTxns.get(transactionID).roomStarted) {
				ServerConnection sc = cm.getConnection(ServerMode.ROOM);
				sc.sendRequest(req);
				activeTxns.get(transactionID).roomStarted = true;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private ResponseDescriptor queryCustomer(RequestDescriptor request) throws Exception{
		String custInfo = "";
		String startLine = null;
		String endLine = null;
		for( ServerConnection connection : cm.getAllConnections()){
			String resp = connection.sendRequest(request).stringResponse;
			String[] lines = resp.split("\n");
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
			res.booleanResponse = false;
			res.additionalMessage = "No flight numbers requested";
			return res;
		}
		RequestDescriptor req2 = null;
		int customerNumber = request.customerNumber;
		String location = request.location;
		boolean car = request.car;
		boolean room = request.room;
		int transactionID = request.transactionID;
		//Query all flights, return if any are full
		for (int i = 0 ; i < request.flightNumbers.size() ; i++){
			req2 = new RequestDescriptor(RequestType.QUERYFLIGHT);
			req2.transactionID = transactionID;
			req2.flightNumber = request.flightNumbers.elementAt(i);
			ResponseDescriptor rd = this.handleRequest(req2);
			if (rd.intResponse <= 0) {
				res.booleanResponse = false;
				res.additionalMessage = "Flight " + req2.flightNumber + " has no seats remaining";
				RequestDescriptor req = new RequestDescriptor(RequestType.ABORT);
				req.transactionID = transactionID;
				this.handleRequest(req);
				
				return res;
			}
			if (rd.stringResponse != null && (rd.stringResponse.equals("TransactionNotActive") || rd.stringResponse.equals("AbortedTransaction"))) {
				System.out.println("Aborting Transaction " + transactionID);
				RequestDescriptor req = new RequestDescriptor(RequestType.ABORT);
				req.transactionID = transactionID;
				this.handleRequest(req);

				return rd;
			}
		}
		//Query room and car if selected
		if(room){
			req2 = new RequestDescriptor(RequestType.QUERYROOM);
			req2.transactionID = transactionID;
			req2.location = location;
			ResponseDescriptor rd = this.handleRequest(req2);
			if (rd.intResponse <= 0) {
				res.booleanResponse = false;
				res.additionalMessage = "No car available at " + location;
				RequestDescriptor req = new RequestDescriptor(RequestType.ABORT);
				req.transactionID = transactionID;
				this.handleRequest(req);
				
				return res;
			}
			if (rd.stringResponse != null && (rd.stringResponse.equals("TransactionNotActive") || rd.stringResponse.equals("AbortedTransaction"))) {
				System.out.println("Aborting Transaction " + transactionID);
				RequestDescriptor req = new RequestDescriptor(RequestType.ABORT);
				req.transactionID = transactionID;
				this.handleRequest(req);
				
				return rd;
			}
		}
		if(car){
			req2 = new RequestDescriptor(RequestType.QUERYCAR);
			req2.transactionID = transactionID;
			req2.location = location;
			ResponseDescriptor rd = this.handleRequest(req2);
			if (rd.intResponse <= 0) {
				res.booleanResponse = false;
				res.additionalMessage = "No room available at " + location;
				RequestDescriptor req = new RequestDescriptor(RequestType.ABORT);
				req.transactionID = transactionID;
				this.handleRequest(req);
				return res;
			}
			if (rd.stringResponse != null && (rd.stringResponse.equals("TransactionNotActive") || rd.stringResponse.equals("AbortedTransaction"))) {
				System.out.println("Aborting Transaction " + transactionID);
				RequestDescriptor req = new RequestDescriptor(RequestType.ABORT);
				req.transactionID = transactionID;
				this.handleRequest(req);

				return rd;
			}
		}
		//Book flights, room and car
		for (int i = 0 ; i < request.flightNumbers.size() ; i++){
			req2 = new RequestDescriptor(RequestType.RESERVEFLIGHT);
			req2.transactionID = transactionID;
			req2.customerNumber = customerNumber;
			req2.flightNumber = request.flightNumbers.elementAt(i);
			ResponseDescriptor rd = this.handleRequest(req2);
			if (!rd.booleanResponse) {
				res.booleanResponse = false;
				res.additionalMessage = "Problem booking " + req2.flightNumber;
				RequestDescriptor req = new RequestDescriptor(RequestType.ABORT);
				req.transactionID = transactionID;
				this.handleRequest(req);
				return res;
			}
			if (rd.stringResponse != null && (rd.stringResponse.equals("TransactionNotActive") || rd.stringResponse.equals("AbortedTransaction"))) {
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
			if (!rd.booleanResponse ) {
				res.booleanResponse = false;
				res.additionalMessage = "Problem booking car at " + location;
				RequestDescriptor req = new RequestDescriptor(RequestType.ABORT);
				req.transactionID = transactionID;
				this.handleRequest(req);
				return res;
			}
			if (rd.stringResponse != null && (rd.stringResponse.equals("TransactionNotActive") || rd.stringResponse.equals("AbortedTransaction"))) {
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
			if (!rd.booleanResponse) {
				res.booleanResponse = false;
				res.additionalMessage = "Problem booking room at " + location;
				RequestDescriptor req = new RequestDescriptor(RequestType.ABORT);
				req.transactionID = transactionID;
				this.handleRequest(req);
				return res;
			}
			if (!rd.booleanResponse && rd.stringResponse != null && (rd.stringResponse.equals("TransactionNotActive") || rd.stringResponse.equals("AbortedTransaction"))) {
				System.out.println("Aborting Transaction " + transactionID);
				RequestDescriptor req = new RequestDescriptor(RequestType.ABORT);
				req.transactionID = transactionID;
				this.handleRequest(req);

				return rd;
			}
		}
		res.booleanResponse = true;
		res.additionalMessage = "Success!";
		return res;
	}
}
