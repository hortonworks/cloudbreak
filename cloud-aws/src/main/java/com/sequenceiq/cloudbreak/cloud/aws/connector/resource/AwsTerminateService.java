package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static com.amazonaws.services.cloudformation.model.StackStatus.DELETE_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.DELETE_FAILED;
import static com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsResourceConstants.ERROR_STATUSES;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DeleteLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.ResumeProcessesRequest;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DeleteKeyPairRequest;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.encryption.EncryptedImageCopyService;
import com.sequenceiq.cloudbreak.cloud.aws.encryption.EncryptedSnapshotService;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.AwsBackoffSyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.aws.task.AwsPollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.service.Retry.ActionWentFailException;

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
    private EncryptedSnapshotService encryptedSnapshotService;

    @Inject
    private EncryptedImageCopyService encryptedImageCopyService;

    @Inject
    private AwsPollTaskFactory awsPollTaskFactory;

    @Inject
    private AwsBackoffSyncPollingScheduler<Boolean> awsBackoffSyncPollingScheduler;

    @Inject
    private AwsResourceConnector awsResourceConnector;

    @Inject
    @Qualifier("DefaultRetryService")
    private Retry retryService;

    @Inject
    private AwsElasticIpService awsElasticIpService;

    public List<CloudResourceStatus> terminate(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resources) {
        LOGGER.debug("Deleting stack: {}", ac.getCloudContext().getId());
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        if (resources != null && !resources.isEmpty()) {
            awsComputeResourceService.deleteComputeResources(ac, stack, resources);

            AmazonCloudFormationRetryClient cfRetryClient = awsClient.createCloudFormationRetryClient(credentialView, regionName);
            CloudResource stackResource = cfStackUtil.getCloudFormationStackResource(resources);
            AmazonEC2Client amazonEC2Client = awsClient.createAccess(credentialView, regionName);
            if (stackResource == null) {
                cleanupEncryptedResources(ac, resources, regionName, amazonEC2Client);
                return Collections.emptyList();
            }
            String cFStackName = stackResource.getName();
            LOGGER.debug("Deleting CloudFormation stack for stack: {} [cf stack id: {}]", cFStackName, ac.getCloudContext().getId());
            DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(cFStackName);
            try {
                retryService.testWith2SecDelayMax15Times(() -> {
                    try {
                        cfRetryClient.describeStacks(describeStacksRequest);
                    } catch (AmazonServiceException e) {
                        if (!e.getErrorMessage().contains(cFStackName + " does not exist")) {
                            throw e;
                        }
                        throw new ActionWentFailException("Stack not exists");
                    }
                    return Boolean.TRUE;
                });
            } catch (ActionWentFailException ignored) {
                LOGGER.debug("Stack not found with name: {}", cFStackName);
                awsElasticIpService.releaseReservedIp(amazonEC2Client, resources);
                cleanupEncryptedResources(ac, resources, regionName, amazonEC2Client);
                return Collections.emptyList();
            }
            resumeAutoScalingPolicies(ac, stack);
            DeleteStackRequest deleteStackRequest = new DeleteStackRequest().withStackName(cFStackName);
            cfRetryClient.deleteStack(deleteStackRequest);

            AmazonCloudFormationClient cfClient = awsClient.createCloudFormationClient(credentialView, regionName);
            PollTask<Boolean> task = awsPollTaskFactory.newAwsTerminateStackStatusCheckerTask(ac, cfClient, DELETE_COMPLETE, DELETE_FAILED, ERROR_STATUSES,
                    cFStackName);
            try {
                awsBackoffSyncPollingScheduler.schedule(task);
            } catch (Exception e) {
                throw new CloudConnectorException(e.getMessage(), e);
            }
            awsElasticIpService.releaseReservedIp(amazonEC2Client, resources);
            cleanupEncryptedResources(ac, resources, regionName, amazonEC2Client);
            deleteKeyPair(ac, stack);
            deleteLaunchConfiguration(resources, ac);
        } else if (resources != null) {
            AmazonEC2Client amazonEC2Client = awsClient.createAccess(credentialView, regionName);
            awsElasticIpService.releaseReservedIp(amazonEC2Client, resources);
            LOGGER.debug("No CloudFormation stack saved for stack.");
        } else {
            LOGGER.debug("No resources to release.");
        }
        return awsResourceConnector.check(ac, resources);
    }

    private void deleteKeyPair(AuthenticatedContext ac, CloudStack stack) {
        AwsCredentialView awsCredential = new AwsCredentialView(ac.getCloudCredential());
        String region = ac.getCloudContext().getLocation().getRegion().value();
        if (!awsClient.existingKeyPairNameSpecified(stack.getInstanceAuthentication())) {
            try {
                AmazonEC2Client client = awsClient.createAccess(awsCredential, region);
                DeleteKeyPairRequest deleteKeyPairRequest = new DeleteKeyPairRequest(awsClient.getKeyPairName(ac));
                client.deleteKeyPair(deleteKeyPairRequest);
            } catch (Exception e) {
                String errorMessage = String.format("Failed to delete public key [roleArn:'%s', region: '%s'], detailed message: %s",
                        awsCredential.getRoleArn(), region, e.getMessage());
                LOGGER.warn(errorMessage, e);
            }
        }
    }

    private void cleanupEncryptedResources(AuthenticatedContext ac, List<CloudResource> resources, String regionName, AmazonEC2Client amazonEC2Client) {
        encryptedSnapshotService.deleteResources(ac, amazonEC2Client, resources);
        encryptedImageCopyService.deleteResources(regionName, amazonEC2Client, resources);
    }

    private void resumeAutoScalingPolicies(AuthenticatedContext ac, CloudStack stack) {
        for (Group instanceGroup : stack.getGroups()) {
            try {
                String asGroupName = cfStackUtil.getAutoscalingGroupName(ac, instanceGroup.getName(), ac.getCloudContext().getLocation().getRegion().value());
                if (asGroupName != null) {
                    AmazonAutoScalingRetryClient amazonASClient = awsClient.createAutoScalingRetryClient(new AwsCredentialView(ac.getCloudCredential()),
                            ac.getCloudContext().getLocation().getRegion().value());
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
                if (e.getErrorMessage().matches("Resource.*does not exist for stack.*") || e.getErrorMessage().matches("Stack '.*' does not exist.*")) {
                    LOGGER.debug(e.getMessage());
                } else {
                    throw e;
                }
            }
        }
    }

    private void deleteLaunchConfiguration(List<CloudResource> resources, AuthenticatedContext ac) {
        AmazonAutoScalingClient autoScalingClient = awsClient.createAutoScalingClient(new AwsCredentialView(ac.getCloudCredential()),
                ac.getCloudContext().getLocation().getRegion().value());
        resources.stream().filter(cloudResource -> cloudResource.getType() == ResourceType.AWS_LAUNCHCONFIGURATION).forEach(cloudResource ->
                autoScalingClient.deleteLaunchConfiguration(
                        new DeleteLaunchConfigurationRequest().withLaunchConfigurationName(cloudResource.getName())));
    }
}
