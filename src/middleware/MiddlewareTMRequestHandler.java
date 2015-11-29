package middleware;

import java.util.Map;

import server.AbortedTransactionException;
import server.TMRequestHandler;
import server.TransactionBlockingException;
import server.TransactionManager;
import server.TransactionNotActiveException;
import shared.CommitLogger;
import shared.CommitLoggerImpl;
import shared.IRequestHandler;
import shared.LogType;
import shared.RequestDescriptor;
import shared.RequestType;
import shared.ResponseDescriptor;
import shared.ResponseType;
import shared.ServerConnection;

public class MiddlewareTMRequestHandler implements IRequestHandler {
	private static final int TWO_PHASE_COMMIT_TIMEOUT = 10000;
	private ConnectionManager cm;
	private MiddlewareActiveTransactionThread activeTxns; 
	private int transactionCounter;
	private CommitLogger logger;
	
	public MiddlewareTMRequestHandler(ConnectionManager cm) {
		this.cm = cm;
		this.activeTxns = new MiddlewareActiveTransactionThread();
		this.activeTxns.start();
		this.logger = new CommitLoggerImpl("middlewareLog.txt");
		this.transactionCounter = this.logger.largestTransactionInLog();
		System.out.println("Transaction Counter Initialized to " + this.transactionCounter);
		for(int i = 0 ; i <= this.transactionCounter ; i++ ){
			if(logger.hasLog(LogType.STARTED, i)){
				if(logger.hasLog(LogType.VOTESTARTED, i)){
					if(logger.hasLog(LogType.COMMITTED, i)){
						if(!logger.hasLog(LogType.DONE, i)){
							//START,VOTESTARTED,COMMITTED
							this.reconnectServers(i);
							//resend commits
							System.out.println("Resending COMMIT vote");
							resend2PhaseResponse(i, true);
						}
					}else if(logger.hasLog(LogType.ABORTED, i)){
						if(!logger.hasLog(LogType.DONE, i)){
							//START,VOTESTARTED,ABORTED
							this.reconnectServers(i);
							//resend abort
							System.out.println("Resending ABORT vote");
							resend2PhaseResponse(i, false);
						}	
					}else{
						if(!logger.hasLog(LogType.DONE, i)){
							//START,VOTESTARTED
							logger.log(LogType.ABORTED, i);
							this.reconnectServers(i);
							System.out.println("Sending ABORT vote");
							resend2PhaseResponse(i, false);
						}
					}
				}else{
					if(logger.hasLog(LogType.ABORTED, i)){
						if(!logger.hasLog(LogType.DONE, i)){
							//START,ABORTED
							this.reconnectServers(i);
							//resend abort (non 2PC vote)
							System.out.println("Sending abort (non 2PC vote)");
							this.abortTransaction(i);
						}	
					}else{
						//START
						this.reconnectServers(i);
						//resend abort (non 2PC vote)
						System.out.println("Sending abort (non 2PC vote)");
						this.abortTransaction(i);
					}
				}
			}
		}
	}
	private boolean resend2PhaseResponse(int transactionID, boolean voteResult){
		RequestDescriptor voteResp = new RequestDescriptor(RequestType.TWOPHASECOMMITVOTERESP);
		voteResp.canCommit = voteResult;
		voteResp.transactionID = transactionID;
		boolean boolResponse = false;
		try {
			boolResponse = sendRequestToStarted(voteResp);
		} catch (Exception e) {
			System.out.println("Problem resending 2PC vote");
			e.printStackTrace();
		}
		activeTxns.remove(transactionID);
		logger.log(LogType.DONE, transactionID);
		System.out.println("resend2PhaseResponse: returning " + boolResponse);
		return boolResponse;	
	}
	private void reconnectServers(int transactionID) {
		activeTxns.add(transactionID);
		if(logger.hasLog(LogType.CARENLISTED, transactionID)){
			activeTxns.get(transactionID).hasStarted.put(ServerMode.CAR, true);
		}
		if(logger.hasLog(LogType.FLIGHTENLISTED, transactionID)){
			activeTxns.get(transactionID).hasStarted.put(ServerMode.FLIGHT, true);
		}
		if(logger.hasLog(LogType.ROOMENLISTED, transactionID)){
			activeTxns.get(transactionID).hasStarted.put(ServerMode.ROOM, true);
		}
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
					request.requestType != RequestType.STARTTXN &&
					request.requestType != RequestType.CRASH) {
				throw new TransactionNotActiveException();
			}
			if (request.requestType == RequestType.ITINERARY) {
				return reserveItinerary(request);
			}
			if(request.requestType == RequestType.CRASH){
				crashServer(request);
			}
			
