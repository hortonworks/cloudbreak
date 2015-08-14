package com.sequenceiq.cloudbreak.cloud.event.resource;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

public class TerminateStackRequest<T> extends CloudStackRequest<T> {

    private List<CloudResource> cloudResources;

    public TerminateStackRequest(CloudContext cloudContext, CloudStack cloudStack, CloudCredential cloudCredential, List<CloudResource> resources) {
        super(cloudContext, cloudCredential, cloudStack);
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
