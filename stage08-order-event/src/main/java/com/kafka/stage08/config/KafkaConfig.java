package com.kafka.stage08.config;

import com.kafka.stage08.dto.OrderEvent;
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
 * 실무형 Kafka 통합 설정 - spring-kafka 4.0 기준
 *
 * [변경사항]
 * JsonSerializer  → JacksonJsonSerializer
 * JsonDeserializer → JacksonJsonDeserializer
 */
@EnableKafka
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    public static final String ORDER_TOPIC = "order-events";

    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name(ORDER_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
    }

    // =========================================================
    // Producer
    // =========================================================

    @Bean
    public ProducerFactory<String, OrderEvent> orderProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, true);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, OrderEvent> orderKafkaTemplate() {
        return new KafkaTemplate<>(orderProducerFactory());
    }

    // =========================================================
    // Consumer - 재고 서비스
    // =========================================================

    @Bean
    public ConsumerFactory<String, OrderEvent> inventoryConsumerFactory() {
        return buildConsumerFactory("inventory-service-group");
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderEvent> inventoryListenerFactory(
            KafkaTemplate<String, OrderEvent> orderKafkaTemplate) {
        return buildListenerFactory(inventoryConsumerFactory(), orderKafkaTemplate);
    }

    // =========================================================
    // Consumer - 알림 서비스
    // =========================================================

    @Bean
    public ConsumerFactory<String, OrderEvent> notificationConsumerFactory() {
        return buildConsumerFactory("notification-service-group");
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderEvent> notificationListenerFactory(
            KafkaTemplate<String, OrderEvent> orderKafkaTemplate) {
        return buildListenerFactory(notificationConsumerFactory(), orderKafkaTemplate);
    }

    // =========================================================
    // 공통 빌더
    // =========================================================

    private ConsumerFactory<String, OrderEvent> buildConsumerFactory(String groupId) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // spring-kafka 4.0: JacksonJsonDeserializer
        JacksonJsonDeserializer<OrderEvent> deserializer = new JacksonJsonDeserializer<>(OrderEvent.class);
        deserializer.addTrustedPackages("com.kafka.stage08.dto");
        deserializer.setUseTypeHeaders(false);

        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);
    }

    private ConcurrentKafkaListenerContainerFactory<String, OrderEvent> buildListenerFactory(
            ConsumerFactory<String, OrderEvent> consumerFactory,
            KafkaTemplate<String, OrderEvent> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, OrderEvent> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        factory.setConcurrency(3);

        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxAttempts(3);

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}
