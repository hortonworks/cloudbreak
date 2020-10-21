package com.sequenceiq.cloudbreak.cloud.aws.scheduler.acceptor;

import org.springframework.stereotype.Component;

import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.waiters.WaiterAcceptor;
import com.amazonaws.waiters.WaiterState;

@Component
public class DescribeDbInstanceForModifyFailureAcceptor extends WaiterAcceptor<DescribeDBInstancesResult> {

    @Override
    public boolean matches(DescribeDBInstancesResult describeDBInstancesResult) {
        return describeDBInstancesResult.getDBInstances().stream()
                .anyMatch(instance ->
                        "failed".equals(instance.getDBInstanceStatus())
                                || "deleted".equals(instance.getDBInstanceStatus())
                );
    }

    @Override
    public WaiterState getState() {
        return WaiterState.FAILURE;
    }
}
