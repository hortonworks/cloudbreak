package com.sequenceiq.cloudbreak.cloud.model.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StackDetails {

    @JsonProperty("version")
    private String version;

    @JsonProperty("repo")
    private StackRepoDetails repo;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public StackRepoDetails getRepo() {
        return repo;
    }

    public void setRepo(StackRepoDetails repo) {
        this.repo = repo;
    }
}
