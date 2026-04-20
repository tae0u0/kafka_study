package com.kafka.stage01.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    private String orderId;
    private String productId;
    private int quantity;
    private String userId;
}
