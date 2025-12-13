package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.exception.PollerException;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.CustomAmazonWaiterProvider;
import com.sequenceiq.cloudbreak.cloud.aws.util.poller.upgrade.UpgradeStartPoller;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

import software.amazon.awssdk.core.waiters.Waiter;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;

@ExtendWith(MockitoExtension.class)
public class AwsRdsUpgradeWaitOperationsTest {

    @Mock
    private UpgradeStartPoller upgradeStartPoller;

    @Mock
    private CustomAmazonWaiterProvider customAmazonWaiterProvider;

    @InjectMocks
    private AwsRdsUpgradeWaitOperations underTest;

    @Mock
    private AmazonRdsClient rdsClient;

    @Test
    void testWaitUntilUpgradeStarts() {
        underTest.waitUntilUpgradeStarts(rdsClient, DescribeDbInstancesRequest.builder().build());

        verify(upgradeStartPoller).waitForUpgradeToStart(any());
    }

    @Test
    void testWaitUntilUpgradeStartsWhenPollingThrows() {
        doThrow(PollerException.class).when(upgradeStartPoller).waitForUpgradeToStart(any());

        assertThrows(CloudConnectorException.class, () ->
            underTest.waitUntilUpgradeStarts(rdsClient, DescribeDbInstancesRequest.builder().build())
        );
    }

    @Test
    void testWaitUntilUpgradeFinishes() {
        Waiter<DescribeDbInstancesResponse> rdsWaiter = mock(Waiter.class);
        when(customAmazonWaiterProvider.getDbInstanceModifyWaiter()).thenReturn(rdsWaiter);

        underTest.waitUntilUpgradeFinishes(rdsClient, DescribeDbInstancesRequest.builder().build());

        verify(rdsWaiter).run(any());
    }
}
