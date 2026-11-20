package cl.duoc.mssmartrentbff.dto;

import java.util.Map;

public class KafkaTopicConfigUpdateDto {

    private Integer partitions;
    private Map<String, String> configs;

    public KafkaTopicConfigUpdateDto() {
    }

    public KafkaTopicConfigUpdateDto(Integer partitions, Map<String, String> configs) {
        this.partitions = partitions;
        this.configs = configs;
    }

    public Integer getPartitions() {
        return partitions;
    }

    public void setPartitions(Integer partitions) {
        this.partitions = partitions;
    }

    public Map<String, String> getConfigs() {
        return configs;
    }

    public void setConfigs(Map<String, String> configs) {
        this.configs = configs;
    }
}
