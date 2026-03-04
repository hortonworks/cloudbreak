package com.sequenceiq.cloudbreak.cloud.openstack.compute;

import java.util.Collections;
import java.util.List;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.OS4JException;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.Port;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.openstack.OpenStackResourceException;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants;
import com.sequenceiq.cloudbreak.cloud.openstack.context.OpenStackContext;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class OpenStackPortBuilder extends AbstractOpenStackComputeResourceBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackPortBuilder.class);

    @Override
    public List<CloudResource> build(OpenStackContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group,
        List<CloudResource> buildableResource, CloudStack cloudStack) {
        CloudResource resource = buildableResource.getFirst();
        try {
            OSClient<?> osClient = createOSClient(auth);
            Port port = Builders.port()
                    .tenantId(context.getStringParameter(OpenStackConstants.TENANT_ID))
                    .networkId(cloudStack.getNetwork().getStringParameter(OpenStackConstants.NETWORK_ID))
                    .fixedIp(null, cloudStack.getNetwork().getStringParameter(OpenStackConstants.SUBNET_ID))
                    .securityGroup(context.getGroupResources(group.getName()).getFirst().getReference())
                    .build();
            port = osClient.networking().port().create(port);
            return Collections.singletonList(createPersistedResource(privateId, resource, group.getName(), port.getId(), Collections.singletonMap(
                    OpenStackConstants.PORT_ID, port.getId())));
        } catch (OS4JException ex) {
            throw new OpenStackResourceException("Port creation failed", resourceType(), resource.getName(), ex);
        }
    }

    @Override
    public CloudResource delete(OpenStackContext context, AuthenticatedContext auth, CloudResource resource) {
        try {
            OSClient<?> osClient = createOSClient(auth);
            LOGGER.debug("About to delete port: [{}]", resource.toString());
            if (resource.getReference() != null) {
                ActionResponse response = osClient.networking().port().delete(resource.getReference());
                return checkDeleteResponse(response, resourceType(), auth, resource, "Port deletion failed");
            } else {
                return resource;
            }
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
