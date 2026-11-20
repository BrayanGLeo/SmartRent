package cl.duoc.mssmartrentbff.dto;

import java.util.Map;

public class KafkaTopicRequestDto {

    private String name;
    private Integer partitions = 3;
    private Short replicationFactor = 3;
    private Map<String, String> configs;

    public KafkaTopicRequestDto() {
    }

    public KafkaTopicRequestDto(String name, Integer partitions, Short replicationFactor, Map<String, String> configs) {
        this.name = name;
        this.partitions = partitions;
        this.replicationFactor = replicationFactor;
        this.configs = configs;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getPartitions() {
        return partitions;
    }

    public void setPartitions(Integer partitions) {
        this.partitions = partitions;
    }

    public Short getReplicationFactor() {
        return replicationFactor;
    }

    public void setReplicationFactor(Short replicationFactor) {
        this.replicationFactor = replicationFactor;
    }

    public Map<String, String> getConfigs() {
        return configs;
    }

    public void setConfigs(Map<String, String> configs) {
        this.configs = configs;
    }
}
