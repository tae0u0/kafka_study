package com.kafka.stage04;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 4단계: Consumer Group 이해
 *
 * [학습 목표]
 * - Consumer Group의 개념과 역할을 이해한다
 * - 같은 그룹 내에서 메시지가 파티션 단위로 분산되는 원리를 확인한다
 * - 다른 그룹은 같은 메시지를 독립적으로 소비하는 것을 확인한다
 *
 * [Consumer Group 핵심 개념]
 *
 * 토픽: stage04-topic (파티션 3개)
 * ┌────────────────────────────────────────────────────┐
 * │  Partition 0 │  Partition 1 │  Partition 2         │
 * │  msg1,msg4   │  msg2,msg5   │  msg3,msg6           │
 * └────────────────────────────────────────────────────┘
 *        ↓                ↓                ↓
 * [Group A]
 *   Consumer-A1      Consumer-A2      Consumer-A3
 *   (msg1,msg4)      (msg2,msg5)      (msg3,msg6)
 *   → 각 파티션은 그룹 내 하나의 Consumer만 담당 (경쟁 소비)
 *
 * [Group B]
 *   Consumer-B1 (모든 파티션 읽기 - Consumer가 1개라서)
 *   → Group A와 완전히 독립적으로 처음부터 읽음
 *
 * 결론:
 * - 같은 그룹: 메시지를 나눠 처리 (부하 분산, 처리량 증가)
 * - 다른 그룹: 메시지를 각자 전부 처리 (예: 결제 서비스, 알림 서비스 각각 수신)
 */
@SpringBootApplication
public class Stage04Application {
    public static void main(String[] args) {
        SpringApplication.run(Stage04Application.class, args);
    }
}
