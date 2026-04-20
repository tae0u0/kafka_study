package com.kafka.stage03.producer;

import com.kafka.stage03.dto.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * JSON 객체 메시지 Producer
 *
 * [String Producer와의 차이]
 * KafkaTemplate<String, String> → KafkaTemplate<String, OrderEvent>
 * String 직렬화기 → JsonSerializer
 *
 * [내부 동작 흐름]
 * 1. send(topic, orderEvent) 호출
 * 2. JsonSerializer가 OrderEvent를 JSON 문자열로 변환
 *    예: {"orderId":"ORD-001","productId":"PROD-A","userId":"user1",...}
 * 3. JSON 문자열을 byte[]로 인코딩
 * 4. Kafka 브로커로 전송
 *
 * Kafka UI에서 메시지 조회 시 JSON 형태로 보임
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventProducer {

    // KafkaConfig에서 정의한 KafkaTemplate Bean을 주입받는다
    // Spring이 타입으로 Bean을 찾아 주입하므로 정확한 제네릭 타입이 중요하다
    private final KafkaTemplate<String, OrderEvent> orderEventKafkaTemplate;

    public static final String TOPIC = "stage03-order-events";

    public void publishOrderEvent(OrderEvent event) {
        log.info("[Producer] 주문 이벤트 발행 - orderId: {}, productId: {}",
            event.getOrderId(), event.getProductId());

        // Key를 orderId로 설정: 같은 주문의 이벤트는 같은 파티션으로 간다 (순서 보장)
        CompletableFuture<SendResult<String, OrderEvent>> future =
            orderEventKafkaTemplate.send(TOPIC, event.getOrderId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("[Producer] 이벤트 발행 성공 - orderId: {}, partition: {}, offset: {}",
                    event.getOrderId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            } else {
                log.error("[Producer] 이벤트 발행 실패 - orderId: {}, error: {}",
                    event.getOrderId(), ex.getMessage());
                // 실무에서는 여기서 실패 이벤트를 별도 저장하거나 알림을 보낸다
            }
        });
    }
}
