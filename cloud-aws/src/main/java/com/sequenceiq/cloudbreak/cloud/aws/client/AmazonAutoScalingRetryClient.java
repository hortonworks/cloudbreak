package com.sequenceiq.cloudbreak.cloud.aws.client;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesRequest;
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesResult;
import com.amazonaws.services.autoscaling.model.DetachInstancesRequest;
import com.amazonaws.services.autoscaling.model.DetachInstancesResult;
import com.amazonaws.services.autoscaling.model.ResumeProcessesRequest;
import com.amazonaws.services.autoscaling.model.ResumeProcessesResult;
import com.amazonaws.services.autoscaling.model.SuspendProcessesRequest;
import com.amazonaws.services.autoscaling.model.SuspendProcessesResult;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupResult;
import com.sequenceiq.cloudbreak.service.Retry;

public class AmazonAutoScalingRetryClient extends AmazonRetryClient {

    private final AmazonAutoScalingClient client;

    private final Retry retry;

    public AmazonAutoScalingRetryClient(AmazonAutoScalingClient client, Retry retry) {
        this.client = client;
        this.retry = retry;
    }

    public SuspendProcessesResult suspendProcesses(SuspendProcessesRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.suspendProcesses(request)));
    }

    public ResumeProcessesResult resumeProcesses(ResumeProcessesRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.resumeProcesses(request)));
    }

    public DescribeAutoScalingGroupsResult describeAutoScalingGroups(DescribeAutoScalingGroupsRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.describeAutoScalingGroups(request)));
    }

    public UpdateAutoScalingGroupResult updateAutoScalingGroup(UpdateAutoScalingGroupRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.updateAutoScalingGroup(request)));
    }

    public DetachInstancesResult detachInstances(DetachInstancesRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.detachInstances(request)));
    }

    public DescribeScalingActivitiesResult describeScalingActivities(DescribeScalingActivitiesRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.describeScalingActivities(request)));
    }
}
