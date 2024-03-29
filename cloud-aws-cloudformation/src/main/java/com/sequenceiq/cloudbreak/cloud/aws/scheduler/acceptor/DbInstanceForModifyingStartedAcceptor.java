package com.sequenceiq.cloudbreak.cloud.aws.scheduler.acceptor;

import software.amazon.awssdk.core.waiters.WaiterAcceptor;
import software.amazon.awssdk.core.waiters.WaiterState;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;

public class DbInstanceForModifyingStartedAcceptor implements WaiterAcceptor<DescribeDbInstancesResponse> {

    @Override
    public WaiterState waiterState() {
        return WaiterState.SUCCESS;
    }

    @Override
    public boolean matches(DescribeDbInstancesResponse response) {
        return response.dbInstances()
                .stream()
                .allMatch(instance -> "modifying".equals(instance.dbInstanceStatus()));
    }

}
