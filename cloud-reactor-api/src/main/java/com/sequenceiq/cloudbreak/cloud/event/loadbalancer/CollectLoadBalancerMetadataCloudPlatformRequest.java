package com.sequenceiq.cloudbreak.cloud.event.loadbalancer;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.LoadBalancerType;

public class CollectLoadBalancerMetadataCloudPlatformRequest extends CloudPlatformRequest<CollectLoadBalancerMetadataCloudPlatformResult> {

    private final List<LoadBalancerType> typesPresentInStack;

    private final List<CloudResource> cloudResources;

    public CollectLoadBalancerMetadataCloudPlatformRequest(CloudContext cloudContext, CloudCredential cloudCredential,
            List<LoadBalancerType> typesPresentInStack, List<CloudResource> cloudResources) {
        super(cloudContext, cloudCredential);
        this.typesPresentInStack = typesPresentInStack;
        this.cloudResources = cloudResources;
    }

    public List<LoadBalancerType> getTypesPresentInStack() {
        return typesPresentInStack;
    }

    public List<CloudResource> getCloudResources() {
        return cloudResources;
    }

    @Override
    public String toString() {
        return "CollectLoadBalancerMetadataCloudPlatformRequest{"
                + "typesPresentInStack=" + typesPresentInStack
                + ", cloudResources=" + cloudResources
                + '}';
    }
}
