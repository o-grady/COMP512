package webserver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import shared.RequestDescriptor;
import shared.RequestType;
import shared.ResponseDescriptor;
import shared.ServerConnection;

public class WebServer {
	private static Scanner scanner;
	private static ServerConnection middlewareConnection;
    public static void main(String[] args) {
    	System.out.println(System.getProperty("user.dir"));
    	System.out.println(System.getProperty("os.name"));
    	scanner = new Scanner(System.in);
        HttpServer server = null;
		try {
			server = HttpServer.create(new InetSocketAddress(8000), 0);
		} catch (IOException e) {
			e.printStackTrace();
		}
        server.createContext("/", new StaticFileHandler());
        server.createContext("/request", new RequestHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
        String hostname = null;
        String port = null;
        if(args.length >= 2){
        	hostname = args[0];
        	port = args[1];
        }else if(args.length == 1){
        	hostname = args[0];
        	System.out.println("Enter middleware port number");
    		port = scanner.nextLine();
        }else if(args.length == 0){
        	System.out.println("Enter middleware hostname");
    		hostname = scanner.nextLine();
        	System.out.println("Enter middleware port number");
    		port = scanner.nextLine();
        }
		try {
			middlewareConnection = new ServerConnection(hostname, Integer.parseInt(port));
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
        	System.out.println(t.getRequestURI().toString());
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
        	RequestDescriptor req = parsePostData(bodyAsString);
    		ResponseDescriptor response;
    		String message = null;
			try {
				response = middlewareConnection.sendRequest(req);
				if(response.data != null) {
					message = "Data: " +  response.data.toString();
				}
				if(response.additionalMessage != null) {
					message += ", Message: " + response.additionalMessage;
				}
	    		System.out.println("Response recieved " + message);
			} catch (Exception e) {
				message = "MIDDLEWARE ERROR";
				e.printStackTrace();
			}
    		Headers h = t.getResponseHeaders();
    		h.set("Content-Type", "text/plain");

    		byte[] messageBytes = message.getBytes();
    		t.sendResponseHeaders(200, messageBytes.length);
    		OutputStream responseBody = t.getResponseBody();
    		System.out.println(messageBytes.length);
    		responseBody.write(messageBytes);
    		responseBody.close();
    		t.close();

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
    	//Fill in reqest fields using reflection
    	for(int i = 0 ; i < fields.length ; i++){
    		if(parameterMap.containsKey(fields[i].getName())){
    			try {
    				String paramValueStr = parameterMap.get(fields[i].getName());
    				System.out.println(fields[i].toString());
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
    					case "class java.util.Vector":
    						//Convert comma separated flights into Vector
    						Vector<Integer> paramValueVector = new Vector<Integer>();
    						String[] flightNumStrings = paramValueStr.split("%2C");
    						for(int j = 0 ; j < flightNumStrings.length ; j++){
    							int flightNum = Integer.parseInt(flightNumStrings[j]);
    							paramValueVector.addElement(flightNum);
    						}
    						fields[i].set(request, paramValueVector);
    						break;
    				}
				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
				}
    		}
    	}
    	return request;
    }
    static String convertStreamToString(java.io.InputStream is) {
        Scanner s1 = new Scanner(is);
        Scanner s2 = s1.useDelimiter("\\A");
        String ret = s2.hasNext() ? s2.next() : "";
        s1.close();
        s2.close();
        return ret;
    }
}
