# E-commerce Event-Driven với Kafka

```
Client ──► API Gateway :8080
              ├─ /api/customers/** ─► customer-service :8081
              ├─ /api/products/**  ─► product-service  :8082
              ├─ /api/orders/**    ─► order-service    :8083 ──(OrderCreatedEvent)──► Kafka topic "order-events"
              └─ /api/inventory/** ─► inventory-service:8085                             │
                                                                   ┌──────────────────────┴──────────────┐
                                                        email-service :8084              inventory-service :8085
                                                        group-id = email-group           group-id = inventory-group
```

## Chạy

Yêu cầu: JDK 17+, Maven 3.9+, Docker.

```bash
docker compose up -d            # Kafka (localhost:9092) + Kafka UI (http://localhost:8090)
mvn clean package -DskipTests   # build cả 6 module

# mở 6 terminal (hoặc chạy nền), mỗi service một lệnh:
mvn -pl customer-service  spring-boot:run
mvn -pl product-service   spring-boot:run
mvn -pl order-service     spring-boot:run
mvn -pl inventory-service spring-boot:run
mvn -pl email-service     spring-boot:run
mvn -pl api-gateway       spring-boot:run
```

## Thử nghiệm

Dữ liệu mẫu: customer id 1,2; product id 1,2,3; tồn kho mỗi sản phẩm = 100.

```bash
# Đặt hàng (trả về 202 gần như ngay lập tức)
curl -i -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"customerId":1,"items":[{"productId":1,"quantity":2},{"productId":2,"quantity":1}]}'

# Vài giây sau, kiểm tra kho đã bị trừ (100 -> 98 / 99)
curl http://localhost:8080/api/inventory

# Xem log email-service: email "gửi" sau ~1.5s, nhưng khách đã nhận response từ trước đó.
```

## Các điểm thiết kế đáng chú ý

| Vấn đề | Cách xử lý |
|---|---|
| Khách không phải chờ | `kafkaTemplate.send()` bất đồng bộ; API trả `202 Accepted` |
| Không phát event của đơn chưa lưu | `@TransactionalEventListener(AFTER_COMMIT)` |
| 2 service cùng nhận 1 event | khác `group-id` (`email-group`, `inventory-group`) |
| Thứ tự theo đơn | message key = `orderId` |
| Kafka giao ít nhất 1 lần | Inventory idempotent qua bảng `ProcessedOrder` |
| Coupling class giữa service | không share class; mỗi service có record riêng + `use.type.headers=false` |
| Message lỗi (poison pill) | `ErrorHandlingDeserializer` |

## Hạn chế / hướng mở rộng

- **Outbox pattern**: nếu app chết ngay giữa lúc commit DB và gửi Kafka, event bị mất. Production nên ghi event vào bảng outbox cùng transaction rồi có relay đẩy lên Kafka.
- **Saga**: hết hàng hiện chỉ log; nên phát `InventoryFailedEvent` để Order-Service chuyển đơn sang `CANCELLED`.
- **Cộng điểm thành viên / tạo hóa đơn**: thêm consumer mới (vd. `loyalty-group`, `invoice-group`) trên cùng topic, không cần sửa Order-Service.
- **Dead Letter Topic**: cấu hình `DefaultErrorHandler` + `DeadLetterPublishingRecoverer`.
- **Service discovery**: URL đang hard-code qua `application.yml`; có thể thay bằng Eureka/Consul hoặc Kubernetes DNS.
- Mỗi service dùng H2 in-memory (database-per-service); đổi sang PostgreSQL/MySQL cho môi trường thật.

---

## Flash Sale: khóa phân tán với Redisson

Thay đổi so với bản trước: **trừ kho không còn chạy bất đồng bộ qua Kafka**, vì kiểm tra tồn kho rồi mới trừ
qua một event sẽ có khoảng hở cho 100 người cùng "thấy còn hàng". Giờ việc giữ hàng chạy đồng bộ dưới khóa
`lock:product:{id}` ở Inventory-Service; Kafka vẫn dùng cho email và các hậu xử lý.

