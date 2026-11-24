package cl.duoc.mssmartrentcatalog.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.util.Objects;

@Configuration
public class KafkaProducerConfig {

    @Value("${smartrent.kafka.topics.catalog:catalog.events}")
    private String catalogTopic;

    @Value("${smartrent.kafka.topics.catalog-dlt:catalog.events-dlt}")
    private String catalogDltTopic;

    @Value("${smartrent.kafka.topics.partitions:3}")
    private int partitions;

    @Value("${smartrent.kafka.topics.replicas:3}")
    private int replicas;

    @Value("${smartrent.kafka.topics.catalog-retention-ms:1209600000}")
    private String catalogRetentionMs;

    @Value("${smartrent.kafka.topics.catalog-cleanup-policy:compact,delete}")
    private String catalogCleanupPolicy;

    @Bean
    public NewTopic catalogEventsTopic() {
        return TopicBuilder.name(Objects.requireNonNull(catalogTopic))
                .partitions(partitions)
                .replicas(replicas)
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, Objects.requireNonNull(catalogCleanupPolicy))
                .config(TopicConfig.RETENTION_MS_CONFIG, Objects.requireNonNull(catalogRetentionMs))
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
