package testclient;

import java.io.IOException;
import java.util.Vector;

import shared.RequestDescriptor;
import shared.RequestType;
import shared.ResponseDescriptor;
import shared.ServerConnection;

public class TestClient {
	private static ServerConnection middlewareConnection;
	public static void main(String args[]){
		int successCount = 0;
		int failCount = 0;
		if(args.length != 2){
			System.out.println("Expecting 2 parameters");
		}
		try {
			middlewareConnection = new ServerConnection(args[0], Integer.parseInt(args[1]));
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
		RequestDescriptor r;
		//RESERVE WHEN EMPTY
		System.out.println("Sending ITINERARY, expecting false");
		r = new RequestDescriptor(RequestType.ITINERARY);
		r.id = 1;
		r.customerNumber = 1;
		r.flightNumbers = new Vector<Integer>();
		r.location = "Montreal";
		r.car = false;
		r.room = false;
		if(TestClient.sendRequest(r).booleanResponse == false){
            System.out.println("Success!");
            successCount++;
        }else{
            System.out.println("Fail!");
            failCount++;
        }
		
		System.out.println("Sending NEWCUSTOMERID, expecting true");
		r = new RequestDescriptor(RequestType.NEWCUSTOMERID);
		r.id = 1;
		r.customerNumber = 1;
		TestClient.sendRequest(r);
		//QUERY, RESERVE, ADD 1, QUERY, RESERVE, RESERVE

		System.out.println("Sending QUERYCAR, expecting 0");
		r = new RequestDescriptor(RequestType.QUERYCAR);
		r.id = 1;
		r.location = "Montreal";
		if(TestClient.sendRequest( r ).intResponse == 0){
            System.out.println("Success!");
            successCount++;
        }else{
            System.out.println("Fail!");
            failCount++;
        }
		
		System.out.println("Sending RESERVECAR, expecting false");
		r = new RequestDescriptor(RequestType.RESERVECAR);
		r.id = 1;
		r.customerNumber = 1;
		r.location = "Montreal";
		if(TestClient.sendRequest(r).booleanResponse == false){
            System.out.println("Success!");
            successCount++;
        }else{
            System.out.println("Fail!");
            failCount++;
        }
		
		System.out.println("Sending ADDCAR, expecting true");
		r = new RequestDescriptor(RequestType.NEWCAR);
		r.id = 1;
		r.location = "Montreal";
		r.numCars = 1;
		r.price = 5;
		if(TestClient.sendRequest(r).booleanResponse == true){
            System.out.println("Success!");
            successCount++;
        }else{
            System.out.println("Fail!");
            failCount++;
        }
		
		System.out.println("Sending QUERYCAR, expecting 1");
		r = new RequestDescriptor(RequestType.QUERYCAR);
		r.id = 1;
		r.location = "Montreal";
		if(TestClient.sendRequest( r ).intResponse == 1){
            System.out.println("Success!");
            successCount++;
        }else{
            System.out.println("Fail!");
            failCount++;
        }
		
		System.out.println("Sending RESERVECAR, expecting true");
		r = new RequestDescriptor(RequestType.RESERVECAR);
		r.id = 1;
		r.customerNumber = 1;
		r.location = "Montreal";
		if(TestClient.sendRequest(r).booleanResponse == true){
            System.out.println("Success!");
            successCount++;
        }else{
            System.out.println("Fail!");
            failCount++;
        }
		
		System.out.println("Sending QUERYCAR, expecting 0");
		r = new RequestDescriptor(RequestType.QUERYCAR);
		r.id = 1;
		r.location = "Montreal";
		if(TestClient.sendRequest( r ).intResponse == 0){
            System.out.println("Success!");
            successCount++;
        }else{
            System.out.println("Fail!");
            failCount++;
        }
		
		System.out.println("Sending RESERVECAR, expecting false");
		r = new RequestDescriptor(RequestType.RESERVECAR);
		r.id = 1;
		r.customerNumber = 1;
		r.location = "Montreal";
		if(TestClient.sendRequest(r).booleanResponse == false){
            System.out.println("Success!");
            successCount++;
        }else{
            System.out.println("Fail!");
            failCount++;
        }
		//QUERY, RESERVE, ADD 1, QUERY, RESERVE, RESERVE

		System.out.println("Sending QUERYFLIGHT, expecting 0");
		r = new RequestDescriptor(RequestType.QUERYFLIGHT);
		r.id = 1;
		r.location = "Montreal";
		if(TestClient.sendRequest( r ).intResponse == 0){
            System.out.println("Success!");
            successCount++;
        }else{
            System.out.println("Fail!");
            failCount++;
        }
		
		System.out.println("Sending RESERVEFLIGHT, expecting false");
		r = new RequestDescriptor(RequestType.RESERVEFLIGHT);
		r.id = 1;
		r.customerNumber = 1;
		r.location = "Montreal";
		if(TestClient.sendRequest(r).booleanResponse == false){
            System.out.println("Success!");
            successCount++;
        }else{
            System.out.println("Fail!");
            failCount++;
        }
		
		System.out.println("Sending ADDFLIGHT, expecting true");
		r = new RequestDescriptor(RequestType.NEWFLIGHT);
		r.id = 1;
		r.location = "Montreal";
		r.numSeats = 1;
		r.price = 5;
		if(TestClient.sendRequest(r).booleanResponse == true){
            System.out.println("Success!");
            successCount++;
        }else{
            System.out.println("Fail!");
            failCount++;
        }
		
		System.out.println("Sending QUERYFLIGHT, expecting 1");
		r = new RequestDescriptor(RequestType.QUERYFLIGHT);
		r.id = 1;
		r.location = "Montreal";
		if(TestClient.sendRequest( r ).intResponse == 1){
            System.out.println("Success!");
            successCount++;
        }else{
            System.out.println("Fail!");
            failCount++;
        }
		
		System.out.println("Sending RESERVEFLIGHT, expecting true");
		r = new RequestDescriptor(RequestType.RESERVEFLIGHT);
		r.id = 1;
		r.customerNumber = 1;
		r.location = "Montreal";
		if(TestClient.sendRequest(r).booleanResponse == true){
            System.out.println("Success!");
            successCount++;
        }else{
            System.out.println("Fail!");
            failCount++;
        }
		
		System.out.println("Sending QUERYFLIGHT, expecting 0");
		r = new RequestDescriptor(RequestType.QUERYFLIGHT);
		r.id = 1;
		r.location = "Montreal";
		if(TestClient.sendRequest( r ).intResponse == 0){
            System.out.println("Success!");
            successCount++;
        }else{
            System.out.println("Fail!");
            failCount++;
        }
		
		System.out.println("Sending RESERVEFLIGHT, expecting false");
		r = new RequestDescriptor(RequestType.RESERVEFLIGHT);
		r.id = 1;
		r.customerNumber = 1;
		r.location = "Montreal";
		if(TestClient.sendRequest(r).booleanResponse == false){
            System.out.println("Success!");
            successCount++;
        }else{
            System.out.println("Fail!");
            failCount++;
        }
		//QUERY, RESERVE, ADD 1, QUERY, RESERVE, RESERVE

		System.out.println("Sending QUERYROOM, expecting 0");
		r = new RequestDescriptor(RequestType.QUERYROOM);
		r.id = 1;
		r.location = "Montreal";
		if(TestClient.sendRequest( r ).intResponse == 0){
            System.out.println("Success!");
            successCount++;
        }else{
            System.out.println("Fail!");
            failCount++;
        }
		
		System.out.println("Sending RESERVEROOM, expecting false");
		r = new RequestDescriptor(RequestType.RESERVEROOM);
		r.id = 1;
		r.customerNumber = 1;
		r.location = "Montreal";
		if(TestClient.sendRequest(r).booleanResponse == false){
            System.out.println("Success!");
            successCount++;
        }else{
            System.out.println("Fail!");
            failCount++;
        }
		
		System.out.println("Sending ADDROOM, expecting true");
		r = new RequestDescriptor(RequestType.NEWROOM);
		r.id = 1;
		r.location = "Montreal";
		r.numRooms = 1;
		r.price = 5;
		if(TestClient.sendRequest(r).booleanResponse == true){
            System.out.println("Success!");
            successCount++;
        }else{
            System.out.println("Fail!");
            failCount++;
        }
		
		System.out.println("Sending QUERYROOM, expecting 1");
		r = new RequestDescriptor(RequestType.QUERYROOM);
		r.id = 1;
		r.location = "Montreal";
		if(TestClient.sendRequest( r ).intResponse == 1){
            System.out.println("Success!");
            successCount++;
        }else{
            System.out.println("Fail!");
            failCount++;
        }
		
		System.out.println("Sending RESERVEROOM, expecting true");
		r = new RequestDescriptor(RequestType.RESERVEROOM);
		r.id = 1;
		r.customerNumber = 1;
		r.location = "Montreal";
		if(TestClient.sendRequest(r).booleanResponse == true){
            System.out.println("Success!");
            successCount++;
        }else{
            System.out.println("Fail!");
            failCount++;
        }
		
		System.out.println("Sending QUERYROOM, expecting 0");
		r = new RequestDescriptor(RequestType.QUERYROOM);
		r.id = 1;
		r.location = "Montreal";
		if(TestClient.sendRequest( r ).intResponse == 0){
            System.out.println("Success!");
            successCount++;
        }else{
            System.out.println("Fail!");
            failCount++;
        }
		
		System.out.println("Sending RESERVEROOM, expecting false");
		r = new RequestDescriptor(RequestType.RESERVEROOM);
		r.id = 1;
		r.customerNumber = 1;
		r.location = "Montreal";
		if(TestClient.sendRequest(r).booleanResponse == false){
            System.out.println("Success!");
            successCount++;
        }else{
            System.out.println("Fail!");
            failCount++;
        }
		System.out.println("Total tests: " + (successCount + failCount));
		System.out.println("Successful tests: "+successCount);
		System.out.println("Failed tests: "+ failCount);
		

	}
	public static ResponseDescriptor sendRequest(RequestDescriptor req){
		ResponseDescriptor response = null;
		String message = null;
		try {
			response = middlewareConnection.sendRequest(req);
			if(response.stringResponse != null){
				message = response.stringResponse;
			}else if(response.intResponse != -1){
				message = "" + response.intResponse;
			}else{
				message = "" + response.booleanResponse;
			}
			if(response.additionalMessage != null){
				message += ", Message: " + response.additionalMessage;
			}
			return response;
		} catch (Exception e) {
			message = "Test Client: excpetion caught sending request";
			e.printStackTrace();
			return response;
		}
	}
}
