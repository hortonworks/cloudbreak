package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

public class StartInstancesRequest extends CloudPlatformRequest<StartInstancesResult> {

    private List<CloudInstance> cloudInstances;
    private List<CloudResource> resources;

    public StartInstancesRequest(CloudContext cloudContext, CloudCredential credential, List<CloudResource> resources, List<CloudInstance> cloudInstances) {
        super(cloudContext, credential);
        this.cloudInstances = cloudInstances;
        this.resources = resources;
    }

    public List<CloudInstance> getCloudInstances() {
        return cloudInstances;
    }

    public List<CloudResource> getResources() {
        return resources;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("StartInstancesRequest{");
        sb.append("cloudInstances=").append(cloudInstances);
        sb.append(", resources=").append(resources);
        sb.append('}');
        return sb.toString();
    }
}
