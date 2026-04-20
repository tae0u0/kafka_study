package com.kafka.stage08.producer;

import com.kafka.stage08.config.KafkaConfig;
import com.kafka.stage08.dto.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventProducer {

    // 같은 KafkaTemplate 타입이 여러 개일 때는 필드명으로 Bean을 매칭한다.
    // 필드명 "orderKafkaTemplate" == KafkaConfig의 @Bean 메서드명
    private final KafkaTemplate<String, OrderEvent> orderKafkaTemplate;

    public void publish(OrderEvent event) {
        log.info("[OrderProducer] 주문 이벤트 발행 → topic: {}, orderId: {}",
            KafkaConfig.ORDER_TOPIC, event.getOrderId());

        orderKafkaTemplate.send(KafkaConfig.ORDER_TOPIC, event.getOrderId(), event)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("[OrderProducer] 발행 완료 - partition: {}, offset: {}",
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                } else {
                    log.error("[OrderProducer] 발행 실패 - orderId: {}", event.getOrderId(), ex);
                }
            });
    }
}
