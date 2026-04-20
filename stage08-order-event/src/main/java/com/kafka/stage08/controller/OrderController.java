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
 * [테스트 방법]
 *
 * 주문 생성:
 *   curl -X POST http://localhost:8087/orders \
 *     -H "Content-Type: application/json" \
 *     -d '{
 *       "productId": "PROD-A",
 *       "productName": "노트북",
 *       "userId": "user-1",
 *       "userEmail": "user1@example.com",
 *       "quantity": 1,
 *       "unitPrice": 1500000,
 *       "shippingAddress": "서울시 강남구"
 *     }'
 *
 * 응답: 즉시 반환 (Kafka 발행만 하고 끝)
 * 로그: [재고서비스], [알림서비스] 가 각각 독립적으로 처리됨
 *
 * [1단계 vs 8단계 응답 시간 비교]
 * 1단계: 약 500ms (재고200ms + 알림300ms)
 * 8단계: 약 5-10ms (Kafka 발행만)
 *
 * 여러 주문:
 *   curl -X POST "http://localhost:8087/orders/bulk?count=10"
 */
@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderEventProducer producer;

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

        producer.publish(event);

        long elapsed = System.currentTimeMillis() - start;
        log.info("[주문서비스] 주문 이벤트 발행 완료 - orderId: {}, 응답시간: {}ms", orderId, elapsed);

        return ResponseEntity.ok(Map.of(
            "orderId", orderId,
            "status", "ORDER_CREATED",
            "responseTimeMs", elapsed,
            "message", "주문이 접수되었습니다. 재고 차감 및 알림은 비동기로 처리됩니다."
        ));
    }

    @PostMapping("/bulk")
    public ResponseEntity<Map<String, Object>> createBulkOrders(
            @RequestParam(defaultValue = "5") int count) {
        long start = System.currentTimeMillis();

        for (int i = 1; i <= count; i++) {
            String orderId = "BULK-" + i;
            OrderEvent event = OrderEvent.builder()
                .orderId(orderId)
                .productId("PROD-" + (i % 3 + 1))
                .productName("상품-" + (i % 3 + 1))
                .userId("user-" + (i % 3 + 1))
                .userEmail("user" + (i % 3 + 1) + "@example.com")
                .quantity(i)
                .unitPrice(10000L * i)
                .totalPrice(10000L * i * i)
                .status("ORDER_CREATED")
                .orderedAt(LocalDateTime.now())
                .shippingAddress("서울시 강남구 " + i + "번지")
                .build();
            producer.publish(event);
        }

        long elapsed = System.currentTimeMillis() - start;
        return ResponseEntity.ok(Map.of(
            "published", count,
            "responseTimeMs", elapsed
        ));
    }

    record OrderCreateRequest(
        String productId, String productName,
        String userId, String userEmail,
        int quantity, long unitPrice,
        String shippingAddress
    ) {}
}
