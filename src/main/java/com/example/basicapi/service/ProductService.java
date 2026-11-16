package com.example.basicapi.service;

import com.example.basicapi.exception.ResourceNotFoundException;
import com.example.basicapi.model.Product;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final Map<Long, Product> productStore = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    @PostConstruct
    public void initDefaultData() {
        create(new Product(null, "Laptop", "Electronics", new BigDecimal("1200.00"), true));
        create(new Product(null, "Wireless Mouse", "Electronics", new BigDecimal("25.50"), true));
        create(new Product(null, "Desk Chair", "Furniture", new BigDecimal("180.00"), false));
        create(new Product(null, "Notebook", "Stationery", new BigDecimal("4.99"), true));
    }

    public List<Product> getAllProducts(String category) {
        if (category != null && !category.trim().isEmpty()) {
            return productStore.values().stream()
                    .filter(p -> p.getCategory() != null && p.getCategory().equalsIgnoreCase(category.trim()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>(productStore.values());
    }

    public Product getProductById(Long id) {
        Product product = productStore.get(id);
        if (product == null) {
            throw new ResourceNotFoundException("Product with ID " + id + " not found");
        }
        return product;
    }

    public Product create(Product product) {
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be empty");
        }
        Long newId = idGenerator.incrementAndGet();
        product.setId(newId);
        productStore.put(newId, product);
        return product;
    }

    public Product update(Long id, Product updatedProduct) {
        if (!productStore.containsKey(id)) {
            throw new ResourceNotFoundException("Product with ID " + id + " not found");
        }
        if (updatedProduct.getName() == null || updatedProduct.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be empty");
        }
        updatedProduct.setId(id);
        productStore.put(id, updatedProduct);
        return updatedProduct;
    }

    public void delete(Long id) {
        if (!productStore.containsKey(id)) {
            throw new ResourceNotFoundException("Product with ID " + id + " not found");
        }
        productStore.remove(id);
    }
}
