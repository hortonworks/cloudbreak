package com.sequenceiq.cloudbreak.cloud.model.catalog;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StackDetails {

    private final String version;

    private final StackRepoDetails repo;

    @JsonCreator
    public StackDetails(
            @JsonProperty(value = "version", required = true) String version,
            @JsonProperty(value = "repo", required = true) StackRepoDetails repo) {
        this.version = version;
        this.repo = repo;
    }

    public String getVersion() {
        return version;
    }

    public StackRepoDetails getRepo() {
        return repo;
    }
}
