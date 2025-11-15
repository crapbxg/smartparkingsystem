package service;

import main.*;
import exceptions.SlotUnavailableException;
import util.Constants;

import java.io.*;

/**
 * ParkingSystem: core coordinator (booking, entry, exit)
 */
public class ParkingSystem {
    private ParkingLot lot;
    private AuthService auth;
    private BillingService billing;
    private Booking[] bookings;
    private int bookingCount;
    private ParkingTicket[] activeTickets;
    private int ticketCount;
    private String bookingsFilePath;

    public ParkingSystem(ParkingLot lot, AuthService auth, BillingService billing, String bookingsFilePath) {
        this.lot = lot;
        this.auth = auth;
        this.billing = billing;
        this.bookingsFilePath = bookingsFilePath;
        this.bookings = new Booking[1000];
        this.bookingCount = 0;
        this.activeTickets = new ParkingTicket[1000];
        this.ticketCount = 0;
        loadBookingsFromFile();
    }

    private void loadBookingsFromFile() {
        File f = new File(bookingsFilePath);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split("\\|", -1);
                if (p.length < 7) continue;
                String id = p[0];
                String user = p[1];
                String veh = p[2];
                String pref = p[3];
                long s = Long.parseLong(p[4]);
                long e = Long.parseLong(p[5]);
                String status = p[6];
                Booking b = new Booking(id, user, veh, pref, s, e);
                if (!status.equals("ACTIVE")) {
                    if (status.equals("EXPIRED")) b.markExpired();
                    else if (status.equals("CANCELLED")) b.cancel();
                }
                if (bookingCount < bookings.length) bookings[bookingCount++] = b;
            }
        } catch (IOException ex) {
            System.out.println("Error loading bookings: " + ex.getMessage());
        }
    }

    private void saveBookingsToFile() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(bookingsFilePath, false))) {
            for (int i = 0; i < bookingCount; i++) {
                Booking b = bookings[i];
                if (b == null) continue;
                String line = String.join("|", new String[]{b.getBookingId(), b.getUsername(), b.getVehicleLicense(), b.getPreferredSpotId(), String.valueOf(b.getStartTimeMillis()), String.valueOf(b.getEndTimeMillis()), b.getStatus()});
                bw.write(line);
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error saving bookings: " + e.getMessage());
        }
    }

    public Booking bookSlot(Customer c, Vehicle v) throws SlotUnavailableException {
        ParkingSpot p = lot.findFreeSpotForVehicle(v.getVehicleType());
        if (p == null) throw new SlotUnavailableException("No free spots available");
        long now = System.currentTimeMillis();
        long ends = now + 60L * 60L * 1000L; // default 1 hour
        String id = c.getUsername() + "_" + now;
        Booking b = new Booking(id, c.getUsername(), v.getLicensePlate(), p.getSpotId(), now, ends);
        if (bookingCount < bookings.length) bookings[bookingCount++] = b;
        saveBookingsToFile();
        return b;
    }

    public Booking bookSlot(Customer c, Vehicle v, String preferredSpotId) throws SlotUnavailableException {
        ParkingSpot pref = lot.findSpot(preferredSpotId);
        if (pref != null && pref.isFree() && pref.getSpotType().equals(v.getVehicleType())) {
            long now = System.currentTimeMillis();
            long ends = now + 60L * 60L * 1000L;
            String id = c.getUsername() + "_" + now;
            Booking b = new Booking(id, c.getUsername(), v.getLicensePlate(), pref.getSpotId(), now, ends);
            if (bookingCount < bookings.length) bookings[bookingCount++] = b;
            saveBookingsToFile();
            return b;
        }
        return bookSlot(c, v);
    }

    public boolean cancelBooking(String bookingId, String username) {
        for (int i = 0; i < bookingCount; i++) {
            Booking b = bookings[i];
            if (b != null && b.getBookingId().equals(bookingId) && b.getUsername().equals(username) && b.getStatus().equals("ACTIVE")) {
                b.cancel();
                saveBookingsToFile();
                return true;
            }
        }
        return false;
    }

    public ParkingTicket markEntry(String username, String vehicleLicense) {
        ParkingSpot chosen = null;
        for (int i = 0; i < bookingCount; i++) {
            Booking b = bookings[i];
            if (b != null && b.getUsername().equals(username) && b.getVehicleLicense().equals(vehicleLicense) && b.getStatus().equals("ACTIVE")) {
                String pref = b.getPreferredSpotId();
                ParkingSpot p = lot.findSpot(pref);
                if (p != null && (p.isFree() || p.getStatus().equals(Constants.SPOT_RESERVED))) {
                    chosen = p;
                    b.markExpired();
                    break;
                }
            }
        }
        if (chosen == null) {
            chosen = lot.findFreeSpotForVehicle(Constants.VEHICLE_CAR);
            if (chosen == null) chosen = lot.findFreeSpotForVehicle(Constants.VEHICLE_MOTORCYCLE);
        }
        if (chosen == null) return null;
        boolean ok = chosen.occupy(vehicleLicense);
        if (!ok) return null;
        long now = System.currentTimeMillis();
        String ticketId = username + "_T_" + now;
        ParkingTicket t = new ParkingTicket(ticketId, vehicleLicense, chosen.getSpotId(), now);
        if (ticketCount < activeTickets.length) activeTickets[ticketCount++] = t;
        updateUsersCurrentEntry(username, vehicleLicense, chosen.getSpotId(), now);
        saveBookingsToFile();
        updateParkingLotFile();
        return t;
    }

    public Double markExit(String ticketId) throws Exception {
        for (int i = 0; i < ticketCount; i++) {
            ParkingTicket t = activeTickets[i];
            if (t != null && t.getTicketId().equals(ticketId) && t.isOpen()) {
                long now = System.currentTimeMillis();
                Double amount = billing.calculateFee(t.getEntryTimeMillis(), now);
                t.closeTicket(now, amount);
                String username = extractUsernameFromTicketId(ticketId);
                User u = auth.findUserByUsername(username);
                if (u instanceof Customer) {
                    Customer c = (Customer) u;
                    boolean paid = c.getAccount().processPayment(username, amount.doubleValue());
                    if (!paid) throw new Exception("Payment failed: insufficient funds");
                }
                String spotId = t.getSpotId();
                ParkingSpot p = lot.findSpot(spotId);
                if (p != null) p.vacate();
                auth.logoutByUsername(username);
                updateParkingLotFile();
                return amount;
            }
        }
        return null;
    }

    private String extractUsernameFromTicketId(String ticketId) {
        String[] parts = ticketId.split("_T_");
        if (parts.length >= 1) return parts[0];
        return ticketId;
    }

    private void updateUsersCurrentEntry(String username, String vehicleLicense, String spotId, long entryTime) {
        String usersCurrentPath = auth.getUsersCurrentFilePath();
        File f = new File(usersCurrentPath);
        try {
            StringBuilder sb = new StringBuilder();
            boolean found = false;
            if (f.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.trim().isEmpty()) continue;
                        String[] parts = line.split("\\|", -1);
                        if (parts.length >= 1 && parts[0].equals(username)) {
                            String token = parts.length >= 2 ? parts[1] : "";
                            String newline = String.join("|", new String[]{username, token, vehicleLicense, spotId, String.valueOf(entryTime)});
                            sb.append(newline).append(System.lineSeparator());
                            found = true;
                        } else {
                            sb.append(line).append(System.lineSeparator());
                        }
                    }
                }
            }
            if (!found) {
                String token = auth.generateSessionToken(username);
                String newline = String.join("|", new String[]{username, token, vehicleLicense, spotId, String.valueOf(entryTime)});
                sb.append(newline).append(System.lineSeparator());
            }
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(f, false))) {
                bw.write(sb.toString());
            }
        } catch (IOException e) {
            System.out.println("Error updating users_current: " + e.getMessage());
        }
    }

    private void updateParkingLotFile() {
        File f = new File("parkinglot.txt");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f, false))) {
            Floor[] floors = lot.getFloors();
            for (int i = 0; i < floors.length; i++) {
                ParkingSpot[] spots = floors[i].getSpots();
                for (int j = 0; j < spots.length; j++) {
                    ParkingSpot p = spots[j];
                    if (p == null) continue;
                    String line = String.join("|", new String[]{p.getSpotId(), p.getSpotType(), p.getStatus(), p.getCurrentVehicleLicense()});
                    bw.write(line);
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            System.out.println("Error writing parkinglot file: " + e.getMessage());
        }
    }

    public ParkingSpot findSpotById(String spotId) {
        return lot.findSpot(spotId);
    }

    public String getLiveStatus() {
        StringBuilder sb = new StringBuilder();
        Floor[] floors = lot.getFloors();
        for (int i = 0; i < floors.length; i++) {
            sb.append("Floor ").append(floors[i].getFloorNumber()).append("\n");
            ParkingSpot[] spots = floors[i].getSpots();
            for (int j = 0; j < spots.length; j++) {
                ParkingSpot p = spots[j];
                if (p == null) continue;
                sb.append(p.getSpotId()).append(": ").append(p.getStatus()).append(" ").append(p.getCurrentVehicleLicense()).append("\n");
            }
        }
        return sb.toString();
    }

    public Booking[] getActiveBookings() { return bookings; }

    public ParkingTicket[] getActiveTickets() { return activeTickets; }

    // Nested class: BookingValidator (non-static inner class)
    private class BookingValidator {
        public boolean canBook(Customer c, Vehicle v, long startMillis, long endMillis) {
            for (int i = 0; i < bookingCount; i++) {
                Booking b = bookings[i];
                if (b == null) continue;
                if (b.getVehicleLicense().equals(v.getLicensePlate()) && b.getStatus().equals("ACTIVE")) {
                    if (!(endMillis < b.getStartTimeMillis() || startMillis > b.getEndTimeMillis())) return false;
                }
            }
            return true;
        }
    }
}
