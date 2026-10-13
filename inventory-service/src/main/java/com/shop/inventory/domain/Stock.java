package com.shop.inventory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Stock {
    @Id
    private Long productId;
    private int quantity;
}
