package com.sequenceiq.cloudbreak.cloud.aws.resource.group;

import static com.sequenceiq.cloudbreak.cloud.aws.resource.AwsNativeResourceBuilderOrderConstants.NATIVE_SECURITY_GROUP_RESOURCE_BUILDER_ORDER;
import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.amazonaws.services.ec2.model.DeleteSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DeleteSecurityGroupResult;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsMethodExecutor;
import com.sequenceiq.cloudbreak.cloud.aws.resource.instance.util.SecurityGroupBuilderUtil;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class AwsSecurityGroupResourceBuilder extends AbstractAwsNativeGroupBuilder {

    private static final Logger LOGGER = getLogger(AwsSecurityGroupResourceBuilder.class);

    @Inject
    private SecurityGroupBuilderUtil securityGroupBuilderUtil;

    @Inject
    private AwsMethodExecutor awsMethodExecutor;

    @Override
    public CloudResource create(AwsContext context, AuthenticatedContext auth, Group group, Network network) {
        CloudContext cloudContext = auth.getCloudContext();
        String securityGroupId = group.getSecurity().getCloudSecurityId();
        String availabilityZone = context.getLocation().getAvailabilityZone().value();
        CloudResource ret = null;
        if (securityGroupId == null) {
            securityGroupId = getResourceNameService().resourceName(resourceType(), context.getName(), group.getName(), cloudContext.getId());
            ret = createNamedResource(resourceType(), securityGroupId, availabilityZone);
        } else {
            LOGGER.info("Security group id exists with id: {}", securityGroupId);
        }
        return ret;
    }

    @Override
    public CloudResource build(AwsContext context, AuthenticatedContext auth, Group group, Network network, Security security, CloudResource resource)
            throws Exception {
        AmazonEc2Client amazonEc2Client = context.getAmazonEc2Client();
        String securityGroupId = securityGroupBuilderUtil
                .createSecurityGroup(network, group, amazonEc2Client, auth.getCloudContext(), auth);
        LOGGER.info("Security group successfully created with id: {}", securityGroupId);
        return CloudResource.builder().cloudResource(resource)
                .reference(securityGroupId)
                .build();
    }

    @Override
    public CloudResource delete(AwsContext context, AuthenticatedContext auth, CloudResource resource, Network network) throws Exception {
        AmazonEc2Client amazonEc2Client = context.getAmazonEc2Client();
        DeleteSecurityGroupRequest request = new DeleteSecurityGroupRequest()
                .withGroupId(resource.getReference());
        DeleteSecurityGroupResult result = awsMethodExecutor.execute(() -> amazonEc2Client.deleteSecurityGroup(request), null);
        return result == null ? null : resource;
    }

    @Override
    public CloudResourceStatus update(AwsContext context, AuthenticatedContext auth, Group group, Network network, Security security, CloudResource resource) {
        return null;
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.AWS_SECURITY_GROUP;
    }

    @Override
    public int order() {
        return NATIVE_SECURITY_GROUP_RESOURCE_BUILDER_ORDER;
    }
}
