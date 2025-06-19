package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
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
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.StartDbInstanceRequest;
import software.amazon.awssdk.services.rds.waiters.RdsWaiter;

@ExtendWith(MockitoExtension.class)
public class AwsRdsStartServiceTest {

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
    private RdsWaiter waiters;

    @InjectMocks
    private AwsRdsStartService victim;

    @BeforeEach
    public void initTests() {
        lenient().when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(awsClient.createRdsClient(any())).thenReturn(amazonRDS);
        when(dbStack.getDatabaseServer()).thenReturn(databaseServer);
        when(databaseServer.getServerId()).thenReturn(DB_INSTANCE_IDENTIFIER);
    }

    @Test
    public void shouldStartAndScheduleTask() throws InterruptedException, ExecutionException, TimeoutException {
        ArgumentCaptor<StartDbInstanceRequest> startDBInstanceRequestArgumentCaptor = ArgumentCaptor.forClass(StartDbInstanceRequest.class);
        when(amazonRDS.startDBInstance(startDBInstanceRequestArgumentCaptor.capture())).thenReturn(null);
        when(amazonRDS.waiters()).thenReturn(waiters);

        victim.start(authenticatedContext, dbStack);

        assertEquals(DB_INSTANCE_IDENTIFIER, startDBInstanceRequestArgumentCaptor.getValue().dbInstanceIdentifier());
        verify(waiters, times(1)).waitUntilDBInstanceAvailable(any(DescribeDbInstancesRequest.class), any(WaiterOverrideConfiguration.class));
    }

    @Test
    public void shouldThrowCloudConnectorExceptionInCaseOfRdsClientException() {
        ArgumentCaptor<StartDbInstanceRequest> startDBInstanceRequestArgumentCaptor = ArgumentCaptor.forClass(StartDbInstanceRequest.class);

        when(amazonRDS.startDBInstance(startDBInstanceRequestArgumentCaptor.capture())).thenThrow(new RuntimeException());

        assertThrows(CloudConnectorException.class, () -> victim.start(authenticatedContext, dbStack));
    }

    @Test
    public void shouldThrowCloudConnectorExceptionInCaseOfSchedulerException() {
        ArgumentCaptor<StartDbInstanceRequest> startDBInstanceRequestArgumentCaptor = ArgumentCaptor.forClass(StartDbInstanceRequest.class);
        when(amazonRDS.startDBInstance(startDBInstanceRequestArgumentCaptor.capture())).thenReturn(null);
        when(amazonRDS.waiters()).thenReturn(waiters);
        doThrow(SdkException.class).when(waiters).waitUntilDBInstanceAvailable(any(DescribeDbInstancesRequest.class), any(WaiterOverrideConfiguration.class));

        assertThrows(CloudConnectorException.class, () -> victim.start(authenticatedContext, dbStack));
    }
}
