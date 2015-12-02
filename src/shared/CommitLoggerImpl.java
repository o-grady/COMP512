package shared;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class CommitLoggerImpl implements CommitLogger {
	private PrintWriter pw;
	private ArrayList<Tuple<LogType, Integer>> logAsList = new ArrayList<Tuple<LogType, Integer>>();
	public CommitLoggerImpl(String logLocation){
		try {
			BufferedReader br = new BufferedReader(new FileReader(logLocation));
			String logEntry = null;
			while( (logEntry = br.readLine()) != null){
				String[] splitLogEntry = logEntry.split(",");
				logAsList.add(new Tuple<LogType, Integer>(LogType.valueOf(splitLogEntry[0]), Integer.parseInt(splitLogEntry[1])));
			}
			br.close();
		} catch (FileNotFoundException e1) {
			System.out.println("CommitLoggerImpl: can't read old commits, file not found");
		} catch (IOException e) {
			System.out.println("CommitLoggerImpl: Problem reading from file");
		}
		try {
			this.pw = new PrintWriter(new BufferedWriter(new FileWriter(logLocation, true)));
		} catch (IOException e) {
			System.out.println("CommitLoggerImpl: Cant open PrintWriter");
			this.pw = null;
		}
	}
	@Override
	public void log(LogType logType, int transactionID){
		pw.println(logType.toString() + "," + transactionID);
		pw.flush();
		logAsList.add(new Tuple<LogType, Integer>(logType, transactionID));
	}
	@Override
	public boolean hasLog(LogType logType, int transactionID){
		Tuple<LogType, Integer> tuple = new Tuple<LogType, Integer>(logType, transactionID);
		for(int i = 0 ; i < logAsList.size() ; i++){
			if(logAsList.get(i).equals(tuple)){
				return true;
			}
		}
		return false;
	}
	@Override
	public int largestTransactionInLog(){
		int largestTransaction = 0;
		for(int i = 0 ; i < logAsList.size() ; i++){
			int transactionID = -1;
			if((transactionID = logAsList.get(i).y) > largestTransaction){
				largestTransaction = transactionID;
			}
		}
		return largestTransaction;
	}

	@Override
	public int mostRecentlyCommittedTransaction(){
		for( int i = logAsList.size() - 1 ; i >= 0 ; i--){
			Tuple<LogType, Integer> item = logAsList.get(i);
			if(item.x == LogType.COMMITTED){
				return item.y;
			}
		}
		return 0;
	} 

	//TransactionID of most recent commit since start of param transactionID. Return -1 if transactionID wasnt started or no commit has happened since.
	@Override
	public int mostRecentCommitSinceTransactionStart(int transactionID){
		Tuple<LogType, Integer> startTuple = new Tuple<LogType, Integer>(LogType.STARTED, transactionID);
		int indexOfStart = this.logAsList.lastIndexOf(startTuple);
		int mostRecentCommitIndex = -1;
		if(indexOfStart == -1){
			return -1;
		}
		for(int i = indexOfStart + 1 ; i < logAsList.size() ; i++){
			if(logAsList.get(i).x == LogType.COMMITTED){
				mostRecentCommitIndex = i;
			}
		}
		if(mostRecentCommitIndex == -1){
			return -1;
		}
		return logAsList.get(mostRecentCommitIndex).y;
	}
	public static class Tuple<X, Y> { 
		public final X x; 
		public final Y y; 
		public Tuple(X x, Y y) { 
			this.x = x; 
			this.y = y; 
		}
		@Override
		public int hashCode(){
			return x.hashCode() + y.hashCode();
		}
		@Override
		public boolean equals(Object other){
			if( other instanceof Tuple<?, ?> ){
				Tuple<?, ?> otherTuple = (Tuple<?, ?>) other;
				return otherTuple.x.equals(this.x) && otherTuple.y.equals(this.y);
			}else{
				return false;
			}
		}
	} 	
}
