package com.sequenceiq.redbeams.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.domain.stack.Network;
import com.sequenceiq.redbeams.domain.upgrade.UpgradeDatabaseRequest;
import com.sequenceiq.redbeams.flow.RedbeamsFlowManager;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsUpgradeEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.RedbeamsStartUpgradeRequest;
import com.sequenceiq.redbeams.service.network.NetworkBuilderService;

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

    @InjectMocks
    private RedbeamsUpgradeService underTest;

    @Test
    void testUpgradeDatabaseServerWhenAvailable() {
        DBStack dbStack = getDbStack(Status.AVAILABLE);
        when(dbStackService.getByCrn(SERVER_CRN_STRING)).thenReturn(dbStack);
        UpgradeDatabaseRequest upgradeDatabaseRequest = getUpgradeDatabaseRequest();
        when(networkBuilderService.updateNetworkSubnets(dbStack)).thenReturn(dbStack);

        underTest.upgradeDatabaseServer(SERVER_CRN_STRING, upgradeDatabaseRequest);

        verify(dbStackService).getByCrn(SERVER_CRN_STRING);
        verify(dbStackStatusUpdater).updateStatus(1L, DetailedDBStackStatus.UPGRADE_REQUESTED);

        ArgumentCaptor<RedbeamsStartUpgradeRequest> upgradeRequestArgumentCaptor = ArgumentCaptor.forClass(RedbeamsStartUpgradeRequest.class);
        verify(flowManager).notify(eq(RedbeamsUpgradeEvent.REDBEAMS_START_UPGRADE_EVENT.selector()), upgradeRequestArgumentCaptor.capture());
        RedbeamsStartUpgradeRequest actualRedbeamsStartUpgradeRequest = upgradeRequestArgumentCaptor.getValue();
        assertEquals(TARGET_MAJOR_VERSION, actualRedbeamsStartUpgradeRequest.getTargetMajorVersion());
    }

    @Test
    void testUpgradeDatabaseServerWhenUpgradeAlreadyRequested() {
        DBStack dbStack = getDbStack(Status.UPGRADE_IN_PROGRESS);
        when(dbStackService.getByCrn(SERVER_CRN_STRING)).thenReturn(dbStack);

        UpgradeDatabaseRequest upgradeDatabaseRequest = getUpgradeDatabaseRequest();

        underTest.upgradeDatabaseServer(SERVER_CRN_STRING, upgradeDatabaseRequest);

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

        underTest.upgradeDatabaseServer(SERVER_CRN_STRING, upgradeDatabaseRequest);

        verify(dbStackService).getByCrn(SERVER_CRN_STRING);
        verify(dbStackStatusUpdater, never()).updateStatus(anyLong(), any());
        verify(flowManager, never()).notify(any(), any());
    }

    @Test
    void testUpgradeDatabaseServerWhenAvailableAndUpdateSubnetsEnabled() {
        DBStack dbStack = getDbStack(Status.AVAILABLE);
        when(dbStackService.getByCrn(SERVER_CRN_STRING)).thenReturn(dbStack);
        UpgradeDatabaseRequest upgradeDatabaseRequest = getUpgradeDatabaseRequest();
        when(networkBuilderService.updateNetworkSubnets(dbStack)).thenReturn(dbStack);

        underTest.upgradeDatabaseServer(SERVER_CRN_STRING, upgradeDatabaseRequest);

        verify(dbStackService).getByCrn(SERVER_CRN_STRING);
        verify(dbStackStatusUpdater).updateStatus(1L, DetailedDBStackStatus.UPGRADE_REQUESTED);

        ArgumentCaptor<RedbeamsStartUpgradeRequest> upgradeRequestArgumentCaptor = ArgumentCaptor.forClass(RedbeamsStartUpgradeRequest.class);
        verify(flowManager).notify(eq(RedbeamsUpgradeEvent.REDBEAMS_START_UPGRADE_EVENT.selector()), upgradeRequestArgumentCaptor.capture());
        RedbeamsStartUpgradeRequest actualRedbeamsStartUpgradeRequest = upgradeRequestArgumentCaptor.getValue();
        assertEquals(TARGET_MAJOR_VERSION, actualRedbeamsStartUpgradeRequest.getTargetMajorVersion());
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
        dbStack.setEnvironmentId("envcrn");
        dbStack.setDBStackStatus(getDbStackStatus(status));
        DatabaseServer databaseServer = new DatabaseServer();
        databaseServer.setAttributes(new Json(DATABASE_SERVER_ATTRIBUTES));
        dbStack.setDatabaseServer(databaseServer);
        Network network = new Network();
        network.setAttributes(new Json(Collections.emptyMap()));
        dbStack.setNetwork(network);
        return dbStack;
    }
}
