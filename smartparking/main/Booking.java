package main;

public class Booking {
private String bookingId;
private String username;
private String vehicleLicense;
private String preferredSpotId;
private long startTimeMillis;
private long endTimeMillis;
private String status; // ACTIVE, EXPIRED, CANCELLED, COMPLETED


public Booking(String bookingId, String username, String vehicleLicense, String preferredSpotId, long startTimeMillis, long endTimeMillis) {
this.bookingId = bookingId;
this.username = username;
this.vehicleLicense = vehicleLicense;
this.preferredSpotId = preferredSpotId;
this.startTimeMillis = startTimeMillis;
this.endTimeMillis = endTimeMillis;
this.status = "ACTIVE";
}


public boolean isExpired(long currentMillis) {
return status.equals("ACTIVE") && currentMillis > endTimeMillis;
}


public void cancel() { this.status = "CANCELLED"; }
public void markExpired() { this.status = "EXPIRED"; }
public String getBookingId() { return bookingId; }
public String getUsername() { return username; }
public String getVehicleLicense() { return vehicleLicense; }
public String getPreferredSpotId() { return preferredSpotId; }
public long getStartTimeMillis() { return startTimeMillis; }
public long getEndTimeMillis() { return endTimeMillis; }
public String getStatus() { return status; }
}