package com.kafka.stage08.consumer;

import com.kafka.stage08.config.KafkaConfig;
import com.kafka.stage08.dto.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

/**
 * 재고 차감 Consumer
 *
 * [1단계와의 차이]
 * 1단계: OrderService.processOrder()에서 직접 inventoryService.decreaseStock() 호출 (동기)
 * 8단계: 이벤트를 수신하여 독립적으로 재고 차감 처리 (비동기)
 *
 * [장점]
 * - OrderService가 이 클래스를 전혀 모른다 → 완전한 결합도 제거
 * - 재고 서비스 장애 → 주문 수신에는 영향 없음
 * - 재고 처리량이 많으면 concurrency만 늘리면 됨
 */
@Slf4j
@Service
public class InventoryConsumer {

    @KafkaListener(
        topics = KafkaConfig.ORDER_TOPIC,
        groupId = "inventory-service-group",
        containerFactory = "inventoryListenerFactory"
    )
    public void processInventory(ConsumerRecord<String, OrderEvent> record,
                                  Acknowledgment acknowledgment) {
        OrderEvent event = record.value();
        log.info("[재고서비스] 재고 차감 처리 시작 - orderId: {}, productId: {}, quantity: {}",
            event.getOrderId(), event.getProductId(), event.getQuantity());

        try {
            // 실제 재고 차감 로직 (DB 업데이트 시뮬레이션)
            decreaseStock(event.getProductId(), event.getQuantity());

            acknowledgment.acknowledge();
            log.info("[재고서비스] 재고 차감 완료 - orderId: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("[재고서비스] 재고 차감 실패 - orderId: {}, error: {}",
                event.getOrderId(), e.getMessage());
            // acknowledge() 미호출 → DefaultErrorHandler가 재시도 처리
            throw e;
        }
    }

    private void decreaseStock(String productId, int quantity) {
        log.debug("[재고서비스] {} 재고 {}개 차감", productId, quantity);
        // 실제: UPDATE inventory SET stock = stock - ? WHERE product_id = ?
        // 멱등성: 같은 orderId로 두 번 처리 시 중복 차감 방지 로직 필요
    }
}
