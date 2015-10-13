package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_AWS_SPOTINSTANCE_ENABLED;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInternetGatewaysRequest;
import com.amazonaws.services.ec2.model.DescribeInternetGatewaysResult;
import com.amazonaws.services.ec2.model.InternetGateway;
import com.amazonaws.services.ec2.model.InternetGatewayAttachment;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.cloudbreak.common.type.ImageStatus;
import com.sequenceiq.cloudbreak.common.type.ImageStatusResult;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@Component
public class AwsSetup implements Setup {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsSetup.class);
    private static final String IGW_DOES_NOT_EXIST_MSG = "The given internet gateway '%s' does not exist or belongs to a different region.";
    private static final String VPC_DOES_NOT_EXIST_MSG = "The given internet gateway '%s' does not belong to the given VPC '%s'.";
    private static final int FINISHED_PROGRESS_VALUE = 100;

    @Value("${cb.aws.spotinstances.enabled:" + CB_AWS_SPOTINSTANCE_ENABLED + "}")
    private boolean awsSpotinstanceEnabled;

    @Inject
    private CloudFormationStackUtil cfStackUtil;
    @Inject
    private ResourceNotifier resourceNotifier;
    @Inject
    private AwsClient awsClient;

    @Override
    public void prepareImage(AuthenticatedContext authenticatedContext, CloudStack stack) {
        LOGGER.debug("prepare image has been executed");
    }

    @Override
    public ImageStatusResult checkImageStatus(AuthenticatedContext authenticatedContext, CloudStack stack) {
        return new ImageStatusResult(ImageStatus.CREATE_FINISHED, FINISHED_PROGRESS_VALUE);
    }

    @Override
    public void execute(AuthenticatedContext authenticatedContext, CloudStack stack) {
        Network network = stack.getNetwork();
        if (!awsSpotinstanceEnabled) {
            for (Group group : stack.getGroups()) {
                if (group.getInstances().get(0).getParameter("spotPrice", Double.class) != null) {
                    throw new CloudConnectorException(String.format("Spot instances are not supported on this AMI: %s", stack.getImage()));
                }
            }
        }
        if (isExistingVPC(network)) {
            try {
                AmazonEC2Client amazonEC2Client = awsClient.createAccess(new AwsCredentialView(authenticatedContext.getCloudCredential()), stack.getRegion());
                DescribeInternetGatewaysRequest describeInternetGatewaysRequest = new DescribeInternetGatewaysRequest();
                describeInternetGatewaysRequest.withInternetGatewayIds(network.getStringParameter("internetGatewayId"));
                DescribeInternetGatewaysResult describeInternetGatewaysResult = amazonEC2Client.describeInternetGateways(describeInternetGatewaysRequest);
                if (describeInternetGatewaysResult.getInternetGateways().size() < 1) {
                    throw new CloudConnectorException(String.format(IGW_DOES_NOT_EXIST_MSG, network.getStringParameter("internetGatewayId")));
                } else {
                    InternetGateway internetGateway = describeInternetGatewaysResult.getInternetGateways().get(0);
                    InternetGatewayAttachment attachment = internetGateway.getAttachments().get(0);
                    if (attachment != null && !attachment.getVpcId().equals(network.getStringParameter("vpcId"))) {
                        throw new CloudConnectorException(String.format(VPC_DOES_NOT_EXIST_MSG,
                                network.getStringParameter("internetGatewayId"), network.getStringParameter("vpcId")));
                    }
                }
            } catch (AmazonClientException e) {
                throw new CloudConnectorException(String.format(IGW_DOES_NOT_EXIST_MSG, network.getStringParameter("internetGatewayId")));
            }
        }
        String cFStackName = cfStackUtil.getCfStackName(authenticatedContext);
        CloudResource cloudFormationStack = new CloudResource.Builder().type(ResourceType.CLOUDFORMATION_STACK).name(cFStackName).build();
        resourceNotifier.notifyAllocation(cloudFormationStack, authenticatedContext.getCloudContext());
        LOGGER.debug("setup has been executed");
    }

    public boolean isExistingVPC(Network network) {
        return network.getStringParameter("subnetCIDR") != null
                && network.getStringParameter("vpcId") != null
                && network.getStringParameter("internetGatewayId") != null;
    }
}
