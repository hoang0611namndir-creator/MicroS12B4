package com.shop.promotion.service;

import com.shop.promotion.domain.Promotion;
import com.shop.promotion.domain.PromotionRepository;
import com.shop.promotion.dto.UpsertPromotionRequest;
import com.shop.promotion.event.PromotionChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Promotion upsert(Long productId, UpsertPromotionRequest request) {
        Promotion promotion = repository.findById(productId).orElseGet(Promotion::new);
        promotion.setProductId(productId);
        promotion.setDiscountPercent(request.discountPercent());
        promotion.setActive(request.active());
        promotion.setUpdatedAt(Instant.now());
        Promotion saved = repository.save(promotion);
        eventPublisher.publishEvent(new PromotionChangedEvent(productId)); // publish sau COMMIT
        return saved;
    }

    @Transactional
    public void remove(Long productId) {
        if (!repository.existsById(productId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No promotion for product " + productId);
        }
        repository.deleteById(productId);
        eventPublisher.publishEvent(new PromotionChangedEvent(productId));
    }

    @Transactional(readOnly = true)
    public Promotion get(Long productId) {
        return repository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No promotion for product " + productId));
    }

    @Transactional(readOnly = true)
    public List<Promotion> findAll() {
        return repository.findAll();
    }
}
