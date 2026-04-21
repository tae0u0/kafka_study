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

## 16. 프로듀서 파티션 선택 전략

Kafka 프로듀서가 "어느 파티션으로 보낼지" 결정하는 방식은 5가지다.

---

### 방식 1: 직접 파티션 지정

```java
kafkaTemplate.send("topic", partition, key, value);
// 또는
new ProducerRecord<>("topic", 2, key, value); // partition=2 고정
```

* 파티션 번호를 코드에서 명시적으로 지정
* 파티셔너 로직 자체를 건너뜀
* 특정 파티션에 특수 목적 데이터를 넣을 때 사용
* **주의**: 파티션 수 변경 시 코드도 함께 수정해야 함

---

### 방식 2: 키 해시 기반 (Key-based)

```java
kafkaTemplate.send("topic", userId, message);
```

```
파티션 = murmur2(key) % partitionCount
```

* key가 있으면 항상 동일한 파티션 → **같은 key는 순서 보장**
* 같은 userId의 이벤트(주문생성 → 결제 → 배송)가 항상 같은 파티션에 들어감
* 핫 파티션 주의: 특정 key가 압도적으로 많으면 편중 발생

```
user-1 → P0
user-2 → P2
user-3 → P1
user-1 → P0 (항상 같은 파티션)
```

---

### 방식 3: Sticky Partitioner (키 없는 기본 동작)

```java
kafkaTemplate.send("topic", message); // key = null
```

* **Kafka 2.4+의 기본 동작** (key=null일 때)
* 배치 하나가 꽉 차거나 `linger.ms`가 만료될 때까지 같은 파티션에 계속 전송
* 배치가 전송되면 다음 파티션으로 이동 (랜덤 선택)
* RoundRobin보다 배치 효율이 높아 처리량이 좋음

```
배치1: NoKey-1, NoKey-2, NoKey-3 → P2 (배치 전송)
배치2: NoKey-4, NoKey-5, NoKey-6 → P0 (다음 파티션)
배치3: NoKey-7, NoKey-8, NoKey-9 → P1
```

> **bulk-no-key로 9개를 보내도 P2에만 들어가는 이유**
> 9개가 루프 내에서 너무 빠르게 생성되어 하나의 배치로 묶임
> → Sticky Partitioner가 배치 단위로 파티션을 고르므로 전부 같은 파티션으로 전송됨

---

### 방식 4: RoundRobin Partitioner

```yaml
# application.yml
spring:
  kafka:
    producer:
      properties:
        partitioner.class: org.apache.kafka.clients.producer.RoundRobinPartitioner
```

* key가 없을 때 메시지마다 파티션을 순서대로 돌아가며 선택
* 균등 분산이 목적일 때 사용
* 배치 효율은 Sticky보다 낮음 (메시지마다 다른 파티션 → 배치 분산)

```
NoKey-1 → P0
NoKey-2 → P1
NoKey-3 → P2
NoKey-4 → P0 (순환)
```

---

### 방식 5: 커스텀 파티셔너

```java
public class CustomPartitioner implements Partitioner {
    @Override
    public int partition(String topic, Object key, byte[] keyBytes,
                         Object value, byte[] valueBytes, Cluster cluster) {
        int partitionCount = cluster.partitionCountForTopic(topic);
        // 비즈니스 로직으로 파티션 결정
        String msg = (String) value;
        if (msg.startsWith("VIP")) return 0; // VIP는 항상 P0
        return 1 + (Math.abs(msg.hashCode()) % (partitionCount - 1));
    }
}
```

```yaml
spring:
  kafka:
    producer:
      properties:
        partitioner.class: com.example.CustomPartitioner
```

* 비즈니스 규칙으로 파티션을 직접 제어
* VIP 우선 처리, 지역별 분배 등 특수 요구사항에 활용

---

### partitioner.ignore.keys 옵션

```yaml
spring:
  kafka:
    producer:
      properties:
        partitioner.ignore.keys: true
```

