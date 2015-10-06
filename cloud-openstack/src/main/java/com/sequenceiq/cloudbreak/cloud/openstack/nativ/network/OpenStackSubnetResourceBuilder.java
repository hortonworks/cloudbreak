package com.sequenceiq.cloudbreak.cloud.openstack.nativ.network;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.OS4JException;
import org.openstack4j.model.compute.ActionResponse;
import org.openstack4j.model.network.IPVersionType;
import org.openstack4j.model.network.Subnet;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants;
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.OpenStackResourceException;
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.context.OpenStackContext;
import com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView;
import com.sequenceiq.cloudbreak.cloud.openstack.view.NeutronNetworkView;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@Service
public class OpenStackSubnetResourceBuilder extends AbstractOpenStackNetworkResourceBuilder {
    @Override
    public CloudResource build(OpenStackContext context, AuthenticatedContext auth, Network network, Security security, CloudResource resource)
            throws Exception {
        try {
            OSClient osClient = createOSClient(auth);
            KeystoneCredentialView credentialView = new KeystoneCredentialView(auth.getCloudCredential());
            NeutronNetworkView networkView = new NeutronNetworkView(network);
            Subnet subnet = Builders.subnet().name(resource.getName())
                    .networkId(context.getParameter(OpenStackConstants.NETWORK_ID, String.class))
                    .tenantId(context.getStringParameter(OpenStackConstants.TENANT_ID))
                    .ipVersion(IPVersionType.V4)
                    .cidr(networkView.getSubnetCIDR())
                    .enableDHCP(true)
                    .build();
            subnet = osClient.networking().subnet().create(subnet);
            context.putParameter(OpenStackConstants.SUBNET_ID, subnet.getId());
            return createPersistedResource(resource, subnet.getId());
        } catch (OS4JException ex) {
            throw new OpenStackResourceException("Subnet creation failed", resourceType(), resource.getName(), ex);
        }
    }

    @Override
    public CloudResource delete(OpenStackContext context, AuthenticatedContext auth, CloudResource resource) throws Exception {
        try {
            OSClient osClient = createOSClient(auth);
            ActionResponse response = osClient.networking().subnet().delete(resource.getReference());
            return checkDeleteResponse(response, resourceType(), auth, resource, "Subnet deletion failed");
        } catch (OS4JException ex) {
            throw new OpenStackResourceException("Subnet deletion failed", resourceType(), resource.getName(), ex);
        }
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.OPENSTACK_SUBNET;
    }

    @Override
    protected boolean checkStatus(OpenStackContext context, AuthenticatedContext auth, CloudResource resource) {
        return true;
    }
}
