package com.vending.model;

public class InsufficientFundsException extends VendingException {
    private final double missingAmount;

    public InsufficientFundsException(double missingAmount) {
        super("Insufficient funds. You need $" + String.format("%.2f", missingAmount) + " more.");
        this.missingAmount = missingAmount;
    }

    public double getMissingAmount() {
        return missingAmount;
    }
}
