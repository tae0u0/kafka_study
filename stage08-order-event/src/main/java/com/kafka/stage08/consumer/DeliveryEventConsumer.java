package com.kafka.stage08.consumer;

import com.kafka.stage08.config.KafkaConfig;
import com.kafka.stage08.dto.DeliveryEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

/**
 * 배송 이벤트 Consumer
 *
 * [역할]
 * delivery-events 토픽에서 배송 요청을 수신하고
 * 실제 배송 처리(물류 시스템 연동)를 담당한다.
 * 이벤트 체인의 마지막 단계.
 *
 * [이벤트 체인 전체]
 * order-events → payment-events → delivery-events → (끝)
 */
@Slf4j
@Service
public class DeliveryEventConsumer {

    @KafkaListener(
        topics = KafkaConfig.DELIVERY_TOPIC,
        groupId = "delivery-consumer-group",
        containerFactory = "deliveryListenerFactory"
    )
    public void consume(ConsumerRecord<String, DeliveryEvent> record, Acknowledgment ack) {
        DeliveryEvent delivery = record.value();

        log.info("[배송 Consumer] 배송 요청 수신 - deliveryId: {}, orderId: {}, address: {}",
            delivery.getDeliveryId(), delivery.getOrderId(), delivery.getShippingAddress());

        try {
            startDelivery(delivery);

            ack.acknowledge();
            log.info("[배송 Consumer] 배송 처리 완료 - deliveryId: {}", delivery.getDeliveryId());

        } catch (Exception e) {
            log.error("[배송 Consumer] 처리 실패 - deliveryId: {}", delivery.getDeliveryId(), e);
            throw e;
        }
    }

    private void startDelivery(DeliveryEvent delivery) {
        log.debug("[배송 Consumer] 물류 시스템 연동 - deliveryId: {}", delivery.getDeliveryId());
        // 실제: 물류 API 호출, 배송 DB 저장
    }
}
