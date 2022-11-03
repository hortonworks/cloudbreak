package com.sequenceiq.cloudbreak.cloud.notification.model;

import java.util.Collections;
import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.eventbus.Promise;

/**
 * Notification sent to Cloudbreak.
 */
public class ResourceNotification {

    private final List<CloudResource> cloudResources;

    private final CloudContext cloudContext;

    private final Promise<ResourcePersisted> promise;

    private final ResourceNotificationType type;

    public ResourceNotification(CloudResource cloudResource, CloudContext cloudContext, ResourceNotificationType type) {
        this(Collections.singletonList(cloudResource), cloudContext, type);
    }

    public ResourceNotification(List<CloudResource> cloudResources, CloudContext cloudContext, ResourceNotificationType type) {
        this.cloudResources = cloudResources;
        this.cloudContext = cloudContext;
        promise = Promise.prepare();
        this.type = type;
    }

    public CloudResource getCloudResource() {
        if (cloudResources.size() > 1) {
            throw new UnsupportedOperationException("Please use the list version of this function");
        }
        return cloudResources.stream().findFirst().orElse(null);
    }

    public List<CloudResource> getCloudResources() {
        return cloudResources;
    }

    public Promise<ResourcePersisted> getPromise() {
        return promise;
    }

    public ResourcePersisted getResult() {
        try {
            return promise.await();
        } catch (InterruptedException e) {
            throw new CloudConnectorException("ResourceNotification has been interrupted", e);
        }
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public ResourceNotificationType getType() {
        return type;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ResourceNotification{");
        sb.append("cloudResources=").append(cloudResources);
        sb.append(", promise=").append(promise);
        sb.append(", cloudContext=").append(cloudContext);
        sb.append(", type=").append(type);
        sb.append('}');
        return sb.toString();
    }
}
