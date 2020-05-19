package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBInstanceNotFoundException;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class AwsRdsStatusLookupServiceTest {

    private static final String REGION = "region";

    private static final String DB_INSTANCE_IDENTIFIER = "dbInstance";

    private static final String DB_INSTANCE_STATUS_STARTED = "available";

    private static final String DB_INSTANCE_STATUS_STOPPED = "stopped";

    private static final String DB_INSTANCE_STATUS_ANY = "any";

    @Mock
    private AwsClient awsClient;

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
    private DescribeDBInstancesResult describeDBInstancesResult;

    @Mock
    private DBInstance dbInstance;

    @Mock
    private DatabaseStack dbStack;

    @Mock
    private DatabaseServer databaseServer;

    @InjectMocks
    private AwsRdsStatusLookupService victim;

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
    }

    @Test
    public void shouldLookupStartedExternalDatabaseStatus() {
        when(amazonRDS.describeDBInstances(any(DescribeDBInstancesRequest.class))).thenReturn(describeDBInstancesResult);
        when(describeDBInstancesResult.getDBInstances()).thenReturn(Arrays.asList(dbInstance));
        when(dbInstance.getDBInstanceStatus()).thenReturn(DB_INSTANCE_STATUS_STARTED);

        ExternalDatabaseStatus result = victim.getStatus(authenticatedContext, dbStack);

        assertEquals(ExternalDatabaseStatus.STARTED, result);
    }

    @Test
    public void shouldLookupStoppedExternalDatabaseStatus() {
        when(amazonRDS.describeDBInstances(any(DescribeDBInstancesRequest.class))).thenReturn(describeDBInstancesResult);
        when(describeDBInstancesResult.getDBInstances()).thenReturn(Arrays.asList(dbInstance));
        when(dbInstance.getDBInstanceStatus()).thenReturn(DB_INSTANCE_STATUS_STOPPED);

        ExternalDatabaseStatus result = victim.getStatus(authenticatedContext, dbStack);

        assertEquals(ExternalDatabaseStatus.STOPPED, result);
    }

    @Test
    public void shouldLookupUpdateInProgressExternalDatabaseStatus() {
        when(amazonRDS.describeDBInstances(any(DescribeDBInstancesRequest.class))).thenReturn(describeDBInstancesResult);
        when(describeDBInstancesResult.getDBInstances()).thenReturn(Arrays.asList(dbInstance));
        when(dbInstance.getDBInstanceStatus()).thenReturn(DB_INSTANCE_STATUS_ANY);

        ExternalDatabaseStatus result = victim.getStatus(authenticatedContext, dbStack);

        assertEquals(ExternalDatabaseStatus.UPDATE_IN_PROGRESS, result);
    }

    @Test
    public void shouldReturnDeletedInCaseOfDBInstanceNotFoundException() {
        when(amazonRDS.describeDBInstances(any(DescribeDBInstancesRequest.class))).thenThrow(DBInstanceNotFoundException.class);

        ExternalDatabaseStatus result = victim.getStatus(authenticatedContext, dbStack);

        assertEquals(ExternalDatabaseStatus.DELETED, result);
    }

    @Test(expected = CloudConnectorException.class)
    public void shouldThrowCloudConnectorExceptionInCaseOfAnyRuntimeException() {
        when(amazonRDS.describeDBInstances(any(DescribeDBInstancesRequest.class))).thenThrow(RuntimeException.class);

        victim.getStatus(authenticatedContext, dbStack);
    }
}