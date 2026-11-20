package cl.duoc.mssmartrentbff.services;

import cl.duoc.mssmartrentbff.dto.KafkaTopicConfigUpdateDto;
import cl.duoc.mssmartrentbff.dto.KafkaTopicRequestDto;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigsResult;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.CreatePartitionsResult;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DeleteConsumerGroupsResult;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.clients.admin.DescribeConsumerGroupsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.MemberAssignment;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaAdminServiceTest {

    @Mock
    private AdminClient adminClient;

    private KafkaAdminService service;

    private final Node n1 = new Node(1, "kafka-1", 29092, "az-1");
    private final Node n2 = new Node(2, "kafka-2", 29092, "az-2");
    private final Node n3 = new Node(3, "kafka-3", 29092, "az-3");

    @BeforeEach
    void setUp() {
        service = new KafkaAdminService(() -> adminClient);
    }

    @Test
    void constructorWithBootstrapServers_ShouldInstantiateCleanly() {
        KafkaAdminService realConfigService = new KafkaAdminService("localhost:9092", 5000, 8000);
        assertNotNull(realConfigService);
    }

    @Test
    void getClusterInfo_ShouldReturnClusterAndSortedBrokers() {
        DescribeClusterResult clusterResult = mock(DescribeClusterResult.class);
        when(adminClient.describeCluster()).thenReturn(clusterResult);
        when(clusterResult.clusterId()).thenReturn(KafkaFuture.completedFuture("smartrent-cluster-id"));
        when(clusterResult.controller()).thenReturn(KafkaFuture.completedFuture(n1));
        when(clusterResult.nodes()).thenReturn(KafkaFuture.completedFuture(List.of(n3, n1, n2)));

        Map<String, Object> info = service.getClusterInfo();

        assertEquals("smartrent-cluster-id", info.get("clusterId"));
        assertEquals(3, info.get("brokerCount"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> brokers = (List<Map<String, Object>>) info.get("brokers");
        assertEquals(1, brokers.get(0).get("id"));
        assertEquals(3, brokers.get(2).get("id"));
    }

    @Test
    void listTopics_ShouldReturnTopicsWithPartitionsReplicasAndConfigs() {
        ListTopicsResult listTopicsResult = mock(ListTopicsResult.class);
        when(adminClient.listTopics(any(ListTopicsOptions.class))).thenReturn(listTopicsResult);
        when(listTopicsResult.names()).thenReturn(KafkaFuture.completedFuture(Set.of("rentals.events", "catalog.events")));

        TopicPartitionInfo p0 = new TopicPartitionInfo(0, n1, List.of(n1, n2, n3), List.of(n1, n2, n3));
        TopicPartitionInfo p1 = new TopicPartitionInfo(1, n2, List.of(n1, n2, n3), List.of(n1, n2)); // 1 under-replicated
        TopicDescription rentalsDesc = new TopicDescription("rentals.events", false, List.of(p0, p1));
        TopicDescription catalogDesc = new TopicDescription("catalog.events", false, List.of(p0));

        DescribeTopicsResult describeTopicsResult = mock(DescribeTopicsResult.class);
        when(adminClient.describeTopics(anyCollection())).thenReturn(describeTopicsResult);
        when(describeTopicsResult.allTopicNames()).thenReturn(
                KafkaFuture.completedFuture(Map.of("rentals.events", rentalsDesc, "catalog.events", catalogDesc)));

        Config rentalsCfg = new Config(List.of(
                new ConfigEntry("cleanup.policy", "delete"),
                new ConfigEntry("retention.ms", "604800000"),
                new ConfigEntry("min.insync.replicas", "2")
        ));
        Config catalogCfg = new Config(List.of(new ConfigEntry("cleanup.policy", "compact,delete")));
        DescribeConfigsResult describeConfigsResult = mock(DescribeConfigsResult.class);
        when(adminClient.describeConfigs(anyCollection())).thenReturn(describeConfigsResult);
        when(describeConfigsResult.all()).thenReturn(KafkaFuture.completedFuture(Map.of(
                new ConfigResource(ConfigResource.Type.TOPIC, "rentals.events"), rentalsCfg,
                new ConfigResource(ConfigResource.Type.TOPIC, "catalog.events"), catalogCfg
        )));

        List<Map<String, Object>> topics = service.listTopics(false);
        assertEquals(2, topics.size());
        assertEquals("catalog.events", topics.get(0).get("name"));
        assertEquals("rentals.events", topics.get(1).get("name"));
        assertEquals(1L, topics.get(1).get("underReplicatedPartitions"));

        when(listTopicsResult.names()).thenReturn(KafkaFuture.completedFuture(Set.of()));
        assertTrue(service.listTopics(true).isEmpty());
    }

    @Test
    void describeTopic_ShouldReturnPartitionDetailsOffsetsAndAllConfigs() {
        TopicPartitionInfo p0 = new TopicPartitionInfo(0, n1, List.of(n1, n2, n3), List.of(n1, n2, n3));
        TopicDescription desc = new TopicDescription("rentals.events", false, List.of(p0));

        DescribeTopicsResult descRes = mock(DescribeTopicsResult.class);
        when(adminClient.describeTopics(List.of("rentals.events"))).thenReturn(descRes);
        when(descRes.allTopicNames()).thenReturn(KafkaFuture.completedFuture(Map.of("rentals.events", desc)));

        ConfigResource res = new ConfigResource(ConfigResource.Type.TOPIC, "rentals.events");
        Config cfg = new Config(List.of(new ConfigEntry("retention.ms", "604800000")));
        DescribeConfigsResult cfgRes = mock(DescribeConfigsResult.class);
        when(adminClient.describeConfigs(List.of(res))).thenReturn(cfgRes);
        when(cfgRes.all()).thenReturn(KafkaFuture.completedFuture(Map.of(res, cfg)));

        TopicPartition tp0 = new TopicPartition("rentals.events", 0);
        ListOffsetsResult.ListOffsetsResultInfo earliest = new ListOffsetsResult.ListOffsetsResultInfo(5L, 0L, Optional.empty());
        ListOffsetsResult.ListOffsetsResultInfo latest = new ListOffsetsResult.ListOffsetsResultInfo(25L, 0L, Optional.empty());
        ListOffsetsResult earliestRes = mock(ListOffsetsResult.class);
        ListOffsetsResult latestRes = mock(ListOffsetsResult.class);
        when(earliestRes.all()).thenReturn(KafkaFuture.completedFuture(Map.of(tp0, earliest)));
        when(latestRes.all()).thenReturn(KafkaFuture.completedFuture(Map.of(tp0, latest)));
        when(adminClient.listOffsets(anyMap())).thenReturn(earliestRes, latestRes);

        Map<String, Object> detail = service.describeTopic("rentals.events");
        assertEquals(1, detail.get("partitions"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> parts = (List<Map<String, Object>>) detail.get("partitionDetails");
        assertEquals(20L, parts.get(0).get("messageCount"));
    }

    @Test
    void createUpdateDeleteTopic_ShouldValidateAndInvokeAdminClient() {
        assertThrows(IllegalArgumentException.class, () -> service.createTopic(null));
        assertThrows(IllegalArgumentException.class, () -> service.createTopic(new KafkaTopicRequestDto("bad name!", 3, (short) 3, null)));
        assertThrows(IllegalArgumentException.class, () -> service.createTopic(new KafkaTopicRequestDto("__internal", 3, (short) 3, null)));
        assertThrows(IllegalArgumentException.class, () -> service.createTopic(new KafkaTopicRequestDto("valid.topic", 0, (short) 3, null)));
        assertThrows(IllegalArgumentException.class, () -> service.createTopic(new KafkaTopicRequestDto("valid.topic", 3, (short) 0, null)));

        CreateTopicsResult createRes = mock(CreateTopicsResult.class);
        when(adminClient.createTopics(anyCollection())).thenReturn(createRes);
        when(createRes.all()).thenReturn(KafkaFuture.completedFuture(null));

        Map<String, Object> created = service.createTopic(
                new KafkaTopicRequestDto("custom.events", 3, (short) 3, Map.of("retention.ms", "86400000")));
        assertEquals("custom.events", created.get("name"));
        assertEquals(3, created.get("partitions"));

        // Update topic config + partition increase
        assertThrows(IllegalArgumentException.class, () -> service.updateTopicConfig("custom.events", null));
        assertThrows(IllegalArgumentException.class, () -> service.updateTopicConfig("custom.events", new KafkaTopicConfigUpdateDto()));

        TopicPartitionInfo p0 = new TopicPartitionInfo(0, n1, List.of(n1), List.of(n1));
        TopicPartitionInfo p1 = new TopicPartitionInfo(1, n2, List.of(n2), List.of(n2));
        TopicDescription desc = new TopicDescription("custom.events", false, List.of(p0, p1));
        DescribeTopicsResult descRes = mock(DescribeTopicsResult.class);
        when(adminClient.describeTopics(List.of("custom.events"))).thenReturn(descRes);
        when(descRes.allTopicNames()).thenReturn(KafkaFuture.completedFuture(Map.of("custom.events", desc)));

        // Cannot reduce partitions
        assertThrows(IllegalArgumentException.class,
                () -> service.updateTopicConfig("custom.events", new KafkaTopicConfigUpdateDto(1, null)));

        CreatePartitionsResult createPartRes = mock(CreatePartitionsResult.class);
        when(adminClient.createPartitions(anyMap())).thenReturn(createPartRes);
        when(createPartRes.all()).thenReturn(KafkaFuture.completedFuture(null));

        AlterConfigsResult alterRes = mock(AlterConfigsResult.class);
        when(adminClient.incrementalAlterConfigs(anyMap())).thenReturn(alterRes);
        when(alterRes.all()).thenReturn(KafkaFuture.completedFuture(null));

        DescribeConfigsResult cfgRes = mock(DescribeConfigsResult.class);
        when(adminClient.describeConfigs(anyCollection())).thenReturn(cfgRes);
        when(cfgRes.all()).thenReturn(KafkaFuture.completedFuture(Map.of(
                new ConfigResource(ConfigResource.Type.TOPIC, "custom.events"),
                new Config(List.of(new ConfigEntry("retention.ms", "172800000")))
        )));
        ListOffsetsResult listOffRes = mock(ListOffsetsResult.class);
        when(listOffRes.all()).thenReturn(KafkaFuture.completedFuture(Map.of()));
        when(adminClient.listOffsets(anyMap())).thenReturn(listOffRes);

        Map<String, Object> updated = service.updateTopicConfig(
                "custom.events",
                new KafkaTopicConfigUpdateDto(4, Map.of("retention.ms", "172800000")));
        assertEquals("custom.events", updated.get("name"));

        // Delete topic
        assertThrows(IllegalArgumentException.class, () -> service.deleteTopic("__consumer_offsets"));
        DeleteTopicsResult delRes = mock(DeleteTopicsResult.class);
        when(adminClient.deleteTopics(List.of("custom.events"))).thenReturn(delRes);
        when(delRes.all()).thenReturn(KafkaFuture.completedFuture(null));
        service.deleteTopic("custom.events");
        verify(adminClient).deleteTopics(List.of("custom.events"));
    }

    @Test
    void consumerGroupsOperations_ShouldListDescribeWithLagAndDelete() {
        ConsumerGroupListing g1 = mock(ConsumerGroupListing.class);
        when(g1.groupId()).thenReturn("report-group");
        when(g1.state()).thenReturn(Optional.of(ConsumerGroupState.STABLE));
        when(g1.isSimpleConsumerGroup()).thenReturn(false);

        ConsumerGroupListing g2 = mock(ConsumerGroupListing.class);
        when(g2.groupId()).thenReturn("audit-group");
        when(g2.state()).thenReturn(Optional.empty());
        when(g2.isSimpleConsumerGroup()).thenReturn(false);

        ListConsumerGroupsResult listRes = mock(ListConsumerGroupsResult.class);
        when(adminClient.listConsumerGroups()).thenReturn(listRes);
        when(listRes.all()).thenReturn(KafkaFuture.completedFuture(List.of(g1, g2)));

        List<Map<String, Object>> groups = service.listConsumerGroups();
        assertEquals(2, groups.size());
        assertEquals("audit-group", groups.get(0).get("groupId"));
        assertEquals("UNKNOWN", groups.get(0).get("state"));

        // Describe consumer group with lag
        assertThrows(IllegalArgumentException.class, () -> service.describeConsumerGroup(" "));
        MemberAssignment assignment = new MemberAssignment(Set.of(new TopicPartition("rentals.events", 0)));
        MemberDescription member = new MemberDescription("c-1", "client-1", "/10.0.0.1", assignment);
        ConsumerGroupDescription cgDesc = new ConsumerGroupDescription(
                "audit-group", false, List.of(member), "range", ConsumerGroupState.STABLE, n1);

        DescribeConsumerGroupsResult descCgRes = mock(DescribeConsumerGroupsResult.class);
        when(adminClient.describeConsumerGroups(List.of("audit-group"))).thenReturn(descCgRes);
        when(descCgRes.all()).thenReturn(KafkaFuture.completedFuture(Map.of("audit-group", cgDesc)));

        TopicPartition tp0 = new TopicPartition("rentals.events", 0);
        ListConsumerGroupOffsetsResult cgOffsetsRes = mock(ListConsumerGroupOffsetsResult.class);
        when(adminClient.listConsumerGroupOffsets("audit-group")).thenReturn(cgOffsetsRes);
        when(cgOffsetsRes.partitionsToOffsetAndMetadata())
                .thenReturn(KafkaFuture.completedFuture(Map.of(tp0, new OffsetAndMetadata(12L))));

        ListOffsetsResult endOffsetsRes = mock(ListOffsetsResult.class);
        when(adminClient.listOffsets(anyMap())).thenReturn(endOffsetsRes);
        when(endOffsetsRes.all()).thenReturn(KafkaFuture.completedFuture(
                Map.of(tp0, new ListOffsetsResult.ListOffsetsResultInfo(15L, 0L, Optional.empty()))));

        Map<String, Object> cgDetail = service.describeConsumerGroup("audit-group");
        assertEquals(1, cgDetail.get("memberCount"));
        assertEquals(3L, cgDetail.get("totalLag"));

        // Delete consumer group
        assertThrows(IllegalArgumentException.class, () -> service.deleteConsumerGroup(null));
        DeleteConsumerGroupsResult delCgRes = mock(DeleteConsumerGroupsResult.class);
        when(adminClient.deleteConsumerGroups(List.of("audit-group"))).thenReturn(delCgRes);
        when(delCgRes.all()).thenReturn(KafkaFuture.completedFuture(null));
        service.deleteConsumerGroup("audit-group");
        verify(adminClient).deleteConsumerGroups(List.of("audit-group"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void awaitFuture_ShouldPropagateExecutionAndInterruptedExceptions() throws Exception {
        DescribeClusterResult clusterResult = mock(DescribeClusterResult.class);
        KafkaFuture<String> failingFuture = mock(KafkaFuture.class);
        when(failingFuture.get()).thenThrow(new ExecutionException(new UnknownTopicOrPartitionException("Not found")));
        when(adminClient.describeCluster()).thenReturn(clusterResult);
        when(clusterResult.clusterId()).thenReturn(failingFuture);

        assertThrows(UnknownTopicOrPartitionException.class, () -> service.getClusterInfo());

        KafkaFuture<String> interruptedFuture = mock(KafkaFuture.class);
        when(interruptedFuture.get()).thenThrow(new InterruptedException("Interrupted"));
        when(clusterResult.clusterId()).thenReturn(interruptedFuture);

        assertThrows(IllegalStateException.class, () -> service.getClusterInfo());
        assertTrue(Thread.interrupted()); // clears interrupt flag

        KafkaFuture<String> checkedExFuture = mock(KafkaFuture.class);
        when(checkedExFuture.get()).thenThrow(new ExecutionException(new Exception("Checked broker error")));
        when(clusterResult.clusterId()).thenReturn(checkedExFuture);
        assertThrows(IllegalStateException.class, () -> service.getClusterInfo());
    }

    @Test
    void edgeBranches_NullControllerEmptyTopicsNullConfigsAndCheckedExceptions_ShouldBeCovered() {
        // Cluster with null controller
        DescribeClusterResult clusterResult = mock(DescribeClusterResult.class);
        when(adminClient.describeCluster()).thenReturn(clusterResult);
        when(clusterResult.clusterId()).thenReturn(KafkaFuture.completedFuture("id-2"));
        when(clusterResult.controller()).thenReturn(KafkaFuture.completedFuture(null));
        when(clusterResult.nodes()).thenReturn(KafkaFuture.completedFuture(List.of(n1)));
        Map<String, Object> info = service.getClusterInfo();
        assertEquals(1, info.get("brokerCount"));

        // listTopics null names & listConsumerGroups empty
        ListTopicsResult listTopicsResult = mock(ListTopicsResult.class);
        when(adminClient.listTopics(any(ListTopicsOptions.class))).thenReturn(listTopicsResult);
        when(listTopicsResult.names()).thenReturn(KafkaFuture.completedFuture(null));
        assertTrue(service.listTopics(false).isEmpty());

        ListConsumerGroupsResult listCgRes = mock(ListConsumerGroupsResult.class);
        when(adminClient.listConsumerGroups()).thenReturn(listCgRes);
        when(listCgRes.all()).thenReturn(KafkaFuture.completedFuture(List.of()));
        assertTrue(service.listConsumerGroups().isEmpty());
        when(listCgRes.all()).thenReturn(KafkaFuture.completedFuture(null));
        assertTrue(service.listConsumerGroups().isEmpty());

        // createTopic with null partitions/replication/configs & blank topic name
        assertThrows(IllegalArgumentException.class, () -> service.createTopic(new KafkaTopicRequestDto("  ", 3, (short) 3, null)));
        assertThrows(IllegalArgumentException.class, () -> service.createTopic(new KafkaTopicRequestDto(null, 3, (short) 3, null)));
        CreateTopicsResult createRes = mock(CreateTopicsResult.class);
        when(adminClient.createTopics(anyCollection())).thenReturn(createRes);
        when(createRes.all()).thenReturn(KafkaFuture.completedFuture(null));
        KafkaTopicRequestDto defaultsReq = new KafkaTopicRequestDto("defaults.topic", null, null, Map.of());
        Map<String, Object> created = service.createTopic(defaultsReq);
        assertEquals(3, created.get("partitions"));

        // updateTopicConfig with equal partitions & null/blank config keys + describeTopic empty partitions & null leader
        TopicPartitionInfo pNoLeader = new TopicPartitionInfo(0, null, List.of(), List.of());
        TopicDescription descNoLeader = new TopicDescription("defaults.topic", true, List.of(pNoLeader));
        DescribeTopicsResult descRes = mock(DescribeTopicsResult.class);
        when(adminClient.describeTopics(List.of("defaults.topic"))).thenReturn(descRes);
        when(descRes.allTopicNames()).thenReturn(KafkaFuture.completedFuture(Map.of("defaults.topic", descNoLeader)));

        DescribeConfigsResult cfgRes = mock(DescribeConfigsResult.class);
        when(adminClient.describeConfigs(anyCollection())).thenReturn(cfgRes);
        when(cfgRes.all()).thenReturn(KafkaFuture.completedFuture(Map.of()));
        ListOffsetsResult offRes = mock(ListOffsetsResult.class);
        when(offRes.all()).thenReturn(KafkaFuture.completedFuture(Map.of()));
        when(adminClient.listOffsets(anyMap())).thenReturn(offRes);

        Map<String, String> mixedConfigs = new java.util.HashMap<>();
        mixedConfigs.put(" ", "ignored");
        mixedConfigs.put("valid.key", null);
        service.updateTopicConfig("defaults.topic", new KafkaTopicConfigUpdateDto(1, mixedConfigs));

        // Consumer group with empty offsets, null coordinator, null state, null assignment
        assertThrows(IllegalArgumentException.class, () -> service.describeConsumerGroup(null));
        assertThrows(IllegalArgumentException.class, () -> service.deleteConsumerGroup(" "));

        MemberDescription noAssignMember = new MemberDescription("c-2", "client-2", "/10.0.0.2", null);
        ConsumerGroupDescription cgDesc = new ConsumerGroupDescription(
                "empty-group", true, List.of(noAssignMember), "range", null, null);
        DescribeConsumerGroupsResult cgRes = mock(DescribeConsumerGroupsResult.class);
        when(adminClient.describeConsumerGroups(List.of("empty-group"))).thenReturn(cgRes);
        when(cgRes.all()).thenReturn(KafkaFuture.completedFuture(Map.of("empty-group", cgDesc)));

        ListConsumerGroupOffsetsResult cgOffRes = mock(ListConsumerGroupOffsetsResult.class);
        when(adminClient.listConsumerGroupOffsets("empty-group")).thenReturn(cgOffRes);
        when(cgOffRes.partitionsToOffsetAndMetadata()).thenReturn(KafkaFuture.completedFuture(null));

        Map<String, Object> cgInfo = service.describeConsumerGroup("empty-group");
        assertEquals("UNKNOWN", cgInfo.get("state"));
        assertEquals(0L, cgInfo.get("totalLag"));
    }
}
