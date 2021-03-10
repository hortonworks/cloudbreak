package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;

public class AwsResourceConnectorTest {

    private static final String DB_INSTANCE_IDENTIFIER = "dbInstance";

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private DatabaseStack dbStack;

    @Mock
    private DatabaseServer databaseServer;

    @Mock
    private AwsRdsStatusLookupService awsRdsStatusLookupService;

    @Mock
    private AwsRdsModifyService awsRdsModifyService;

    @Mock
    private AwsRdsTerminateService awsRdsTerminateService;

    @InjectMocks
    private AwsResourceConnector awsResourceConnector;

    @Before
    public void initTests() {
        initMocks(this);
        when(dbStack.getDatabaseServer()).thenReturn(databaseServer);
        when(databaseServer.getServerId()).thenReturn(DB_INSTANCE_IDENTIFIER);
    }

    @Test
    public void terminateDatabaseServerWithDeleteProtectionTest() throws Exception {
        PersistenceNotifier persistenceNotifier = mock(PersistenceNotifier.class);
        DescribeDBInstancesResult describeDBInstancesResult = mock(DescribeDBInstancesResult.class);
        when(awsRdsStatusLookupService.getDescribeDBInstancesResult(any(), any())).thenReturn(describeDBInstancesResult);
        when(awsRdsStatusLookupService.isDeleteProtectionEnabled(describeDBInstancesResult)).thenReturn(true);
        when(awsRdsStatusLookupService.isDbStackExistOnProviderSide(describeDBInstancesResult)).thenReturn(true);

        awsResourceConnector.terminateDatabaseServer(authenticatedContext, dbStack,
                Collections.emptyList(), persistenceNotifier, true);
        verify(awsRdsModifyService, times(1)).disableDeleteProtection(any(), any());
        verify(awsRdsTerminateService, times(1)).terminate(authenticatedContext, dbStack,
                true, persistenceNotifier, Collections.emptyList());

    }

    @Test
    public void terminateDatabaseServerWithOutDeleteProtectionTest() throws Exception {
        PersistenceNotifier persistenceNotifier = mock(PersistenceNotifier.class);
        DescribeDBInstancesResult describeDBInstancesResult = mock(DescribeDBInstancesResult.class);
        when(awsRdsStatusLookupService.getDescribeDBInstancesResult(any(), any())).thenReturn(describeDBInstancesResult);
        when(awsRdsStatusLookupService.isDeleteProtectionEnabled(describeDBInstancesResult)).thenReturn(false);
        when(awsRdsStatusLookupService.isDbStackExistOnProviderSide(describeDBInstancesResult)).thenReturn(true);

        awsResourceConnector.terminateDatabaseServer(authenticatedContext, dbStack,
                Collections.emptyList(), persistenceNotifier, true);
        verify(awsRdsModifyService, times(0)).disableDeleteProtection(any(), any());
        verify(awsRdsTerminateService, times(1)).terminate(authenticatedContext, dbStack,
                true, persistenceNotifier, Collections.emptyList());
    }
}
