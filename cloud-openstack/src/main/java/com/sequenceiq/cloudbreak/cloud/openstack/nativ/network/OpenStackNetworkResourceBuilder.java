package com.sequenceiq.cloudbreak.cloud.openstack.nativ.network;

import java.util.List;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.OS4JException;
import org.openstack4j.model.compute.ActionResponse;
import org.openstack4j.model.network.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.openstack.OpenStackConstants;
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.OpenStackResourceException;
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.context.OpenStackContext;
import com.sequenceiq.cloudbreak.domain.ResourceType;

@Service
public class OpenStackNetworkResourceBuilder extends AbstractOpenStackNetworkResourceBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackNetworkResourceBuilder.class);

    @Override
    public CloudResource build(OpenStackContext context, AuthenticatedContext auth, Network network, Security security, CloudResource buildableResource)
            throws Exception {
        OSClient osClient = createOSClient(auth);
        try {
            org.openstack4j.model.network.Network osNetwork = Builders.network()
                    .name(buildableResource.getName())
                    .tenantId(context.getStringParameter(OpenStackConstants.TENANT_ID))
                    .adminStateUp(true)
                    .build();
            osNetwork = osClient.networking().network().create(osNetwork);
            context.putParameter(OpenStackConstants.NETWORK_ID, osNetwork.getId());
            return createPersistedResource(buildableResource, osNetwork.getId());
        } catch (OS4JException ex) {
            throw new OpenStackResourceException("Network creation failed", resourceType(), buildableResource.getName(), ex);
        }
    }

    @Override
    public CloudResource delete(OpenStackContext context, AuthenticatedContext auth, CloudResource resource) throws Exception {
        try {
            OSClient osClient = createOSClient(auth);
            deAllocateFloatingIps(context, osClient);
            ActionResponse response = osClient.networking().network().delete(resource.getReference());
            return checkDeleteResponse(response, resourceType(), auth, resource, "Network deletion failed");
        } catch (OS4JException ex) {
            throw new OpenStackResourceException("Network deletion failed", resourceType(), resource.getName(), ex);
        }
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.OPENSTACK_NETWORK;
    }

    @Override
    public int order() {
        return 0;
    }

    @Override
    protected boolean checkStatus(OpenStackContext context, AuthenticatedContext auth, CloudResource resource) {
        CloudContext cloudContext = auth.getCloudContext();
        OSClient osClient = createOSClient(auth);
        org.openstack4j.model.network.Network osNetwork = osClient.networking().network().get(resource.getReference());
        if (osNetwork != null && context.isBuild()) {
            State networkStatus = osNetwork.getStatus();
            if (State.ERROR == networkStatus) {
                throw new OpenStackResourceException("Network in failed state", resource.getType(), resource.getName(), cloudContext.getStackId(),
                        networkStatus.name());
            }
            return networkStatus == State.ACTIVE;
        } else if (osNetwork == null && !context.isBuild()) {
            return true;
        }
        return false;
    }

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
