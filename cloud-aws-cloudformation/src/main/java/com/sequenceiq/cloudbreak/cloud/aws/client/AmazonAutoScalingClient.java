package com.sequenceiq.cloudbreak.cloud.aws.client;

import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationResult;
import com.amazonaws.services.autoscaling.model.DeleteLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.DeleteLaunchConfigurationResult;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsRequest;
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsResult;
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesRequest;
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesResult;
import com.amazonaws.services.autoscaling.model.DetachInstancesRequest;
import com.amazonaws.services.autoscaling.model.DetachInstancesResult;
import com.amazonaws.services.autoscaling.model.ResumeProcessesRequest;
import com.amazonaws.services.autoscaling.model.ResumeProcessesResult;
import com.amazonaws.services.autoscaling.model.SuspendProcessesRequest;
import com.amazonaws.services.autoscaling.model.SuspendProcessesResult;
import com.amazonaws.services.autoscaling.model.TerminateInstanceInAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.TerminateInstanceInAutoScalingGroupResult;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupResult;
import com.amazonaws.services.autoscaling.waiters.AmazonAutoScalingWaiters;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonClient;
import com.sequenceiq.cloudbreak.service.Retry;

public class AmazonAutoScalingClient extends AmazonClient {

    private final AmazonAutoScaling client;

    private final Retry retry;

    public AmazonAutoScalingClient(AmazonAutoScaling client, Retry retry) {
        this.client = client;
        this.retry = retry;
    }

    public SuspendProcessesResult suspendProcesses(SuspendProcessesRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.suspendProcesses(request));
    }

    public ResumeProcessesResult resumeProcesses(ResumeProcessesRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.resumeProcesses(request));
    }

    public DescribeAutoScalingGroupsResult describeAutoScalingGroups(DescribeAutoScalingGroupsRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.describeAutoScalingGroups(request));
    }

    public UpdateAutoScalingGroupResult updateAutoScalingGroup(UpdateAutoScalingGroupRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.updateAutoScalingGroup(request));
    }

    public DetachInstancesResult detachInstances(DetachInstancesRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.detachInstances(request));
    }

    public DescribeScalingActivitiesResult describeScalingActivities(DescribeScalingActivitiesRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.describeScalingActivities(request));
    }

    public TerminateInstanceInAutoScalingGroupResult terminateInstance(TerminateInstanceInAutoScalingGroupRequest terminateInstanceInAutoScalingGroupRequest) {
        return retry.testWith2SecDelayMax15Times(() ->
                client.terminateInstanceInAutoScalingGroup(terminateInstanceInAutoScalingGroupRequest));
    }

    public DescribeLaunchConfigurationsResult describeLaunchConfigurations(DescribeLaunchConfigurationsRequest launchConfigurationsRequest) {
        return client.describeLaunchConfigurations(launchConfigurationsRequest);
    }

    public DeleteLaunchConfigurationResult deleteLaunchConfiguration(DeleteLaunchConfigurationRequest deleteLaunchConfigurationRequest) {
        return client.deleteLaunchConfiguration(deleteLaunchConfigurationRequest);
    }

    // FIXME return actual waiter instead
    public AmazonAutoScalingWaiters waiters() {
        return client.waiters();
    }

    public CreateLaunchConfigurationResult createLaunchConfiguration(CreateLaunchConfigurationRequest createLaunchConfigurationRequest) {
        return client.createLaunchConfiguration(createLaunchConfigurationRequest);
    }
}
