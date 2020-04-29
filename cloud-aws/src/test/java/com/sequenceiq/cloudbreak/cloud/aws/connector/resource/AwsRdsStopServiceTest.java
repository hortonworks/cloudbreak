package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.StopDBInstanceRequest;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.AwsBackoffSyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.aws.task.AwsPollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class AwsRdsStopServiceTest {

    private static final String REGION = "region";

    private static final String DB_INSTANCE_IDENTIFIER = "dbInstance";

    private static final String SUCCESS_STATUS = "stopped";

    @Mock
    private AwsClient awsClient;

    @Mock
    private AwsPollTaskFactory awsPollTaskFactory;

    @Mock
    private AwsBackoffSyncPollingScheduler<Boolean> awsBackoffSyncPollingScheduler;

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
    private AmazonRDS amazonRDS;

    @Mock
    private PollTask<Boolean> task;

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
    }

    @Test
    public void shouldStopAndScheduleTask() throws InterruptedException, ExecutionException, TimeoutException {
        ArgumentCaptor<StopDBInstanceRequest> stopDBInstanceRequestArgumentCaptor = ArgumentCaptor.forClass(StopDBInstanceRequest.class);
        when(amazonRDS.stopDBInstance(stopDBInstanceRequestArgumentCaptor.capture())).thenReturn(null);
        when(awsPollTaskFactory.newRdbStatusCheckerTask(authenticatedContext, DB_INSTANCE_IDENTIFIER, SUCCESS_STATUS, amazonRDS)).thenReturn(task);

        victim.stop(authenticatedContext, DB_INSTANCE_IDENTIFIER);

        assertEquals(DB_INSTANCE_IDENTIFIER, stopDBInstanceRequestArgumentCaptor.getValue().getDBInstanceIdentifier());
        verify(awsBackoffSyncPollingScheduler).schedule(task);
    }

    @Test(expected = CloudConnectorException.class)
    public void shouldThrowCloudConnectorExceptionInCaseOfRdsClientException() throws InterruptedException, ExecutionException, TimeoutException {
        ArgumentCaptor<StopDBInstanceRequest> stopDBInstanceRequestArgumentCaptor = ArgumentCaptor.forClass(StopDBInstanceRequest.class);

        when(amazonRDS.stopDBInstance(stopDBInstanceRequestArgumentCaptor.capture())).thenThrow(new RuntimeException());

        victim.stop(authenticatedContext, DB_INSTANCE_IDENTIFIER);
    }

    @Test(expected = CloudConnectorException.class)
    public void shouldThrowCloudConnectorExceptionInCaseOfSchedulerException() throws InterruptedException, ExecutionException, TimeoutException {
        ArgumentCaptor<StopDBInstanceRequest> stopDBInstanceRequestArgumentCaptor = ArgumentCaptor.forClass(StopDBInstanceRequest.class);
        when(amazonRDS.stopDBInstance(stopDBInstanceRequestArgumentCaptor.capture())).thenReturn(null);
        when(awsPollTaskFactory.newRdbStatusCheckerTask(authenticatedContext, DB_INSTANCE_IDENTIFIER, SUCCESS_STATUS, amazonRDS)).thenReturn(task);
        doThrow(RuntimeException.class).when(awsBackoffSyncPollingScheduler).schedule(task);

        victim.stop(authenticatedContext, DB_INSTANCE_IDENTIFIER);
    }
}