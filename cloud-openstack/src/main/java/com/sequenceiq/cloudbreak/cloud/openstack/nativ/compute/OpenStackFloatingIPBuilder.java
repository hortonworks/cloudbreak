package com.sequenceiq.cloudbreak.cloud.openstack.nativ.compute;

import java.util.Collections;
import java.util.List;

import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.OS4JException;
import org.openstack4j.model.compute.ActionResponse;
import org.openstack4j.model.compute.FloatingIP;
import org.openstack4j.model.compute.Server;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants;
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.OpenStackResourceException;
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.context.OpenStackContext;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@Service
public class OpenStackFloatingIPBuilder extends AbstractOpenStackComputeResourceBuilder {
    @Override
    public List<CloudResource> build(OpenStackContext context, long privateId, AuthenticatedContext auth, Group group, Image image,
            List<CloudResource> buildableResource) throws Exception {
        CloudResource resource = buildableResource.get(0);
        try {
            String publicNetId = context.getStringParameter(OpenStackConstants.PUBLIC_NET_ID);
            if (publicNetId != null) {
                OSClient osClient = createOSClient(auth);
                List<CloudResource> computeResources = context.getComputeResources(privateId);
                CloudResource instance = getInstance(computeResources);
                FloatingIP unusedIp = osClient.compute().floatingIps().allocateIP(publicNetId);
                ActionResponse response = osClient.compute().floatingIps().addFloatingIP(instance.getParameter(OpenStackConstants.SERVER, Server.class),
                        unusedIp.getFloatingIpAddress());
                if (!response.isSuccess()) {
                    throw new OpenStackResourceException("Add floating-ip to server failed", resourceType(), resource.getName(),
                            auth.getCloudContext().getId(), response.getFault());
                }
                return Collections.singletonList(createPersistedResource(resource, group.getName(), unusedIp.getId()));
            }
            return (List<CloudResource>) Collections.EMPTY_LIST;
        } catch (OS4JException ex) {
            throw new OpenStackResourceException("Add floating-ip to server failed", resourceType(), resource.getName(), ex);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public CloudResource delete(OpenStackContext context, AuthenticatedContext auth, CloudResource resource) throws Exception {
        context.getParameter(OpenStackConstants.FLOATING_IP_IDS, List.class).add(resource.getReference());
        return null;
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.OPENSTACK_FLOATING_IP;
    }

    @Override
    protected boolean checkStatus(OpenStackContext context, AuthenticatedContext auth, CloudResource resource) {
        return true;
    }

    private CloudResource getInstance(List<CloudResource> computeResources) {
        CloudResource instance = null;
        for (CloudResource computeResource : computeResources) {
            if (computeResource.getType() == ResourceType.OPENSTACK_INSTANCE) {
                instance = computeResource;
            }
        }
        return instance;
    }
}
