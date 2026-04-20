package com.kafka.stage02.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka 메시지 Consumer
 *
 * [@KafkaListener란?]
 * 지정한 토픽을 구독하고, 메시지가 도착하면 해당 메서드를 자동으로 호출한다.
 * 내부적으로 별도 스레드에서 폴링(polling) 방식으로 동작한다.
 *
 * [폴링(Polling) 방식]
 * Consumer는 Kafka 브로커에 주기적으로 "새 메시지 있어?" 라고 물어보는 방식.
 * Push 방식(브로커가 Consumer에게 밀어주기)이 아니다.
 * → Consumer가 자신의 속도로 메시지를 가져갈 수 있어 부하 조절이 쉽다.
 *
 * [groupId]
 * application.yml의 group-id와 동일하게 설정하거나, 여기서 직접 지정할 수 있다.
 * 여기서 지정하면 application.yml 설정을 덮어쓴다.
 */
@Slf4j
@Service
public class MessageConsumer {

    /**
     * 단순 String 메시지 수신
     *
     * @param message: 역직렬화된 메시지 Value (byte[] → String)
     */
    @KafkaListener(
        topics = "stage02-topic",       // 구독할 토픽 이름 (Producer와 동일해야 함)
        groupId = "stage02-group"        // Consumer Group ID
    )
    public void consume(String message) {
        log.info("[Consumer] 메시지 수신 - message: {}", message);

        // 실제 처리 로직을 여기에 작성한다
        // 예: DB 저장, 외부 API 호출, 이메일 전송 등
        processMessage(message);
    }

    /**
     * ConsumerRecord로 수신 - 메타데이터(파티션, offset, 키 등)도 함께 확인
     *
     * 학습 포인트: 실제 운영에서는 ConsumerRecord를 받아서
     * offset 정보를 로그에 남기는 것이 디버깅에 매우 유용하다.
     */
    @KafkaListener(
        topics = "stage02-topic",
        groupId = "stage02-group-detail" // 다른 그룹 ID → 같은 메시지를 독립적으로 소비
    )
    public void consumeWithMetadata(ConsumerRecord<String, String> record) {
        log.info("[Consumer-Detail] 메시지 수신 상세정보");
        log.info("  - Topic    : {}", record.topic());
        log.info("  - Partition: {}", record.partition());  // 어느 파티션에서 왔는지
        log.info("  - Offset   : {}", record.offset());    // 해당 파티션의 몇 번째 메시지인지
        log.info("  - Key      : {}", record.key());       // 메시지 Key (없으면 null)
        log.info("  - Value    : {}", record.value());     // 실제 메시지 내용
        log.info("  - Timestamp: {}", record.timestamp()); // 메시지 생성 시각
    }

    private void processMessage(String message) {
        // 실제 비즈니스 로직 처리
        log.debug("[Consumer] 처리 완료 - message: {}", message);
    }
}
