package middleware;

import java.util.Scanner;

import shared.ServerConnection;
import shared.WelcomeManager;


public class MiddlewareServer {
	
	private WelcomeManager wm;
	private ConnectionManager cm;
	private RequestHandler rh;
	private static Scanner scanner;

    public MiddlewareServer(int port) {
    	cm = new ConnectionManager();
    	rh = new RequestHandler(cm);
    	wm = new WelcomeManager(rh, port);
    	wm.startThread();
	}

	public static void main(String[] args) {
		scanner = new Scanner(System.in);
    	String input;
    	
    	System.out.println("Enter the port to listen on : ");
        input = scanner.nextLine();
        int port = Integer.parseInt(input);
        
        MiddlewareServer client = new MiddlewareServer(port);
        client.run();
        
        scanner.close();
        System.exit(0);
    }

    private void run() {
        System.out.println("Middleware Interface");
        boolean exit = false;
        
        while (!exit) {
        	System.out.println("\nOptions:\n"
        			+ "1. Add server\n"
        			+ "2. Remove server\n"
        			+ "3. List all connections\n"
        			+ "4. Quit\n"
        			+ "Please make a selection by entering a number:");
            
        	String operation = scanner.nextLine();
        	
        	switch(operation) {
        	case "1":
        		ServerMode mode = this.inputServerMode();
        		if (cm.modeIsConnected(mode)) {
        			System.out.println("There is already a " + mode.toString() + " server connected. Please remove it before attempting a new server.");
        			break;
        		}
        		System.out.println("Enter the hostname: ");
        		String hostname = scanner.nextLine();
        		System.out.println("Enter the port number: ");
        		int port = Integer.parseInt(scanner.nextLine());
        		if (cm.addServer(mode, hostname, port)) {
        			System.out.println("Success!");
        		} else {
        			System.out.println("Failed.");
        		}
        		break;
        	case "2":
        		mode = this.inputServerMode();
        		if (cm.modeIsConnected(mode)) {
        			cm.removeServer(this.inputServerMode());
            		System.out.println("Server removed.");
        		} else {
        			System.out.println("There is no " + mode.toString() + " server connected.");
        		}
        		break;
        	case "3":
        		for (ServerMode sm : ServerMode.values()) {
        			if (sm == ServerMode.CUSTOMER) {
        				// The middleware server is the customer server so we 
        				// have no reason to check whether it is connected
        				continue;
        			}
        			if (cm.modeIsConnected(sm)) {
        				ServerConnection sc = cm.getConnection(sm);
        				System.out.println("Mode " + sm.toString() 
        						+ " is connected: " 
        						+ sc.getHostname() + ":" 
        						+ sc.getPort());
        			} else {
        				System.out.println("Mode " + sm.toString() + " is not connected.");
        			}
        		}
        		break;
        	case "4":
        		exit = true;
        		continue;
        	default:
        		System.out.println("Incorrect selection.");
        		continue;
        	}
    	}
        
        wm.stopThread();
    }
    
    /*
     * Guarantees the user inputs a valid mode selection.
     */
    private ServerMode inputServerMode() {
    	ServerMode mode = null;
    	
    	while(mode == null) {
        	System.out.println("Server mode:\n"
        			+ "1. Car\n"
        			+ "2. Plane\n"
        			+ "3. Room\n"
        			+ "Please make a selection by entering a number:");
        	
        	String input = scanner.nextLine();
    		
        	switch(input) {
        	case "1":
        		mode = ServerMode.CAR;
        		break;
        	case "2":
        		mode = ServerMode.PLANE;
        		break;
        	case "3":
        		mode = ServerMode.ROOM;
        		break;
        	}
    	}
    	
    	return mode;
    }
}
