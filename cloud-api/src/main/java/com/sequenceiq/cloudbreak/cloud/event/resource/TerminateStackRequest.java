package com.sequenceiq.cloudbreak.cloud.event.resource;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

public class TerminateStackRequest<T> extends CloudPlatformRequest<T> {

    private List<CloudResource> cloudResources;

    public TerminateStackRequest(CloudContext cloudContext, CloudCredential cloudCredential, List<CloudResource> resources) {
        super(cloudContext, cloudCredential);
        this.cloudResources = new ArrayList<>(resources);
    }

    public List<CloudResource> getCloudResources() {
        return cloudResources;
    }

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "TerminateStackRequest{" +
                ", cloudResources=" + cloudResources +
                '}';
    }
    //END GENERATED CODE

}
