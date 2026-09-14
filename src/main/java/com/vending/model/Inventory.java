package com.vending.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Inventory {
    private final Map<Product, Integer> stock = new HashMap<>();

    public void addProduct(Product product, int quantity) {
        stock.put(product, stock.getOrDefault(product, 0) + quantity);
    }

    public boolean hasStock(Product product) {
        return stock.getOrDefault(product, 0) > 0;
    }

    public void reduceStock(Product product) {
        if (hasStock(product)) {
            stock.put(product, stock.get(product) - 1);
        }
    }

    public int getQuantity(Product product) {
        return stock.getOrDefault(product, 0);
    }

    public Set<Product> getProducts() {
        return stock.keySet();
    }
}
