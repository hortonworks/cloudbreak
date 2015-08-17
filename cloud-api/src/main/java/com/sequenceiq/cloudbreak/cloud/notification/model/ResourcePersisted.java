package com.sequenceiq.cloudbreak.cloud.notification.model;

public class ResourcePersisted {

    private ResourceNotification request;

    public ResourcePersisted(ResourceNotification request) {
        this.request = request;
    }

    public ResourceNotification getRequest() {
        return request;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ResourcePersisted{");
        sb.append("request=").append(request);
        sb.append('}');
        return sb.toString();
    }
}
