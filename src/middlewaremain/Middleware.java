package middlewaremain;

import java.util.*;
import java.io.*;

import org.apache.catalina.startup.Tomcat;


public class Middleware {

    public Middleware(String serviceNameCar, String serviceHostCar, int servicePortCar, 
    		String serviceNameFlight, String serviceHostFlight, int servicePortFlight, 
    		String serviceNameRoom, String serviceHostRoom, int servicePortRoom) 
    throws Exception {
        /*super(serviceNameCar, serviceHostCar, servicePortCar, 
        		serviceNameFlight, serviceHostFlight,servicePortFlight, 
        		serviceNameRoom, serviceHostRoom, servicePortRoom) ;*/
    }
    
    public static void main(String[] args) {
        
        try {        
            if (args.length != 12) {
                System.out.println("Usage: Middleware " 
                        + "<service-name-car> <service-host-car> <service-port-car>" 
                        + "<service-name-flight> <service-host-flight> <service-port-flight>"
                        + "<service-name-room> <service-host-room> <service-port-room>"
                        + "<service-name> <service-port> <deploy-dir>");
                System.exit(-1);
            }
            
            String serviceNameCar = args[0];
            String serviceHostCar = args[1];
            int servicePortCar = Integer.parseInt(args[2]);
            
            String serviceNameFlight = args[3];
            String serviceHostFlight = args[4];
            int servicePortFlight = Integer.parseInt(args[5]);
            
            String serviceNameRoom = args[6];
            String serviceHostRoom = args[7];
            int servicePortRoom = Integer.parseInt(args[8]);
            
            System.out.println("Instantiating Middleware");
            
            /*Middleware client = new Middleware(serviceNameCar, serviceHostCar, servicePortCar, 
            		serviceNameFlight, serviceHostFlight,servicePortFlight, 
            		serviceNameRoom, serviceHostRoom, servicePortRoom) ;*/
            		
            //MiddlewareImpl.setupProxies(serviceNameCar, serviceHostCar, servicePortCar, serviceNameFlight, serviceHostFlight, servicePortFlight, serviceNameRoom, serviceHostRoom, servicePortRoom);
            
            String serviceName = args[9];
            int port = Integer.parseInt(args[10]);
            String deployDir = args[11];
            
            System.out.println("Starting cat");
        
            Tomcat tomcat = new Tomcat();
            tomcat.setPort(port);
            tomcat.setBaseDir(deployDir);

            tomcat.getHost().setAppBase(deployDir);
            tomcat.getHost().setDeployOnStartup(true);
            tomcat.getHost().setAutoDeploy(true);
            
            tomcat.addWebapp("/" + serviceName, 
                    new File(deployDir + "/" + serviceName).getAbsolutePath());

            tomcat.start();
            tomcat.getServer().await();            
            
        } catch(Exception e) {
            e.printStackTrace();
        }
    }



    
}
