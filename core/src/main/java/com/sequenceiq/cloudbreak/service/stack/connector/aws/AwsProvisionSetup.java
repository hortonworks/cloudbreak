package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInternetGatewaysRequest;
import com.amazonaws.services.ec2.model.DescribeInternetGatewaysResult;
import com.amazonaws.services.ec2.model.InternetGateway;
import com.amazonaws.services.ec2.model.InternetGatewayAttachment;
import com.sequenceiq.cloudbreak.domain.AwsNetwork;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.ProvisionSetup;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionEvent;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionSetupComplete;

@Component
public class AwsProvisionSetup implements ProvisionSetup {
    private static final String IGW_DOES_NOT_EXIST_MSG = "The given internet gateway '%s' does not exist or belongs to a different region.";
    private static final String VPC_DOES_NOT_EXIST_MSG = "The given internet gateway '%s' does not belong to the given VPC '%s'.";

    @Inject
    private AwsStackUtil awsStackUtil;

    @Override
    public ProvisionEvent setupProvisioning(Stack stack) {
        return new ProvisionSetupComplete(getCloudPlatform(), stack.getId())
                .withSetupProperties(new HashMap<String, Object>());
    }

    @Override
    public String preProvisionCheck(Stack stack) {
        String result = null;
        AwsNetwork network = (AwsNetwork) stack.getNetwork();
        if (network.isExistingVPC()) {
            try {
                AmazonEC2Client amazonEC2Client = awsStackUtil.createEC2Client(stack);
                DescribeInternetGatewaysRequest describeInternetGatewaysRequest = new DescribeInternetGatewaysRequest();
                describeInternetGatewaysRequest.withInternetGatewayIds(network.getInternetGatewayId());
                DescribeInternetGatewaysResult describeInternetGatewaysResult = amazonEC2Client.describeInternetGateways(describeInternetGatewaysRequest);
                if (describeInternetGatewaysResult.getInternetGateways().size() < 1) {
                    result = String.format(IGW_DOES_NOT_EXIST_MSG, network.getInternetGatewayId());
                } else {
                    InternetGateway internetGateway = describeInternetGatewaysResult.getInternetGateways().get(0);
                    InternetGatewayAttachment attachment = internetGateway.getAttachments().get(0);
                    if (attachment != null && !attachment.getVpcId().equals(network.getVpcId())) {
                        result = String.format(VPC_DOES_NOT_EXIST_MSG, network.getInternetGatewayId(), network.getVpcId());
                    }
                }
            } catch (AmazonClientException e) {
                result = String.format(IGW_DOES_NOT_EXIST_MSG, network.getInternetGatewayId());
            }
        }
        return result;
    }

    public Map<String, Object> getSetupProperties(Stack stack) {
        return new HashMap<>();
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }

}
