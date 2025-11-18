package service;

import main.*;
import exceptions.SlotUnavailableException;
import util.*; 
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class ParkingSystem implements Loggable {
    private ParkingLot lot;
    private AuthService auth;
    private BillingService billing;
    private List<Booking> bookings;
    private List<ParkingTicket> allTickets;
    private int ticketCounter = 1000;
    
    private List<String> activityLogs;
    
    private final String TICKET_FILE = "tickets.txt"; 

    public static class ParkingStatistics {
        private int totalEntries = 0;
        private int totalExits = 0;
        private double totalRevenue = 0.0;
        
        public void recordEntry() { totalEntries++; }
        public void recordExit(double amount) { totalExits++; totalRevenue += amount; }
        
        public int getTotalEntries() { return totalEntries; }
        public int getTotalExits() { return totalExits; }
        public double getTotalRevenue() { return totalRevenue; }
        public int getCurrentOccupancy() { return totalEntries - totalExits; }
        
        @Override
        public String toString() {
            return String.format("Stats[Entries=%d, Exits=%d, Revenue=₹%.2f, Current=%d]",
                                 totalEntries, totalExits, totalRevenue, getCurrentOccupancy());
        }
    }
    
    private ParkingStatistics statistics;

    public ParkingSystem(ParkingLot lot, AuthService auth, BillingService billing) {
        this.lot = lot;
        this.auth = auth;
        this.billing = billing;
        this.bookings = new ArrayList<>();
        this.allTickets = new ArrayList<>();
        this.activityLogs = new ArrayList<>();
        this.statistics = new ParkingStatistics();
        
        loadTickets();
    }
    
    public void logActivity(String message, Object... args) {
        String m = String.format(message, args);
        String entry = "[" + new Date() + "] " + m;
        activityLogs.add(entry);
    }

    public List<String> getActivityLogs() {
        return activityLogs;
    }
    
    public ParkingStatistics getStatistics() {
        return statistics;
    }

    public Booking bookSlot(Customer c, Vehicle v) throws SlotUnavailableException {
        return bookSlot(c, v, null);
    }
    
    public Booking bookSlot(Customer c, Vehicle v, String preferredSpotId) throws SlotUnavailableException {
        ParkingSpot p = null;

        if (preferredSpotId != null && !preferredSpotId.isEmpty()) {
            p = lot.findSpot(preferredSpotId);
            if (p == null) throw new SlotUnavailableException("Spot ID " + preferredSpotId + " does not exist.");
            if (!p.isFree()) throw new SlotUnavailableException("Spot " + preferredSpotId + " is not free.");
            if (!p.getSpotType().equalsIgnoreCase(v.getVehicleType())) {
                throw new SlotUnavailableException("Spot " + preferredSpotId + " is for " + p.getSpotType());
            }
        } else {
            p = lot.findFreeSpotForVehicle(v.getVehicleType());
            if (p == null) throw new SlotUnavailableException("No free spots available for " + v.getVehicleType());
        }
        
        String id = c.getUsername() + "_" + (bookings.size() + 1);
        Booking b = new Booking(id, c.getUsername(), v.getLicensePlate(), v.getVehicleType(), p.getSpotId());
        bookings.add(b);
        p.reserve(c.getUsername());
        
        logActivity("BOOKING: User %s reserved spot %s (ID: %s)", c.getUsername(), p.getSpotId(), id);
        return b;
    }

    public boolean cancelBooking(String bookingId, String username) {
        Booking b = findBookingById(bookingId);
        if (b == null) return false;
        
        if (!b.getUsername().equals(username)) return false;
        
        if (!b.isActive()) return false;

        ParkingSpot spot = lot.findSpot(b.getPreferredSpotId());
        if (spot != null && spot.getStatus().equals(Constants.SPOT_RESERVED)) {
            spot.vacate();
        }

        b.cancel();
        logActivity("CANCEL: User %s cancelled booking %s", username, bookingId);
        return true;
    }

    public ParkingTicket markEntry(String username, String vehicleLicense, String vehicleType, String bookingId) throws SlotUnavailableException {
        ParkingSpot chosen = null;
        String licenseForTicket = vehicleLicense;
        String vehicleTypeForTicket = vehicleType;
        double surcharge = 0.0;

        if (bookingId != null && !bookingId.isEmpty()) {
            Booking b = findBookingById(bookingId);
            if (b == null || !b.getUsername().equals(username) || !b.isActive()) {
                throw new SlotUnavailableException("Invalid or inactive booking ID.");
            }
            ParkingSpot p = lot.findSpot(b.getPreferredSpotId());
            if (p != null && (p.getStatus().equals(Constants.SPOT_RESERVED) || p.getStatus().equals(Constants.SPOT_FREE))) {
                chosen = p;
                b.consume();
                licenseForTicket = b.getVehicleLicense();
                vehicleTypeForTicket = b.getVehicleType();
            } else {
                throw new SlotUnavailableException("Reserved spot is no longer available.");
            }
        } else {
            chosen = lot.findFreeSpotForVehicle(vehicleType);
            if (chosen == null) throw new SlotUnavailableException("No free spots available for " + vehicleType);
            surcharge = 50.0;
        }
        
        chosen.occupy(licenseForTicket);
        String ticketId = "TKT" + (++ticketCounter);
        long entryTime = System.currentTimeMillis();
        
        ParkingTicket t = new ParkingTicket(ticketId, licenseForTicket, vehicleTypeForTicket, chosen.getSpotId(), entryTime);
        t.setSurcharge(surcharge);
        allTickets.add(t);
        auth.addCurrentUser(username);
        
        statistics.recordEntry();
        saveTickets();
   
        logActivity("ENTRY: Ticket %s generated for %s at Spot %s", ticketId, username, chosen.getSpotId());
        
        return t;
    }

    public Double markExit(String ticketId, Customer customer) throws Exception {
        ParkingTicket ticket = findOpenTicket(ticketId);
        if (ticket == null) throw new Exception("Invalid or already-closed ticket ID.");

        long exitTime = System.currentTimeMillis();
        Double amount = billing.calculateFee(ticket.getEntryTimeMillis(), exitTime, ticket.getVehicleType());
        amount += ticket.getSurcharge();

        boolean paid = customer.getAccount().processPayment(customer.getUsername(), amount);
        if (!paid) {
            throw new exceptions.InsufficientFundsException("Payment failed. Amount due: " + amount);
        }

        ticket.close(exitTime, amount);
        ParkingSpot p = lot.findSpot(ticket.getSpotId());
        if (p != null) p.vacate();
        
        auth.removeCurrentUser(customer.getUsername());
        statistics.recordExit(amount);
        saveTickets();
    
        logActivity("EXIT: Ticket %s closed. User %s paid %.2f", ticketId, customer.getUsername(), amount);
        
        return amount;
    }

    private void saveTickets() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(TICKET_FILE))) {
            for (ParkingTicket t : allTickets) {
                String line = String.join(";",
                    t.getTicketId(),
                    t.getVehicleLicense(),
                    t.getVehicleType(),
                    t.getSpotId(),
                    String.valueOf(t.getEntryTimeMillis()),
                    String.valueOf(t.isOpen()),
                    String.valueOf(t.getAmountDue()),
                    String.valueOf(t.getSurcharge())
                );
                bw.write(line);
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error saving tickets: " + e.getMessage());
        }
    }

    private void loadTickets() {
        File f = new File(TICKET_FILE);
        if (!f.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            int maxId = 1000;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(";");
                if (p.length >= 8) {
                    String tid = p[0];
                    long entryTime = Long.parseLong(p[4]);
                    boolean isOpen = Boolean.parseBoolean(p[5]);
                    double amount = Double.parseDouble(p[6]);
                    double surcharge = Double.parseDouble(p[7]);

                    ParkingTicket t = new ParkingTicket(tid, p[1], p[2], p[3], entryTime);
                    t.setSurcharge(surcharge);
                    if (!isOpen) {
                        t.close(0, amount);
                    }
                    allTickets.add(t);

                    try {
                        int idNum = Integer.parseInt(tid.replace("TKT", ""));
                        if (idNum > maxId) maxId = idNum;
                    } catch (NumberFormatException ignored) {}
                }
            }
            this.ticketCounter = maxId;
        } catch (IOException e) {
            System.out.println("Error loading tickets: " + e.getMessage());
        }
    }

    public ParkingTicket findOpenTicket(String ticketId) {
        for (ParkingTicket t : allTickets) {
            if (t.getTicketId().equals(ticketId) && t.isOpen()) {
                return t;
            }
        }
        return null;
    }
    
    private Booking findBookingById(String bookingId) {
        for (Booking b : bookings) {
            if (b.getBookingId().equals(bookingId)) return b;
        }
        return null;
    }
    
    public String generateSystemReport() {
        return generateSystemReport(false);
    }
    
    public String generateSystemReport(boolean includeStatistics) {
        int occupied = 0;
        int free = 0;
        for (Floor f : lot.getFloors()) {
            for (ParkingSpot p : f.getSpots()) {
                if (p != null) {
                    if (p.isFree()) free++;
                    else occupied++;
                }
            }
        }
        long openTickets = allTickets.stream().filter(ParkingTicket::isOpen).count();

        StringBuilder report = new StringBuilder();
        report.append(String.format("--- System Report ---\nOccupied Spots: %d\nFree Spots: %d\nActive Tickets: %d\n",
            occupied, free, openTickets));
            
        if (includeStatistics) {
            report.append("\n--- System Statistics ---\n");
            report.append(statistics.toString()).append("\n");
        }
        
        return report.toString();
    }

    public String getParkingHistory(Customer c) {
        StringBuilder sb = new StringBuilder();
        sb.append("--- Parking History for ").append(c.getUsername()).append(" ---\n");
        
        List<String> customerPlates = java.util.Arrays.asList(c.getVehiclePlates());
        
        if (customerPlates.isEmpty()) {
            return sb.append("No vehicles registered.\n").toString();
        }
        
        int found = 0;
        for (ParkingTicket t : allTickets) {
            if (customerPlates.contains(t.getVehicleLicense())) {
                 sb.append(String.format("Ticket: %s, Spot: %s, Status: %s\n",
                     t.getTicketId(), t.getSpotId(), t.isOpen() ? "Active" : "Closed"));
                 found++;
            }
        }
        
        if (found == 0) {
            sb.append("No parking history found.\n");
        }
        return sb.toString();
    }

    public String getLiveStatus() {
        StringBuilder sb = new StringBuilder();
        for (Floor f : lot.getFloors()) {
            sb.append("--- Floor ").append(f.getFloorNumber()).append(" ---\n");
            for (ParkingSpot p : f.getSpots()) {
                if (p == null) continue;
                sb.append(String.format("  Spot %s (%s): %s", p.getSpotId(), p.getSpotType(), p.getStatus()));
                if (p.getStatus().equals(Constants.SPOT_OCCUPIED)) {
                    sb.append(" (").append(p.getCurrentVehicleLicense()).append(")");
                } else if (p.getStatus().equals(Constants.SPOT_RESERVED)) {
                    sb.append(" [Reserved by: ").append(p.getReservedUsername()).append("]");
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }
    
}