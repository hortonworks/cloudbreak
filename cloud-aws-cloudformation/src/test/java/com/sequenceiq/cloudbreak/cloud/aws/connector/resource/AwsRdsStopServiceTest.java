package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.CustomAmazonWaiterProvider;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.waiters.Waiter;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;
import software.amazon.awssdk.services.rds.model.StopDbInstanceRequest;

@ExtendWith(MockitoExtension.class)
public class AwsRdsStopServiceTest {

    private static final String REGION = "region";

    private static final String DB_INSTANCE_IDENTIFIER = "dbInstance";

    @Mock
    private AwsCloudFormationClient awsClient;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private Location location;

    @Mock
    private Region region;

    @Mock
    private AmazonRdsClient amazonRDS;

    @Mock
    private DatabaseStack dbStack;

    @Mock
    private DatabaseServer databaseServer;

    @Mock
    private Waiter<DescribeDbInstancesResponse> rdsWaiter;

    @Mock
    private CustomAmazonWaiterProvider provider;

    @InjectMocks
    private AwsRdsStopService victim;

    @BeforeEach
    public void initTests() {
        when(awsClient.createRdsClient(any())).thenReturn(amazonRDS);
        when(dbStack.getDatabaseServer()).thenReturn(databaseServer);
        when(databaseServer.getServerId()).thenReturn(DB_INSTANCE_IDENTIFIER);
    }

    @Test
    public void shouldStopAndScheduleTask() throws InterruptedException, ExecutionException, TimeoutException {
        ArgumentCaptor<StopDbInstanceRequest> stopDBInstanceRequestArgumentCaptor = ArgumentCaptor.forClass(StopDbInstanceRequest.class);
        when(amazonRDS.stopDBInstance(stopDBInstanceRequestArgumentCaptor.capture())).thenReturn(null);
        when(provider.getDbInstanceStopWaiter()).thenReturn(rdsWaiter);

        victim.stop(authenticatedContext, dbStack);

        assertEquals(DB_INSTANCE_IDENTIFIER, stopDBInstanceRequestArgumentCaptor.getValue().dbInstanceIdentifier());
        verify(rdsWaiter, times(1)).run(any());
    }

    @Test
    public void shouldThrowCloudConnectorExceptionInCaseOfRdsClientException() {
        ArgumentCaptor<StopDbInstanceRequest> stopDBInstanceRequestArgumentCaptor = ArgumentCaptor.forClass(StopDbInstanceRequest.class);

        when(amazonRDS.stopDBInstance(stopDBInstanceRequestArgumentCaptor.capture())).thenThrow(new RuntimeException());

        assertThrows(CloudConnectorException.class, () -> victim.stop(authenticatedContext, dbStack));
    }

    @Test
    public void shouldThrowCloudConnectorExceptionInCaseOfSchedulerException() {
        ArgumentCaptor<StopDbInstanceRequest> stopDBInstanceRequestArgumentCaptor = ArgumentCaptor.forClass(StopDbInstanceRequest.class);
        when(amazonRDS.stopDBInstance(stopDBInstanceRequestArgumentCaptor.capture())).thenReturn(null);
        when(provider.getDbInstanceStopWaiter()).thenReturn(rdsWaiter);
        doThrow(SdkClientException.class).when(rdsWaiter).run(any());

        assertThrows(CloudConnectorException.class, () -> victim.stop(authenticatedContext, dbStack));
    }
}
