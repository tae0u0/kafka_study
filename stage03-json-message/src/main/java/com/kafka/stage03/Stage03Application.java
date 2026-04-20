package com.kafka.stage03;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 3단계: JSON 메시지 송수신
 *
 * [학습 목표]
 * - String 대신 Java DTO 객체를 Kafka로 주고받는다
 * - JsonSerializer/JsonDeserializer 동작 원리를 이해한다
 * - 타입 정보(TypeId 헤더) 설정의 필요성을 이해한다
 *
 * [왜 JSON인가?]
 * 실무에서는 단순 문자열이 아닌 구조화된 데이터(주문, 결제, 사용자 이벤트 등)를 전송한다.
 * JSON은 언어/플랫폼에 무관하게 통용되는 형식이라 마이크로서비스 간 통신에 적합하다.
 *
 * [대안: Avro, Protobuf]
 * JSON보다 효율적인 바이너리 직렬화 형식이다.
 * Schema Registry와 함께 사용하면 스키마 진화(버전 관리)도 가능하다.
 * 학습 순서: JSON 먼저 → 이후 Avro/Protobuf로 발전
 */
@SpringBootApplication
public class Stage03Application {
    public static void main(String[] args) {
        SpringApplication.run(Stage03Application.class, args);
    }
}
