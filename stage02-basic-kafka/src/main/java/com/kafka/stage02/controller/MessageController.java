package com.kafka.stage02.controller;

import com.kafka.stage02.producer.MessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 메시지 발행 REST API
 *
 * [테스트 방법]
 *
 * 단순 메시지 전송:
 *   curl -X POST "http://localhost:8081/messages?message=Hello+Kafka"
 *
 * Key 포함 전송:
 *   curl -X POST "http://localhost:8081/messages/with-key?key=user1&message=Hello"
 *
 * 여러 메시지 한번에:
 *   curl -X POST "http://localhost:8081/messages/bulk?count=10"
 */
@Slf4j
@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageProducer messageProducer;

    @PostMapping
    public ResponseEntity<Map<String, String>> sendMessage(@RequestParam String message) {
        messageProducer.sendMessage(message);
        return ResponseEntity.ok(Map.of(
            "status", "sent",
            "message", message,
            "topic", MessageProducer.TOPIC
        ));
    }

    @PostMapping("/with-key")
    public ResponseEntity<Map<String, String>> sendWithKey(
            @RequestParam String key,
            @RequestParam String message) {
        messageProducer.sendMessageWithKey(key, message);
        return ResponseEntity.ok(Map.of(
            "status", "sent",
            "key", key,
            "message", message
        ));
    }

    /**
     * 여러 메시지를 한번에 발행 - Consumer가 메시지를 소비하는 것을 관찰하기 위해
     */
    @PostMapping("/bulk")
    public ResponseEntity<Map<String, Object>> sendBulk(@RequestParam(defaultValue = "5") int count) {
        for (int i = 1; i <= count; i++) {
            messageProducer.sendMessage("Bulk message #" + i + " at " + System.currentTimeMillis());
        }
        return ResponseEntity.ok(Map.of(
            "status", "sent",
            "count", count,
            "topic", MessageProducer.TOPIC
        ));
    }
}
