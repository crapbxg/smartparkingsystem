package main;

import util.Constants;


public class ParkingSpot {
private String spotId;
private String spotType; // "CAR" or "MOTORCYCLE"
private String status; // "FREE", "OCCUPIED", "RESERVED"
private String currentVehicleLicense;
private long reservedUntilMillis;


public ParkingSpot(String spotId) {
this.spotId = spotId;
this.spotType = Constants.VEHICLE_CAR;
this.status = Constants.SPOT_FREE;
this.currentVehicleLicense = "";
this.reservedUntilMillis = 0L;
}


public ParkingSpot(String spotId, String spotType) {
this.spotId = spotId;
this.spotType = spotType;
this.status = Constants.SPOT_FREE;
this.currentVehicleLicense = "";
this.reservedUntilMillis = 0L;
}


public boolean occupy(String vehicleLicense) {
if (!status.equals(Constants.SPOT_FREE) && !status.equals(Constants.SPOT_RESERVED)) return false;
this.currentVehicleLicense = vehicleLicense;
this.status = Constants.SPOT_OCCUPIED;
this.reservedUntilMillis = 0L;
return true;
}


public boolean vacate() {
if (!status.equals(Constants.SPOT_OCCUPIED)) return false;
this.currentVehicleLicense = "";
this.status = Constants.SPOT_FREE;
return true;
}


public boolean reserve(long untilMillis, String username) {
if (!status.equals(Constants.SPOT_FREE)) return false;
this.status = Constants.SPOT_RESERVED;
this.reservedUntilMillis = untilMillis;
return true;
}


public boolean isFree() { return status.equals(Constants.SPOT_FREE); }
public String getSpotId() { return spotId; }
public String getStatus() { return status; }
public String getCurrentVehicleLicense() { return currentVehicleLicense; }
public void forceFree() { this.status = Constants.SPOT_FREE; this.currentVehicleLicense = ""; this.reservedUntilMillis = 0L; }
public String getSpotType() { return spotType; }
public long getReservedUntilMillis() { return reservedUntilMillis; }
}