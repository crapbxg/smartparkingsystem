package main;

public abstract class Vehicle {
    protected String licensePlate;
    protected String ownerUsername;
    protected String vehicleType; // "CAR" or "MOTORCYCLE"


    public Vehicle(String licensePlate, String ownerUsername, String vehicleType) {
        this.licensePlate = licensePlate;
        this.ownerUsername = ownerUsername;
        this.vehicleType = vehicleType;
    }


    public String getLicensePlate() { return licensePlate; }
    public String getVehicleType() { return vehicleType; }

}