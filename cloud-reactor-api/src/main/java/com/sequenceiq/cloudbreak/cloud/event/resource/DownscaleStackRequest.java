package com.sequenceiq.cloudbreak.cloud.event.resource;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

public class DownscaleStackRequest extends CloudStackRequest<DownscaleStackResult> {

    private final List<CloudResource> cloudResources;

    private final List<CloudInstance> instances;

    private final Object resourcesToScale;

    public DownscaleStackRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack, List<CloudResource> cloudResources,
            List<CloudInstance> instances) {
        super(cloudContext, cloudCredential, cloudStack);
        this.cloudResources = cloudResources;
        this.instances = instances;
        resourcesToScale = null;
    }

    public DownscaleStackRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack, List<CloudResource> cloudResources,
            List<CloudInstance> instances, Object resourcesToScale) {
        super(cloudContext, cloudCredential, cloudStack);
        this.cloudResources = cloudResources;
        this.instances = instances;
        this.resourcesToScale = resourcesToScale;
    }

    public List<CloudResource> getCloudResources() {
        return cloudResources;
    }

    public List<CloudInstance> getInstances() {
        return instances;
    }

    public Object getResourcesToScale() {
        return resourcesToScale;
    }
}
