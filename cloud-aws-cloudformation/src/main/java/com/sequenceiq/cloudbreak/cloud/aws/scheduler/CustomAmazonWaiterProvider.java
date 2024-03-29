package com.sequenceiq.cloudbreak.cloud.aws.scheduler;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.scheduler.acceptor.AutoscalingActivityAcceptor;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.acceptor.AutoscalingInstancesInServiceAcceptor;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.acceptor.DbInstanceForModifyingStartedAcceptor;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.acceptor.DbInstanceStopFailureAcceptor;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.acceptor.DbInstanceStopSuccessAcceptor;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.acceptor.DescribeDbInstanceForMasterPasswordChangeSuccessAcceptor;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.acceptor.DescribeDbInstanceForModifyFailureAcceptor;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.acceptor.DescribeDbInstanceForModifySuccessAcceptor;

import software.amazon.awssdk.core.retry.backoff.FixedDelayBackoffStrategy;
import software.amazon.awssdk.core.waiters.Waiter;
import software.amazon.awssdk.core.waiters.WaiterAcceptor;
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsResponse;
import software.amazon.awssdk.services.autoscaling.model.DescribeScalingActivitiesResponse;
import software.amazon.awssdk.services.ec2.waiters.internal.WaitersRuntime;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;

@Component
public class CustomAmazonWaiterProvider {

    private static final int MAX_ATTEMPTS = 60;

    private static final int ACTIVITIES_DEFAULT_MAX_ATTEMPTS = 120;

    private static final int DEFAULT_DELAY_IN_SECONDS = 30;

    public Waiter<DescribeAutoScalingGroupsResponse> getAutoscalingInstancesInServiceWaiter(Integer requiredCount) {
        List<WaiterAcceptor<? super DescribeAutoScalingGroupsResponse>> acceptors = new ArrayList<>();
        acceptors.add(new AutoscalingInstancesInServiceAcceptor(requiredCount));
        acceptors.addAll(WaitersRuntime.DEFAULT_ACCEPTORS);
        return Waiter.builder(DescribeAutoScalingGroupsResponse.class)
                .acceptors(acceptors)
                .overrideConfiguration(createWaiterOverrideConfiguration(MAX_ATTEMPTS))
                .build();
    }

    public Waiter<DescribeScalingActivitiesResponse> getAutoscalingActivitiesWaiter(Date timeBeforeASUpdate) {
        List<WaiterAcceptor<? super DescribeScalingActivitiesResponse>> acceptors = new ArrayList<>();
        acceptors.add(new AutoscalingActivityAcceptor(timeBeforeASUpdate));
        acceptors.addAll(WaitersRuntime.DEFAULT_ACCEPTORS);
        return Waiter.builder(DescribeScalingActivitiesResponse.class)
                .acceptors(acceptors)
                .overrideConfiguration(createWaiterOverrideConfiguration(ACTIVITIES_DEFAULT_MAX_ATTEMPTS))
                .build();
    }

    public Waiter<DescribeDbInstancesResponse> getDbInstanceStopWaiter() {
        List<WaiterAcceptor<? super DescribeDbInstancesResponse>> acceptors = new ArrayList<>();
        acceptors.add(new DbInstanceStopSuccessAcceptor());
        acceptors.add(new DbInstanceStopFailureAcceptor());
        acceptors.addAll(WaitersRuntime.DEFAULT_ACCEPTORS);
        return Waiter.builder(DescribeDbInstancesResponse.class)
                .acceptors(acceptors)
                .overrideConfiguration(createWaiterOverrideConfiguration(MAX_ATTEMPTS))
                .build();
    }

    public Waiter<DescribeDbInstancesResponse> getDbInstanceModifyWaiter() {
        List<WaiterAcceptor<? super DescribeDbInstancesResponse>> acceptors = new ArrayList<>();
        acceptors.add(new DescribeDbInstanceForModifyFailureAcceptor());
        acceptors.add(new DescribeDbInstanceForModifySuccessAcceptor());
        acceptors.addAll(WaitersRuntime.DEFAULT_ACCEPTORS);
        return Waiter.builder(DescribeDbInstancesResponse.class)
                .acceptors(acceptors)
                .overrideConfiguration(createWaiterOverrideConfiguration(MAX_ATTEMPTS))
                .build();
    }

    public Waiter<DescribeDbInstancesResponse> getDbMasterPasswordStartWaiter() {
        List<WaiterAcceptor<? super DescribeDbInstancesResponse>> acceptors = new ArrayList<>();
        acceptors.add(new DescribeDbInstanceForModifyFailureAcceptor());
        acceptors.add(new DescribeDbInstanceForMasterPasswordChangeSuccessAcceptor());
        acceptors.addAll(WaitersRuntime.DEFAULT_ACCEPTORS);
        return Waiter.builder(DescribeDbInstancesResponse.class)
                .acceptors(acceptors)
                .overrideConfiguration(createWaiterOverrideConfiguration(MAX_ATTEMPTS))
                .build();
    }

    public Waiter<DescribeDbInstancesResponse> getCertRotationStartWaiter() {
        List<WaiterAcceptor<? super DescribeDbInstancesResponse>> acceptors = new ArrayList<>();
        acceptors.add(new DescribeDbInstanceForModifyFailureAcceptor());
        acceptors.add(new DbInstanceForModifyingStartedAcceptor());
        acceptors.addAll(WaitersRuntime.DEFAULT_ACCEPTORS);
        return Waiter.builder(DescribeDbInstancesResponse.class)
                .acceptors(acceptors)
                .overrideConfiguration(createWaiterOverrideConfiguration(MAX_ATTEMPTS))
                .build();
    }

    private WaiterOverrideConfiguration createWaiterOverrideConfiguration(int maxAttempts) {
        return WaiterOverrideConfiguration.builder()
                .backoffStrategy(FixedDelayBackoffStrategy
                        .create(Duration.ofSeconds(DEFAULT_DELAY_IN_SECONDS)))
                .maxAttempts(maxAttempts)
                .build();
    }
}
