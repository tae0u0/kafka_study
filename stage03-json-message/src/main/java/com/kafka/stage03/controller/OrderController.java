package com.kafka.stage03.controller;

import com.kafka.stage03.dto.OrderEvent;
import com.kafka.stage03.producer.OrderEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * 주문 이벤트 발행 REST API
 *
 * [테스트 방법]
 *
 * 주문 이벤트 발행:
 *   curl -X POST http://localhost:8082/orders \
 *     -H "Content-Type: application/json" \
 *     -d '{"productId":"PROD-A","userId":"user1","quantity":2,"totalPrice":50000}'
 *
 * 여러 주문 동시 발행:
 *   curl -X POST "http://localhost:8082/orders/bulk?count=5"
 *
 * Kafka UI에서 확인: http://localhost:8989
 *   → Topics → stage03-order-events → Messages 탭에서 JSON 메시지 확인
 */
@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderEventProducer orderEventProducer;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody OrderCreateRequest request) {
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        OrderEvent event = OrderEvent.of(
            orderId,
            request.productId(),
            request.userId(),
            request.quantity(),
            request.totalPrice()
        );

        orderEventProducer.publishOrderEvent(event);

        return ResponseEntity.ok(Map.of(
            "orderId", orderId,
            "status", "EVENT_PUBLISHED",
            "message", "주문 이벤트가 Kafka에 발행되었습니다."
        ));
    }

    @PostMapping("/bulk")
    public ResponseEntity<Map<String, Object>> createBulkOrders(
            @RequestParam(defaultValue = "3") int count) {
        for (int i = 1; i <= count; i++) {
            String orderId = "BULK-" + i;
            OrderEvent event = OrderEvent.of(
                orderId, "PROD-" + (i % 3 + 1), "user" + i, i, i * 10000L
            );
            orderEventProducer.publishOrderEvent(event);
        }

        return ResponseEntity.ok(Map.of(
            "status", "BULK_PUBLISHED",
            "count", count
        ));
    }

    // Java 16+ Record: 간단한 요청 DTO
    record OrderCreateRequest(String productId, String userId, int quantity, long totalPrice) {}
}
