package com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute.RegionBackendServices.Delete;
import com.google.api.services.compute.Compute.RegionBackendServices.Insert;
import com.google.api.services.compute.model.Backend;
import com.google.api.services.compute.model.BackendService;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.common.api.type.ResourceType;

/**
 * Responsible for CRUD opertaions of a GCP Backend Service Resource
 * A backend service could have multiple definitions, but currently the only one used is based on instance groups
 * Set the health check mentod to be used for the related instance groups
 */
@Service
public class GcpBackendServiceResourceBuilder extends AbstractGcpLoadBalancerBuilder {

    public static final String GCP_INSTANCEGROUP_REFERENCE_FORMAT = "https://www.googleapis.com/compute/v1/projects/%s/zones/%s/instanceGroups/%s";

    public static final String CONNECTION = "CONNECTION";

    public static final String GCP_HEALTH_CHECK_FORMAT = "https://www.googleapis.com/compute/v1/projects/%s/regions/%s/healthChecks/%s";

    private static final int ORDER = 2;

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpBackendServiceResourceBuilder.class);

    @Override
    public List<CloudResource> create(GcpContext context, AuthenticatedContext auth, CloudLoadBalancer loadBalancer) {
        List<CloudResource> resources = new ArrayList<>();
        for (TargetGroupPortPair targetGroupPortPair : loadBalancer.getPortToTargetGroupMapping().keySet()) {
            Integer healthCheckPort = targetGroupPortPair.getHealthCheckPort();
            String resourceName = getResourceNameService().resourceName(resourceType(), context.getName(), loadBalancer.getType(), healthCheckPort);
            Map<String, Object> parameters = Map.of(TRAFFICPORT, targetGroupPortPair.getTrafficPort(), HCPORT, targetGroupPortPair.getHealthCheckPort());
            resources.add(new CloudResource.Builder().type(resourceType())
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
        String projectId = context.getProjectId();
        String zone = context.getLocation().getAvailabilityZone().value();
        List<CloudResource> healthResources = filterResourcesByType(context.getLoadBalancerResources(loadBalancer.getType()), ResourceType.GCP_HEALTH_CHECK);

        for (CloudResource buildableResource : buildableResources) {
            LOGGER.info("Building backend service {} for {}", buildableResource.getName(), projectId);
            Optional<String> name = healthResources.stream()
                    .filter(healthResource -> buildableResource.getParameter(HCPORT, Integer.class).equals(healthResource.getParameter(HCPORT, Integer.class)))
                    .findFirst()
                    .map(CloudResource::getName);
            if (!name.isPresent()) {
                LOGGER.info("Health check resource not found for port {}", buildableResource.getParameter(HCPORT, Integer.class));
            }

            BackendService backendService = new BackendService();
            backendService.setHealthChecks(Collections.singletonList(
                String.format(GCP_HEALTH_CHECK_FORMAT,
                        projectId, context.getLocation().getRegion().getRegionName(), name.get())));

            List<Backend> backends = new ArrayList<>();
            TargetGroupPortPair targetGroupPortPair = new TargetGroupPortPair(buildableResource.getParameter(TRAFFICPORT, Integer.class),
                    buildableResource.getParameter(HCPORT, Integer.class));
            Set<Group> groups = loadBalancer.getPortToTargetGroupMapping().get(targetGroupPortPair);
            for (Group group : groups) {
                Backend backend = new Backend();
                String groupname = getResourceNameService().resourceName(ResourceType.GCP_INSTANCE_GROUP, context.getName(), group.getName());
                backend.setGroup(String.format(GCP_INSTANCEGROUP_REFERENCE_FORMAT,
                        projectId, zone, groupname));
                backend.setBalancingMode(CONNECTION);
                backends.add(backend);
            }

            backendService.setBackends(backends);
            backendService.setName(buildableResource.getName());
            backendService.setLoadBalancingScheme(GcpLoadBalancerScheme.getScheme(loadBalancer).getGcpType());
            backendService.setProtocol("TCP");
            String regionName = context.getLocation().getRegion().getRegionName();
            Insert insert = context.getCompute().regionBackendServices().insert(projectId, regionName, backendService);
            results.add(doOperationalRequest(buildableResource, insert));
        }
        return results;
    }

    @Override
    public CloudResource delete(GcpContext context, AuthenticatedContext auth, CloudResource resource) throws Exception {
        String regionName = context.getLocation().getRegion().getRegionName();
        LOGGER.info("deleting backend service {} for {}", resource.getName(), context.getProjectId());
        Delete delete = context.getCompute().regionBackendServices().delete(context.getProjectId(), regionName, resource.getName());
        try {
            Operation operation = delete.execute();
            return createOperationAwareCloudResource(resource, operation);
        } catch (GoogleJsonResponseException e) {
            exceptionHandler(e, resource.getName(), resourceType());
            return null;
        }
    }

    private List<CloudResource> filterResourcesByType(List<CloudResource> resources, ResourceType resourceType) {
        return Optional.ofNullable(resources).orElseGet(List::of).stream()
                .filter(resource -> resourceType.equals(resource.getType()))
                .collect(Collectors.toList());
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.GCP_BACKEND_SERVICE;
    }

    @Override
    public int order() {
        return ORDER;
    }
}
