package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StackDetailsV4Response implements JsonEntity {
    @JsonProperty("version")
    private String version;

    @JsonProperty("repo")
    private StackRepoDetailsV4Response repo;

    private Map<String, List<ManagementPackV4Entry>> mpacks = new HashMap<>();

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public StackRepoDetailsV4Response getRepo() {
        return repo;
    }

    public void setRepo(StackRepoDetailsV4Response repo) {
        this.repo = repo;
    }

    public Map<String, List<ManagementPackV4Entry>> getMpacks() {
        return mpacks;
    }

    public void setMpacks(Map<String, List<ManagementPackV4Entry>> mpacks) {
        this.mpacks = mpacks;
    }
}
