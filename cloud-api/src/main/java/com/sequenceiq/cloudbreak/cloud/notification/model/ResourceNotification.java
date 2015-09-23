package com.sequenceiq.cloudbreak.cloud.notification.model;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

import reactor.rx.Promise;

public class ResourceNotification {

    private CloudResource cloudResource;
    private Promise<ResourcePersisted> promise;
    private Long stackId;
    private ResourceNotificationType type;

    public ResourceNotification(CloudResource cloudResource, Long stackId, Promise<ResourcePersisted> promise, ResourceNotificationType type) {
        this.cloudResource = cloudResource;
        this.stackId = stackId;
        this.promise = promise;
        this.type = type;
    }

    public CloudResource getCloudResource() {
        return cloudResource;
    }

    public Promise<ResourcePersisted> getPromise() {
        return promise;
    }

    public Long getStackId() {
        return stackId;
    }

    public ResourceNotificationType getType() {
        return type;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ResourceNotification{");
        sb.append("cloudResource=").append(cloudResource);
        sb.append(", promise=").append(promise);
        sb.append(", stackId=").append(stackId);
        sb.append(", type=").append(type);
        sb.append('}');
        return sb.toString();
    }
}
