package webserver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import java.nio.file.Files;
import java.nio.file.Paths;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class WebServer {
    public static void main(String[] args) throws Exception {
    	System.out.println(System.getProperty("user.dir"));
    	System.out.println(System.getProperty("os.name"));

        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/", new HTMLHandler());
        server.createContext("/request", new RequestHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    static class HTMLHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
        	String path = System.getProperty("user.dir")+File.separator+"src"+File.separator+"webserver"+File.separator+"view.html";
            byte[] response = Files.readAllBytes(Paths.get(path));
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
        	String bodyAsString = convertStreamToString(body).replace("%2C", ",");
        	System.out.println(bodyAsString);
        }
    }
    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}