package service;

import main.Booking;

/**
 * BookingExpiryService: simplified (no threads). Call checkOnce() periodically from CLI if needed.
 */
public class BookingExpiryService {
    private ParkingSystem system;
    private long lastCheckedMillis;

    public BookingExpiryService(ParkingSystem system) {
        this.system = system;
        this.lastCheckedMillis = 0L;
    }

    /**
     * Single pass check for expired bookings. Call this from CLI periodically (e.g., every loop).
     */
    public void checkOnce() {
        Booking[] bookings = system.getActiveBookings();
        long now = System.currentTimeMillis();
        boolean changed = false;
        if (bookings == null) return;
        for (int i = 0; i < bookings.length; i++) {
            Booking b = bookings[i];
            if (b == null) continue;
            if (b.isExpired(now)) {
                b.markExpired();
                changed = true;
            }
        }
        if (changed) {
            // The ParkingSystem's saveBooking method is private; but bookings are in-memory,
            // call system.getLiveStatus() to force a write of lot file if you added such a method
            // or implement a public saveBookings method if you want persistence here.
        }
        lastCheckedMillis = now;
    }
}
