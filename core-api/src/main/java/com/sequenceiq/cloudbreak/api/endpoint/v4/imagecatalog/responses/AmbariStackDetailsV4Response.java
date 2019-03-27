package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AmbariStackDetailsV4Response extends BaseStackDetailsV4Response implements JsonEntity {

    @JsonProperty
    private AmbariStackRepoDetailsV4Response repository;

    private Map<String, List<ManagementPackV4Entry>> mpacks;

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
