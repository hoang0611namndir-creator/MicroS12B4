package com.shop.product.dto;

public record PromotionDto(Long productId, int discountPercent, boolean active) {
}
