package server;

import java.io.IOException;
import java.net.*;


public class Main {

    public static void main(String[] args) throws IOException {
    
    	ResourceManager rm = new ResourceManagerImpl();
    	RMRequestHandler rh = new RMRequestHandler(rm);
    	
        if (args.length != 1) {
            System.out.println(
                "Usage: java Main <service-port>");
            System.exit(-1);
        }
        
        int port = Integer.parseInt(args[0]);
        ServerSocket welcomeSocket = null;
        
        try {
        	welcomeSocket = new ServerSocket(port);
	        while(true){
		    	System.out.println("Listening for connections on port " + args[0]);
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
