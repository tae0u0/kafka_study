package com.kafka.stage04.controller;

import com.kafka.stage04.producer.MessageProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * [테스트 방법]
 *
 * 1. Kafka UI에서 stage04-topic을 파티션 3개로 먼저 생성:
 *    http://localhost:8989 → Topics → Add a Topic
 *    Name: stage04-topic, Partitions: 3
 *
 * 2. 메시지 발행:
 *    curl -X POST "http://localhost:8083/messages/bulk?count=9"
 *
 * 3. 로그 확인:
 *    [Group-A] partition:0, partition:1, partition:2 가 각각 다른 스레드에서 처리됨
 *    [Group-B] 동일한 메시지를 처음부터 전부 다시 처리
 *
 * 관찰 포인트:
 *   - Group A와 Group B 모두 9개 메시지를 수신함 (독립적 소비)
 *   - Group A 내에서는 partition 0,1,2 를 각각 다른 스레드가 처리
 *   - 로그의 thread 이름에 "-0", "-1", "-2" 가 붙음 (각 파티션 담당 스레드)
 */
@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageProducer producer;

    @PostMapping("/bulk")
    public ResponseEntity<Map<String, Object>> sendBulk(
            @RequestParam(defaultValue = "9") int count) {
        for (int i = 1; i <= count; i++) {
            // Key를 다양하게 설정하여 파티션 분산 확인
            String key = "key-" + (i % 3);  // key-0, key-1, key-2 반복
            producer.send(key, "Message #" + i + " (key=" + key + ")");
        }
        return ResponseEntity.ok(Map.of("sent", count, "topic", MessageProducer.TOPIC));
    }
}
