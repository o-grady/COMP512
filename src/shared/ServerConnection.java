package shared;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ServerConnection {
	
	private String hostname;
	private int port;
	private Object socketCreationLock = new Object();
	
	public ServerConnection(String hostname, int port) throws IOException {
		this.hostname = hostname;
		this.port = port;
	}
	
	public ResponseDescriptor sendRequest(RequestDescriptor request) throws Exception {
		Socket connectionSocket = null;
		synchronized ( socketCreationLock ){
			connectionSocket = new Socket(hostname, port);
		}
		ObjectOutputStream streamOut = new ObjectOutputStream(connectionSocket.getOutputStream());
		ObjectInputStream streamIn = new ObjectInputStream(connectionSocket.getInputStream());
		
		streamOut.writeObject(request);
		Object read = streamIn.readObject();
		
		connectionSocket.close();
		
		if (read.getClass() == ResponseDescriptor.class) {
			return (ResponseDescriptor) read;
		} else {
			throw new Exception("Incorrect class");
		}
	}
	public ResponseDescriptor sendRequestWithTimeOut(RequestDescriptor request, int timeOut) throws java.net.SocketException, Exception {
		Socket connectionSocket = null;
		synchronized ( socketCreationLock ){
			connectionSocket = new Socket(hostname, port);
			connectionSocket.setSoTimeout(timeOut);
		}
		ObjectOutputStream streamOut = new ObjectOutputStream(connectionSocket.getOutputStream());
		ObjectInputStream streamIn = new ObjectInputStream(connectionSocket.getInputStream());
		
		streamOut.writeObject(request);
		Object read = streamIn.readObject();
		
		connectionSocket.close();
		
		if (read.getClass() == ResponseDescriptor.class) {
			return (ResponseDescriptor) read;
		} else {
			throw new Exception("Incorrect class");
		}
	}
	
	public String getHostname() {
		return this.hostname; //connectionSocket.getInetAddress().getHostName();
	}
	
	public int getPort() {
		return this.port; //connectionSocket.getPort();
	}
	
	public boolean isConnected() {
		return true; //connectionSocket.isConnected();
	}
}
