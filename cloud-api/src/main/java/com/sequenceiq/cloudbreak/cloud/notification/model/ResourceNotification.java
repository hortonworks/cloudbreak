package com.sequenceiq.cloudbreak.cloud.notification.model;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

/**
 * Notification sent to Cloudbreak.
 */
public class ResourceNotification {

    private final CloudResource cloudResource;
    private final CloudContext cloudContext;
    private final ResourceNotificationType type;

    private ResourcePersisted resource;
    private String error;

    public ResourceNotification(CloudResource cloudResource, CloudContext cloudContext, ResourceNotificationType type) {
        this.cloudResource = cloudResource;
        this.cloudContext = cloudContext;
        this.type = type;
    }

    public CloudResource getCloudResource() {
        return cloudResource;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public ResourceNotificationType getType() {
        return type;
    }

    public ResourcePersisted getResource() {
        return resource;
    }

    public void setResource(ResourcePersisted resource) {
        this.resource = resource;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public boolean isFailed() {
        return error != null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ResourceNotification{");
        sb.append("cloudResource=").append(cloudResource);
        sb.append(", resource=").append(resource);
        sb.append(", error=").append(error);
        sb.append(", cloudContext=").append(cloudContext);
        sb.append(", type=").append(type);
        sb.append('}');
        return sb.toString();
    }
}
