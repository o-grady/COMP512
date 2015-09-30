package middleware;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import shared.RequestDescriptor;
import shared.ResponseDescriptor;

public class ClientConnectionThread extends Thread{

	private RequestHandler requestHandler;
	private Socket socket;
	private ObjectInputStream inFromClient;
	private ObjectOutputStream outToClient;
	
	public ClientConnectionThread(Socket socket, RequestHandler requestHandler) {
		super();
		this.requestHandler = requestHandler;
		this.socket = socket;
	}
	
	public void run() {
		try{
	        System.out.println("Connection accepted!");
	        inFromClient = new ObjectInputStream(socket.getInputStream());
	        outToClient = new ObjectOutputStream(socket.getOutputStream());
	        
	        Object read;
	        
	        while((read = inFromClient.readObject()) != null){
	        	System.out.println("Waiting for input");
	        	
	        	RequestDescriptor request;
	        	ResponseDescriptor response;
	        	
	        	if (read.getClass() == RequestDescriptor.class) {
	        		request = (RequestDescriptor) read;
	        	} else {
	        		System.out.println("Expected an object of type " + RequestDescriptor.class.getName() + " but received an object of type " + read.getClass().getName());
	        		continue;
	        	}
	        	
	        	response = requestHandler.handleRequest(request);
		        outToClient.writeObject(response);
	        } 
	        socket.close();
		}catch(Exception e){
			System.out.println("Exception caught in ClientConnectionThread");
			e.printStackTrace();
		}
	}
}
