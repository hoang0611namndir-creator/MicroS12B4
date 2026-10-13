package com.shop.promotion.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

import java.time.Instant;

/** Mỗi sản phẩm có tối đa 1 chương trình khuyến mãi (khóa chính = productId). */
@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Promotion {
    @Id
    private Long productId;
    private int discountPercent;
    private boolean active;
    private Instant updatedAt;
}
