package com.shop.order.client;

import com.shop.order.dto.CustomerDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CustomerClient {

    private final RestClient rest;

    public CustomerClient(@Value("${services.customer.url}") String baseUrl) {
        this.rest = RestClient.create(baseUrl);
    }

    public CustomerDto getById(Long id) {
        try {
            return rest.get().uri("/customers/{id}", id).retrieve().body(CustomerDto.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer not found: " + id);
        }
    }
}
