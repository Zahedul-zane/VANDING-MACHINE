package com.vending.model;

public class OutOfStockException extends VendingException {
    public OutOfStockException(String productName) {
        super("Product out of stock: " + productName);
    }
}
