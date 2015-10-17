package middleware;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import javax.jws.WebService;

@WebService(endpointInterface = "middleware.MiddlewareInterface")
public class MiddlewareImpl implements middleware.MiddlewareInterface {
	
    private ResourceManager proxyCar;
    private ResourceManager proxyFlight;
    private ResourceManager proxyRoom;

	public MiddlewareImpl() {
		// TODO Auto-generated constructor stub
		try {
			this.setupProxies(
					"rmCar", "localhost", 8080, 
					"rmFlight", "localhost", 8081, 
					"rmRoom", "localhost", 8082);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void setupProxies(String serviceNameCar, String serviceHostCar, int servicePortCar, 
    		String serviceNameFlight, String serviceHostFlight, int servicePortFlight, 
    		String serviceNameRoom, String serviceHostRoom, int servicePortRoom) throws MalformedURLException {
	    
        URL wsdlLocation = new URL("http", serviceHostCar, servicePortCar, 
                "/" + serviceNameCar + "/service?wsdl");
                
        ResourceManagerImplService service = new ResourceManagerImplService(wsdlLocation);
        
        System.out.println("opening car proxy");
        
        proxyCar = service.getResourceManagerImplPort();
        
        System.out.println("car proxy opened, " + proxyCar.toString());
        
        wsdlLocation = new URL("http", serviceHostFlight, servicePortFlight, 
                "/" + serviceNameFlight + "/service?wsdl");
                
        service = new ResourceManagerImplService(wsdlLocation);
        
        System.out.println("opening flight proxy");
        
        proxyFlight = service.getResourceManagerImplPort();
        
        System.out.println("flight proxy opened, " + proxyFlight.toString());
        
        wsdlLocation = new URL("http", serviceHostRoom, servicePortRoom, 
                "/" + serviceNameRoom + "/service?wsdl");
                
        service = new ResourceManagerImplService(wsdlLocation);
        
        proxyRoom = service.getResourceManagerImplPort();
	}

	@Override
	public String handleAndReply(String command) {

		int id;
		int flightNumber;
		int flightPrice;
		int numSeats;
		boolean room;
		boolean car;
		int price;
		int numRooms;
		int numCars;
		String location;

		Vector arguments = new Vector();

		// remove heading and trailing white space
		command = command.trim();
		arguments = parse(command);
		String ret = "";

		// decide which of the commands this was
		switch (findChoice((String) arguments.elementAt(0))) {

		case 1:
			break;

		case 2: // new flight
			if (arguments.size() != 5) {
				wrongNumber();
				break;
			}
			ret = ret + ("Adding a new Flight using id: "
					+ arguments.elementAt(1));
			ret = ret + ("Flight number: " + arguments.elementAt(2));
			ret = ret + "\n" + ("Add Flight Seats: " + arguments.elementAt(3));
			ret = ret + "\n" + ("Set Flight Price: " + arguments.elementAt(4));

			try {
				id = getInt(arguments.elementAt(1));
				flightNumber = getInt(arguments.elementAt(2));
				numSeats = getInt(arguments.elementAt(3));
				flightPrice = getInt(arguments.elementAt(4));

				if (proxyFlight.addFlight(id, flightNumber,
						numSeats, flightPrice))
					ret = ret + "\n" + ("Flight added");
				else
					ret = ret + "\n" + ("Flight could not be added");
			} catch (Exception e) {
				ret = ret + "\n" + ("EXCEPTION: ");
				ret = ret + "\n" + (e.getMessage());
				e.printStackTrace();
			}
			break;

		case 3: // new car
			if (arguments.size() != 5) {
				wrongNumber();
				break;
			}
			ret = ret + "\n" + ("Adding a new car using id: "
					+ arguments.elementAt(1));
			ret = ret + "\n" + ("car Location: " + arguments.elementAt(2));
			ret = ret + "\n" + ("Add Number of cars: " + arguments.elementAt(3));
			ret = ret + "\n" + ("Set Price: " + arguments.elementAt(4));
			try {
				id = getInt(arguments.elementAt(1));
				location = getString(arguments.elementAt(2));
				numCars = getInt(arguments.elementAt(3));
				price = getInt(arguments.elementAt(4));
				
				System.out.println(proxyCar);

				if (proxyCar.addCars(id, location, numCars, price))
					ret = ret + "\n" + ("cars added");
				else
					ret = ret + "\n" + ("cars could not be added");
			} catch (Exception e) {
				ret = ret + "\n" + ("EXCEPTION: ");
				ret = ret + "\n" + (e.getMessage());
				e.printStackTrace();
			}
			break;

		case 4: // new room
			if (arguments.size() != 5) {
				wrongNumber();
				break;
			}
			ret = ret + "\n" + ("Adding a new room using id: "
					+ arguments.elementAt(1));
			ret = ret + "\n" + ("room Location: " + arguments.elementAt(2));
			System.out
					.println("Add Number of rooms: " + arguments.elementAt(3));
			ret = ret + "\n" + ("Set Price: " + arguments.elementAt(4));
			try {
				id = getInt(arguments.elementAt(1));
				location = getString(arguments.elementAt(2));
				numRooms = getInt(arguments.elementAt(3));
				price = getInt(arguments.elementAt(4));

				if (proxyRoom.addRooms(id, location, numRooms,
						price))
					ret = ret + "\n" + ("rooms added");
				else
					ret = ret + "\n" + ("rooms could not be added");
			} catch (Exception e) {
				ret = ret + "\n" + ("EXCEPTION: ");
				ret = ret + "\n" + (e.getMessage());
				e.printStackTrace();
			}
			break;

		case 5: // new Customer
			if (arguments.size() != 2) {
				wrongNumber();
				break;
			}
			ret = ret + "\n" + ("Adding a new Customer using id: "
					+ arguments.elementAt(1));
			try {
				id = getInt(arguments.elementAt(1));
				int customer = proxyCar.newCustomer(id);
				proxyFlight.newCustomer(id);
				proxyRoom.newCustomer(id);
				ret = ret + "\n" + ("new customer id: " + customer);
			} catch (Exception e) {
				ret = ret + "\n" + ("EXCEPTION: ");
				ret = ret + "\n" + (e.getMessage());
				e.printStackTrace();
			}
			break;

		case 6: // delete Flight
			if (arguments.size() != 3) {
				wrongNumber();
				break;
			}
			ret = ret + "\n" + ("Deleting a flight using id: "
					+ arguments.elementAt(1));
			ret = ret + "\n" + ("Flight Number: " + arguments.elementAt(2));
			try {
				id = getInt(arguments.elementAt(1));
				flightNumber = getInt(arguments.elementAt(2));

				if (proxyFlight.deleteFlight(id, flightNumber))
					ret = ret + "\n" + ("Flight Deleted");
				else
					ret = ret + "\n" + ("Flight could not be deleted");
			} catch (Exception e) {
				ret = ret + "\n" + ("EXCEPTION: ");
				ret = ret + "\n" + (e.getMessage());
				e.printStackTrace();
			}
			break;

		case 7: // delete car
			if (arguments.size() != 3) {
				wrongNumber();
				break;
			}
			System.out
					.println("Deleting the cars from a particular location  using id: "
							+ arguments.elementAt(1));
			ret = ret + "\n" + ("car Location: " + arguments.elementAt(2));
			try {
				id = getInt(arguments.elementAt(1));
				location = getString(arguments.elementAt(2));

				if (proxyCar.deleteCars(id, location))
					ret = ret + "\n" + ("cars Deleted");
				else
					ret = ret + "\n" + ("cars could not be deleted");
			} catch (Exception e) {
				ret = ret + "\n" + ("EXCEPTION: ");
				ret = ret + "\n" + (e.getMessage());
				e.printStackTrace();
			}
			break;

		case 8: // delete room
			if (arguments.size() != 3) {
				wrongNumber();
				break;
			}
			System.out
					.println("Deleting all rooms from a particular location  using id: "
							+ arguments.elementAt(1));
			ret = ret + "\n" + ("room Location: " + arguments.elementAt(2));
			try {
				id = getInt(arguments.elementAt(1));
				location = getString(arguments.elementAt(2));

				if (proxyRoom.deleteRooms(id, location))
					ret = ret + "\n" + ("rooms Deleted");
				else
					ret = ret + "\n" + ("rooms could not be deleted");
			} catch (Exception e) {
				ret = ret + "\n" + ("EXCEPTION: ");
				ret = ret + "\n" + (e.getMessage());
				e.printStackTrace();
			}
			break;

		case 9: // delete Customer
			if (arguments.size() != 3) {
				wrongNumber();
				break;
			}
			System.out
					.println("Deleting a customer from the database using id: "
							+ arguments.elementAt(1));
			ret = ret + "\n" + ("Customer id: " + arguments.elementAt(2));
			try {
				id = getInt(arguments.elementAt(1));
				int customer = getInt(arguments.elementAt(2));
				proxyCar.deleteCustomer(id, customer);
				proxyFlight.deleteCustomer(id, customer);
				if (proxyRoom.deleteCustomer(id, customer))
					ret = ret + "\n" + ("Customer Deleted");
				else
					ret = ret + "\n" + ("Customer could not be deleted");
			} catch (Exception e) {
				ret = ret + "\n" + ("EXCEPTION: ");
				ret = ret + "\n" + (e.getMessage());
				e.printStackTrace();
			}
			break;

		case 10: // querying a flight
			if (arguments.size() != 3) {
				wrongNumber();
				break;
			}
			ret = ret + "\n" + ("Querying a flight using id: "
					+ arguments.elementAt(1));
			ret = ret + "\n" + ("Flight number: " + arguments.elementAt(2));
			try {
				id = getInt(arguments.elementAt(1));
				flightNumber = getInt(arguments.elementAt(2));
				int seats = proxyFlight.queryFlight(id,
						flightNumber);
				ret = ret + "\n" + ("Number of seats available: " + seats);
			} catch (Exception e) {
				ret = ret + "\n" + ("EXCEPTION: ");
				ret = ret + "\n" + (e.getMessage());
				e.printStackTrace();
			}
			break;

		case 11: // querying a car Location
			if (arguments.size() != 3) {
				wrongNumber();
				break;
			}
			ret = ret + "\n" + ("Querying a car location using id: "
					+ arguments.elementAt(1));
			ret = ret + "\n" + ("car location: " + arguments.elementAt(2));
			try {
				id = getInt(arguments.elementAt(1));
				location = getString(arguments.elementAt(2));

				numCars = proxyCar.queryCars(id, location);
				ret = ret + "\n" + ("number of cars at this location: "
						+ numCars);
			} catch (Exception e) {
				ret = ret + "\n" + ("EXCEPTION: ");
				ret = ret + "\n" + (e.getMessage());
				e.printStackTrace();
			}
			break;

		case 12: // querying a room location
			if (arguments.size() != 3) {
				wrongNumber();
				break;
			}
			ret = ret + "\n" + ("Querying a room location using id: "
					+ arguments.elementAt(1));
			ret = ret + "\n" + ("room location: " + arguments.elementAt(2));
			try {
				id = getInt(arguments.elementAt(1));
				location = getString(arguments.elementAt(2));

				numRooms = proxyRoom.queryRooms(id, location);
				ret = ret + "\n" + ("number of rooms at this location: "
						+ numRooms);
			} catch (Exception e) {
				ret = ret + "\n" + ("EXCEPTION: ");
				ret = ret + "\n" + (e.getMessage());
				e.printStackTrace();
			}
			break;

		case 13: // querying Customer Information
			if (arguments.size() != 3) {
				wrongNumber();
				break;
			}
			ret = ret + "\n" + ("Querying Customer information using id: "
					+ arguments.elementAt(1));
			ret = ret + "\n" + ("Customer id: " + arguments.elementAt(2));
			try {
				id = getInt(arguments.elementAt(1));
				int customer = getInt(arguments.elementAt(2));

				String bill = proxyCar.queryCustomerInfo(id,
						customer);
				bill += proxyFlight
						.queryCustomerInfo(id, customer);
				bill += proxyRoom.queryCustomerInfo(id, customer);
				ret = ret + "\n" + ("Customer info: " + bill);
			} catch (Exception e) {
				ret = ret + "\n" + ("EXCEPTION: ");
				ret = ret + "\n" + (e.getMessage());
				e.printStackTrace();
			}
			break;

		case 14: // querying a flight Price
			if (arguments.size() != 3) {
				wrongNumber();
				break;
			}
			ret = ret + "\n" + ("Querying a flight Price using id: "
					+ arguments.elementAt(1));
			ret = ret + "\n" + ("Flight number: " + arguments.elementAt(2));
			try {
				id = getInt(arguments.elementAt(1));
				flightNumber = getInt(arguments.elementAt(2));

				price = proxyFlight.queryFlightPrice(id,
						flightNumber);
				ret = ret + "\n" + ("Price of a seat: " + price);
			} catch (Exception e) {
				ret = ret + "\n" + ("EXCEPTION: ");
				ret = ret + "\n" + (e.getMessage());
				e.printStackTrace();
			}
			break;

		case 15: // querying a car Price
			if (arguments.size() != 3) {
				wrongNumber();
				break;
			}
			ret = ret + "\n" + ("Querying a car price using id: "
					+ arguments.elementAt(1));
			ret = ret + "\n" + ("car location: " + arguments.elementAt(2));
			try {
				id = getInt(arguments.elementAt(1));
				location = getString(arguments.elementAt(2));

				price = proxyCar.queryCarsPrice(id, location);
				ret = ret + "\n" + ("Price of a car at this location: " + price);
			} catch (Exception e) {
				ret = ret + "\n" + ("EXCEPTION: ");
				ret = ret + "\n" + (e.getMessage());
				e.printStackTrace();
			}
			break;

		case 16: // querying a room price
			if (arguments.size() != 3) {
				wrongNumber();
				break;
			}
			ret = ret + "\n" + ("Querying a room price using id: "
					+ arguments.elementAt(1));
			ret = ret + "\n" + ("room Location: " + arguments.elementAt(2));
			try {
				id = getInt(arguments.elementAt(1));
				location = getString(arguments.elementAt(2));

				price = proxyRoom.queryRoomsPrice(id, location);
				ret = ret + "\n" + ("Price of rooms at this location: " + price);
			} catch (Exception e) {
				ret = ret + "\n" + ("EXCEPTION: ");
				ret = ret + "\n" + (e.getMessage());
				e.printStackTrace();
			}
			break;

		case 17: // reserve a flight
			if (arguments.size() != 4) {
				wrongNumber();
				break;
			}
			ret = ret + "\n" + ("Reserving a seat on a flight using id: "
					+ arguments.elementAt(1));
			ret = ret + "\n" + ("Customer id: " + arguments.elementAt(2));
			ret = ret + "\n" + ("Flight number: " + arguments.elementAt(3));
			try {
				id = getInt(arguments.elementAt(1));
				int customer = getInt(arguments.elementAt(2));
				flightNumber = getInt(arguments.elementAt(3));

				if (proxyFlight.reserveFlight(id, customer,
						flightNumber))
					ret = ret + "\n" + ("Flight Reserved");
				else
					ret = ret + "\n" + ("Flight could not be reserved.");
			} catch (Exception e) {
				ret = ret + "\n" + ("EXCEPTION: ");
				ret = ret + "\n" + (e.getMessage());
				e.printStackTrace();
			}
			break;

		case 18: // reserve a car
			if (arguments.size() != 4) {
				wrongNumber();
				break;
			}
			ret = ret + "\n" + ("Reserving a car at a location using id: "
					+ arguments.elementAt(1));
			ret = ret + "\n" + ("Customer id: " + arguments.elementAt(2));
			ret = ret + "\n" + ("Location: " + arguments.elementAt(3));
			try {
				id = getInt(arguments.elementAt(1));
				int customer = getInt(arguments.elementAt(2));
				location = getString(arguments.elementAt(3));

				if (proxyCar.reserveCar(id, customer, location))
					ret = ret + "\n" + ("car Reserved");
				else
					ret = ret + "\n" + ("car could not be reserved.");
			} catch (Exception e) {
				ret = ret + "\n" + ("EXCEPTION: ");
				ret = ret + "\n" + (e.getMessage());
				e.printStackTrace();
			}
			break;

		case 19: // reserve a room
			if (arguments.size() != 4) {
				wrongNumber();
				break;
			}
			ret = ret + "\n" + ("Reserving a room at a location using id: "
					+ arguments.elementAt(1));
			ret = ret + "\n" + ("Customer id: " + arguments.elementAt(2));
			ret = ret + "\n" + ("Location: " + arguments.elementAt(3));
			try {
				id = getInt(arguments.elementAt(1));
				int customer = getInt(arguments.elementAt(2));
				location = getString(arguments.elementAt(3));

				if (proxyRoom.reserveRoom(id, customer, location))
					ret = ret + "\n" + ("room Reserved");
				else
					ret = ret + "\n" + ("room could not be reserved.");
			} catch (Exception e) {
				ret = ret + "\n" + ("EXCEPTION: ");
				ret = ret + "\n" + (e.getMessage());
				e.printStackTrace();
			}
			break;

		case 20: // reserve an Itinerary
			if (arguments.size() < 7) {
				wrongNumber();
				break;
			}
			ret = ret + "\n" + ("Reserving an Itinerary using id: "
					+ arguments.elementAt(1));
			ret = ret + "\n" + ("Customer id: " + arguments.elementAt(2));
			for (int i = 0; i < arguments.size() - 6; i++)
				System.out
						.println("Flight number" + arguments.elementAt(3 + i));
			ret = ret + "\n" + ("Location for car/room booking: "
					+ arguments.elementAt(arguments.size() - 3));
			ret = ret + "\n" + ("car to book?: "
					+ arguments.elementAt(arguments.size() - 2));
			ret = ret + "\n" + ("room to book?: "
					+ arguments.elementAt(arguments.size() - 1));
			try {
				id = getInt(arguments.elementAt(1));
				int customer = getInt(arguments.elementAt(2));
				Vector flightNumbers = new Vector();
				for (int i = 0; i < arguments.size() - 6; i++)
					flightNumbers.addElement(arguments.elementAt(3 + i));
				location = getString(arguments.elementAt(arguments.size() - 3));
				car = getBoolean(arguments.elementAt(arguments.size() - 2));
				room = getBoolean(arguments.elementAt(arguments.size() - 1));

				if (proxyCar.reserveItinerary(id, customer,
						flightNumbers, location, car, room))
					ret = ret + "\n" + ("Itinerary Reserved");
				else
					ret = ret + "\n" + ("Itinerary could not be reserved.");
			} catch (Exception e) {
				ret = ret + "\n" + ("EXCEPTION: ");
				ret = ret + "\n" + (e.getMessage());
				e.printStackTrace();
			}
			break;

		case 21: // quit the client
			if (arguments.size() != 1) {
				wrongNumber();
				break;
			}
			ret = ret + "\n" + ("Quitting client.");
			return "";

		case 22: // new Customer given id
			if (arguments.size() != 3) {
				wrongNumber();
				break;
			}
			ret = ret + "\n" + ("Adding a new Customer using id: "
					+ arguments.elementAt(1) + " and cid "
					+ arguments.elementAt(2));
			try {
				id = getInt(arguments.elementAt(1));
				int customer = getInt(arguments.elementAt(2));
				proxyFlight.newCustomerId(id, customer);
				proxyRoom.newCustomerId(id, customer);
				boolean c = proxyCar.newCustomerId(id, customer);
				ret = ret + "\n" + ("new customer id: " + customer);
			} catch (Exception e) {
				ret = ret + "\n" + ("EXCEPTION: ");
				ret = ret + "\n" + (e.getMessage());
				e.printStackTrace();
			}
			break;

		default:
			ret = ret + "\n" + ("The interface does not support this command.");
			break;
		}
		
		return ret;
	}

	public Vector parse(String command) {
		Vector arguments = new Vector();
		StringTokenizer tokenizer = new StringTokenizer(command, ",");
		String argument = "";
		while (tokenizer.hasMoreTokens()) {
			argument = tokenizer.nextToken();
			argument = argument.trim();
			arguments.add(argument);
		}
		return arguments;
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

	public void wrongNumber() {
		System.out
				.println("The number of arguments provided in this command are wrong.");
		System.out
				.println("Type help, <commandname> to check usage of this command.");
	}

	public int getInt(Object temp) throws Exception {
		try {
			return (new Integer((String) temp)).intValue();
		} catch (Exception e) {
			throw e;
		}
	}

	public boolean getBoolean(Object temp) throws Exception {
		try {
			return (new Boolean((String) temp)).booleanValue();
		} catch (Exception e) {
			throw e;
		}
	}

	public String getString(Object temp) throws Exception {
		try {
			return (String) temp;
		} catch (Exception e) {
			throw e;
		}
	}

}
