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
		try {
			tm.abortTransaction(transactionID);
		} catch (TransactionBlockingException e) {
			//this should not be hit, if transaction is blocking then TTL will not abort on timeout because timeout cannot occur when blocking
		}
	}


	public void add(int transactionID) {
		this.add(transactionID, System.currentTimeMillis());
	}


	public void signalTransaction(int transactionID) {
		this.signalTransaction(transactionID, System.currentTimeMillis());
	}
}
