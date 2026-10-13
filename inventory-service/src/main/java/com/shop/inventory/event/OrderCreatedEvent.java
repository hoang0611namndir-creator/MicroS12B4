package com.shop.inventory.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Bản sao "hợp đồng" event phía consumer - chỉ cần các field mình dùng. */
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
