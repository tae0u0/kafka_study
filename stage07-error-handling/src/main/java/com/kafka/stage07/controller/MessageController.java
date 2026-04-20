package com.kafka.stage07.controller;

import com.kafka.stage07.producer.MessageProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * [테스트 방법]
 *
 * 정상 메시지 (처리 성공):
 *   curl -X POST "http://localhost:8086/messages?message=normal-message"
 *   → 로그: [Retry Consumer] 처리 성공
 *
 * 실패 메시지 (3회 재시도 후 DLT):
 *   curl -X POST "http://localhost:8086/messages?message=FAIL-this-message"
 *   → 로그: [Retry Consumer] 시도 #1, #2, #3 (1초 간격)
 *   → 로그: [DLT Consumer] Dead Letter 메시지 수신
 *
 * 즉시 DLT 메시지 (재시도 없이 바로 DLT):
 *   curl -X POST "http://localhost:8086/messages?message=INVALID-format"
 *   → 로그: [DLT Consumer] 즉시 수신 (재시도 없음)
 *
 * Kafka UI에서 DLT 확인:
 *   http://localhost:8989 → Topics → stage07-topic.DLT
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

    @PostMapping("/scenario")
    public ResponseEntity<Map<String, Object>> runScenario() {
        // 다양한 시나리오 메시지 발행
        producer.send("normal-1");           // 성공
        producer.send("FAIL-bad-data");      // 3회 재시도 후 DLT
        producer.send("normal-2");           // 성공
        producer.send("INVALID-format");     // 즉시 DLT
        producer.send("normal-3");           // 성공

        return ResponseEntity.ok(Map.of(
            "sent", 5,
            "expected", "normal-1,2,3 성공 / FAIL-bad-data DLT(3회 재시도) / INVALID-format 즉시 DLT"
        ));
    }
}
