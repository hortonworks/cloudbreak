package com.sequenceiq.distrox.api.v1.distrox.model;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

public interface CloudPlatformProvider {

    default CloudPlatform getCloudPlatform() {

        if (getAws() != null) {
            return CloudPlatform.AWS;
        } else if (getAzure() != null) {
            return CloudPlatform.AZURE;
        } else if (getYarn() != null) {
            return CloudPlatform.YARN;
        }
        return null;
    }

    Object getAws();

    Object getAzure();

    Object getYarn();
}
