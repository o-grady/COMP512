package middleware;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import server.ConnectionSocketThread;

public class WelcomeManager extends Thread {
	
	private int port;
	private RequestHandler requestHandler;
	
	public WelcomeManager(RequestHandler requestHandler, int port) {
		this.port = port;
		this.requestHandler = requestHandler;
	}
	
	public void startThread() {
		this.start();
	}

	public void run() {
        ServerSocket welcomeSocket = null;
        
        try {
        	welcomeSocket = new ServerSocket(port);
	        while(true){
		    	System.out.println("Listening for connections on port " + port);
		        Socket connectionSocket = welcomeSocket.accept();
		        //After connection is accepted start a new thread to handle
		        (new ClientConnectionThread(connectionSocket, requestHandler)).start();
	        }
        } catch (IOException e) {
			e.printStackTrace();
		}
        finally {
        	if (welcomeSocket != null) {
        		try {
					welcomeSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
        	}
        }
	}
}
