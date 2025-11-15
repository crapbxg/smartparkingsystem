package cli;

import main.*;
import service.*;
import util.Constants;
import util.AuthUtil;
import exceptions.SlotUnavailableException;

import java.util.Scanner;
import java.io.File;

/**
 * ParkingLotCLI - Command-line interface for the Smart Parking System.
 * Place this file at: src/com/smartparking/cli/ParkingLotCLI.java
 */
public class ParkingLotCLI {
    private ParkingSystem system;
    private AuthService auth;
    private BookingExpiryService expiryService;
    private Scanner scanner;
    private boolean running;

    public ParkingLotCLI(ParkingSystem system, AuthService auth, BookingExpiryService expiryService) {
        this.system = system;
        this.auth = auth;
        this.expiryService = expiryService;
        this.scanner = new Scanner(System.in);
        this.running = true;
    }

    public static void main(String[] args) {
        try {
            new File("users_all.txt").createNewFile();
            new File("users_current.txt").createNewFile();
            new File("bookings.txt").createNewFile();
            new File("parkinglot.txt").createNewFile();
        } catch (Exception e) {
            // ignore - files may already exist or permission problems will show later
        }

        AuthService auth = new AuthService("users_all.txt", "users_current.txt");
        // small demo lot: 2 floors × 10 spots
        ParkingLot lot = new ParkingLot(2, 10);
        for (int f = 1; f <= 2; f++) {
            for (int i = 1; i <= 10; i++) {
                String id = (char) ('A' + f - 1) + String.format("%02d", i);
                lot.addSpotToFloor(f, new main.ParkingSpot(id, Constants.VEHICLE_CAR));
            }
        }

        BillingService billing = new BillingService(Double.valueOf(20.0)); // ₹20/hr for demo
        ParkingSystem system = new ParkingSystem(lot, auth, billing, "bookings.txt");
        BookingExpiryService expiry = new BookingExpiryService(system);

        ParkingLotCLI cli = new ParkingLotCLI(system, auth, expiry);
        cli.run();
    }

