package com.sequenceiq.cloudbreak.cloud.event.loadbalancer;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.common.api.type.LoadBalancerType;

public class CollectLoadBalancerMetadataRequest extends CloudPlatformRequest<CollectLoadBalancerMetadataResult> {

    private final List<LoadBalancerType> typesPresentInStack;

    public CollectLoadBalancerMetadataRequest(CloudContext cloudContext, CloudCredential cloudCredential,
            List<LoadBalancerType> typesPresentInStack) {
        super(cloudContext, cloudCredential);
        this.typesPresentInStack = typesPresentInStack;
    }

    public List<LoadBalancerType> getTypesPresentInStack() {
        return typesPresentInStack;
    }
}
