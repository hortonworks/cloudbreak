package com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.ForwardingRule;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpLabelUtil;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.HealthProbeParameters;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.common.api.type.LoadBalancerTypeAttribute;
import com.sequenceiq.common.api.type.ResourceType;

/**
 * Responsible for the CRUD operations of a GCP Forwarding Rule.
 * A Forwarding Rule maps an incoming request based on IP address, protocol and port, to a specific Backend Service
 */
@Service
public class GcpForwardingRuleResourceBuilder extends AbstractGcpLoadBalancerBuilder {

    public static final String GCP_BACKEND_SERVICE_REF_FORMAT = "https://www.googleapis.com/compute/v1/projects/%s/regions/%s/backendServices/%s";

    public static final String GCP_IP_REF_FORMAT = "https://www.googleapis.com/compute/v1/projects/%s/regions/%s/addresses/%s";

    public static final String TCP = "TCP";

    private static final int ORDER = 4;

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpForwardingRuleResourceBuilder.class);

    @Inject
    private GcpLabelUtil gcpLabelUtil;

    @Inject
    private GcpStackUtil gcpStackUtil;

    @Inject
    private GcpLoadBalancerTypeConverter gcpLoadBalancerTypeConverter;

    @Override
    public List<CloudResource> create(GcpContext context, AuthenticatedContext auth, CloudLoadBalancer loadBalancer, Network network) {
        Map<HealthProbeParameters, GcpLBTrafficsMap> hcPortToTrafficPorts = new HashMap<>();
        loadBalancer.getPortToTargetGroupMapping().keySet().forEach(targetGroupPortPair -> hcPortToTrafficPorts
                .computeIfAbsent(targetGroupPortPair.getHealthProbeParameters(), lbHealthCheck -> new GcpLBTrafficsMap())
                .addTraffic(targetGroupPortPair.getTrafficProtocol(), targetGroupPortPair.getTrafficPort()));
        return hcPortToTrafficPorts.entrySet().stream()
                .map(trafficMapEntry -> Pair.of(trafficMapEntry.getKey(), trafficMapEntry.getValue().getTraffics()))
                .flatMap(trafficsEntry -> trafficsEntry.getValue().stream()
                        .map(traffics -> Pair.of(trafficsEntry.getKey(), traffics)))
                .map(trafficsByHealth -> createCloudResource(context, loadBalancer, trafficsByHealth))
                .toList();
    }

    private CloudResource createCloudResource(GcpContext context, CloudLoadBalancer loadBalancer, Pair<HealthProbeParameters, GcpLBTraffics> trafficsByHealth) {
        HealthProbeParameters lbHealthCheck = trafficsByHealth.getKey();
        GcpLBTraffics lbTraffics = trafficsByHealth.getValue();
        String resourceName = getResourceNameService().loadBalancerWithProtocolAndPort(context.getName(), loadBalancer.getType(),
                lbTraffics.trafficProtocol().name(), lbHealthCheck.getPort());
        Map<String, Object> parameters = Map.of(
                TRAFFICPORTS, lbTraffics,
                HCPORT, lbHealthCheck,
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
        String projectId = context.getProjectId();
        String regionName = context.getLocation().getRegion().getRegionName();
        Network network = cloudStack.getNetwork();
        List<CloudResource> results = new ArrayList<>();
        GcpLoadBalancerScheme scheme = gcpLoadBalancerTypeConverter.getScheme(loadBalancer);

        List<CloudResource> backendResources = filterResourcesByType(context.getLoadBalancerResources(loadBalancer.getType()), ResourceType.GCP_BACKEND_SERVICE);
        List<CloudResource> ipResources = filterResourcesByType(context.getLoadBalancerResources(loadBalancer.getType()), ResourceType.GCP_RESERVED_IP);

        for (CloudResource buildableResource : buildableResources) {
            LOGGER.debug("Building forwarding rule {} for {}", buildableResource.getName(), projectId);
            ForwardingRule forwardingRule = new ForwardingRule().setIPProtocol(TCP)
                    .setName(buildableResource.getName())
                    .setLoadBalancingScheme(scheme.getGcpType());
            GcpLBTraffics traffics = buildableResource.getParameter(TRAFFICPORTS, GcpLBTraffics.class);

            List<String> ports = traffics.trafficPorts().stream().map(Object::toString).toList();
            forwardingRule.setPorts(ports);
            forwardingRule.setIPProtocol(convertProtocolWithTcpFallback(traffics.trafficProtocol()));
            if (scheme.equals(GcpLoadBalancerScheme.INTERNAL) || scheme.equals(GcpLoadBalancerScheme.GATEWAY_INTERNAL)) {
                String sharedProjectId = gcpStackUtil.getSharedProjectId(network);
                String networkUrl;
                String subnetUrl;
                if (StringUtils.isEmpty(sharedProjectId)) {
                    networkUrl = gcpStackUtil.getNetworkUrl(projectId, gcpStackUtil.getCustomNetworkId(network));
                    subnetUrl = gcpStackUtil.getSubnetUrl(projectId, regionName, getForwardingRuleSubnet(network, scheme));
                } else {
                    networkUrl = gcpStackUtil.getNetworkUrl(sharedProjectId, gcpStackUtil.getCustomNetworkId(network));
                    subnetUrl = gcpStackUtil.getSubnetUrl(sharedProjectId, regionName, getForwardingRuleSubnet(network, scheme));
                }
                forwardingRule.setNetwork(networkUrl);
                forwardingRule.setSubnetwork(subnetUrl);
                forwardingRule.setAllowGlobalAccess(true);
                LOGGER.debug("Set network to '{}' and subnet to '{}' for {} load balancer", networkUrl, subnetUrl, scheme);
            }

            HealthProbeParameters healthCheck = buildableResource.getParameter(HCPORT, HealthProbeParameters.class);
            Optional<String> backendName = backendResources.stream()
                    .filter(backendResource -> healthCheck.equals(backendResource.getParameter(HCPORT, HealthProbeParameters.class)))
                    .findFirst()
                    .map(CloudResource::getName);

            if (!backendName.isPresent()) {
                LOGGER.warn("backend not found for forwarding rule {}, port {}, project {}", buildableResource.getName(), healthCheck, projectId);
                continue;
            } else {
                forwardingRule.setBackendService(String.format(GCP_BACKEND_SERVICE_REF_FORMAT, projectId, regionName, backendName.get()));
            }

            if (ipResources.isEmpty()) {
                LOGGER.warn("No reserved IP address for loadbalancer {}-{}, using ephemeral", loadBalancer.getType(), projectId);
            } else {
                forwardingRule.setIPAddress(String.format(GCP_IP_REF_FORMAT, projectId, regionName, ipResources.get(0).getName()));
            }

            forwardingRule.setUnknownKeys(new HashMap<>(gcpLabelUtil.createLabelsFromTags(cloudStack)));
            Compute.ForwardingRules.Insert insert = context.getCompute().forwardingRules().insert(projectId, regionName, forwardingRule);
            results.add(doOperationalRequest(buildableResource, insert));

        }
        return results;
    }

    private String getForwardingRuleSubnet(Network network, GcpLoadBalancerScheme scheme) {
        if (scheme == GcpLoadBalancerScheme.GATEWAY_INTERNAL) {
            String endpointGwSubnetId = Objects.requireNonNullElse(gcpStackUtil.getEndpointGatewaySubnetId(network), gcpStackUtil.getSubnetId(network));
            LOGGER.debug("Setting {} as subnet for {} load balancer.", endpointGwSubnetId, scheme);
            return endpointGwSubnetId;
        }
        LOGGER.debug("Setting {} as subnet for {} load balancer.", gcpStackUtil.getSubnetId(network), scheme);
        return gcpStackUtil.getSubnetId(network);
    }

    @Override
    public CloudResource delete(GcpContext context, AuthenticatedContext auth, CloudResource resource) throws Exception {
        String regionName = context.getLocation().getRegion().getRegionName();
        LOGGER.info("Deleting forwarding rule {} for {}", resource.getName(), context.getProjectId());
        Compute.ForwardingRules.Delete delete = context.getCompute().forwardingRules().delete(context.getProjectId(), regionName, resource.getName());
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
        return ResourceType.GCP_FORWARDING_RULE;
    }

    @Override
    public int order() {
        return ORDER;
    }
}
