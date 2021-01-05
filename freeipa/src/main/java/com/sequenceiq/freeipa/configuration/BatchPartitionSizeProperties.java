package com.sequenceiq.freeipa.configuration;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "freeipa.batch.partitionsize")
public class BatchPartitionSizeProperties {

    private Integer defaultSize;

    private Map<String, Integer> operations;

    public Integer getDefaultSize() {
        return defaultSize;
    }

    public void setDefaultSize(Integer defaultSize) {
        this.defaultSize = defaultSize;
    }

    public Map<String, Integer> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, Integer> operations) {
        this.operations = operations;
    }

    public Integer getByOperation(String operation) {
        if (operations.containsKey(operation)) {
            return operations.get(operation);
        }
        return defaultSize;
    }
}
