package com.sequenceiq.cloudbreak.cloud.event.resource;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

import reactor.rx.Promise;

public class TerminateStackRequest<T> extends CloudPlatformRequest<T> {

    private CloudCredential cloudCredential;

    private List<CloudResource> cloudResources;

    public TerminateStackRequest(CloudContext cloudContext, CloudCredential cloudCredential, List<CloudResource> resources, Promise<T> result) {
        super(cloudContext, result);
        this.cloudCredential = cloudCredential;
        this.cloudResources = new ArrayList<>(resources);
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public List<CloudResource> getCloudResources() {
        return cloudResources;
    }

    @Override
    public String toString() {
        return "TerminateStackRequest{" +
                "cloudCredential=" + cloudCredential +
                ", cloudResources=" + cloudResources +
                '}';
    }

}
