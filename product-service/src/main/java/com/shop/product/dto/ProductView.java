package com.shop.product.dto;

import java.math.BigDecimal;

/**
 * Dữ liệu trả cho client và lưu trong Redis cache.
 * price = giá cuối cùng (đã áp khuyến mãi) - Order-Service dùng trường này để tính tiền.
 */
public record ProductView(
        Long id,
        String name,
        String description,
        BigDecimal originalPrice,
        Integer discountPercent,   // null nếu không có khuyến mãi
        BigDecimal price) {
}
