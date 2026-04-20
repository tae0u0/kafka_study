package com.kafka.stage04.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Group B Consumer - 완전히 독립적인 소비
 *
 * [핵심 포인트]
 * Group B는 Group A와 같은 토픽을 구독하지만,
 * 각자의 offset을 독립적으로 관리한다.
 *
 * 즉, Group A가 메시지를 소비해도 Group B의 offset에는 영향이 없다.
 * Group B는 처음부터(auto-offset-reset: earliest) 모든 메시지를 읽는다.
 *
 * [실무 사용 예시]
 * 같은 "주문 생성" 이벤트를:
 * - Group A (결제 서비스): 결제 처리
 * - Group B (알림 서비스): 주문 확인 이메일 발송
 * - Group C (물류 서비스): 배송 준비
 * 각각 독립적으로 처리 → 이것이 이벤트 기반 아키텍처의 핵심!
 */
@Slf4j
@Service
public class GroupBConsumer {

    @KafkaListener(
        topics = "stage04-topic",
        groupId = "group-B",        // Group B: Group A와 완전히 독립
        concurrency = "3"
    )
    public void consume(ConsumerRecord<String, String> record) {
        log.info("[Group-B] 수신 → partition: {}, offset: {}, key: {}, value: {}",
            record.partition(),
            record.offset(),
            record.key(),
            record.value());

        // Group B의 처리 로직 (예: 알림 전송)
    }
}
