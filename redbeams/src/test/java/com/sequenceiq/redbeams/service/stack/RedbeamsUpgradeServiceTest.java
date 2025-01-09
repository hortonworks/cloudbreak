package com.sequenceiq.redbeams.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowProgressResponse;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.api.model.operation.OperationProgressStatus;
import com.sequenceiq.flow.api.model.operation.OperationView;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.redbeams.converter.spi.DBStackToDatabaseStackConverter;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.domain.upgrade.UpgradeDatabaseRequest;
import com.sequenceiq.redbeams.domain.upgrade.UpgradeDatabaseResponse;
import com.sequenceiq.redbeams.dto.UpgradeDatabaseMigrationParams;
import com.sequenceiq.redbeams.flow.RedbeamsFlowManager;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsUpgradeEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.RedbeamsStartUpgradeRequest;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.RedbeamsStartValidateUpgradeRequest;
import com.sequenceiq.redbeams.service.CredentialService;
import com.sequenceiq.redbeams.service.network.NetworkBuilderService;
import com.sequenceiq.redbeams.service.operation.OperationService;
import com.sequenceiq.redbeams.service.validation.DatabaseEncryptionValidator;

@ExtendWith(MockitoExtension.class)
public class RedbeamsUpgradeServiceTest {

    private static final String SERVER_CRN_STRING = "ServerCrn";

    private static final TargetMajorVersion TARGET_MAJOR_VERSION = TargetMajorVersion.VERSION_11;

    private static final String DATABASE_SERVER_ATTRIBUTES = "{ \"dbVersion\": \"10\", \"this\": \"that\" }";

    @Mock
    private DBStackService dbStackService;

    @Mock
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Mock
    private RedbeamsFlowManager flowManager;

    @Mock
    private NetworkBuilderService networkBuilderService;

    @Mock
    private OperationService operationService;

    @Mock
    private CredentialService credentialService;

    @Mock
    private CredentialToCloudCredentialConverter credentialConverter;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private DBStackToDatabaseStackConverter databaseStackConverter;

    @Mock
    private ResourceConnector resourceConnector;

    @Mock
    private Authenticator authenticator;

    @Mock
    private DatabaseEncryptionValidator databaseEncryptionValidator;

    @InjectMocks
    private RedbeamsUpgradeService underTest;

    @Test
    void testUpgradeDatabaseServerWhenAvailable() {
        DBStack dbStack = getDbStack(Status.AVAILABLE);
        when(dbStackService.getByCrn(SERVER_CRN_STRING)).thenReturn(dbStack);
        UpgradeDatabaseRequest upgradeDatabaseRequest = getUpgradeDatabaseRequest();

        RedbeamsStartUpgradeRequest redbeamsStartUpgradeRequest = new RedbeamsStartUpgradeRequest(dbStack.getId(),
                upgradeDatabaseRequest.getTargetMajorVersion(), UpgradeDatabaseMigrationParams
                .fromDatabaseServer(upgradeDatabaseRequest.getMigratedDatabaseServer()));

        when(flowManager.notify(RedbeamsUpgradeEvent.REDBEAMS_START_UPGRADE_EVENT.selector(), redbeamsStartUpgradeRequest)).
                thenReturn(new FlowIdentifier(FlowType.FLOW, "1"));

        UpgradeDatabaseResponse response = underTest.upgradeDatabaseServer(SERVER_CRN_STRING, upgradeDatabaseRequest);

        verify(dbStackService).getByCrn(SERVER_CRN_STRING);
        verify(dbStackStatusUpdater).updateStatus(1L, DetailedDBStackStatus.UPGRADE_REQUESTED);

        assertEquals("1", response.getFlowIdentifier().getPollableId());
        assertEquals(MajorVersion.VERSION_10, response.getCurrentVersion());
        assertNull(response.getReason());

        ArgumentCaptor<RedbeamsStartUpgradeRequest> upgradeRequestArgumentCaptor = ArgumentCaptor.forClass(RedbeamsStartUpgradeRequest.class);
        verify(flowManager).notify(eq(RedbeamsUpgradeEvent.REDBEAMS_START_UPGRADE_EVENT.selector()), upgradeRequestArgumentCaptor.capture());
        RedbeamsStartUpgradeRequest actualRedbeamsStartUpgradeRequest = upgradeRequestArgumentCaptor.getValue();
        assertEquals(TARGET_MAJOR_VERSION, actualRedbeamsStartUpgradeRequest.getTargetMajorVersion());
    }

