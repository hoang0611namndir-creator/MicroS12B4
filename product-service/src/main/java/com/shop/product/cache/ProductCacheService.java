package com.shop.product.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.product.dto.ProductView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/** Cache sản phẩm trên Redis: key = product:{id}. Redis lỗi thì coi như cache miss (không làm sập API). */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCacheService {

    private static final String KEY_PREFIX = "product:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Value("${app.cache.product-ttl-seconds}")
    private long ttlSeconds;

    public Optional<ProductView> get(Long productId) {
        try {
            String json = redis.opsForValue().get(KEY_PREFIX + productId);
            return json == null ? Optional.empty() : Optional.of(objectMapper.readValue(json, ProductView.class));
        } catch (Exception e) {
            log.warn("[Cache] Đọc cache lỗi productId={}: {}", productId, e.getMessage());
            return Optional.empty();
        }
    }

    public void put(ProductView view) {
        try {
            redis.opsForValue().set(KEY_PREFIX + view.id(), objectMapper.writeValueAsString(view),
                    Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            log.warn("[Cache] Ghi cache lỗi productId={}: {}", view.id(), e.getMessage());
        }
    }

    /** @return true nếu key tồn tại và đã bị xóa */
    public boolean evict(Long productId) {
        return Boolean.TRUE.equals(redis.delete(KEY_PREFIX + productId));
    }
}
