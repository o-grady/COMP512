package middleware;

import javax.jws.WebService;
import javax.jws.WebMethod;

@WebService
public interface MiddlewareInterface {
	
	/* Handles the command and returns the output to display. */
	@WebMethod
	public String handleAndReply(String command);
}
