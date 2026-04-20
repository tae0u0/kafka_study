package com.kafka.stage07.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    public static final String TOPIC = "stage07-topic";

    public void send(String message) {
        log.info("[Producer] 전송: {}", message);
        kafkaTemplate.send(TOPIC, message);
    }
}
