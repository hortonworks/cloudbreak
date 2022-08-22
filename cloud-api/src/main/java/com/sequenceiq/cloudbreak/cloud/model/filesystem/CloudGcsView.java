package com.sequenceiq.cloudbreak.cloud.model.filesystem;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.common.model.CloudIdentityType;

public class CloudGcsView extends CloudFileSystemView {

    private String serviceAccountEmail;

    @JsonCreator
    public CloudGcsView(@JsonProperty("cloudIdentityType") CloudIdentityType cloudIdentityType) {
        super(cloudIdentityType);
    }

    public String getServiceAccountEmail() {
        return serviceAccountEmail;
    }

    public void setServiceAccountEmail(String serviceAccountEmail) {
        this.serviceAccountEmail = serviceAccountEmail;
    }
}
