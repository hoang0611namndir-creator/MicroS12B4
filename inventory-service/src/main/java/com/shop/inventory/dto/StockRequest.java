package com.shop.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record StockRequest(@NotEmpty @Valid List<Item> items) {

    public record Item(@NotNull Long productId, @Min(1) int quantity) {
    }
}
