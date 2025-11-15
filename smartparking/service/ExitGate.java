package service;

public class ExitGate {
    private ParkingSystem system;
    public ExitGate(ParkingSystem system) { this.system = system; }
    public Double processExit(String ticketId) throws Exception { return system.markExit(ticketId); }
}
