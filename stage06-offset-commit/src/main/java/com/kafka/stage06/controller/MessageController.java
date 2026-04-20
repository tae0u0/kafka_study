package com.kafka.stage06.controller;

import com.kafka.stage06.producer.MessageProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * [테스트 방법]
 *
 * 정상 메시지 전송:
 *   curl -X POST "http://localhost:8085/messages?message=normal-message"
 *
 * 실패 유도 메시지 (FAIL 포함):
 *   curl -X POST "http://localhost:8085/messages?message=FAIL-this-message"
 *   → Manual Commit Consumer에서 acknowledge()가 호출되지 않음
 *   → Consumer 재시작 시 다시 처리됨
 *
 * 여러 메시지:
 *   curl -X POST "http://localhost:8085/messages/bulk?count=5"
 */
@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageProducer producer;

    @PostMapping
    public ResponseEntity<Map<String, String>> send(@RequestParam String message) {
        producer.send(message);
        return ResponseEntity.ok(Map.of("sent", message));
    }

    @PostMapping("/bulk")
    public ResponseEntity<Map<String, Object>> sendBulk(
            @RequestParam(defaultValue = "5") int count) {
        for (int i = 1; i <= count; i++) {
            producer.send("Message-" + i);
        }
        return ResponseEntity.ok(Map.of("sent", count));
    }
}
