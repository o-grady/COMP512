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
	private RMRequestHandler requestHandler;
	private ObjectInputStream inFromClient;
	private ObjectOutputStream outToClient;
	
	public ConnectionSocketThread(Socket connectionSocket, RMRequestHandler requestHandler){
		super();
		this.connectionSocket = connectionSocket;
		this.requestHandler = requestHandler;
	}
	
	public void run(){
		try{
	        System.out.println("Connection accepted!");
	        inFromClient = new ObjectInputStream(connectionSocket.getInputStream());
	        outToClient = new ObjectOutputStream(connectionSocket.getOutputStream());
	        
	        Object read;
	        
	        while((read = inFromClient.readObject()) != null){
	        	System.out.println("Waiting for input");
	        	
	        	RequestDescriptor request;
	        	ResponseDescriptor response = new ResponseDescriptor();
	        	
	        	if (read.getClass() == RequestDescriptor.class) {
	        		request = (RequestDescriptor) read;
	        	} else {
	        		System.out.println("Expected an object of type " + RequestDescriptor.class.getName() + " but received an object of type " + read.getClass().getName());
	        		continue;
	        	}
	        	
		        response = requestHandler.handleRequest(request);
		        
		        outToClient.writeObject(response);
	        } 
	        connectionSocket.close();
		}catch(Exception e){
			System.out.println("Exception caught in ConnectionSocketThread");
			e.printStackTrace();
		}
	}
}
