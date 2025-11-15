package main;

import util.Reportable;
import util.Loggable;
import util.Constants;


public class Admin extends User implements Reportable, Loggable {
private String adminKey;
private Integer maxFloors;
private String[] auditLogs;
private int logCount;


public Admin(String username, String hashedPassword, String name, String email, String adminKey) {
super(username, hashedPassword, name, email, Constants.ROLE_ADMIN);
this.adminKey = adminKey;
this.maxFloors = Integer.valueOf(5);
this.auditLogs = new String[1000];
this.logCount = 0;
}


public Admin(String username, String hashedPassword) {
super(username, hashedPassword, "Admin", "admin@local", Constants.ROLE_ADMIN);
this.adminKey = "default";
this.maxFloors = Integer.valueOf(5);
this.auditLogs = new String[1000];
this.logCount = 0;
}


@Override
public void showDashboard() {
System.out.println("--- Admin Dashboard for " + name + " ---");
System.out.println("Use admin commands in CLI to add/remove spots and generate reports.");
}


@Override
public String generateReport() {
// simple stub: in real implementation read files and compile metrics
return "Report: (stub)";
}


@Override
public void logActivity(String message, Object... args) {
String formatted = String.format(message, args);
if (logCount < auditLogs.length) auditLogs[logCount++] = formatted;
System.out.println("[ADMIN LOG] " + formatted);
}


@Override
public void logActivityWithTags(String tag, String message, Object... args) {
String formatted = String.format("[%s] " + message, tag, args);
if (logCount < auditLogs.length) auditLogs[logCount++] = formatted;
System.out.println("[ADMIN LOG] " + formatted);
}


public void addSpots(int floorNumber, String... spotIds) {
// This should call ParkingLot.addSpotToFloor; placeholder
logActivity("Adding %d spots to floor %d", spotIds.length, floorNumber);
}


public void addSpots(int floorNumber, int count, String prefix) {
logActivity("Generating %d spots on floor %d with prefix %s", count, floorNumber, prefix);
}


public void removeSpot(String spotId) {
logActivity("Removing spot %s", spotId);
}
}