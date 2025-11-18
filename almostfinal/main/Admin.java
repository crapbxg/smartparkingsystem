package main;

import util.*;
import service.ParkingSystem;

public class Admin extends User implements Reportable, Loggable {
    private String[] auditLogs = new String[500];
    private int logCount = 0;
    private ParkingSystem system;
    
    public Admin(String username, String password, String name) {
        super(username, password, name, Constants.ROLE_ADMIN);
    }
    
    public void setSystem(ParkingSystem system) {
        this.system = system;
    }

    @Override
    public void showDashboard() {
        System.out.println("   Admin Dashboard for " + name);
        System.out.println("Logs recorded this session: " + logCount);
    }

    @Override
    public String generateReport() {
        if (system == null) {
            return "Report: System not linked.";
        }
        return system.generateSystemReport();
    }

    @Override
    public void logActivity(String message, Object... args) {
        String m = String.format(message, args);
        if (logCount < auditLogs.length) {
            auditLogs[logCount++] = m;
        }
        System.out.println("[ADMIN LOG] " + m);
    }
 
    //for vararg overloading req only
    public void addSpots(int floorNumber, String... spotIds) {
        logActivity("Add %d spots to floor %d", spotIds.length, floorNumber);
        System.out.println("Adding " + spotIds.length + " spots to floor " + floorNumber);
    }

    public void addSpots(int floorNumber, String spotType, String... spotIds) {
        logActivity("Add %d %s spots to floor %d", spotIds.length, spotType, floorNumber);
        System.out.println("Adding " + spotIds.length + " " + spotType + " spots to floor " + floorNumber);
    }
}