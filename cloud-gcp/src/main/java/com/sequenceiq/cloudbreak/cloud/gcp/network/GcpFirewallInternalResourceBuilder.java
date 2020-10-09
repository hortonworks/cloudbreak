package com.sequenceiq.cloudbreak.cloud.gcp.network;

import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getCustomNetworkId;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getSharedProjectId;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getSubnetId;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.isLegacyNetwork;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.isNewNetworkAndSubnet;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.isNewSubnetInExistingNetwork;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.noFirewallRules;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute.Firewalls.Insert;
import com.google.api.services.compute.Compute.Networks;
import com.google.api.services.compute.Compute.Subnetworks.Get;
import com.google.api.services.compute.model.Firewall;
import com.google.api.services.compute.model.Firewall.Allowed;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.template.ResourceNotNeededException;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class GcpFirewallInternalResourceBuilder extends AbstractGcpNetworkBuilder {

    @Override
    public CloudResource create(GcpContext context, AuthenticatedContext auth, Network network) {
        if (noFirewallRules(network)) {
            throw new ResourceNotNeededException("Firewall rules won't be created.");
        }
        String resourceName = getResourceNameService().resourceName(resourceType(), context.getName());
        return createNamedResource(resourceType(), resourceName);
    }

    @Override
    public CloudResource build(GcpContext context, AuthenticatedContext auth, Network network, Security security, CloudResource buildableResource)
            throws Exception {
        String projectId = context.getProjectId();

        Firewall firewall = new Firewall();
        firewall.setDescription(description());
        Allowed allowed1 = new Allowed();
        allowed1.setIPProtocol("tcp");
        allowed1.setPorts(Collections.singletonList("1-65535"));

        Allowed allowed2 = new Allowed();
        allowed2.setIPProtocol("icmp");

        Allowed allowed3 = new Allowed();
        allowed3.setIPProtocol("udp");
        allowed3.setPorts(Collections.singletonList("1-65535"));

        firewall.setTargetTags(Collections.singletonList(GcpStackUtil.getClusterTag(auth.getCloudContext())));
        firewall.setAllowed(Arrays.asList(allowed1, allowed2, allowed3));
        firewall.setName(buildableResource.getName());
        if (isLegacyNetwork(network)) {
            Networks.Get networkRequest = context.getCompute().networks().get(projectId, getCustomNetworkId(network));
            com.google.api.services.compute.model.Network existingNetwork = networkRequest.execute();
            firewall.setSourceRanges(Collections.singletonList(existingNetwork.getIPv4Range()));
        } else if (isNewNetworkAndSubnet(network) || isNewSubnetInExistingNetwork(network)) {
            firewall.setSourceRanges(Collections.singletonList(network.getSubnet().getCidr()));
        } else if (isNotEmpty(getSharedProjectId(network))) {
            Get sn = context.getCompute().subnetworks().get(getSharedProjectId(network), context.getLocation().getRegion().value(), getSubnetId(network));
            com.google.api.services.compute.model.Subnetwork existingSubnet = sn.execute();
            List<String> strings = new ArrayList<>();
            strings.add(existingSubnet.getIpCidrRange());
            firewall.setSourceRanges(strings);
        } else if (isNotEmpty(getSubnetId(network))) {
            Get sn = context.getCompute().subnetworks().get(projectId, context.getLocation().getRegion().value(), getSubnetId(network));
            com.google.api.services.compute.model.Subnetwork existingSubnet = sn.execute();
            firewall.setSourceRanges(Collections.singletonList(existingSubnet.getIpCidrRange()));
        }
        if (isNotEmpty(getSharedProjectId(network))) {
            firewall.setNetwork(String.format("https://www.googleapis.com/compute/v1/projects/%s/global/networks/%s",
                    getSharedProjectId(network),
                    context.getParameter(GcpNetworkResourceBuilder.NETWORK_NAME, String.class)));
        } else {
            firewall.setNetwork(String.format("https://www.googleapis.com/compute/v1/projects/%s/global/networks/%s", projectId,
                    context.getParameter(GcpNetworkResourceBuilder.NETWORK_NAME, String.class)));
        }
        Insert firewallInsert = context.getCompute().firewalls().insert(projectId, firewall);
        try {
            Operation operation = firewallInsert.execute();
            if (operation.getHttpErrorStatusCode() != null) {
                throw new GcpResourceException(operation.getHttpErrorMessage(), resourceType(), buildableResource.getName());
            }
            return createOperationAwareCloudResource(buildableResource, operation);
        } catch (GoogleJsonResponseException e) {
            throw new GcpResourceException(checkException(e), resourceType(), buildableResource.getName());
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
        return ResourceType.GCP_FIREWALL_INTERNAL;
    }

    @Override
    public int order() {
        return 2;
    }

}
