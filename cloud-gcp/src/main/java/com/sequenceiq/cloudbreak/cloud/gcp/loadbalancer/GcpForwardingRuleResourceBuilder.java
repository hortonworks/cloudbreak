package com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.ForwardingRule;
import com.google.api.services.compute.model.Operation;
import com.google.common.base.Functions;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpLabelUtil;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Network;
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

    @Inject
    private GcpLoadBalancerTypeConverter gcpLoadBalancerTypeConverter;

    @Override
    public List<CloudResource> create(GcpContext context, AuthenticatedContext auth, CloudLoadBalancer loadBalancer) {
        return getCloudResourcesForFrontendAndBackendCreate(context, loadBalancer);
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
            Map<String, Object> portMap = buildableResource.getParameters();
            ForwardingRule forwardingRule = new ForwardingRule().setIPProtocol(TCP)
                    .setName(buildableResource.getName())
                    .setLoadBalancingScheme(scheme.getGcpType());
            if (portMap.containsKey(TRAFFICPORTS)) {
                List<String> ports = Lists.transform((List<Integer>) portMap.get(TRAFFICPORTS), Functions.toStringFunction());
                forwardingRule.setPorts(ports);
            } else {
                Integer trafficPort = buildableResource.getParameter(TRAFFICPORT, Integer.class);
                forwardingRule.setPorts(List.of(String.valueOf(trafficPort)));
            }
            if (scheme.equals(GcpLoadBalancerScheme.INTERNAL)) {
                String sharedProjectId = gcpStackUtil.getSharedProjectId(network);
                if (StringUtils.isEmpty(sharedProjectId)) {
                    forwardingRule.setNetwork(gcpStackUtil.getNetworkUrl(projectId, gcpStackUtil.getCustomNetworkId(network)));
                    forwardingRule.setSubnetwork(gcpStackUtil.getSubnetUrl(projectId, regionName, gcpStackUtil.getSubnetId(network)));
                } else {
                    forwardingRule.setNetwork(gcpStackUtil.getNetworkUrl(sharedProjectId, gcpStackUtil.getCustomNetworkId(network)));
                    forwardingRule.setSubnetwork(gcpStackUtil.getSubnetUrl(sharedProjectId, regionName, gcpStackUtil.getSubnetId(network)));
                }
            }

            Integer hcPort = buildableResource.getParameter(HCPORT, Integer.class);
            Optional<String> backendName = backendResources.stream()
                    .filter(backendResource -> hcPort.equals(backendResource.getParameter(HCPORT, Integer.class)))
                    .findFirst()
                    .map(CloudResource::getName);

            if (!backendName.isPresent()) {
                LOGGER.warn("backend not found for forwarding rule {}, port {}, project {}", buildableResource.getName(), hcPort, projectId);
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
