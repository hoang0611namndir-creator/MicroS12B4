package com.shop.inventory.service;

import com.shop.inventory.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kho đã được trừ ĐỒNG BỘ (có khóa) lúc đặt hàng nên consumer này KHÔNG trừ lại.
 * Giữ lại làm điểm mở rộng: thống kê, cảnh báo sắp hết hàng, đồng bộ sang hệ thống kho (WMS)...
 */
@Slf4j
@Component
public class OrderEventListener {

    @KafkaListener(topics = "${app.kafka.topic}", groupId = "${app.kafka.group-id}")
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("[Inventory] Xác nhận orderId={} đã giữ hàng trước đó, {} dòng sản phẩm",
                event.orderId(), event.items().size());
    }
}
