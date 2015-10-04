package shared;

import java.io.Serializable;

public class ResponseDescriptor implements Serializable {

	private static final long serialVersionUID = 8637699970545623702L;
	public String stringResponse;
	public boolean booleanResponse;
	public int intResponse;
	public String additionalMessage;
	public ResponseDescriptor(String response) {
		this.stringResponse = response;
		this.intResponse = -1;
		this.booleanResponse = false;
		this.additionalMessage = null;
	}
	public ResponseDescriptor(int response) {
		this.stringResponse = null;
		this.intResponse = response;
		this.booleanResponse = false;
		this.additionalMessage = null;
	}
	public ResponseDescriptor(boolean response) {
		this.stringResponse = null;
		this.intResponse = -1;
		this.booleanResponse = response;
		this.additionalMessage = null;
	}

	public ResponseDescriptor() {
		this.stringResponse = null;
		this.intResponse = -1;
		this.booleanResponse = false;
		this.additionalMessage = null;
	}

}