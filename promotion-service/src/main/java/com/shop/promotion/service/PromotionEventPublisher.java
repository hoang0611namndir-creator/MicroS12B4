package com.shop.promotion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.promotion.event.PromotionChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * PUBLISHER: bắn thông điệp {"product_id": 1, "published_at": ...} vào Redis channel "promotion-updates".
 * Phải chạy SAU commit, nếu không subscriber có thể xóa cache rồi nạp lại dữ liệu khuyến mãi cũ.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromotionEventPublisher {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Value("${app.redis.channel}")
    private String channel;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPromotionChanged(PromotionChangedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(
                    Map.of("product_id", event.productId(), "published_at", System.currentTimeMillis()));
            Long receivers = redis.convertAndSend(channel, payload);   // trả về số subscriber nhận được
            log.info("[Promotion] PUBLISH {} -> {} ({} subscriber)", channel, payload, receivers);
        } catch (Exception e) {
            // Pub/Sub là "bắn rồi quên": lỗi ở đây chỉ log; TTL của cache ở Product-Service là lưới an toàn.
            log.error("[Promotion] Publish thất bại, productId={}", event.productId(), e);
        }
    }
}
