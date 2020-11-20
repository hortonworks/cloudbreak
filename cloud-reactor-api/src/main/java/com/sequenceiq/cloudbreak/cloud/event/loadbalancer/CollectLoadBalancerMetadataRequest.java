package com.sequenceiq.cloudbreak.cloud.event.loadbalancer;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public class CollectLoadBalancerMetadataRequest extends CloudPlatformRequest<CollectLoadBalancerMetadataResult> {

    private final List<String> typesPresentInStack;

    public CollectLoadBalancerMetadataRequest(CloudContext cloudContext, CloudCredential cloudCredential, List<String> typesPresentInStack) {
        super(cloudContext, cloudCredential);
        this.typesPresentInStack = typesPresentInStack;
    }

    public List<String> getTypesPresentInStack() {
        return typesPresentInStack;
    }
}
