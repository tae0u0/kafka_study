package com.kafka.stage05.controller;

import com.kafka.stage05.producer.OrderProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * [테스트 시나리오]
 *
 * 1. Key 있는 전송 (순서 보장):
 *    curl -X POST "http://localhost:8084/orders/with-key?userId=user-1&order=ORDER_PLACED"
 *    curl -X POST "http://localhost:8084/orders/with-key?userId=user-1&order=ORDER_PAID"
 *    curl -X POST "http://localhost:8084/orders/with-key?userId=user-1&order=ORDER_SHIPPED"
 *    → 로그: 항상 같은 Partition, 같은 Thread
 *
 * 2. Key 없는 전송 (순서 미보장):
 *    curl -X POST "http://localhost:8084/orders/bulk-no-key?count=9"
 *    → 로그: 파티션이 섞임, 다른 스레드에서 처리될 수 있음
 *
 * 3. 여러 유저 동시 전송:
 *    curl -X POST "http://localhost:8084/orders/bulk-with-key?count=12"
 *    → user-1,2,3 각각 다른 파티션으로 라우팅됨을 확인
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderProducer producer;

    @PostMapping("/with-key")
    public ResponseEntity<Map<String, String>> sendWithKey(
            @RequestParam String userId, @RequestParam String order) {
        producer.sendOrderWithKey(userId, order);
        return ResponseEntity.ok(Map.of("userId", userId, "order", order));
    }

    @PostMapping("/bulk-with-key")
    public ResponseEntity<Map<String, Object>> sendBulkWithKey(
            @RequestParam(defaultValue = "12") int count) {
        String[] users = {"user-1", "user-2", "user-3"};
        String[] statuses = {"ORDER_PLACED", "ORDER_PAID", "ORDER_SHIPPED", "ORDER_DELIVERED"};

        for (int i = 0; i < count; i++) {
            String userId = users[i % users.length];
            String status = statuses[i % statuses.length] + "-" + (i / users.length + 1);
            producer.sendOrderWithKey(userId, status);
        }
        return ResponseEntity.ok(Map.of("sent", count));
    }

    @PostMapping("/bulk-no-key")
    public ResponseEntity<Map<String, Object>> sendBulkNoKey(
            @RequestParam(defaultValue = "9") int count) {
        for (int i = 1; i <= count; i++) {
            producer.sendOrderWithoutKey("NoKey-Order-" + i);
        }
        return ResponseEntity.ok(Map.of("sent", count, "note", "파티션 무작위 분산"));
    }
}
