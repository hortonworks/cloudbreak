package com.sequenceiq.cloudbreak.cloud.event.resource;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.StackPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

public class GetInstancesStateRequest<T> extends StackPlatformRequest<T> {

    private final List<CloudInstance> instances;

    public GetInstancesStateRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack, List<CloudInstance> instances) {
        super(cloudContext, cloudCredential, cloudStack);
        this.instances = instances;
    }

    public List<CloudInstance> getInstances() {
        return instances;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GetInstancesStateRequest{");
        sb.append(", instances=").append(instances);
        sb.append('}');
        return sb.toString();
    }
}