    public void run() {
        System.out.println("Welcome to SmartParking CLI");
        while (running) {
            // call expiry check once on each loop to auto-expire bookings (no threads)
            expiryService.checkOnce();

            printMainMenu();
            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1": handleRegister(); break;
                    case "2": handleLogin(); break;
                    case "3": showLiveStatus(); break;
                    case "4": handleQuickSeedAdmin(); break;
                    case "0": running = false; System.out.println("Exiting..."); break;
                    default: System.out.println("Invalid choice"); break;
                }
            } catch (Exception e) {
                printError("Error: %s", e.getMessage());
            }
        }
        scanner.close();
    }

    private void printMainMenu() {
        System.out.println("\n--- Main Menu ---");
        System.out.println("1. Register (Customer)");
        System.out.println("2. Login");
        System.out.println("3. Show Live Slot Status");
        System.out.println("4. (Dev) Quick create admin & attendant");
        System.out.println("0. Exit");
        System.out.print("Choice: ");
    }

    private void handleRegister() {
        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Enter password: ");
        String pass = scanner.nextLine().trim();
        System.out.print("Enter full name: ");
        String name = scanner.nextLine().trim();
        System.out.print("Enter email: ");
        String email = scanner.nextLine().trim();

        String hashed = AuthUtil.hash(pass);
        Customer c = new Customer(username, hashed, name, email, 100.0); // demo balance
        auth.registerUser(c);
        System.out.println("Registered successfully. (Demo: initial balance ₹100)");
    }

    private void handleLogin() {
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String pass = scanner.nextLine().trim();

        User u = auth.login(username, pass);
        if (u == null) {
            System.out.println("Login failed (wrong username/password).");
            return;
        }

        System.out.println("Welcome " + u.getName() + " (" + u.getRole() + ")");
        if (u instanceof Customer) handleCustomer((Customer) u);
        else if (u instanceof Admin) handleAdmin((Admin) u);
        else if (u instanceof Attendant) handleAttendant((Attendant) u);
        else System.out.println("Unknown role - contact admin.");
    }

    private void handleCustomer(Customer c) {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- Customer Menu ---");
            System.out.println("1. View Dashboard");
            System.out.println("2. Book Slot (auto)");
            System.out.println("3. Book Slot (preferred)");
            System.out.println("4. Simulate Entry (generate ticket)");
            System.out.println("5. Simulate Exit (pay & close ticket)");
            System.out.println("9. Logout");
            System.out.print("Choice: ");
            String ch = scanner.nextLine().trim();
            try {
                switch (ch) {
                    case "1": c.showDashboard(); break;
                    case "2": handleBookAuto(c); break;
                    case "3": handleBookPreferred(c); break;
                    case "4": handleEntrySim(c); break;
                    case "5": handleExitSim(c); break;
                    case "9": auth.logoutByUsername(c.getUsername()); back = true; break;
                    default: System.out.println("Invalid"); break;
                }
            } catch (SlotUnavailableException sue) {
                printError("Slot error: %s", sue.getMessage());
            } catch (Exception e) {
                printError("Error: %s", e.getMessage());
            }
        }
    }

    private void handleAdmin(Admin a) {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- Admin Menu ---");
            System.out.println("1. View Dashboard");
            System.out.println("2. Add spot(s)");
            System.out.println("3. Generate report (stub)");
            System.out.println("9. Logout");
            System.out.print("Choice: ");
            String ch = scanner.nextLine().trim();
            switch (ch) {
                case "1": a.showDashboard(); break;
                case "2":
                    System.out.print("Enter floor number: ");
                    int f = Integer.parseInt(scanner.nextLine().trim());
                    System.out.print("Enter spot ids (comma separated): ");
                    String s = scanner.nextLine().trim();
                    String[] ids = s.split(",");
                    a.addSpots(f, ids);
                    // actually add to parkinglot in system's lot:
                    for (String id: ids) {
                        String spotId = id.trim();
                        if (!spotId.isEmpty()) system.findSpotById(spotId); // no-op; admin.addSpots is stubbed
                        // to actually add, you could call lot.addSpotToFloor(...) if you expose lot
                    }
                    break;
                case "3":
                    System.out.println(a.generateReport());
                    break;
                case "9":
                    auth.logoutByUsername(a.getUsername());
                    back = true;
                    break;
                default:
                    System.out.println("Invalid");
            }
        }
    }

    private void handleAttendant(Attendant at) {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- Attendant Menu ---");
            System.out.println("1. Mark entry (by user & vehicle)");
            System.out.println("2. Mark exit (by ticket)");
            System.out.println("9. Logout");
            System.out.print("Choice: ");
            String ch = scanner.nextLine().trim();
            switch (ch) {
                case "1":
                    System.out.print("Vehicle plate: ");
                    String p = scanner.nextLine().trim();
                    System.out.print("Username: ");
                    String u = scanner.nextLine().trim();
                    ParkingTicket t = new EntryGate(system).processEntry(u, p);
                    if (t != null) System.out.println("Ticket id: " + t.getTicketId());
                    else System.out.println("Entry failed - no available spot");
                    break;
                case "2":
                    System.out.print("Ticket id: ");
                    String tid = scanner.nextLine().trim();
                    try {
                        Double amt = new ExitGate(system).processExit(tid);
                        System.out.println("Charge: " + amt);
                    } catch (Exception e) {
                        printError("Exit failed: %s", e.getMessage());
                    }
                    break;
                case "9":
                    auth.logoutByUsername(at.getUsername());
                    back = true;
                    break;
                default:
                    System.out.println("Invalid");
            }
        }
    }

    private void handleBookAuto(Customer c) throws SlotUnavailableException {
        System.out.print("Enter vehicle plate: ");
        String plate = scanner.nextLine().trim();
        Vehicle v = new Car(plate, c.getUsername());
        Booking b = system.bookSlot(c, v);
        System.out.println("Booked: " + b.getBookingId() + " for spot " + b.getPreferredSpotId());
    }

    private void handleBookPreferred(Customer c) throws SlotUnavailableException {
        System.out.print("Enter vehicle plate: ");
        String plate = scanner.nextLine().trim();
        System.out.print("Enter preferred spot id: ");
        String pref = scanner.nextLine().trim();
        Vehicle v = new Car(plate, c.getUsername());
        Booking b = system.bookSlot(c, v, pref);
        System.out.println("Booked: " + b.getBookingId() + " pref " + b.getPreferredSpotId());
    }

    private void handleEntrySim(Customer c) {
        System.out.print("Enter vehicle plate: ");
        String plate = scanner.nextLine().trim();
        ParkingTicket t = new EntryGate(system).processEntry(c.getUsername(), plate);
        if (t == null) System.out.println("Entry failed - no spots");
        else System.out.println("Entry ticket: " + t.getTicketId());
    }

    private void handleExitSim(Customer c) {
        System.out.print("Enter ticket id: ");
        String tid = scanner.nextLine().trim();
        try {
            Double amt = new ExitGate(system).processExit(tid);
            System.out.println("Please pay: " + amt);
        } catch (Exception e) {
            printError("Exit failed: %s", e.getMessage());
        }
    }

    private void showLiveStatus() {
        System.out.println(system.getLiveStatus());
    }

    // quick developer helper - create an admin and attendant if they don't exist
    private void handleQuickSeedAdmin() {
        if (auth.findUserByUsername("admin") == null) {
            Admin a = new Admin("admin", AuthUtil.hash("admin123"), "Administrator", "admin@local", "adminkey");
            auth.registerUser(a);
            System.out.println("Admin 'admin' created with password 'admin123'");
        } else {
            System.out.println("Admin already exists");
        }
        if (auth.findUserByUsername("att") == null) {
            Attendant at = new Attendant("att", AuthUtil.hash("att123"), "Attendant", "att@local", "MORNING");
            auth.registerUser(at);
            System.out.println("Attendant 'att' created with password 'att123'");
        } else {
            System.out.println("Attendant already exists");
        }
    }

    private void printError(String message, Object... args) {
        String m = String.format(message, args);
        System.err.println(m);
    }
}
