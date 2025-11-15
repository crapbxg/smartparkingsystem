package main;

import util.Constants;


public class Attendant extends User {
private String shift;
private String[] handledTickets;
private int ticketCount;


public Attendant(String username, String hashedPassword, String name, String email, String shift) {
super(username, hashedPassword, name, email, Constants.ROLE_ATTENDANT);
this.shift = shift;
this.handledTickets = new String[200];
this.ticketCount = 0;
}


@Override
public void showDashboard() {
System.out.println("--- Attendant Dashboard: " + name + " (" + shift + ") ---");
System.out.println("Use the CLI to mark entry/exit for vehicles.");
}


public void markEntry(String vehiclePlate, String spotId) {
// placeholder - real work done by EntryGate
System.out.println("Marked entry: " + vehiclePlate + " at " + spotId);
}


    public void markExit(String ticketId) {
        System.out.println("Marked exit for ticket: " + ticketId);
    }
}
