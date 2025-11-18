package main;

import util.Constants;

public class ParkingSpot {
    private String spotId;
    private String spotType;
    private String status;
    private String currentVehicleLicense;
    private String reservedUsername;

    public ParkingSpot(String spotId) {
        this(spotId, Constants.VEHICLE_CAR);
    }

    public ParkingSpot(String spotId, String spotType) {
        this.spotId = spotId;
        this.spotType = spotType;
        this.status = Constants.SPOT_FREE;
        this.currentVehicleLicense = "";
        this.reservedUsername = null;
    }

    public boolean occupy(String vehicleLicense) {
        if (!status.equals(Constants.SPOT_FREE) && !status.equals(Constants.SPOT_RESERVED)) {
            return false;
        }
        this.currentVehicleLicense = vehicleLicense;
        this.status = Constants.SPOT_OCCUPIED;
        return true;
    }

    public boolean vacate() {

        if (!status.equals(Constants.SPOT_OCCUPIED) && !status.equals(Constants.SPOT_RESERVED)) {
            return false;
        }
        this.currentVehicleLicense = "";
        this.reservedUsername = null;
        this.status = Constants.SPOT_FREE;
        return true;
    }

    public boolean reserve(String username) {
        if (!status.equals(Constants.SPOT_FREE)) {
            return false;
        }
        this.status = Constants.SPOT_RESERVED;
        this.reservedUsername = username;
        return true;
    }

    public boolean isFree() {
        return status.equals(Constants.SPOT_FREE);
    }

    public String getSpotId() {
        return spotId;
    }

    public String getStatus() {
        return status;
    }

    public String getCurrentVehicleLicense() {
        return currentVehicleLicense;
    }

    public String getSpotType() {
        return spotType;
    }
    
    public String getReservedUsername() {
        return reservedUsername;
    }
}