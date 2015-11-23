package shared.LockManager;

public class WaitObj extends DataObj
{
    protected Thread thread = null;
    private boolean stopped = false;
    // The data members inherited are 
    // XObj:: protected int xid;
    // TrxnObj:: protected String strData;
    // TrxnObj:: protected int lockType;
    
    WaitObj() {
        super();
        thread = null;
    }
    
    WaitObj(int xid, String strData, int lockType) {
        super(xid, strData, lockType);
        thread = null;
    }
    
    WaitObj(int xid, String strData, int lockType, Thread thread) {
        super(xid, strData, lockType);
        this.thread = thread;
    }
    
    public Thread getThread() {
        return this.thread;
    }
    public boolean isStopped(){
    	return this.stopped;
    }
    public void setStopped(boolean stop){
    	this.stopped = stop;
    }
}
