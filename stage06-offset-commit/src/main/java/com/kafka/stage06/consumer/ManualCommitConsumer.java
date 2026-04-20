package com.kafka.stage06.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

/**
 * Manual Commit Consumer
 *
 * [설정 변경이 필요하다]
 * Manual Commit을 사용하려면 KafkaListenerContainerFactory의 AckMode를 변경해야 한다.
 * application.yml만으로는 부족하고 Java Config가 필요하다.
 * → ManualCommitConfig.java 참고
 *
 * [동작 방식]
 * 1. 메시지 poll
 * 2. 리스너 메서드 실행
 * 3. 처리 성공 → acknowledgment.acknowledge() 호출 → offset 커밋
 * 4. 처리 실패 → acknowledge() 호출 안 함 → offset 커밋 안 됨
 *    → Consumer 재시작 시 같은 메시지부터 다시 처리
 *
 * [문제 시나리오: 메시지 중복]
 * Time 0: offset 5 처리 완료
 * Time 1: acknowledge() 호출 직전 Consumer 크래시
 * Time 2: Consumer 재시작 → offset 5부터 다시 읽음 → 중복 처리!
 * → 처리 로직이 멱등성(idempotency)을 가져야 한다
 *   (같은 메시지를 두 번 처리해도 결과가 동일해야 함)
 *   예: DB upsert, 중복 체크 등
 */
@Slf4j
@Service
public class ManualCommitConsumer {

    /**
     * Acknowledgment 파라미터를 선언하면 Spring이 자동으로 주입한다.
     * containerFactory에서 AckMode.MANUAL 또는 MANUAL_IMMEDIATE를 설정해야 한다.
     *
     * AckMode.MANUAL: 배치 처리 후 커밋 (배치 내 모든 처리 완료 후)
     * AckMode.MANUAL_IMMEDIATE: acknowledge() 호출 즉시 커밋 (메시지 단위)
     */
    @KafkaListener(
        topics = "stage06-topic",
        groupId = "manual-commit-group",
        containerFactory = "manualAckContainerFactory"  // ManualCommitConfig에서 정의
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        log.info("[Manual Commit] 메시지 수신 - offset: {}, value: {}",
            record.offset(), record.value());

        try {
            // 비즈니스 로직 처리
            processMessage(record.value());

            // 처리 성공 후 명시적으로 커밋
            // 이 호출 전까지는 offset이 커밋되지 않는다
            acknowledgment.acknowledge();
            log.info("[Manual Commit] offset {} 커밋 완료 ← 명시적 커밋!", record.offset());

        } catch (Exception e) {
            // 실패 시 acknowledge() 를 호출하지 않음
            // → offset이 커밋되지 않음 → 재시작 시 같은 메시지 재처리
            log.error("[Manual Commit] 처리 실패 - offset: {} (재처리 예정)", record.offset());
            // 주의: 무한 재처리 루프를 막기 위해 7단계(에러 핸들러)가 필요하다
        }
    }

    private void processMessage(String message) throws Exception {
        if (message.contains("FAIL")) {
            throw new Exception("처리 실패 시뮬레이션: " + message);
        }
        log.debug("[Manual Commit] 처리 중: {}", message);
    }
}
