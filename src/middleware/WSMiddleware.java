package middleware;

import java.net.URL;
import java.net.MalformedURLException;

import middleware.ResourceManager;
import middleware.ResourceManagerImplService;

public class WSMiddleware {

    
    public static ResourceManager proxyCar;
    public static ResourceManager proxyFlight;
    public static ResourceManager proxyRoom;
    
    public WSMiddleware(String serviceNameCar, String serviceHostCar, int servicePortCar, 
    		String serviceNameFlight, String serviceHostFlight, int servicePortFlight, 
    		String serviceNameRoom, String serviceHostRoom, int servicePortRoom) 
    throws MalformedURLException {
    
        URL wsdlLocation = new URL("http", serviceHostCar, servicePortCar, 
                "/" + serviceNameCar + "/service?wsdl");
                
        ResourceManagerImplService service = new ResourceManagerImplService(wsdlLocation);
        
        System.out.println("opening car proxy");
        
        proxyCar = service.getResourceManagerImplPort();
        
        System.out.println("car proxy opened, " + proxyCar.toString());
        
        wsdlLocation = new URL("http", serviceHostFlight, servicePortFlight, 
                "/" + serviceNameFlight + "/service?wsdl");
                
        service = new ResourceManagerImplService(wsdlLocation);
        
        System.out.println("opening flight proxy");
        
        proxyFlight = service.getResourceManagerImplPort();
        
        System.out.println("flight proxy opened, " + proxyFlight.toString());
        
        wsdlLocation = new URL("http", serviceHostRoom, servicePortRoom, 
                "/" + serviceNameRoom + "/service?wsdl");
                
        service = new ResourceManagerImplService(wsdlLocation);
        
        proxyRoom = service.getResourceManagerImplPort();
    }

}
