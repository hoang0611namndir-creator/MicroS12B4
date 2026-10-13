package com.shop.order.service;

import com.shop.order.client.CustomerClient;
import com.shop.order.client.InventoryClient;
import com.shop.order.client.ProductClient;
import com.shop.order.domain.*;
import com.shop.order.dto.*;
import com.shop.order.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerClient customerClient;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    /**
     * Luồng: validate -> GIỮ HÀNG đồng bộ (khóa phân tán) -> lưu đơn -> (sau COMMIT) bắn event Kafka.
     * Hết hàng => 409 "Sản phẩm đã hết hàng", không tạo đơn.
     * Không đặt @Transactional ở đây để không giữ connection DB trong lúc gọi HTTP sang service khác.
     */
    public Order create(CreateOrderRequest request) {
        CustomerDto customer = customerClient.getById(request.customerId());

        Order order = new Order();
        order.setCustomerId(customer.id());
        order.setCustomerName(customer.name());
        order.setCustomerEmail(customer.email());
        order.setStatus(OrderStatus.CONFIRMED);
        order.setCreatedAt(Instant.now());

        BigDecimal total = BigDecimal.ZERO;
        for (CreateOrderRequest.Item item : request.items()) {
            ProductDto product = productClient.getById(item.productId());
            order.getItems().add(new OrderItem(product.id(), product.name(), item.quantity(), product.price()));
            total = total.add(product.price().multiply(BigDecimal.valueOf(item.quantity())));
        }
        order.setTotalAmount(total);

        inventoryClient.reserve(request.items()); // ném 409 nếu hết hàng

        try {
            return transactionTemplate.execute(status -> {
                Order saved = orderRepository.save(order);
                eventPublisher.publishEvent(toEvent(saved)); // chỉ gửi Kafka khi COMMIT thành công
                return saved;
            });
        } catch (RuntimeException e) {
            log.error("Lưu đơn thất bại sau khi giữ hàng -> hoàn kho", e);
            inventoryClient.release(request.items());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Order getById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    private OrderCreatedEvent toEvent(Order o) {
        List<OrderCreatedEvent.Item> items = o.getItems().stream()
                .map(i -> new OrderCreatedEvent.Item(i.getProductId(), i.getProductName(), i.getQuantity(), i.getUnitPrice()))
                .toList();
        return new OrderCreatedEvent(o.getId(), o.getCustomerId(), o.getCustomerName(), o.getCustomerEmail(),
                o.getTotalAmount(), items, o.getCreatedAt());
    }
}
