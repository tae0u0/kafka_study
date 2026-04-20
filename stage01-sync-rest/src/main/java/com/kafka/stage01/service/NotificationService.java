package com.kafka.stage01.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 알림 전송 서비스 (실제로는 별도 마이크로서비스 또는 이메일/SMS 서버라고 가정)
 *
 * [동기 호출 문제 포인트]
 * - 알림 서버가 느리면(300ms) 주문 응답 전체가 느려진다
 * - 알림 서버가 다운되면 주문 자체가 실패한다 (핵심 기능이 부가 기능에 의해 실패)
 * - 이것이 Kafka 도입의 가장 큰 이유 중 하나
 */
@Slf4j
@Service
public class NotificationService {

    public void sendOrderConfirmation(String userId, String orderId) {
        log.info("[알림서비스] 주문 확인 알림 전송 시작 - 사용자: {}, 주문: {}", userId, orderId);

        // 이메일/SMS 전송 시뮬레이션 (300ms 지연)
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 알림 서버 장애 시뮬레이션: 특정 유저에 대해 실패
        if ("ERROR_USER".equals(userId)) {
            throw new RuntimeException("알림 서버 연결 실패 - 주문 처리 전체가 롤백됨!");
        }

        log.info("[알림서비스] 주문 확인 알림 전송 완료 - 사용자: {}, 주문: {}", userId, orderId);
    }
}
