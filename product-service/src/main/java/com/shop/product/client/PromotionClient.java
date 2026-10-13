package com.shop.product.client;

import com.shop.product.dto.PromotionDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
public class PromotionClient {

    private final RestClient rest;

    public PromotionClient(@Value("${services.promotion.url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1000);
        factory.setReadTimeout(2000);
        this.rest = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    /** 404 = sản phẩm không có khuyến mãi. Lỗi mạng/5xx sẽ được ném ra cho nơi gọi xử lý. */
    public Optional<PromotionDto> findByProductId(Long productId) {
        try {
            return Optional.ofNullable(
                    rest.get().uri("/promotions/{id}", productId).retrieve().body(PromotionDto.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }
}
