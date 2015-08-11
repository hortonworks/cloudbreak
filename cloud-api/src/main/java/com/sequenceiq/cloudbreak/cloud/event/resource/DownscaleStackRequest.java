package com.sequenceiq.cloudbreak.cloud.event.resource;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;

public class DownscaleStackRequest<T> extends CloudStackRequest<T> {

    private List<CloudResource> cloudResources;
    private List<InstanceTemplate> instanceTemplates;

    public DownscaleStackRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack, List<CloudResource> cloudResources,
            List<InstanceTemplate> instanceTemplates) {
        super(cloudContext, cloudCredential, cloudStack);
        this.cloudResources = cloudResources;
        this.instanceTemplates = instanceTemplates;
    }

    public List<CloudResource> getCloudResources() {
        return cloudResources;
    }

    public List<InstanceTemplate> getInstanceTemplates() {
        return instanceTemplates;
    }
}
