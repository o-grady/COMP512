package server;

import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.net.Socket;

import shared.RequestDescriptor;
import shared.ResponseDescriptor;
/**
 * Started when a new client connects and handles their requests
 * @author grady_000
 *
 */
public class ConnectionSocketThread extends Thread{
	
	private Socket connectionSocket;
	private ResourceManager resourceManager;
	private ObjectInputStream inFromClient;
	private ObjectOutputStream outToClient;
	
	public ConnectionSocketThread(Socket connectionSocket, ResourceManager rm){
		super();
		this.connectionSocket = connectionSocket;
		this.resourceManager = rm;
	}
	
	public void run(){
		try{
	        System.out.println("Connection accepted!");
	        inFromClient = new ObjectInputStream(connectionSocket.getInputStream());
	        outToClient = new ObjectOutputStream(connectionSocket.getOutputStream());
	        
	        Object read;
	        
	        System.out.println("Waiting for input");
	        
	        while((read = inFromClient.readObject()) != null){
	        	RequestDescriptor request;
	        	ResponseDescriptor response = new ResponseDescriptor();
	        	
	        	if (read.getClass() == RequestDescriptor.class) {
	        		request = (RequestDescriptor) read;
	        	} else {
	        		System.out.println("Expected an object of type " + RequestDescriptor.class.getName() + " but received an object of type " + read.getClass().getName());
	        		continue;
	        	}
	        	
	        	String responseMessage = null;
		        
		        switch(request.requestType) {
			        case NEWFLIGHT:
			        	System.out.println("NEWFLIGHT received");
			        	responseMessage = Boolean.toString(
			        			resourceManager.addFlight(request.id, request.flightNumber, request.numSeats, request.price)
			        			);
			        	break;
			        case NEWCAR:
			        	System.out.println("NEWCAR received");
			        	responseMessage = Boolean.toString(
			        			resourceManager.addCars(request.id, request.location, request.numCars, request.price)
			        			);
			        	break;
			        case NEWROOM:
			        	System.out.println("NEWROOM received");
			        	responseMessage = Boolean.toString(
			        			resourceManager.addRooms(request.id, request.location, request.numRooms, request.price)
			        			);
			        	break;
			        case NEWCUSTOMER:
			        	System.out.println("NEWCUSTOMER received");
			        	responseMessage = Integer.toString(
			        			resourceManager.newCustomer(request.id)
			        			);
			        	break;
			        case NEWCUSTOMERID:
			        	System.out.println("NEWCUSTOMERID received");
			        	responseMessage = Boolean.toString(
			        			resourceManager.newCustomerId(request.id, request.customerNumber)
			        			);
			        	break;
			        case DELETEFLIGHT:
			        	System.out.println("DELETEFLIGHT received");
			        	responseMessage = Boolean.toString(
			        			resourceManager.deleteFlight(request.id, request.flightNumber)
			        			);
			        	break;
			        case DELETECAR:
			        	System.out.println("DELETECAR received");
			        	responseMessage = Boolean.toString(
			        			resourceManager.deleteCars(request.id, request.location)
			        			);
			        	break;
			        case DELETEROOM:
			        	System.out.println("DELETEROOM received");
			        	responseMessage = Boolean.toString(
			        			resourceManager.deleteRooms(request.id, request.location)
			        			);
			        	break;
			        case DELETECUSTOMER:
			        	System.out.println("DELETECUSTOMER received");
			        	responseMessage = Boolean.toString(
			        			resourceManager.deleteCustomer(request.id, request.customerNumber)
			        			);
			        	break;
			        case QUERYFLIGHT:
			        	System.out.println("QUERYFLIGHT received");
			        	responseMessage = Integer.toString(
			        			resourceManager.queryFlight(request.id, request.flightNumber)
			        			);
			        	break;
			        case QUERYCAR:
			        	System.out.println("QUERYCAR received");
			        	responseMessage = Integer.toString(
			        			resourceManager.queryCars(request.id, request.location)
			        			);
			        	break;
			        case QUERYROOM:
			        	System.out.println("QUERYROOM received");
			        	responseMessage = Integer.toString(
			        			resourceManager.queryRooms(request.id, request.location)
			        			);
			        	break;
			        case QUERYCUSTOMER:
			        	System.out.println("QUERYCUSTOMER received");
			        	//Pretty sure its this method
			        	responseMessage = resourceManager.queryCustomerInfo(request.id, request.customerNumber);
			        	break;
			        case QUERYFLIGHTPRICE:
			        	System.out.println("QUERYFLIGHTPRICE received");
			        	responseMessage = Integer.toString(
			        			resourceManager.queryFlightPrice(request.id, request.flightNumber)
			        			);
			        	break;
			        case QUERYCARPRICE:
			        	System.out.println("QUERYCARPRICE received");
			        	responseMessage = Integer.toString(
			        			resourceManager.queryCarsPrice(request.id, request.location)
			        			);
			        	break;
			        case QUERYROOMPRICE:
			        	System.out.println("QUERYROOMPRICE received");
			        	responseMessage = Integer.toString(
			        			resourceManager.queryRoomsPrice(request.id, request.location)
			        			);
			        	break;
			        case RESERVEFLIGHT:
			        	System.out.println("RESERVEFLIGHT received");
			        	responseMessage = Boolean.toString(
			        			resourceManager.reserveFlight(request.id, request.customerNumber, request.flightNumber)
			        			);
			        	break;
			        case RESERVECAR:
			        	System.out.println("RESERVECAR received");
			        	responseMessage = Boolean.toString(
			        			resourceManager.reserveCar(request.id, request.customerNumber, request.location)
			        			);
			        	break;
			        case RESERVEROOM:
			        	System.out.println("RESERVEROOM received");
			        	responseMessage = Boolean.toString(
			        			resourceManager.reserveRoom(request.id, request.customerNumber, request.location)
			        			);
			        	break;
			        case ITINERARY:
			        	System.out.println("ITINERARY received");
			        	//This one doesn't work. Fix when messages are passed as JSON
			        	//aResourceManager.reserveItinerary(request.id, request[2], Integer.parseInt(request[3]), Integer.parseInt(request[4]));
			        	//outToClient.writeBytes("ITINERARY received");
			        	break;
		        }
		        
		        response = new ResponseDescriptor(responseMessage);
		        
		        outToClient.writeObject(response);
		        
		        System.out.println("Waiting for input");
	        } 
	        connectionSocket.close();
		}catch(Exception e){
			System.out.println("Exception caught in ConnectionSocketThread");
			e.printStackTrace();
		}
	}
}
