package com.sequenceiq.cloudbreak.cloud.gcp.network;

import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getCustomNetworkId;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getSubnetId;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.legacyNetwork;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.newNetworkAndSubnet;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.newSubnetInExistingNetwork;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.noFirewallRules;

import java.util.Arrays;
import java.util.Collections;

import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Firewall;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.template.ResourceNotNeededException;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

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
        Firewall.Allowed allowed1 = new Firewall.Allowed();
        allowed1.setIPProtocol("tcp");
        allowed1.setPorts(Collections.singletonList("1-65535"));

        Firewall.Allowed allowed2 = new Firewall.Allowed();
        allowed2.setIPProtocol("icmp");

        Firewall.Allowed allowed3 = new Firewall.Allowed();
        allowed3.setIPProtocol("udp");
        allowed3.setPorts(Collections.singletonList("1-65535"));

        firewall.setTargetTags(Collections.singletonList(GcpStackUtil.getClusterTag(auth.getCloudContext())));
        firewall.setAllowed(Arrays.asList(allowed1, allowed2, allowed3));
        firewall.setName(buildableResource.getName());
        if (legacyNetwork(network)) {
            Compute.Networks.Get networkRequest = context.getCompute().networks().get(projectId, getCustomNetworkId(network));
            com.google.api.services.compute.model.Network existingNetwork = networkRequest.execute();
            firewall.setSourceRanges(Collections.singletonList(existingNetwork.getIPv4Range()));
        } else if (newNetworkAndSubnet(network) || newSubnetInExistingNetwork(network)) {
            firewall.setSourceRanges(Collections.singletonList(network.getSubnet().getCidr()));
        } else {
            Compute.Subnetworks.Get sn = context.getCompute().subnetworks().get(projectId, context.getLocation().getRegion().value(), getSubnetId(network));
            com.google.api.services.compute.model.Subnetwork existingSubnet = sn.execute();
            firewall.setSourceRanges(Collections.singletonList(existingSubnet.getIpCidrRange()));
        }
        firewall.setNetwork(String.format("https://www.googleapis.com/compute/v1/projects/%s/global/networks/%s", projectId,
                context.getParameter(GcpNetworkResourceBuilder.NETWORK_NAME, String.class)));

        Compute.Firewalls.Insert firewallInsert = context.getCompute().firewalls().insert(projectId, firewall);
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
