package com.kafka.stage04.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Group A Consumer - 3개의 파티션을 3개의 스레드로 처리
 *
 * [concurrency = 3 의미]
 * 하나의 @KafkaListener가 3개의 스레드를 생성한다.
 * 파티션이 3개이면, 각 스레드가 하나의 파티션을 담당한다.
 * → 동시에 3개의 파티션에서 병렬로 메시지를 처리!
 *
 * [주의: concurrency > 파티션 수이면?]
 * 초과된 스레드는 파티션을 할당받지 못하고 유휴 상태가 된다.
 * 실무에서는 concurrency ≤ 파티션 수로 설정한다.
 */
@Slf4j
@Service
public class GroupAConsumer {

    @KafkaListener(
        topics = "stage04-topic",
        groupId = "group-A",        // Group A
        concurrency = "3"           // 3개 스레드 → 3개 파티션 병렬 처리
    )
    public void consume(ConsumerRecord<String, String> record) {
        log.info("[Group-A] 수신 → partition: {}, offset: {}, key: {}, value: {}",
            record.partition(),
            record.offset(),
            record.key(),
            record.value());

        // Group A의 처리 로직 (예: 결제 처리)
    }
}
