package middleware;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import shared.RequestDescriptor;
import shared.ResponseDescriptor;

public class ServerConnection {
	
	private Socket connectionSocket;
	private ObjectInputStream streamIn;
	private ObjectOutputStream streamOut;
	
	public ServerConnection(String hostname, int port) throws IOException {
		this.connectionSocket = new Socket(hostname, port);
		this.streamIn = new ObjectInputStream(connectionSocket.getInputStream());
		this.streamOut = new ObjectOutputStream(connectionSocket.getOutputStream());
	}
	
	public ResponseDescriptor sendRequest(RequestDescriptor request) throws Exception {
		streamOut.writeObject(request);
		Object read = streamIn.readObject();
		if (read.getClass() == ResponseDescriptor.class) {
			return (ResponseDescriptor) read;
		} else {
			throw new Exception("Incorrect class");
		}
	}
	
	public void closeConnection() throws IOException {
		if (!connectionSocket.isClosed()) {
			connectionSocket.close();
		}
	}

}
