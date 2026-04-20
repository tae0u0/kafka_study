package com.kafka.stage07.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 재시도 Consumer
 *
 * [처리 흐름]
 * 메시지 "FAIL"이 포함된 경우:
 *   1차 시도: 예외 발생
 *   1초 후 2차 시도: 예외 발생
 *   1초 후 3차 시도: 예외 발생
 *   → DeadLetterPublishingRecoverer가 stage07-topic.DLT 로 전송
 *   → DltConsumer가 수신
 *
 * 정상 메시지:
 *   1차 시도: 성공
 *   → offset 커밋 완료
 */
@Slf4j
@Service
public class RetryConsumer {

    // 재시도 횟수 추적 (로그 확인용)
    private final AtomicInteger attemptCount = new AtomicInteger(0);

    @KafkaListener(
        topics = "stage07-topic",
        groupId = "stage07-group",
        containerFactory = "retryContainerFactory"  // KafkaErrorConfig에서 정의
    )
    public void consume(ConsumerRecord<String, String> record) {
        int attempt = attemptCount.incrementAndGet();

        log.info("[Retry Consumer] 시도 #{} - offset: {}, value: {}",
            attempt, record.offset(), record.value());

        if (record.value().contains("FAIL")) {
            // 예외를 던지면 DefaultErrorHandler가 재시도를 처리한다
            throw new RuntimeException("처리 실패 시뮬레이션 (시도 #" + attempt + "): " + record.value());
        }

        if (record.value().contains("INVALID")) {
            // NotRetryableException으로 지정된 예외 → 즉시 DLT로 이동
            throw new IllegalArgumentException("잘못된 데이터 형식 (즉시 DLT): " + record.value());
        }

        log.info("[Retry Consumer] 처리 성공 - value: {}", record.value());
    }
}
