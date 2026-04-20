package com.kafka.stage08.consumer;

import com.kafka.stage08.config.KafkaConfig;
import com.kafka.stage08.dto.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

/**
 * 알림 전송 Consumer
 *
 * [1단계와의 차이]
 * 1단계: 알림 서버 다운 → 주문 처리 전체 실패
 * 8단계: 알림 서버 다운 → 이 Consumer만 재시도/DLT → 주문은 이미 완료
 *
 * [다른 그룹(notification-service-group) 사용]
 * inventory-service-group과 완전히 독립적으로 같은 이벤트를 소비한다.
 * → 재고 차감이 완료되지 않아도 알림은 독립적으로 처리됨
 * → 재고 차감과 알림이 서로를 기다리지 않음
 */
@Slf4j
@Service
public class NotificationConsumer {

    @KafkaListener(
        topics = KafkaConfig.ORDER_TOPIC,
        groupId = "notification-service-group",
        containerFactory = "notificationListenerFactory"
    )
    public void sendNotification(ConsumerRecord<String, OrderEvent> record,
                                  Acknowledgment acknowledgment) {
        OrderEvent event = record.value();
        log.info("[알림서비스] 주문 확인 알림 전송 시작 - orderId: {}, email: {}",
            event.getOrderId(), event.getUserEmail());

        try {
            // 실제 이메일/SMS 발송 로직
            sendOrderConfirmationEmail(event);

            acknowledgment.acknowledge();
            log.info("[알림서비스] 알림 전송 완료 - orderId: {}, to: {}",
                event.getOrderId(), event.getUserEmail());

        } catch (Exception e) {
            log.error("[알림서비스] 알림 전송 실패 - orderId: {}", event.getOrderId(), e);
            throw e;
        }
    }

    private void sendOrderConfirmationEmail(OrderEvent event) {
        // 실제: EmailService.send(event.getUserEmail(), ...)
        log.debug("[알림서비스] 이메일 발송: {} → {}", event.getOrderId(), event.getUserEmail());
        log.debug("  상품: {} × {}개 = {}원",
            event.getProductName(), event.getQuantity(), event.getTotalPrice());
    }
}
