package shared;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import server.TransactionNotActiveException;

public abstract class AbstractTTLThread<T> extends Thread {

	public AbstractTTLThread() {
		this.activeTransactions = new ConcurrentHashMap<Integer, T>();
		this.hangingTransactions = new ArrayList<Integer>();
	}

	protected Map<Integer, T> activeTransactions;
	private List<Integer> hangingTransactions;
	private static final long TTL_INTERVAL = 60 * 1000;

	public void run() {
		while (true) {
			long curTime = System.currentTimeMillis();
			for (int i : this.getAllActiveTransactions()) {
				try {
					if (this.transactionLastActive(i) + TTL_INTERVAL < curTime) {
						this.abortTransaction(i);
					}
				} catch (TransactionNotActiveException ex) {
					System.out.println("WARN: transaction " + i
							+ " was removed before it could be timed out");
				}
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				break;
			}
		}
	}
	//used for 2PC, client can't timeout when waiting for vote result
	public void hangTransaction(int transactionID){
		synchronized(hangingTransactions){
			hangingTransactions.add(transactionID);
		}
	}
	public void unhangTransaction(int transactionID){
		synchronized(hangingTransactions){
			hangingTransactions.remove(transactionID);
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
				System.out
						.println("WARN: Cannot remove inactive transactionID: "
								+ transactionID);
			}
		}
	}

	public void add(int transactionID, T entry) {
		synchronized (activeTransactions) {
			if (activeTransactions.containsKey(transactionID)) {
				System.out.println("WARN: Cannot add existing transactionID: "
						+ transactionID);
			} else {
				this.activeTransactions.put(transactionID, entry);
			}
		}
	}

	public void signalTransaction(int transactionID, T entry) {
		synchronized (activeTransactions) {
			if (activeTransactions.containsKey(transactionID)) {
				this.activeTransactions.put(transactionID, entry);
			} else {
				System.out
						.println("WARN: Cannot signal inactive transactionID: "
								+ transactionID);
			}
		}
	}

	public long transactionLastActive(int transactionID)
			throws TransactionNotActiveException {
		//if transaction is hanging, return current time so transaction is always active.
		synchronized (hangingTransactions) {
			if (hangingTransactions.contains(transactionID)) {
				return System.currentTimeMillis(); 
			}
		}
		synchronized (activeTransactions) {
			if (activeTransactions.containsKey(transactionID)) {
				return this.getLastActive(transactionID);
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
	
	public T get(int transactionID) {
		synchronized (activeTransactions) {
			return activeTransactions.get(transactionID);
		}
	}

	protected abstract long getLastActive(int transactionID) throws TransactionNotActiveException;

	protected abstract void abortTransaction(int transactionID) throws TransactionNotActiveException;

}
