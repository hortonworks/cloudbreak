package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsResourceConstants.SUSPENDED_PROCESSES;
import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.BackoffCancellablePollingStrategy.getBackoffCancellablePollingStrategy;

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

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.autoscaling.model.Activity;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesRequest;
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesResult;
import com.amazonaws.services.autoscaling.model.ResumeProcessesRequest;
import com.amazonaws.services.autoscaling.model.SuspendProcessesRequest;
import com.amazonaws.services.autoscaling.model.TerminateInstanceInAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.waiters.PollingStrategy;
import com.amazonaws.waiters.Waiter;
import com.amazonaws.waiters.WaiterParameters;
import com.amazonaws.waiters.WaiterTimedOutException;
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
            amazonASClient.suspendProcesses(new SuspendProcessesRequest().withAutoScalingGroupName(asGroupName).withScalingProcesses(SUSPENDED_PROCESSES));
        }
    }

    public void resumeAutoScaling(AmazonAutoScalingClient amazonASClient, Collection<String> groupNames, List<String> autoScalingPolicies) {
        for (String groupName : groupNames) {
            LOGGER.info("Resume autoscaling group '{}'", groupName);
            amazonASClient.resumeProcesses(new ResumeProcessesRequest().withAutoScalingGroupName(groupName).withScalingProcesses(autoScalingPolicies));
        }
    }

    public void scheduleStatusChecks(List<Group> groups, AuthenticatedContext ac, AmazonCloudFormationClient cloudFormationClient,
            Date timeBeforeASUpdate, List<String> knownInstances)
            throws AmazonAutoscalingFailed {
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
            Long stackId, List<String> knownInstances) throws AmazonAutoscalingFailed {
        Waiter<DescribeAutoScalingGroupsRequest> groupInServiceWaiter = asClient.waiters().groupInService();
        PollingStrategy backoff = getBackoffCancellablePollingStrategy(new StackCancellationCheck(stackId));
        try {
            groupInServiceWaiter.run(new WaiterParameters<>(new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(autoScalingGroupName))
                    .withPollingStrategy(backoff));
        } catch (Exception e) {
            throw new AmazonAutoscalingFailed(e.getMessage(), e);
        }

        Waiter<DescribeAutoScalingGroupsRequest> instancesInServiceWaiter = customAmazonWaiterProvider
                .getAutoscalingInstancesInServiceWaiter(asClient, requiredInstanceCount);
        try {
            instancesInServiceWaiter.run(new WaiterParameters<>(new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(autoScalingGroupName))
                    .withPollingStrategy(backoff));
        } catch (WaiterTimedOutException e) {
            String message = String.format("Polling timed out. Not all the %s instance(s) reached InService state.", requiredInstanceCount);
            throw new AmazonAutoscalingFailed(message, e);
        } catch (Exception e) {
            throw new AmazonAutoscalingFailed(e.getMessage(), e);
        }

        List<String> instanceIds = cloudFormationStackUtil.getInstanceIds(asClient, autoScalingGroupName);
        List<String> instanceIdsForWait = getInstanceIdsForWait(instanceIds, knownInstances);
        if (requiredInstanceCount != 0) {
            List<List<String>> partitionedInstanceIdsList = Lists.partition(instanceIdsForWait, MAX_INSTANCE_ID_SIZE);

            Waiter<DescribeInstancesRequest> instanceRunningStateWaiter = amClient.waiters().instanceRunning();
            for (List<String> partitionedInstanceIds : partitionedInstanceIdsList) {
                try {
                    instanceRunningStateWaiter.run(new WaiterParameters<>(new DescribeInstancesRequest().withInstanceIds(partitionedInstanceIds))
                            .withPollingStrategy(backoff));
                } catch (AmazonServiceException e) {
                    LOGGER.error("Cannot describeInstances", e);
                    e.setErrorMessage("Cannot describeInstances. " + e.getErrorMessage());
                    throw e;
                } catch (Exception e) {
                    throw new AmazonAutoscalingFailed("Error occurred in describeInstances: " + e.getMessage(), e);
                }
            }
        }
    }

    private List<String> getInstanceIdsForWait(List<String> instanceIds, List<String> knownInstances) {
        List<String> result = null;
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
    void checkLastScalingActivity(AmazonAutoScalingClient asClient, String autoScalingGroupName,
            Date timeBeforeASUpdate, Group group) throws AmazonAutoscalingFailed {
        if (group.getInstancesSize() > 0) {
            LOGGER.debug("Check last activity after AS update. Wait for the first if it doesn't exist. [asGroup: {}]", autoScalingGroupName);
            Optional<Activity> firstActivity = Optional.empty();
            try {
                AutoScalingGroup scalingGroup = asClient
                        .describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(autoScalingGroupName))
                        .getAutoScalingGroups().stream().findFirst()
                        .orElseThrow(() -> new AmazonAutoscalingFailed("Can not find autoscaling group by name: " + autoScalingGroupName));
                if (group.getInstancesSize() > scalingGroup.getInstances().size()) {
                    Waiter<DescribeScalingActivitiesRequest> autoscalingActivitiesWaiter = customAmazonWaiterProvider
                            .getAutoscalingActivitiesWaiter(asClient, timeBeforeASUpdate);
                    autoscalingActivitiesWaiter.run(new WaiterParameters<>(new DescribeScalingActivitiesRequest()
                            .withAutoScalingGroupName(autoScalingGroupName)));
                    DescribeScalingActivitiesResult describeScalingActivitiesResult = asClient
                            .describeScalingActivities(new DescribeScalingActivitiesRequest().withAutoScalingGroupName(autoScalingGroupName));

                    // if we run into InsufficientInstanceCapacity we can skip to waitForGroup because that method will wait for the required instance count
                    firstActivity = describeScalingActivitiesResult.getActivities().stream().findFirst()
                            .filter(activity -> "failed".equals(activity.getStatusCode().toLowerCase()) &&
                                    !activity.getStatusMessage().contains("InsufficientInstanceCapacity"));

                } else {
                    LOGGER.info("Skip checking activities because the AS group contains the desired instance count");
                }
            } catch (Exception e) {
                LOGGER.error("Failed to list activities: {}", e.getMessage(), e);
                throw new AmazonAutoscalingFailed(e.getMessage(), e);
            }

            if (firstActivity.isPresent()) {
                Activity activity = firstActivity.get();
                LOGGER.error("Cannot execute autoscale, because last activity is failed: {}", activity);
                throw new AmazonAutoscalingFailed(activity.getDescription() + " " + activity.getCause());
            }
        }
    }

    public void scheduleStatusChecks(List<Group> groups, AuthenticatedContext ac, AmazonCloudFormationClient cloudFormationClient)
            throws AmazonAutoscalingFailed {
        scheduleStatusChecks(groups, ac, cloudFormationClient, null, null);
    }

    public List<AutoScalingGroup> getAutoscalingGroups(AmazonAutoScalingClient amazonASClient, Set<String> groupNames) {
        return amazonASClient.describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(groupNames)).getAutoScalingGroups();
    }

    public void updateAutoscalingGroup(AmazonAutoScalingClient amazonASClient, String groupName, Integer newSize) {
        LOGGER.info("Update '{}' Auto Scaling groups max size to {}, desired capacity to {}", groupName, newSize, newSize);
        amazonASClient.updateAutoScalingGroup(new UpdateAutoScalingGroupRequest()
                .withAutoScalingGroupName(groupName)
                .withMaxSize(newSize)
                .withDesiredCapacity(newSize));
        LOGGER.debug("Updated Auto Scaling group's desiredCapacity: [to: '{}']", newSize);
    }

    public void terminateInstance(AmazonAutoScalingClient amazonASClient, String instanceId) {
        amazonASClient.terminateInstance(new TerminateInstanceInAutoScalingGroupRequest().withShouldDecrementDesiredCapacity(true).withInstanceId(instanceId));
    }
}
