package com.sequenceiq.cloudbreak.cloud.notification.model;

public class ResourceAllocationPersisted {

    private ResourceAllocationNotification request;

    public ResourceAllocationPersisted(ResourceAllocationNotification request) {
        this.request = request;
    }

    public ResourceAllocationNotification getRequest() {
        return request;
    }


    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "ResourceAllocationPersisted{" +
                "request=" + request +
                '}';
    }
    //BEGIN GENERATED CODE
}
