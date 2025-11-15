package main;

import exceptions.InsufficientFundsException;
import util.PaymentGateway;


public class Account implements PaymentGateway {
    private Double balance;
    private String ownerUsername;


    public Account(String ownerUsername, double initialBalance) {
        this.ownerUsername = ownerUsername;
        this.balance = Double.valueOf(initialBalance);
    }


    public Account(String ownerUsername) {
        this(ownerUsername, 0.0);
    }


    public void deposit(double amount) {
        balance = Double.valueOf(balance.doubleValue() + amount);
    }


    public void withdraw(double amount) throws InsufficientFundsException {
        if (balance.doubleValue() < amount) throw new InsufficientFundsException("Insufficient funds: required " + amount);
        balance = Double.valueOf(balance.doubleValue() - amount);
    }


    @Override
    public boolean processPayment(String username, double amount) {
        try {
            withdraw(amount);
            return true;
        } catch (InsufficientFundsException e) {
            return false;
        }
    }


    public Double getBalance() { 
        return balance; 
    }
}