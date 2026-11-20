package cl.duoc.mssmartrentbff.dto;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class KafkaDtosTest {

    @Test
    void kafkaDtos_ShouldStoreAndExposeFields() {
        KafkaTopicRequestDto req = new KafkaTopicRequestDto();
        assertNull(req.getName());
        assertEquals(3, req.getPartitions());
        assertEquals((short) 3, req.getReplicationFactor());

        req.setName("audit.events");
        req.setPartitions(6);
        req.setReplicationFactor((short) 3);
        req.setConfigs(Map.of("cleanup.policy", "delete"));

        assertEquals("audit.events", req.getName());
        assertEquals(6, req.getPartitions());
        assertEquals((short) 3, req.getReplicationFactor());
        assertEquals("delete", req.getConfigs().get("cleanup.policy"));

        KafkaTopicConfigUpdateDto upd = new KafkaTopicConfigUpdateDto();
        assertNull(upd.getPartitions());
        upd.setPartitions(4);
        upd.setConfigs(Map.of("retention.ms", "86400000"));
        assertEquals(4, upd.getPartitions());
        assertEquals("86400000", upd.getConfigs().get("retention.ms"));
    }
}