* key가 있어도 키 해시를 무시하고 Sticky/RoundRobin 방식으로 파티션 선택
* key는 메시지 식별용으로만 쓰고 파티션은 균등 분산하고 싶을 때 사용

---

### Kafka 버전별 변경 사항

#### Kafka 4.0 (Breaking Change)

| 변경 내용 | 설명 |
|-----------|------|
| `DefaultPartitioner` 제거 | 별도 클래스 없이 프로듀서 내부에 기본 동작 통합 |
| `UniformStickyPartitioner` 제거 | Sticky 동작이 기본값으로 내장되어 별도 클래스 불필요 |

* Kafka 4.0 이전에는 `partitioner.class`를 명시하지 않으면 `DefaultPartitioner`가 사용됨
* Kafka 4.0부터는 프로듀서 내부 로직으로 통합 — key 유무에 따라 자동 분기

#### 버전별 key=null 기본 동작

| 버전 | key=null 기본 동작 |
|------|-------------------|
| Kafka 2.3 이하 | RoundRobin |
| Kafka 2.4 ~ 3.x | Sticky Partitioner (DefaultPartitioner 내장) |
| Kafka 4.0+ | Sticky 동작 내장 (DefaultPartitioner 클래스 제거) |

---

### 5가지 방식 비교 요약

| 방식 | key 필요 | 순서 보장 | 균등 분산 | 사용 시점 |
|------|---------|----------|----------|----------|
| 직접 파티션 지정 | 선택 | 파티션 내 보장 | X | 특정 파티션 강제 |
| 키 해시 기반 | O | 같은 key끼리 보장 | key 분포에 따라 다름 | 순서가 중요한 이벤트 |
| Sticky | X | X | 배치 단위 분산 | 기본값, 처리량 우선 |
| RoundRobin | X | X | O | key 없이 균등 분산 |
| 커스텀 파티셔너 | 선택 | 로직에 따라 | 로직에 따라 | 비즈니스 규칙 필요 시 |

---

## 17. Kafka를 쓰는 이유 (동기 vs 비동기)

### 동기 방식의 문제

```
OrderService → InventoryService (200ms) → NotificationService (300ms)
총 응답시간: 500ms 이상 (순차 블로킹)
```

```
[문제 1] 장애 전파: 알림 서비스 장애가 주문 서비스 실패로 이어짐
[문제 2] 응답 지연: 독립적인 작업을 순차 실행
[문제 3] 강한 결합: 새 서비스 추가 시 OrderService 코드 수정 필요
[문제 4] 확장 어려움: 트래픽 증가 시 모든 서비스가 함께 부하 증가
```

### Kafka 도입 후

```
OrderService → Kafka 이벤트 발행 (5ms) → 즉시 응답 반환

이후 비동기로:
InventoryService → 이벤트 소비
NotificationService → 이벤트 소비 (독립적으로)
```

```
응답시간: 500ms → 5ms
결합도: 강결합 → 느슨한 결합
확장성: 서비스별 독립 확장 가능
```

---

## 18. Spring Boot 4.0 Kafka 설정 변화

### @EnableKafka 필수

```java
@EnableKafka   // 없으면 @KafkaListener가 동작하지 않음
@Configuration
public class KafkaConfig { ... }
```

### Bean 수동 정의 필수

Spring Boot 4.0부터 Kafka 자동 설정이 제거됨 → `ProducerFactory`, `ConsumerFactory`, `KafkaTemplate`, `ConcurrentKafkaListenerContainerFactory`를 모두 `@Configuration`에서 직접 정의해야 한다.

### 권장 Producer 설정

```java
config.put(ProducerConfig.ACKS_CONFIG, "all");         // 모든 레플리카 수신 확인
config.put(ProducerConfig.RETRIES_CONFIG, 3);           // 전송 실패 시 3회 재시도
config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // 중복 발행 방지
```

### KafkaTemplate 비동기 전송 패턴