```
POST /api/orders
  └ Order-Service: validate khách + sản phẩm
      └ POST inventory/reserve  ─►  Inventory-Service
            tryLock("lock:product:{id}")   ◄── Redisson / Redis
              ├ mở transaction → kiểm tra tồn kho → trừ → COMMIT
              └ finally: unlock
            hết hàng → 409 "Sản phẩm đã hết hàng"
      └ lưu đơn → (sau COMMIT) OrderCreatedEvent → Kafka → email-service
         (lưu đơn lỗi → gọi inventory/release để hoàn kho)
```

Điểm quan trọng trong `ProductLockService` / `StockReservationService`:
- **Khóa trước, transaction sau**: nếu `@Transactional` bao ngoài lock, người giành khóa kế tiếp có thể đọc số liệu cũ khi giao dịch trước chưa commit.
- `unlock()` trong `finally`, có kiểm tra `isHeldByCurrentThread()` để không ném lỗi khi lease đã hết hạn.
- `leaseTime = 10s`: nếu service chết giữa chừng, khóa tự hết hạn thay vì treo mãi.
- Đơn nhiều sản phẩm: khóa theo thứ tự id tăng dần để tránh deadlock.

### Chạy thử

```bash
docker compose up -d        # Kafka + Redis
# chạy các service như trên, rồi:
./flash-sale-test.sh 1 100 1    # 1 món, 100 người cùng mua sản phẩm id=1
```
Kết quả mong đợi: đúng 1 response `202`, 99 response `409` ("Sản phẩm đã hết hàng"), tồn kho = 0, đúng 1 đơn.

Lưu ý: inventory-service dùng H2 in-memory nên chỉ chạy được 1 instance. Muốn chạy nhiều instance để thấy rõ
tác dụng của khóa phân tán, đổi sang DB dùng chung (PostgreSQL/MySQL); khóa Redis đã sẵn sàng cho việc đó.

---

## Redis Pub/Sub: đồng bộ giá khuyến mãi

```
Admin ─PUT /api/promotions/{id}─► promotion-service :8086
                                    │ lưu DB → COMMIT
                                    └─ PUBLISH  promotion-updates  {"product_id":1,"published_at":...}
                                                     │  (Redis Pub/Sub)
                         ┌───────────────────────────┴──────────────┐
                 product-service #1                          product-service #N     (mọi instance đều nhận)
                 DEL product:1                               DEL product:1
                                    │
Client ─GET /api/products/{id}─► cache MISS → đọc DB + GET promotion-service → nạp cache (giá mới)
```

| Thành phần | Vai trò |
|---|---|
| `PromotionEventPublisher` (promotion-service) | `convertAndSend("promotion-updates", {"product_id":..})` sau khi DB commit |
| `RedisSubscriberConfig` + `PromotionUpdateListener` (product-service) | lắng nghe channel, xóa key `product:{id}` |
| `ProductQueryService` | cache-aside; `price` trả về là giá đã áp khuyến mãi, Order-Service dùng giá này |

Chạy thử (cần Redis từ `docker compose up -d`, và 7 service đang chạy: thêm `mvn -pl promotion-service spring-boot:run`):

```bash
./promotion-latency-test.sh 1 30
```
Script in `X-Cache: MISS` rồi `HIT` (cache nóng), sau đó đặt khuyến mãi 30% và đo thời gian đến khi API trả giá mới.
Log product-service sẽ có dòng "độ trễ publish->xóa" (thường vài ms).

Đặc tính cần biết của Redis Pub/Sub:
- **Bắn rồi quên**: subscriber đang tắt/mất kết nối thì **mất** thông điệp (không lưu lại, khác Kafka). Vì vậy cache có TTL 300s làm lưới an toàn.
- Hiếm khi có race: một request đọc khuyến mãi cũ *trước* commit nhưng ghi cache *sau* lệnh xóa -> cache cũ tồn tại tối đa đến hết TTL.
- Cần đảm bảo tuyệt đối: dùng Redis Streams hoặc Kafka thay cho Pub/Sub.
