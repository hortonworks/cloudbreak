package com.sequenceiq.cloudbreak.cloud.openstack.securitygroup;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.openstack.AbstractOpenStackResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackResourceNameService;
import com.sequenceiq.cloudbreak.cloud.openstack.context.OpenStackContext;
import com.sequenceiq.cloudbreak.cloud.template.GroupResourceBuilder;
import com.sequenceiq.common.api.type.CommonStatus;

public abstract class AbstractOpenStackGroupResourceBuilder extends AbstractOpenStackResourceBuilder implements GroupResourceBuilder<OpenStackContext> {

    public static final String EXISTING_SECURITY_GROUP = "existingSecurityGroup";

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOpenStackGroupResourceBuilder.class);

    @Inject
    private OpenStackResourceNameService resourceNameService;

    @Override
    public List<CloudResourceStatus> checkResources(OpenStackContext context, AuthenticatedContext auth, List<CloudResource> resources) {
        return checkResources(resourceType(), context, auth, resources);
    }

    @Override
    public CloudResource create(OpenStackContext context, AuthenticatedContext auth, Group group, Network network) {
        Security security = group.getSecurity();
        if (security != null && StringUtils.isNotBlank(security.getCloudSecurityId())) {
            String existingId = security.getCloudSecurityId();
            LOGGER.info("Using existing security group [{}] for group [{}]", existingId, group.getName());
            return CloudResource.builder()
                    .withName(existingId)
                    .withGroup(group.getName())
                    .withType(resourceType())
                    .withReference(existingId)
                    .withStatus(CommonStatus.CREATED)
                    .withParameters(Map.of(CloudResource.ATTRIBUTES, Map.of(EXISTING_SECURITY_GROUP, true)))
                    .build();
        }
        String resourceName = resourceNameService.resourceName(resourceType(), context.getName(), group.getName(), auth.getCloudContext().getId());
        return createNamedResource(resourceType(), group.getName(), resourceName);
    }

    @Override
    public CloudResourceStatus update(OpenStackContext context, AuthenticatedContext auth, Group group, Network network, Security security,
            CloudResource resource) {
        return null;
    }
}
