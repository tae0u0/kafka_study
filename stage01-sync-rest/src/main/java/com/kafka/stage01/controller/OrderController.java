package com.kafka.stage01.controller;

import com.kafka.stage01.dto.OrderRequest;
import com.kafka.stage01.dto.OrderResponse;
import com.kafka.stage01.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 주문 REST 컨트롤러
 *
 * 테스트 방법 (curl):
 *   정상 주문: curl -X POST http://localhost:8080/orders \
 *     -H "Content-Type: application/json" \
 *     -d '{"orderId":"ORD-001","productId":"PROD-A","quantity":2,"userId":"user1"}'
 *
 *   재고 부족: productId를 "OUT_OF_STOCK"으로 변경
 *   알림 실패: userId를 "ERROR_USER"로 변경
 */
@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
        log.info("주문 요청 수신 - orderId: {}", request.getOrderId());
        try {
            OrderResponse response = orderService.processOrder(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 동기 호출의 문제: 하위 서비스 예외가 그대로 HTTP 500으로 전파된다
            log.error("주문 처리 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(new OrderResponse(request.getOrderId(), "FAILED", 0, e.getMessage()));
        }
    }

    /**
     * 동기 vs 비동기 비교 설명 엔드포인트
     */
    @GetMapping("/compare")
    public ResponseEntity<String> compareWithKafka() {
        String comparison = """
            === 동기(REST) vs 비동기(Kafka) 비교 ===

            [동기 REST 방식 - 현재 구조]
            클라이언트 → OrderService → InventoryService (200ms 대기)
                                     → NotificationService (300ms 대기)
            총 응답 시간: 500ms+
            문제점:
              - 알림 서버 다운 → 주문 실패
              - 새 서비스 추가 → OrderService 코드 수정 필수
              - 순차 실행으로 인한 응답 지연

            [Kafka 비동기 방식 - 2단계부터 구현]
            클라이언트 → OrderService → Kafka에 이벤트 발행 (5ms)
            응답 완료!

            (별도 스레드/프로세스)
            Kafka → InventoryConsumer (재고 차감, 독립 실행)
            Kafka → NotificationConsumer (알림 전송, 독립 실행)

            장점:
              - 알림 서버 다운 → 주문은 성공, 알림만 나중에 재처리
              - 새 서비스 추가 → Consumer만 추가, 기존 코드 무수정
              - 응답 시간 500ms → 5ms로 단축
            """;
        return ResponseEntity.ok(comparison);
    }
}
