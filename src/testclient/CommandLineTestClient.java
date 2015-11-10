package testclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;

import shared.RequestDescriptor;
import shared.RequestType;
import shared.ResponseDescriptor;
import shared.ServerConnection;

public class CommandLineTestClient {
	private ServerConnection middlewareConnection;
	public CommandLineTestClient(ServerConnection sc) {
		this.middlewareConnection = sc;
	}
	
    public static void main(String[] args) {
        try {
        
        	ServerConnection sc = null;
    		if(args.length != 2){
    			System.out.println("Expecting 2 parameters");
    			System.exit(-1);
    		}
    		try {
    			sc = new ServerConnection(args[0], Integer.parseInt(args[1]));
    		} catch (NumberFormatException | IOException e) {
    			e.printStackTrace();
    		}
            
            CommandLineTestClient client = new CommandLineTestClient(sc);
            
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

        		try {
            		request = buildRequest(rt, arguments);
            		reply = sendRequest(request);
            		System.out.println("ResponseType: " + reply.responseType + ", Data: " + reply.data + ", Message: " + reply.additionalMessage);
        		}
        		catch (java.net.SocketException ex) {
        			System.out.println("Failure: connection closed");
        		}
        		catch (Exception ex) {
        			System.out.println("Failure: " + ex.getMessage());
        			ex.printStackTrace();
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
			System.out.println("Deleting the cars from a particular location  in transactionID:  " + arguments[1]);
            System.out.println("car Location: " + arguments[2]);

            rd.transactionID = getInt(arguments[1]);
            rd.location = getString(arguments[2]);
			break;
		case DELETECUSTOMER:
            System.out.println("Deleting a customer from the database in transactionID:  " + arguments[1]);
            System.out.println("Customer id: " + arguments[2]);

                rd.transactionID = getInt(arguments[1]);
                rd.customerNumber = getInt(arguments[2]);
			break;
		case DELETEFLIGHT:
			System.out.println("Deleting a flight in transactionID:  " + arguments[1]);
            System.out.println("Flight Number: " + arguments[2]);

            rd.transactionID = getInt(arguments[1]);
            rd.flightNumber = getInt(arguments[2]);
			break;
		case DELETEROOM:
            System.out.println("Deleting all rooms from a particular location  in transactionID:  " + arguments[1]);
            System.out.println("room Location: " + arguments[2]);

            rd.transactionID = getInt(arguments[1]);
            rd.location = getString(arguments[2]);
			break;
		case ITINERARY:
            if (arguments.length < 7) {
                System.out.println("Wrong number of arguments");
                break;
            }
            System.out.println("Reserving an Itinerary in transactionID:  " + arguments[1]);
            System.out.println("Customer id: " + arguments[2]);
            for (int i = 0; i<arguments.length-6; i++)
                System.out.println("Flight number" + arguments[3 + i]);
            System.out.println("Location for car/room booking: " + arguments[arguments.length-3]);
            System.out.println("car to book?: " + arguments[arguments.length-2]);
            System.out.println("room to book?: " + arguments[arguments.length-1]);

            rd.transactionID = getInt(arguments[1]);
            rd.customerNumber = getInt(arguments[2]);
            rd.flightNumbers = new Vector<Integer>();
            for (int i = 0; i < arguments.length-6; i++)
                rd.flightNumbers.addElement(getInt(arguments[3 + i]));
            rd.location = getString(arguments[arguments.length-3]);
            rd.car = getInt(arguments[arguments.length-2]) > 0;
            rd.room = getInt(arguments[arguments.length-1]) > 0;

			break;
		case NEWCAR:
            System.out.println("Adding a new car in transactionID:  " + arguments[1]);
            System.out.println("car Location: " + arguments[2]);
            System.out.println("Add Number of cars: " + arguments[3]);
            System.out.println("Set Price: " + arguments[4]);

            rd.transactionID = getInt(arguments[1]);
            rd.location = getString(arguments[2]);
            rd.numCars = getInt(arguments[3]);
            rd.price = getInt(arguments[4]);
			break;
		case NEWCUSTOMER:
			System.out.println("Adding a new Customer in transactionID:  " + arguments[1]);
			
			rd.transactionID = getInt(arguments[1]);
			break;
		case NEWCUSTOMERID:
            System.out.println("Adding a new Customer in transactionID:  "
                    + arguments[1]  +  " and cid "  + arguments[2]);

            rd.transactionID = getInt(arguments[1]);
            rd.customerNumber = getInt(arguments[2]);
			break;
		case NEWFLIGHT:
            System.out.println("Adding a new Flight in transactionID:  " + arguments[1]);
            System.out.println("Flight number: " + arguments[2]);
            System.out.println("Add Flight Seats: " + arguments[3]);
            System.out.println("Set Flight Price: " + arguments[4]);

            rd.transactionID = getInt(arguments[1]);
            rd.flightNumber = getInt(arguments[2]);
            rd.numSeats = getInt(arguments[3]);
            rd.price = getInt(arguments[4]);
			break;
		case NEWROOM:
            System.out.println("Adding a new room in transactionID:  " + arguments[1]);
            System.out.println("room Location: " + arguments[2]);
            System.out.println("Add Number of rooms: " + arguments[3]);
            System.out.println("Set Price: " + arguments[4]);

            rd.transactionID = getInt(arguments[1]);
            rd.location = getString(arguments[2]);
            rd.numRooms = getInt(arguments[3]);
            rd.price = getInt(arguments[4]);
			break;
		case QUERYCAR:
            System.out.println("Querying a car location in transactionID:  " + arguments[1]);
            System.out.println("car location: " + arguments[2]);
            
            rd.transactionID = getInt(arguments[1]);
            rd.location = getString(arguments[2]);
			break;
		case QUERYCARPRICE:
            System.out.println("Querying a car price in transactionID:  " + arguments[1]);
            System.out.println("car location: " + arguments[2]);

            rd.transactionID = getInt(arguments[1]);
            rd.location = getString(arguments[2]);
			break;
		case QUERYCUSTOMER:
            System.out.println("Querying Customer information in transactionID:  " + arguments[1]);
            System.out.println("Customer id: " + arguments[2]);

            rd.transactionID = getInt(arguments[1]);
            rd.customerNumber = getInt(arguments[2]);
			break;
		case QUERYFLIGHT:
            System.out.println("Querying a flight in transactionID:  " + arguments[1]);
            System.out.println("Flight number: " + arguments[2]);

            rd.transactionID = getInt(arguments[1]);
            rd.flightNumber = getInt(arguments[2]);
			break;
		case QUERYFLIGHTPRICE:
            System.out.println("Querying a flight Price in transactionID:  " + arguments[1]);
            System.out.println("Flight number: " + arguments[2]);

            rd.transactionID = getInt(arguments[1]);
            rd.flightNumber = getInt(arguments[2]);
			break;
		case QUERYROOM:
            System.out.println("Querying a room location in transactionID:  " + arguments[1]);
            System.out.println("room location: " + arguments[2]);

            rd.transactionID = getInt(arguments[1]);
            rd.location = getString(arguments[2]);
			break;
		case QUERYROOMPRICE:
            System.out.println("Querying a room price in transactionID:  " + arguments[1]);
            System.out.println("room Location: " + arguments[2]);

            rd.transactionID = getInt(arguments[1]);
            rd.location = getString(arguments[2]);
			break;
		case RESERVECAR:
            System.out.println("Reserving a car at a location in transactionID:  " + arguments[1]);
            System.out.println("Customer id: " + arguments[2]);
            System.out.println("Location: " + arguments[3]);

            rd.transactionID = getInt(arguments[1]);
            rd.customerNumber = getInt(arguments[2]);
            rd.location = getString(arguments[3]);
			break;
		case RESERVEFLIGHT:
            System.out.println("Reserving a seat on a flight in transactionID:  " + arguments[1]);
            System.out.println("Customer id: " + arguments[2]);
            System.out.println("Flight number: " + arguments[3]);

            rd.transactionID = getInt(arguments[1]);
            rd.customerNumber = getInt(arguments[2]);
            rd.flightNumber = getInt(arguments[3]);
			break;
		case RESERVEROOM:
            System.out.println("Reserving a room at a location in transactionID:  " + arguments[1]);
            System.out.println("Customer id: " + arguments[2]);
            System.out.println("Location: " + arguments[3]);

            rd.transactionID = getInt(arguments[1]);
            rd.customerNumber = getInt(arguments[2]);
            rd.location = getString(arguments[3]);
			break;
		case STARTTXN:
            System.out.println("Starting a transaction");
            
			break;
		case COMMIT:
            System.out.println("Committing txn with id: " + arguments[1]);
            
            rd.transactionID = getInt(arguments[1]);
			break;
		case ABORT:
            System.out.println("Aborting txn with id: " + arguments[1]);
            
            rd.transactionID = getInt(arguments[1]);
			break;
		case SHUTDOWN:
            System.out.println("Shutting down");
            
			break;
		default:
			break;
    	}
    	return rd;
    }
    
    public ResponseDescriptor sendRequest(RequestDescriptor rd) throws Exception {
		try {
			return this.middlewareConnection.sendRequest(rd);
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
        else if (argument.compareToIgnoreCase("starttxn") == 0)
            return 23;
        else if (argument.compareToIgnoreCase("commit") == 0)
            return 24;
        else if (argument.compareToIgnoreCase("abort") == 0)
            return 25;
        else if (argument.compareToIgnoreCase("shutdown") == 0)
            return 26;
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
        System.out.println("start\ncommit\nabort\nshutdown");
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
            System.out.println("\tnewflight, <transactionID>, <flightnumber>, <numSeats>, <flightprice>");
            break;
            
            case 3:  //new car
            System.out.println("Adding a new car.");
            System.out.println("Purpose: ");
            System.out.println("\tAdd information about a new car location.");
            System.out.println("\nUsage: ");
            System.out.println("\tnewcar, <transactionID>, <location>, <numberofcars>, <pricepercar>");
            break;
            
            case 4:  //new room
            System.out.println("Adding a new room.");
            System.out.println("Purpose: ");
            System.out.println("\tAdd information about a new room location.");
            System.out.println("\nUsage: ");
            System.out.println("\tnewroom, <transactionID>, <location>, <numberofrooms>, <priceperroom>");
            break;
            
            case 5:  //new Customer
            System.out.println("Adding a new Customer.");
            System.out.println("Purpose: ");
            System.out.println("\tGet the system to provide a new customer id. (same as adding a new customer)");
            System.out.println("\nUsage: ");
            System.out.println("\tnewcustomer, <transactionID>");
            break;
            
            
            case 6: //delete Flight
            System.out.println("Deleting a flight");
            System.out.println("Purpose: ");
            System.out.println("\tDelete a flight's information.");
            System.out.println("\nUsage: ");
            System.out.println("\tdeleteflight, <transactionID>, <flightnumber>");
            break;
            
            case 7: //delete car
            System.out.println("Deleting a car");
            System.out.println("Purpose: ");
            System.out.println("\tDelete all cars from a location.");
            System.out.println("\nUsage: ");
            System.out.println("\tdeletecar, <transactionID>, <location>, <numCars>");
            break;
            
            case 8: //delete room
            System.out.println("Deleting a room");
            System.out.println("\nPurpose: ");
            System.out.println("\tDelete all rooms from a location.");
            System.out.println("Usage: ");
            System.out.println("\tdeleteroom, <transactionID>, <location>, <numRooms>");
            break;
            
            case 9: //delete Customer
            System.out.println("Deleting a Customer");
            System.out.println("Purpose: ");
            System.out.println("\tRemove a customer from the database.");
            System.out.println("\nUsage: ");
            System.out.println("\tdeletecustomer, <transactionID>, <customerid>");
            break;
            
            case 10: //querying a flight
            System.out.println("Querying flight.");
            System.out.println("Purpose: ");
            System.out.println("\tObtain Seat information about a certain flight.");
            System.out.println("\nUsage: ");
            System.out.println("\tqueryflight, <transactionID>, <flightnumber>");
            break;
            
            case 11: //querying a car Location
            System.out.println("Querying a car location.");
            System.out.println("Purpose: ");
            System.out.println("\tObtain number of cars at a certain car location.");
            System.out.println("\nUsage: ");
            System.out.println("\tquerycar, <transactionID>, <location>");        
            break;
            
            case 12: //querying a room location
            System.out.println("Querying a room Location.");
            System.out.println("Purpose: ");
            System.out.println("\tObtain number of rooms at a certain room location.");
            System.out.println("\nUsage: ");
            System.out.println("\tqueryroom, <transactionID>, <location>");        
            break;
            
            case 13: //querying Customer Information
            System.out.println("Querying Customer Information.");
            System.out.println("Purpose: ");
            System.out.println("\tObtain information about a customer.");
            System.out.println("\nUsage: ");
            System.out.println("\tquerycustomer, <transactionID>, <customerid>");
            break;               
            
            case 14: //querying a flight for price 
            System.out.println("Querying flight.");
            System.out.println("Purpose: ");
            System.out.println("\tObtain price information about a certain flight.");
            System.out.println("\nUsage: ");
            System.out.println("\tqueryflightprice, <transactionID>, <flightnumber>");
            break;
            
            case 15: //querying a car Location for price
            System.out.println("Querying a car location.");
            System.out.println("Purpose: ");
            System.out.println("\tObtain price information about a certain car location.");
            System.out.println("\nUsage: ");
            System.out.println("\tquerycarprice, <transactionID>, <location>");        
            break;
            
            case 16: //querying a room location for price
            System.out.println("Querying a room Location.");
            System.out.println("Purpose: ");
            System.out.println("\tObtain price information about a certain room location.");
            System.out.println("\nUsage: ");
            System.out.println("\tqueryroomprice, <transactionID>, <location>");        
            break;

            case 17:  //reserve a flight
            System.out.println("Reserving a flight.");
            System.out.println("Purpose: ");
            System.out.println("\tReserve a flight for a customer.");
            System.out.println("\nUsage: ");
            System.out.println("\treserveflight, <transactionID>, <customerid>, <flightnumber>");
            break;
            
            case 18:  //reserve a car
            System.out.println("Reserving a car.");
            System.out.println("Purpose: ");
            System.out.println("\tReserve a given number of cars for a customer at a particular location.");
            System.out.println("\nUsage: ");
            System.out.println("\treservecar, <transactionID>, <customerid>, <location>, <nummberofcars>");
            break;
            
            case 19:  //reserve a room
            System.out.println("Reserving a room.");
            System.out.println("Purpose: ");
            System.out.println("\tReserve a given number of rooms for a customer at a particular location.");
            System.out.println("\nUsage: ");
            System.out.println("\treserveroom, <transactionID>, <customerid>, <location>, <nummberofrooms>");
            break;
            
            case 20:  //reserve an Itinerary
            System.out.println("Reserving an Itinerary.");
            System.out.println("Purpose: ");
            System.out.println("\tBook one or more flights.Also book zero or more cars/rooms at a location.");
            System.out.println("\nUsage: ");
            System.out.println("\titinerary, <transactionID>, <customerid>, "
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
            System.out.println("\tnewcustomerid, <transactionID>, <customerid>");
            break;

            case 23:  //start transaction
            System.out.println("Start transaction");
            System.out.println("Purpose: ");
            System.out.println("\tStart a new transaction and returns its id");
            System.out.println("\nUsage: ");
            System.out.println("\tstarttxn");
            break;
            
            case 24:  //commit transaction
            System.out.println("Commit transaction");
            System.out.println("Purpose: ");
            System.out.println("\tAttempt to commit the given transaction; return true upon success");
            System.out.println("\nUsage: ");
            System.out.println("\tcommit, <transactionId>");
            break;
            
            case 25:  //abort transaction
            System.out.println("Abort Transaction");
            System.out.println("Purpose: ");
            System.out.println("\tAbort the given transaction");
            System.out.println("\nUsage: ");
            System.out.println("\tabort, <transactionId>");
            break;
            
            case 26:  //shutdown
            System.out.println("Shutdown");
            System.out.println("Purpose: ");
            System.out.println("\tShutdown gracefully");
            System.out.println("\nUsage: ");
            System.out.println("\tshutdown");
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
