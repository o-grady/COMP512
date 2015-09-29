package shared;

import java.io.Serializable;

public class ResponseDescriptor implements Serializable {

	private static final long serialVersionUID = 8637699970545623702L;
	public String message;
	
	public ResponseDescriptor(String response) {
		this.message = response;
	}

	public ResponseDescriptor() {
		// TODO Auto-generated constructor stub
	}

}