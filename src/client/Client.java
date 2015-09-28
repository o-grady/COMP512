package client;

import java.io.*;
import java.net.Socket;

import shared.RequestDescriptor;
import shared.RequestType;
import shared.ResponseDescriptor;


public class Client {
	
	Socket clientSocket;
	ObjectOutputStream outToServer;
	ObjectInputStream inFromServer;

    public Client(String serviceHost, int servicePort) 
    throws Exception {
        clientSocket = new Socket(serviceHost, servicePort);
        outToServer = new ObjectOutputStream(clientSocket.getOutputStream());
        inFromServer = new ObjectInputStream(clientSocket.getInputStream());
    }

    public static void main(String[] args) {
        try {
        
            if (args.length != 3) {
                System.out.println("Usage: MyClient <service-host> <service-port>");
                System.exit(-1);
            }
            
            String serviceHost = args[0];
            int servicePort = Integer.parseInt(args[1]);
            
            Client client = new Client(serviceHost, servicePort);
            
            client.run();
            
        } catch(Exception e) {
            e.printStackTrace();
        }
    }


    public void run() {
        String command = null;
        String[] arguments;

        BufferedReader stdin = 
                new BufferedReader(new InputStreamReader(System.in));
        
        System.out.println("Client Interface");
        System.out.println("Type \"help\" for list of supported commands");

        while (true) {
        
            try {
                //read the next command
                command = stdin.readLine();
            }
            catch (IOException io) {
                System.out.println("Unable to read from standard in");
                System.exit(1);
            }
            
            //remove heading and trailing white space
            command = command.trim();
            arguments = command.split(",");
            
            // verify that there are arguments. 
            if (arguments.length <= 0) {
            	continue;
            }
            
            if (arguments[0].compareToIgnoreCase("QUIT") == 0) {
            	return;
            } else if (arguments[0].compareToIgnoreCase("HELP") == 0) {
                if (arguments.length == 1)   //command was "help"
                    listCommands();
                else if (arguments.length == 2)  //command was "help <commandname>"
                    listSpecific((String) arguments[1]);
                else  //wrong use of help command
                    System.out.println("Improper use of help command. Type help or help, <commandname>");
            } else if (commandExists(arguments[0].toUpperCase())) {
            	RequestType rt = RequestType.valueOf(arguments[0].toUpperCase());
            	RequestDescriptor request;
            	ResponseDescriptor reply;
            	
            	if (rt != RequestType.ITINERARY) {
            		try {
	            		request = buildRequest(rt, arguments);
	            		reply = sendRequest(request);
	            		System.out.println("Success: " + reply.message);
            		}
            		catch (Exception ex) {
            			System.out.println("Failure: " + ex.getMessage());
            			ex.printStackTrace();
            		}
            	}
            } else {
            	System.out.println("The interface does not support this command.");
            }
        }
    }
    
