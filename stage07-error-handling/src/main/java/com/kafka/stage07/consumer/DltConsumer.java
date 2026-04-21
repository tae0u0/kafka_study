package com.kafka.stage07.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Dead Letter Topic Consumer
 *
 * [DLT 메시지 구조]
 * 원본 메시지 + 추가 헤더:
 *   kafka_dlt-original-topic   : 원본 토픽 이름
 *   kafka_dlt-original-partition: 원본 파티션
 *   kafka_dlt-original-offset  : 원본 offset
 *   kafka_dlt-exception-fqcn   : 발생한 예외 클래스명
 *   kafka_dlt-exception-message: 예외 메시지
 *   kafka_dlt-original-timestamp: 원본 타임스탬프
 *
 * [DLT 처리 전략]
 * 1. 알림: 개발팀/운영팀에 Slack/이메일 알림
 * 2. 저장: DB에 실패 메시지 저장 (나중에 수동 재처리)
 * 3. 분석: 왜 실패했는지 분석 후 버그 수정
 * 4. 재처리: 수정된 로직으로 DLT 메시지 재처리
 */
@Slf4j
@Service
public class DltConsumer {

    @KafkaListener(
        topics = "stage07-topic-dlt",   // DLT 토픽 이름: {원본}-dlt (spring-kafka 4.x 기본값)
        groupId = "stage07-dlt-group"
    )
    public void consumeDlt(ConsumerRecord<String, String> record) {
        log.error("========================================");
        log.error("[DLT Consumer] ⚠️ Dead Letter 메시지 수신");
        log.error("  원본 토픽  : {}",
            getHeaderValue(record, "kafka_dlt-original-topic"));
        log.error("  원본 파티션: {}",
            getHeaderValue(record, "kafka_dlt-original-partition"));
        log.error("  원본 Offset: {}",
            getHeaderValue(record, "kafka_dlt-original-offset"));
        log.error("  예외 클래스: {}",
            getHeaderValue(record, "kafka_dlt-exception-fqcn"));
        log.error("  예외 메시지: {}",
            getHeaderValue(record, "kafka_dlt-exception-message"));
        log.error("  메시지 내용: {}", record.value());
        log.error("========================================");

        // 실무 처리:
        // 1. DB에 실패 메시지 저장
        // 2. 운영팀에 알림
        // 3. 수동 재처리 대기열에 추가
        saveDltMessage(record);
    }

    private void saveDltMessage(ConsumerRecord<String, String> record) {
        // DB 저장 로직 (실무에서 구현)
        log.info("[DLT Consumer] 실패 메시지 저장 완료: {}", record.value());
    }

    private String getHeaderValue(ConsumerRecord<String, String> record, String headerKey) {
        Header header = record.headers().lastHeader(headerKey);
        if (header != null) {
            return new String(header.value());
        }
        return "N/A";
    }
}
