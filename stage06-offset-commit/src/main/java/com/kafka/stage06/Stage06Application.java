package com.kafka.stage06;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 6단계: Offset & Commit 이해
 *
 * [학습 목표]
 * - Offset이 무엇인지, 어디에 저장되는지 이해한다
 * - Auto Commit과 Manual Commit의 차이를 코드로 확인한다
 * - 각 방식의 메시지 유실/중복 위험성을 이해한다
 *
 * [Offset이란?]
 * 각 파티션 내에서 메시지의 위치를 나타내는 번호 (0부터 시작).
 * Consumer Group별로 "어기까지 읽었다"는 오프셋을 Kafka 내부 토픽(__consumer_offsets)에 저장한다.
 * → Consumer가 재시작해도 마지막으로 커밋한 offset부터 이어서 읽을 수 있다.
 *
 * [Auto Commit]
 * Kafka 클라이언트가 일정 주기(auto.commit.interval.ms)마다 자동으로 offset을 커밋한다.
 * 장점: 코드가 간단하다
 * 단점: 처리 중 실패해도 이미 커밋된 경우 → 메시지 유실 가능
 *       처리 완료 전 커밋 → 재시작 시 재처리 안 함 → 유실
 *
 * [Manual Commit]
 * 처리 완료 후 코드에서 명시적으로 커밋한다.
 * 장점: "처리 완료 = 커밋" 이므로 유실 없음
 * 단점: 처리 후 커밋 전 실패 시 재시작하면 같은 메시지를 다시 처리 → 중복 처리 가능
 *       → 멱등성(idempotency) 처리가 필요하다
 */
@SpringBootApplication
public class Stage06Application {
    public static void main(String[] args) {
        SpringApplication.run(Stage06Application.class, args);
    }
}
