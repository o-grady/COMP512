package shared; 
public enum RequestType {
	NEWCAR,
    NEWFLIGHT,
    NEWROOM,
    NEWCUSTOMER,
    DELETEFLIGHT,
    DELETECAR,
    DELETEROOM,
    DELETECUSTOMER,
    QUERYFLIGHT,
    QUERYCAR,
    QUERYROOM,
    QUERYCUSTOMER,
    QUERYFLIGHTPRICE,
    QUERYCARPRICE,
    QUERYROOMPRICE,
    RESERVEFLIGHT,
    RESERVECAR,
    RESERVEROOM,
    ITINERARY,
    NEWCUSTOMERID,
    STARTTXN,
    COMMIT,
    ABORT,
    SHUTDOWN,
    CRASH,
 // internal signals, do not expose to client
    ENLIST,
    ABORTALL,
    PREPARE,
    SELFDESTRUCT,
    TWOPHASECOMMITVOTERESP
}
