package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.database.CloudDatabaseServerSslCertificate;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;

@ExtendWith(MockitoExtension.class)
class AwsResourceConnectorTest {

    private static final String DB_INSTANCE_IDENTIFIER = "dbInstance";

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private DatabaseStack dbStack;

    @Mock
    private AwsRdsStatusLookupService awsRdsStatusLookupService;

    @Mock
    private AwsRdsModifyService awsRdsModifyService;

    @Mock
    private AwsRdsTerminateService awsRdsTerminateService;

    @InjectMocks
    private AwsResourceConnector underTest;

    @Test
    void terminateDatabaseServerWithDeleteProtectionTest() throws Exception {
        DatabaseServer databaseServer = mock(DatabaseServer.class);
        when(dbStack.getDatabaseServer()).thenReturn(databaseServer);
        when(databaseServer.getServerId()).thenReturn(DB_INSTANCE_IDENTIFIER);

        PersistenceNotifier persistenceNotifier = mock(PersistenceNotifier.class);
        DescribeDBInstancesResult describeDBInstancesResult = mock(DescribeDBInstancesResult.class);
        when(awsRdsStatusLookupService.getDescribeDBInstancesResultForDeleteProtection(any(), any())).thenReturn(describeDBInstancesResult);
        when(awsRdsStatusLookupService.isDeleteProtectionEnabled(describeDBInstancesResult)).thenReturn(true);

        underTest.terminateDatabaseServer(authenticatedContext, dbStack,
                Collections.emptyList(), persistenceNotifier, true);
        verify(awsRdsModifyService, times(1)).disableDeleteProtection(any(), any());
        verify(awsRdsTerminateService, times(1)).terminate(authenticatedContext, dbStack,
                true, persistenceNotifier, Collections.emptyList());

    }

    @Test
    void terminateDatabaseServerWithOutDeleteProtectionTest() throws Exception {
        PersistenceNotifier persistenceNotifier = mock(PersistenceNotifier.class);
        DescribeDBInstancesResult describeDBInstancesResult = mock(DescribeDBInstancesResult.class);
        when(awsRdsStatusLookupService.getDescribeDBInstancesResultForDeleteProtection(any(), any())).thenReturn(describeDBInstancesResult);
        when(awsRdsStatusLookupService.isDeleteProtectionEnabled(describeDBInstancesResult)).thenReturn(false);

        underTest.terminateDatabaseServer(authenticatedContext, dbStack,
                Collections.emptyList(), persistenceNotifier, true);
        verify(awsRdsModifyService, times(0)).disableDeleteProtection(any(), any());
        verify(awsRdsTerminateService, times(1)).terminate(authenticatedContext, dbStack,
                true, persistenceNotifier, Collections.emptyList());
    }

    @Test
    void getDatabaseServerActiveSslRootCertificateTest() {
        CloudDatabaseServerSslCertificate sslCertificate = mock(CloudDatabaseServerSslCertificate.class);
        when(awsRdsStatusLookupService.getActiveSslRootCertificate(authenticatedContext, dbStack)).thenReturn(sslCertificate);

        CloudDatabaseServerSslCertificate result = underTest.getDatabaseServerActiveSslRootCertificate(authenticatedContext, dbStack);

        assertThat(result).isSameAs(sslCertificate);
    }

}
