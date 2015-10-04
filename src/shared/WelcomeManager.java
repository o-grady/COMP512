package shared;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class WelcomeManager extends Thread {
	
	private int port;
	private IRequestHandler requestHandler;
	private ServerSocket welcomeSocket;
	
	public WelcomeManager(IRequestHandler requestHandler, int port) {
		this.port = port;
		this.requestHandler = requestHandler;
		this.welcomeSocket = null;
	}
	
	public void startThread() {
		this.start();
	}
	
	public void stopThread() {
		if (!welcomeSocket.isClosed()) {
			try {
				welcomeSocket.close();
			} catch (Exception e) {
				//e.printStackTrace();
			}
			finally {
				this.interrupt();
			}
		}
		
	}

	public void run() {
        try {
        	welcomeSocket = new ServerSocket(port);
	        while(true){
		    	//System.out.println("Listening for connections on port " + port);
		        Socket connectionSocket = welcomeSocket.accept();
		        //After connection is accepted start a new thread to handle
		        (new ClientConnectionThread(connectionSocket, requestHandler)).start();
	        }
        } catch (Exception e) {
			//e.printStackTrace();
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
