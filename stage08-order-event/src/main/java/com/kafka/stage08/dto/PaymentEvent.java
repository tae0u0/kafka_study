package com.kafka.stage08.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 결제 이벤트 DTO
 *
 * [이벤트 체인]
 * OrderEvent(order-events) → [OrderConsumer] → PaymentEvent(payment-events)
 *
 * 이벤트에 원본 orderId를 포함시켜서 추적 가능하게 한다.
 */
@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {

    private String paymentId;
    private String orderId;       // 원본 주문 ID (추적용)
    private String userId;
    private long amount;
    private String paymentMethod; // CARD, BANK_TRANSFER, POINT
    private String status;        // PAYMENT_REQUESTED, PAYMENT_COMPLETED, PAYMENT_FAILED
    private LocalDateTime requestedAt;
}