```java
CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(TOPIC, key, value);

future.whenComplete((result, ex) -> {
    if (ex == null) {
        int partition = result.getRecordMetadata().partition();
        long offset = result.getRecordMetadata().offset();
        log.info("전송 성공 - partition: {}, offset: {}", partition, offset);
    } else {
        log.error("전송 실패", ex);
    }
});
```

* `send()`는 즉시 반환 (non-blocking)
* 전송 결과는 콜백(`whenComplete`)으로 확인

---

## 19. JSON 직렬화

### spring-kafka 4.0 클래스명 변경

| 구버전 (3.x 이하) | 신버전 (4.0+) |
|------------------|--------------|
| `JsonSerializer` | `JacksonJsonSerializer` |
| `JsonDeserializer` | `JacksonJsonDeserializer` |

```yaml
spring:
  kafka:
    producer:
      value-serializer: org.springframework.kafka.support.serializer.JacksonJsonSerializer
    consumer:
      value-deserializer: org.springframework.kafka.support.serializer.JacksonJsonDeserializer
      properties:
        spring.json.trusted.packages: "com.example.dto"
```

### @NoArgsConstructor 필수

```java
@NoArgsConstructor   // ← Jackson 역직렬화 필수 (없으면 역직렬화 실패)
@AllArgsConstructor
@Builder
@Getter
public class OrderEvent {
    private String orderId;
    private String userId;
    ...
}
```

역직렬화 흐름:
```
byte[] → JSON 문자열 → ObjectMapper → 기본 생성자로 객체 생성 → 필드 주입
                                       ↑ @NoArgsConstructor 없으면 여기서 실패
```

### containerFactory 명시

JSON 타입을 사용하는 `@KafkaListener`는 `containerFactory`를 반드시 지정해야 한다.

```java
@KafkaListener(
    topics = "order-events",
    groupId = "order-group",
    containerFactory = "orderEventListenerContainerFactory"  // 명시 필수
)
public void consume(OrderEvent event) { ... }
```

---

## 20. 이벤트 설계 원칙

```
[원칙 1] 과거형 이름: OrderCreated (O)  /  CreateOrder (X)
[원칙 2] 불변성: setter 없음, final 필드
[원칙 3] 충분한 정보: Consumer가 추가 DB 조회 없이 처리 가능하도록 필드 포함
[원칙 4] 추적 ID 포함: 이벤트 체인에서 원본 ID를 다음 이벤트에 전달
[원칙 5] 버전 관리: 필드 추가는 OK, 필드 삭제/타입 변경은 하위 호환성 고려
```

### 이벤트 체인에서 추적 ID

```
OrderEvent  { orderId }
    ↓
PaymentEvent  { paymentId, orderId }          ← orderId 포함
    ↓
DeliveryEvent { deliveryId, paymentId, orderId }  ← 체인 전체 추적 가능
```

---

## 21. AckMode 종류

수동 커밋 사용 시 `AckMode`로 커밋 시점을 제어한다.

```java
factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
```

| AckMode | 커밋 시점 |
|---------|---------|
| `RECORD` | 메시지 1건 처리 후 자동 커밋 |
| `BATCH` | poll()로 받은 배치 전체 처리 후 자동 커밋 |
| `MANUAL` | `acknowledge()` 호출 시 다음 배치 처리 후 커밋 |
| `MANUAL_IMMEDIATE` | `acknowledge()` 호출 즉시 커밋 |

### Auto Commit의 메시지 유실 시나리오

```
Time 0s: offset 5 메시지 poll
Time 1s: 처리 중 (DB 저장 시도)
Time 3s: [자동 커밋] offset 6으로 커밋 완료 ← 아직 처리 중인데!
Time 4s: 처리 실패, Consumer 재시작
Time 5s: offset 6부터 읽기 시작
결과: offset 5 메시지 영원히 유실
```

### Manual Commit의 중복 처리 시나리오

```
Time 0: offset 5 처리 완료
Time 1: acknowledge() 호출 직전 Consumer 크래시
Time 2: Consumer 재시작 → offset 5부터 다시 읽음
결과: offset 5 메시지 중복 처리
```

