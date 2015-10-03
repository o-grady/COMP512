package server;

import java.io.IOException;
import java.net.*;
import java.util.Scanner;


public class Main {

    public static void main(String[] args) throws IOException {
    	Scanner scanner = new Scanner(System.in);
    	ResourceManager rm = new ResourceManagerImpl();
    	RMRequestHandler rh = new RMRequestHandler(rm);
    	int port = -1;
        if (args.length == 0) {
        	System.out.println("Enter port to listen on");
    		port = Integer.parseInt(scanner.nextLine());
        }else{
        	port = Integer.parseInt(args[0]);
        }
        if(port == -1){
        	return;
        }
      
        ServerSocket welcomeSocket = null;
        
        try {
        	welcomeSocket = new ServerSocket(port);
	        while(true){
		    	System.out.println("Listening for connections on port " + port);
		        Socket connectionSocket = welcomeSocket.accept();
		        //After connection is accepted start a new thread to handle
		        (new ConnectionSocketThread(connectionSocket, rh)).start();
	        }
        }
        finally {
        	if (welcomeSocket != null) {
        		welcomeSocket.close();
        	}
        }
    }
}
