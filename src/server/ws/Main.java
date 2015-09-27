package server.ws;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.net.*;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

import server.ResourceManagerImpl;


public class Main {

    public static void main(String[] args) 
    throws Exception {
    
    	ResourceManager rm = new ResourceManagerImpl();
    	
        if (args.length != 3) {
            System.out.println(
                "Usage: java Main <service-name> <service-port> <deploy-dir>");
            System.exit(-1);
        }
    
        String serviceName = args[0];
        int port = Integer.parseInt(args[1]);
        String deployDir = args[2];
        
        ServerSocket welcomeSocket = new ServerSocket(port);
        System.out.println("Waiting on connection...");
        Socket connectionSocket = welcomeSocket.accept();
        System.out.println("Connection accepted!");
        BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
        DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
        String clientSentence = inFromClient.readLine();
        String[] request = clientSentence.split(",");
        System.out.println(clientSentence);
        
        switch(request[0].toUpperCase()) {
	        case "NEWCAR":
	        	System.out.println("NEWCAR received");
	        	rm.addCars(Integer.parseInt(request[1]), request[2], Integer.parseInt(request[3]), Integer.parseInt(request[4]));
	        	System.out.println("car added : "+ request[0]+","+request[1]+","+request[2]+","+request[3]+","+request[4]);
	        	break;
        }
        
        welcomeSocket.close();
    }
    
}
