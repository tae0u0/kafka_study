package com.kafka.stage01.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderResponse {
    private String orderId;
    private String status;
    private long processingTimeMs;
    private String message;
}
