package com.sequenceiq.cloudbreak.cloud.aws.scheduler.acceptor;

import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.acceptor.DBInstanceStatuses.DB_STATUSES;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.waiters.WaiterState;

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
                DBInstance dbInstance1 = new DBInstance().withDBInstanceStatus(status1);
                DBInstance dbInstance2 = new DBInstance().withDBInstanceStatus(status2);
                DescribeDBInstancesResult describeDBInstanceResult = new DescribeDBInstancesResult().withDBInstances(dbInstance1, dbInstance2);
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
        assertThat(underTest.getState()).isEqualTo(WaiterState.FAILURE);
    }
}
