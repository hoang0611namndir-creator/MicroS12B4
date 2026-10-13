package com.shop.promotion.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpsertPromotionRequest(
        @Min(1) @Max(90) int discountPercent,
        boolean active) {
}
