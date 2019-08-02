package com.sequenceiq.cloudbreak.cloud.model.filesystem;

import com.sequenceiq.common.model.CloudIdentityType;

public abstract class CloudFileSystemView {

    private final CloudIdentityType cloudIdentityType;

    protected CloudFileSystemView(CloudIdentityType cloudIdentityType) {
        this.cloudIdentityType = cloudIdentityType;
    }

    public CloudIdentityType getCloudIdentityType() {
        return cloudIdentityType;
    }
}
