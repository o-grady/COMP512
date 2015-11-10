package middleware;

import server.TransactionNotActiveException;
import shared.AbstractTTLThread;

public class MiddlewareActiveTransactionThread extends AbstractTTLThread<TransactionDescriptor> {
	
	public MiddlewareActiveTransactionThread() {
		super();
	}
	
	@Override
	protected long getLastActive(int transactionID) throws TransactionNotActiveException {
		if (this.activeTransactions.containsKey(transactionID)) {
			return this.activeTransactions.get(transactionID).lastActive;
		} else {
			throw new TransactionNotActiveException();
		}
	}

	@Override
	protected void abortTransaction(int transactionID)
			throws TransactionNotActiveException {
		this.activeTransactions.remove(transactionID);
	}

	public void add(int transactionID) {
		this.add(transactionID, new TransactionDescriptor());
	}
}
