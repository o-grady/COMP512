package server;

public class TransactionAlreadyActiveException extends Exception {

	public TransactionAlreadyActiveException(String string) {
		super(string);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -1584011417854267155L;

}
