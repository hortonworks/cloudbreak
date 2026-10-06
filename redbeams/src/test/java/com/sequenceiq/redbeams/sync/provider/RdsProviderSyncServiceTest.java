package com.sequenceiq.redbeams.sync.provider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

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
import com.sequenceiq.cloudbreak.cloud.model.database.DatabaseServerStorageMetrics;
import com.sequenceiq.cloudbreak.cloud.model.database.ExternalDatabaseParameters;
import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.events.RedbeamsEventSenderService;
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

    @Mock
    private RedbeamsEventSenderService eventSenderService;

    @InjectMocks
    private RdsProviderSyncService underTest;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(dbStack.getResourceCrn()).thenReturn(CRN);
        lenient().when(dbStack.getDatabaseServer()).thenReturn(databaseServer);
        lenient().when(dbStackConnector.connect(dbStack)).thenReturn(new ConnectedDatabaseStack(cloudConnector, authenticatedContext, databaseStack));
        lenient().when(cloudConnector.resources()).thenReturn(resourceConnector);
        lenient().when(config.isUpdateInstanceType()).thenReturn(true);
        lenient().when(config.isUpdateVersion()).thenReturn(true);
        lenient().when(dbStack.getMajorVersion()).thenReturn(MajorVersion.VERSION_10);
        // Non-null provider parameters by default (blank instance type / version so the instance-type and version steps no-op),
        // so storage-monitoring tests reach checkStorageAndNotify. Instance-type tests override this via stubParameters(...).
        lenient().when(resourceConnector.getDatabaseServerParameters(authenticatedContext, databaseStack))
                .thenReturn(new ExternalDatabaseParameters(ExternalDatabaseStatus.STARTED, null, null, null, null));
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
    void shouldSkipInstanceTypeButSyncVersionWhenDatabaseServerNull() throws Exception {
        when(dbStack.getDatabaseServer()).thenReturn(null);
        when(dbStack.getMajorVersion()).thenReturn(MajorVersion.VERSION_10);
        stubParameters("db.r5.large", "14.8");

        underTest.syncInstanceTypeAndVersion(dbStack);

        verify(databaseServer, never()).setInstanceType(any());
        verify(dbStack).setMajorVersion(MajorVersion.VERSION_14);
        verify(dbStackService).save(dbStack);
    }

    @Test
    void shouldUpdateVersionWhenDrifted() throws Exception {
        when(databaseServer.getInstanceType()).thenReturn("db.t3.medium");
        when(dbStack.getMajorVersion()).thenReturn(MajorVersion.VERSION_10);
        stubParameters("db.t3.medium", "14.8");

        underTest.syncInstanceTypeAndVersion(dbStack);

        verify(databaseServer, never()).setInstanceType(any());
        verify(dbStack).setMajorVersion(MajorVersion.VERSION_14);
        verify(dbStackService).save(dbStack);
    }

    @Test
    void shouldNotUpdateVersionWhenUpdateDisabled() throws Exception {
        when(config.isUpdateVersion()).thenReturn(false);
        when(databaseServer.getInstanceType()).thenReturn("db.t3.medium");
        when(dbStack.getMajorVersion()).thenReturn(MajorVersion.VERSION_10);
        stubParameters("db.t3.medium", "14.8");

        underTest.syncInstanceTypeAndVersion(dbStack);

        verify(dbStack, never()).setMajorVersion(any());
        verify(dbStackService, never()).save(any());
    }

    @Test
    void shouldNotUpdateVersionWhenProviderValueUnrecognized() throws Exception {
        when(databaseServer.getInstanceType()).thenReturn("db.t3.medium");
        when(dbStack.getMajorVersion()).thenReturn(MajorVersion.VERSION_10);
        stubParameters("db.t3.medium", "bogus");

        underTest.syncInstanceTypeAndVersion(dbStack);

        verify(dbStack, never()).setMajorVersion(any());
        verify(dbStackService, never()).save(any());
    }

    @Test
    void shouldSaveOnceWhenBothInstanceTypeAndVersionDrift() throws Exception {
        when(databaseServer.getInstanceType()).thenReturn("db.t3.medium");
        when(dbStack.getMajorVersion()).thenReturn(MajorVersion.VERSION_10);
        stubParameters("db.r5.large", "14.8");

        underTest.syncInstanceTypeAndVersion(dbStack);

        verify(databaseServer).setInstanceType("db.r5.large");
        verify(dbStack).setMajorVersion(MajorVersion.VERSION_14);
        verify(dbStackService).save(dbStack);
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

    // Stubs the full path up to the low/not-low comparison: monitoring on, threshold at 10%, and the reported free %.
    private void enableStorageMonitoringWithMetrics(Double freePercentage) throws Exception {
        when(config.isStorageMonitoringEnabled()).thenReturn(true);
        when(config.getStorageLowThresholdPercentage()).thenReturn(10.0d);
        when(resourceConnector.getDatabaseServerStorageMetrics(authenticatedContext, databaseStack))
                .thenReturn(Optional.of(new DatabaseServerStorageMetrics(freePercentage, null, null)));
    }

    @Test
    void shouldNotifyWhenStorageLowAndNotPreviouslyAlerted() throws Exception {
        enableStorageMonitoringWithMetrics(5.0d);
        when(dbStack.isLowStorage()).thenReturn(false);

        underTest.syncInstanceTypeAndVersion(dbStack);

        verify(eventSenderService).sendStorageLowNotification(dbStack, 5.0d);
        verify(dbStack).setLowStorage(true);
        verify(dbStackService).save(dbStack);
    }

    @Test
    void shouldNotNotifyAgainWhenAlreadyAlerted() throws Exception {
        enableStorageMonitoringWithMetrics(5.0d);
        when(dbStack.isLowStorage()).thenReturn(true);

        underTest.syncInstanceTypeAndVersion(dbStack);

        verify(eventSenderService, never()).sendStorageLowNotification(any(), anyDouble());
        verify(dbStack, never()).setLowStorage(any(Boolean.class));
        verify(dbStackService, never()).save(any());
    }

    @Test
    void shouldClearAlertWhenStorageRecovered() throws Exception {
        enableStorageMonitoringWithMetrics(50.0d);
        when(dbStack.isLowStorage()).thenReturn(true);

        underTest.syncInstanceTypeAndVersion(dbStack);

        verify(eventSenderService, never()).sendStorageLowNotification(any(), anyDouble());
        verify(dbStack).setLowStorage(false);
        verify(dbStackService).save(dbStack);
    }

    @Test
    void shouldDoNothingWhenStorageOkAndNoAlert() throws Exception {
        enableStorageMonitoringWithMetrics(50.0d);
        when(dbStack.isLowStorage()).thenReturn(false);

        underTest.syncInstanceTypeAndVersion(dbStack);

        verify(eventSenderService, never()).sendStorageLowNotification(any(), anyDouble());
        verify(dbStack, never()).setLowStorage(any(Boolean.class));
        verify(dbStackService, never()).save(any());
    }

    @Test
    void shouldTreatThresholdBoundaryAsNotLow() throws Exception {
        enableStorageMonitoringWithMetrics(10.0d);
        when(dbStack.isLowStorage()).thenReturn(false);

        underTest.syncInstanceTypeAndVersion(dbStack);

        verify(eventSenderService, never()).sendStorageLowNotification(any(), anyDouble());
        verify(dbStackService, never()).save(any());
    }

    @Test
    void shouldSkipStorageCheckWhenMonitoringDisabled() throws Exception {
        when(config.isStorageMonitoringEnabled()).thenReturn(false);

        underTest.syncInstanceTypeAndVersion(dbStack);

        verify(resourceConnector, never()).getDatabaseServerStorageMetrics(any(), any());
        verify(eventSenderService, never()).sendStorageLowNotification(any(), anyDouble());
    }

    @Test
    void shouldSkipStorageCheckWhenMetricsEmpty() throws Exception {
        when(config.isStorageMonitoringEnabled()).thenReturn(true);
        when(resourceConnector.getDatabaseServerStorageMetrics(authenticatedContext, databaseStack)).thenReturn(Optional.empty());

        underTest.syncInstanceTypeAndVersion(dbStack);

        verify(eventSenderService, never()).sendStorageLowNotification(any(), anyDouble());
        verify(dbStackService, never()).save(any());
    }

    @Test
    void shouldSkipStorageCheckWhenFreePercentageNull() throws Exception {
        when(config.isStorageMonitoringEnabled()).thenReturn(true);
        when(resourceConnector.getDatabaseServerStorageMetrics(authenticatedContext, databaseStack))
                .thenReturn(Optional.of(new DatabaseServerStorageMetrics(null, null, null)));

        underTest.syncInstanceTypeAndVersion(dbStack);

        verify(eventSenderService, never()).sendStorageLowNotification(any(), anyDouble());
        verify(dbStackService, never()).save(any());
    }

    @Test
    void shouldSwallowStorageException() throws Exception {
        when(config.isStorageMonitoringEnabled()).thenReturn(true);
        when(resourceConnector.getDatabaseServerStorageMetrics(authenticatedContext, databaseStack)).thenThrow(new RuntimeException("boom"));

        underTest.syncInstanceTypeAndVersion(dbStack);

        verify(eventSenderService, never()).sendStorageLowNotification(any(), anyDouble());
        verify(dbStackService, never()).save(any());
    }
}
