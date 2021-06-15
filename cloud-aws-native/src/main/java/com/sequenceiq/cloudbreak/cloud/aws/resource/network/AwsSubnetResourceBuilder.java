package com.sequenceiq.cloudbreak.cloud.aws.resource.network;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class AwsSubnetResourceBuilder extends AbstractAwsNetworkBuilder {

    public static final String SUBNET_NAME = "subnetName";

    @Inject
    private AwsStackUtil awsStackUtil;

    @Override
    public CloudResource create(AwsContext context, AuthenticatedContext auth, Network network) {
        String name = awsStackUtil.isExistingSubnet(network) ?
                awsStackUtil.getSubnetId(network) :
                getResourceNameService().resourceName(resourceType(), context.getName());
        return createNamedResource(resourceType(), name);
    }

    @Override
    public CloudResource build(AwsContext context, AuthenticatedContext auth, Network network, Security security, CloudResource resource) throws Exception {
        context.putParameter(SUBNET_NAME, resource.getName());
        return new CloudResource.Builder().cloudResource(resource).persistent(false).build();
    }

    @Override
    public CloudResourceStatus update(AwsContext context, AuthenticatedContext auth, Network network, Security security, CloudResource resource) {
        return null;
    }

    @Override
    public CloudResource delete(AwsContext context, AuthenticatedContext auth, CloudResource resource, Network network) throws Exception {
        return null;
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.AWS_SUBNET;
    }

    @Override
    public int order() {
        return 1;
    }
}
