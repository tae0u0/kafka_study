package com.kafka.stage01;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 1단계: Kafka 없는 동기 REST 아키텍처
 *
 * [학습 목표]
 * - 전통적인 동기 호출 방식의 구조와 문제점을 이해한다.
 * - 주문 처리 시 재고 차감, 알림 전송이 순서대로 실행될 때 발생하는 문제를 확인한다.
 * - 이 문제들이 Kafka(비동기 메시지 브로커)로 어떻게 해결되는지 대비 포인트를 파악한다.
 *
 * [동기 호출의 문제점]
 * 1. 강한 결합(Tight Coupling): OrderService가 InventoryService, NotificationService를 직접 알고 있다.
 * 2. 장애 전파: 알림 서버가 다운되면 주문 전체가 실패한다.
 * 3. 응답 지연: 재고 차감(200ms) + 알림 전송(300ms) = 총 500ms 지연 (순차 실행)
 * 4. 확장 어려움: 알림 채널이 추가되면 OrderService 코드를 직접 수정해야 한다.
 */
@SpringBootApplication
public class Stage01Application {
    public static void main(String[] args) {
        SpringApplication.run(Stage01Application.class, args);
    }
}
