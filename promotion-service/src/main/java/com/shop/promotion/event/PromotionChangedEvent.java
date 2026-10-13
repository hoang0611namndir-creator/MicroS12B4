package com.shop.promotion.event;

/** Sự kiện nội bộ (Spring) - sau khi DB commit sẽ được đẩy lên Redis channel. */
public record PromotionChangedEvent(Long productId) {
}
