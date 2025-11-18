package main;

import util.Constants;

public class Attendant extends User {
    private int vehiclesProcessed = 0;

    public Attendant(String username, String password, String name) {
        super(username, password, name, Constants.ROLE_ATTENDANT);
    }

    @Override
    public void showDashboard() {
        System.out.println("Vehicles processed today: " + vehiclesProcessed);
    }
    
    public void incrementProcessed() {
        vehiclesProcessed++;
    }
    
    public int getVehiclesProcessed() {
        return vehiclesProcessed;
    }
    
}