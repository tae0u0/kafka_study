package com.kafka.stage08.producer;

import com.kafka.stage08.config.KafkaConfig;
import com.kafka.stage08.dto.DeliveryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryEventProducer {

    private final KafkaTemplate<String, DeliveryEvent> deliveryKafkaTemplate;

    public void publish(DeliveryEvent event) {
        log.info("[DeliveryProducer] 배송 이벤트 발행 → topic: {}, deliveryId: {}",
            KafkaConfig.DELIVERY_TOPIC, event.getDeliveryId());

        deliveryKafkaTemplate.send(KafkaConfig.DELIVERY_TOPIC, event.getOrderId(), event)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("[DeliveryProducer] 발행 완료 - partition: {}, offset: {}",
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                } else {
                    log.error("[DeliveryProducer] 발행 실패 - deliveryId: {}", event.getDeliveryId(), ex);
                }
            });
    }
}
