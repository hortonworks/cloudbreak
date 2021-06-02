package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.StopDBInstanceRequest;
import com.amazonaws.waiters.Waiter;
import com.amazonaws.waiters.WaiterTimedOutException;
import com.sequenceiq.cloudbreak.cloud.aws.LegacyAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.CustomAmazonWaiterProvider;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;

public class AwsRdsStopServiceTest {

    private static final String REGION = "region";

    private static final String DB_INSTANCE_IDENTIFIER = "dbInstance";

    private static final String SUCCESS_STATUS = "stopped";

    @Mock
    private LegacyAwsClient awsClient;

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
    private Waiter<DescribeDBInstancesRequest> rdsWaiter;

    @Mock
    private CustomAmazonWaiterProvider provider;

    @InjectMocks
    private AwsRdsStopService victim;

    @Before
    public void initTests() {
        initMocks(this);

        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location);
        when(location.getRegion()).thenReturn(region);
        when(region.value()).thenReturn(REGION);
        when(awsClient.createRdsClient(any(AwsCredentialView.class), eq(REGION))).thenReturn(amazonRDS);
        when(dbStack.getDatabaseServer()).thenReturn(databaseServer);
        when(databaseServer.getServerId()).thenReturn(DB_INSTANCE_IDENTIFIER);
        when(provider.getDbInstanceStopWaiter(any())).thenReturn(rdsWaiter);
    }

    @Test
    public void shouldStopAndScheduleTask() throws InterruptedException, ExecutionException, TimeoutException {
        ArgumentCaptor<StopDBInstanceRequest> stopDBInstanceRequestArgumentCaptor = ArgumentCaptor.forClass(StopDBInstanceRequest.class);
        when(amazonRDS.stopDBInstance(stopDBInstanceRequestArgumentCaptor.capture())).thenReturn(null);

        victim.stop(authenticatedContext, dbStack);

        assertEquals(DB_INSTANCE_IDENTIFIER, stopDBInstanceRequestArgumentCaptor.getValue().getDBInstanceIdentifier());
        verify(rdsWaiter, times(1)).run(any());
    }

    @Test(expected = CloudConnectorException.class)
    public void shouldThrowCloudConnectorExceptionInCaseOfRdsClientException() throws InterruptedException, ExecutionException, TimeoutException {
        ArgumentCaptor<StopDBInstanceRequest> stopDBInstanceRequestArgumentCaptor = ArgumentCaptor.forClass(StopDBInstanceRequest.class);

        when(amazonRDS.stopDBInstance(stopDBInstanceRequestArgumentCaptor.capture())).thenThrow(new RuntimeException());

        victim.stop(authenticatedContext, dbStack);
    }

    @Test(expected = CloudConnectorException.class)
    public void shouldThrowCloudConnectorExceptionInCaseOfSchedulerException() throws InterruptedException, ExecutionException, TimeoutException {
        ArgumentCaptor<StopDBInstanceRequest> stopDBInstanceRequestArgumentCaptor = ArgumentCaptor.forClass(StopDBInstanceRequest.class);
        when(amazonRDS.stopDBInstance(stopDBInstanceRequestArgumentCaptor.capture())).thenReturn(null);
        doThrow(WaiterTimedOutException.class).when(rdsWaiter).run(any());

        victim.stop(authenticatedContext, dbStack);
    }
}