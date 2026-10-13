package com.shop.order.domain;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.math.BigDecimal;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class OrderItem {
    private Long productId;
    private String productName;
    private int quantity;
    private BigDecimal unitPrice;
}
