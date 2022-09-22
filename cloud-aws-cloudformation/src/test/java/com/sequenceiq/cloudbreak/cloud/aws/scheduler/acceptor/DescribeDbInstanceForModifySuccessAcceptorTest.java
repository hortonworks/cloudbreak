package com.sequenceiq.cloudbreak.cloud.aws.scheduler.acceptor;

import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.acceptor.DBInstanceStatuses.DB_STATUSES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import software.amazon.awssdk.core.waiters.WaiterState;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;

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
                DBInstance dbInstance1 = DBInstance.builder().dbInstanceStatus(status1).build();
                DBInstance dbInstance2 = DBInstance.builder().dbInstanceStatus(status2).build();
                DescribeDbInstancesResponse describeDBInstanceResponse = DescribeDbInstancesResponse.builder().dbInstances(dbInstance1, dbInstance2).build();
                boolean matches = underTest.matches(describeDBInstanceResponse);
                assertThat(matches)
                        .withFailMessage("matches expected as '%s' but was '%s' for DB instance states '%s' and '%s'",
                                expected, matches, status1, status2)
                        .isEqualTo(expected);
            });
        });
    }

    @Test
    void getState() {
        assertEquals(WaiterState.SUCCESS, underTest.waiterState());
    }
}
