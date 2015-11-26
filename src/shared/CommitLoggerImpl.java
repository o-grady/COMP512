package shared;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class CommitLoggerImpl implements CommitLogger {
	private PrintWriter pw;
	public CommitLoggerImpl(String logLocation){
		try {
			this.pw = new PrintWriter(new BufferedWriter(new FileWriter(logLocation, true)));
		} catch (IOException e) {
			System.out.println("Cant open PrintWriter");
			this.pw = null;
		}
	}
	@Override
	public void log(LogType logType, int transactionID){
		pw.println(logType.toString() + "," + transactionID);
		pw.flush();
	}
	@Override
	public boolean hasLog(LogType logType, int transactionID){
		//returns if log exist
		return false;
	}
	@Override
	public int largestTransactionInLog(){
		//returns largest transactionID seen, can be used for looping over transactions of recovery.
		return -1;
	}
}
