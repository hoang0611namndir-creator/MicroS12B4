package com.shop.product.service;

import com.shop.product.Product;
import com.shop.product.ProductRepository;
import com.shop.product.cache.ProductCacheService;
import com.shop.product.client.PromotionClient;
import com.shop.product.dto.ProductView;
import com.shop.product.dto.PromotionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductQueryService {

    public record Lookup(ProductView view, boolean cacheHit) {
    }

    private final ProductRepository productRepository;
    private final ProductCacheService cache;
    private final PromotionClient promotionClient;

    /** Cache-aside: có trong Redis -> trả luôn; thiếu -> lấy DB + khuyến mãi mới nhất rồi nạp lại cache. */
    public Lookup find(Long id) {
        Optional<ProductView> cached = cache.get(id);
        if (cached.isPresent()) {
            return new Lookup(cached.get(), true);
        }

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + id));

        ProductView view;
        boolean cacheable = true;
        try {
            Optional<PromotionDto> promo = promotionClient.findByProductId(id).filter(PromotionDto::active);
            view = toView(product, promo);
        } catch (RestClientException e) {
            // Promotion-Service không trả lời: phục vụ giá gốc nhưng KHÔNG cache để lần sau thử lại.
            log.warn("[Product] Không lấy được khuyến mãi productId={}: {}", id, e.getMessage());
            view = toView(product, Optional.empty());
            cacheable = false;
        }
        if (cacheable) {
            cache.put(view);
        }
        log.info("[Product] MISS productId={} -> nạp từ DB, giá cuối={}", id, view.price());
        return new Lookup(view, false);
    }

    private ProductView toView(Product p, Optional<PromotionDto> promo) {
        if (promo.isEmpty()) {
            return new ProductView(p.getId(), p.getName(), p.getDescription(), p.getPrice(), null, p.getPrice());
        }
        int pct = promo.get().discountPercent();
        BigDecimal finalPrice = p.getPrice().multiply(BigDecimal.valueOf(100 - pct))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        return new ProductView(p.getId(), p.getName(), p.getDescription(), p.getPrice(), pct, finalPrice);
    }
}
