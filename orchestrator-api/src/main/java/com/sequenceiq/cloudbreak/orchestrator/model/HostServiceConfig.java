package com.sequenceiq.cloudbreak.orchestrator.model;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
public class HostServiceConfig {

    private final String name;

    private final String version;

    private final String repoUrl;

    public HostServiceConfig(@JsonProperty("name") String name, @JsonProperty("version") String version, @JsonProperty("repoUrl") String repoUrl) {
        this.name = name;
        this.version = version;
        this.repoUrl =repoUrl;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    @Override
    public String toString() {
        return "ContainerConfig{"
                + "name='" + name + '\''
                + ", version='" + version + '\''
                + '}';
    }
}
