package com.kafka.stage03.consumer;

import com.kafka.stage03.dto.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * JSON 객체 메시지 Consumer
 *
 * [@KafkaListener의 containerFactory 속성]
 * KafkaConfig에서 정의한 ConcurrentKafkaListenerContainerFactory Bean 이름을 지정한다.
 * 지정하지 않으면 기본 Factory를 사용하는데, JSON 타입이 맞지 않아 역직렬화 오류가 발생할 수 있다.
 *
 * [역직렬화 흐름]
 * 1. Kafka 브로커에서 byte[] 수신
 * 2. JsonDeserializer가 byte[] → JSON 문자열로 디코딩
 * 3. Jackson ObjectMapper가 JSON → OrderEvent 객체로 변환
 *    (이 때 @NoArgsConstructor가 있어야 함)
 * 4. 메서드 파라미터로 주입
 */
@Slf4j
@Service
public class OrderEventConsumer {

    /**
     * KafkaConfig에서 정의한 Factory를 사용하여 OrderEvent 타입으로 수신
     */
    @KafkaListener(
        topics = "stage03-order-events",
        groupId = "stage03-group",
        containerFactory = "orderEventListenerContainerFactory"  // KafkaConfig의 Bean 이름
    )
    public void consumeOrderEvent(OrderEvent event) {
        log.info("========================================");
        log.info("[Consumer] 주문 이벤트 수신");
        log.info("  orderId   : {}", event.getOrderId());
        log.info("  productId : {}", event.getProductId());
        log.info("  userId    : {}", event.getUserId());
        log.info("  quantity  : {}", event.getQuantity());
        log.info("  totalPrice: {}원", event.getTotalPrice());
        log.info("  status    : {}", event.getStatus());
        log.info("  createdAt : {}", event.getCreatedAt());
        log.info("========================================");

        // 비즈니스 로직 처리
        processOrderEvent(event);
    }

    /**
     * ConsumerRecord로 수신 - 메타데이터 + 객체 동시 접근 (다른 그룹으로 설정)
     */
    @KafkaListener(
        topics = "stage03-order-events",
        groupId = "stage03-group-audit",  // 감사(audit) 목적의 별도 그룹
        containerFactory = "orderEventListenerContainerFactory"
    )
    public void auditOrderEvent(ConsumerRecord<String, OrderEvent> record) {
        log.info("[Audit Consumer] 감사 로그 기록");
        log.info("  partition: {}, offset: {}, key: {}",
            record.partition(), record.offset(), record.key());
        log.info("  event: {}", record.value());

        // 실무에서는 여기서 감사 DB에 기록하거나 모니터링 시스템에 전송
    }

    private void processOrderEvent(OrderEvent event) {
        log.debug("[Consumer] 주문 이벤트 처리 완료 - orderId: {}", event.getOrderId());
        // 실제 처리: 재고 확인, 결제 요청, DB 저장 등
    }
}
