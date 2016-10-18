package com.sequenceiq.cloudbreak.cloud.openstack.nativ.network;

import static com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants.NETWORK_ID;

import java.util.List;

import javax.inject.Inject;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.OS4JException;
import org.openstack4j.model.compute.ActionResponse;
import org.openstack4j.model.network.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
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
public class OpenStackNetworkResourceBuilder extends AbstractOpenStackNetworkResourceBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackNetworkResourceBuilder.class);

    @Inject
    private OpenStackUtils utils;

    @Override
    public CloudResource build(OpenStackContext context, AuthenticatedContext auth, Network network, Security security, CloudResource buildableResource)
            throws Exception {
        OSClient osClient = createOSClient(auth);
        try {
            NeutronNetworkView neutronView = new NeutronNetworkView(network);
            String networkId = neutronView.isExistingNetwork() ? neutronView.getCustomNetworkId() : context.getParameter(NETWORK_ID, String.class);
            if (!neutronView.isExistingNetwork()) {
                org.openstack4j.model.network.Network osNetwork = Builders.network()
                        .name(buildableResource.getName())
                        .tenantId(context.getStringParameter(OpenStackConstants.TENANT_ID))
                        .adminStateUp(true)
                        .build();
                networkId = osClient.networking().network().create(osNetwork).getId();
            }
            context.putParameter(OpenStackConstants.NETWORK_ID, networkId);
            return createPersistedResource(buildableResource, networkId);
        } catch (OS4JException ex) {
            throw new OpenStackResourceException("Network creation failed", resourceType(), buildableResource.getName(), ex);
        }
    }

    @Override
    public CloudResource delete(OpenStackContext context, AuthenticatedContext auth, CloudResource resource, Network network) throws Exception {
        try {
            OSClient osClient = createOSClient(auth);
            deAllocateFloatingIps(context, osClient);
            NeutronNetworkView neutronView = new NeutronNetworkView(network);
            if (!neutronView.isExistingNetwork()) {
                ActionResponse response = osClient.networking().network().delete(resource.getReference());
                return checkDeleteResponse(response, resourceType(), auth, resource, "Network deletion failed");
            }
            return null;
        } catch (OS4JException ex) {
            throw new OpenStackResourceException("Network deletion failed", resourceType(), resource.getName(), ex);
        }
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.OPENSTACK_NETWORK;
    }

    @Override
    protected boolean checkStatus(OpenStackContext context, AuthenticatedContext auth, CloudResource resource) {
        CloudContext cloudContext = auth.getCloudContext();
        OSClient osClient = createOSClient(auth);
        org.openstack4j.model.network.Network osNetwork = osClient.networking().network().get(resource.getReference());
        if (osNetwork != null && context.isBuild()) {
            State networkStatus = osNetwork.getStatus();
            if (State.ERROR == networkStatus) {
                throw new OpenStackResourceException("Network in failed state", resource.getType(), resource.getName(), cloudContext.getId(),
                        networkStatus.name());
            }
            return networkStatus == State.ACTIVE;
        } else if (osNetwork == null && !context.isBuild()) {
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private void deAllocateFloatingIps(OpenStackContext context, OSClient osClient) {
        List<String> floatingIpIds = context.getParameter(OpenStackConstants.FLOATING_IP_IDS, List.class);
        for (String floatingIpId : floatingIpIds) {
            try {
                ActionResponse response = osClient.compute().floatingIps().deallocateIP(floatingIpId);
                if (!response.isSuccess()) {
                    LOGGER.warn("FloatingIp {} cannot be deallocated: {}", floatingIpId, response.getFault());
                }
            } catch (OS4JException ex) {
                LOGGER.warn("FloatingIp {} cannot be deallocated: {}", floatingIpId, ex.getMessage());
            }
        }
    }
}
