package com.kafka.stage08.consumer;

import com.kafka.stage08.config.KafkaConfig;
import com.kafka.stage08.dto.OrderEvent;
import com.kafka.stage08.dto.PaymentEvent;
import com.kafka.stage08.producer.PaymentEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 주문 이벤트 Consumer
 *
 * [역할]
 * order-events 토픽에서 주문을 수신하고
 * 결제 요청을 payment-events 토픽으로 발행한다.
 *
 * [이벤트 체인]
 * order-events → [OrderEventConsumer] → payment-events
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final PaymentEventProducer paymentEventProducer;

    @KafkaListener(
        topics = KafkaConfig.ORDER_TOPIC,
        groupId = "order-consumer-group",
        containerFactory = "orderListenerFactory"
    )
    public void consume(ConsumerRecord<String, OrderEvent> record, Acknowledgment ack) {
        OrderEvent order = record.value();

        log.info("[주문 Consumer] 주문 수신 - orderId: {}, userId: {}, totalPrice: {}원",
            order.getOrderId(), order.getUserId(), order.getTotalPrice());

        try {
            // 주문 저장 처리 (DB insert 시뮬레이션)
            saveOrder(order);

            // 다음 단계: 결제 이벤트 발행
            PaymentEvent paymentEvent = PaymentEvent.builder()
                .paymentId("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .amount(order.getTotalPrice())
                .paymentMethod("CARD")
                .status("PAYMENT_REQUESTED")
                .requestedAt(LocalDateTime.now())
                .build();

            paymentEventProducer.publish(paymentEvent);
            log.info("[주문 Consumer] 결제 이벤트 발행 완료 - orderId: {}", order.getOrderId());

            ack.acknowledge();

        } catch (Exception e) {
            log.error("[주문 Consumer] 처리 실패 - orderId: {}", order.getOrderId(), e);
            throw e;
        }
    }

    private void saveOrder(OrderEvent order) {
        log.debug("[주문 Consumer] 주문 저장 - orderId: {}", order.getOrderId());
        // 실제: orderRepository.save(...)
    }
}
