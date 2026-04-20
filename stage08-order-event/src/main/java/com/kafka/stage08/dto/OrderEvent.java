package com.kafka.stage08.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 주문 생성 이벤트
 *
 * [이벤트 설계 원칙]
 * - 이벤트는 충분한 정보를 담아야 한다: Consumer가 추가 DB 조회 없이 처리 가능해야 함
 * - 이벤트는 불변: 한 번 발행된 이벤트는 수정하지 않는다
 * - 버전 관리: 스키마 변경 시 하위 호환성 고려 (필드 추가 O, 삭제/타입변경 주의)
 */
@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {
    private String orderId;
    private String productId;
    private String productName;
    private String userId;
    private String userEmail;   // 알림 Consumer가 이메일 조회 없이 직접 전송 가능
    private int quantity;
    private long unitPrice;
    private long totalPrice;
    private String status;
    private LocalDateTime orderedAt;
    private String shippingAddress;
}
