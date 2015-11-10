package middleware;

import java.util.Scanner;

import server.ResourceManager;
import server.ResourceManagerImpl;
import server.TMRequestHandler;
import server.TransactionManager;
import server.TransactionManagerImpl;
import shared.IRequestHandler;
import shared.RequestDescriptor;
import shared.RequestType;
import shared.ServerConnection;
import shared.WelcomeManager;
import shared.LockManager.LockManager;


public class MiddlewareServer {
	
	private WelcomeManager wm;
	private ConnectionManager cm;
	private IRequestHandler rh;
	private ResourceManager customerRM;
	private TransactionManager customerTM;
	private TMRequestHandler customerTMRH;		
	private static Scanner scanner;

    public MiddlewareServer(int port) {
    	cm = new ConnectionManager();
    	customerRM = new ResourceManagerImpl();
    	customerTM = new TransactionManagerImpl(customerRM, new LockManager(), 45654);
    	customerTMRH = new TMRequestHandler(customerTM);    	
    	rh = new MiddlewareTMRequestHandler(cm, customerTM, customerTMRH);
    	wm = new WelcomeManager(rh, port);
    	wm.startThread();
	}
    public MiddlewareServer(int port, String carHost, int carPort, String flightHost,
    		int flightPort, String roomHost, int roomPort) {
    	cm = new ConnectionManager();
    	customerRM = new ResourceManagerImpl();
    	customerTM = new TransactionManagerImpl(customerRM, new LockManager(), 45654);
    	customerTMRH = new TMRequestHandler(customerTM);
    	rh = new MiddlewareTMRequestHandler(cm, customerTM, customerTMRH);
    	wm = new WelcomeManager(rh, port);
    	wm.startThread();
    	//connect the servers
    	for(int i = 0 ; i < 3 ; i++){
			ServerMode mode = null;
			String serverHost = null;
			int serverPort = 0;
			switch(i){
				case 0:
					mode = ServerMode.CAR;
					serverHost = carHost;
					serverPort = carPort;
					break;
				case 1:
					mode = ServerMode.FLIGHT;
					serverHost = flightHost;
					serverPort = flightPort;
					break;
				case 2:
					mode = ServerMode.ROOM;
					serverHost = roomHost;
					serverPort = roomPort;
					break;
			}

			if (cm.addServer(mode, serverHost, serverPort)) {
				System.out.println("Success!");
    			try {
					cm.getConnection(mode).sendRequest(new RequestDescriptor(RequestType.ABORTALL));
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				System.out.println("Failed.");
			}
    	}
	}
	public static void main(String[] args) {
		scanner = new Scanner(System.in);
    	String input;
    	int port = 0;
        MiddlewareServer client = null;
    	if(args.length == 0){
	    	System.out.println("Enter the port to listen on : ");
	        input = scanner.nextLine();
	        port = Integer.parseInt(input);
	        client = new MiddlewareServer(port);
		}else if(args.length == 7){
			//port carHost carPort flightHost flightPort roomHost roomPort
			port = Integer.parseInt(args[0]);
			String carHost = args[1];
			int carPort = Integer.parseInt(args[2]);
			String flightHost = args[3];
			int flightPort = Integer.parseInt(args[4]);
			String roomHost = args[5];
			int roomPort = Integer.parseInt(args[6]);
			client = new MiddlewareServer(port, carHost, carPort, 
					flightHost, flightPort, roomHost, roomPort);
		}else{
			System.out.println("Incorrect number of arguments, expecting 0 or 7");
			scanner.close();
			System.exit(0);
		}

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
        			try {
						cm.getConnection(mode).sendRequest(new RequestDescriptor(RequestType.ABORTALL));
					} catch (Exception e) {
						e.printStackTrace();
					}
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
        		mode = ServerMode.FLIGHT;
        		break;
        	case "3":
        		mode = ServerMode.ROOM;
        		break;
        	}
    	}
    	
    	return mode;
    }
}
