package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StackDetailsV4Response implements JsonEntity {
    @JsonProperty
    private String version;

    @JsonProperty
    private AmbariStackRepoDetailsV4Response repository;

    private Map<String, List<ManagementPackV4Entry>> mpacks = new HashMap<>();

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public AmbariStackRepoDetailsV4Response getRepository() {
        return repository;
    }

    public void setRepository(AmbariStackRepoDetailsV4Response repository) {
        this.repository = repository;
    }

    public Map<String, List<ManagementPackV4Entry>> getMpacks() {
        return mpacks;
    }

    public void setMpacks(Map<String, List<ManagementPackV4Entry>> mpacks) {
        this.mpacks = mpacks;
    }
}
