package client;

import java.net.URL;
import java.net.MalformedURLException;


public class WSClient {

    
    ResourceManager proxyCar;
    ResourceManager proxyFlight;
    ResourceManager proxyRoom;
    
    public WSClient(String serviceNameCar, String serviceHostCar, int servicePortCar, 
    		String serviceNameFlight, String serviceHostFlight, int servicePortFlight, 
    		String serviceNameRoom, String serviceHostRoom, int servicePortRoom) 
    throws MalformedURLException {
    
        URL wsdlLocation = new URL("http", serviceHostCar, servicePortCar, 
                "/" + serviceNameCar + "/service?wsdl");
                
        ResourceManagerImplService service = new ResourceManagerImplService(wsdlLocation);
        
        proxyCar = service.getResourceManagerImplPort();
        
        wsdlLocation = new URL("http", serviceHostFlight, servicePortFlight, 
                "/" + serviceNameFlight + "/service?wsdl");
                
        service = new ResourceManagerImplService(wsdlLocation);
        
        proxyFlight = service.getResourceManagerImplPort();
        
        wsdlLocation = new URL("http", serviceHostRoom, servicePortRoom, 
                "/" + serviceNameRoom + "/service?wsdl");
                
        service = new ResourceManagerImplService(wsdlLocation);
        
        proxyRoom = service.getResourceManagerImplPort();
    }

}
