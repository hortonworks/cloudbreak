package com.sequenceiq.cloudbreak.cloud.notification.model;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

import reactor.rx.Promise;
import reactor.rx.Promises;

/**
 * Notification sent to Cloudbreak.
 */
public class ResourceNotification {

    private final CloudResource cloudResource;

    private final CloudContext cloudContext;

    private final Promise<ResourcePersisted> promise;

    private final ResourceNotificationType type;

    public ResourceNotification(CloudResource cloudResource, CloudContext cloudContext, ResourceNotificationType type) {
        this.cloudResource = cloudResource;
        this.cloudContext = cloudContext;
        this.promise = Promises.prepare();
        this.type = type;
    }

    public CloudResource getCloudResource() {
        return cloudResource;
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
        final StringBuilder sb = new StringBuilder("ResourceNotification{");
        sb.append("cloudResource=").append(cloudResource);
        sb.append(", promise=").append(promise);
        sb.append(", cloudContext=").append(cloudContext);
        sb.append(", type=").append(type);
        sb.append('}');
        return sb.toString();
    }
}
