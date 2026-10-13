package com.shop.order.client;

import com.shop.order.dto.CreateOrderRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class InventoryClient {

    private final RestClient rest;

    public InventoryClient(@Value("${services.inventory.url}") String baseUrl) {
        this.rest = RestClient.create(baseUrl);
    }

    /** Giữ hàng đồng bộ (có khóa phân tán ở Inventory-Service). */
    public void reserve(List<CreateOrderRequest.Item> items) {
        try {
            rest.post().uri("/inventory/reserve").body(Map.of("items", items)).retrieve().toBodilessEntity();
        } catch (HttpClientErrorException.Conflict e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sản phẩm đã hết hàng");
        } catch (HttpServerErrorException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Hệ thống đang bận, vui lòng thử lại");
        }
    }

    /** Hoàn kho khi lưu đơn lỗi. Không ném lỗi để không che lỗi gốc. */
    public void release(List<CreateOrderRequest.Item> items) {
        try {
            rest.post().uri("/inventory/release").body(Map.of("items", items)).retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.error("Hoàn kho thất bại, cần xử lý thủ công: {}", items, e);
        }
    }
}