    @Test
    void testUpgradeDatabaseServerWhenUpgradeAlreadyRequested() {
        DBStack dbStack = getDbStack(Status.UPGRADE_IN_PROGRESS);
        when(dbStackService.getByCrn(SERVER_CRN_STRING)).thenReturn(dbStack);
        when(operationService.getOperationProgressByResourceCrn(SERVER_CRN_STRING, false)).
                thenReturn(getOperationViewWithStatus(OperationProgressStatus.RUNNING));

        UpgradeDatabaseRequest upgradeDatabaseRequest = getUpgradeDatabaseRequest();

        UpgradeDatabaseResponse response = underTest.upgradeDatabaseServer(SERVER_CRN_STRING, upgradeDatabaseRequest);

        assertEquals("123", response.getFlowIdentifier().getPollableId());
        assertEquals(MajorVersion.VERSION_10, response.getCurrentVersion());
        assertEquals("DatabaseServer with crn ServerCrn is already being upgraded.", response.getReason());

        verify(dbStackService).getByCrn(SERVER_CRN_STRING);
        verify(dbStackStatusUpdater, never()).updateStatus(anyLong(), any());
        verify(flowManager, never()).notify(any(), any());

        verify(dbStackService).getByCrn(SERVER_CRN_STRING);
        verify(dbStackStatusUpdater, never()).updateStatus(anyLong(), any());
        verify(flowManager, never()).notify(any(), any());
    }

    @Test
    void testUpgradeDatabaseServerWhenAlreadyOnUpgradedVersion() {
        DBStack dbStack = getDbStack(Status.AVAILABLE);
        dbStack.setMajorVersion(MajorVersion.VERSION_11);
        when(dbStackService.getByCrn(SERVER_CRN_STRING)).thenReturn(dbStack);

        UpgradeDatabaseRequest upgradeDatabaseRequest = getUpgradeDatabaseRequest();

        UpgradeDatabaseResponse response = underTest.upgradeDatabaseServer(SERVER_CRN_STRING, upgradeDatabaseRequest);

        assertNull(response.getFlowIdentifier());
        assertEquals(MajorVersion.VERSION_11, response.getCurrentVersion());
        assertEquals("DatabaseServer with crn ServerCrn is already on version 11, upgrade is not necessary.", response.getReason());

        verify(dbStackService).getByCrn(SERVER_CRN_STRING);
        verify(dbStackStatusUpdater, never()).updateStatus(anyLong(), any());
        verify(flowManager, never()).notify(any(), any());
    }

    @Test
    void testUpgradeDatabaseServerWhenAvailableAndUpdateSubnetsEnabled() {
        DBStack dbStack = getDbStack(Status.AVAILABLE);
        when(dbStackService.getByCrn(SERVER_CRN_STRING)).thenReturn(dbStack);
        UpgradeDatabaseRequest upgradeDatabaseRequest = getUpgradeDatabaseRequest();

        underTest.upgradeDatabaseServer(SERVER_CRN_STRING, upgradeDatabaseRequest);
    }

    @Test
    void testUpgradeDatabaseServerWhenUpgradeInProgressStatusWithNoRunningFlowThenReTriggerFlow() {
        DBStack dbStack = getDbStack(Status.UPGRADE_IN_PROGRESS);
        when(dbStackService.getByCrn(SERVER_CRN_STRING)).thenReturn(dbStack);
        UpgradeDatabaseRequest upgradeDatabaseRequest = getUpgradeDatabaseRequest();
        when(operationService.getOperationProgressByResourceCrn(SERVER_CRN_STRING, false)).
                thenReturn(getOperationViewWithStatus(OperationProgressStatus.FINISHED));
        RedbeamsStartUpgradeRequest redbeamsStartUpgradeRequest = new RedbeamsStartUpgradeRequest(dbStack.getId(),
                upgradeDatabaseRequest.getTargetMajorVersion(), UpgradeDatabaseMigrationParams
                .fromDatabaseServer(upgradeDatabaseRequest.getMigratedDatabaseServer()));

        when(flowManager.notify(RedbeamsUpgradeEvent.REDBEAMS_START_UPGRADE_EVENT.selector(), redbeamsStartUpgradeRequest)).
                thenReturn(new FlowIdentifier(FlowType.FLOW, "1"));

        UpgradeDatabaseResponse response = underTest.upgradeDatabaseServer(SERVER_CRN_STRING, upgradeDatabaseRequest);

        verify(dbStackService).getByCrn(SERVER_CRN_STRING);
        verify(dbStackStatusUpdater).updateStatus(1L, DetailedDBStackStatus.UPGRADE_REQUESTED);

        assertEquals("1", response.getFlowIdentifier().getPollableId());
        assertEquals(MajorVersion.VERSION_10, response.getCurrentVersion());
        assertNull(response.getReason());

        ArgumentCaptor<RedbeamsStartUpgradeRequest> upgradeRequestArgumentCaptor = ArgumentCaptor.forClass(RedbeamsStartUpgradeRequest.class);
        verify(flowManager).notify(eq(RedbeamsUpgradeEvent.REDBEAMS_START_UPGRADE_EVENT.selector()), upgradeRequestArgumentCaptor.capture());
        RedbeamsStartUpgradeRequest actualRedbeamsStartUpgradeRequest = upgradeRequestArgumentCaptor.getValue();
        assertEquals(TARGET_MAJOR_VERSION, actualRedbeamsStartUpgradeRequest.getTargetMajorVersion());
    }

