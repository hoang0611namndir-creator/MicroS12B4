package com.shop.order.client;

import com.shop.order.dto.ProductDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ProductClient {

    private final RestClient rest;

    public ProductClient(@Value("${services.product.url}") String baseUrl) {
        this.rest = RestClient.create(baseUrl);
    }

    public ProductDto getById(Long id) {
        try {
            return rest.get().uri("/products/{id}", id).retrieve().body(ProductDto.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product not found: " + id);
        }
    }
}
