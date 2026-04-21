package com.kafka.stage08.consumer;

import com.kafka.stage08.config.KafkaConfig;
import com.kafka.stage08.dto.DeliveryEvent;
import com.kafka.stage08.dto.PaymentEvent;
import com.kafka.stage08.producer.DeliveryEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 결제 이벤트 Consumer
 *
 * [역할]
 * payment-events 토픽에서 결제 요청을 수신하고
 * 결제를 처리한 뒤 배송 이벤트를 delivery-events 토픽으로 발행한다.
 *
 * [이벤트 체인]
 * payment-events → [PaymentEventConsumer] → delivery-events
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final DeliveryEventProducer deliveryEventProducer;

    @KafkaListener(
        topics = KafkaConfig.PAYMENT_TOPIC,
        groupId = "payment-consumer-group",
        containerFactory = "paymentListenerFactory"
    )
    public void consume(ConsumerRecord<String, PaymentEvent> record, Acknowledgment ack) {
        PaymentEvent payment = record.value();

        log.info("[결제 Consumer] 결제 요청 수신 - paymentId: {}, orderId: {}, amount: {}원",
            payment.getPaymentId(), payment.getOrderId(), payment.getAmount());

        try {
            // 결제 처리 (PG사 연동 시뮬레이션)
            processPayment(payment);

            // 다음 단계: 배송 이벤트 발행
            DeliveryEvent deliveryEvent = DeliveryEvent.builder()
                .deliveryId("DEL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .orderId(payment.getOrderId())
                .paymentId(payment.getPaymentId())
                .userId(payment.getUserId())
                .shippingAddress(payment.getShippingAddress())
                .status("DELIVERY_REQUESTED")
                .requestedAt(LocalDateTime.now())
                .build();

            deliveryEventProducer.publish(deliveryEvent);
            log.info("[결제 Consumer] 배송 이벤트 발행 완료 - orderId: {}", payment.getOrderId());

            ack.acknowledge();

        } catch (Exception e) {
            log.error("[결제 Consumer] 처리 실패 - paymentId: {}", payment.getPaymentId(), e);
            throw e;
        }
    }

    private void processPayment(PaymentEvent payment) {
        log.debug("[결제 Consumer] 결제 처리 중 - paymentId: {}, amount: {}원",
            payment.getPaymentId(), payment.getAmount());
        // 실제: PG사 API 호출
    }
}