    public RequestDescriptor buildRequest(RequestType rt, String[] arguments) throws Exception {
    	RequestDescriptor rd = new RequestDescriptor(rt);
    	switch(rt) {
		case DELETECAR:
			System.out.println("Deleting the cars from a particular location  using id: " + arguments[1]);
            System.out.println("car Location: " + arguments[2]);

            rd.id = getInt(arguments[1]);
            rd.location = getString(arguments[2]);
			break;
		case DELETECUSTOMER:
            System.out.println("Deleting a customer from the database using id: " + arguments[1]);
            System.out.println("Customer id: " + arguments[2]);

                rd.id = getInt(arguments[1]);
                rd.customerNumber = getInt(arguments[2]);
			break;
		case DELETEFLIGHT:
			System.out.println("Deleting a flight using id: " + arguments[1]);
            System.out.println("Flight Number: " + arguments[2]);

            rd.id = getInt(arguments[1]);
            rd.flightNumber = getInt(arguments[2]);
			break;
		case DELETEROOM:
            System.out.println("Deleting all rooms from a particular location  using id: " + arguments[1]);
            System.out.println("room Location: " + arguments[2]);

            rd.id = getInt(arguments[1]);
            rd.location = getString(arguments[2]);
			break;
		case ITINERARY:
			break;
		case NEWCAR:
            System.out.println("Adding a new car using id: " + arguments[1]);
            System.out.println("car Location: " + arguments[2]);
            System.out.println("Add Number of cars: " + arguments[3]);
            System.out.println("Set Price: " + arguments[4]);

            rd.id = getInt(arguments[1]);
            rd.location = getString(arguments[2]);
            rd.numCars = getInt(arguments[3]);
            rd.price = getInt(arguments[4]);
			break;
		case NEWCUSTOMER:
			System.out.println("Adding a new Customer using id: " + arguments[1]);
			
			rd.id = getInt(arguments[1]);
			break;
		case NEWCUSTOMERID:
            System.out.println("Adding a new Customer using id: "
                    + arguments[1]  +  " and cid "  + arguments[2]);

            rd.id = getInt(arguments[1]);
            rd.customerNumber = getInt(arguments[2]);
			break;
		case NEWFLIGHT:
            System.out.println("Adding a new Flight using id: " + arguments[1]);
            System.out.println("Flight number: " + arguments[2]);
            System.out.println("Add Flight Seats: " + arguments[3]);
            System.out.println("Set Flight Price: " + arguments[4]);

            rd.id = getInt(arguments[1]);
            rd.flightNumber = getInt(arguments[2]);
            rd.numSeats = getInt(arguments[3]);
            rd.flightPrice = getInt(arguments[4]);
			break;
		case NEWROOM:
            System.out.println("Adding a new room using id: " + arguments[1]);
            System.out.println("room Location: " + arguments[2]);
            System.out.println("Add Number of rooms: " + arguments[3]);
            System.out.println("Set Price: " + arguments[4]);

            rd.id = getInt(arguments[1]);
            rd.location = getString(arguments[2]);
            rd.numRooms = getInt(arguments[3]);
            rd.price = getInt(arguments[4]);
			break;
		case QUERYCAR:
            System.out.println("Querying a car location using id: " + arguments[1]);
            System.out.println("car location: " + arguments[2]);
            
            rd.id = getInt(arguments[1]);
            rd.location = getString(arguments[2]);
			break;
		case QUERYCARPRICE:
            System.out.println("Querying a car price using id: " + arguments[1]);
            System.out.println("car location: " + arguments[2]);

            rd.id = getInt(arguments[1]);
            rd.location = getString(arguments[2]);
			break;
		case QUERYCUSTOMER:
            System.out.println("Querying Customer information using id: " + arguments[1]);
            System.out.println("Customer id: " + arguments[2]);

            rd.id = getInt(arguments[1]);
            rd.customerNumber = getInt(arguments[2]);
			break;
		case QUERYFLIGHT:
            System.out.println("Querying a flight using id: " + arguments[1]);
            System.out.println("Flight number: " + arguments[2]);

            rd.id = getInt(arguments[1]);
            rd.flightNumber = getInt(arguments[2]);
			break;
		case QUERYFLIGHTPRICE:
            System.out.println("Querying a flight Price using id: " + arguments[1]);
            System.out.println("Flight number: " + arguments[2]);

            rd.id = getInt(arguments[1]);
            rd.flightNumber = getInt(arguments[2]);
			break;
		case QUERYROOM:
            System.out.println("Querying a room location using id: " + arguments[1]);
            System.out.println("room location: " + arguments[2]);

            rd.id = getInt(arguments[1]);
            rd.location = getString(arguments[2]);
			break;
		case QUERYROOMPRICE:
            System.out.println("Querying a room price using id: " + arguments[1]);
            System.out.println("room Location: " + arguments[2]);

            rd.id = getInt(arguments[1]);
            rd.location = getString(arguments[2]);
			break;
		case RESERVECAR:
            System.out.println("Reserving a car at a location using id: " + arguments[1]);
            System.out.println("Customer id: " + arguments[2]);
            System.out.println("Location: " + arguments[3]);

            rd.id = getInt(arguments[1]);
            rd.customerNumber = getInt(arguments[2]);
            rd.location = getString(arguments[3]);
			break;
		case RESERVEFLIGHT:
            System.out.println("Reserving a seat on a flight using id: " + arguments[1]);
            System.out.println("Customer id: " + arguments[2]);
            System.out.println("Flight number: " + arguments[3]);

            rd.id = getInt(arguments[1]);
            rd.customerNumber = getInt(arguments[2]);
            rd.flightNumber = getInt(arguments[3]);
			break;
		case RESERVEROOM:
            System.out.println("Reserving a room at a location using id: " + arguments[1]);
            System.out.println("Customer id: " + arguments[2]);
            System.out.println("Location: " + arguments[3]);

            rd.id = getInt(arguments[1]);
            rd.customerNumber = getInt(arguments[2]);
            rd.flightNumber = getInt(arguments[3]);
			break;
    	}
    	return rd;
    }
    
