package com.shop.email.service;

import com.shop.email.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final EmailSender emailSender;

    @KafkaListener(topics = "${app.kafka.topic}", groupId = "${app.kafka.group-id}")
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("[Email] Nhận OrderCreatedEvent orderId={}", event.orderId());

        StringBuilder body = new StringBuilder();
        body.append("Xin chào ").append(event.customerName()).append(",\n\n")
            .append("Cảm ơn bạn đã đặt hàng. Mã đơn: #").append(event.orderId()).append("\n");
        event.items().forEach(i -> body.append(" - ").append(i.productName())
                .append(" x").append(i.quantity())
                .append(" @ ").append(i.unitPrice()).append("\n"));
        body.append("\nTổng tiền: ").append(event.totalAmount());

        emailSender.send(event.customerEmail(), "Xác nhận đơn hàng #" + event.orderId(), body.toString());
    }
}
