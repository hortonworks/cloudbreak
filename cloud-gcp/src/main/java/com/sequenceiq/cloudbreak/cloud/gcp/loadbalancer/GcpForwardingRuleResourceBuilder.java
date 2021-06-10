package com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.ForwardingRule;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.network.GcpNetworkResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.gcp.network.GcpSubnetResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.ResourceType;

public class GcpForwardingRuleResourceBuilder extends AbstractGcpLoadBalancerBuilder {

    public static final String GCP_BACKEND_SERVICE_REF_FORMAT = "https://www.googleapis.com/compute/v1/projects/%s/regions/%s/backendServices/%s";

    public static final String GCP_IP_REF_FORMAT = "https://www.googleapis.com/compute/v1/projects/%s/regions/%s/addresses/%s";

    private static final int ORDER = 4;

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpForwardingRuleResourceBuilder.class);

    @Override
    public List<CloudResource> create(GcpContext context, AuthenticatedContext auth, CloudLoadBalancer loadBalancer) {
        List<CloudResource> resources = new ArrayList<>();
        for (TargetGroupPortPair targetGroupPortPair : loadBalancer.getPortToTargetGroupMapping().keySet()) {
            Integer healthCheckPort = targetGroupPortPair.getHealthCheckPort();
            String resourceName = getResourceNameService().resourceName(resourceType(), context.getName(),
                    loadBalancer.getType(), targetGroupPortPair.getTrafficPort());
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
        String projectId = context.getProjectId();
        String regionName = context.getLocation().getRegion().getRegionName();
        List<CloudResource> results = new ArrayList<>();


        List<CloudResource> backendResources = filterResourcesByType(context.getLoadBalancerResources(loadBalancer.getType()), ResourceType.GCP_BACKEND_SERVICE);
        List<CloudResource> ipResources = filterResourcesByType(context.getLoadBalancerResources(loadBalancer.getType()), ResourceType.GCP_RESERVED_IP);

        for (CloudResource buildableResource : buildableResources) {
            Integer trafficPort = buildableResource.getParameter(TRAFFICPORT, Integer.class);
            ForwardingRule forwardingRule = new ForwardingRule().setIPProtocol("TCP")
                    .setName(buildableResource.getName())
                    .setPorts(List.of(String.valueOf(trafficPort)));
            if (loadBalancer.getType().equals(LoadBalancerType.PRIVATE)) {
                forwardingRule.setLoadBalancingScheme("INTERNAL")
                        .setNetwork(context.getParameter(GcpNetworkResourceBuilder.NETWORK_NAME, String.class))
                        .setSubnetwork(context.getParameter(GcpSubnetResourceBuilder.SUBNET_NAME, String.class));
            } else {
                forwardingRule.setLoadBalancingScheme("EXTERNAL");
            }

            Optional<String> backendName = backendResources.stream()
                    .filter(backendResource -> trafficPort.equals(backendResource.getParameter(TRAFFICPORT, Integer.class)))
                    .findFirst()
                    .map(CloudResource::getName);

            if (!backendName.isPresent()) {
                LOGGER.warn("backend not found for fowarding rule {}, port {}, project {}", buildableResource.getName(), trafficPort, projectId);
                continue;
            } else {
                forwardingRule.setBackendService(String.format(GCP_BACKEND_SERVICE_REF_FORMAT, projectId, regionName, backendName.get()));
            }

            if (ipResources.isEmpty()) {
                LOGGER.warn("No reserved IP address for loadbalancer {}-{}, using empmeral", loadBalancer.getType(), projectId);
            } else {
                forwardingRule.setIPAddress(String.format(GCP_IP_REF_FORMAT, projectId, regionName, ipResources.get(0).getName()));

            }
            Compute.ForwardingRules.Insert insert = context.getCompute().forwardingRules().insert(projectId, regionName, forwardingRule);
            results.add(doOperationalRequest(buildableResource, insert));

        }
        return results;
    }

    @Override
    public CloudResource delete(GcpContext context, AuthenticatedContext auth, CloudResource resource) throws Exception {
        String regionName = context.getLocation().getRegion().getRegionName();

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
