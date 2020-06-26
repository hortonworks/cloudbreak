package com.sequenceiq.cloudbreak.cloud.aws.scheduler;

import org.springframework.stereotype.Component;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.waiters.DescribeAutoScalingGroupsFunction;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.waiters.DescribeDBInstancesFunction;
import com.amazonaws.waiters.FixedDelayStrategy;
import com.amazonaws.waiters.MaxAttemptsRetryStrategy;
import com.amazonaws.waiters.PollingStrategy;
import com.amazonaws.waiters.Waiter;
import com.amazonaws.waiters.WaiterAcceptor;
import com.amazonaws.waiters.WaiterBuilder;
import com.amazonaws.waiters.WaiterExecutorServiceFactory;
import com.amazonaws.waiters.WaiterState;

@Component
public class CustomAmazonWaiterProvider {

    private static final String IN_SERVICE = "InService";

    private static final int DEFAULT_MAX_ATTEMPTS = 60;

    private static final int DEFAULT_DELAY_IN_SECONDS = 30;

    public Waiter<DescribeAutoScalingGroupsRequest> getAutoscalingInstancesInServiceWaiter(AmazonAutoScalingClient asClient, Integer requiredCount) {
        return new WaiterBuilder<DescribeAutoScalingGroupsRequest, DescribeAutoScalingGroupsResult>()
                .withSdkFunction(new DescribeAutoScalingGroupsFunction(asClient))
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

    public Waiter<DescribeDBInstancesRequest> getDbInstanceStopWaiter(AmazonRDS rdsClient) {
        return new WaiterBuilder<DescribeDBInstancesRequest, DescribeDBInstancesResult>()
                .withSdkFunction(new DescribeDBInstancesFunction(rdsClient))
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
}
