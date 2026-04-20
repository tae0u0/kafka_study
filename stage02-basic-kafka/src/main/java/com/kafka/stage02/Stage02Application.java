package com.kafka.stage02;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 2단계: Kafka 기본 Producer / Consumer
 *
 * [학습 목표]
 * - Kafka Producer로 String 메시지를 토픽에 발행한다
 * - Kafka Consumer로 토픽에서 메시지를 구독한다
 * - spring-kafka의 KafkaTemplate과 @KafkaListener 사용법을 익힌다
 *
 * [실행 순서]
 * 1. docker-compose up -d        → Kafka 브로커 실행
 * 2. ./gradlew bootRun            → Spring Boot 애플리케이션 실행
 * 3. POST /messages               → 메시지 발행
 * 4. 콘솔 로그에서 Consumer 수신 확인
 * 5. http://localhost:8989        → Kafka UI에서 시각적 확인
 */
@SpringBootApplication
public class Stage02Application {
    public static void main(String[] args) {
        SpringApplication.run(Stage02Application.class, args);
    }
}
