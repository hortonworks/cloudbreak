package com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.Firewalls.Insert;
import com.google.api.services.compute.model.Firewall;
import com.google.api.services.compute.model.Firewall.Allowed;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.template.ResourceNotNeededException;
import com.sequenceiq.common.api.type.LoadBalancerTypeAttribute;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class GcpHealthCheckFirewallResourceBuilder extends AbstractGcpLoadBalancerBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(GcpHealthCheckFirewallResourceBuilder.class);

    @Value("${cb.gcp.loadbalancer.healthcheck.cidrs.internal}")
    private List<String> internalHealthCheckCidrs;

    @Value("${cb.gcp.loadbalancer.healthcheck.cidrs.external}")
    private List<String> externalHealthCheckCidrs;

    @Inject
    private GcpStackUtil gcpStackUtil;

    @Inject
    private GcpLoadBalancerTypeConverter gcpLoadBalancerTypeConverter;

    @Override
    public List<CloudResource> create(GcpContext context, AuthenticatedContext auth, CloudLoadBalancer loadBalancer, Network network) {
        if (gcpStackUtil.noFirewallRules(network)) {
            throw new ResourceNotNeededException("Firewall rules won't be created.");
        }
        Map<Integer, Set<String>> hcPortToGroups = loadBalancer.getPortToTargetGroupMapping().entrySet().stream()
                .collect(Collectors.groupingBy(
                        entry -> entry.getKey().getHealthCheckPort(),
                        Collectors.flatMapping(
                                entry -> entry.getValue().stream().map(Group::getName),
                                Collectors.toSet())));
        List<CloudResource> cloudResources = hcPortToGroups.entrySet().stream()
                .map(entry -> createCloudResource(context, loadBalancer, entry.getKey(), entry.getValue()))
                .toList();
        LOGGER.debug("Collected health firewall resources for loadbalancer {}: {}", loadBalancer, cloudResources);
        return cloudResources;
    }

    private CloudResource createCloudResource(GcpContext context, CloudLoadBalancer loadBalancer, Integer healthCheckPort,
            Set<String> groups) {
        String resourceName = getResourceNameService().loadBalancerWithPort(context.getName(), loadBalancer.getType(), healthCheckPort);
        Map<String, Object> parameters = Map.of(TRAFFICPORTS, groups, HCPORT, healthCheckPort,
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
        String sharedProjectId = gcpStackUtil.getSharedProjectId(cloudStack.getNetwork());
        for (CloudResource buildableResource : buildableResources) {
            Firewall firewall = new Firewall();
            firewall.setName(buildableResource.getName());
            firewall.setDescription(description());
            Allowed allowed = new Allowed()
                    .setIPProtocol("tcp")
                    .setPorts(List.of(buildableResource.getParameter(HCPORT, Integer.class).toString()));
            firewall.setAllowed(List.of(allowed));
            GcpLoadBalancerScheme scheme = gcpLoadBalancerTypeConverter.getScheme(loadBalancer);
            if (scheme.equals(GcpLoadBalancerScheme.INTERNAL) || scheme.equals(GcpLoadBalancerScheme.GATEWAY_INTERNAL)) {
                firewall.setSourceRanges(internalHealthCheckCidrs);
            } else {
                firewall.setSourceRanges(externalHealthCheckCidrs);
            }
            Set<String> groups = buildableResource.getParameter(TRAFFICPORTS, Set.class);
            firewall.setTargetTags(groups.stream()
                    .map(group -> gcpStackUtil.getGroupClusterTag(auth.getCloudContext(), group))
                    .toList());
            String networkUrl;
            if (isNotEmpty(sharedProjectId)) {
                networkUrl = gcpStackUtil.getNetworkUrl(sharedProjectId, gcpStackUtil.getCustomNetworkId(cloudStack.getNetwork()));
            } else {
                networkUrl = gcpStackUtil.getNetworkUrl(projectId, gcpStackUtil.getCustomNetworkId(cloudStack.getNetwork()));
            }
            firewall.setNetwork(networkUrl);
            Insert firewallInsert = context.getCompute().firewalls().insert(projectId, firewall);
            LOGGER.debug("Creating healthcheck firewall {} for {} healthcheck firewall resource", firewall, buildableResource.getName());
            results.add(doOperationalRequest(buildableResource, firewallInsert));
        }
        return results;
    }

    @Override
    public CloudResource delete(GcpContext context, AuthenticatedContext auth, CloudResource resource) throws Exception {
        LOGGER.info("deleting healthcheck firewall {} for {}", resource.getName(), context.getProjectId());
        Compute.Firewalls.Delete delete = context.getCompute().firewalls().delete(context.getProjectId(), resource.getName());
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
        return ResourceType.GCP_HEALTHCHECK_FIREWALL;
    }

    @Override
    public int order() {
        return 1;
    }
}
