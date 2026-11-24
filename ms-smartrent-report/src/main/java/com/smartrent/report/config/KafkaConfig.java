package com.smartrent.report.config;

import com.smartrent.report.dto.CatalogEventDto;
import com.smartrent.report.dto.RentalEventDto;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.lang.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092,localhost:9093,localhost:9094}")
    private String bootstrapServers;

    @Value("${smartrent.kafka.consumer.group-id:report-group}")
    private String groupId;

    @Value("${smartrent.kafka.consumer.concurrency:3}")
    private int concurrency;

    @Value("${smartrent.kafka.topics.rentals:rentals.events}")
    private String rentalsTopic;

    @Value("${smartrent.kafka.topics.rentals-dlt:rentals.events-dlt}")
    private String rentalsDltTopic;

    @Value("${smartrent.kafka.topics.catalog:catalog.events}")
    private String catalogTopic;

    @Value("${smartrent.kafka.topics.catalog-dlt:catalog.events-dlt}")
    private String catalogDltTopic;

    @Value("${smartrent.kafka.topics.partitions:3}")
    private int partitions;

    @Value("${smartrent.kafka.topics.replicas:3}")
    private int replicas;

    @Bean
    public ProducerFactory<String, Object> dltProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(@NonNull ProducerFactory<String, Object> dltProducerFactory) {
        return new KafkaTemplate<>(dltProducerFactory);
    }

    @Bean
    public CommonErrorHandler kafkaErrorHandler(@NonNull KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (consumerRecord, ex) -> new TopicPartition(consumerRecord.topic() + "-dlt", consumerRecord.partition())
        );
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(2);
        backOff.setInitialInterval(1000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(5000L);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(IllegalArgumentException.class, DeserializationException.class);
        return handler;
    }

    @NonNull
    private Map<String, Object> baseConsumerProps(String consumerGroupId) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 50);
        return props;
    }

    @NonNull
    private <T> ConsumerFactory<String, T> createJsonConsumerFactory(Class<T> targetType) {
        Map<String, Object> props = baseConsumerProps(groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, targetType.getName());
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, RentalEventDto> rentalKafkaListenerContainerFactory(
            @NonNull CommonErrorHandler kafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, RentalEventDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(createJsonConsumerFactory(RentalEventDto.class));
        factory.setConcurrency(concurrency);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CatalogEventDto> catalogKafkaListenerContainerFactory(
            @NonNull CommonErrorHandler kafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, CatalogEventDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(createJsonConsumerFactory(CatalogEventDto.class));
        factory.setConcurrency(concurrency);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> dltKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        Map<String, Object> props = baseConsumerProps(groupId + "-dlt");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        ConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(props);
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(1);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }

    @Bean
    public NewTopic rentalsEventsTopic() {
        return TopicBuilder.name(Objects.requireNonNull(rentalsTopic))
                .partitions(partitions)
                .replicas(replicas)
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .config(TopicConfig.RETENTION_MS_CONFIG, "604800000")
                .config(TopicConfig.RETENTION_BYTES_CONFIG, "1073741824")
                .build();
    }

    @Bean
    public NewTopic rentalsEventsDltTopic() {
        return TopicBuilder.name(Objects.requireNonNull(rentalsDltTopic))
                .partitions(partitions)
                .replicas(replicas)
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .config(TopicConfig.RETENTION_MS_CONFIG, "2592000000")
                .build();
    }

    @Bean
    public NewTopic catalogEventsTopic() {
        return TopicBuilder.name(Objects.requireNonNull(catalogTopic))
                .partitions(partitions)
                .replicas(replicas)
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, "compact,delete")
                .config(TopicConfig.RETENTION_MS_CONFIG, "1209600000")
                .config(TopicConfig.SEGMENT_MS_CONFIG, "86400000")
                .config(TopicConfig.DELETE_RETENTION_MS_CONFIG, "86400000")
                .build();
    }

    @Bean
    public NewTopic catalogEventsDltTopic() {
        return TopicBuilder.name(Objects.requireNonNull(catalogDltTopic))
                .partitions(partitions)
                .replicas(replicas)
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .config(TopicConfig.RETENTION_MS_CONFIG, "2592000000")
                .build();
    }
}
