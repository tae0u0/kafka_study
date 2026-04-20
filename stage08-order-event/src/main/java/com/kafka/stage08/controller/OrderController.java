package com.kafka.stage08.controller;

import com.kafka.stage08.dto.OrderEvent;
import com.kafka.stage08.producer.OrderEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * [이벤트 흐름 전체]
 *
 * POST /orders
 *   → OrderEventProducer → order-events
 *   → OrderEventConsumer  → payment-events
 *   → PaymentEventConsumer → delivery-events
 *   → DeliveryEventConsumer (끝)
 *
 * [테스트]
 * curl -X POST http://localhost:8087/orders \
 *   -H "Content-Type: application/json" \
 *   -d '{"productId":"PROD-A","productName":"노트북","userId":"user-1",
 *        "userEmail":"user1@test.com","quantity":1,"unitPrice":1500000,
 *        "shippingAddress":"서울시 강남구"}'
 *
 * 예상 로그 순서:
 *   [OrderProducer]   주문 이벤트 발행 → order-events
 *   [주문 Consumer]   주문 수신 → 결제 이벤트 발행
 *   [PaymentProducer] 결제 이벤트 발행 → payment-events
 *   [결제 Consumer]   결제 수신 → 배송 이벤트 발행
 *   [DeliveryProducer] 배송 이벤트 발행 → delivery-events
 *   [배송 Consumer]   배송 처리 완료
 */
@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderEventProducer orderEventProducer;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody OrderCreateRequest req) {
        long start = System.currentTimeMillis();
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        OrderEvent event = OrderEvent.builder()
            .orderId(orderId)
            .productId(req.productId())
            .productName(req.productName())
            .userId(req.userId())
            .userEmail(req.userEmail())
            .quantity(req.quantity())
            .unitPrice(req.unitPrice())
            .totalPrice((long) req.quantity() * req.unitPrice())
            .status("ORDER_CREATED")
            .orderedAt(LocalDateTime.now())
            .shippingAddress(req.shippingAddress())
            .build();

        orderEventProducer.publish(event);

        long elapsed = System.currentTimeMillis() - start;
        log.info("[주문 API] 이벤트 발행 완료 - orderId: {}, 응답시간: {}ms", orderId, elapsed);

        return ResponseEntity.ok(Map.of(
            "orderId", orderId,
            "status", "ORDER_CREATED",
            "responseTimeMs", elapsed,
            "flow", "order-events → payment-events → delivery-events"
        ));
    }

    @PostMapping("/bulk")
    public ResponseEntity<Map<String, Object>> createBulkOrders(
            @RequestParam(defaultValue = "3") int count) {
        long start = System.currentTimeMillis();

        for (int i = 1; i <= count; i++) {
            OrderEvent event = OrderEvent.builder()
                .orderId("BULK-" + i)
                .productId("PROD-" + (i % 3 + 1))
                .productName("상품-" + (i % 3 + 1))
                .userId("user-" + (i % 3 + 1))
                .userEmail("user" + (i % 3 + 1) + "@test.com")
                .quantity(i)
                .unitPrice(10000L * i)
                .totalPrice(10000L * i * i)
                .status("ORDER_CREATED")
                .orderedAt(LocalDateTime.now())
                .shippingAddress("서울시 강남구 " + i + "번지")
                .build();
            orderEventProducer.publish(event);
        }

        return ResponseEntity.ok(Map.of(
            "published", count,
            "responseTimeMs", System.currentTimeMillis() - start
        ));
    }

    record OrderCreateRequest(
        String productId, String productName,
        String userId, String userEmail,
        int quantity, long unitPrice,
        String shippingAddress
    ) {}
}
