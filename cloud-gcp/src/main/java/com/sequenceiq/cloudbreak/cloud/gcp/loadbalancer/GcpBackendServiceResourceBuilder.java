package com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer;

import static com.sequenceiq.cloudbreak.cloud.service.CloudbreakResourceNameService.DELIMITER;

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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.RegionBackendServices.Delete;
import com.google.api.services.compute.Compute.RegionBackendServices.Insert;
import com.google.api.services.compute.Compute.RegionBackendServices.Update;
import com.google.api.services.compute.model.Backend;
import com.google.api.services.compute.model.BackendService;
import com.google.api.services.compute.model.InstanceGroupsListInstances;
import com.google.api.services.compute.model.InstanceGroupsListInstancesRequest;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpExceptionUtil;
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
        List<CloudResource> resourcesWithLbType = fetchAllResourceFromDb(resourceType(), auth.getCloudContext().getId()).stream()
                .filter(resource -> loadBalancer.getType().name()
                        .equals(resource.getParameter(CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.class).getName()))
                .toList();
        LOGGER.debug("Existing resources with type [{}] and Loadbalancer type [{}]: {}", resourceType(), loadBalancer.getType(), resourcesWithLbType);
        Map<HealthProbeParameters, List<TargetGroupPortPair>> hcPortToTrafficPorts = new HashMap<>();
        loadBalancer.getPortToTargetGroupMapping().keySet().forEach(targetGroupPortPair -> {
            hcPortToTrafficPorts.computeIfAbsent(targetGroupPortPair.getHealthProbeParameters(), healthParams -> new ArrayList<>()).add(targetGroupPortPair);
        });
        List<CloudResource> cloudResources = hcPortToTrafficPorts.entrySet().stream()
                .map(trafficByHealth ->
                        resourcesWithLbType.stream()
                                .filter(resource -> resource.getName().contains(mapPortToPortPart(trafficByHealth.getKey().getPort())))
                                .findFirst().map(existingCloudResource ->
                                        CloudResource.builder()
                                                .cloudResource(existingCloudResource)
                                                .withParameters(enrichParametersWithAttributes(
                                                        Map.of(TRAFFICPORTS, trafficByHealth.getValue(), HCPORT, trafficByHealth.getKey()),
                                                        loadBalancer.getType()))
                                                .build())
                                .orElseGet(() -> createCloudResource(context, loadBalancer, trafficByHealth.getKey(), trafficByHealth.getValue())))
                .toList();
        LOGGER.debug("Created cloud resources with type [{}] and Loadbalancer type [{}]: {}", resourceType(), loadBalancer.getType(), cloudResources);
        return cloudResources;
    }

    private CloudResource createCloudResource(GcpContext context, CloudLoadBalancer loadBalancer, HealthProbeParameters lbHealthCheck,
            List<TargetGroupPortPair> lbTraffics) {
        String resourceName = getResourceNameService().loadBalancerWithPort(context.getName(), loadBalancer.getType(), lbHealthCheck.getPort());
        Map<String, Object> parameters = Map.of(TRAFFICPORTS, lbTraffics, HCPORT, lbHealthCheck);
        parameters = enrichParametersWithAttributes(parameters, loadBalancer.getType());
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
            Optional<BackendService> backendServiceFromProvider = fetchFromProvider(() ->
                    context.getCompute().regionBackendServices().get(projectId, regionName, buildableResource.getName()).execute(), buildableResource.getName());
            List<TargetGroupPortPair> lbTraffics = buildableResource.getParameter(TRAFFICPORTS, List.class);
            Set<Group> groups = lbTraffics.stream()
                    .map(targetGroupPortPair -> loadBalancer.getPortToTargetGroupMapping().get(targetGroupPortPair))
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());
            if (backendServiceFromProvider.isPresent()) {
                BackendService backendService = backendServiceFromProvider.get();
                List<Backend> backends = makeBackendForTargetGroup(context, auth, projectId, groups);
                if (backends.isEmpty() && !CollectionUtils.isEmpty(backendService.getBackends())) {
                    throw new GcpResourceException(String.format("No instance group with instances found for groups %s, refusing to remove all backends %s",
                            groups.stream().map(Group::getName).toList(), backendService.getBackends()), resourceType(), backendService.getName());
                }
                backendService.setBackends(backends);
                Update update = context.getCompute().regionBackendServices().update(projectId, regionName, backendService.getName(), backendService);
                results.add(doOperationalRequest(buildableResource, update));
            } else {
                HealthProbeParameters lbHealthCheck = buildableResource.getParameter(HCPORT, HealthProbeParameters.class);
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
        List<CloudResource> instanceGroupResources = fetchAllResourceFromDb(ResourceType.GCP_INSTANCE_GROUP, auth.getCloudContext().getId());
        if (instanceGroupResources.isEmpty()) {
            throw new GcpResourceException(String.format("Stack %s has no instance groups, so the backends of the load balancer can't be set up",
                    auth.getCloudContext().getName()), resourceType(), context.getName());
        }
        for (Group group : groups) {
            for (String availabilityZone : getAvailabilityZones(group, context)) {
                String instanceGroupName = getResourceNameService().group(context.getName(), group.getName(), auth.getCloudContext().getId(), availabilityZone);
                Optional<String> realInstanceGroupNameIfNotEmpty =
                        fetchInstanceGroupNameIfNotEmpty(context.getCompute(), projectId, availabilityZone, instanceGroupName, instanceGroupResources,
                                auth.getCloudContext().getId());
                if (realInstanceGroupNameIfNotEmpty.isPresent()) {
                    Backend backend = new Backend();
                    backend.setGroup(String.format(GCP_INSTANCEGROUP_REFERENCE_FORMAT, projectId, availabilityZone, realInstanceGroupNameIfNotEmpty.get()));
                    backend.setBalancingMode(CONNECTION);
                    backends.add(backend);
                } else {
                    LOGGER.info("Neither instance group {} nor its legacy names have any instances in zone {}. Do not add it to backend",
                            instanceGroupName, availabilityZone);
                }
            }
        }
        return backends;
    }

    private Optional<String> fetchInstanceGroupNameIfNotEmpty(Compute compute, String projectId, String zone, String instanceGroupName,
            List<CloudResource> instanceGroupResources, Long stackId) throws IOException {
        for (String candidateName : instanceGroupNameCandidates(instanceGroupName, stackId)) {
            Optional<InstanceGroupsListInstances> instances = listInstancesIfInstanceGroupExists(compute, projectId, zone, candidateName);
            if (instances.isPresent()) {
                LOGGER.info("Instance group [{}] found in project [{}] zone [{}] (requested [{}])", candidateName, projectId, zone, instanceGroupName);
                return CollectionUtils.isEmpty(instances.get().getItems()) ? Optional.empty() : Optional.of(candidateName);
            }
            LOGGER.debug("Instance group [{}] not found in project [{}] zone [{}], trying the next legacy name", candidateName, projectId, zone);
        }
        LOGGER.warn("No instance group found in project [{}] zone [{}] for [{}] or its legacy names. Existing instance groups in DB: {}",
                projectId, zone, instanceGroupName, instanceGroupResources.stream()
                        .map(resource -> String.format("name: %s, availability zone: %s", resource.getName(), resource.getAvailabilityZone()))
                        .toList());
        return Optional.empty();
    }

    /**
     * Instance group names got the AZ postfix in mid-2024, and very old FreeIPA stacks have no stack id in the name either,
     * so the current name is tried first, followed by the legacy ones.
     */
    private List<String> instanceGroupNameCandidates(String instanceGroupName, Long stackId) {
        String instanceGroupNameWithoutAz = StringUtils.substringBeforeLast(instanceGroupName, DELIMITER);
        if (!instanceGroupNameWithoutAz.endsWith(DELIMITER + stackId)) {
            return List.of(instanceGroupName, instanceGroupNameWithoutAz);
        }
        String instanceGroupNameWithoutStackId = StringUtils.substringBeforeLast(instanceGroupNameWithoutAz, DELIMITER);
        return List.of(instanceGroupName, instanceGroupNameWithoutAz, instanceGroupNameWithoutStackId);
    }

    private Optional<InstanceGroupsListInstances> listInstancesIfInstanceGroupExists(Compute compute, String projectId, String zone, String instanceGroupName)
            throws IOException {
        try {
            return Optional.of(compute.instanceGroups().listInstances(projectId, zone, instanceGroupName, new InstanceGroupsListInstancesRequest()).execute());
        } catch (GoogleJsonResponseException e) {
            if (GcpExceptionUtil.resourceNotFoundException(e)) {
                return Optional.empty();
            }
            LOGGER.warn("Failed to list instances of instance group [{}] in project [{}] zone [{}]", instanceGroupName, projectId, zone, e);
            throw e;
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

    private Set<String> getAvailabilityZones(Group group, ResourceBuilderContext context) {
        Set<String> availabilityZones = new HashSet<>();
        availabilityZones.add(context.getLocation().getAvailabilityZone().value());
        if (!CollectionUtils.isEmpty(group.getNetwork().getAvailabilityZones())) {
            return group.getNetwork().getAvailabilityZones();
        }
        return availabilityZones;
    }
}
