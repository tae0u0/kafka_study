package com.kafka.stage06.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Auto Commit Consumer
 *
 * [동작 방식]
 * 1. 메시지 poll (가져오기)
 * 2. 리스너 메서드 실행 (비즈니스 로직)
 * 3. [5초 후 자동] Kafka 클라이언트가 offset 커밋
 *
 * [문제 시나리오: 메시지 유실]
 * Time 0s: offset 5 메시지 poll
 * Time 1s: 처리 중 (DB 저장 시도)
 * Time 3s: [자동 커밋] offset 6으로 커밋 완료 ← 아직 처리 중인데!
 * Time 4s: 처리 실패, Consumer 재시작
 * Time 4s: offset 6부터 읽기 시작 → offset 5 메시지 영원히 유실!
 *
 * [실무 권장]
 * - 메시지 유실이 절대 안 되는 경우: Manual Commit 사용
 * - 처리 중복이 허용되고 단순한 경우: Auto Commit도 무방
 */
@Slf4j
@Service
public class AutoCommitConsumer {

    @KafkaListener(
        topics = "stage06-topic",
        groupId = "auto-commit-group"
        // enable.auto.commit=true (application.yml 설정 사용)
        // Spring @KafkaListener 기본 AckMode: BATCH
    )
    public void consume(ConsumerRecord<String, String> record) {
        log.info("[Auto Commit] 메시지 수신 - offset: {}, value: {}",
            record.offset(), record.value());

        try {
            // 비즈니스 로직 처리
            processMessage(record.value());

            log.info("[Auto Commit] 처리 완료 - offset: {} (Kafka가 나중에 자동 커밋)", record.offset());
            // ← 이 시점에서 offset이 커밋되지 않는다!
            //   5초(auto.commit.interval) 후 Kafka 클라이언트가 자동으로 커밋한다.

        } catch (Exception e) {
            // 예외가 발생해도 커밋 타이밍은 Kafka 클라이언트가 결정
            // → 자동 커밋이 이미 되었다면 이 메시지는 재처리되지 않는다 (유실!)
            log.error("[Auto Commit] 처리 실패 - offset: {}, error: {}", record.offset(), e.getMessage());
        }
    }

    private void processMessage(String message) {
        log.debug("[Auto Commit] 처리 중: {}", message);
        // 실제 처리 로직
    }
}
