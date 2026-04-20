package com.kafka.stage05.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    public static final String TOPIC = "stage05-orders";

    /**
     * Key를 포함하여 전송 - 같은 Key는 항상 같은 파티션으로 라우팅
     *
     * [Kafka 파티션 결정 알고리즘]
     * key가 있는 경우: murmur2(key) % numPartitions
     * key가 null인 경우: Sticky Partitioner (배치 단위로 같은 파티션 선택)
     */
    public void sendOrderWithKey(String userId, String orderInfo) {
        // key = userId: 같은 유저의 주문은 항상 같은 파티션 → 순서 보장
        CompletableFuture<SendResult<String, String>> future =
            kafkaTemplate.send(TOPIC, userId, orderInfo);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                int partition = result.getRecordMetadata().partition();
                long offset = result.getRecordMetadata().offset();
                // 같은 userId는 항상 같은 partition 번호가 출력된다
                log.info("[Producer] KEY={} → Partition={}, Offset={}, msg={}",
                    userId, partition, offset, orderInfo);
            }
        });
    }

    /**
     * Key 없이 전송 - 파티션이 무작위로 결정됨
     * → 같은 사용자의 메시지가 여러 파티션에 흩어질 수 있어 순서 보장 불가
     */
    public void sendOrderWithoutKey(String orderInfo) {
        CompletableFuture<SendResult<String, String>> future =
            kafkaTemplate.send(TOPIC, orderInfo);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("[Producer] NO KEY → Partition={}, msg={}",
                    result.getRecordMetadata().partition(), orderInfo);
            }
        });
    }
}
