package com.kafka.stage05.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * 파티션 기반 Consumer
 *
 * [concurrency = "3" + 파티션 3개]
 * 스레드 0 → Partition 0 담당 (user-1의 모든 주문)
 * 스레드 1 → Partition 1 담당 (user-2의 모든 주문)
 * 스레드 2 → Partition 2 담당 (user-3의 모든 주문)
 *
 * 로그에서 thread 이름 확인:
 * [stage05-group-0-C-1] → 스레드 0 (Partition 0 처리)
 * [stage05-group-0-C-2] → 스레드 1 (Partition 1 처리)
 * [stage05-group-0-C-3] → 스레드 2 (Partition 2 처리)
 *
 * [순서 보장 확인]
 * user-1의 주문은 항상 Partition 0, 스레드 0이 처리
 * → offset 순서대로 처리됨 (ORDER_PLACED → ORDER_PAID → ORDER_SHIPPED)
 */
@Slf4j
@Service
public class OrderConsumer {

    @KafkaListener(
        topics = "stage05-orders",
        groupId = "stage05-group",
        concurrency = "3"  // 파티션 수와 동일하게 설정
    )
    public void consume(ConsumerRecord<String, String> record) {
        String threadName = Thread.currentThread().getName();

        log.info("=== 주문 수신 ===");
        log.info("  Thread   : {}", threadName);      // 어떤 스레드가 처리하는지
        log.info("  Partition: {}", record.partition()); // 파티션 번호
        log.info("  Offset   : {}", record.offset());   // 파티션 내 순서
        log.info("  Key(userId): {}", record.key());    // 라우팅 기준 키
        log.info("  Value    : {}", record.value());
        log.info("================");

        // 관찰:
        // key="user-1" → 항상 같은 partition, 같은 thread 처리
        // key="user-2" → 다른 partition, 다른 thread 처리
        // → user별 처리 순서가 격리되어 보장됨
    }
}
