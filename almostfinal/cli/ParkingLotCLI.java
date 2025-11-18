package cli;

import main.*;
import service.*;
import util.Constants;
import exceptions.UserAlreadyExistsException;
import exceptions.SlotUnavailableException;
import java.util.Scanner;
import java.io.File;
import java.util.Arrays;
import java.util.List;

public class ParkingLotCLI {
    private ParkingSystem system;
    private AuthService auth;
    private Scanner scanner;
    private boolean running;

    public ParkingLotCLI(ParkingSystem system, AuthService auth) {
        this.system = system;
        this.auth = auth;
        this.scanner = new Scanner(System.in);
        this.running = true;
    }

    public static void main(String[] args) {
        File recordFile = new File("record.txt");
        try {
            if (recordFile.createNewFile()) {
                System.out.println("System: Created new user record file.");
            }
        } catch (Exception e) {
            System.out.println("Error checking record.txt: " + e.getMessage());
        }

        int numFloors = 2;
        int spotsPerFloor = 8;
        int totalSpots = numFloors * spotsPerFloor;
        
        AuthService auth = new AuthService(recordFile.getPath(), totalSpots);
        ParkingLot lot = new ParkingLot(numFloors, spotsPerFloor); 
        
        for (int i = 1; i <= 5; i++) lot.addSpotToFloor(1, new ParkingSpot("A" + i, Constants.VEHICLE_CAR));
        for (int i = 6; i <= 8; i++) lot.addSpotToFloor(1, new ParkingSpot("A" + i, Constants.VEHICLE_MOTORCYCLE));
        for (int i = 1; i <= 5; i++) lot.addSpotToFloor(2, new ParkingSpot("B" + i, Constants.VEHICLE_CAR));
        for (int i = 6; i <= 8; i++) lot.addSpotToFloor(2, new ParkingSpot("B" + i, Constants.VEHICLE_MOTORCYCLE));
        System.out.println("Parking lot initialized with " + totalSpots + " total spots.");

        BillingService billing = new BillingService(20.0, 10.0, 10.0);
        ParkingSystem system = new ParkingSystem(lot, auth, billing);

        try {
            Admin admin = new Admin("admin", "admin123", "Admin User");
            admin.setSystem(system);
            auth.registerUser(admin);
        
        } catch (UserAlreadyExistsException e) {
         
        }

        try {
            Attendant att1 = new Attendant("att1", "att123", "Attendant One");
            auth.registerUser(att1);
          
        } catch (UserAlreadyExistsException e) {
        }

        ParkingLotCLI cli = new ParkingLotCLI(system, auth);
        cli.run();
    }

