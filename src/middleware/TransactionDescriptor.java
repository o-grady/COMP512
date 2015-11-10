package middleware;

import java.util.EnumMap;
import java.util.Map;

public class TransactionDescriptor {
	TransactionDescriptor() {
		super();
		this.hasStarted = new EnumMap<ServerMode, Boolean>(ServerMode.class);
		this.hasStarted.put(ServerMode.CAR, false);
		this.hasStarted.put(ServerMode.FLIGHT, false);
		this.hasStarted.put(ServerMode.ROOM, false);
		this.hasStarted.put(ServerMode.CUSTOMER, false);
		this.lastActive = System.currentTimeMillis();
	}
	public Map<ServerMode, Boolean> hasStarted;
	public long lastActive = 0;
}
