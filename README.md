# 📦 Kafka 핵심 정리

---

## 1. Kafka 기본 구조

```
Producer → Topic → Partition → Consumer Group → Consumer
```

* **Producer**: 메시지 전송
* **Topic**: 메시지 카테고리
* **Partition**: 병렬 처리 단위
* **Consumer Group**: 메시지 소비 방식 결정
* **Consumer**: 실제 처리 주체

---

## 2. Partition

### 특징

* Kafka의 **병렬 처리 단위**
* **같은 partition 내부에서는 순서 보장**

### 핵심

```
같은 partition → 순서 보장
다른 partition → 순서 보장 ❌
```

---

## 3. Consumer Group

### 핵심 개념

```
같은 group → 메시지 분배 (Queue)
다른 group → 메시지 복제 (Pub/Sub)
```

---

### 같은 group

```
Partition을 나눠서 소비
→ 각 consumer는 일부 데이터만 처리
```

예:

```
P0 → consumer1
P1 → consumer2
```

👉 결과: 데이터 나눠먹기

---

### 다른 group

```
각 group이 전체 데이터를 각각 소비
```

👉 결과: 모든 서비스가 전체 이벤트 처리

---

## 4. 언제 같은 group / 다른 group 쓰는가

### 같은 group (Queue 방식)

* 로그 처리
* 이미지 처리
* 비동기 작업

```
"누군가 한 번만 처리하면 됨"
```

---

### 다른 group (Event 방식)

* 주문 이벤트
* 결제 이벤트
* 알림 처리

```
"여러 서비스가 각각 처리해야 함"
```

---

## 5. 핵심 판단 기준

```
1. 한 번만 처리하면 된다 → 같은 group
2. 여러 서비스가 각각 처리해야 한다 → 다른 group
```

---

## 6. concurrency (동시성)

### 설정

```java
factory.setConcurrency(3);
```

### 의미

```
한 컨슈머 인스턴스 내부에서 KafkaConsumer 3개 생성
```

---

### 공식

```
실제 병렬 처리 수 = min(concurrency, partition 수)
```

---

### 예시 (partition 3개)

| concurrency | 동작                 |
| ----------- | ------------------ |
| 1           | 1개 consumer가 모두 처리 |
| 2           | 2개로 나눠 처리          |
| 3           | 완전 병렬              |
| 5           | 3개만 사용 (나머지 idle)  |

---

### 핵심

```
group-id → 누가 데이터를 받을지
concurrency → 몇 개 동시에 처리할지
```

---

## 7. Ordering (순서 보장)

### 핵심

```
Kafka는 같은 partition 안에서만 순서 보장
```

---

### 안전한 경우

```
같은 orderId → 같은 partition
→ 순서 유지
```

---

### 위험한 경우

```
같은 주문이 다른 partition으로 분산
→ 순서 깨짐
```

---

### 해결 방법

```java
kafkaTemplate.send("topic", orderId, event);
```

👉 partition key = orderId

---

## 8. KafkaListener 동작

### 흐름

```
1. Kafka에서 byte[] 수신
2. Deserializer로 객체 변환
3. 메서드 파라미터에 주입
```

---

### 핵심

```
직렬화 ❌
역직렬화 ⭕
```

---

## 9. Consumer 파라미터

### 가능한 타입

```java
OrderEvent
ConsumerRecord<String, OrderEvent>
String
byte[]
List<OrderEvent>
```

---

### Header 사용

```java
@Header(KafkaHeaders.RECEIVED_KEY) String key
```

---

## 10. Batch Listener

### 개념

```
여러 메시지를 묶어서 처리
```

---

### 설정

```java
factory.setBatchListener(true);
```

---

### 예시

```java
public void listen(List<OrderEvent> events)
```

---

### 장점

* 성능 향상
* DB batch 처리 가능

---

### 단점

* 일부 실패 처리 어려움

---

## 11. 수동 커밋 (Acknowledgment)

### 개념

```
성공했을 때만 offset commit
```

---

### 예시

```java
public void listen(OrderEvent event, Acknowledgment ack) {
    process(event);
    ack.acknowledge();
}
```

---

### 장점

* 데이터 유실 방지
* 재처리 가능

---

### 단점

* 중복 처리 가능

---

### 설정

```java
factory.getContainerProperties().setAckMode(MANUAL);
```

---

## 12. Batch + Manual Commit

```java
public void listen(List<OrderEvent> events, Acknowledgment ack) {
    process(events);
    ack.acknowledge();
}
```

---

## 13. Consumer Group 설계

### 원칙

```
group-id = 서비스 단위
```

---

### 예시

```
prod-order
prod-notification
prod-analytics
```

---

### 환경 분리

```
dev-order
staging-order
prod-order
```

---

## 14. 토픽 vs 서버

```
Kafka 서버 ≠ 토픽
```

* 서버 1대 → 토픽 여러 개 가능
* 토픽은 논리적 분리

---

## 15. 토픽 설계

### 같은 타입

* 설정 그대로 사용 가능

### 다른 타입

* Producer / Consumer 분리 필요

---

# 🚀 최종 핵심 정리

```
Kafka 설계 핵심 질문:

이 메시지는
"나눠서 처리할 것인가?"
"각자 모두 처리할 것인가?"
```

---

## 🔥 한 줄 요약

```
group-id는 소비 방식,
partition은 병렬 처리,
concurrency는 처리 속도를 결정한다
```