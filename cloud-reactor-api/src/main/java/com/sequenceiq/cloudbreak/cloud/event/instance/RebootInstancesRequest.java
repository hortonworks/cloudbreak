package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

public class RebootInstancesRequest<T> extends CloudPlatformRequest<T> {

    private final List<CloudInstance> cloudInstances;

    private final List<CloudResource> cloudResources;

    public RebootInstancesRequest(CloudContext cloudContext, CloudCredential cloudCredential,
            List<CloudResource> cloudResources,
            List<CloudInstance> cloudInstances) {
        super(cloudContext, cloudCredential);
        this.cloudInstances = cloudInstances;
        this.cloudResources = cloudResources;
    }

    public List<CloudInstance> getCloudInstances() {
        return cloudInstances;
    }

    public List<CloudResource> getCloudResources() {
        return cloudResources;
    }

    @Override
    public String toString() {
        return "RebootInstancesRequest{" +
                "cloudInstances=" + cloudInstances +
                ", cloudResources=" + cloudResources +
                '}';
    }
}
