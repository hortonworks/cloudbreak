package com.sequenceiq.cloudbreak.cloud.notification.model;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

public class ResourceAllocationNotification {

    private CloudResource cloudResource;

    public ResourceAllocationNotification(CloudResource cloudResource) {
        this.cloudResource = cloudResource;
    }

    public CloudResource getCloudResource() {
        return cloudResource;
    }

    @Override
    public String toString() {
        return "ResourceAllocationNotification{" +
                "cloudResource=" + cloudResource +
                '}';
    }
}
