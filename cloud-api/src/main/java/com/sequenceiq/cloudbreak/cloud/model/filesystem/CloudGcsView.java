package com.sequenceiq.cloudbreak.cloud.model.filesystem;

import com.sequenceiq.common.model.CloudIdentityType;

public class CloudGcsView extends CloudFileSystemView {

    private String serviceAccountEmail;

    public CloudGcsView(CloudIdentityType cloudIdentityType) {
        super(cloudIdentityType);
    }

    public String getServiceAccountEmail() {
        return serviceAccountEmail;
    }

    public void setServiceAccountEmail(String serviceAccountEmail) {
        this.serviceAccountEmail = serviceAccountEmail;
    }
}
