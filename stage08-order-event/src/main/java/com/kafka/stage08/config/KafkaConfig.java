package com.kafka.stage08.config;

import com.kafka.stage08.dto.DeliveryEvent;
import com.kafka.stage08.dto.OrderEvent;
import com.kafka.stage08.dto.PaymentEvent;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
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

@EnableKafka
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    public static final String ORDER_TOPIC = "order-events";
    public static final String PAYMENT_TOPIC = "payment-events";
    public static final String DELIVERY_TOPIC = "delivery-events";

    public static final String ORDER_DLT_TOPIC = "order-events-dlt";
    public static final String PAYMENT_DLT_TOPIC = "payment-events-dlt";
    public static final String DELIVERY_DLT_TOPIC = "delivery-events-dlt";

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

    // DLT도 원본과 같은 파티션 수로 생성
    @Bean
    public NewTopic orderDltTopic() {
        return TopicBuilder.name(ORDER_DLT_TOPIC).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentDltTopic() {
        return TopicBuilder.name(PAYMENT_DLT_TOPIC).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic deliveryDltTopic() {
        return TopicBuilder.name(DELIVERY_DLT_TOPIC).partitions(3).replicas(1).build();
    }

    @Bean
    public ProducerFactory<String, OrderEvent> orderProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfig(), new StringSerializer(), jsonSerializer());
    }

    @Bean
    public KafkaTemplate<String, OrderEvent> orderKafkaTemplate() {
        return new KafkaTemplate<>(orderProducerFactory());
    }

    @Bean
    public ProducerFactory<String, PaymentEvent> paymentProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfig(), new StringSerializer(), jsonSerializer());
    }

    @Bean
    public KafkaTemplate<String, PaymentEvent> paymentKafkaTemplate() {
        return new KafkaTemplate<>(paymentProducerFactory());
    }

    @Bean
    public ProducerFactory<String, DeliveryEvent> deliveryProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfig(), new StringSerializer(), jsonSerializer());
    }

    @Bean
    public KafkaTemplate<String, DeliveryEvent> deliveryKafkaTemplate() {
        return new KafkaTemplate<>(deliveryProducerFactory());
    }

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
        return listenerFactory(orderConsumerFactory(), orderKafkaTemplate(), ORDER_DLT_TOPIC);
    }

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
        return listenerFactory(paymentConsumerFactory(), paymentKafkaTemplate(), PAYMENT_DLT_TOPIC);
    }

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
        return listenerFactory(deliveryConsumerFactory(), deliveryKafkaTemplate(), DELIVERY_DLT_TOPIC);
    }

    private Map<String, Object> producerConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return config;
    }

    private <T> JacksonJsonSerializer<T> jsonSerializer() {
        JacksonJsonSerializer<T> serializer = new JacksonJsonSerializer<>();
        serializer.setAddTypeInfo(true);
        return serializer;
    }

    private Map<String, Object> consumerConfig(String groupId) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return config;
    }

    private <T> JacksonJsonDeserializer<T> deserializer(Class<T> targetType) {
        JacksonJsonDeserializer<T> d = new JacksonJsonDeserializer<>(targetType);
        d.addTrustedPackages("com.kafka.stage08.dto");
        d.setUseTypeHeaders(false);
        return d;
    }

    private <T> ConcurrentKafkaListenerContainerFactory<String, T> listenerFactory(
            ConsumerFactory<String, T> consumerFactory,
            KafkaTemplate<String, T> kafkaTemplate,
            String dltTopic) {

        ConcurrentKafkaListenerContainerFactory<String, T> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setConcurrency(3);

        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxAttempts(3);

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (record, ex) -> new TopicPartition(dltTopic, record.partition())
                );

        factory.setCommonErrorHandler(new DefaultErrorHandler(recoverer, backOff));
        return factory;
    }
}