package com.sequenceiq.cloudbreak.api.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class S3FileSystemConfiguration implements JsonEntity {

    @NotNull
    private String instanceProfile;

    public String getInstanceProfile() {
        return instanceProfile;
    }

    public void setInstanceProfile(String instanceProfile) {
        this.instanceProfile = instanceProfile;
    }
}
