package shared;

public interface CommitLogger {

	void log(LogType logType, int transactionID);

	boolean hasLog(LogType logType, int transactionID);

	int largestTransactionInLog();

}