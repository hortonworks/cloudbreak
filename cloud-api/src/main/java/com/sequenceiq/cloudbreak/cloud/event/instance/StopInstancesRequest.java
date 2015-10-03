package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

public class StopInstancesRequest<T> extends CloudPlatformRequest<T> {

    private List<CloudResource> resources;
    private List<CloudInstance> cloudInstances;

    public StopInstancesRequest(CloudContext cloudContext, CloudCredential cloudCredential, List<CloudResource> resources, List<CloudInstance> cloudInstances) {
        super(cloudContext, cloudCredential);
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
