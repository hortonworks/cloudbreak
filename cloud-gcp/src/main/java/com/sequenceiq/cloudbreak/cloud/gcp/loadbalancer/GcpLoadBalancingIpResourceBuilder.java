package com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.compute.GcpReservedIpResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.ResourceType;

/**
 * The loadbalancer version of creating a reserved IP address, where possible use {@link GcpReservedIpResourceBuilder}
 * Used to reserve an IP address to act as the destination for the load balancer forwarding rule
 *
 */
@Service
public class GcpLoadBalancingIpResourceBuilder extends AbstractGcpLoadBalancerBuilder {

    private static final int ORDER = 3;

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpLoadBalancingIpResourceBuilder.class);

    @Inject
    private GcpReservedIpResourceBuilder reservedIpResourceBuilder;

    @Override
    public List<CloudResource> create(GcpContext context, AuthenticatedContext auth, CloudLoadBalancer loadBalancer) {
            String resourceName = getResourceNameService().resourceName(resourceType(), auth.getCloudContext().getName(), loadBalancer.getType(), 1);
            return List.of(new CloudResource.Builder().type(resourceType())
                    .name(resourceName)
                    .build());
    }

    @Override
    public List<CloudResource> build(GcpContext context, AuthenticatedContext auth, List<CloudResource> buildableResources,
            CloudLoadBalancer loadBalancer, CloudStack cloudStack) throws Exception {
        return reservedIpResourceBuilder.buildReservedIp(context, buildableResources, cloudStack,
                loadBalancer.getType() == LoadBalancerType.PRIVATE ? GcpReservedIpResourceBuilder.INTERNAL : GcpReservedIpResourceBuilder.EXTERNAL);
    }

    @Override
    public CloudResource delete(GcpContext context, AuthenticatedContext auth, CloudResource resource) throws Exception {
        return reservedIpResourceBuilder.deleteReservedIP(context, resource);
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.GCP_RESERVED_IP;
    }

    @Override
    public int order() {
        return ORDER;
    }
}
