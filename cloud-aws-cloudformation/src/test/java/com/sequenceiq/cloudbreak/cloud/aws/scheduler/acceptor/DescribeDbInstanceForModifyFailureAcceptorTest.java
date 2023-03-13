package com.sequenceiq.cloudbreak.cloud.aws.scheduler.acceptor;

import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.acceptor.DBInstanceStatuses.DB_STATUSES;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import software.amazon.awssdk.core.waiters.WaiterState;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;

class DescribeDbInstanceForModifyFailureAcceptorTest {

    private final DescribeDbInstanceForModifyFailureAcceptor underTest = new DescribeDbInstanceForModifyFailureAcceptor();

    @Test
    void matches() {
        DB_STATUSES.forEach(status1 -> {
            DB_STATUSES.forEach(status2 -> {
                boolean expected = "failed".equals(status1) ||
                        "failed".equals(status2) ||
                        "deleted".equals(status1) ||
                        "deleted".equals(status2);
                DBInstance dbInstance1 = DBInstance.builder().dbInstanceStatus(status1).build();
                DBInstance dbInstance2 = DBInstance.builder().dbInstanceStatus(status2).build();
                DescribeDbInstancesResponse describeDBInstanceResult = DescribeDbInstancesResponse.builder().dbInstances(dbInstance1, dbInstance2).build();
                boolean matches = underTest.matches(describeDBInstanceResult);
                assertThat(matches)
                        .withFailMessage("matches expected as '%s' but was '%s' for DB instance states '%s' and '%s'",
                                expected, matches, status1, status2)
                        .isEqualTo(expected);
            });
        });
    }

    @Test
    void getState() {
        assertThat(underTest.waiterState()).isEqualTo(WaiterState.FAILURE);
    }
}
