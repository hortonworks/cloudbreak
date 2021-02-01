package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.common.api.type.LoadBalancerType;

public class LoadBalancerMetadataRequest extends StackEvent {

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    private final CloudStack cloudStack;

    private final List<LoadBalancerType> typesPresentInStack;

    private final Stack stack;

    private final List<CloudResource> cloudResources;

    public LoadBalancerMetadataRequest(Stack stack, CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack,
            List<LoadBalancerType> typesPresentInStack, List<CloudResource> cloudResources) {
        super(stack.getId());
        this.stack = stack;
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
        this.cloudStack = cloudStack;
        this.typesPresentInStack = typesPresentInStack;
        this.cloudResources = cloudResources;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public CloudStack getCloudStack() {
        return cloudStack;
    }

    public List<LoadBalancerType> getTypesPresentInStack() {
        return typesPresentInStack;
    }

    public Stack getStack() {
        return stack;
    }

    public List<CloudResource> getCloudResources() {
        return cloudResources;
    }
}