			switch (request.requestType) {
			case DELETECAR:
			case NEWCAR:
			case QUERYCAR:
			case QUERYCARPRICE:
			case RESERVECAR:
				mode = ServerMode.CAR;
				System.out.println("mode = ServerMode.CAR");
				break;
			case DELETEFLIGHT:
			case NEWFLIGHT:
			case QUERYFLIGHT:
			case QUERYFLIGHTPRICE:
			case RESERVEFLIGHT:
				mode = ServerMode.FLIGHT;
				System.out.println("mode = ServerMode.FLIGHT");
				break;
			case DELETEROOM:
			case NEWROOM:
			case QUERYROOM:
			case QUERYROOMPRICE:
			case RESERVEROOM:
				mode = ServerMode.ROOM;
				System.out.println("mode = ServerMode.ROOM");
				break;
			case DELETECUSTOMER:
			case NEWCUSTOMER:
			case NEWCUSTOMERID:
			case QUERYCUSTOMER:
				mode = ServerMode.CUSTOMER;
				System.out.println("mode = ServerMode.CUSTOMER");
				break;
			case STARTTXN:
	            System.out.println("STARTTXN received");
	            intResponse = this.startTransaction();
	            break;
		    case COMMIT:
	            System.out.println("COMMIT received");
	            boolResponse = twoPhaseCommit(transactionID);
	            break;
		    case ABORT:
	            System.out.println("ABORT received");
	            logger.log(LogType.ABORTED, transactionID);
	            boolResponse = this.sendRequestToStarted(request);
	            if(!logger.hasLog(LogType.DONE, transactionID)){
	            	logger.log(LogType.DONE, transactionID);
	            }
	            activeTxns.remove(transactionID);
	            break;
		    case SHUTDOWN:
	            System.out.println("SHUTDOWN received");
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
			default:
				break;
			}
			
