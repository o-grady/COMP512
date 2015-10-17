package client;

import java.net.URL;
import java.net.MalformedURLException;

public class WSClient {

    MiddlewareInterface proxy;
    
    public WSClient(String serviceName, String serviceHost, int servicePort) 
    throws MalformedURLException {
    
        URL wsdlLocation = new URL("http", serviceHost, servicePort, 
                "/" + serviceName + "/service?wsdl");
                
        MiddlewareImplService service = new MiddlewareImplService(wsdlLocation);
        
        proxy = service.getMiddlewareImplPort();
    }

}
