package com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute.RegionHealthChecks.Delete;
import com.google.api.services.compute.Compute.RegionHealthChecks.Insert;
import com.google.api.services.compute.model.HTTPSHealthCheck;
import com.google.api.services.compute.model.HealthCheck;
import com.google.api.services.compute.model.Operation;
import com.google.api.services.compute.model.TCPHealthCheck;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.HealthProbeParameters;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.NetworkProtocol;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.common.api.type.ResourceType;

/**
 * Resposible for CRUD operations for a GCP health checker resource.
 * Currently only regional health checks are supported.
 * Generally only one exists for each port being used in a given stack, but not a hard rule
 * Currently the health check is a graceful termination of TCP handshake, support exists for HTTP level health checks
 */
@Service
public class GcpHealthCheckResourceBuilder extends AbstractGcpLoadBalancerBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpHealthCheckResourceBuilder.class);

    private static final int ORDER = 1;

    @Override
    public List<CloudResource> create(GcpContext context, AuthenticatedContext auth, CloudLoadBalancer loadBalancer, Network network) {
        return loadBalancer.getPortToTargetGroupMapping().keySet().stream()
                .map(TargetGroupPortPair::getHealthProbeParameters)
                .distinct()
                .map(lbHealthCheck -> createCloudResource(context, loadBalancer, lbHealthCheck))
                .toList();
    }

    private CloudResource createCloudResource(GcpContext context, CloudLoadBalancer loadBalancer, HealthProbeParameters lbHealthCheck) {
        String resourceName = getResourceNameService().loadBalancerWithPort(context.getName(), loadBalancer.getType(), lbHealthCheck.getPort());
        Map<String, Object> parameters = Map.of(HCPORT, lbHealthCheck);
        return CloudResource.builder()
                .withType(resourceType())
                .withName(resourceName)
                .withParameters(parameters)
                .build();
    }

    @Override
    public List<CloudResource> build(GcpContext context, AuthenticatedContext auth, List<CloudResource> buildableResources,
            CloudLoadBalancer loadBalancer, CloudStack cloudStack) throws Exception {
        List<CloudResource> results = new ArrayList<>();
        for (CloudResource buildableResource : buildableResources) {
            LOGGER.debug("Building Healthcheck {} for {}", buildableResource.getName(), context.getProjectId());
            HealthCheck healthCheck = createHealthCheck(buildableResource);
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
        LOGGER.info("Deleting healthcheck {} for {}", resource.getName(), context.getProjectId());
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

    private HealthCheck createHealthCheck(CloudResource cloudResource) {
        HealthProbeParameters lbHealthCheck = cloudResource.getParameter(HCPORT, HealthProbeParameters.class);
        HealthCheck healthCheck = new HealthCheck();
        healthCheck.setName(cloudResource.getName());
        switch (lbHealthCheck.getProtocol()) {
            case NetworkProtocol.HTTPS -> {
                healthCheck.setHttpsHealthCheck(new HTTPSHealthCheck()
                        .setPort(lbHealthCheck.getPort())
                        .setRequestPath(lbHealthCheck.getPath()));
                healthCheck.setType(NetworkProtocol.HTTPS.name());
            }
            case null, default -> {
                healthCheck.setTcpHealthCheck(new TCPHealthCheck().setPort(lbHealthCheck.getPort()));
                healthCheck.setType(NetworkProtocol.TCP.name());
            }
        }
        if (lbHealthCheck.getInterval() > 0) {
            healthCheck.setCheckIntervalSec(lbHealthCheck.getInterval());
        }
        if (lbHealthCheck.getProbeDownThreshold() > 0) {
            healthCheck.setUnhealthyThreshold(lbHealthCheck.getProbeDownThreshold());
        }
        return healthCheck;
    }
}
