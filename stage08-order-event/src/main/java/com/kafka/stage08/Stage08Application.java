package com.kafka.stage08;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 8단계: 실무형 이벤트 기반 주문 시스템
 *
 * [학습 목표]
 * - 1단계의 동기 구조를 완전한 이벤트 기반 아키텍처로 전환한다
 * - 하나의 이벤트를 여러 Consumer가 독립적으로 처리하는 구조를 구현한다
 * - 이벤트 기반 아키텍처의 실제 장점을 체감한다
 *
 * [1단계(동기) vs 8단계(이벤트 기반) 비교]
 *
 * [1단계 동기]
 * 클라이언트 ──────────────────────────────→ 응답 (500ms+)
 *              OrderService
 *                  ├→ InventoryService (200ms 대기)
 *                  └→ NotificationService (300ms 대기)
 *
 * [8단계 이벤트 기반]
 * 클라이언트 ──────────────────────────────→ 응답 (10ms)
 *              OrderService
 *                  └→ Kafka 이벤트 발행 (끝!)
 *
 *                  (비동기, 독립 실행)
 *                  Kafka ──→ InventoryConsumer (재고 차감)
 *                       ──→ NotificationConsumer (알림 전송)
 *                       ──→ (나중에 추가할 서비스들...)
 *
 * [아키텍처 장점]
 * 1. 응답 시간: 500ms → 10ms
 * 2. 장애 격리: 알림 서버 다운 → 주문은 정상, 알림만 나중에 재처리
 * 3. 확장성: 새 Consumer 추가만으로 기능 확장 (기존 코드 무수정)
 * 4. 독립 배포: 재고/알림 서비스를 별도 JVM/컨테이너로 분리 가능
 */
@SpringBootApplication
public class Stage08Application {
    public static void main(String[] args) {
        SpringApplication.run(Stage08Application.class, args);
    }
}
