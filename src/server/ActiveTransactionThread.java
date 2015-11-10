package server;

import shared.AbstractTTLThread;

public class ActiveTransactionThread extends AbstractTTLThread<Long> {
	private TransactionManager tm;

	ActiveTransactionThread(TransactionManager tm) {
		super();
		this.tm = tm;
	}

	@Override
	protected long getLastActive(int transactionID) throws TransactionNotActiveException {
		if (this.activeTransactions.containsKey(transactionID)) {
			return this.activeTransactions.get(transactionID);
		} else {
			throw new TransactionNotActiveException();
		}
	}

	@Override
	protected void abortTransaction(int transactionID) throws TransactionNotActiveException {
		tm.abortTransaction(transactionID);
	}


	public void add(int transactionID) {
		this.add(transactionID, System.currentTimeMillis());
	}


	public void signalTransaction(int transactionID) {
		this.signalTransaction(transactionID, System.currentTimeMillis());
	}
}
