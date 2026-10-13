package com.shop.order.service;

import com.shop.order.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    @Value("${app.kafka.topic}")
    private String topic;

    /** Chỉ gửi sau khi DB commit để không bao giờ có event của một đơn hàng chưa tồn tại. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        // key = orderId -> các event cùng đơn luôn vào cùng partition (giữ thứ tự)
        // send() là bất đồng bộ: không chờ broker xử lý xong -> request của khách trả về ngay.
        kafkaTemplate.send(topic, String.valueOf(event.orderId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Gửi OrderCreatedEvent thất bại, orderId={}", event.orderId(), ex);
                    } else {
                        log.info("Đã gửi OrderCreatedEvent orderId={} -> {}-{}@{}", event.orderId(),
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
