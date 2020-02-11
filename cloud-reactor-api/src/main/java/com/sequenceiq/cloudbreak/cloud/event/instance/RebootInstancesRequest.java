package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;

public class RebootInstancesRequest<T> extends CloudPlatformRequest<T> {

    private final List<CloudInstance> cloudInstances;

    public RebootInstancesRequest(CloudContext cloudContext, CloudCredential cloudCredential,
            List<CloudInstance> cloudInstances) {
        super(cloudContext, cloudCredential);
        this.cloudInstances = cloudInstances;
    }

    public List<CloudInstance> getCloudInstances() {
        return cloudInstances;
    }

    @Override
    public String toString() {
        super.toString();
        return "RebootInstancesRequest{cloudInstances=" + cloudInstances + "}";
    }
}
