package util;

public interface PaymentGateway {

    boolean processPayment(String username, double amount);
}