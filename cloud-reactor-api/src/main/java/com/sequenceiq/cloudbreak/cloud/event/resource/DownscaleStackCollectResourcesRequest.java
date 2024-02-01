package com.sequenceiq.cloudbreak.cloud.event.resource;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

public class DownscaleStackCollectResourcesRequest extends CloudStackRequest<DownscaleStackCollectResourcesResult> {

    private final List<CloudResource> cloudResources;

    private final List<CloudInstance> instances;

    public DownscaleStackCollectResourcesRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack,
            List<CloudResource> cloudResources, List<CloudInstance> instances) {
        super(cloudContext, cloudCredential, cloudStack);
        this.cloudResources = cloudResources;
        this.instances = instances;
    }

    public List<CloudResource> getCloudResources() {
        return cloudResources;
    }

    public List<CloudInstance> getInstances() {
        return instances;
    }

    @Override
    public String toString() {
        return "DownscaleStackCollectResourcesRequest{" +
                "cloudResources=" + cloudResources +
                ", instances=" + instances +
                "} " + super.toString();
    }
}
