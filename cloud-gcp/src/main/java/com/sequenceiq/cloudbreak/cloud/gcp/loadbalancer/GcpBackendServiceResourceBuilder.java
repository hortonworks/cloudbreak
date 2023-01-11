package com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

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
 * Responsible for CRUD operations of a GCP Backend Service Resource
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

    @Inject
    private GcpLoadBalancerTypeConverter gcpLoadBalancerTypeConverter;

    @Override
    public List<CloudResource> create(GcpContext context, AuthenticatedContext auth, CloudLoadBalancer loadBalancer) {
        return getCloudResourcesForFrontendAndBackendCreate(context, loadBalancer);
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
            Map<String, Object> portMap = buildableResource.getParameters();
            Integer hcPort = buildableResource.getParameter(HCPORT, Integer.class);
            Set<Group> groups = new HashSet<>();
            if (portMap.containsKey(TRAFFICPORTS)) {
                for (Integer trafficPort : (List<Integer>) portMap.get(TRAFFICPORTS)) {
                    TargetGroupPortPair targetGroupPortPair = new TargetGroupPortPair(trafficPort, hcPort);
                    groups.addAll(loadBalancer.getPortToTargetGroupMapping().get(targetGroupPortPair));
                }
            } else {
                Integer trafficPort = buildableResource.getParameter(TRAFFICPORT, Integer.class);
                TargetGroupPortPair targetGroupPortPair = new TargetGroupPortPair(trafficPort, hcPort);
                groups.addAll(loadBalancer.getPortToTargetGroupMapping().get(targetGroupPortPair));
            }
            makeBackendForTargetGroup(context, auth, loadBalancer, projectId, zone, groups, backends);

            backendService.setBackends(backends);
            backendService.setName(buildableResource.getName());
            backendService.setLoadBalancingScheme(gcpLoadBalancerTypeConverter.getScheme(loadBalancer).getGcpType());
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

    private void makeBackendForTargetGroup(GcpContext context, AuthenticatedContext auth, CloudLoadBalancer loadBalancer, String projectId, String zone,
            Set<Group> groups, List<Backend> backends) {
        for (Group group : groups) {
            Backend backend = new Backend();
            String groupname = getResourceNameService()
                    .resourceName(ResourceType.GCP_INSTANCE_GROUP, context.getName(), group.getName(), auth.getCloudContext().getId());
            backend.setGroup(String.format(GCP_INSTANCEGROUP_REFERENCE_FORMAT,
                    projectId, zone, groupname));
            backend.setBalancingMode(CONNECTION);
            backends.add(backend);
        }
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
