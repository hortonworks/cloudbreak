package com.sequenceiq.cloudbreak.cloud.aws.scheduler.acceptor;

import software.amazon.awssdk.core.waiters.WaiterAcceptor;
import software.amazon.awssdk.core.waiters.WaiterState;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;

public class DbInstanceStopFailureAcceptor implements WaiterAcceptor<DescribeDbInstancesResponse> {

    @Override
    public WaiterState waiterState() {
        return WaiterState.FAILURE;
    }

    @Override
    public boolean matches(DescribeDbInstancesResponse response) {
        return response.dbInstances().stream()
                .anyMatch(instance ->
                        "failed".equals(instance.dbInstanceStatus())
                                || "deleting".equals(instance.dbInstanceStatus())
                                || "deleted".equals(instance.dbInstanceStatus())
                );
    }
}
