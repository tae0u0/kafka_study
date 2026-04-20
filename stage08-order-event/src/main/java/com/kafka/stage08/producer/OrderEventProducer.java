package com.kafka.stage08.producer;

import com.kafka.stage08.config.KafkaConfig;
import com.kafka.stage08.dto.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * 주문 이벤트 Producer
 *
 * [핵심 원칙]
 * OrderService는 이벤트를 발행하고 즉시 리턴한다.
 * 재고 차감, 알림 전송은 각 Consumer가 비동기로 처리한다.
 * → OrderService는 Consumer들의 존재를 알 필요가 없다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, OrderEvent> orderKafkaTemplate;

    public void publish(OrderEvent event) {
        log.info("[OrderProducer] 주문 이벤트 발행 시작 - orderId: {}", event.getOrderId());

        long start = System.currentTimeMillis();

        // key=orderId: 같은 주문의 상태 변경 이벤트가 항상 같은 파티션으로 → 순서 보장
        orderKafkaTemplate.send(KafkaConfig.ORDER_TOPIC, event.getOrderId(), event)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    long elapsed = System.currentTimeMillis() - start;
                    log.info("[OrderProducer] 발행 완료 - orderId: {}, partition: {}, offset: {}, {}ms",
                        event.getOrderId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        elapsed);
                } else {
                    log.error("[OrderProducer] 발행 실패 - orderId: {}", event.getOrderId(), ex);
                }
            });
    }
}
