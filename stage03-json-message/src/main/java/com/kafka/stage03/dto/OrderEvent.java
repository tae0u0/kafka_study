package com.kafka.stage03.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 주문 이벤트 DTO
 *
 * [중요: 기본 생성자(@NoArgsConstructor) 필수!]
 * Jackson이 JSON → 객체로 역직렬화할 때 기본 생성자를 사용한다.
 * 기본 생성자가 없으면 JsonDeserializer가 "Cannot construct instance" 오류를 발생시킨다.
 * → 이것이 Kafka JSON 처리 시 가장 흔한 오류 원인 중 하나다.
 *
 * [이벤트 설계 원칙]
 * - 이벤트는 "무슨 일이 일어났다"는 사실을 담는다 (과거형 네이밍: OrderCreated)
 * - 이벤트는 불변(immutable)에 가깝게 설계한다
 * - 이벤트에는 처리에 필요한 충분한 정보를 담는다 (Consumer가 추가 조회 없이 처리 가능하도록)
 */
@Getter
@Builder
@ToString
@NoArgsConstructor  // Jackson 역직렬화에 필수
@AllArgsConstructor // @Builder와 함께 사용 시 필요
public class OrderEvent {

    private String orderId;
    private String productId;
    private String userId;
    private int quantity;
    private long totalPrice;
    private String status;        // ORDER_CREATED, ORDER_CONFIRMED, ORDER_CANCELLED
    private LocalDateTime createdAt;

    // 정적 팩토리 메서드: 객체 생성을 명확하게
    public static OrderEvent of(String orderId, String productId, String userId,
                                 int quantity, long totalPrice) {
        return OrderEvent.builder()
            .orderId(orderId)
            .productId(productId)
            .userId(userId)
            .quantity(quantity)
            .totalPrice(totalPrice)
            .status("ORDER_CREATED")
            .createdAt(LocalDateTime.now())
            .build();
    }
}
