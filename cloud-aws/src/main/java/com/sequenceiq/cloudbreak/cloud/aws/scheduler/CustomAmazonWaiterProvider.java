package com.sequenceiq.cloudbreak.cloud.aws.scheduler;

import java.util.Date;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.amazonaws.services.autoscaling.model.Activity;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesRequest;
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesResult;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.waiters.FixedDelayStrategy;
import com.amazonaws.waiters.MaxAttemptsRetryStrategy;
import com.amazonaws.waiters.PollingStrategy;
import com.amazonaws.waiters.Waiter;
import com.amazonaws.waiters.WaiterAcceptor;
import com.amazonaws.waiters.WaiterBuilder;
import com.amazonaws.waiters.WaiterExecutorServiceFactory;
import com.amazonaws.waiters.WaiterState;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.acceptor.DescribeDbInstanceForModifyFailureAcceptor;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.acceptor.DescribeDbInstanceForModifySuccessAcceptor;

@Component
public class CustomAmazonWaiterProvider {

    private static final String IN_SERVICE = "InService";

    private static final int DEFAULT_MAX_ATTEMPTS = 60;

    private static final int ACTIVITIES_DEFAULT_MAX_ATTEMPTS = 120;

    private static final int DEFAULT_DELAY_IN_SECONDS = 30;

    @Inject
    private DescribeDbInstanceForModifySuccessAcceptor describeDbInstanceForModifySuccessAcceptor;

    @Inject
    private DescribeDbInstanceForModifyFailureAcceptor describeDbInstanceForModifyFailureAcceptor;

    public Waiter<DescribeAutoScalingGroupsRequest> getAutoscalingInstancesInServiceWaiter(AmazonAutoScalingClient asClient, Integer requiredCount) {
        return new WaiterBuilder<DescribeAutoScalingGroupsRequest, DescribeAutoScalingGroupsResult>()
                .withSdkFunction(asClient::describeAutoScalingGroups)
                .withAcceptors(new WaiterAcceptor<DescribeAutoScalingGroupsResult>() {
                    @Override
                    public boolean matches(DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult) {
                        return describeAutoScalingGroupsResult.getAutoScalingGroups().get(0).getInstances()
                                .stream().filter(instance -> IN_SERVICE.equals(instance.getLifecycleState())).count() == requiredCount;
                    }

                    @Override
                    public WaiterState getState() {
                        return WaiterState.SUCCESS;
                    }
                })
                .withDefaultPollingStrategy(new PollingStrategy(new MaxAttemptsRetryStrategy(DEFAULT_MAX_ATTEMPTS),
                        new FixedDelayStrategy(DEFAULT_DELAY_IN_SECONDS)))
                .withExecutorService(WaiterExecutorServiceFactory.buildExecutorServiceForWaiter("AmazonRDSWaiters")).build();
    }

    public Waiter<DescribeScalingActivitiesRequest> getAutoscalingActivitiesWaiter(AmazonAutoScalingClient asClient, Date timeBeforeASUpdate) {
        return new WaiterBuilder<DescribeScalingActivitiesRequest, DescribeScalingActivitiesResult>()
                .withSdkFunction(asClient::describeScalingActivities)
                .withAcceptors(new WaiterAcceptor<DescribeScalingActivitiesResult>() {
                    @Override
                    public boolean matches(DescribeScalingActivitiesResult describeScalingActivitiesResult) {
                        Optional<Activity> firstActivity = describeScalingActivitiesResult.getActivities().stream().findFirst();
                        return firstActivity
                                .filter(activity -> timeBeforeASUpdate == null ||
                                        (activity.getEndTime() != null && activity.getEndTime().after(timeBeforeASUpdate)))
                                .isPresent();
                    }

                    @Override
                    public WaiterState getState() {
                        return WaiterState.SUCCESS;
                    }
                })
                .withDefaultPollingStrategy(new PollingStrategy(new MaxAttemptsRetryStrategy(ACTIVITIES_DEFAULT_MAX_ATTEMPTS),
                        new FixedDelayStrategy(DEFAULT_DELAY_IN_SECONDS)))
                .withExecutorService(WaiterExecutorServiceFactory.buildExecutorServiceForWaiter("AmazonRDSWaiters")).build();
    }

    public Waiter<DescribeDBInstancesRequest> getDbInstanceStopWaiter(AmazonRdsClient rdsClient) {
        return new WaiterBuilder<DescribeDBInstancesRequest, DescribeDBInstancesResult>()
                .withSdkFunction(rdsClient::describeDBInstances)
                .withAcceptors(new WaiterAcceptor<DescribeDBInstancesResult>() {
                    @Override
                    public boolean matches(DescribeDBInstancesResult describeDBInstancesResult) {
                        return describeDBInstancesResult.getDBInstances().stream().allMatch(instance -> "stopped".equals(instance.getDBInstanceStatus()));
                    }

                    @Override
                    public WaiterState getState() {
                        return WaiterState.SUCCESS;
                    }
                }, new WaiterAcceptor<DescribeDBInstancesResult>() {
                    @Override
                    public boolean matches(DescribeDBInstancesResult describeDBInstancesResult) {
                        return describeDBInstancesResult.getDBInstances().stream()
                                .anyMatch(instance ->
                                        "failed".equals(instance.getDBInstanceStatus())
                                                || "deleting".equals(instance.getDBInstanceStatus())
                                                || "deleted".equals(instance.getDBInstanceStatus())
                                );
                    }

                    @Override
                    public WaiterState getState() {
                        return WaiterState.FAILURE;
                    }
                })
                .withDefaultPollingStrategy(new PollingStrategy(new MaxAttemptsRetryStrategy(DEFAULT_MAX_ATTEMPTS),
                        new FixedDelayStrategy(DEFAULT_DELAY_IN_SECONDS)))
                .withExecutorService(WaiterExecutorServiceFactory.buildExecutorServiceForWaiter("AmazonRDSWaiters")).build();
    }

    public Waiter<DescribeDBInstancesRequest> getDbInstanceModifyWaiter(AmazonRdsClient rdsClient) {
        return new WaiterBuilder<DescribeDBInstancesRequest, DescribeDBInstancesResult>()
                .withSdkFunction(rdsClient::describeDbInstances)
                .withAcceptors(describeDbInstanceForModifySuccessAcceptor, describeDbInstanceForModifyFailureAcceptor)
                .withDefaultPollingStrategy(new PollingStrategy(new MaxAttemptsRetryStrategy(DEFAULT_MAX_ATTEMPTS),
                        new FixedDelayStrategy(DEFAULT_DELAY_IN_SECONDS)))
                .withExecutorService(WaiterExecutorServiceFactory.buildExecutorServiceForWaiter("AmazonRDSWaiters")).build();
    }
}
