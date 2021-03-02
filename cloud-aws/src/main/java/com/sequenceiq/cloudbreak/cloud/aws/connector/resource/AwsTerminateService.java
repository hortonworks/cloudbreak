package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.BackoffCancellablePollingStrategy.getBackoffCancellablePollingStrategy;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DeleteLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.ResumeProcessesRequest;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.ResourceStatus;
import com.amazonaws.services.ec2.model.DeleteKeyPairRequest;
import com.amazonaws.waiters.Waiter;
import com.amazonaws.waiters.WaiterParameters;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.util.AwsCloudFormationErrorMessageProvider;
import com.sequenceiq.cloudbreak.cloud.aws.view.AuthenticatedContextView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.service.Retry.ActionFailedException;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AwsTerminateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsTerminateService.class);

    @Inject
    private AwsComputeResourceService awsComputeResourceService;

    @Inject
    private AwsClient awsClient;

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

    public List<CloudResourceStatus> terminate(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resources) {
        LOGGER.debug("Deleting stack: {}", ac.getCloudContext().getId());
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        AuthenticatedContextView authenticatedContextView = new AuthenticatedContextView(ac);
        String regionName = authenticatedContextView.getRegion();
        AmazonEc2Client amazonEC2Client = authenticatedContextView.getAmazonEC2Client();
        AmazonCloudFormationClient amazonCloudFormationClient = awsClient.createCloudFormationClient(credentialView, regionName);

        awsCloudWatchService.deleteCloudWatchAlarmsForSystemFailures(stack, regionName, credentialView);
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
        DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(cFStackName);
        try {
            retryService.testWith2SecDelayMax15Times(() -> isStackExist(amazonCloudFormationClient, cFStackName, describeStacksRequest));
        } catch (ActionFailedException ignored) {
            LOGGER.debug("Stack not found with name: {}", cFStackName);
            return;
        }

        resumeAutoScalingPolicies(ac, stack);
        LOGGER.debug("Delete cloudformation stack from resources");
        DeleteStackRequest deleteStackRequest = new DeleteStackRequest().withStackName(cFStackName);
        try {
            retryService.testWith2SecDelayMax5Times(() -> isStackDeleted(amazonCloudFormationClient, describeStacksRequest, deleteStackRequest));
        } catch (Exception e) {
            String errorReason = awsCloudFormationErrorMessageProvider.getErrorReason(ac, cFStackName, ResourceStatus.DELETE_FAILED);
            String message = String.format("Cloudformation stack delete failed: %s", errorReason);
            LOGGER.debug(message, e);
            throw new CloudConnectorException(message, e);
        }
        LOGGER.debug("Cloudformation stack from resources has been deleted");
    }

    private Boolean isStackExist(AmazonCloudFormationClient cfRetryClient, String cFStackName, DescribeStacksRequest describeStacksRequest) {
        try {
            cfRetryClient.describeStacks(describeStacksRequest);
        } catch (AmazonServiceException e) {
            if (!e.getErrorMessage().contains(cFStackName + " does not exist")) {
                throw e;
            }
            throw new ActionFailedException("Stack not exists");
        }
        return Boolean.TRUE;
    }

    private Boolean isStackDeleted(AmazonCloudFormationClient cfRetryClient, DescribeStacksRequest describeStacksRequest,
            DeleteStackRequest deleteStackRequest) {
        cfRetryClient.deleteStack(deleteStackRequest);
        Waiter<DescribeStacksRequest> stackDeleteCompleteWaiter = cfRetryClient.waiters().stackDeleteComplete();
        try {
            LOGGER.debug("Waiting for final state of CloudFormation deletion attempt.");
            WaiterParameters<DescribeStacksRequest> describeStacksRequestWaiterParameters = new WaiterParameters<>(describeStacksRequest)
                .withPollingStrategy(getBackoffCancellablePollingStrategy(null));
            stackDeleteCompleteWaiter.run(describeStacksRequestWaiterParameters);
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
                DeleteKeyPairRequest deleteKeyPairRequest = new DeleteKeyPairRequest(awsClient.getKeyPairName(ac));
                amazonEC2Client.deleteKeyPair(deleteKeyPairRequest);
            } catch (Exception e) {
                String errorMessage = String.format("Failed to delete public key [roleArn:'%s', region: '%s'], detailed message: %s",
                        credentialView.getRoleArn(), regionName, e.getMessage());
                LOGGER.warn(errorMessage, e);
            }
        }
    }

    private void resumeAutoScalingPolicies(AuthenticatedContext ac, CloudStack stack) {
        for (Group instanceGroup : stack.getGroups()) {
            try {
                String regionName = ac.getCloudContext().getLocation().getRegion().value();
                String asGroupName = cfStackUtil.getAutoscalingGroupName(ac, instanceGroup.getName(), regionName);
                if (asGroupName != null) {
                    AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(new AwsCredentialView(ac.getCloudCredential()), regionName);
                    List<AutoScalingGroup> asGroups = amazonASClient.describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest()
                            .withAutoScalingGroupNames(asGroupName)).getAutoScalingGroups();
                    if (!asGroups.isEmpty()) {
                        if (!asGroups.get(0).getSuspendedProcesses().isEmpty()) {
                            amazonASClient.updateAutoScalingGroup(new UpdateAutoScalingGroupRequest()
                                    .withAutoScalingGroupName(asGroupName)
                                    .withMinSize(0)
                                    .withDesiredCapacity(0));
                            amazonASClient.resumeProcesses(new ResumeProcessesRequest().withAutoScalingGroupName(asGroupName));
                        }
                    }
                } else {
                    LOGGER.debug("Autoscaling Group's physical id is null (the resource doesn't exist), it is not needed to resume scaling policies.");
                }
            } catch (AmazonServiceException e) {
                if (e.getErrorMessage().matches(".*Resource.*does not exist for stack.*") || e.getErrorMessage().matches(".*Stack '.*' does not exist.*")) {
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
                        new DeleteLaunchConfigurationRequest().withLaunchConfigurationName(cloudResource.getName())));
    }
}
