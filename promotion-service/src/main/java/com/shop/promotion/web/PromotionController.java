package com.shop.promotion.web;

import com.shop.promotion.domain.Promotion;
import com.shop.promotion.dto.UpsertPromotionRequest;
import com.shop.promotion.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService service;

    /** Admin tạo/sửa khuyến mãi của 1 sản phẩm -> publish thông báo ngay sau khi lưu. */
    @PutMapping("/{productId}")
    public Promotion upsert(@PathVariable Long productId, @Valid @RequestBody UpsertPromotionRequest request) {
        return service.upsert(productId, request);
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long productId) {
        service.remove(productId);
    }

    @GetMapping("/{productId}")
    public Promotion get(@PathVariable Long productId) {
        return service.get(productId);
    }

    @GetMapping
    public List<Promotion> findAll() {
        return service.findAll();
    }
}
