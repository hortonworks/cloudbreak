package com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer;

import static com.sequenceiq.cloudbreak.cloud.service.CloudbreakResourceNameService.DELIMITER;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.services.compute.model.Address;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.compute.GcpReservedIpResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.LoadBalancerTypeAttribute;
import com.sequenceiq.common.api.type.ResourceType;

/**
 * The loadbalancer version of creating a reserved IP address, where possible use {@link GcpReservedIpResourceBuilder}
 * Used to reserve an IP address to act as the destination for the load balancer forwarding rule
 */
@Service
public class GcpLoadBalancingIpResourceBuilder extends AbstractGcpLoadBalancerBuilder {

    private static final int ORDER = 3;

    private static final int KNOX_PORT = 8443;

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpLoadBalancingIpResourceBuilder.class);

    @Inject
    private GcpReservedIpResourceBuilder reservedIpResourceBuilder;

    @Inject
    private GcpLoadBalancerTypeConverter gcpLoadBalancerTypeConverter;

    @Inject
    private PersistenceNotifier resourceNotifier;

    @Override
    public List<CloudResource> create(GcpContext context, AuthenticatedContext auth, CloudLoadBalancer loadBalancer, Network network) {
        List<CloudResource> resourceFromDb = fetchAllResourceFromDb(resourceType(), auth.getCloudContext().getId());
        LOGGER.debug("Existing resources with type [{}] and Loadbalancer type [{}]: {}", resourceType(), loadBalancer.getType(), resourceFromDb);
        String lbTypePart = DELIMITER + getResourceNameService().normalize(getResourceNameService().getInitials(loadBalancer.getType().name())) + DELIMITER;
        Integer hcPort = loadBalancer.getPortToTargetGroupMapping().keySet().stream().map(TargetGroupPortPair::getHealthCheckPort).findFirst().orElse(KNOX_PORT);
        List<CloudResource> resourcesWithRightName = resourceFromDb.stream()
                .filter(resource -> resource.getName().contains(mapPortToPortPart(hcPort)) && resource.getName().contains(lbTypePart))
                .toList();
        String resourceName =
                getResourceNameService().instance(auth.getCloudContext().getName(), loadBalancer.getType().name(), hcPort.toString());
        Map<Boolean, List<CloudResource>> partitionedResources = resourcesWithRightName.stream()
                .collect(Collectors.partitioningBy(resourceHasLbTypeSpecified()));
        List<CloudResource> resourcesWithLbType = partitionedResources.get(true);
        List<CloudResource> resourcesWithoutLbType = partitionedResources.get(false);
        List<CloudResource> updatedResources = updateResourceWithoutLbType(context, resourcesWithoutLbType, loadBalancer, hcPort, auth.getCloudContext());
        List<CloudResource> resourcesWithoutLbTypeDoesntExistOnGcp = resourcesWithoutLbType.stream()
                .filter(resource -> updatedResources.stream()
                        .noneMatch(updatedResource -> updatedResource.getName().equals(resource.getName())))
                .toList();
        Map<String, Object> parameters = enrichParametersWithAttributes(Map.of(HCPORT, hcPort), loadBalancer.getType());
        Optional<CloudResource> resourceWithoutTypeButMatchingName = resourcesWithoutLbTypeDoesntExistOnGcp.stream().findFirst();
        CloudResource cloudResources = Stream.concat(resourcesWithLbType.stream(), updatedResources.stream())
                .filter(resource -> loadBalancer.getType().name()
                        .equals(resource.getParameter(CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.class).name()))
                .findFirst()
                .or(() -> resourceWithoutTypeButMatchingName.map(cloudResource ->
                        CloudResource.builder()
                                .cloudResource(cloudResource)
                                .withParameters(parameters)
                                .withPersistent(true)
                                .build()))
                .orElseGet(() ->
                        CloudResource.builder()
                                .withType(resourceType())
                                .withName(resourceName)
                                .withParameters(parameters)
                                .build());
        LOGGER.debug("Created cloud resources with type [{}] and Loadbalancer type [{}]: {}", resourceType(), loadBalancer.getType(), cloudResources);
        return List.of(cloudResources);
    }

    private Predicate<CloudResource> resourceHasLbTypeSpecified() {
        return resource -> resource.getParameters() != null
                && resource.getParameter(CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.class) != null;
    }

    private List<CloudResource> updateResourceWithoutLbType(GcpContext context, List<CloudResource> resourcesWithoutLbType, CloudLoadBalancer loadBalancer,
            Integer hcPort, CloudContext cloudContext) {
        List<CloudResource> updatedResources = resourcesWithoutLbType.stream()
                .map(resource -> tryUpdateResource(context, loadBalancer, hcPort, resource))
                .flatMap(Optional::stream)
                .toList();
        if (!updatedResources.isEmpty()) {
            resourceNotifier.notifyUpdates(updatedResources, cloudContext);
        }
        return updatedResources;
    }

    private Optional<CloudResource> tryUpdateResource(GcpContext context, CloudLoadBalancer loadBalancer, Integer hcPort, CloudResource resource) {
        String projectId = context.getProjectId();
        String region = context.getLocation().getRegion().value();
        try {
            Optional<Address> addressFromProvider = fetchFromProvider(() ->
                    context.getCompute().addresses().get(projectId, region, resource.getName()).execute(), resource.getName());
            return addressFromProvider.map(address -> {
                LoadBalancerType loadBalancerType = calculateLbTypeBasedOnGcpAddressType(loadBalancer, resource, address);
                LOGGER.info("Calculated LB type: {}", loadBalancerType);
                Map<String, Object> parameters = enrichParametersWithAttributes(Map.of(HCPORT, hcPort), loadBalancerType);
                return CloudResource.builder().cloudResource(resource).withParameters(parameters).withPersistent(true).build();
            });
        } catch (IOException e) {
            LOGGER.error("Failed to get address from provider for {}", resource, e);
            throw new GcpResourceException("Failed to get address from provider for " + resource.getName(), e);
        }
    }

    private LoadBalancerType calculateLbTypeBasedOnGcpAddressType(CloudLoadBalancer loadBalancer, CloudResource resource, Address addressFromProvider) {
        String addressType = addressFromProvider.getAddressType();
        String gcpType = gcpLoadBalancerTypeConverter.getScheme(loadBalancer).getGcpType();
        LOGGER.info("Calculating LB type for {} based on address type [{}] and LB GCP type [{}]", resource, addressType, gcpType);
        if (gcpType.equalsIgnoreCase(addressType)) {
            return loadBalancer.getType();
        } else {
            if (GcpLoadBalancerScheme.INTERNAL.getGcpType().equalsIgnoreCase(addressType)) {
                String lbGwPrivPart = DELIMITER
                        + getResourceNameService().normalize(getResourceNameService().getInitials(LoadBalancerType.GATEWAY_PRIVATE.name()))
                        + DELIMITER;
                if (resource.getName().contains(lbGwPrivPart)) {
                    return LoadBalancerType.GATEWAY_PRIVATE;
                } else {
                    return LoadBalancerType.PRIVATE;
                }
            } else {
                return LoadBalancerType.PUBLIC;
            }
        }
    }

    @Override
    public List<CloudResource> build(GcpContext context, AuthenticatedContext auth, List<CloudResource> buildableResources,
            CloudLoadBalancer loadBalancer, CloudStack cloudStack) throws Exception {
        return reservedIpResourceBuilder.buildReservedIp(context, buildableResources, cloudStack,
                gcpLoadBalancerTypeConverter.getScheme(loadBalancer));
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
