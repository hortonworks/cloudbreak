package com.sequenceiq.cloudbreak.cloud.aws.util.poller.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptState;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;

import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;

@ExtendWith(MockitoExtension.class)
public class UpgradeStartWaitTaskTest {

    @Mock
    private AmazonRdsClient rdsClient;

    @InjectMocks
    private UpgradeStartWaitTask underTest;

    @Test
    void testProcessWhenUpgradingThenFinishes() {
        when(rdsClient.describeDBInstances(any())).thenReturn(setupDescribeDbInstancesResult("upgrading"));

        AttemptResult<Boolean> result = underTest.process();

        assertEquals(AttemptState.FINISH, result.getState());
        assertTrue(result.getResult());
    }

    @Test
    void testProcessWhenMultipleUpgradingStateThenFinishes() {
        when(rdsClient.describeDBInstances(any())).thenReturn(setupDescribeDbInstancesResult("upgrading", "upgrading"));

        AttemptResult<Boolean> result = underTest.process();

        assertEquals(AttemptState.FINISH, result.getState());
        assertTrue(result.getResult());
    }

    @Test
    void testProcessWhenMixedStatesWithOneUpgradingThenFinishes() {
        when(rdsClient.describeDBInstances(any())).thenReturn(setupDescribeDbInstancesResult("upgrading", "a state"));

        AttemptResult<Boolean> result = underTest.process();

        assertEquals(AttemptState.FINISH, result.getState());
        assertTrue(result.getResult());
    }

    @Test
    void testProcessWhenNotUpgradingThenContinue() {
        when(rdsClient.describeDBInstances(any())).thenReturn(setupDescribeDbInstancesResult("a state"));

        AttemptResult<Boolean> result = underTest.process();

        assertEquals(AttemptState.CONTINUE, result.getState());
    }

    private DescribeDbInstancesResponse setupDescribeDbInstancesResult(String... instanceStates) {
        Set<DBInstance> dbInstances = new HashSet<>();
        for (String instanceState : instanceStates) {
            dbInstances.add(DBInstance.builder().dbInstanceStatus(instanceState).build());
        }
        return DescribeDbInstancesResponse.builder().dbInstances(dbInstances).build();
    }

}
