package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.common.model.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseStackDetailsV4Response implements JsonEntity {

    @JsonProperty
    private String version;

    @JsonProperty
    private String stackBuildNumber;

    @JsonProperty
    private BaseStackRepoDetailsV4Response repository;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getStackBuildNumber() {
        return stackBuildNumber;
    }

    public void setStackBuildNumber(String stackBuildNumber) {
        this.stackBuildNumber = stackBuildNumber;
    }

    public BaseStackRepoDetailsV4Response getRepository() {
        return repository;
    }

    public void setRepository(BaseStackRepoDetailsV4Response repository) {
        this.repository = repository;
    }

    @Override
    public String toString() {
        return "BaseStackDetailsV4Response{" +
                "version='" + version + '\'' +
                ", stackBuildNumber='" + stackBuildNumber + '\'' +
                ", repository=" + repository +
                '}';
    }
}