			// handle customer related requests
			if (mode == ServerMode.CUSTOMER) {
				if (request.requestType == RequestType.QUERYCUSTOMER){
					return this.queryCustomer(request);
				} else {
					ResponseDescriptor rd = null;
					// enlist the RMs if not already started
					this.checkAndEnlistAll(transactionID);
					// bounce the request out to all RMs
					boolean needToResend = false;
					for(ServerConnection connection : cm.getAllConnections()) {
						rd = connection.sendRequest(request);
						// check for abort condition
						if (rd.responseType == ResponseType.ABORT || rd.responseType == ResponseType.ERROR) {
							this.abortTransaction(transactionID);
							return rd;
						}else if(rd.responseType == ResponseType.WAITINGFORVOTES){
							needToResend = true;
							System.out.println("Got a WAITINGFORVOTES resp");
							RequestDescriptor req = new RequestDescriptor(RequestType.TWOPHASECOMMITVOTERESP);
							req.transactionID = (int) rd.data;
							if(logger.hasLog(LogType.COMMITTED, req.transactionID)){
								req.canCommit = true;
							}else if(logger.hasLog(LogType.ABORTED, req.transactionID)){
								req.canCommit = false;
							}
							connection.sendRequest(req);
						}
					}
					if(needToResend){
						return this.handleRequest(request);
					}else{
						return rd;
					}
				}
			} else if (cm.modeIsConnected(mode)) {
				this.checkAndEnlist(mode, transactionID);
				ResponseDescriptor rd = cm.getConnection(mode).sendRequest(request); 
				if (rd.responseType == ResponseType.ABORT || rd.responseType == ResponseType.ERROR) {
					this.abortTransaction(transactionID);
					return rd;
				}else if(rd.responseType == ResponseType.WAITINGFORVOTES){
					System.out.println("Got a WAITINGFORVOTES resp");
					RequestDescriptor req = new RequestDescriptor(RequestType.TWOPHASECOMMITVOTERESP);
					req.transactionID = (int) rd.data;
					if(logger.hasLog(LogType.COMMITTED, req.transactionID)){
						req.canCommit = true;
					}else if(logger.hasLog(LogType.ABORTED, req.transactionID)){
						req.canCommit = false;
					}
					cm.getConnection(mode).sendRequest(req);
					return this.handleRequest(request);
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

	private boolean twoPhaseCommit(int transactionID) throws Exception {
		logger.log(LogType.VOTESTARTED, transactionID);
		boolean boolResponse;
		boolean voteResult = twoPhaseCommitVoteRequest(transactionID);
		System.out.println("Vote result = " + voteResult);
		if(voteResult){
			logger.log(LogType.COMMITTED, transactionID);
		}else{
			logger.log(LogType.ABORTED, transactionID);
		}
		RequestDescriptor voteResp = new RequestDescriptor(RequestType.TWOPHASECOMMITVOTERESP);
		voteResp.canCommit = voteResult;
		voteResp.transactionID = transactionID;
		boolResponse = sendRequestToStarted(voteResp);
		activeTxns.remove(transactionID);
		logger.log(LogType.DONE, transactionID);
		return boolResponse;
	}

	private int startTransaction() {
		transactionCounter++;
		logger.log(LogType.STARTED, transactionCounter);
	    activeTxns.add(transactionCounter);
		return transactionCounter;
	}

	private void crashServer(RequestDescriptor request) throws Exception {
		if(request.serverToCrash == null){
			throw new Exception();
		}
		System.out.println("CRASH received");
		ServerMode toCrash = request.serverToCrash;
		RequestDescriptor selfDestructMessage = new RequestDescriptor(RequestType.SELFDESTRUCT);
		//using customer for middleware here
		if(toCrash == ServerMode.CUSTOMER){
			System.exit(1);
		}else{
			if(cm.modeIsConnected(toCrash)){
				cm.getConnection(toCrash).sendRequest(selfDestructMessage);
			}
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
				if(mode == ServerMode.CAR){
					logger.log(LogType.CARENLISTED, transactionID);
				}else if(mode == ServerMode.FLIGHT){
					logger.log(LogType.FLIGHTENLISTED, transactionID);
				}else if(mode == ServerMode.ROOM){
					logger.log(LogType.ROOMENLISTED, transactionID);
				}
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
					if(mode == ServerMode.CAR){
						logger.log(LogType.CARENLISTED, transactionID);
					}else if(mode == ServerMode.FLIGHT){
						logger.log(LogType.FLIGHTENLISTED, transactionID);
					}else if(mode == ServerMode.ROOM){
						logger.log(LogType.ROOMENLISTED, transactionID);
					}
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
	private boolean twoPhaseCommitVoteRequest(int transactionID){
		System.out.println("Starting 2PC voting");
		Map<ServerMode, Boolean> serversUsed = this.activeTxns.get(transactionID).hasStarted;
		RequestDescriptor request = null;
		for( ServerMode mode : ServerMode.values() ){
			if(serversUsed.get(mode)){
				request = new RequestDescriptor(RequestType.PREPARE);
				request.transactionID = transactionID;
				try {
					System.out.println("Sending vote request to " + mode.toString());
					boolean dataReceived = false;
					boolean vote = false;
					try{
						ResponseDescriptor rd = cm.getConnection(mode).sendRequestWithTimeOut(request, TWO_PHASE_COMMIT_TIMEOUT);
						System.out.println("Data Recieved: " + rd.data.toString());
						vote = (boolean) rd.data; 
						dataReceived = true;
					}catch(java.net.SocketException e){}
					
					//if it times out / dies and hasn't received a vote 
					if(!dataReceived){
						return false;
					}
					if(vote == false){
						return false;
					}
				} catch (Exception e) {
					System.out.println("Error in twoPhaseCommitVoteRequest");
					e.printStackTrace();
					//dont think this should happen, abort just in case?
					return false;
				} 
			}
		}
		//No one voted no, so return true
		return true;
	}
}
