package com.sequenceiq.cloudbreak.cloud.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConfigsSpecification {
    @JsonProperty("configs")
    private List<ConfigSpecification> configs;

    @JsonProperty("properties")
    private PropertySpecification properties;

    public ConfigsSpecification() {
    }

    public List<ConfigSpecification> getConfigs() {
        return configs;
    }

    public PropertySpecification getProperties() {
        return properties;
    }

    public void setProperties(PropertySpecification properties) {
        this.properties = properties;
    }

    public void setConfigs(List<ConfigSpecification> configs) {
        this.configs = configs;
    }
}
