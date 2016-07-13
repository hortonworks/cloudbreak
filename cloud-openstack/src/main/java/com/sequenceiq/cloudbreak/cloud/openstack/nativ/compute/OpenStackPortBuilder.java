package com.sequenceiq.cloudbreak.cloud.openstack.nativ.compute;

import java.util.Collections;
import java.util.List;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.OS4JException;
import org.openstack4j.model.compute.ActionResponse;
import org.openstack4j.model.network.Port;
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
public class OpenStackPortBuilder extends AbstractOpenStackComputeResourceBuilder {
    @Override
    public List<CloudResource> build(OpenStackContext context, long privateId, AuthenticatedContext auth, Group group, Image image,
            List<CloudResource> buildableResource) throws Exception {
        CloudResource resource = buildableResource.get(0);
        try {
            OSClient osClient = createOSClient(auth);
            Port port = Builders.port()
                    .tenantId(context.getStringParameter(OpenStackConstants.TENANT_ID))
                    .networkId(context.getStringParameter(OpenStackConstants.NETWORK_ID))
                    .fixedIp(null, context.getStringParameter(OpenStackConstants.SUBNET_ID))
                    .securityGroup(context.getGroupResources(group.getName()).get(0).getReference())
                    .build();
            port = osClient.networking().port().create(port);
            return Collections.singletonList(createPersistedResource(resource, group.getName(), port.getId(), Collections.singletonMap(
                    OpenStackConstants.PORT_ID, port.getId())));
        } catch (OS4JException ex) {
            throw new OpenStackResourceException("Port creation failed", resourceType(), resource.getName(), ex);
        }
    }

    @Override
    public CloudResource delete(OpenStackContext context, AuthenticatedContext auth, CloudResource resource) throws Exception {
        try {
            OSClient osClient = createOSClient(auth);
            ActionResponse response = osClient.networking().port().delete(resource.getReference());
            return checkDeleteResponse(response, resourceType(), auth, resource, "Port deletion failed");
        } catch (OS4JException ex) {
            throw new OpenStackResourceException("Port deletion failed", resourceType(), resource.getName(), ex);
        }
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.OPENSTACK_PORT;
    }

    @Override
    protected boolean checkStatus(OpenStackContext context, AuthenticatedContext auth, CloudResource resource) {
        return true;
    }
}
