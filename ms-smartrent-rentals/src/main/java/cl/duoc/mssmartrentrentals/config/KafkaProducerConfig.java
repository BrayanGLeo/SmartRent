package cl.duoc.mssmartrentrentals.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaProducerConfig {

    @Value("${smartrent.kafka.topics.rentals:rentals.events}")
    private String rentalsTopic;

    @Value("${smartrent.kafka.topics.rentals-dlt:rentals.events-dlt}")
    private String rentalsDltTopic;

    @Value("${smartrent.kafka.topics.partitions:3}")
    private int partitions;

    @Value("${smartrent.kafka.topics.replicas:3}")
    private int replicas;

    @Value("${smartrent.kafka.topics.rentals-retention-ms:604800000}")
    private String rentalsRetentionMs;

    @Value("${smartrent.kafka.topics.rentals-cleanup-policy:delete}")
    private String rentalsCleanupPolicy;

    @Bean
    public NewTopic rentalsEventsTopic() {
        return TopicBuilder.name(rentalsTopic)
                .partitions(partitions)
                .replicas(replicas)
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, rentalsCleanupPolicy)
                .config(TopicConfig.RETENTION_MS_CONFIG, rentalsRetentionMs)
                .config(TopicConfig.RETENTION_BYTES_CONFIG, "1073741824")
                .build();
    }

    @Bean
    public NewTopic rentalsEventsDltTopic() {
        return TopicBuilder.name(rentalsDltTopic)
                .partitions(partitions)
                .replicas(replicas)
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .config(TopicConfig.RETENTION_MS_CONFIG, "2592000000")
                .build();
    }
}
