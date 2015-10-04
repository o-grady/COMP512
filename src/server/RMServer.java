package server;

import java.io.IOException;
import java.util.Scanner;

import shared.WelcomeManager;


public class RMServer {
	
	private static WelcomeManager wm;

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
        	scanner.close();
        	return;
        }
        
        wm = new WelcomeManager(rh, port);
        wm.startThread();
        
        System.out.println("Press enter to quit");
        scanner.nextLine();
        
        wm.stopThread();
        scanner.close();
        
        System.exit(0);
    }
}
