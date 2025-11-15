package service;
import main.ParkingTicket;

public class BillingService {
    private Double hourlyRate;
private Double minimumCharge;


public BillingService(Double hourlyRate) { this(hourlyRate, 0.0); }
public BillingService(Double hourlyRate, Double minimumCharge) {
this.hourlyRate = hourlyRate;
this.minimumCharge = minimumCharge;
}


public Double calculateFee(ParkingTicket ticket) {
if (ticket == null || ticket.getExitTimeMillis() == 0L) return null;
return calculateFee(ticket.getEntryTimeMillis(), ticket.getExitTimeMillis());
}


public Double calculateFee(long entryMillis, long exitMillis) {
if (exitMillis <= entryMillis) return Double.valueOf(0.0);
double hours = (exitMillis - entryMillis) / 3600000.0;
double raw = hours * hourlyRate.doubleValue();
if (raw < minimumCharge.doubleValue()) raw = minimumCharge.doubleValue();
return Double.valueOf(roundTo2Decimals(raw));
}


private double roundTo2Decimals(double value) {
return Math.round(value * 100.0) / 100.0;
}
}
