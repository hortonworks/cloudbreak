package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.common.model.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClouderaManagerStackDetailsV4Response extends BaseStackDetailsV4Response implements JsonEntity {

    @JsonProperty
    private ClouderaManagerStackRepoDetailsV4Response repository;

    public ClouderaManagerStackRepoDetailsV4Response getRepository() {
        return repository;
    }

    public void setRepository(ClouderaManagerStackRepoDetailsV4Response repository) {
        this.repository = repository;
    }

    @Override
    public String toString() {
        return "ClouderaManagerStackDetailsV4Response{" +
                "repository=" + repository +
                "} " + super.toString();
    }
}
