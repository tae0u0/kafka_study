package com.kafka.stage07;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 7단계: 장애 처리 - 재시도 & Dead Letter Topic(DLT)
 *
 * [학습 목표]
 * - Consumer 처리 중 예외가 발생하면 어떻게 되는지 이해한다
 * - Spring Kafka의 재시도(retry) 메커니즘을 설정하고 확인한다
 * - 재시도 후에도 실패한 메시지가 DLT로 이동하는 것을 확인한다
 *
 * [Dead Letter Topic(DLT)이란?]
 * 여러 번 재시도해도 처리할 수 없는 "독성 메시지(Poison Pill)"를
 * 별도의 토픽(DLT)으로 이동시키는 패턴.
 *
 * 원본 토픽: stage07-topic
 * DLT       : stage07-topic.DLT  ← 자동 생성
 *
 * [흐름]
 * 메시지 수신
 *   → 처리 실패 → 1초 후 재시도 (1차)
 *   → 처리 실패 → 2초 후 재시도 (2차)
 *   → 처리 실패 → 4초 후 재시도 (3차)
 *   → 처리 실패 → DLT로 전송 (더 이상 재시도 안 함)
 *
 * [DLT 메시지 처리 전략]
 * 1. DLT Consumer로 별도 처리 (수동 재처리, 알림 발송)
 * 2. 일정 시간 후 재시도 (지연 재처리)
 * 3. 로그 기록 후 폐기 (비즈니스적으로 무시 가능한 경우)
 */
@SpringBootApplication
public class Stage07Application {
    public static void main(String[] args) {
        SpringApplication.run(Stage07Application.class, args);
    }
}
