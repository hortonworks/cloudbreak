package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.waiters.Waiter;
import com.dyngr.exception.PollerException;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.CustomAmazonWaiterProvider;
import com.sequenceiq.cloudbreak.cloud.aws.util.poller.upgrade.UpgradeStartPoller;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

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
        DescribeDBInstancesRequest describeDBInstancesRequest = new DescribeDBInstancesRequest();

        underTest.waitUntilUpgradeStarts(rdsClient, describeDBInstancesRequest);

        verify(upgradeStartPoller).waitForUpgradeToStart(any());
    }

    @Test
    void testWaitUntilUpgradeStartsWhenPollingThrows() {
        DescribeDBInstancesRequest describeDBInstancesRequest = new DescribeDBInstancesRequest();
        doThrow(PollerException.class).when(upgradeStartPoller).waitForUpgradeToStart(any());

        Assertions.assertThrows(CloudConnectorException.class, () ->
            underTest.waitUntilUpgradeStarts(rdsClient, describeDBInstancesRequest)
        );
    }

    @Test
    void testWaitUntilUpgradeFinishes() {
        CloudContext cloudContext = mock(CloudContext.class);
        when(cloudContext.getId()).thenReturn(1L);
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        when(ac.getCloudContext()).thenReturn(cloudContext);
        DescribeDBInstancesRequest describeDBInstancesRequest = new DescribeDBInstancesRequest();
        Waiter<DescribeDBInstancesRequest> rdsWaiter = mock(Waiter.class);
        when(customAmazonWaiterProvider.getDbInstanceModifyWaiter(rdsClient)).thenReturn(rdsWaiter);

        underTest.waitUntilUpgradeFinishes(ac, rdsClient, describeDBInstancesRequest);

        verify(rdsWaiter).run(any());
    }
}
