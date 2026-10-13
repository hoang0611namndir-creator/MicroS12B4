package com.shop.email.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderCreatedEvent(
        Long orderId,
        Long customerId,
        String customerName,
        String customerEmail,
        BigDecimal totalAmount,
        List<Item> items,
        Instant createdAt) {

    public record Item(Long productId, String productName, int quantity, BigDecimal unitPrice) {
    }
}
