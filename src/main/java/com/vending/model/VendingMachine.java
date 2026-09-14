package com.vending.model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class VendingMachine {
    private static VendingMachine instance;
    private final Inventory inventory;
    private final DoubleProperty currentBalance = new SimpleDoubleProperty(0.0);
    private final ObservableList<String> logs = FXCollections.observableArrayList();
    private final ObservableList<Product> selection = FXCollections.observableArrayList();

    private VendingMachine() {
        this.inventory = new Inventory();
        seedInventory();
    }

    public static VendingMachine getInstance() {
        if (instance == null) {
            instance = new VendingMachine();
        }
        return instance;
    }

    private void seedInventory() {
        // Drinks
        inventory.addProduct(new Drink("Water", 1.00, "waterBottle.png", "A1"), 15);
        inventory.addProduct(new Drink("Coca Cola", 1.50, "Cocacola.png", "A2"), 12);
        inventory.addProduct(new Drink("Coffee", 2.50, "Coffee.png", "A3"), 10);
        
        // Snacks & Food
        inventory.addProduct(new Snack("Burger", 5.50, "Burger.png", "B1"), 8);
        inventory.addProduct(new Snack("Pizza", 4.00, "Pizza.png", "B2"), 10);
        inventory.addProduct(new Snack("Fries", 2.25, "Fries.png", "B3"), 15);
        inventory.addProduct(new Snack("Drumstick", 3.50, "Drumstick.png", "C1"), 12);
        inventory.addProduct(new Snack("Bread", 1.25, "Bread.png", "C2"), 10);
        
        // Sweets
        inventory.addProduct(new Snack("Cake", 3.00, "Cake.png", "D1"), 5);
        inventory.addProduct(new Snack("Pastry", 2.50, "Pastry cake.png", "D2"), 6);
        inventory.addProduct(new Snack("Cookies", 2.00, "cookies.png", "D3"), 10);
        inventory.addProduct(new Snack("Candy", 0.75, "Candy.png", "E1"), 20);
        inventory.addProduct(new Snack("Chocolate", 1.25, "chocolate.png", "E2"), 15);
    }

    public ObservableList<Product> getSelection() {
        return selection;
    }

    public void addToSelection(Product product) throws OutOfStockException {
        if (inventory.hasStock(product)) {
            selection.add(product);
            log("Selected: " + product.getName() + " (" + product.getDescription() + ")");
        } else {
            log("Error: " + product.getName() + " out of stock!");
            throw new OutOfStockException(product.getName());
        }
    }

    public static final double VAT_RATE = 0.05;
    
    public double getTotalPrice() {
        return selection.stream().mapToDouble(Product::getPrice).sum();
    }
    
    public double getVatAmount() {
        return getTotalPrice() * VAT_RATE;
    }
    
    public double getGrandTotal() {
        return getTotalPrice() + getVatAmount();
    }

    public void clearSelection() {
        selection.clear();
    }

    public Product getProductByCode(String code) {
        return inventory.getProducts().stream()
                .filter(p -> p.getCode().equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }

    public void insertMoney(double amount) {
        currentBalance.set(currentBalance.get() + amount);
        log("Inserted: $" + String.format("%.2f", amount));
    }

    public boolean processCheckout() throws InsufficientFundsException, OutOfStockException {
        double subtotal = getTotalPrice();
        double grandTotal = getGrandTotal();
        
        if (subtotal == 0) return false;

        if (currentBalance.get() < grandTotal) {
            double missing = grandTotal - currentBalance.get();
            log("Error: Insufficient funds. Need $" + String.format("%.2f", missing) + " more.");
            throw new InsufficientFundsException(missing);
        }

        // Check stock for all items
        for (Product p : selection) {
            if (!inventory.hasStock(p)) {
                log("Error: " + p.getName() + " became unavailable.");
                throw new OutOfStockException(p.getName());
            }
        }

        // Process payment and stock
        currentBalance.set(currentBalance.get() - grandTotal);
        for (Product p : selection) {
            inventory.reduceStock(p);
        }

        log("Purchase Complete! Total: $" + String.format("%.2f", grandTotal) + " (Incl. 5% VAT)");
        return true;
    }

    public double refund() {
        double amount = currentBalance.get();
        currentBalance.set(0.0);
        selection.clear();
        if (amount > 0) {
            log("Refunded: $" + String.format("%.2f", amount));
        }
        return amount;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public DoubleProperty currentBalanceProperty() {
        return currentBalance;
    }

    public ObservableList<String> getLogs() {
        return logs;
    }

    private void log(String message) {
        logs.add(0, message); // Add to top
    }

    /**
     * Destructor equivalent in Java (Deprecated but fulfills assignment requirements).
     */
    @Override
    @SuppressWarnings("deprecation")
    protected void finalize() throws Throwable {
        try {
            System.out.println("VendingMachine instance is being destroyed by Garbage Collector.");
        } finally {
            super.finalize();
        }
    }

    // Static helper method (Technical Requirement)
    public static String formatCurrency(double amount) {
        return "$" + String.format("%.2f", amount);
    }

    // Inner classes for concrete products
    public static class Drink extends Product {
        public Drink(String name, double price, String icon, String code) { 
            super(name, price, icon, code); 
        }

        @Override
        public String getDescription() {
            return "Refreshing " + name;
        }
    }

    public static class Snack extends Product {
        public Snack(String name, double price, String icon, String code) { 
            super(name, price, icon, code); 
        }

        @Override
        public String getDescription() {
            return "Delicious " + name;
        }
    }
}
