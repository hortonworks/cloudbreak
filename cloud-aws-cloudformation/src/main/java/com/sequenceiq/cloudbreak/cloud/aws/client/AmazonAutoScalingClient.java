package com.sequenceiq.cloudbreak.cloud.aws.client;


import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonClient;
import com.sequenceiq.cloudbreak.service.Retry;

import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.CreateLaunchConfigurationRequest;
import software.amazon.awssdk.services.autoscaling.model.CreateLaunchConfigurationResponse;
import software.amazon.awssdk.services.autoscaling.model.DeleteLaunchConfigurationRequest;
import software.amazon.awssdk.services.autoscaling.model.DeleteLaunchConfigurationResponse;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsResponse;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingInstancesRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingInstancesResponse;
import software.amazon.awssdk.services.autoscaling.model.DescribeLaunchConfigurationsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeLaunchConfigurationsResponse;
import software.amazon.awssdk.services.autoscaling.model.DescribeScalingActivitiesRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeScalingActivitiesResponse;
import software.amazon.awssdk.services.autoscaling.model.DetachInstancesRequest;
import software.amazon.awssdk.services.autoscaling.model.DetachInstancesResponse;
import software.amazon.awssdk.services.autoscaling.model.ResumeProcessesRequest;
import software.amazon.awssdk.services.autoscaling.model.ResumeProcessesResponse;
import software.amazon.awssdk.services.autoscaling.model.SuspendProcessesRequest;
import software.amazon.awssdk.services.autoscaling.model.SuspendProcessesResponse;
import software.amazon.awssdk.services.autoscaling.model.TerminateInstanceInAutoScalingGroupRequest;
import software.amazon.awssdk.services.autoscaling.model.TerminateInstanceInAutoScalingGroupResponse;
import software.amazon.awssdk.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import software.amazon.awssdk.services.autoscaling.model.UpdateAutoScalingGroupResponse;
import software.amazon.awssdk.services.autoscaling.waiters.AutoScalingWaiter;

public class AmazonAutoScalingClient extends AmazonClient {

    private final AutoScalingClient client;

    private final Retry retry;

    public AmazonAutoScalingClient(AutoScalingClient client, Retry retry) {
        this.client = client;
        this.retry = retry;
    }

    public SuspendProcessesResponse suspendProcesses(SuspendProcessesRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.suspendProcesses(request));
    }

    public ResumeProcessesResponse resumeProcesses(ResumeProcessesRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.resumeProcesses(request));
    }

    public DescribeAutoScalingGroupsResponse describeAutoScalingGroups(DescribeAutoScalingGroupsRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.describeAutoScalingGroups(request));
    }

    public UpdateAutoScalingGroupResponse updateAutoScalingGroup(UpdateAutoScalingGroupRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.updateAutoScalingGroup(request));
    }

    public DetachInstancesResponse detachInstances(DetachInstancesRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.detachInstances(request));
    }

    public DescribeAutoScalingInstancesResponse describeAutoScalingInstances(DescribeAutoScalingInstancesRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.describeAutoScalingInstances(request));
    }

    public DescribeScalingActivitiesResponse describeScalingActivities(DescribeScalingActivitiesRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> client.describeScalingActivities(request));
    }

    public TerminateInstanceInAutoScalingGroupResponse terminateInstance(TerminateInstanceInAutoScalingGroupRequest terminateInstanceInAutoScalingGroupRequest) {
        return retry.testWith2SecDelayMax15Times(() ->
                client.terminateInstanceInAutoScalingGroup(terminateInstanceInAutoScalingGroupRequest));
    }

    public DescribeLaunchConfigurationsResponse describeLaunchConfigurations(DescribeLaunchConfigurationsRequest launchConfigurationsRequest) {
        return client.describeLaunchConfigurations(launchConfigurationsRequest);
    }

    public DeleteLaunchConfigurationResponse deleteLaunchConfiguration(DeleteLaunchConfigurationRequest deleteLaunchConfigurationRequest) {
        return client.deleteLaunchConfiguration(deleteLaunchConfigurationRequest);
    }

    // FIXME return actual waiter instead
    public AutoScalingWaiter waiters() {
        return client.waiter();
    }

    public CreateLaunchConfigurationResponse createLaunchConfiguration(CreateLaunchConfigurationRequest createLaunchConfigurationRequest) {
        return client.createLaunchConfiguration(createLaunchConfigurationRequest);
    }
}
