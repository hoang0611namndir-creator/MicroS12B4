package com.shop.customer;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CustomerRepository repository;

    @Override
    public void run(String... args) {
        if (repository.count() == 0) {
            repository.save(new Customer(null, "Nguyen Van A", "vana@example.com"));
            repository.save(new Customer(null, "Tran Thi B", "thib@example.com"));
        }
    }
}