→ 해결책: **멱등성 설계** (같은 메시지를 두 번 처리해도 결과가 동일)

```java
// 나쁜 예: INSERT는 중복 시 에러 또는 데이터 중복
repository.save(event);

// 좋은 예: orderId 기준 upsert로 멱등성 보장
repository.findByOrderId(event.getOrderId())
    .orElseGet(() -> repository.save(event));
```

---

## 22. 에러 핸들링 (DefaultErrorHandler + DLT)

### 구성 요소

```java
@Bean
public DefaultErrorHandler errorHandler(KafkaTemplate<String, String> kafkaTemplate) {
    // 실패 메시지를 DLT로 전송하는 Recoverer
    DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);

    // 재시도 전략 설정
    FixedBackOff backOff = new FixedBackOff(1000L, 3L); // 1초 간격, 최대 3회

    DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);

    // 재시도 없이 즉시 DLT로 보낼 예외 지정
    handler.addNotRetryableExceptions(IllegalArgumentException.class);
    return handler;
}
```

```java
// containerFactory에 등록
factory.setCommonErrorHandler(errorHandler);
```

### 처리 흐름

```
예외 발생 → NotRetryable?
    ├── Yes → 즉시 DLT 이동
    └── No  → 재시도 (BackOff 간격으로)
                 ├── 성공 → 정상 처리
                 └── 모두 실패 → DLT 이동
```

### NotRetryable 예외 선택 기준

```
재시도할 가치 없음 (NotRetryable):
  → JSON 형식 오류, 데이터 검증 실패, 코드 버그

재시도할 가치 있음:
  → DB 연결 실패, 외부 API 타임아웃, 일시적 네트워크 오류
```

### DLT 토픽 이름

```
원본 토픽: stage07-topic
DLT 토픽:  stage07-topic-dlt  ← 자동 생성
```

| spring-kafka 버전 | DLT 접미사 |
|------------------|-----------|
| 2.x ~ 3.x        | `.DLT`    |
| 4.x+             | `-dlt`    |

### DLT 목적지 결정 방식

`DeadLetterPublishingRecoverer` 내부에 `destinationResolver`가 기본값으로 내장되어 있다.

```java
// spring-kafka 4.x 내부 기본 resolver
(record, exception) -> new TopicPartition(record.topic() + "-dlt", record.partition())
```

`@KafkaListener`가 어느 토픽을 구독하는지는 전혀 영향을 주지 않는다.
recoverer가 독립적으로 목적지를 결정해서 전송한다.

목적지를 직접 지정하려면 두 번째 인자로 커스텀 resolver를 넘기면 된다.

```java
new DeadLetterPublishingRecoverer(kafkaTemplate,
    (record, ex) -> new TopicPartition("my-custom-dlt", record.partition())
);
```

### @KafkaListener와 토픽 자동 생성

`@KafkaListener`에 토픽 이름을 선언하는 것은 구독 선언일 뿐, Spring이 토픽을 생성하지는 않는다.
하지만 컨슈머가 구독을 시작할 때 브로커에 메타데이터 요청을 보내고, 브로커의
`auto.create.topics.enable=true`(기본값)에 의해 해당 토픽이 자동 생성된다.

```
앱 시작
  → @KafkaListener가 "stage07-topic.DLT" 구독 시도
  → 브로커에 메타데이터 요청
  → 브로커: 없는 토픽 + auto.create.topics.enable=true
  → stage07-topic.DLT 토픽 자동 생성 (빈 토픽)
```

잘못된 토픽 이름으로 구독하면 두 토픽이 동시에 만들어지는 상황이 발생한다.

```
stage07-topic.DLT  ← @KafkaListener 구독 시도로 생성 (메시지 없음, 빈 토픽)
stage07-topic-dlt  ← DeadLetterPublishingRecoverer가 메시지 전송하며 생성 (실제 DLT 메시지)
```

