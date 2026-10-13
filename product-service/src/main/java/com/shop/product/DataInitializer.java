package com.shop.product;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository repository;

    @Override
    public void run(String... args) {
        if (repository.count() == 0) {
            repository.save(new Product(null, "Laptop", "Laptop 14 inch", new BigDecimal("15000000")));
            repository.save(new Product(null, "Chuot khong day", "Wireless mouse", new BigDecimal("250000")));
            repository.save(new Product(null, "Ban phim co", "Mechanical keyboard", new BigDecimal("1200000")));
        }
    }
}
