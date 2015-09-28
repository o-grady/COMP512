package server;

import java.io.BufferedReader;
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
	        //DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
	        String clientSentence;
	        System.out.println("Waiting for input");
	        while((clientSentence = inFromClient.readLine()) != null){
		        String[] request = clientSentence.split(",");
		        System.out.println(clientSentence);
		        
		        switch(request[0].toUpperCase()) {
			        case "NEWCAR":
			        	System.out.println("NEWCAR received");
			        	aResourceManager.addCars(Integer.parseInt(request[1]), request[2], Integer.parseInt(request[3]), Integer.parseInt(request[4]));
			        	System.out.println("car added : "+ request[0]+","+request[1]+","+request[2]+","+request[3]+","+request[4]);
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
