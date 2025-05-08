package com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.RegionBackendServices.Delete;
import com.google.api.services.compute.Compute.RegionBackendServices.Insert;
import com.google.api.services.compute.model.Backend;
import com.google.api.services.compute.model.BackendService;
import com.google.api.services.compute.model.InstanceGroupsListInstances;
import com.google.api.services.compute.model.InstanceGroupsListInstancesRequest;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.HealthProbeParameters;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.NetworkProtocol;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.common.api.type.LoadBalancerTypeAttribute;
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

    // For L3 load balancing the protocol have to be set as "UNSPECIFIED". https://cloud.google.com/load-balancing/docs/backend-service
    private static final String L3 = "UNSPECIFIED";

    private static final int ORDER = 2;

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpBackendServiceResourceBuilder.class);

    @Inject
    private GcpLoadBalancerTypeConverter gcpLoadBalancerTypeConverter;

    @Override
    public List<CloudResource> create(GcpContext context, AuthenticatedContext auth, CloudLoadBalancer loadBalancer, Network network) {
        Map<HealthProbeParameters, List<TargetGroupPortPair>> hcPortToTrafficPorts = new HashMap<>();
        loadBalancer.getPortToTargetGroupMapping().keySet().forEach(targetGroupPortPair -> {
            hcPortToTrafficPorts.computeIfAbsent(targetGroupPortPair.getHealthProbeParameters(), healthParams -> new ArrayList<>()).add(targetGroupPortPair);
        });
        return hcPortToTrafficPorts.entrySet().stream()
                .map(trafficByHealth -> createCloudResource(context, loadBalancer, trafficByHealth.getKey(), trafficByHealth.getValue()))
                .toList();
    }

    private CloudResource createCloudResource(GcpContext context, CloudLoadBalancer loadBalancer, HealthProbeParameters lbHealthCheck,
            List<TargetGroupPortPair> lbTraffics) {
        String resourceName = getResourceNameService().loadBalancerWithPort(context.getName(), loadBalancer.getType(), lbHealthCheck.getPort());
        Map<String, Object> parameters = Map.of(TRAFFICPORTS, lbTraffics, HCPORT, lbHealthCheck,
                CloudResource.ATTRIBUTES, Enum.valueOf(LoadBalancerTypeAttribute.class, loadBalancer.getType().name()));
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
        String projectId = context.getProjectId();
        String regionName = context.getLocation().getRegion().getRegionName();
        List<CloudResource> healthResources = filterResourcesByType(context.getLoadBalancerResources(loadBalancer.getType()), ResourceType.GCP_HEALTH_CHECK);

        for (CloudResource buildableResource : buildableResources) {
            LOGGER.info("Building backend service {} for {}", buildableResource.getName(), projectId);
            HealthProbeParameters lbHealthCheck = buildableResource.getParameter(HCPORT, HealthProbeParameters.class);
            List<TargetGroupPortPair> lbTraffics = buildableResource.getParameter(TRAFFICPORTS, List.class);
            Set<Group> groups = lbTraffics.stream()
                    .map(targetGroupPortPair -> loadBalancer.getPortToTargetGroupMapping().get(targetGroupPortPair))
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());
            String backendProtocol = lbTraffics.stream()
                    .map(lbTraffic -> convertProtocol(lbTraffic.getTrafficProtocol()))
                    .reduce((np1, np2) -> Objects.equals(np1, np2) ? np1 : L3)
                    .orElse(NetworkProtocol.TCP.name());

            BackendService backendService = new BackendService();
            backendService.setName(buildableResource.getName());
            backendService.setBackends(makeBackendForTargetGroup(context, auth, projectId, groups));
            backendService.setLoadBalancingScheme(gcpLoadBalancerTypeConverter.getScheme(loadBalancer).getGcpType());
            backendService.setProtocol(backendProtocol);
            setupBackendHealthCheck(backendService, projectId, regionName, lbHealthCheck, healthResources);
            Insert insert = context.getCompute().regionBackendServices().insert(projectId, regionName, backendService);
            results.add(doOperationalRequest(buildableResource, insert));
        }
        return results;
    }

    private void setupBackendHealthCheck(BackendService backendService, String projectId, String regionName, HealthProbeParameters lbHealthCheck,
            List<CloudResource> healthResources) {
        Optional<String> healthCheckName = healthResources.stream()
                .filter(healthResource -> lbHealthCheck.equals(healthResource.getParameter(HCPORT, HealthProbeParameters.class)))
                .findFirst()
                .map(CloudResource::getName);
        if (healthCheckName.isPresent()) {
            backendService.setHealthChecks(List.of(String.format(GCP_HEALTH_CHECK_FORMAT, projectId, regionName, healthCheckName.get())));
        } else {
            LOGGER.warn("Health check resource not found for port {}, loadbalancer will be created without healthcheck", lbHealthCheck);
        }
    }

    private String convertProtocol(NetworkProtocol protocol) {
        return switch (protocol) {
            case UDP -> NetworkProtocol.UDP.name();
            case TCP_UDP -> L3;
            case null, default -> NetworkProtocol.TCP.name();
        };
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

    private List<Backend> makeBackendForTargetGroup(GcpContext context, AuthenticatedContext auth, String projectId, Set<Group> groups)
            throws IOException {
        List<Backend> backends = new ArrayList<>();
        for (Group group : groups) {
            for (String availabilityZone : getAvailabilityZones(group, context)) {
                String instanceGroupName = getResourceNameService()
                        .group(context.getName(), group.getName(), auth.getCloudContext().getId(), availabilityZone);
                if (!isInstanceGroupEmpty(context.getCompute(), projectId, availabilityZone, instanceGroupName)) {
                    Backend backend = new Backend();
                    backend.setGroup(String.format(GCP_INSTANCEGROUP_REFERENCE_FORMAT,
                            projectId, availabilityZone, instanceGroupName));
                    backend.setBalancingMode(CONNECTION);
                    backends.add(backend);
                } else {
                    LOGGER.info("Instance group {} does not have any instances. Do not add it to backend", instanceGroupName);
                }
            }
        }
        return backends;
    }

    private boolean isInstanceGroupEmpty(Compute compute, String projectId, String zone, String instanceGroupName) throws IOException {
        InstanceGroupsListInstances instances = compute.instanceGroups().listInstances(projectId, zone, instanceGroupName,
                new InstanceGroupsListInstancesRequest()).execute();
        return CollectionUtils.isEmpty(instances.getItems());
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.GCP_BACKEND_SERVICE;
    }

    @Override
    public int order() {
        return ORDER;
    }

    private Set<String> getAvailabilityZones(Group group, ResourceBuilderContext context) {
        Set<String> availabilityZones = new HashSet<>();
        availabilityZones.add(context.getLocation().getAvailabilityZone().value());
        if (!CollectionUtils.isEmpty(group.getNetwork().getAvailabilityZones())) {
            return group.getNetwork().getAvailabilityZones();
        }
        return availabilityZones;
    }
}