    public ResponseDescriptor sendRequest(RequestDescriptor rd) throws Exception {
		try {
			outToServer.writeObject(rd);
			Object reply = inFromServer.readObject();
			if (reply.getClass() == ResponseDescriptor.class) {
				System.out.println("Success: " + reply);
				return (ResponseDescriptor) reply;
			} else {
				throw new Exception("Incorrect class");
			}
		} catch (Exception ex) {
            System.out.println("EXCEPTION: ");
            System.out.println(ex.getMessage());
            ex.printStackTrace();
            throw ex;
		}
    }
    
    public boolean commandExists(String command) {
    	try {
    		RequestType.valueOf(command.toUpperCase());
    	} catch (IllegalArgumentException ex) {
    		return false;
    	}
    	return true;
    }
    
    public int findChoice(String argument) {
        if (argument.compareToIgnoreCase("help") == 0)
            return 1;
        else if (argument.compareToIgnoreCase("newflight") == 0)
            return 2;
        else if (argument.compareToIgnoreCase("newcar") == 0)
            return 3;
        else if (argument.compareToIgnoreCase("newroom") == 0)
            return 4;
        else if (argument.compareToIgnoreCase("newcustomer") == 0)
            return 5;
        else if (argument.compareToIgnoreCase("deleteflight") == 0)
            return 6;
        else if (argument.compareToIgnoreCase("deletecar") == 0)
            return 7;
        else if (argument.compareToIgnoreCase("deleteroom") == 0)
            return 8;
        else if (argument.compareToIgnoreCase("deletecustomer") == 0)
            return 9;
        else if (argument.compareToIgnoreCase("queryflight") == 0)
            return 10;
        else if (argument.compareToIgnoreCase("querycar") == 0)
            return 11;
        else if (argument.compareToIgnoreCase("queryroom") == 0)
            return 12;
        else if (argument.compareToIgnoreCase("querycustomer") == 0)
            return 13;
        else if (argument.compareToIgnoreCase("queryflightprice") == 0)
            return 14;
        else if (argument.compareToIgnoreCase("querycarprice") == 0)
            return 15;
        else if (argument.compareToIgnoreCase("queryroomprice") == 0)
            return 16;
        else if (argument.compareToIgnoreCase("reserveflight") == 0)
            return 17;
        else if (argument.compareToIgnoreCase("reservecar") == 0)
            return 18;
        else if (argument.compareToIgnoreCase("reserveroom") == 0)
            return 19;
        else if (argument.compareToIgnoreCase("itinerary") == 0)
            return 20;
        else if (argument.compareToIgnoreCase("quit") == 0)
            return 21;
        else if (argument.compareToIgnoreCase("newcustomerid") == 0)
            return 22;
        else
            return 666;
    }

