package com.sequenceiq.cloudbreak.cloud.aws.scheduler.acceptor;

import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.acceptor.DBInstanceStatuses.DB_STATUSES;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.waiters.WaiterState;

class DescribeDbInstanceForModifySuccessAcceptorTest {

    private final DescribeDbInstanceForModifySuccessAcceptor underTest = new DescribeDbInstanceForModifySuccessAcceptor();

    @Test
    void matches() {
        DB_STATUSES.forEach(status1 -> {
            DB_STATUSES.forEach(status2 -> {
                boolean expected = "available".equals(status1) && "available".equals(status2) ||
                        "available".equals(status1) && "stopped".equals(status2) ||
                        "stopped".equals(status1) && "available".equals(status2) ||
                        "stopped".equals(status1) && "stopped".equals(status2);
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
        assertThat(underTest.getState()).isEqualTo(WaiterState.SUCCESS);
    }
}
