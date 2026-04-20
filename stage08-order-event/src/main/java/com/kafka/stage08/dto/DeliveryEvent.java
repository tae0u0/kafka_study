package com.kafka.stage08.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 배송 이벤트 DTO
 *
 * [이벤트 체인]
 * PaymentEvent(payment-events) → [PaymentConsumer] → DeliveryEvent(delivery-events)
 */
@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryEvent {

    private String deliveryId;
    private String orderId;         // 원본 주문 ID (추적용)
    private String paymentId;       // 원본 결제 ID (추적용)
    private String userId;
    private String shippingAddress;
    private String status;          // DELIVERY_REQUESTED, IN_TRANSIT, DELIVERED
    private LocalDateTime requestedAt;
}
