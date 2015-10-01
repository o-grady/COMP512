package middleware;

import java.util.Scanner;


public class Main {
	
	private WelcomeManager wm;
	private ConnectionManager cm;
	private RequestHandler rh;
	private static Scanner scanner;

    public Main(int port) {
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
        
        Main client = new Main(port);
        client.run();
        
        scanner.close();
    }

    public void run() {
        System.out.println("Middleware Interface");
        
        while (true) {
        	System.out.println("Options:\n"
        			+ "1. Add server\n"
        			+ "2. Remove server\n"
        			+ "3. Quit\n"
        			+ "Please make a selection by entering a number:");
            
        	String operation = scanner.nextLine();
        	
        	if (operation.equalsIgnoreCase("3")) {
        		break;
        	}
        	
        	System.out.println("Server mode:\n"
        			+ "1. Car\n"
        			+ "2. Plane\n"
        			+ "3. Room\n"
        			+ "Please make a selection by entering a number:");
        	
        	String serverMode = scanner.nextLine();
        	
        	ServerMode mode = null;
        	switch(serverMode) {
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
        	
        	if (mode == null || operation == null) {
        		System.out.println("Incorrect selection.");
        		continue;
        	}
        	
        	switch(operation) {
        	case "1":
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
        		cm.removeServer(mode);
        		System.out.println("Server removed.");
        		break;
        	}
    	}
        
        wm.stopThread();
    }
}
