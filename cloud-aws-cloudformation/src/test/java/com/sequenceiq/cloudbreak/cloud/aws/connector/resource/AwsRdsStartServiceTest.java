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
import com.amazonaws.services.rds.model.StartDBInstanceRequest;
import com.amazonaws.services.rds.waiters.AmazonRDSWaiters;
import com.amazonaws.waiters.Waiter;
import com.amazonaws.waiters.WaiterTimedOutException;
import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;

public class AwsRdsStartServiceTest {

    private static final String REGION = "region";

    private static final String DB_INSTANCE_IDENTIFIER = "dbInstance";

    private static final String SUCCESS_STATUS = "available";

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
    private Waiter<DescribeDBInstancesRequest> rdsWaiter;

    @Mock
    private AmazonRDSWaiters waiters;

    @InjectMocks
    private AwsRdsStartService victim;

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
        when(amazonRDS.waiters()).thenReturn(waiters);
        when(waiters.dBInstanceAvailable()).thenReturn(rdsWaiter);
    }

    @Test
    public void shouldStartAndScheduleTask() throws InterruptedException, ExecutionException, TimeoutException {
        ArgumentCaptor<StartDBInstanceRequest> startDBInstanceRequestArgumentCaptor = ArgumentCaptor.forClass(StartDBInstanceRequest.class);
        when(amazonRDS.startDBInstance(startDBInstanceRequestArgumentCaptor.capture())).thenReturn(null);

        victim.start(authenticatedContext, dbStack);

        assertEquals(DB_INSTANCE_IDENTIFIER, startDBInstanceRequestArgumentCaptor.getValue().getDBInstanceIdentifier());
        verify(rdsWaiter, times(1)).run(any());
    }

    @Test(expected = CloudConnectorException.class)
    public void shouldThrowCloudConnectorExceptionInCaseOfRdsClientException() throws InterruptedException, ExecutionException, TimeoutException {
        ArgumentCaptor<StartDBInstanceRequest> startDBInstanceRequestArgumentCaptor = ArgumentCaptor.forClass(StartDBInstanceRequest.class);

        when(amazonRDS.startDBInstance(startDBInstanceRequestArgumentCaptor.capture())).thenThrow(new RuntimeException());

        victim.start(authenticatedContext, dbStack);
    }

    @Test(expected = CloudConnectorException.class)
    public void shouldThrowCloudConnectorExceptionInCaseOfSchedulerException() throws InterruptedException, ExecutionException, TimeoutException {
        ArgumentCaptor<StartDBInstanceRequest> startDBInstanceRequestArgumentCaptor = ArgumentCaptor.forClass(StartDBInstanceRequest.class);
        when(amazonRDS.startDBInstance(startDBInstanceRequestArgumentCaptor.capture())).thenReturn(null);
        doThrow(WaiterTimedOutException.class).when(rdsWaiter).run(any());

        victim.start(authenticatedContext, dbStack);
    }
}