package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsResourceConstants.SUSPENDED_PROCESSES;
import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.CancellableWaiterConfiguration.cancellableWaiterConfiguration;
import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.WaiterRunner.handleWaiterError;
import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.WaiterRunner.run;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.CustomAmazonWaiterProvider;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.StackCancellationCheck;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.waiters.Waiter;
import software.amazon.awssdk.services.autoscaling.model.Activity;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsResponse;
import software.amazon.awssdk.services.autoscaling.model.DescribeScalingActivitiesRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeScalingActivitiesResponse;
import software.amazon.awssdk.services.autoscaling.model.ResumeProcessesRequest;
import software.amazon.awssdk.services.autoscaling.model.SuspendProcessesRequest;
import software.amazon.awssdk.services.autoscaling.model.TerminateInstanceInAutoScalingGroupRequest;
import software.amazon.awssdk.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import software.amazon.awssdk.services.autoscaling.waiters.AutoScalingWaiter;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.waiters.Ec2Waiter;

@Service
public class AwsAutoScalingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsAutoScalingService.class);

    private static final int MAX_INSTANCE_ID_SIZE = 100;

    @Inject
    private CloudFormationStackUtil cfStackUtil;

    @Inject
    private AwsCloudFormationClient awsClient;

    @Inject
    private CustomAmazonWaiterProvider customAmazonWaiterProvider;

    @Inject
    private CloudFormationStackUtil cloudFormationStackUtil;

    public void suspendAutoScaling(AuthenticatedContext ac, CloudStack stack) {
        AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(new AwsCredentialView(ac.getCloudCredential()),
                ac.getCloudContext().getLocation().getRegion().value());
        for (Group group : stack.getGroups()) {
            String asGroupName = cfStackUtil.getAutoscalingGroupName(ac, group.getName(), ac.getCloudContext().getLocation().getRegion().value());
            LOGGER.info("Suspend autoscaling group '{}'", asGroupName);
            amazonASClient.suspendProcesses(SuspendProcessesRequest.builder()
                    .autoScalingGroupName(asGroupName)
                    .scalingProcesses(SUSPENDED_PROCESSES)
                    .build());
        }
    }

    public void resumeAutoScaling(AmazonAutoScalingClient amazonASClient, Collection<String> groupNames, List<String> autoScalingPolicies) {
        for (String groupName : groupNames) {
            LOGGER.info("Resume autoscaling group '{}'", groupName);
            amazonASClient.resumeProcesses(ResumeProcessesRequest.builder()
                    .autoScalingGroupName(groupName)
                    .scalingProcesses(autoScalingPolicies)
                    .build());
        }
    }

    public void scheduleStatusChecks(List<Group> groups, AuthenticatedContext ac, AmazonCloudFormationClient cloudFormationClient, Date timeBeforeASUpdate,
            List<String> knownInstances) throws AmazonAutoscalingFailedException {
        AmazonEc2Client amClient = awsClient.createEc2Client(new AwsCredentialView(ac.getCloudCredential()),
                ac.getCloudContext().getLocation().getRegion().value());
        AmazonAutoScalingClient asClient = awsClient.createAutoScalingClient(new AwsCredentialView(ac.getCloudCredential()),
                ac.getCloudContext().getLocation().getRegion().value());
        for (Group group : groups) {
            String asGroupName = cfStackUtil.getAutoscalingGroupName(ac, cloudFormationClient, group.getName());
            checkLastScalingActivity(asClient, asGroupName, timeBeforeASUpdate, group);
            LOGGER.debug("Polling Auto Scaling group until new instances are ready. [stack: {}, asGroup: {}]", ac.getCloudContext().getId(), asGroupName);
            waitForGroup(amClient, asClient, asGroupName, group.getInstancesSize(), ac.getCloudContext().getId(), knownInstances);
        }
    }

    private void waitForGroup(AmazonEc2Client amClient, AmazonAutoScalingClient asClient, String autoScalingGroupName, Integer requiredInstanceCount,
            Long stackId, List<String> knownInstances) throws AmazonAutoscalingFailedException {
        DescribeAutoScalingGroupsRequest request = DescribeAutoScalingGroupsRequest.builder().autoScalingGroupNames(autoScalingGroupName).build();
        StackCancellationCheck cancellationCheck = new StackCancellationCheck(stackId);
        try (AutoScalingWaiter waiter = asClient.waiters()) {
            LOGGER.debug("Waiting for {} auto scaling group in service.", autoScalingGroupName);
            waiter.waitUntilGroupInService(request, cancellableWaiterConfiguration(cancellationCheck));
        } catch (Exception e) {
            handleWaiterError("Failed to launch auto scaling group", e);
        }

        Waiter<DescribeAutoScalingGroupsResponse> instancesInServiceWaiter = customAmazonWaiterProvider.getAutoscalingInstancesInServiceWaiter(
                requiredInstanceCount);
        String errorMessage = String.format("Not all the %s instance(s) reached InService state.", requiredInstanceCount);
        run(() -> asClient.describeAutoScalingGroups(request), instancesInServiceWaiter, errorMessage);

        List<String> instanceIds = cloudFormationStackUtil.getInstanceIds(asClient, autoScalingGroupName);
        List<String> instanceIdsForWait = getInstanceIdsForWait(instanceIds, knownInstances);
        if (requiredInstanceCount != 0) {
            List<List<String>> partitionedInstanceIdsList = Lists.partition(instanceIdsForWait, MAX_INSTANCE_ID_SIZE);
            try (Ec2Waiter waiter = amClient.waiters()) {
                for (List<String> partitionedInstanceIds : partitionedInstanceIdsList) {
                    DescribeInstancesRequest describeInstancesRequest = DescribeInstancesRequest.builder().instanceIds(partitionedInstanceIds).build();
                    waiter.waitUntilInstanceRunning(describeInstancesRequest, cancellableWaiterConfiguration(cancellationCheck));
                }
            } catch (AwsServiceException e) {
                AwsErrorDetails newErrorDetails = e.awsErrorDetails().toBuilder()
                        .errorMessage("Cannot describeInstances. " + e.awsErrorDetails().errorMessage())
                        .build();
                AwsServiceException newException = e.toBuilder().awsErrorDetails(newErrorDetails).build();
                LOGGER.error("Cannot describeInstances", e);
                throw newException;
            } catch (Exception e) {
                throw new AmazonAutoscalingFailedException("Error occurred in describeInstances: " + e.getMessage(), e);
            }
        }
    }

    private List<String> getInstanceIdsForWait(List<String> instanceIds, List<String> knownInstances) {
        List<String> result;
        if (knownInstances == null) {
            result = instanceIds;
        } else {
            result = instanceIds.stream()
                    .filter(instanceId -> !knownInstances.contains(instanceId))
                    .collect(Collectors.toList());
        }

        return result;
    }

    @VisibleForTesting
    void checkLastScalingActivity(AmazonAutoScalingClient asClient, String autoScalingGroupName, Date timeBeforeASUpdate, Group group)
            throws AmazonAutoscalingFailedException {
        if (group.getInstancesSize() > 0) {
            LOGGER.debug("Check last activity after AS update. Wait for the first if it doesn't exist. [asGroup: {}]", autoScalingGroupName);
            Optional<Activity> firstActivity = Optional.empty();
            try {
                AutoScalingGroup scalingGroup = asClient
                        .describeAutoScalingGroups(DescribeAutoScalingGroupsRequest.builder().autoScalingGroupNames(autoScalingGroupName).build())
                        .autoScalingGroups().stream().findFirst()
                        .orElseThrow(() -> new AmazonAutoscalingFailedException("Can not find autoscaling group by name: " + autoScalingGroupName));
                if (group.getInstancesSize() > scalingGroup.instances().size()) {
                    Waiter<DescribeScalingActivitiesResponse> autoscalingActivitiesWaiter =
                            customAmazonWaiterProvider.getAutoscalingActivitiesWaiter(timeBeforeASUpdate);
                    DescribeScalingActivitiesRequest request = DescribeScalingActivitiesRequest.builder().autoScalingGroupName(autoScalingGroupName).build();
                    autoscalingActivitiesWaiter.run(() -> asClient.describeScalingActivities(request));
                    DescribeScalingActivitiesResponse describeScalingActivitiesResponse = asClient.describeScalingActivities(request);

                    // if we run into InsufficientInstanceCapacity we can skip to waitForGroup because that method will wait for the required instance count
                    firstActivity = describeScalingActivitiesResponse.activities().stream().findFirst()
                            .filter(activity -> "failed".equalsIgnoreCase(activity.statusCodeAsString()) &&
                                    !activity.statusMessage().contains("InsufficientInstanceCapacity"));

                } else {
                    LOGGER.info("Skip checking activities because the AS group contains the desired instance count");
                }
            } catch (Exception e) {
                LOGGER.error("Failed to list activities: {}", e.getMessage(), e);
                throw new AmazonAutoscalingFailedException(e.getMessage(), e);
            }

            if (firstActivity.isPresent()) {
                Activity activity = firstActivity.get();
                LOGGER.error("Cannot execute autoscale, because last activity is failed: {}", activity);
                throw new AmazonAutoscalingFailedException(activity.description() + " " + activity.cause());
            }
        }
    }

    public void scheduleStatusChecks(List<Group> groups, AuthenticatedContext ac, AmazonCloudFormationClient cloudFormationClient)
            throws AmazonAutoscalingFailedException {
        scheduleStatusChecks(groups, ac, cloudFormationClient, null, null);
    }

    public List<AutoScalingGroup> getAutoscalingGroups(AmazonAutoScalingClient amazonASClient, Set<String> groupNames) {
        return amazonASClient.describeAutoScalingGroups(DescribeAutoScalingGroupsRequest.builder()
                .autoScalingGroupNames(groupNames)
                .build()).autoScalingGroups();
    }

    public void updateAutoscalingGroup(AmazonAutoScalingClient amazonASClient, String groupName, Integer newSize) {
        LOGGER.info("Update '{}' Auto Scaling groups max size to {}, desired capacity to {}", groupName, newSize, newSize);
        amazonASClient.updateAutoScalingGroup(UpdateAutoScalingGroupRequest.builder()
                .autoScalingGroupName(groupName)
                .maxSize(newSize)
                .desiredCapacity(newSize)
                .build());
        LOGGER.debug("Updated Auto Scaling group's desiredCapacity: [to: '{}']", newSize);
    }

    public void terminateInstance(AmazonAutoScalingClient amazonASClient, String instanceId) {
        amazonASClient.terminateInstance(TerminateInstanceInAutoScalingGroupRequest.builder()
                .shouldDecrementDesiredCapacity(true)
                .instanceId(instanceId)
                .build());
    }
}
