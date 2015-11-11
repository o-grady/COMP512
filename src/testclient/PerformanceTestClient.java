package testclient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import shared.RequestDescriptor;
import shared.RequestType;
import shared.ResponseDescriptor;
import shared.ServerConnection;

public class PerformanceTestClient {
	private static final int round = 53;
	private static final int iterations = 60;
	
	static class ClientThread extends Thread{
		PerformanceTestClient t;
		int threadNumber;
		int delay;
		public ClientThread(PerformanceTestClient t, int threadNumber, int delay){
			super();
			this.t = t;
			this.threadNumber = threadNumber;
			this.delay = delay;
		}
		public void run(){
			float ave = t.averageTxnTime(false, iterations, delay);
			System.out.println("Thread " + threadNumber + " average," + ave);
		}
	}
	
	public static void main(String args[]){
		ServerConnection middlewareConnection;
		if(args.length != 2){
			//Need mw hostname, port
			System.out.println("Expecting 2 parameters");
			return;
		}
		try {
			middlewareConnection = new ServerConnection(args[0], Integer.parseInt(args[1]));
			int customerNumber = round;
			int flightNumber = round;
			String carLocation = Integer.toString(round);
			String roomLocation = Integer.toString(round);
			/*
			newCustomerID(middlewareConnection, customerNumber);
			//Single Client Single RM
			PerformanceTestClient t = new PerformanceTestClient(middlewareConnection, customerNumber, flightNumber, carLocation, roomLocation);
			float singleAve = t.averageTxnTime(true, iterations, 0);
			System.out.println("Single Client Single RM Average Txn Time = " + singleAve);
			
			//Change numbers to avoid deadlocks
			customerNumber++;
			flightNumber++;
			carLocation += "a";
			roomLocation += "a";
			
			newCustomerID(middlewareConnection, customerNumber);
			//Single Client MultiRM
			t = new PerformanceTestClient(middlewareConnection, customerNumber, flightNumber, carLocation, roomLocation);
			float multiAve = t.averageTxnTime(false, iterations, 0);
			System.out.println("Multi Client Multi RM Average Txn Time = " + multiAve);*/
			System.out.println("Starting multiClient test");
			
			//MultiClient MultiRM
			int numberClients = 10;
			PerformanceTestClient[] clientArray = new PerformanceTestClient[numberClients];
			for(int i = 0 ; i < numberClients ; i++){
				//Change numbers to avoid deadlocks
				customerNumber++;
				flightNumber++;
				
				newCustomerID(middlewareConnection, customerNumber);
				clientArray[i] = new PerformanceTestClient(middlewareConnection, customerNumber, flightNumber, carLocation+Integer.toString(i), roomLocation+Integer.toString(i));
			}
			for(int i = 0 ; i < numberClients ; i++){
				(new ClientThread(clientArray[i], i, numberClients * round)).start();
				Thread.sleep(round);
			}
			System.in.read();
			
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	ServerConnection middlewareConnection;
	int customerID;
	int flightNumber;
	String carLocation;
	String roomLocation;
	public PerformanceTestClient(ServerConnection mw, int customerNumber, int flightNumber, String carLocation, String roomLocation ){
		this.middlewareConnection = mw;
		this.customerID = customerNumber;
		this.flightNumber = flightNumber;
		this.carLocation = carLocation;
		this.roomLocation = roomLocation;
	}
	public float averageTxnTime(boolean isSingleRM, int iterations, int delayMillis ){
		try {
			List<Long> transactionTimes = new ArrayList<Long>();
			int transactionsDone = 0;
			while(transactionsDone < iterations){		
				long timeStart = System.currentTimeMillis();
				if(isSingleRM){
					singleRMTest(middlewareConnection, customerID, flightNumber);
				}else{ 
					multipleRMTest(middlewareConnection, customerID, flightNumber, carLocation, roomLocation);
				}
				long timeEnd = System.currentTimeMillis();
				long timeElapsed = timeEnd - timeStart;
				transactionTimes.add(timeElapsed);
				
				transactionsDone++;
				//System.out.println("Transaction " + transactionsDone + " completed");
				if (delayMillis - timeElapsed > 0) 
					Thread.sleep(delayMillis - timeElapsed);
			}
			long totalTime = 0;
			for( long l : transactionTimes){
				totalTime += l;
			}
			float averageTime = (float) totalTime / transactionTimes.size();

			return averageTime;
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
	}
	//Makes customers first
	public static void newCustomerID(ServerConnection mw, int customerNumber){
		RequestDescriptor req = new RequestDescriptor(RequestType.STARTTXN);
		try {
			ResponseDescriptor resp = mw.sendRequest(req);
			int transactionID = (int)resp.data;

			req = new RequestDescriptor(RequestType.NEWCUSTOMERID);
			req.transactionID = transactionID;
			req.customerNumber = customerNumber;
			resp = mw.sendRequest(req);
		
			req = new RequestDescriptor(RequestType.COMMIT);
			req.transactionID = transactionID; 
			resp = mw.sendRequest(req);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void singleRMTest(ServerConnection mw, int customerNumber, int flightNumber){
		RequestDescriptor req = new RequestDescriptor(RequestType.STARTTXN);
		try {
			ResponseDescriptor resp = mw.sendRequest(req);
			int transactionID = (int)resp.data;
			req = new RequestDescriptor(RequestType.NEWFLIGHT);
			req.transactionID = transactionID;
			req.flightNumber = flightNumber;
			req.numSeats = 5;
			req.price = 5;
			mw.sendRequest(req);
			
			req = new RequestDescriptor(RequestType.QUERYFLIGHT);
			req.transactionID = transactionID;
			req.flightNumber = flightNumber;
			mw.sendRequest(req);

			req = new RequestDescriptor(RequestType.RESERVEFLIGHT);
			req.transactionID = transactionID;
			req.flightNumber = flightNumber;
			req.customerNumber = customerNumber;
			mw.sendRequest(req);
			
			req = new RequestDescriptor(RequestType.COMMIT);
			req.transactionID = transactionID;
			mw.sendRequest(req);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void multipleRMTest(ServerConnection mw, int customerNumber, int flightNumber, String carLocation, String roomLocation){
		RequestDescriptor req = new RequestDescriptor(RequestType.STARTTXN);
		try {
			ResponseDescriptor resp = mw.sendRequest(req);
			int transactionID = (int)resp.data;
			req = new RequestDescriptor(RequestType.NEWFLIGHT);
			req.transactionID = transactionID;
			req.flightNumber = flightNumber;
			req.numSeats = 5;
			req.price = 5;	
			mw.sendRequest(req);
			
			req = new RequestDescriptor(RequestType.NEWCAR);
			req.transactionID = transactionID;
			req.location = carLocation;
			req.numCars = 5;
			req.price = 5;
			mw.sendRequest(req);
			
			req = new RequestDescriptor(RequestType.NEWROOM);
			req.transactionID = transactionID;
			req.location = roomLocation;
			req.numRooms = 5;
			req.price = 5;
			mw.sendRequest(req);

			req = new RequestDescriptor(RequestType.COMMIT);
			req.transactionID = transactionID;
			mw.sendRequest(req);
		}catch (Exception e){
			e.printStackTrace();
		}
	}
}
