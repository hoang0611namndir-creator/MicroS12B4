package com.shop.order.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Sự kiện phát ra sau khi đơn hàng được lưu thành công.
 * Consumers (email, inventory) tự định nghĩa record tương ứng ở service của mình (không share class).
 */
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
