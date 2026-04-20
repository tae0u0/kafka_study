package com.kafka.stage01.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 재고 차감 서비스 (실제로는 별도 마이크로서비스라고 가정)
 *
 * [동기 호출 문제 포인트]
 * - OrderService가 이 클래스를 직접 주입받아 호출한다 → 강한 결합
 * - 이 메서드가 200ms 걸리면 주문 응답도 그만큼 늦어진다
 * - 실무에서는 HTTP 클라이언트(RestTemplate/WebClient)로 외부 서비스를 호출하는 구조
 */
@Slf4j
@Service
public class InventoryService {

    public void decreaseStock(String productId, int quantity) {
        log.info("[재고서비스] 재고 차감 시작 - 상품: {}, 수량: {}", productId, quantity);

        // 실제 재고 DB 업데이트를 시뮬레이션 (200ms 지연)
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 특정 상품은 재고 부족으로 실패 시뮬레이션
        if ("OUT_OF_STOCK".equals(productId)) {
            throw new RuntimeException("재고 부족: " + productId);
        }

        log.info("[재고서비스] 재고 차감 완료 - 상품: {}, 수량: {}", productId, quantity);
    }
}
