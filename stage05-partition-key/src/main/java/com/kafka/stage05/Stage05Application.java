package com.kafka.stage05;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 5단계: Partition & Key 기반 순서 보장
 *
 * [학습 목표]
 * - Key를 기준으로 메시지가 특정 파티션에 라우팅되는 원리를 이해한다
 * - 순서 보장이 어떤 조건에서 성립하는지 정확히 이해한다
 * - 파티션 번호를 로그로 직접 확인한다
 *
 * [Kafka 순서 보장 규칙]
 *
 * ✅ 순서 보장: "같은 파티션 안에서"만 순서가 보장된다
 * ❌ 순서 미보장: 파티션이 다르면 메시지 순서가 섞일 수 있다
 *
 * [Key → Partition 결정 공식]
 * partition = hash(key) % 파티션수
 *
 * 예시 (파티션 3개):
 *   key="user-1" → hash("user-1") % 3 = 0 → 항상 Partition 0
 *   key="user-2" → hash("user-2") % 3 = 1 → 항상 Partition 1
 *   key="user-3" → hash("user-3") % 3 = 2 → 항상 Partition 2
 *
 * → user-1의 모든 주문은 Partition 0에만 쌓임 → 처리 순서 보장!
 *
 * [실무 활용]
 * - 사용자별 이벤트 순서 보장: key = userId
 * - 주문별 상태 순서 보장: key = orderId
 * - 계좌별 거래 순서 보장: key = accountId
 */
@SpringBootApplication
public class Stage05Application {
    public static void main(String[] args) {
        SpringApplication.run(Stage05Application.class, args);
    }
}
