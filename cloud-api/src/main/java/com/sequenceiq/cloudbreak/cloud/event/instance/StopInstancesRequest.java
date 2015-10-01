package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.StackPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

public class StopInstancesRequest<T> extends StackPlatformRequest<T> {

    private final List<CloudResource> resources;
    private final List<CloudInstance> cloudInstances;

    public StopInstancesRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack, List<CloudResource> resources,
            List<CloudInstance> cloudInstances) {
        super(cloudContext, cloudCredential, cloudStack);
        this.resources = resources;
        this.cloudInstances = cloudInstances;
    }

    public List<CloudInstance> getCloudInstances() {
        return cloudInstances;
    }

    public List<CloudResource> getResources() {
        return resources;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("StopInstancesRequest{");
        sb.append("cloudInstances=").append(cloudInstances);
        sb.append(", resources=").append(resources);
        sb.append('}');
        return sb.toString();
    }
}
