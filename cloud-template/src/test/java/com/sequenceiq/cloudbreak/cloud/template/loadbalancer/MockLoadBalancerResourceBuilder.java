package com.sequenceiq.cloudbreak.cloud.template.loadbalancer;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.template.LoadBalancerResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.common.api.type.ResourceType;

public class MockLoadBalancerResourceBuilder implements LoadBalancerResourceBuilder<ResourceBuilderContext> {
    @Override
    public Platform platform() {
        return null;
    }

    @Override
    public Variant variant() {
        return null;
    }

    @Override
    public List<CloudResource> create(ResourceBuilderContext context, AuthenticatedContext auth, CloudLoadBalancer loadBalancer, Network network) {
        return null;
    }

    @Override
    public CloudResource delete(ResourceBuilderContext context, AuthenticatedContext auth, CloudResource resource) throws Exception {
        return null;
    }

    @Override
    public ResourceType resourceType() {
        return null;
    }

    @Override
    public List<CloudResource> build(ResourceBuilderContext context, AuthenticatedContext auth, List buildableResources,
            CloudLoadBalancer loadBalancer, CloudStack cloudStack) throws Exception {
        return null;
    }

    @Override
    public int order() {
        return 0;
    }

    @Override
    public List<CloudResourceStatus> checkResources(ResourceBuilderContext context, AuthenticatedContext auth, List list) {
        return null;
    }
}
