package com.vending.model;

import java.util.Objects;

public abstract class Product {
    protected final String name;
    protected final double price;
    protected final String iconName;
    protected final String code; // e.g., A1, B2

    public Product(String name, double price, String iconName, String code) {
        this.name = name;
        this.price = price;
        this.iconName = iconName;
        this.code = code;
    }

    // Mandatory structure for subclasses to implement (Polymorphism)
    public abstract String getDescription();

    public String getName() { return name; }
    public double getPrice() { return price; }
    public String getIconName() { return iconName; }
    public String getCode() { return code; }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Double.compare(product.price, price) == 0 && Objects.equals(name, product.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, price);
    }
}
