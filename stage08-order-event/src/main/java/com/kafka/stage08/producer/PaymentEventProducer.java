package com.kafka.stage08.producer;

import com.kafka.stage08.config.KafkaConfig;
import com.kafka.stage08.dto.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, PaymentEvent> paymentKafkaTemplate;

    public void publish(PaymentEvent event) {
        log.info("[PaymentProducer] 결제 이벤트 발행 → topic: {}, paymentId: {}",
            KafkaConfig.PAYMENT_TOPIC, event.getPaymentId());

        paymentKafkaTemplate.send(KafkaConfig.PAYMENT_TOPIC, event.getOrderId(), event)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("[PaymentProducer] 발행 완료 - partition: {}, offset: {}",
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                } else {
                    log.error("[PaymentProducer] 발행 실패 - paymentId: {}", event.getPaymentId(), ex);
                }
            });
    }
}
