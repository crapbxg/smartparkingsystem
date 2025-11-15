package main;

public class ParkingTicket {
private String ticketId;
private String vehicleLicense;
private String spotId;
private long entryTimeMillis;
private long exitTimeMillis; // 0 if open
private Double amountDue;


public ParkingTicket(String ticketId, String vehicleLicense, String spotId, long entryTimeMillis) {
this.ticketId = ticketId;
this.vehicleLicense = vehicleLicense;
this.spotId = spotId;
this.entryTimeMillis = entryTimeMillis;
this.exitTimeMillis = 0L;
this.amountDue = null;
}


public void closeTicket(long exitMillis, Double amount) {
this.exitTimeMillis = exitMillis;
this.amountDue = amount;
}


public boolean isOpen() { return exitTimeMillis == 0L; }
public String getTicketId() { return ticketId; }
public String getSpotId() { return spotId; }
public long getEntryTimeMillis() { return entryTimeMillis; }
public long getExitTimeMillis() { return exitTimeMillis; }
public Double getAmountDue() { return amountDue; }
}