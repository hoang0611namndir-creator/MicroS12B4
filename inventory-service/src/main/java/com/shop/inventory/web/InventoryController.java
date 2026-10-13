package com.shop.inventory.web;

import com.shop.inventory.domain.Stock;
import com.shop.inventory.domain.StockRepository;
import com.shop.inventory.dto.StockRequest;
import com.shop.inventory.service.StockReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final StockRepository repository;
    private final StockReservationService reservationService;

    @GetMapping
    public List<Stock> findAll() {
        return repository.findAll();
    }

    @GetMapping("/{productId}")
    public Stock get(@PathVariable Long productId) {
        return repository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No stock for product " + productId));
    }

    /** Đặt lại số lượng tồn kho (nhập hàng / chuẩn bị flash sale). */
    @PutMapping("/{productId}")
    public Stock setQuantity(@PathVariable Long productId, @RequestParam int quantity) {
        return repository.save(new Stock(productId, quantity));
    }

    /** 204 nếu giữ hàng thành công, 409 "Sản phẩm đã hết hàng" nếu không đủ. Order-Service gọi nội bộ. */
    @PostMapping("/reserve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reserve(@Valid @RequestBody StockRequest request) {
        reservationService.reserve(request.items());
    }

    @PostMapping("/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@Valid @RequestBody StockRequest request) {
        reservationService.release(request.items());
    }
}