    public void listCommands() {
        System.out.println("\nWelcome to the client interface provided to test your project.");
        System.out.println("Commands accepted by the interface are: ");
        System.out.println("help");
        System.out.println("newflight\nnewcar\nnewroom\nnewcustomer\nnewcustomerid\ndeleteflight\ndeletecar\ndeleteroom");
        System.out.println("deletecustomer\nqueryflight\nquerycar\nqueryroom\nquerycustomer");
        System.out.println("queryflightprice\nquerycarprice\nqueryroomprice");
        System.out.println("reserveflight\nreservecar\nreserveroom\nitinerary");
        System.out.println("quit");
        System.out.println("\ntype help, <commandname> for detailed info (note the use of comma).");
    }


    public void listSpecific(String command) {
        System.out.print("Help on: ");
        switch(findChoice(command)) {
            case 1:
            System.out.println("Help");
            System.out.println("\nTyping help on the prompt gives a list of all the commands available.");
            System.out.println("Typing help, <commandname> gives details on how to use the particular command.");
            break;

            case 2:  //new flight
            System.out.println("Adding a new Flight.");
            System.out.println("Purpose: ");
            System.out.println("\tAdd information about a new flight.");
            System.out.println("\nUsage: ");
            System.out.println("\tnewflight, <id>, <flightnumber>, <numSeats>, <flightprice>");
            break;
            
            case 3:  //new car
            System.out.println("Adding a new car.");
            System.out.println("Purpose: ");
            System.out.println("\tAdd information about a new car location.");
            System.out.println("\nUsage: ");
            System.out.println("\tnewcar, <id>, <location>, <numberofcars>, <pricepercar>");
            break;
            
            case 4:  //new room
            System.out.println("Adding a new room.");
            System.out.println("Purpose: ");
            System.out.println("\tAdd information about a new room location.");
            System.out.println("\nUsage: ");
            System.out.println("\tnewroom, <id>, <location>, <numberofrooms>, <priceperroom>");
            break;
            
            case 5:  //new Customer
            System.out.println("Adding a new Customer.");
            System.out.println("Purpose: ");
            System.out.println("\tGet the system to provide a new customer id. (same as adding a new customer)");
            System.out.println("\nUsage: ");
            System.out.println("\tnewcustomer, <id>");
            break;
            
            
            case 6: //delete Flight
            System.out.println("Deleting a flight");
            System.out.println("Purpose: ");
            System.out.println("\tDelete a flight's information.");
            System.out.println("\nUsage: ");
            System.out.println("\tdeleteflight, <id>, <flightnumber>");
            break;
            
            case 7: //delete car
            System.out.println("Deleting a car");
            System.out.println("Purpose: ");
            System.out.println("\tDelete all cars from a location.");
            System.out.println("\nUsage: ");
            System.out.println("\tdeletecar, <id>, <location>, <numCars>");
            break;
            
            case 8: //delete room
            System.out.println("Deleting a room");
            System.out.println("\nPurpose: ");
            System.out.println("\tDelete all rooms from a location.");
            System.out.println("Usage: ");
            System.out.println("\tdeleteroom, <id>, <location>, <numRooms>");
            break;
            
            case 9: //delete Customer
            System.out.println("Deleting a Customer");
            System.out.println("Purpose: ");
            System.out.println("\tRemove a customer from the database.");
            System.out.println("\nUsage: ");
            System.out.println("\tdeletecustomer, <id>, <customerid>");
            break;
            
            case 10: //querying a flight
            System.out.println("Querying flight.");
            System.out.println("Purpose: ");
            System.out.println("\tObtain Seat information about a certain flight.");
            System.out.println("\nUsage: ");
            System.out.println("\tqueryflight, <id>, <flightnumber>");
            break;
            
            case 11: //querying a car Location
            System.out.println("Querying a car location.");
            System.out.println("Purpose: ");
            System.out.println("\tObtain number of cars at a certain car location.");
            System.out.println("\nUsage: ");
            System.out.println("\tquerycar, <id>, <location>");        
            break;
            
            case 12: //querying a room location
            System.out.println("Querying a room Location.");
            System.out.println("Purpose: ");
            System.out.println("\tObtain number of rooms at a certain room location.");
            System.out.println("\nUsage: ");
            System.out.println("\tqueryroom, <id>, <location>");        
            break;
            
            case 13: //querying Customer Information
            System.out.println("Querying Customer Information.");
            System.out.println("Purpose: ");
            System.out.println("\tObtain information about a customer.");
            System.out.println("\nUsage: ");
            System.out.println("\tquerycustomer, <id>, <customerid>");
            break;               
            
            case 14: //querying a flight for price 
            System.out.println("Querying flight.");
            System.out.println("Purpose: ");
            System.out.println("\tObtain price information about a certain flight.");
            System.out.println("\nUsage: ");
            System.out.println("\tqueryflightprice, <id>, <flightnumber>");
            break;
            
            case 15: //querying a car Location for price
            System.out.println("Querying a car location.");
            System.out.println("Purpose: ");
            System.out.println("\tObtain price information about a certain car location.");
            System.out.println("\nUsage: ");
            System.out.println("\tquerycarprice, <id>, <location>");        
            break;
            
            case 16: //querying a room location for price
            System.out.println("Querying a room Location.");
            System.out.println("Purpose: ");
            System.out.println("\tObtain price information about a certain room location.");
            System.out.println("\nUsage: ");
            System.out.println("\tqueryroomprice, <id>, <location>");        
            break;

            case 17:  //reserve a flight
            System.out.println("Reserving a flight.");
            System.out.println("Purpose: ");
            System.out.println("\tReserve a flight for a customer.");
            System.out.println("\nUsage: ");
            System.out.println("\treserveflight, <id>, <customerid>, <flightnumber>");
            break;
            
            case 18:  //reserve a car
            System.out.println("Reserving a car.");
            System.out.println("Purpose: ");
            System.out.println("\tReserve a given number of cars for a customer at a particular location.");
            System.out.println("\nUsage: ");
            System.out.println("\treservecar, <id>, <customerid>, <location>, <nummberofcars>");
            break;
            
            case 19:  //reserve a room
            System.out.println("Reserving a room.");
            System.out.println("Purpose: ");
            System.out.println("\tReserve a given number of rooms for a customer at a particular location.");
            System.out.println("\nUsage: ");
            System.out.println("\treserveroom, <id>, <customerid>, <location>, <nummberofrooms>");
            break;
            
            case 20:  //reserve an Itinerary
            System.out.println("Reserving an Itinerary.");
            System.out.println("Purpose: ");
            System.out.println("\tBook one or more flights.Also book zero or more cars/rooms at a location.");
            System.out.println("\nUsage: ");
            System.out.println("\titinerary, <id>, <customerid>, "
                    + "<flightnumber1>....<flightnumberN>, "
                    + "<LocationToBookcarsOrrooms>, <NumberOfcars>, <NumberOfroom>");
            break;
            

            case 21:  //quit the client
            System.out.println("Quitting client.");
            System.out.println("Purpose: ");
            System.out.println("\tExit the client application.");
            System.out.println("\nUsage: ");
            System.out.println("\tquit");
            break;
            
            case 22:  //new customer with id
            System.out.println("Create new customer providing an id");
            System.out.println("Purpose: ");
            System.out.println("\tCreates a new customer with the id provided");
            System.out.println("\nUsage: ");
            System.out.println("\tnewcustomerid, <id>, <customerid>");
            break;

            default:
            System.out.println(command);
            System.out.println("The interface does not support this command.");
            break;
        }
    }

    public int getInt(Object temp) throws Exception {
        try {
            return (new Integer((String)temp)).intValue();
        }
        catch(Exception e) {
            throw e;
        }
    }
    
    public boolean getBoolean(Object temp) throws Exception {
        try {
            return (new Boolean((String)temp)).booleanValue();
        }
        catch(Exception e) {
            throw e;
        }
    }

    public String getString(Object temp) throws Exception {
        try {    
            return (String)temp;
        }
        catch (Exception e) {
            throw e;
        }
    }
    
}
