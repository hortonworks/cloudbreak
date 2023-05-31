package com.sequenceiq.cloudbreak.cloud.aws.scheduler.acceptor;

import static software.amazon.awssdk.services.autoscaling.model.LifecycleState.IN_SERVICE;

import java.util.List;

import software.amazon.awssdk.core.waiters.WaiterAcceptor;
import software.amazon.awssdk.core.waiters.WaiterState;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsResponse;

public class AutoscalingInstancesInServiceAcceptor implements WaiterAcceptor<DescribeAutoScalingGroupsResponse> {

    private final long requiredCount;

    public AutoscalingInstancesInServiceAcceptor(long requiredCount) {
        this.requiredCount = requiredCount;
    }

    @Override
    public WaiterState waiterState() {
        return WaiterState.SUCCESS;
    }

    @Override
    public boolean matches(DescribeAutoScalingGroupsResponse response) {
        List<AutoScalingGroup> asgs = response.autoScalingGroups();
        return !asgs.isEmpty() && asgs.get(0).instances().stream()
                .filter(instance -> IN_SERVICE.equals(instance.lifecycleState())).count() == requiredCount;
    }
}
