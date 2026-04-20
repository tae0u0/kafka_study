package com.kafka.stage08.config;

import com.kafka.stage08.dto.DeliveryEvent;
import com.kafka.stage08.dto.OrderEvent;
import com.kafka.stage08.dto.PaymentEvent;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * 다중 토픽 Kafka 설정
 *
 * [토픽 구조]
 * order-events    : 주문 생성 이벤트   (OrderEvent)
 * payment-events  : 결제 요청 이벤트   (PaymentEvent)
 * delivery-events : 배송 시작 이벤트   (DeliveryEvent)
 *
 * [다중 KafkaTemplate 관리 전략]
 * 이벤트 타입마다 ProducerFactory + KafkaTemplate을 별도로 정의한다.
 * 주입 시 @Qualifier로 원하는 Bean을 지정한다.
 *
 * [대안: 단일 KafkaTemplate<String, Object>]
 * 타입을 Object로 두면 Bean이 하나만 필요하지만,
 * 타입 안정성이 떨어지고 직렬화 설정이 복잡해진다.
 * 명확한 이벤트 타입이 있다면 타입별 KafkaTemplate이 더 안전하다.
 */
@EnableKafka
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // =========================================================
    // 토픽 이름 상수
    // =========================================================

    public static final String ORDER_TOPIC    = "order-events";
    public static final String PAYMENT_TOPIC  = "payment-events";
    public static final String DELIVERY_TOPIC = "delivery-events";

    // =========================================================
    // 토픽 생성 (파티션 3개, KafkaAdmin Bean 필요)
    // =========================================================

    @Bean
    public KafkaAdmin kafkaAdmin() {
        return new KafkaAdmin(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers));
    }

    @Bean
    public NewTopic orderTopic() {
        return TopicBuilder.name(ORDER_TOPIC).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentTopic() {
        return TopicBuilder.name(PAYMENT_TOPIC).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic deliveryTopic() {
        return TopicBuilder.name(DELIVERY_TOPIC).partitions(3).replicas(1).build();
    }

    // =========================================================
    // Producer - OrderEvent
    // =========================================================

    @Bean
    public ProducerFactory<String, OrderEvent> orderProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfig(OrderEvent.class));
    }

    @Bean
    public KafkaTemplate<String, OrderEvent> orderKafkaTemplate() {
        return new KafkaTemplate<>(orderProducerFactory());
    }

    // =========================================================
    // Producer - PaymentEvent
    // =========================================================

    @Bean
    public ProducerFactory<String, PaymentEvent> paymentProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfig(PaymentEvent.class));
    }

    @Bean
    public KafkaTemplate<String, PaymentEvent> paymentKafkaTemplate() {
        return new KafkaTemplate<>(paymentProducerFactory());
    }

    // =========================================================
    // Producer - DeliveryEvent
    // =========================================================

    @Bean
    public ProducerFactory<String, DeliveryEvent> deliveryProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfig(DeliveryEvent.class));
    }

    @Bean
    public KafkaTemplate<String, DeliveryEvent> deliveryKafkaTemplate() {
        return new KafkaTemplate<>(deliveryProducerFactory());
    }

    // =========================================================
    // Consumer Factory - OrderEvent
    // =========================================================

    @Bean
    public ConsumerFactory<String, OrderEvent> orderConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
            consumerConfig("order-consumer-group"),
            new StringDeserializer(),
            deserializer(OrderEvent.class)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderEvent> orderListenerFactory() {
        return listenerFactory(orderConsumerFactory(), orderKafkaTemplate());
    }

    // =========================================================
    // Consumer Factory - PaymentEvent
    // =========================================================

    @Bean
    public ConsumerFactory<String, PaymentEvent> paymentConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
            consumerConfig("payment-consumer-group"),
            new StringDeserializer(),
            deserializer(PaymentEvent.class)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentEvent> paymentListenerFactory() {
        return listenerFactory(paymentConsumerFactory(), paymentKafkaTemplate());
    }

    // =========================================================
    // Consumer Factory - DeliveryEvent
    // =========================================================

    @Bean
    public ConsumerFactory<String, DeliveryEvent> deliveryConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
            consumerConfig("delivery-consumer-group"),
            new StringDeserializer(),
            deserializer(DeliveryEvent.class)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DeliveryEvent> deliveryListenerFactory() {
        return listenerFactory(deliveryConsumerFactory(), deliveryKafkaTemplate());
    }

    // =========================================================
    // 공통 빌더 메서드
    // =========================================================

    /** Producer 공통 설정: 타입만 다르고 나머지는 동일 */
    private <T> Map<String, Object> producerConfig(Class<T> valueType) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, true);
        return config;
    }

    /** Consumer 공통 설정: group-id만 다르고 나머지는 동일 */
    private Map<String, Object> consumerConfig(String groupId) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return config;
    }

    /** 역직렬화기: 타입만 지정하면 나머지는 공통 */
    private <T> JacksonJsonDeserializer<T> deserializer(Class<T> targetType) {
        JacksonJsonDeserializer<T> d = new JacksonJsonDeserializer<>(targetType);
        d.addTrustedPackages("com.kafka.stage08.dto");
        d.setUseTypeHeaders(false);
        return d;
    }

    /** ListenerContainerFactory: DLT 에러 핸들러 포함 */
    private <T> ConcurrentKafkaListenerContainerFactory<String, T> listenerFactory(
            ConsumerFactory<String, T> consumerFactory,
            KafkaTemplate<String, T> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, T> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        factory.setConcurrency(3);

        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxAttempts(3);
        factory.setCommonErrorHandler(
            new DefaultErrorHandler(new DeadLetterPublishingRecoverer(kafkaTemplate), backOff)
        );
        return factory;
    }
}
