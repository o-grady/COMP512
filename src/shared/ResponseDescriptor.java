package shared;

import java.io.Serializable;

public class ResponseDescriptor implements Serializable {

	private static final long serialVersionUID = 8637699970545623702L;
	
	public Object data;
	public ResponseType responseType;
	
	public String additionalMessage = null;
	
	public ResponseDescriptor(String response) {
		this.responseType = ResponseType.STRING;
		this.data = response;
	}
	public ResponseDescriptor(int response) {
		this.responseType = ResponseType.INTEGER;
		this.data = response;
	}
	public ResponseDescriptor(boolean response) {
		this.responseType = ResponseType.BOOLEAN;
		this.data = response;
	}

	public ResponseDescriptor() {
		this.data = "undefined";
		this.responseType = ResponseType.ERROR;
	}
	
	public ResponseDescriptor(ResponseType rt, String message) {
		this.responseType = rt;
		this.additionalMessage = message;
	}

}