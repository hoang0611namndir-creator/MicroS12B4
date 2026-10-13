package com.shop.order.web;

import com.shop.order.domain.Order;
import com.shop.order.dto.CreateOrderRequest;
import com.shop.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** 202 Accepted: đã giữ hàng và ghi nhận đơn; email và các bước hậu xử lý chạy nền qua Kafka. */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Order create(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.create(request);
    }

    @GetMapping("/{id}")
    public Order get(@PathVariable Long id) {
        return orderService.getById(id);
    }

    @GetMapping
    public List<Order> list() {
        return orderService.findAll();
    }
}
