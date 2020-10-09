package com.sequenceiq.cloudbreak.cloud.gcp.group;

import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getSharedProjectId;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.isExistingNetwork;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.noFirewallRules;
import static com.sequenceiq.common.api.type.ResourceType.GCP_FIREWALL_IN;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.Firewalls.Update;
import com.google.api.services.compute.ComputeRequest;
import com.google.api.services.compute.model.Firewall;
import com.google.api.services.compute.model.Firewall.Allowed;
import com.google.api.services.compute.model.Operation;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.network.GcpNetworkResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.PortDefinition;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.model.SecurityRule;
import com.sequenceiq.cloudbreak.cloud.template.ResourceNotNeededException;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class GcpFirewallInResourceBuilder extends AbstractGcpGroupBuilder {

    private static final int ORDER = 0;

    private static final String ICMP = "icmp";

    @Override
    public CloudResource create(GcpContext context, AuthenticatedContext auth, Group group, Network network) {
        if (noFirewallRules(network)) {
            throw new ResourceNotNeededException("Firewall rules won't be created.");
        }
        String resourceName = getResourceNameService().resourceName(resourceType(), context.getName());
        return createNamedResource(resourceType(), resourceName);
    }

    @Override
    public CloudResource build(GcpContext context, AuthenticatedContext auth, Group group, Network network, Security security, CloudResource buildableResource)
            throws Exception {
        String projectId = context.getProjectId();

        ComputeRequest<Operation> firewallRequest = StringUtils.isNotBlank(security.getCloudSecurityId()) && isExistingNetwork(network)
                ? updateExistingFirewallForNewTargets(context, auth, group, security)
                : createNewFirewallRule(context, auth, group, network, security, buildableResource, projectId);
        try {
            Operation operation = firewallRequest.execute();
            if (operation.getHttpErrorStatusCode() != null) {
                throw new GcpResourceException(operation.getHttpErrorMessage(), resourceType(), buildableResource.getName());
            }
            return createOperationAwareCloudResource(buildableResource, operation);
        } catch (GoogleJsonResponseException e) {
            throw new GcpResourceException(checkException(e), resourceType(), buildableResource.getName());
        }
    }

    private Update updateExistingFirewallForNewTargets(GcpContext context, AuthenticatedContext auth, Group group, Security security)
            throws java.io.IOException {
        Firewall firewall = context.getCompute().firewalls().get(context.getProjectId(), security.getCloudSecurityId()).execute();
        if (firewall.getTargetTags() == null) {
            firewall.setTargetTags(Lists.newArrayListWithCapacity(1));
        }
        firewall.getTargetTags().add(GcpStackUtil.getGroupClusterTag(auth.getCloudContext(), group));
        return context.getCompute().firewalls().update(context.getProjectId(), firewall.getName(), firewall);
    }

    private ComputeRequest<Operation> createNewFirewallRule(GcpContext context, AuthenticatedContext auth, Group group, Network network, Security security,
            CloudResource buildableResource, String projectId) throws IOException {
        List<String> sourceRanges = getSourceRanges(security);

        Firewall firewall = new Firewall();
        firewall.setSourceRanges(sourceRanges);
        firewall.setDescription(description());

        List<Allowed> allowedRules = new ArrayList<>(createAllowedRules(security));

        firewall.setTargetTags(Collections.singletonList(GcpStackUtil.getGroupClusterTag(auth.getCloudContext(), group)));
        firewall.setAllowed(allowedRules);
        firewall.setName(buildableResource.getName());
        if (isNotEmpty(getSharedProjectId(network))) {
            firewall.setNetwork(String.format("https://www.googleapis.com/compute/v1/projects/%s/global/networks/%s",
                    getSharedProjectId(network),
                    context.getParameter(GcpNetworkResourceBuilder.NETWORK_NAME, String.class)));
        } else {
            firewall.setNetwork(String.format("https://www.googleapis.com/compute/v1/projects/%s/global/networks/%s", projectId,
                    context.getParameter(GcpNetworkResourceBuilder.NETWORK_NAME, String.class)));
        }

        return context.getCompute().firewalls().insert(projectId, firewall);
    }

    @Override
    public CloudResourceStatus update(GcpContext context, AuthenticatedContext auth, Group group, Network network, Security security, CloudResource resource) {
        String projectId = context.getProjectId();
        Compute compute = context.getCompute();
        String resourceName = resource.getName();
        try {
            Firewall fireWall = compute.firewalls().get(projectId, resourceName).execute();
            List<String> sourceRanges = getSourceRanges(security);
            fireWall.setSourceRanges(sourceRanges);
            Operation operation = compute.firewalls().update(projectId, resourceName, fireWall).execute();
            CloudResource cloudResource = createOperationAwareCloudResource(resource, operation);
            return checkResources(context, auth, Collections.singletonList(cloudResource)).get(0);
        } catch (IOException e) {
            throw new GcpResourceException("Failed to update resource!", GCP_FIREWALL_IN, resourceName, e);
        }
    }

    @Override
    public CloudResource delete(GcpContext context, AuthenticatedContext auth, CloudResource resource, Network network) throws Exception {
        try {
            Operation operation = context.getCompute().firewalls().delete(context.getProjectId(), resource.getName()).execute();
            return createOperationAwareCloudResource(resource, operation);
        } catch (GoogleJsonResponseException e) {
            exceptionHandler(e, resource.getName(), resourceType());
            return null;
        }
    }

    @Override
    public ResourceType resourceType() {
        return GCP_FIREWALL_IN;
    }

    @Override
    public int order() {
        return ORDER;
    }

    private List<String> getSourceRanges(Security security) {
        List<SecurityRule> rules = security.getRules();
        List<String> sourceRanges = new ArrayList<>(rules.size());
        for (SecurityRule securityRule : rules) {
            sourceRanges.add(securityRule.getCidr());
        }
        return sourceRanges;
    }

    private Collection<Allowed> createAllowedRules(Security security) {
        Collection<Allowed> rules = new LinkedList<>();
        List<SecurityRule> securityRules = security.getRules();
        for (SecurityRule securityRule : securityRules) {
            Allowed rule = new Allowed();
            rule.setIPProtocol(securityRule.getProtocol());
            if (!ICMP.equalsIgnoreCase(securityRule.getProtocol())) {
                List<String> ports = new ArrayList<>();
                for (PortDefinition portDefinition : securityRule.getPorts()) {
                    if (portDefinition.isRange()) {
                        ports.add(String.format("%s-%s", portDefinition.getFrom(), portDefinition.getTo()));
                    } else {
                        ports.add(portDefinition.getFrom());
                    }
                }
                rule.setPorts(ports);
            }
            rules.add(rule);
        }
        return rules;
    }
}
