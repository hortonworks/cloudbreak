package com.sequenceiq.redbeams.sync.provider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.cloudbreak.cloud.model.database.ExternalDatabaseParameters;
import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.service.stack.DBStackService;
import com.sequenceiq.redbeams.sync.DBStackConnector;
import com.sequenceiq.redbeams.sync.DBStackConnector.ConnectedDatabaseStack;

@ExtendWith(MockitoExtension.class)
class RdsProviderSyncServiceTest {

    private static final String CRN = "crn:cdp:redbeams:us-west-1:acc:databaseServer:res";

    @Mock
    private DBStackConnector dbStackConnector;

    @Mock
    private DBStackService dbStackService;

    @Mock
    private RdsProviderSyncConfig config;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private ResourceConnector resourceConnector;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private DatabaseStack databaseStack;

    @Mock
    private DBStack dbStack;

    @Mock
    private DatabaseServer databaseServer;

    @InjectMocks
    private RdsProviderSyncService underTest;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(dbStack.getResourceCrn()).thenReturn(CRN);
        lenient().when(dbStack.getDatabaseServer()).thenReturn(databaseServer);
        lenient().when(dbStackConnector.connect(dbStack)).thenReturn(new ConnectedDatabaseStack(cloudConnector, authenticatedContext, databaseStack));
        lenient().when(cloudConnector.resources()).thenReturn(resourceConnector);
        lenient().when(config.isUpdateInstanceType()).thenReturn(true);
        lenient().when(dbStack.getMajorVersion()).thenReturn(MajorVersion.VERSION_10);
    }

    private void stubParameters(String instanceType, String engineVersion) throws Exception {
        when(resourceConnector.getDatabaseServerParameters(authenticatedContext, databaseStack))
                .thenReturn(new ExternalDatabaseParameters(ExternalDatabaseStatus.STARTED, null, null, instanceType, engineVersion));
    }

    @Test
    void shouldUpdateInstanceTypeWhenDrifted() throws Exception {
        when(databaseServer.getInstanceType()).thenReturn("db.t3.medium");
        stubParameters("db.r5.large", "10");

        underTest.syncInstanceTypeAndVersion(dbStack);

        verify(databaseServer).setInstanceType("db.r5.large");
        verify(dbStackService).save(dbStack);
    }

    @Test
    void shouldNotUpdateInstanceTypeWhenUnchanged() throws Exception {
        when(databaseServer.getInstanceType()).thenReturn("db.t3.medium");
        stubParameters("db.t3.medium", "10");

        underTest.syncInstanceTypeAndVersion(dbStack);

        verify(databaseServer, never()).setInstanceType(any());
        verify(dbStackService, never()).save(any());
    }

    @Test
    void shouldNotUpdateInstanceTypeWhenUpdateDisabled() throws Exception {
        when(config.isUpdateInstanceType()).thenReturn(false);
        when(databaseServer.getInstanceType()).thenReturn("db.t3.medium");
        stubParameters("db.r5.large", "10");

        underTest.syncInstanceTypeAndVersion(dbStack);

        verify(databaseServer, never()).setInstanceType(any());
        verify(dbStackService, never()).save(any());
    }

    @Test
    void shouldNotUpdateInstanceTypeWhenProviderValueBlank() throws Exception {
        stubParameters(null, "10");

        underTest.syncInstanceTypeAndVersion(dbStack);

        verify(databaseServer, never()).setInstanceType(any());
        verify(dbStackService, never()).save(any());
    }

    @Test
    void shouldNotWriteWhenOnlyVersionDrifts() throws Exception {
        when(databaseServer.getInstanceType()).thenReturn("db.t3.medium");
        when(dbStack.getMajorVersion()).thenReturn(MajorVersion.VERSION_10);
        stubParameters("db.t3.medium", "14.8");

        underTest.syncInstanceTypeAndVersion(dbStack);

        verify(databaseServer, never()).setInstanceType(any());
        verify(dbStackService, never()).save(any());
    }

    @Test
    void shouldSwallowProviderException() throws Exception {
        when(resourceConnector.getDatabaseServerParameters(authenticatedContext, databaseStack)).thenThrow(new RuntimeException("boom"));

        underTest.syncInstanceTypeAndVersion(dbStack);

        verify(dbStackService, never()).save(any());
    }

    @Test
    void shouldSkipWhenParametersNull() throws Exception {
        when(resourceConnector.getDatabaseServerParameters(authenticatedContext, databaseStack)).thenReturn(null);

        underTest.syncInstanceTypeAndVersion(dbStack);

        verify(dbStackService, never()).save(any());
    }
}
