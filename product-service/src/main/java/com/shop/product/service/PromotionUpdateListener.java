package com.shop.product.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.product.cache.ProductCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * SUBSCRIBER: nhận {"product_id": 1, ...} từ channel "promotion-updates" và xóa cache sản phẩm đó.
 * Lần truy vấn kế tiếp sẽ miss -> đọc DB + khuyến mãi mới. Mọi instance Product-Service đều nhận được thông báo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromotionUpdateListener implements MessageListener {

    private final ProductCacheService cache;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            JsonNode json = objectMapper.readTree(body);
            JsonNode idNode = json.get("product_id");
            if (idNode == null || !idNode.canConvertToLong()) {
                log.warn("[Product] Thông điệp thiếu product_id: {}", body);
                return;
            }
            long productId = idNode.asLong();
            boolean removed = cache.evict(productId);

            long publishedAt = json.path("published_at").asLong(0);
            String latency = publishedAt > 0 ? (System.currentTimeMillis() - publishedAt) + " ms" : "n/a";
            log.info("[Product] Nhận promotion-updates productId={} -> xóa cache={} (độ trễ publish->xóa: {})",
                    productId, removed, latency);
        } catch (Exception e) {
            // Không ném lỗi ra ngoài để container không dừng lắng nghe vì 1 thông điệp xấu.
            log.error("[Product] Thông điệp không hợp lệ: {}", body, e);
        }
    }
}
