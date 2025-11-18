package service;

import util.Constants;

public class BillingService {
    private Double carHourlyRate;
    private Double bikeHourlyRate;
    private Double minimumCharge;

    public BillingService(Double carHourlyRate, Double bikeHourlyRate, Double minimumCharge) {
        this.carHourlyRate = carHourlyRate;
        this.bikeHourlyRate = bikeHourlyRate;
        this.minimumCharge = minimumCharge;
    }

    public Double calculateFee(long entryMillis, long exitMillis, String vehicleType) {
        long durationMillis = exitMillis - entryMillis;
        if (durationMillis < 0) return 0.0;

        double hours = Math.ceil(durationMillis / (1000.0 * 60.0 * 60.0));
        
        if (hours < 1.0) {
            hours = 1.0;
        }

        double rate = vehicleType.equals(Constants.VEHICLE_MOTORCYCLE) ? bikeHourlyRate : carHourlyRate;
        double rawFee = hours * rate;

        return Math.max(rawFee, minimumCharge);
    }
}