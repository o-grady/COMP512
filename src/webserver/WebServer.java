package webserver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import shared.RequestDescriptor;
import shared.RequestType;

public class WebServer {
	
    public static void main(String[] args) throws Exception {
    	//System.out.println(System.getProperty("user.dir"));
    	//System.out.println(System.getProperty("os.name"));

        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/", new StaticFileHandler());
        server.createContext("/request", new RequestHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
        	//System.out.println(t.getRequestURI().toString());
        	String dirPath = System.getProperty("user.dir")+File.separator+"src"+File.separator+"webserver"+File.separator+"public"+File.separator;
        	String item = t.getRequestURI().toString().substring(1);
        	String pathStr;
        	if(item.equals("")){
        		pathStr = dirPath + "index.html";
        	}else{
        		pathStr = dirPath + item;
        	}
        	File f = new File(pathStr);
        	if(!f.exists() || f.isDirectory()) { 
        		return;
        	}
            byte[] response = Files.readAllBytes(Paths.get(pathStr));
            int index = item.lastIndexOf(".");
            String extension = "";
            if(index >= 0){
            	extension = item.substring(index);
            }
            String contentType;
            if(extension.equals(".css")){
            	contentType = "text/css";
            }else if(extension.equals(".js")){
            	contentType = "application/javascript";
            }else{
            	contentType = "text/html";
            }
            Headers h = t.getResponseHeaders();
            h.set("Content-Type", contentType);
            t.sendResponseHeaders(200, response.length);
            OutputStream os = t.getResponseBody();
            os.write(response);
            os.close();
        }
    }
    static class RequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
        	InputStream body = t.getRequestBody();
        	String bodyAsString = convertStreamToString(body);//.replace("%2C", ",");
        	System.out.println(bodyAsString);
        	RequestDescriptor req = parsePostData(bodyAsString);
        	System.out.println(req.toString());
        }
    }
    static RequestDescriptor parsePostData(String postData){
    	//split string into key value pairs
    	String[] keyValueStrings = postData.split("&");
    	//Stores all key / values except for requestType
    	Map<String, String> parameterMap = new HashMap<String, String>();
    	RequestType requestType = null;
    	for(int i = 0 ; i < keyValueStrings.length ; i++){
    		String[] keyValueArray = keyValueStrings[i].split("=");
    		if( keyValueArray[0].equals("method")){
    			String method = keyValueArray[1].toUpperCase();
    			requestType = RequestType.valueOf(method);
    		}else{
    			if(keyValueArray.length > 1){
    				parameterMap.put(keyValueArray[0], keyValueArray[1]);
    			}
    		}
    	}
    	RequestDescriptor request = new RequestDescriptor(requestType);
    	Field[] fields = RequestDescriptor.class.getDeclaredFields();
    	for(int i = 0 ; i < fields.length ; i++){
    		//System.out.println(fields[i].getType().toString());
    		if(parameterMap.containsKey(fields[i].getName())){
    			try {
    				String paramValueStr = parameterMap.get(fields[i].getName());
    				System.out.println(fields[i].getType().toString());
    				switch(fields[i].getType().toString()){
    					case "int":
    						int intParamValue = Integer.parseInt(paramValueStr);
    						fields[i].set(request, intParamValue);
    						break;
    					case "boolean":
    						boolean boolParamValue = Boolean.parseBoolean(paramValueStr);
    						fields[i].set(request, boolParamValue);
    						break;
    					case "class java.lang.String":
    						fields[i].set(request, paramValueStr);
    						break;
    				}
					//fields[i].set(request, parameterMap.get(fields[i].getName()));
				} catch (IllegalArgumentException | IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		}
    	}
    	return request;
    }
    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}