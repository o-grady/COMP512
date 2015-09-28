package server;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import server.ws.ResourceManager;
/**
 * Started when a new client connects and handles their requests
 * @author grady_000
 *
 */
public class ConnectionSocketThread extends Thread{
	private Socket aConnectionSocket;
	private ResourceManager aResourceManager;
	public ConnectionSocketThread(Socket connectionSocket, ResourceManager rm){
		super();
		this.aConnectionSocket = connectionSocket;
		this.aResourceManager = rm;
	}
	public void run(){
		try{
	        System.out.println("Connection accepted!");
	        BufferedReader inFromClient = new BufferedReader(new InputStreamReader(aConnectionSocket.getInputStream()));
	        DataOutputStream outToClient = new DataOutputStream(aConnectionSocket.getOutputStream());
	        String clientSentence;
	        System.out.println("Waiting for input");
	        while((clientSentence = inFromClient.readLine()) != null){
		        String[] request = clientSentence.split(",");
		        System.out.println(clientSentence);
		        
		        switch(request[0].toUpperCase()) {
			        case "NEWFLIGHT":
			        	System.out.println("NEWFLIGHT received");
			        	aResourceManager.addFlight(Integer.parseInt(request[1]), Integer.parseInt(request[2]), Integer.parseInt(request[3]), Integer.parseInt(request[4]));
			        	outToClient.writeBytes("NEWFLIGHT received");
			        	break;
			        case "NEWCAR":
			        	System.out.println("NEWCAR received");
			        	aResourceManager.addCars(Integer.parseInt(request[1]), request[2], Integer.parseInt(request[3]), Integer.parseInt(request[4]));
			        	outToClient.writeBytes("NEWCAR received");
			        	break;
			        case "NEWROOM":
			        	System.out.println("NEWROOM received");
			        	aResourceManager.addRooms(Integer.parseInt(request[1]), request[2], Integer.parseInt(request[3]), Integer.parseInt(request[4]));
			        	outToClient.writeBytes("NEWROOM received");
			        	break;
			        case "NEWCUSTOMER":
			        	System.out.println("NEWCUSTOMER received");
			        	aResourceManager.newCustomer(Integer.parseInt(request[1]));
			        	outToClient.writeBytes("NEWCUSTOMER received");
			        	break;
			        case "NEWCUSTOMERID":
			        	System.out.println("NEWCUSTOMERID received");
			        	aResourceManager.newCustomerId(Integer.parseInt(request[1]), Integer.parseInt(request[2]));
			        	outToClient.writeBytes("NEWCUSTOMERID received");
			        	break;
			        case "DELETEFLIGHT":
			        	System.out.println("DELETEFLIGHT received");
			        	aResourceManager.deleteFlight(Integer.parseInt(request[1]), Integer.parseInt(request[2]));
			        	outToClient.writeBytes("DELETEFLIGHT received");
			        	break;
			        case "DELETECAR":
			        	System.out.println("DELETECAR received");
			        	aResourceManager.deleteCars(Integer.parseInt(request[1]), request[2]);
			        	outToClient.writeBytes("DELETECAR received");
			        	break;
			        case "DELETEROOM":
			        	System.out.println("DELETEROOM received");
			        	aResourceManager.deleteRooms(Integer.parseInt(request[1]), request[2]);
			        	outToClient.writeBytes("DELETEROOM received");
			        	break;
			        case "DELETECUSTOMER":
			        	System.out.println("DELETECUSTOMER received");
			        	aResourceManager.deleteCustomer(Integer.parseInt(request[1]), Integer.parseInt(request[2]));
			        	outToClient.writeBytes("DELETECUSTOMER received");
			        	break;
			        case "QUERYFLIGHT":
			        	System.out.println("QUERYFLIGHT received");
			        	aResourceManager.queryFlight(Integer.parseInt(request[1]), Integer.parseInt(request[2]));
			        	outToClient.writeBytes("QUERYFLIGHT received");
			        	break;
			        case "QUERYCAR":
			        	System.out.println("QUERYCAR received");
			        	aResourceManager.queryCars(Integer.parseInt(request[1]), request[2]);
			        	outToClient.writeBytes("QUERYCAR received");
			        	break;
			        case "QUERYROOM":
			        	System.out.println("QUERYROOM received");
			        	aResourceManager.queryRooms(Integer.parseInt(request[1]), request[2]);
			        	outToClient.writeBytes("QUERYROOM received");
			        	break;
			        case "QUERYCUSTOMER":
			        	System.out.println("QUERYCUSTOMER received");
			        	//Pretty sure its this method
			        	aResourceManager.queryCustomerInfo(Integer.parseInt(request[1]), Integer.parseInt(request[2]));
			        	outToClient.writeBytes("QUERYCUSTOMER received");
			        	break;
			        case "QUERYFLIGHTPRICE":
			        	System.out.println("QUERYFLIGHTPRICE received");
			        	aResourceManager.queryFlightPrice(Integer.parseInt(request[1]), Integer.parseInt(request[2]));
			        	outToClient.writeBytes("QUERYFLIGHTPRICE received");
			        	break;
			        case "QUERYCARPRICE":
			        	System.out.println("QUERYCARPRICE received");
			        	aResourceManager.queryCarsPrice(Integer.parseInt(request[1]), request[2]);
			        	outToClient.writeBytes("QUERYCARPRICE received");
			        	break;
			        case "QUERYROOMPRICE":
			        	System.out.println("QUERYROOMPRICE received");
			        	aResourceManager.queryRoomsPrice(Integer.parseInt(request[1]), request[2]);
			        	outToClient.writeBytes("QUERYROOMPRICE received");
			        	break;
			        case "RESERVEFLIGHT":
			        	System.out.println("RESERVEFLIGHT received");
			        	aResourceManager.reserveFlight(Integer.parseInt(request[1]), Integer.parseInt(request[2]), Integer.parseInt(request[3]));
			        	outToClient.writeBytes("RESERVEFLIGHT received");
			        	break;
			        case "RESERVECAR":
			        	System.out.println("RESERVECAR received");
			        	aResourceManager.reserveCar(Integer.parseInt(request[1]), Integer.parseInt(request[2]), request[3]);
			        	outToClient.writeBytes("RESERVECAR received");
			        	break;
			        case "RESERVEROOM":
			        	System.out.println("RESERVEROOM received");
			        	aResourceManager.reserveRoom(Integer.parseInt(request[1]), Integer.parseInt(request[2]), request[3]);
			        	outToClient.writeBytes("RESERVEROOM received");
			        	break;
			        case "ITINERARY":
			        	System.out.println("ITINERARY received");
			        	//This one doesn't work. Fix when messages are passed as JSON
			        	//aResourceManager.reserveItinerary(Integer.parseInt(request[1]), request[2], Integer.parseInt(request[3]), Integer.parseInt(request[4]));
			        	outToClient.writeBytes("ITINERARY received");
			        	break;
		        }
		        System.out.println("Waiting for input");
	        } 
	        aConnectionSocket.close();
		}catch(Exception e){
			System.out.println("Exception caught in ConnectionSocketThread");
			e.printStackTrace();
		}
	}
}
