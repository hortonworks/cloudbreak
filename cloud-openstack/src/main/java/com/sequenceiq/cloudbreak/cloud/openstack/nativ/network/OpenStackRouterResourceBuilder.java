package com.sequenceiq.cloudbreak.cloud.openstack.nativ.network;

import javax.inject.Inject;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.OS4JException;
import org.openstack4j.model.compute.ActionResponse;
import org.openstack4j.model.network.AttachInterfaceType;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.State;
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
import com.sequenceiq.cloudbreak.cloud.template.ResourceNotNeededException;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@Service
public class OpenStackRouterResourceBuilder extends AbstractOpenStackNetworkResourceBuilder {

    @Inject
    private OpenStackUtils utils;

    @Override
    public CloudResource create(OpenStackContext context, AuthenticatedContext auth, Network network) {
        if (utils.isExistingSubnet(network)) {
            throw new ResourceNotNeededException("Router isn't needed when a subnet is reused.");
        }
        return super.create(context, auth, network);
    }

    @Override
    public CloudResource build(OpenStackContext context, AuthenticatedContext auth, Network network, Security security, CloudResource resource)
            throws Exception {
        try {
            OSClient osClient = createOSClient(auth);
            NeutronNetworkView networkView = new NeutronNetworkView(network);
            String routerId = utils.getCustomRouterId(network);
            if (!utils.isExistingNetwork(network)) {
                Router router = Builders.router()
                        .name(resource.getName())
                        .adminStateUp(true)
                        .tenantId(context.getStringParameter(OpenStackConstants.TENANT_ID))
                        .externalGateway(networkView.getPublicNetId()).build();
                Router newRouter = osClient.networking().router().create(router);
                if (newRouter == null) {
                    throw new OpenStackResourceException("Router creation failed, maybe network does not exists", resourceType(), resource.getName());
                }
                routerId = newRouter.getId();
            }
            if (!utils.isExistingSubnet(network)) {
                osClient.networking().router().attachInterface(routerId, AttachInterfaceType.SUBNET, context.getStringParameter(OpenStackConstants.SUBNET_ID));
            }
            return createPersistedResource(resource, routerId);
        } catch (OS4JException ex) {
            throw new OpenStackResourceException("Router creation failed", resourceType(), resource.getName(), ex);
        }
    }

    @Override
    public CloudResource delete(OpenStackContext context, AuthenticatedContext auth, CloudResource resource, Network network) throws Exception {
        try {
            OSClient osClient = createOSClient(auth);
            String subnetId = context.getStringParameter(OpenStackConstants.SUBNET_ID);
            if (!utils.isExistingSubnet(network)) {
                osClient.networking().router().detachInterface(resource.getReference(), subnetId, null);
            }
            if (!utils.isExistingNetwork(network)) {
                ActionResponse response = osClient.networking().router().delete(resource.getReference());
                return checkDeleteResponse(response, resourceType(), auth, resource, "Router deletion failed");
            }
            return null;
        } catch (OS4JException ex) {
            throw new OpenStackResourceException("Router deletion failed", resourceType(), resource.getName(), ex);
        }
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.OPENSTACK_ROUTER;
    }

    @Override
    protected boolean checkStatus(OpenStackContext context, AuthenticatedContext auth, CloudResource resource) {
        CloudContext cloudContext = auth.getCloudContext();
        OSClient osClient = createOSClient(auth);
        Router osRouter = osClient.networking().router().get(resource.getReference());
        if (osRouter != null && context.isBuild()) {
            State routerStatus = osRouter.getStatus();
            if (State.ERROR == routerStatus) {
                throw new OpenStackResourceException("Router in failed state", resource.getType(), utils.getStackName(auth), cloudContext.getId(),
                        resource.getName());
            }
            return routerStatus == State.ACTIVE;
        } else if (osRouter == null && !context.isBuild()) {
            return true;
        }
        return false;
    }
}
