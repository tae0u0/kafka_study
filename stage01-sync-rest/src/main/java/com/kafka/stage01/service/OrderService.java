package com.kafka.stage01.service;

import com.kafka.stage01.dto.OrderRequest;
import com.kafka.stage01.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 주문 처리 서비스
 *
 * [핵심 문제: 동기 호출 체인]
 *
 * 주문 생성 → 재고 차감(200ms) → 알림 전송(300ms) → 응답 반환
 *
 * 총 처리 시간: 최소 500ms 이상 (순차 실행)
 *
 * 문제점:
 * 1. [성능] 재고 차감과 알림 전송은 독립적인데 왜 순서대로 기다려야 하나?
 * 2. [장애 전파] 알림 서버 장애 → 주문 실패. 부가 기능이 핵심 기능을 죽인다.
 * 3. [결합도] OrderService가 InventoryService, NotificationService를 직접 의존한다.
 *    새 서비스(포인트, 쿠폰 등) 추가 시 이 클래스를 반드시 수정해야 한다.
 * 4. [확장성] 주문량이 급증하면? OrderService를 스케일아웃해도 연쇄 서비스도 함께 부하를 받는다.
 *
 * → Kafka를 쓰면: 주문 저장 후 이벤트만 발행하면 끝. 나머지는 각 Consumer가 비동기로 처리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    // 직접 의존: 강한 결합의 원인
    private final InventoryService inventoryService;
    private final NotificationService notificationService;

    public OrderResponse processOrder(OrderRequest request) {
        long startTime = System.currentTimeMillis();

        log.info("=== 주문 처리 시작 === orderId: {}", request.getOrderId());

        // Step 1: 주문 저장 (DB insert 시뮬레이션)
        log.info("[1단계] 주문 저장 - orderId: {}", request.getOrderId());

        // Step 2: 재고 차감 (동기 호출 - 완료될 때까지 블로킹!)
        log.info("[2단계] 재고 차감 호출 (동기, 블로킹)");
        inventoryService.decreaseStock(request.getProductId(), request.getQuantity());

        // Step 3: 알림 전송 (동기 호출 - 완료될 때까지 블로킹!)
        // 여기서 예외 발생 시 주문 자체가 실패한다 → 핵심 문제!
        log.info("[3단계] 알림 전송 호출 (동기, 블로킹)");
        notificationService.sendOrderConfirmation(request.getUserId(), request.getOrderId());

        long processingTime = System.currentTimeMillis() - startTime;
        log.info("=== 주문 처리 완료 === 총 처리 시간: {}ms (재고차감 200ms + 알림전송 300ms)", processingTime);

        return new OrderResponse(
            request.getOrderId(),
            "SUCCESS",
            processingTime,
            "주문이 완료되었습니다. (동기 처리 총 " + processingTime + "ms 소요)"
        );
    }
}
