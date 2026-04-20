package com.kafka.stage02.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka 메시지 Producer
 *
 * [KafkaTemplate이란?]
 * Spring이 제공하는 Kafka 전송 헬퍼 클래스.
 * application.yml의 producer 설정을 읽어 자동으로 빈(Bean)이 생성된다.
 * 직접 KafkaProducer를 생성하고 관리할 필요 없이 간단히 send()만 호출하면 된다.
 *
 * [제네릭 타입 <String, String>]
 * 첫 번째 String: 메시지 Key 타입
 * 두 번째 String: 메시지 Value 타입
 * → application.yml의 key-serializer, value-serializer와 일치해야 한다
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    // 토픽 이름 상수 (Consumer와 반드시 동일해야 한다)
    public static final String TOPIC = "stage02-topic";

    /**
     * 기본 메시지 전송 (Key 없음)
     *
     * Key가 없으면 Kafka는 Round-Robin 방식으로 파티션을 선택한다.
     * → 메시지 순서가 보장되지 않을 수 있다 (5단계에서 Key로 해결)
     */
    public void sendMessage(String message) {
        log.info("[Producer] 메시지 전송 시도 - topic: {}, message: {}", TOPIC, message);

        // send()는 CompletableFuture를 반환한다 (비동기 전송)
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(TOPIC, message);

        // 전송 결과 콜백: 성공/실패 여부를 비동기로 처리
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                // 전송 성공: 어느 파티션의 몇 번째 offset에 저장됐는지 확인 가능
                log.info("[Producer] 전송 성공 - topic: {}, partition: {}, offset: {}, message: {}",
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),  // 파티션 번호
                    result.getRecordMetadata().offset(),     // 해당 파티션 내 위치 (offset)
                    message);
            } else {
                // 전송 실패: 네트워크 오류, 브로커 다운 등
                log.error("[Producer] 전송 실패 - message: {}, error: {}", message, ex.getMessage());
            }
        });
    }

    /**
     * Key와 함께 메시지 전송
     *
     * 같은 Key를 가진 메시지는 항상 같은 파티션으로 간다.
     * → 특정 Key에 대한 메시지 순서가 보장된다 (5단계에서 자세히)
     */
    public void sendMessageWithKey(String key, String message) {
        log.info("[Producer] Key 포함 전송 - topic: {}, key: {}, message: {}", TOPIC, key, message);

        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(TOPIC, key, message);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("[Producer] Key 전송 성공 - key: {}, partition: {}, offset: {}",
                    key,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            } else {
                log.error("[Producer] Key 전송 실패 - key: {}, error: {}", key, ex.getMessage());
            }
        });
    }
}
