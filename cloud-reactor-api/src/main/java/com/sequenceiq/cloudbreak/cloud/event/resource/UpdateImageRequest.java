package com.sequenceiq.cloudbreak.cloud.event.resource;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

public class UpdateImageRequest<T> extends CloudStackRequest<UpdateImageResult> {

    private final List<CloudResource> cloudResources;

    public UpdateImageRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack, List<CloudResource> cloudResources) {
        super(cloudContext, cloudCredential, cloudStack);
        this.cloudResources = cloudResources;
    }

    public List<CloudResource> getCloudResources() {
        return cloudResources;
    }
}
