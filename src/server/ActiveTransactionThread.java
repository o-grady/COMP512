package server;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ActiveTransactionThread extends Thread {
	private Map<Integer, Long> activeTransactions;
	private static final long TTL_INTERVAL = 60 * 1000;
	private TransactionManager tm;

	ActiveTransactionThread(TransactionManager tm) {
		super();
		this.tm = tm;
		this.activeTransactions = new HashMap<Integer, Long>();
	}

	public void run() {
		while(true) {
			long curTime = System.currentTimeMillis();
			for (int i : this.getAllActiveTransactions()) {
				try {
					if (this.transactionLastActive(i) + TTL_INTERVAL < curTime) {
						this.tm.abortTransaction(i);
					}
				} catch (TransactionNotActiveException ex) {
					System.out.println("WARN: transaction " + i + " was removed before it could be timed out");
				}
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				break;
			}
		}
	}

	public Set<Integer> getAllActiveTransactions() {
		return new HashSet<Integer>(activeTransactions.keySet());
	}

	public void remove(int transactionID) {
		synchronized (activeTransactions) {
			if (activeTransactions.containsKey(transactionID)) {
				activeTransactions.remove(transactionID);
			} else {
				System.out.println("WARN: Cannot remove inactive transactionID: "+ transactionID);
			}
		}
	}

	public void add(int transactionID) {
		synchronized (activeTransactions) {
			if (activeTransactions.containsKey(transactionID)) {
				System.out.println("WARN: Cannot add existing transactionID: "+ transactionID);
			} else {
				activeTransactions.put(transactionID,
						System.currentTimeMillis());
			}
		}
	}

	public void signalTransaction(int transactionID) {
		synchronized (activeTransactions) {
			if (activeTransactions.containsKey(transactionID)) {
				activeTransactions.put(transactionID,
						System.currentTimeMillis());
			} else {
				System.out.println("WARN: Cannot signal inactive transactionID: "+ transactionID);
			}
		}
	}

	public long transactionLastActive(int transactionID)
			throws TransactionNotActiveException {
		synchronized (activeTransactions) {
			if (activeTransactions.containsKey(transactionID)) {
				return activeTransactions.get(transactionID);
			} else {
				throw new TransactionNotActiveException();
			}
		}
	}
	
	public boolean contains(int transactionID) {
		synchronized (activeTransactions) {
			return activeTransactions.containsKey(transactionID);
		}
	}
}
