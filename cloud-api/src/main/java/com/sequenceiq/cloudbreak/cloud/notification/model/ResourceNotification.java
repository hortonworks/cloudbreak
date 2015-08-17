package com.sequenceiq.cloudbreak.cloud.notification.model;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

import reactor.rx.Promise;

public class ResourceNotification {

    private CloudResource cloudResource;
    private Promise<ResourcePersisted> promise;
    private Long stackId;
    private boolean create;

    public ResourceNotification(CloudResource cloudResource, Long stackId, Promise<ResourcePersisted> promise, boolean create) {
        this.cloudResource = cloudResource;
        this.stackId = stackId;
        this.promise = promise;
        this.create = create;
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

    public boolean isCreate() {
        return create;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ResourceNotification{");
        sb.append("cloudResource=").append(cloudResource);
        sb.append(", promise=").append(promise);
        sb.append(", stackId=").append(stackId);
        sb.append(", create=").append(create);
        sb.append('}');
        return sb.toString();
    }
}
