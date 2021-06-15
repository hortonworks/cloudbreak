package com.sequenceiq.cloudbreak.cloud.aws.resource.network;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.amazonaws.services.ec2.model.CreateVpcResult;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AwsNetworkResourceBuilder extends AbstractAwsNetworkBuilder {

    public static final String VPC_NAME = "vpcName";

    public static final String VPC_ID = "vpcId";

    @Inject
    private AwsStackUtil awsStackUtil;

    @Override
    public CloudResource create(AwsContext context, AuthenticatedContext auth, Network network) {
        String name = awsStackUtil.isExistingNetwork(context.getAmazonEc2Client(), network) ?
                awsStackUtil.getVpcId(network) :
                getResourceNameService().resourceName(resourceType(), context.getName());
        return createNamedResource(ResourceType.AWS_VPC, name);
    }

    @Override
    public CloudResource build(AwsContext context, AuthenticatedContext auth, Network network, Security security, CloudResource resource) throws Exception {
        if (!awsStackUtil.isExistingNetwork(context.getAmazonEc2Client(), network)) {
            AmazonEc2Client amazonEc2Client = context.getAmazonEc2Client();
            CreateVpcResult vpc = amazonEc2Client.createVpc(network, security);
            Map<String, Object> params = new HashMap<>(resource.getParameters());
            params.put(VPC_ID, vpc.getVpc().getVpcId());
            return CloudResource.builder().cloudResource(resource).params(params).build();
        }
        context.putParameter(VPC_NAME, resource.getName());
        return new CloudResource.Builder().cloudResource(resource).persistent(false).build();
    }

    @Override
    public CloudResourceStatus update(AwsContext context, AuthenticatedContext auth, Network network, Security security, CloudResource resource) {
        return null;
    }

    @Override
    public CloudResource delete(AwsContext context, AuthenticatedContext auth, CloudResource resource, Network network) throws Exception {
        return new CloudResource.Builder().cloudResource(resource).persistent(false).build();
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.AWS_VPC;
    }

    @Override
    public int order() {
        return 0;
    }
}