컨슈머는 빈 토픽만 바라보고, 실제 실패 메시지는 다른 토픽에 쌓이는 무증상 버그가 된다.

### DLT 메시지 헤더 활용

```java
@KafkaListener(topics = "stage07-topic-dlt", groupId = "dlt-group")
public void consumeDlt(ConsumerRecord<String, String> record) {
    String originalTopic   = getHeader(record, "kafka_dlt-original-topic");
    String originalOffset  = getHeader(record, "kafka_dlt-original-offset");
    String exceptionClass  = getHeader(record, "kafka_dlt-exception-fqcn");
    String exceptionMsg    = getHeader(record, "kafka_dlt-exception-message");

    // 실무: DB 저장 → 알림 발송 → 수동 재처리 대기열 등록
}
```

---

## 23. 재시도 전략

### FixedBackOff

```java
new FixedBackOff(1000L, 3L);
// 1초 간격으로 최대 3회 재시도
// 1초 → 1초 → 1초
```

### ExponentialBackOff

```java
ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
backOff.setMaxAttempts(3);
// 지수적으로 증가
// 1초 → 2초 → 4초
```

| 전략 | 간격 | 사용 시점 |
|------|------|---------|
| FixedBackOff | 일정 | 단순한 재시도, 빠른 복구 기대 |
| ExponentialBackOff | 지수 증가 | 외부 API, DB 등 부하 분산 필요 시 권장 |

---

## 24. 다중 토픽 & 이벤트 체인

### 타입별 Factory 분리

토픽마다 메시지 타입이 다른 경우 각각 별도의 Factory와 KafkaTemplate을 정의한다.

```java
// OrderEvent용
@Bean ProducerFactory<String, OrderEvent> orderProducerFactory() { ... }
@Bean KafkaTemplate<String, OrderEvent> orderKafkaTemplate() { ... }
@Bean ConsumerFactory<String, OrderEvent> orderConsumerFactory() { ... }
@Bean ConcurrentKafkaListenerContainerFactory<String, OrderEvent> orderListenerFactory() { ... }

// PaymentEvent용
@Bean ProducerFactory<String, PaymentEvent> paymentProducerFactory() { ... }
@Bean KafkaTemplate<String, PaymentEvent> paymentKafkaTemplate() { ... }
// ... 동일한 패턴 반복
```

### Consumer가 Producer가 되는 패턴

```
[OrderConsumer]
  → 주문 저장
  → PaymentEvent 발행  ← Consumer이면서 동시에 Producer

[PaymentConsumer]
  → 결제 처리
  → DeliveryEvent 발행

[DeliveryConsumer]
  → 배송 처리 (최종)
```

### Producer 멱등성 설정

```java
config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
```

* 네트워크 재시도 시 Broker가 중복 메시지를 감지하고 하나만 저장
* `acks=all` + `retries > 0`과 함께 사용해야 효과 있음

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

1. 문제 상황

Kafka 이벤트 처리 과정에서 다음과 같은 문제가 발생했다.

ObjectMapper 바인딩 관련 컴파일 오류
JacksonJsonDeserializer(Class<T>, ObjectMapper) 생성자 미지원
Consumer 처리 실패 후 무한 retry
로그 반복
Record in retry and not yet recovered
Seeking to offset 0 for partition order-events-1
2. ObjectMapper 관련 이슈
   2.1 기존 접근 방식
   @Autowired
   private ObjectMapper objectMapper;

private <T> JacksonJsonSerializer<T> jsonSerializer() {
return new JacksonJsonSerializer<>(objectMapper);
}

private <T> JacksonJsonDeserializer<T> deserializer(Class<T> targetType) {
return new JacksonJsonDeserializer<>(targetType, objectMapper);
}

👉 의도

Spring Boot가 제공하는 ObjectMapper 사용
LocalDateTime 정상 직렬화 (ISO-8601)
2.2 발생한 문제
생성자 'JacksonJsonDeserializer(Class<T>, ObjectMapper)'을(를) 해결할 수 없습니다

