package middleware;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import server.AbortedTransactionException;
import server.RMCrashLocations;
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
	private static final String LOG_NAME = "middlewareLog.txt";
	private static final String CRASH_FILE_NAME = "crash.txt";
	private static final int TWO_PHASE_COMMIT_TIMEOUT = 10000;
	private ConnectionManager cm;
	private MiddlewareActiveTransactionThread activeTxns; 
	private int transactionCounter;
	private CommitLogger logger;
	private Set<MWCrashLocations> whereToCrash = new HashSet<MWCrashLocations>();
	public MiddlewareTMRequestHandler(ConnectionManager cm) {
		this.cm = cm;
		this.activeTxns = new MiddlewareActiveTransactionThread();
		this.activeTxns.start();
		//Set up intended crashing
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(CRASH_FILE_NAME));
		} catch (FileNotFoundException e) {
			System.out.println("MiddlewareTMRequestHandler: No crash.txt found");
		}
		String whereToCrash = null;
		if(br != null){
			try {
				if((whereToCrash = br.readLine()) != null){
					if(MWCrashLocations.valueOf(whereToCrash) != null){
						this.whereToCrash.add(MWCrashLocations.valueOf(whereToCrash));
					}
				}
				br.close();	
			} catch (IOException e) {
				System.out.println("MiddlewareTMRequestHandler: IOException when reading crash.txt");
			}
		}
		this.logger = new CommitLoggerImpl(LOG_NAME);
		this.transactionCounter = this.logger.largestTransactionInLog();
		System.out.println("MiddlewareTMRequestHandler: Transaction Counter initialized to " + this.transactionCounter);
		for(int i = 0 ; i <= this.transactionCounter ; i++ ){
			if(logger.hasLog(LogType.STARTED, i)){
				if(logger.hasLog(LogType.VOTESTARTED, i)){
					if(logger.hasLog(LogType.COMMITTED, i)){
						if(!logger.hasLog(LogType.DONE, i)){
							//START,VOTESTARTED,COMMITTED
							this.reconnectServers(i);
							//resend commits
							System.out.println("MiddlewareTMRequestHandler: Resending COMMIT vote");
							resend2PhaseResponse(i, true);
						}
					}else if(logger.hasLog(LogType.ABORTED, i)){
						if(!logger.hasLog(LogType.DONE, i)){
							//START,VOTESTARTED,ABORTED
							this.reconnectServers(i);
							//resend abort
							System.out.println("MiddlewareTMRequestHandler: Resending ABORT vote");
							resend2PhaseResponse(i, false);
						}	
					}else{
						if(!logger.hasLog(LogType.DONE, i)){
							//START,VOTESTARTED
							logger.log(LogType.ABORTED, i);
							this.reconnectServers(i);
							System.out.println("MiddlewareTMRequestHandler: Sending ABORT vote");
							resend2PhaseResponse(i, false);
						}
					}
				}else{
					if(logger.hasLog(LogType.ABORTED, i)){
						if(!logger.hasLog(LogType.DONE, i)){
							//START,ABORTED
							this.reconnectServers(i);
							//resend abort (non 2PC vote)
							System.out.println("MiddlewareTMRequestHandler: Sending abort (non 2PC vote)");
							this.abortTransaction(i);
						}	
					}else{
						//START
						this.reconnectServers(i);
						//resend abort (non 2PC vote)
						System.out.println("MiddlewareTMRequestHandler: Sending abort (non 2PC vote)");
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
			System.out.println("MiddlewareTMRequestHandler: Problem resending 2PC vote");
			e.printStackTrace();
		}
		activeTxns.remove(transactionID);
		logger.log(LogType.DONE, transactionID);
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
	            System.out.println("MiddlewareTMRequestHandler: STARTTXN received");
	            intResponse = this.startTransaction();
	            break;
		    case COMMIT:
	            System.out.println("MiddlewareTMRequestHandler: COMMIT received");
	            boolResponse = twoPhaseCommit(transactionID);
	            break;
		    case ABORT:
	            System.out.println("MiddlewareTMRequestHandler: ABORT received");
	            logger.log(LogType.ABORTED, transactionID);
	            boolResponse = this.sendRequestToStarted(request);
	            if(!logger.hasLog(LogType.DONE, transactionID)){
	            	logger.log(LogType.DONE, transactionID);
	            }
	            activeTxns.remove(transactionID);
	            break;
		    case SHUTDOWN:
	            System.out.println("MiddlewareTMRequestHandler: SHUTDOWN received");
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
					System.out.println("MiddlewareTMRequestHandler: QUERYCUSTOMER received");
					return this.queryCustomer(request);
				} else {
					System.out.println("MiddlewareTMRequestHandler: "+ request.requestType + " received");
					boolean seenNewCustomer = false;
					int customerId = 0;
					if(request.requestType == RequestType.NEWCUSTOMER){
						seenNewCustomer = true;
				        customerId = Integer.parseInt(String.valueOf(request.transactionID) +
				                String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
				                String.valueOf(Math.round(Math.random() * 100 + 1)));
				        request.requestType = RequestType.NEWCUSTOMERID;
				        request.customerNumber = customerId;
				        
					}
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
							resendVote((int) rd.data, connection);
						}
					}
					if(needToResend){
						return this.handleRequest(request);
					}else{
						if(seenNewCustomer){
							return new ResponseDescriptor(customerId);
						}
						return rd;
					}
				}
			} else if (cm.modeIsConnected(mode)) {
				System.out.println("MiddlewareTMRequestHandler: "+ request.requestType + " received");
				this.checkAndEnlist(mode, transactionID);
				ResponseDescriptor rd = cm.getConnection(mode).sendRequest(request); 
				if (rd.responseType == ResponseType.ABORT || rd.responseType == ResponseType.ERROR) {
					this.abortTransaction(transactionID);
					return rd;
				}else if(rd.responseType == ResponseType.WAITINGFORVOTES){
					resendVote((int) rd.data, cm.getConnection(mode));
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
	private void resendVote(int data, ServerConnection serverConnection) throws Exception {
		System.out.println("MiddlewareTMRequestHandler: Got a WAITINGFORVOTES resp");
		RequestDescriptor req = new RequestDescriptor(RequestType.TWOPHASECOMMITVOTERESP);
		req.transactionID = data;
		if(logger.hasLog(LogType.COMMITTED, req.transactionID)){
			req.canCommit = true;
		}else if(logger.hasLog(LogType.ABORTED, req.transactionID)){
			req.canCommit = false;
		}
		serverConnection.sendRequest(req);
	}
	private boolean twoPhaseCommit(int transactionID) throws Exception {
		crashIfRequested(MWCrashLocations.BEFOREVOTESTARTED);
		logger.log(LogType.VOTESTARTED, transactionID);
		crashIfRequested(MWCrashLocations.AFTERVOTESTARTED);
		boolean voteResult = twoPhaseCommitVoteRequest(transactionID);
		if(voteResult){
			logger.log(LogType.COMMITTED, transactionID);
		}else{
			logger.log(LogType.ABORTED, transactionID);
		}
		RequestDescriptor voteResp = new RequestDescriptor(RequestType.TWOPHASECOMMITVOTERESP);
		voteResp.canCommit = voteResult;
		voteResp.transactionID = transactionID;
		try{
			sendRequestToStarted(voteResp);
		}catch(Exception e ){
			//If commit / abort is not sent to all, it still should be able to write done because RM can recover itslef
			//or by asking coordinator.
		}
		crashIfRequested(MWCrashLocations.BEFOREDONE);
		activeTxns.remove(transactionID);
		logger.log(LogType.DONE, transactionID);
		return voteResult;
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
		System.out.println("MiddlewareTMRequestHandler: CRASH received");
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
	
	private boolean sendRequestToStarted(RequestDescriptor request) throws Exception{
		boolean boolResponse = true;
		boolean anyExceptions = false;
        for (ServerMode sm : ServerMode.values()) {
            if (activeTxns.get(request.transactionID).hasStarted.get(sm)) {
            	ServerConnection sc = cm.getConnection(sm);
            	ResponseDescriptor rd = null;
				try {
					rd = sc.sendRequest(request);
				} catch (Exception e) {
					System.out.println("MiddlewareTMRequestHandler: Problem sending to server " + sm);
					anyExceptions = true;
				}
				if(rd != null){
	            	if (rd.responseType == ResponseType.BOOLEAN) {
	            		boolResponse = boolResponse && (boolean) rd.data; 
	            	} else if (rd.responseType == ResponseType.ABORT || rd.responseType == ResponseType.ERROR) {
	            		boolResponse = false;
	            	}
				}
            }
        }
        if(anyExceptions){
        	throw new Exception();
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
		this.checkAndEnlistAll(request.transactionID);
		for( ServerConnection connection : cm.getAllConnections() ){
			ResponseDescriptor rd = connection.sendRequest(request);
			String data = (String) rd.data;
			if (rd.responseType == ResponseType.ERROR || rd.responseType == ResponseType.ABORT) {
				throw new Exception("Error. Data: " + (String) data + ", Message: " + rd.additionalMessage);
			}else if(rd.responseType == ResponseType.WAITINGFORVOTES){
				resendVote((int) rd.data, connection);
				return this.handleRequest(request);
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
				System.out.println("MiddlewareTMRequestHandler: Aborting Transaction " + transactionID);
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
				System.out.println("MiddlewareTMRequestHandler: Aborting Transaction " + transactionID);
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
				System.out.println("MiddlewareTMRequestHandler: Aborting Transaction " + transactionID);
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
		System.out.println("MiddlewareTMRequestHandler: Starting 2PC voting");
		Map<ServerMode, Boolean> serversUsed = this.activeTxns.get(transactionID).hasStarted;
		RequestDescriptor request = null;
		for( ServerMode mode : ServerMode.values() ){
			if(serversUsed.get(mode)){
				request = new RequestDescriptor(RequestType.PREPARE);
				request.transactionID = transactionID;
				try {
					System.out.println("MiddlewareTMRequestHandler: Sending vote request to " + mode.toString());
					boolean dataReceived = false;
					boolean vote = false;
					try{
						ResponseDescriptor rd = cm.getConnection(mode).sendRequestWithTimeOut(request, TWO_PHASE_COMMIT_TIMEOUT);
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
					System.out.println("MiddlewareTMRequestHandler: Error in twoPhaseCommitVoteRequest");
					e.printStackTrace();
					//dont think this should happen, abort just in case?
					return false;
				} 
			}
		}
		//No one voted no, so return true
		return true;
	}
	private void crashIfRequested(MWCrashLocations crashAt) {
		if(this.whereToCrash.contains(crashAt)){
			System.exit(1);
		}
	}
}
