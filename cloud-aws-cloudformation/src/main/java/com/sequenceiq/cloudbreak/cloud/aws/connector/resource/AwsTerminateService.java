package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonElasticLoadBalancingClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsLoadBalancerCommonService;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AuthenticatedContextView;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.util.AwsCloudFormationErrorMessageProvider;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.service.Retry.ActionFailedException;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.DeleteLaunchConfigurationRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import software.amazon.awssdk.services.autoscaling.model.ResumeProcessesRequest;
import software.amazon.awssdk.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.ResourceStatus;
import software.amazon.awssdk.services.cloudformation.waiters.CloudFormationWaiter;
import software.amazon.awssdk.services.ec2.model.DeleteKeyPairRequest;

@Service
public class AwsTerminateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsTerminateService.class);

    @Inject
    private AwsComputeResourceService awsComputeResourceService;

    @Inject
    private AwsCloudFormationClient awsClient;

    @Inject
    private CloudFormationStackUtil cfStackUtil;

    @Inject
    private AwsResourceConnector awsResourceConnector;

    @Inject
    private AwsCloudFormationErrorMessageProvider awsCloudFormationErrorMessageProvider;

    @Inject
    @Qualifier("DefaultRetryService")
    private Retry retryService;

    @Inject
    private AwsCloudWatchService awsCloudWatchService;

    @Inject
    private AwsLoadBalancerCommonService awsLoadBalancerCommonService;

    public List<CloudResourceStatus> terminate(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resources) {
        LOGGER.debug("Deleting stack: {}", ac.getCloudContext().getId());
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        AuthenticatedContextView authenticatedContextView = new AuthenticatedContextView(ac);
        String regionName = authenticatedContextView.getRegion();
        AmazonEc2Client amazonEC2Client = authenticatedContextView.getAmazonEC2Client();
        AmazonCloudFormationClient amazonCloudFormationClient = awsClient.createCloudFormationClient(credentialView, regionName);

        LOGGER.debug("Calling deleteCloudWatchAlarmsForSystemFailures from AwsTerminateService");
        awsCloudWatchService.deleteAllCloudWatchAlarmsForSystemFailures(stack, regionName, credentialView);
        waitAndDeleteCloudformationStack(ac, stack, resources, amazonCloudFormationClient);
        awsComputeResourceService.deleteComputeResources(ac, stack, resources);
        deleteKeyPair(ac, stack, amazonEC2Client, credentialView, regionName);
        deleteLaunchConfiguration(resources, ac);
        LOGGER.debug("Deleting stack finished");
        return awsResourceConnector.check(ac, resources);
    }

    private void waitAndDeleteCloudformationStack(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resources,
            AmazonCloudFormationClient amazonCloudFormationClient) {
        CloudResource stackResource = cfStackUtil.getCloudFormationStackResource(resources);
        if (stackResource == null) {
            LOGGER.debug("No cloudformation stack in resources");
            return;
        }
        String cFStackName = stackResource.getName();
        LOGGER.debug("Search and wait stack with name: {}", cFStackName);
        boolean exists = retryService.testWith2SecDelayMax15Times(() -> cfStackUtil.isCfStackExists(amazonCloudFormationClient, cFStackName));
        if (exists) {
            DescribeStacksRequest describeStacksRequest = DescribeStacksRequest.builder().stackName(cFStackName).build();
            resumeAutoScalingPolicies(ac, stack);
            disableDeletionProtection(ac, amazonCloudFormationClient);
            LOGGER.debug("Delete cloudformation stack from resources");
            DeleteStackRequest deleteStackRequest = DeleteStackRequest.builder().stackName(cFStackName).build();
            try {
                retryService.testWith2SecDelayMax5Times(() -> isStackDeleted(amazonCloudFormationClient, describeStacksRequest, deleteStackRequest));
            } catch (Exception e) {
                String errorReason = awsCloudFormationErrorMessageProvider.getErrorReason(ac, cFStackName, ResourceStatus.DELETE_FAILED);
                if (!StringUtils.hasText(errorReason)) {
                    LOGGER.debug("Cannot fetch the error reason from AWS by DELETE_FAILED, fallback to exception message.");
                    errorReason = e.getMessage();
                }
                String message = String.format("Cloudformation stack delete failed: %s", errorReason);
                LOGGER.debug(message, e);
                throw new CloudConnectorException(message, e);
            }
            LOGGER.debug("Cloudformation stack from resources has been deleted");
        }
    }

    private Boolean isStackDeleted(AmazonCloudFormationClient cfRetryClient, DescribeStacksRequest describeStacksRequest,
            DeleteStackRequest deleteStackRequest) {
        cfRetryClient.deleteStack(deleteStackRequest);
        LOGGER.debug("Waiting for final state of CloudFormation deletion attempt.");
        try (CloudFormationWaiter cloudFormationWaiter = cfRetryClient.waiters()) {
            cloudFormationWaiter.waitUntilStackDeleteComplete(describeStacksRequest);
        } catch (Exception e) {
            LOGGER.debug("CloudFormation stack delete ended in failed state. Delete operation will be retried.");
            throw new ActionFailedException(e.getMessage());
        }
        return Boolean.TRUE;
    }

    private void deleteKeyPair(AuthenticatedContext ac, CloudStack stack, AmazonEc2Client amazonEC2Client, AwsCredentialView credentialView,
            String regionName) {
        LOGGER.debug("Deleting keypairs");
        if (!awsClient.existingKeyPairNameSpecified(stack.getInstanceAuthentication())) {
            try {
                DeleteKeyPairRequest deleteKeyPairRequest = DeleteKeyPairRequest.builder().keyName(awsClient.getKeyPairName(ac)).build();
                amazonEC2Client.deleteKeyPair(deleteKeyPairRequest);
            } catch (Exception e) {
                String errorMessage = String.format("Failed to delete public key [roleArn:'%s', region: '%s'], detailed message: %s",
                        credentialView.getRoleArn(), regionName, e.getMessage());
                LOGGER.warn(errorMessage, e);
            }
        }
    }

    private void disableDeletionProtection(AuthenticatedContext ac, AmazonCloudFormationClient client) {
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        AmazonElasticLoadBalancingClient lbClient = awsClient
                .createElasticLoadBalancingClient(new AwsCredentialView(ac.getCloudCredential()), regionName);
        awsLoadBalancerCommonService.disableDeletionProtection(lbClient, cfStackUtil.getLoadbalancersArns(ac, client));
    }

    private void resumeAutoScalingPolicies(AuthenticatedContext ac, CloudStack stack) {
        for (Group instanceGroup : stack.getGroups()) {
            try {
                String regionName = ac.getCloudContext().getLocation().getRegion().value();
                String asGroupName = cfStackUtil.getAutoscalingGroupName(ac, instanceGroup.getName(), regionName);
                if (asGroupName != null) {
                    AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(new AwsCredentialView(ac.getCloudCredential()), regionName);
                    List<AutoScalingGroup> asGroups = amazonASClient.describeAutoScalingGroups(DescribeAutoScalingGroupsRequest.builder()
                            .autoScalingGroupNames(asGroupName)
                            .build()).autoScalingGroups();
                    if (!asGroups.isEmpty()) {
                        if (!asGroups.get(0).suspendedProcesses().isEmpty()) {
                            amazonASClient.updateAutoScalingGroup(UpdateAutoScalingGroupRequest.builder()
                                    .autoScalingGroupName(asGroupName)
                                    .minSize(0)
                                    .desiredCapacity(0)
                                    .build());
                            amazonASClient.resumeProcesses(ResumeProcessesRequest.builder().autoScalingGroupName(asGroupName).build());
                        }
                    }
                } else {
                    LOGGER.debug("Autoscaling Group's physical id is null (the resource doesn't exist), it is not needed to resume scaling policies.");
                }
            } catch (AwsServiceException e) {
                String errorMessage = e.awsErrorDetails().errorMessage();
                if (errorMessage.matches(".*Resource.*does not exist for stack.*") || errorMessage.matches(".*Stack '.*' does not exist.*")) {
                    LOGGER.debug(e.getMessage());
                } else {
                    throw e;
                }
            }
        }
    }

    private void deleteLaunchConfiguration(List<CloudResource> resources, AuthenticatedContext ac) {
        if (resources == null) {
            return;
        }
        AmazonAutoScalingClient autoScalingClient = awsClient.createAutoScalingClient(new AwsCredentialView(ac.getCloudCredential()),
                ac.getCloudContext().getLocation().getRegion().value());
        resources.stream().filter(cloudResource -> cloudResource.getType() == ResourceType.AWS_LAUNCHCONFIGURATION).forEach(cloudResource ->
                autoScalingClient.deleteLaunchConfiguration(
                        DeleteLaunchConfigurationRequest.builder().launchConfigurationName(cloudResource.getName()).build()));
    }
}
