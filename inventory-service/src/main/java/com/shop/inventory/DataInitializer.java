package com.shop.inventory;

import com.shop.inventory.domain.Stock;
import com.shop.inventory.domain.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final StockRepository repository;

    @Override
    public void run(String... args) {
        if (repository.count() == 0) { // khớp 3 sản phẩm seed của product-service
            repository.save(new Stock(1L, 100));
            repository.save(new Stock(2L, 100));
            repository.save(new Stock(3L, 100));
        }
    }
}