    public void run() {
        System.out.println("Welcome to SmartParking CLI");
        while (running) {
            printMainMenu();
            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1": handleRegister(); break;
                    case "2": handleLogin(); break;
                    case "3": showLiveStatus(); break;
                    case "4": showCurrentUsersInside(); break;
                    case "0": running = false; System.out.println("Exiting..."); break;
                    default: System.out.println("Invalid choice");
                }
            } catch (Exception e) {
                printError("Error: %s", e.getMessage());
            }
        }
        scanner.close();
    }

    private void printMainMenu() {
        System.out.println("\n    Main Menu    ");
        System.out.println("1. Register (Customer)");
        System.out.println("2. Login");
        System.out.println("3. Show Live Slot Status");
        System.out.println("4. Show users currently inside");
        System.out.println("0. Exit");
        System.out.print("Choice: ");
    }

    private void handleRegister() {
        try {
            System.out.print("Enter username: ");
            String username = scanner.nextLine().trim();
            System.out.print("Enter password: ");
            String pass = scanner.nextLine().trim();
            System.out.print("Enter full name: ");
            String name = scanner.nextLine().trim();

            double initialBalance = 100.0;
            Customer c = new Customer(username, pass, name, initialBalance);
            
            if (auth.registerUser(c)) {
                System.out.println("Registered successfully. Free starting balance: " + initialBalance);
            } else {
                System.out.println("Registration failed.");
            }
            
        } catch (UserAlreadyExistsException e) {
            System.out.println("Registration failed: " + e.getMessage());
        }
    }

    private void handleLogin() {
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String pass = scanner.nextLine().trim();
        
        User u = auth.login(username, pass);
        
        if (u == null) {
            System.out.println("Login failed.");
            return;
        }
        System.out.println("Welcome " + u.getName());
        
        if (u instanceof Customer) {
            handleCustomer((Customer) u);
        } else if (u instanceof Admin) {
            Admin a = (Admin) u;
            a.setSystem(system); 
            handleAdmin(a);
        } else if (u instanceof Attendant) {
            handleAttendant((Attendant) u);
        }
    }

    private void handleCustomer(Customer c) {
        boolean back = false;
        while (!back) {
            boolean isInside = checkUserInside(c.getUsername());

            if (isInside) {
                System.out.println("\n    Customer Menu (" + c.getName() + ") [INSIDE]");
                System.out.println("1. View Dashboard");
                System.out.println("2. Exit Parking");
                System.out.println("3. Deposit Money");
                System.out.println("0. Logout");
                System.out.print("Choice: ");
                String ch = scanner.nextLine().trim();
                try {
                    switch (ch) {
                        case "1": c.showDashboard(); break;
                        case "2": handleExit(c); break;
                        case "3": handleDeposit(c); break;
                        case "0": back = true; break;
                        default: System.out.println("Invalid");
                    }
                } catch (Exception e) {
                    printError("Error: %s", e.getMessage());
                }
            } else {
                System.out.println("\n    Customer Menu (" + c.getUsername() + ") [OUTSIDE]");
                System.out.println("1. View Dashboard");
                System.out.println("2. Book Slot (Reservation)");
                System.out.println("3. Cancel Reservation");
                System.out.println("4. Enter Parking");
                System.out.println("5. View Available Slots");
                System.out.println("6. View My Parking History");
                System.out.println("7. Deposit Money");
                System.out.println("0. Logout");
                System.out.print("Choice: ");
                String ch = scanner.nextLine().trim();
                try {
                    switch (ch) {
                        case "1": c.showDashboard(); break;
                        case "2": handleBookSlot(c); break;
                        case "3": handleCancelBooking(c); break;
                        case "4": handleEntry(c); break;
                        case "5": showLiveStatus(); break;
                        case "6": System.out.println(system.getParkingHistory(c)); break;
                        case "7": handleDeposit(c); break;
                        case "0": back = true; break;
                        default: System.out.println("Invalid");
                    }
                } catch (Exception e) {
                    printError("Error: %s", e.getMessage());
                }
            }
        }
    }

    private void handleAdmin(Admin a) {
        boolean back = false;
        while (!back) {
            System.out.println("\n    Admin Menu    ");
            System.out.println("1. View Live Status");
            System.out.println("2. Generate Report");
            System.out.println("3. View Activity Logs");
            System.out.println("0. Logout");
            System.out.print("Choice: ");
            String ch = scanner.nextLine().trim();
            switch (ch) {
                case "1": showLiveStatus(); break;
                case "2": 
                    if (a.generateReport().contains("null")) {
                        a.setSystem(system);
                    }
                    System.out.println(a.generateReport()); 
                    break;
                case "3": 
                    System.out.println("    Activity Logs    ");
                    List<String> logs = system.getActivityLogs();
                    if (logs.isEmpty()) System.out.println("No activity recorded yet.");
                    else for(String log : logs) System.out.println(log);
                    break;
                case "0": back = true; break;
                default: System.out.println("Invalid");
            }
        }
    }

    private void handleAttendant(Attendant at) {
        boolean back = false;
        while (!back) {
            System.out.println("\n    Attendant Menu    ");
            at.showDashboard();
            System.out.println("1. Mark Entry");
            System.out.println("2. Mark Exit");
            System.out.println("0. Logout");
            System.out.print("Choice: ");
            String ch = scanner.nextLine().trim();
            try {
                switch (ch) {
                    case "1":
                        System.out.print("Enter Customer's Username: ");
                        String user = scanner.nextLine().trim();
                        User u = auth.findUserByUsername(user);
                        if(u == null || !(u instanceof Customer)) {
                            System.out.println("Customer not found.");
                            break;
                        }
                        Customer c = (Customer) u;
                        handleEntry(c);
                        at.incrementProcessed();
                        break;
                    case "2":
                        System.out.print("Enter Customer's Username: ");
                        String userExit = scanner.nextLine().trim();
                        User uExit = auth.findUserByUsername(userExit);
                         if(uExit == null || !(uExit instanceof Customer)) {
                            System.out.println("Customer not found.");
                            break;
                        }
                        Customer cExit = (Customer) uExit;
                        handleExit(cExit);
                        break;
                    case "0": back = true; break;
                    default: System.out.println("Invalid");
                }
            } catch (Exception e) {
                printError("Error: " + e.getMessage());
            }
        }
    }

    private Vehicle getVehicleFromUser(Customer c) {
        System.out.println("Select Vehicle Type:");
        System.out.println("1. Car (" + Constants.VEHICLE_CAR + ")");
        System.out.println("2. Bike (" + Constants.VEHICLE_MOTORCYCLE + ")");
        System.out.print("Choice: ");
        String typeChoice = scanner.nextLine().trim();
        String type = typeChoice.equals("2") ? Constants.VEHICLE_MOTORCYCLE : Constants.VEHICLE_CAR;
        
        System.out.print("Enter License Plate: ");
        String plate = scanner.nextLine().trim();
        
        boolean exists = false;
        for (String p : c.getVehiclePlates()) {
            if (p != null && p.equals(plate)) { 
                exists = true;
                break;
            }
        }
        if (!exists) {
            c.addVehicle(plate);
            auth.updateUser(c); 
        }
        
        if (type.equals(Constants.VEHICLE_CAR)) return new Car(plate, c.getUsername());
        else return new Motorcycle(plate, c.getUsername());
    }

    private void handleBookSlot(Customer c) throws SlotUnavailableException {
        System.out.println("    Book a Slot    ");
        Vehicle v = getVehicleFromUser(c);
        
        System.out.println("1. Auto-assign best spot");
        System.out.println("2. Choose a specific spot");
        System.out.print("Choice: ");
        String choice = scanner.nextLine().trim();

        Booking b;
        if (choice.equals("2")) {
            System.out.print("Enter preferred Spot ID: ");
            String spotId = scanner.nextLine().trim().toUpperCase();
            b = system.bookSlot(c, v, spotId);
        } else {
            b = system.bookSlot(c, v);
        }
        
        System.out.println("SUCCESS! Slot " + b.getPreferredSpotId() + " reserved.");
        System.out.println("Booking ID: " + b.getBookingId());
    }
    
    private void handleCancelBooking(Customer c) {
        System.out.println("    Cancel Reservation    ");
        System.out.print("Enter Booking ID to cancel: ");
        String bid = scanner.nextLine().trim();
        
        if (system.cancelBooking(bid, c.getUsername())) {
            System.out.println("Success: Booking " + bid + " cancelled. Spot is now free.");
        } else {
            System.out.println("Failed: Invalid Booking ID or booking not active.");
        }
    }
    
    private void handleEntry(Customer c) throws Exception {
        System.out.println("   Parking Entry    ");
        System.out.println("1. On-Spot Entry");
        System.out.println("2. Reserved Entry");
        System.out.print("Choice: ");
        String choice = scanner.nextLine().trim();
        
        ParkingTicket t = null;
        EntryGate gate = new EntryGate(system);

        if (choice.equals("1")) {
            System.out.println("On-Spot entry: 50.0 surcharge.");
            Vehicle v = getVehicleFromUser(c);
            t = gate.processEntry(c.getUsername(), v.getLicensePlate(), v.getVehicleType(), null);
        } else if (choice.equals("2")) {
            System.out.print("Enter Booking ID: ");
            String bookingId = scanner.nextLine().trim();
            t = gate.processEntry(c.getUsername(), null, null, bookingId);
        } else {
            System.out.println("Invalid choice.");
            return;
        }
        
        System.out.println("Entry successful!");
        System.out.println("Ticket ID: " + t.getTicketId());
        System.out.println("Spot: " + t.getSpotId() + " (" + t.getVehicleType() + ")");
    }

    private void handleExit(Customer c) throws Exception {
        System.out.println("    Exit & Pay    ");
        System.out.print("Enter Ticket ID: ");
        String tid = scanner.nextLine().trim();
        
        Double amt = new ExitGate(system).processExit(tid, c);
        auth.updateUser(c); 
        
        System.out.println("Exit successful.");
        System.out.println("Total charge: " + amt);
        System.out.println("New balance: " + c.getAccount().getBalance());
    }
    
    private void handleDeposit(Customer c) {
        System.out.print("Enter amount to deposit: ");
        try {
            String input = scanner.nextLine().trim();
            double amt = Double.parseDouble(input); 
            if (amt > 0) {
                c.getAccount().deposit(amt);
                auth.updateUser(c);
                System.out.println("Deposit successful. New balance: " + c.getAccount().getBalance());
            } else {
                System.out.println("Invalid amount.");
            }
        } catch (Exception e) {
            System.out.println("Invalid amount.");
        }
    }
    
    private void showLiveStatus() {
        System.out.println(system.getLiveStatus());
    }

    private void showCurrentUsersInside() {
        String[] users = auth.readCurrentUsers();
        System.out.println("Users currently inside:");
        if (users.length == 0) System.out.println("(none)");
        for (String u : users) System.out.println("- " + u);
    }

    private boolean checkUserInside(String username) {
        String[] users = auth.readCurrentUsers();
        return Arrays.asList(users).contains(username);
    }

    private void printError(String message, Object... args) {
        System.err.println(String.format(message, args));
    }
}