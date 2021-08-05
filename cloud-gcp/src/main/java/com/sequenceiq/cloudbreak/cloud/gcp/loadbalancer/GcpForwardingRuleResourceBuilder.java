package com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

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
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.common.api.type.ResourceType;

/**
 * Responsible for the CRUD operations of a GCP Forwarding Rule.
 * A Forwarding Rule maps an incoming request based on IP address, port, to a specific Backend Service
 * Only one can exist for a given IP address and port combination in a subnet
 *
 */
@Service
public class GcpForwardingRuleResourceBuilder extends AbstractGcpLoadBalancerBuilder {

    public static final String GCP_BACKEND_SERVICE_REF_FORMAT = "https://www.googleapis.com/compute/v1/projects/%s/regions/%s/backendServices/%s";

    public static final String GCP_IP_REF_FORMAT = "https://www.googleapis.com/compute/v1/projects/%s/regions/%s/addresses/%s";

    public static final String TCP = "TCP";

    private static final int ORDER = 4;

    private static final int MAX_LABEL_LENGTH = 63;

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpForwardingRuleResourceBuilder.class);

    @Inject
    private GcpLabelUtil gcpLabelUtil;

    @Inject
    private GcpStackUtil gcpStackUtil;

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
        Network network = cloudStack.getNetwork();
        List<CloudResource> results = new ArrayList<>();
        GcpLoadBalancerScheme scheme = GcpLoadBalancerScheme.getScheme(loadBalancer);

        List<CloudResource> backendResources = filterResourcesByType(context.getLoadBalancerResources(loadBalancer.getType()), ResourceType.GCP_BACKEND_SERVICE);
        List<CloudResource> ipResources = filterResourcesByType(context.getLoadBalancerResources(loadBalancer.getType()), ResourceType.GCP_RESERVED_IP);

        for (CloudResource buildableResource : buildableResources) {
            Integer trafficPort = buildableResource.getParameter(TRAFFICPORT, Integer.class);
            ForwardingRule forwardingRule = new ForwardingRule().setIPProtocol(TCP)
                    .setName(buildableResource.getName())
                    .setPorts(List.of(String.valueOf(trafficPort)));
            forwardingRule.setLoadBalancingScheme(scheme.getGcpType());
            if (scheme.equals(GcpLoadBalancerScheme.INTERNAL)) {
                forwardingRule.setSubnetwork(gcpStackUtil.getNetworkUrl(projectId, gcpStackUtil.getCustomNetworkId(network)));
                forwardingRule.setSubnetwork(gcpStackUtil.getSubnetUrl(projectId, regionName, gcpStackUtil.getSubnetId(network)));
            }

            Optional<String> backendName = backendResources.stream()
                    .filter(backendResource -> trafficPort.equals(backendResource.getParameter(TRAFFICPORT, Integer.class)))
                    .findFirst()
                    .map(CloudResource::getName);

            if (!backendName.isPresent()) {
                LOGGER.warn("backend not found for forwarding rule {}, port {}, project {}", buildableResource.getName(), trafficPort, projectId);
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