👉 원인

현재 사용 중인 Spring Kafka 4.x
해당 생성자가 존재하지 않음
2.3 버전 차이에 따른 변화
항목	기존 방식	현재 방식
ObjectMapper	직접 주입	사용 안 함
Serializer	new JacksonJsonSerializer<>(objectMapper)	new JacksonJsonSerializer<>()
Deserializer	new JacksonJsonDeserializer<>(type, objectMapper)	new JacksonJsonDeserializer<>(type)
기준	Mapper 중심	targetType 중심
2.4 수정된 코드
private <T> JacksonJsonSerializer<T> jsonSerializer() {
JacksonJsonSerializer<T> serializer = new JacksonJsonSerializer<>();
serializer.setAddTypeInfo(true);
return serializer;
}

private <T> JacksonJsonDeserializer<T> deserializer(Class<T> targetType) {
JacksonJsonDeserializer<T> d = new JacksonJsonDeserializer<>(targetType);
d.addTrustedPackages("com.kafka.stage08.dto");
d.setUseTypeHeaders(false);
return d;
}
2.5 핵심 학습 포인트

✔ ObjectMapper 빈 존재 여부 ≠ 사용 가능 여부
✔ 라이브러리 생성자 시그니처가 더 중요
✔ 버전 업 시 API 구조 변화 반드시 확인

3. Consumer AckMode 문제
   3.1 현재 코드
   factory.getContainerProperties().setAckMode(AckMode.RECORD);
   public void consume(..., Acknowledgment ack) {
   ...
   ack.acknowledge();
   }
   3.2 문제 핵심
   설정	코드
   자동 커밋 (RECORD)	수동 커밋 사용

👉 서로 충돌

3.3 결과
메시지 처리 성공
→ ack 호출 시 예외
→ retry 발생
→ offset rollback
→ 다시 consume
→ 무한 반복
3.4 해결 방법
✅ 방법 1 (추천): 자동 커밋 유지
public void consume(ConsumerRecord<String, OrderEvent> record) {
...
}

✔ Acknowledgment 제거

방법 2: 수동 커밋 사용
factory.getContainerProperties().setAckMode(AckMode.MANUAL);

✔ 이 경우만 ack.acknowledge() 사용

3.5 선택 이유 (포트폴리오 기준)

👉 AckMode.RECORD + 자동 커밋 추천

구조 단순
설명 쉬움
retry 흐름 명확
실습/포폴에 적합
4. retry 무한 루프 원인 정리
   offset 0 계속 재조회
   → 처리 실패
   → retry
   → offset commit 안됨
   → 다시 읽음

👉 핵심 원인

AckMode 설정 불일치
ack 호출 시 예외 발생
5. 최종 정리
   ✔ ObjectMapper
   직접 주입 ❌
   기본 생성자 사용 ⭕
   ✔ Kafka Consumer
   AckMode와 코드 일치해야 함
   자동 커밋이면 ack 사용 ❌
   수동 커밋이면 AckMode 변경 필수
6. 적용 결과
   컴파일 오류 해결
   JSON 역직렬화 정상 동작
   retry 루프 제거
   Kafka 이벤트 흐름 안정화
7. 회고

이번 트러블슈팅을 통해 다음을 학습했다.

1. 라이브러리 버전이 설계를 바꾼다

단순 설정 문제가 아니라
👉 API 구조 자체가 바뀐 문제

2. Kafka는 "설정 + 코드"가 세트다

특히 중요 👇

AckMode
Listener 시그니처
ErrorHandler

👉 하나라도 어긋나면 retry 지옥

3. 진짜 원인 찾는 기준
   Record in retry and not yet recovered

이 로그 나오면 무조건:

역직렬화 문제
비즈니스 예외
ack 문제

👉 이 3개부터 본다

🚀 한 줄 정리

Kafka 문제는 대부분 “데이터 문제가 아니라 설정-코드 불일치”에서 시작된다.