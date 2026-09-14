package com.vending.model;

/**
 * Base custom exception for the Vending Machine system.
 */
public class VendingException extends Exception {
    public VendingException(String message) {
        super(message);
    }
}
