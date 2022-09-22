package com.sequenceiq.cloudbreak.cloud.aws.scheduler.acceptor;

import java.util.Date;
import java.util.Optional;

import software.amazon.awssdk.core.waiters.WaiterAcceptor;
import software.amazon.awssdk.core.waiters.WaiterState;
import software.amazon.awssdk.services.autoscaling.model.Activity;
import software.amazon.awssdk.services.autoscaling.model.DescribeScalingActivitiesResponse;

public class AutoscalingActivityAcceptor implements WaiterAcceptor<DescribeScalingActivitiesResponse> {

    private final Date timeBeforeASUpdate;

    public AutoscalingActivityAcceptor(Date timeBeforeASUpdate) {
        this.timeBeforeASUpdate = timeBeforeASUpdate;
    }

    @Override
    public WaiterState waiterState() {
        return WaiterState.SUCCESS;
    }

    @Override
    public boolean matches(DescribeScalingActivitiesResponse response) {
        Optional<Activity> firstActivity = response.activities().stream().findFirst();
        return firstActivity
                .filter(activity -> timeBeforeASUpdate == null || (activity.endTime() != null && activity.endTime().isAfter(timeBeforeASUpdate.toInstant())))
                .isPresent();
    }
}
