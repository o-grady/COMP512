package server.ws;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

import server.ConnectionSocketThread;
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
        while(true){
	    	System.out.println("Waiting on connection...");
	        Socket connectionSocket = welcomeSocket.accept();
	        //After connection is accepted start a new thread to handle
	        (new ConnectionSocketThread(connectionSocket, rm)).start();
        }
    }
}
