package com.sequenceiq.cloudbreak.cloud.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MetaSpecification {
    @JsonProperty("configs")
    private List<ConfigSpecification> configSpecification;

    @JsonProperty("properties")
    private PropertySpecification properties;

    public MetaSpecification() {
    }

    public List<ConfigSpecification> getConfigSpecification() {
        return configSpecification;
    }

    public void setConfigSpecification(List<ConfigSpecification> configSpecification) {
        this.configSpecification = configSpecification;
    }

    public PropertySpecification getProperties() {
        return properties;
    }

    public void setProperties(PropertySpecification properties) {
        this.properties = properties;
    }
}
