package com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute.RegionHealthChecks.Delete;
import com.google.api.services.compute.Compute.RegionHealthChecks.Insert;
import com.google.api.services.compute.model.HealthCheck;
import com.google.api.services.compute.model.Operation;
import com.google.api.services.compute.model.TCPHealthCheck;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.common.api.type.ResourceType;

/**
 * Resposible for CRUD operations for a GCP health checker resource.
 * Currently only regional health checks are supported.
 * Generally only one exists for each port being used in a given stack, but not a hard rule
 * Currently the health check is a graceful termination of TCP handshake, support exists for HTTP level health checks
 *
 */
@Service
public class GcpHealthCheckResourceBuilder extends AbstractGcpLoadBalancerBuilder {

    private static final int ORDER = 1;

    @Override
    public List<CloudResource> create(GcpContext context, AuthenticatedContext auth, CloudLoadBalancer loadBalancer) {
        List<CloudResource> resources = new ArrayList<>();
        List<Integer> healthPorts = loadBalancer.getPortToTargetGroupMapping().keySet().stream().map(TargetGroupPortPair::getHealthCheckPort)
                .distinct().collect(Collectors.toList());
        for (Integer healthCheckPort : healthPorts) {
            String resourceName = getResourceNameService().resourceName(resourceType(), context.getName(), loadBalancer.getType(), healthCheckPort);
            Map<String, Object> parameters = Map.of(HCPORT, healthCheckPort);
            resources.add(new Builder().type(resourceType())
                    .name(resourceName)
                    .params(parameters)
                    .build());
        }
        return resources;
    }

    @Override
    public List<CloudResource> build(GcpContext context, AuthenticatedContext auth, List<CloudResource> buildableResources,
            CloudLoadBalancer loadBalancer, CloudStack cloudStack) throws Exception {
        List<CloudResource> results = new ArrayList<>();
        for (CloudResource buildableResource : buildableResources) {
            HealthCheck healthCheck = new HealthCheck();
            healthCheck.setTcpHealthCheck(new TCPHealthCheck().setPort(buildableResource.getParameter(HCPORT, Integer.class)));
            healthCheck.setType("TCP");
            healthCheck.setName(buildableResource.getName());
            String regionName = context.getLocation().getRegion().getRegionName();
            // global health checks are also supported, but only for internal load balancers
            Insert insert = context.getCompute().regionHealthChecks().insert(context.getProjectId(), regionName, healthCheck);
            results.add(doOperationalRequest(buildableResource, insert));
        }
        return results;
    }

    @Override
    public CloudResource delete(GcpContext context, AuthenticatedContext auth, CloudResource resource) throws Exception {
        String regionName = context.getLocation().getRegion().getRegionName();
        Delete delete = context.getCompute().regionHealthChecks().delete(context.getProjectId(), regionName, resource.getName());
        try {
            Operation operation = delete.execute();
            return createOperationAwareCloudResource(resource, operation);
        } catch (GoogleJsonResponseException e) {
            exceptionHandler(e, resource.getName(), resourceType());
            return null;
        }
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.GCP_HEALTH_CHECK;
    }

    @Override
    public int order() {
        return ORDER;
    }
}
