package cl.duoc.mssmartrentbff.services;

import cl.duoc.mssmartrentbff.dto.KafkaTopicConfigUpdateDto;
import cl.duoc.mssmartrentbff.dto.KafkaTopicRequestDto;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@Service
public class KafkaAdminService {

    private static final Pattern VALID_TOPIC_NAME = Pattern.compile("^[a-zA-Z0-9._-]+$");
    private static final List<String> KEY_CONFIG_NAMES = List.of(
            "cleanup.policy", "retention.ms", "retention.bytes", "min.insync.replicas", "segment.ms"
    );

    private final Supplier<AdminClient> adminClientSupplier;

    @Autowired
    public KafkaAdminService(
            @Value("${spring.kafka.bootstrap-servers:localhost:9092,localhost:9093,localhost:9094}") String bootstrapServers,
            @Value("${smartrent.kafka.admin.request-timeout-ms:5000}") int requestTimeoutMs,
            @Value("${smartrent.kafka.admin.default-api-timeout-ms:8000}") int defaultApiTimeoutMs) {
        this(() -> AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, requestTimeoutMs,
                AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, defaultApiTimeoutMs
        )));
    }

    public KafkaAdminService(Supplier<AdminClient> adminClientSupplier) {
        this.adminClientSupplier = adminClientSupplier;
    }

    public Map<String, Object> getClusterInfo() {
        try (AdminClient admin = adminClientSupplier.get()) {
            DescribeClusterResult cluster = admin.describeCluster();
            String clusterId = awaitFuture(cluster.clusterId());
            Node controller = awaitFuture(cluster.controller());
            Collection<Node> nodes = awaitFuture(cluster.nodes());

            List<Map<String, Object>> brokers = nodes.stream()
                    .sorted(Comparator.comparingInt(Node::id))
                    .map(this::nodeToMap)
                    .toList();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("clusterId", clusterId);
            result.put("controller", controller != null ? nodeToMap(controller) : null);
            result.put("brokerCount", brokers.size());
            result.put("brokers", brokers);
            return result;
        }
    }

    public List<Map<String, Object>> listTopics(boolean includeInternal) {
        try (AdminClient admin = adminClientSupplier.get()) {
            Set<String> names = awaitFuture(admin.listTopics(new ListTopicsOptions().listInternal(includeInternal)).names());
            if (names == null || names.isEmpty()) {
                return List.of();
            }

            List<String> sortedNames = names.stream().sorted().toList();
            Map<String, TopicDescription> descriptions = awaitFuture(admin.describeTopics(sortedNames).allTopicNames());
            List<ConfigResource> resources = sortedNames.stream()
                    .map(n -> new ConfigResource(ConfigResource.Type.TOPIC, n))
                    .toList();
            Map<ConfigResource, Config> configs = awaitFuture(admin.describeConfigs(resources).all());

            List<Map<String, Object>> topics = new ArrayList<>();
            for (String name : sortedNames) {
                TopicDescription desc = descriptions.get(name);
                Config cfg = configs.get(new ConfigResource(ConfigResource.Type.TOPIC, name));
                topics.add(buildTopicSummary(name, desc, cfg));
            }
            return topics;
        }
    }

    public Map<String, Object> describeTopic(String topicName) {
        validateTopicName(topicName);
        try (AdminClient admin = adminClientSupplier.get()) {
            Map<String, TopicDescription> descriptions = awaitFuture(
                    admin.describeTopics(List.of(topicName)).allTopicNames()
            );
            TopicDescription desc = descriptions.get(topicName);
            ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);
            Map<ConfigResource, Config> cfgMap = awaitFuture(admin.describeConfigs(List.of(resource)).all());
            Config cfg = cfgMap.get(resource);

            Map<TopicPartition, OffsetSpec> earliestReq = new HashMap<>();
            Map<TopicPartition, OffsetSpec> latestReq = new HashMap<>();
            for (TopicPartitionInfo pInfo : desc.partitions()) {
                TopicPartition tp = new TopicPartition(topicName, pInfo.partition());
                earliestReq.put(tp, OffsetSpec.earliest());
                latestReq.put(tp, OffsetSpec.latest());
            }

            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> earliestOffsets =
                    earliestReq.isEmpty() ? Map.of() : awaitFuture(admin.listOffsets(earliestReq).all());
            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> latestOffsets =
                    latestReq.isEmpty() ? Map.of() : awaitFuture(admin.listOffsets(latestReq).all());

            List<Map<String, Object>> partitionDetails = new ArrayList<>();
            for (TopicPartitionInfo pInfo : desc.partitions()) {
                TopicPartition tp = new TopicPartition(topicName, pInfo.partition());
                long start = earliestOffsets.containsKey(tp) ? earliestOffsets.get(tp).offset() : 0L;
                long end = latestOffsets.containsKey(tp) ? latestOffsets.get(tp).offset() : 0L;

                Map<String, Object> partMap = new LinkedHashMap<>();
                partMap.put("partition", pInfo.partition());
                partMap.put("leader", pInfo.leader() != null ? pInfo.leader().id() : null);
                partMap.put("replicas", pInfo.replicas().stream().map(Node::id).toList());
                partMap.put("isr", pInfo.isr().stream().map(Node::id).toList());
                partMap.put("earliestOffset", start);
                partMap.put("latestOffset", end);
                partMap.put("messageCount", Math.max(0L, end - start));
                partitionDetails.add(partMap);
            }

            Map<String, Object> result = buildTopicSummary(topicName, desc, cfg);
            result.put("partitionDetails", partitionDetails);
            result.put("allConfigs", extractAllConfigs(cfg));
            return result;
        }
    }

    public Map<String, Object> createTopic(KafkaTopicRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("El cuerpo de solicitud del tópico es obligatorio");
        }
        String name = request.getName() != null ? request.getName().trim() : null;
        validateTopicName(name);
        if (name.startsWith("__")) {
            throw new IllegalArgumentException("No se permite crear tópicos internos con prefijo __");
        }

        int partitions = request.getPartitions() != null ? request.getPartitions() : 3;
        short replication = request.getReplicationFactor() != null ? request.getReplicationFactor() : (short) 3;
        if (partitions < 1 || replication < 1) {
            throw new IllegalArgumentException("Particiones y factor de replicación deben ser mayores o iguales a 1");
        }

        NewTopic newTopic = new NewTopic(name, partitions, replication);
        if (request.getConfigs() != null && !request.getConfigs().isEmpty()) {
            newTopic.configs(request.getConfigs());
        }

        try (AdminClient admin = adminClientSupplier.get()) {
            awaitFuture(admin.createTopics(List.of(newTopic)).all());
        }

        Map<String, Object> created = new LinkedHashMap<>();
        created.put("name", name);
        created.put("partitions", partitions);
        created.put("replicationFactor", (int) replication);
        created.put("configs", request.getConfigs() != null ? request.getConfigs() : Map.of());
        return created;
    }

    public Map<String, Object> updateTopicConfig(String topicName, KafkaTopicConfigUpdateDto request) {
        validateTopicName(topicName);
        if (request == null) {
            throw new IllegalArgumentException("El cuerpo de actualización del tópico es obligatorio");
        }
        boolean hasConfigs = request.getConfigs() != null && !request.getConfigs().isEmpty();
        boolean hasPartitions = request.getPartitions() != null;
        if (!hasConfigs && !hasPartitions) {
            throw new IllegalArgumentException("Debe especificar configuraciones o un nuevo número de particiones");
        }

        try (AdminClient admin = adminClientSupplier.get()) {
            Map<String, TopicDescription> existing = awaitFuture(
                    admin.describeTopics(List.of(topicName)).allTopicNames()
            );
            TopicDescription currentDesc = existing.get(topicName);

            if (hasPartitions) {
                int currentPartitions = currentDesc.partitions().size();
                int targetPartitions = request.getPartitions();
                if (targetPartitions < currentPartitions) {
                    throw new IllegalArgumentException(
                            "Kafka no permite reducir particiones (actual=" + currentPartitions + ", solicitado=" + targetPartitions + ")");
                }
                if (targetPartitions > currentPartitions) {
                    awaitFuture(admin.createPartitions(Map.of(topicName, NewPartitions.increaseTo(targetPartitions))).all());
                }
            }

            if (hasConfigs) {
                ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);
                List<AlterConfigOp> ops = request.getConfigs().entrySet().stream()
                        .filter(e -> e.getKey() != null && !e.getKey().isBlank() && e.getValue() != null)
                        .map(e -> new AlterConfigOp(new ConfigEntry(e.getKey().trim(), e.getValue()), AlterConfigOp.OpType.SET))
                        .toList();
                if (!ops.isEmpty()) {
                    awaitFuture(admin.incrementalAlterConfigs(Map.of(resource, ops)).all());
                }
            }
        }
        return describeTopic(topicName);
    }

    public void deleteTopic(String topicName) {
        validateTopicName(topicName);
        if (topicName.startsWith("__")) {
            throw new IllegalArgumentException("No está permitido eliminar tópicos internos del clúster Kafka");
        }
        try (AdminClient admin = adminClientSupplier.get()) {
            awaitFuture(admin.deleteTopics(List.of(topicName)).all());
        }
    }

    public List<Map<String, Object>> listConsumerGroups() {
        try (AdminClient admin = adminClientSupplier.get()) {
            Collection<ConsumerGroupListing> listings = awaitFuture(admin.listConsumerGroups().all());
            if (listings == null || listings.isEmpty()) {
                return List.of();
            }
            return listings.stream()
                    .sorted(Comparator.comparing(ConsumerGroupListing::groupId))
                    .map(listing -> {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("groupId", listing.groupId());
                        map.put("state", listing.state().map(Enum::name).orElse("UNKNOWN"));
                        map.put("isSimpleConsumerGroup", listing.isSimpleConsumerGroup());
                        return map;
                    })
                    .toList();
        }
    }

    public Map<String, Object> describeConsumerGroup(String groupId) {
        if (groupId == null || groupId.isBlank()) {
            throw new IllegalArgumentException("El identificador del consumer group es obligatorio");
        }
        try (AdminClient admin = adminClientSupplier.get()) {
            Map<String, ConsumerGroupDescription> descMap = awaitFuture(
                    admin.describeConsumerGroups(List.of(groupId)).all()
            );
            ConsumerGroupDescription desc = descMap.get(groupId);

            Map<TopicPartition, OffsetAndMetadata> currentOffsets = awaitFuture(
                    admin.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata()
            );

            Map<TopicPartition, OffsetSpec> latestQuery = new HashMap<>();
            if (currentOffsets != null) {
                currentOffsets.keySet().forEach(tp -> latestQuery.put(tp, OffsetSpec.latest()));
            }
            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> endOffsets =
                    latestQuery.isEmpty() ? Map.of() : awaitFuture(admin.listOffsets(latestQuery).all());

            List<Map<String, Object>> partitionOffsets = new ArrayList<>();
            long totalLag = 0L;
            if (currentOffsets != null) {
                List<TopicPartition> sortedTps = currentOffsets.keySet().stream()
                        .sorted(Comparator.comparing(TopicPartition::topic).thenComparingInt(TopicPartition::partition))
                        .toList();
                for (TopicPartition tp : sortedTps) {
                    long currentOffset = currentOffsets.get(tp) != null ? currentOffsets.get(tp).offset() : 0L;
                    long endOffset = endOffsets.containsKey(tp) ? endOffsets.get(tp).offset() : currentOffset;
                    long lag = Math.max(0L, endOffset - currentOffset);
                    totalLag += lag;

                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("topic", tp.topic());
                    row.put("partition", tp.partition());
                    row.put("currentOffset", currentOffset);
                    row.put("endOffset", endOffset);
                    row.put("lag", lag);
                    partitionOffsets.add(row);
                }
            }

            List<Map<String, Object>> members = desc.members().stream()
                    .map(this::memberToMap)
                    .toList();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("groupId", desc.groupId());
            result.put("state", desc.state() != null ? desc.state().name() : "UNKNOWN");
            result.put("coordinator", desc.coordinator() != null ? nodeToMap(desc.coordinator()) : null);
            result.put("memberCount", members.size());
            result.put("members", members);
            result.put("totalLag", totalLag);
            result.put("offsets", partitionOffsets);
            return result;
        }
    }

    public void deleteConsumerGroup(String groupId) {
        if (groupId == null || groupId.isBlank()) {
            throw new IllegalArgumentException("El identificador del consumer group es obligatorio");
        }
        try (AdminClient admin = adminClientSupplier.get()) {
            awaitFuture(admin.deleteConsumerGroups(List.of(groupId)).all());
        }
    }

    private Map<String, Object> buildTopicSummary(String name, TopicDescription desc, Config cfg) {
        List<TopicPartitionInfo> partInfos = desc != null ? desc.partitions() : List.of();
        int replicationFactor = partInfos.isEmpty() ? 0 : partInfos.get(0).replicas().size();
        long underReplicated = partInfos.stream()
                .filter(p -> p.isr().size() < p.replicas().size())
                .count();

        Map<String, String> keyConfigs = new LinkedHashMap<>();
        if (cfg != null) {
            for (String key : KEY_CONFIG_NAMES) {
                ConfigEntry entry = cfg.get(key);
                if (entry != null && entry.value() != null) {
                    keyConfigs.put(key, entry.value());
                }
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("name", name);
        summary.put("internal", desc != null && desc.isInternal());
        summary.put("partitions", partInfos.size());
        summary.put("replicationFactor", replicationFactor);
        summary.put("underReplicatedPartitions", underReplicated);
        summary.put("configs", keyConfigs);
        return summary;
    }

    private Map<String, String> extractAllConfigs(Config cfg) {
        if (cfg == null) {
            return Collections.emptyMap();
        }
        Map<String, String> map = new LinkedHashMap<>();
        cfg.entries().stream()
                .sorted(Comparator.comparing(ConfigEntry::name))
                .forEach(entry -> map.put(entry.name(), entry.value()));
        return map;
    }

    private Map<String, Object> nodeToMap(Node node) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", node.id());
        map.put("host", node.host());
        map.put("port", node.port());
        map.put("rack", node.rack());
        return map;
    }

    private Map<String, Object> memberToMap(MemberDescription member) {
        List<String> assigned = member.assignment() != null
                ? member.assignment().topicPartitions().stream()
                        .sorted(Comparator.comparing(TopicPartition::topic).thenComparingInt(TopicPartition::partition))
                        .map(tp -> tp.topic() + "-" + tp.partition())
                        .toList()
                : List.of();

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("consumerId", member.consumerId());
        map.put("clientId", member.clientId());
        map.put("host", member.host());
        map.put("assignedPartitions", assigned);
        return map;
    }

    private static void validateTopicName(String topicName) {
        if (topicName == null || topicName.isBlank() || !VALID_TOPIC_NAME.matcher(topicName.trim()).matches()) {
            throw new IllegalArgumentException(
                    "Nombre de tópico inválido: solo se permiten caracteres alfanuméricos, puntos, guiones y guion bajo");
        }
    }

    private static <T> T awaitFuture(KafkaFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operación Kafka interrumpida", ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException runtimeEx) {
                throw runtimeEx;
            }
            throw new IllegalStateException("Error ejecutando operación en Kafka AdminClient", cause);
        }
    }
}
