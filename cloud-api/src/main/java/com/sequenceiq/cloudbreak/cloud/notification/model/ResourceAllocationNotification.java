package com.sequenceiq.cloudbreak.cloud.notification.model;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

import reactor.rx.Promise;

public class ResourceAllocationNotification {

    private CloudResource cloudResource;

    private Promise<ResourceAllocationPersisted> promise;

    private Long stackId;

    public ResourceAllocationNotification(CloudResource cloudResource, Long stackId, Promise<ResourceAllocationPersisted> promise) {
        this.cloudResource = cloudResource;
        this.promise = promise;
        this.stackId = stackId;
    }

    public CloudResource getCloudResource() {
        return cloudResource;
    }

    public Promise<ResourceAllocationPersisted> getPromise() {
        return promise;
    }

    public Long getStackId() {
        return stackId;
    }

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "ResourceAllocationNotification{" +
                "cloudResource=" + cloudResource +
                '}';
    }
    //END GENERATED CODE
}
