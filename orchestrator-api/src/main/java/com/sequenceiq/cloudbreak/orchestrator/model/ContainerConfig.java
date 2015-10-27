package com.sequenceiq.cloudbreak.orchestrator.model;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
public class ContainerConfig {

    private final String name;

    private final String version;

    public ContainerConfig(@JsonProperty("name") String name, @JsonProperty("version") String version) {
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "ContainerConfig{"
                + "name='" + name + '\''
                + ", version='" + version + '\''
                + '}';
    }
}
