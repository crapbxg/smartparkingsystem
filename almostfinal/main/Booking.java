package main;

public class Booking {
    private String bookingId;
    private String username;
    private String vehicleLicense;
    private String vehicleType;
    private String preferredSpotId;
    private String status;

    public Booking(String bookingId, String username, String vehicleLicense, String vehicleType, String preferredSpotId) {
        this.bookingId = bookingId;
        this.username = username;
        this.vehicleLicense = vehicleLicense;
        this.vehicleType = vehicleType;
        this.preferredSpotId = preferredSpotId;
        this.status = "ACTIVE";
    }

    public String getBookingId() { return bookingId; }
    public String getUsername() { return username; }
    public String getVehicleLicense() { return vehicleLicense; }
    public String getVehicleType() { return vehicleType; }
    public String getPreferredSpotId() { return preferredSpotId; }
    public String getStatus() { return status; }

    public void cancel() { 
        this.status = "CANCELLED"; 
    }
    
    public void consume() { 
        this.status = "CONSUMED"; 
    }
 
    public boolean isActive() {
        return "ACTIVE".equals(this.status);
    }
}