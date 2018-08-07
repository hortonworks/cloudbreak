package com.sequenceiq.cloudbreak.api.model.imagecatalog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StackDetailsJson implements JsonEntity {
    @JsonProperty("version")
    private String version;

    @JsonProperty("repo")
    private StackRepoDetailsJson repo;

    private Map<String, List<ManagementPackEntry>> mpacks = new HashMap<>();

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public StackRepoDetailsJson getRepo() {
        return repo;
    }

    public void setRepo(StackRepoDetailsJson repo) {
        this.repo = repo;
    }

    public Map<String, List<ManagementPackEntry>> getMpacks() {
        return mpacks;
    }

    public void setMpacks(Map<String, List<ManagementPackEntry>> mpacks) {
        this.mpacks = mpacks;
    }
}