    @Test
    void testValidateUpgradeDatabaseServer() throws Exception {
        // GIVEN
        DBStack dbStack = getDbStack(Status.AVAILABLE);
        UpgradeDatabaseRequest upgradeDatabaseRequest = getUpgradeDatabaseRequest();
        when(dbStackService.getByCrn(SERVER_CRN_STRING)).thenReturn(dbStack);
        RedbeamsStartValidateUpgradeRequest validateUpgradeRequest = new RedbeamsStartValidateUpgradeRequest(dbStack.getId(),
                upgradeDatabaseRequest.getTargetMajorVersion(), UpgradeDatabaseMigrationParams
                .fromDatabaseServer(upgradeDatabaseRequest.getMigratedDatabaseServer()));
        when(flowManager.notify(RedbeamsValidateUpgradeEvent.REDBEAMS_START_VALIDATE_UPGRADE_EVENT.selector(), validateUpgradeRequest)).
                thenReturn(new FlowIdentifier(FlowType.FLOW, "1"));

        // WHEN
        UpgradeDatabaseResponse actualResponse = underTest.validateUpgradeDatabaseServer(SERVER_CRN_STRING, upgradeDatabaseRequest);
        // THEN
        ArgumentCaptor<RedbeamsStartValidateUpgradeRequest> upgradeRequestArgumentCaptor = ArgumentCaptor.forClass(RedbeamsStartValidateUpgradeRequest.class);
        verify(flowManager).notify(eq(RedbeamsValidateUpgradeEvent.REDBEAMS_START_VALIDATE_UPGRADE_EVENT.selector()), upgradeRequestArgumentCaptor.capture());
        RedbeamsStartValidateUpgradeRequest actualRequest = upgradeRequestArgumentCaptor.getValue();
        assertEquals(TARGET_MAJOR_VERSION, actualRequest.getTargetMajorVersion());
        assertEquals("1", actualResponse.getFlowIdentifier().getPollableId());
        assertNull(actualResponse.getReason());
    }

    private DBStackStatus getDbStackStatus(Status status) {
        DBStackStatus dbStackStatus = new DBStackStatus();
        dbStackStatus.setStatus(status);
        return dbStackStatus;
    }

    private UpgradeDatabaseRequest getUpgradeDatabaseRequest() {
        UpgradeDatabaseRequest upgradeDatabaseRequest = new UpgradeDatabaseRequest();
        upgradeDatabaseRequest.setTargetMajorVersion(TARGET_MAJOR_VERSION);
        return upgradeDatabaseRequest;
    }

    private DBStack getDbStack(Status status) {
        DBStack dbStack = new DBStack();
        dbStack.setId(1L);
        dbStack.setCloudPlatform("AZURE");
        dbStack.setOwnerCrn(Crn.safeFromString("crn:cdp:iam:us-west-1:cloudera:user:test@cloudera.com"));
        dbStack.setEnvironmentId("envcrn");
        dbStack.setDBStackStatus(getDbStackStatus(status));
        DatabaseServer databaseServer = new DatabaseServer();
        databaseServer.setAttributes(new Json(DATABASE_SERVER_ATTRIBUTES));
        dbStack.setDatabaseServer(databaseServer);
        return dbStack;
    }

    private OperationView getOperationViewWithStatus(OperationProgressStatus status) {
        OperationView operationView = new OperationView();
        operationView.setOperations(List.of(new FlowProgressResponse()));
        operationView.setProgressStatus(status);
        operationView.setOperationId("123");
        return operationView;
    }
}