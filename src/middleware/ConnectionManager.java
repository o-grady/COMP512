package middleware;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ConnectionManager {
	
	private Map<ServerMode,ServerConnection> connections;
	
	public ConnectionManager() {
		connections = new HashMap<ServerMode,ServerConnection>();
	}
	
	public boolean addServer(ServerMode mode, String hostname, int port) {
		if (connections.containsKey(mode)) {
			return false;
		} 
		
		try {
			connections.put(mode, new ServerConnection(hostname, port));
			return true;
		}
		catch (Exception ex) {
			return false;
		}
	}
	
	public void removeServer(ServerMode mode) {
		if (!connections.containsKey(mode)) {
			return;
		}
		
		try {
			connections.get(mode).closeConnection();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			connections.remove(mode);
		}
	}
	
	public ServerConnection getConnection(ServerMode mode) {
		return connections.get(mode);
	}
	
	public boolean modeIsConnected(ServerMode mode) {
		if (connections.containsKey(mode)) {
			if (connections.get(mode).isConnected()) {
				return true;
			} else {
				connections.remove(mode);
			}
		}
		return false;
	}
	
	public Collection<ServerConnection> getAllConnections() {
		return connections.values();
	}
}
