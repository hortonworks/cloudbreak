package com.sequenceiq.cloudbreak.cloud.openstack.nativ.network;

import static com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants.SUBNET_ID;

import javax.inject.Inject;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.OS4JException;
import org.openstack4j.model.compute.ActionResponse;
import org.openstack4j.model.network.IPVersionType;
import org.openstack4j.model.network.Subnet;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackUtils;
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.OpenStackResourceException;
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.context.OpenStackContext;
import com.sequenceiq.cloudbreak.cloud.openstack.view.NeutronNetworkView;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@Service
public class OpenStackSubnetResourceBuilder extends AbstractOpenStackNetworkResourceBuilder {

    @Inject
    private OpenStackUtils utils;

    @Override
    public CloudResource build(OpenStackContext context, AuthenticatedContext auth, Network network, Security security, CloudResource resource)
            throws Exception {
        try {
            String subnetId = utils.isExistingSubnet(network) ? utils.getCustomSubnetId(network) : context.getParameter(SUBNET_ID, String.class);
            if (!utils.isExistingSubnet(network)) {
                OSClient osClient = createOSClient(auth);
                NeutronNetworkView networkView = new NeutronNetworkView(network);
                Subnet subnet = Builders.subnet().name(resource.getName())
                        .networkId(context.getParameter(OpenStackConstants.NETWORK_ID, String.class))
                        .tenantId(context.getStringParameter(OpenStackConstants.TENANT_ID))
                        .ipVersion(IPVersionType.V4)
                        .cidr(networkView.getSubnetCIDR())
                        .enableDHCP(true)
                        .build();
                subnetId = osClient.networking().subnet().create(subnet).getId();
            }
            context.putParameter(OpenStackConstants.SUBNET_ID, subnetId);
            return createPersistedResource(resource, subnetId);
        } catch (OS4JException ex) {
            throw new OpenStackResourceException("Subnet creation failed", resourceType(), resource.getName(), ex);
        }
    }

    @Override
    public CloudResource delete(OpenStackContext context, AuthenticatedContext auth, CloudResource resource, Network network) throws Exception {
        try {
            if (!utils.isExistingSubnet(network)) {
                OSClient osClient = createOSClient(auth);
                ActionResponse response = osClient.networking().subnet().delete(resource.getReference());
                return checkDeleteResponse(response, resourceType(), auth, resource, "Subnet deletion failed");
            }
            return null;
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
