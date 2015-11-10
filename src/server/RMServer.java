package server;

import java.io.IOException;
import java.util.Scanner;

import shared.WelcomeManager;
import shared.LockManager.LockManager;


public class RMServer {
	
	private static WelcomeManager wm;

    public static void main(String[] args) throws IOException {
    	Scanner scanner = new Scanner(System.in);
    	ResourceManager rm = new ResourceManagerImpl();
    	LockManager lm = new LockManager();
    	int serverID = -1;
    	int port = -1;
    	if( args.length == 2){
    		serverID = Integer.parseInt(args[0]);
        	port = Integer.parseInt(args[1]);
    	}
        if (args.length == 1) {
    		serverID = Integer.parseInt(args[0]);
        	System.out.println("Enter port to listen on");
    		port = Integer.parseInt(scanner.nextLine());
        }else{
        	System.out.println("Enter server ID");
        	serverID = Integer.parseInt(scanner.nextLine());
        	System.out.println("Enter port to listen on");
    		port = Integer.parseInt(scanner.nextLine());
        }
        if(port == -1 || serverID == -1){
        	scanner.close();
        	return;
        }
        TransactionManager tm = new TransactionManagerImpl(rm, lm, serverID, true);
    	TMRequestHandler rh = new TMRequestHandler(tm);

        wm = new WelcomeManager(rh, port);
        wm.startThread();
        
        System.out.println("Press enter to quit");
        scanner.nextLine();
        
        wm.stopThread();
        scanner.close();
        
        System.exit(0);
    }
}
