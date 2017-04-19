package com.sequenceiq.cloudbreak.orchestrator.model;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
public class ContainerConfig {

    private final String name;

    private final String version;

    private final String queue;

    public ContainerConfig(@JsonProperty("name") String name, @JsonProperty("version") String version, @JsonProperty("queue") String queue) {
        this.name = name;
        this.version = version;
        this.queue = queue;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getQueue() {
        return queue;
    }

    @Override
    public String toString() {
        return "ContainerConfig{"
                + "name='" + name + '\''
                + ", version='" + version + '\''
                + ", queue='" + queue + '\''
                + '}';
    }
}
