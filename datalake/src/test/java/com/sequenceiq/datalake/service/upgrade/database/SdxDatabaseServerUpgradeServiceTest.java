package com.sequenceiq.datalake.service.upgrade.database;

import static com.sequenceiq.cloudbreak.common.database.TargetMajorVersion.VERSION_11;
import static com.sequenceiq.cloudbreak.common.database.TargetMajorVersion.VERSION_14;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseServerStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.sdx.CloudbreakPoller;
import com.sequenceiq.datalake.service.sdx.CloudbreakStackService;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.database.DatabaseService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.datalake.service.validation.database.DatabaseUpgradeRuntimeValidator;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.sdx.api.model.SdxUpgradeDatabaseServerResponse;

@ExtendWith(MockitoExtension.class)
public class SdxDatabaseServerUpgradeServiceTest {

    private static final String SDX_CRN = "sdxCrn";

    private static final String DB_CRN = "dbCrn";

    private static final NameOrCrn NAME_OR_CRN = NameOrCrn.ofCrn(SDX_CRN);

    private static final String POLLABLE_ID = "pollableId";

    private static final String SDX_CLUSTER_NAME = "sdxClusterName";

    @Mock
    private SdxService sdxService;

    @Mock
    private SdxReactorFlowManager reactorFlowManager;

    @Mock
    private CloudbreakPoller cloudbreakPoller;

    @Mock
    private SdxDatabaseServerUpgradeAvailabilityChecker sdxDatabaseServerUpgradeAvailabilityService;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private CloudbreakStackService cloudbreakStackService;

    @Mock
    private DatabaseUpgradeRuntimeValidator databaseUpgradeRuntimeValidator;

    @Mock
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Mock
    private DatabaseEngineVersionReaderService databaseEngineVersionReaderService;

    @Mock
    private DatabaseService databaseService;

    @Mock
    private StackDatabaseServerResponse databaseResponse;

    @InjectMocks
    private SdxDatabaseServerUpgradeService underTest;

    @Test
    void testUpgradeWhenClusterRunningThenUpgradeTriggered() {
        TargetMajorVersion targetMajorVersion = VERSION_11;
        SdxCluster sdxCluster = getSdxCluster();
        when(sdxService.getByNameOrCrn(any(), eq(NAME_OR_CRN))).thenReturn(sdxCluster);
        SdxStatusEntity status = getDatalakeStatus(DatalakeStatusEnum.RUNNING);
        when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(status);
        sdxCluster.getSdxDatabase().setDatabaseCrn(DB_CRN);
        when(databaseService.getDatabaseServer(DB_CRN)).thenReturn(databaseResponse);
        when(sdxDatabaseServerUpgradeAvailabilityService.isUpgradeNeeded(databaseResponse, targetMajorVersion)).thenReturn(true);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, POLLABLE_ID);
        when(reactorFlowManager.triggerDatabaseServerUpgradeFlow(sdxCluster, targetMajorVersion)).thenReturn(flowIdentifier);
        when(databaseUpgradeRuntimeValidator.isRuntimeVersionAllowedForUpgrade(any())).thenReturn(true);
        when(databaseResponse.getStatus()).thenReturn(DatabaseServerStatus.AVAILABLE);

        SdxUpgradeDatabaseServerResponse response = underTest.upgrade(NAME_OR_CRN, targetMajorVersion);

        assertEquals(flowIdentifier, response.getFlowIdentifier());
        assertEquals(targetMajorVersion, response.getTargetMajorVersion());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testUpgradeWhenNoTargetMajorIsDefinedThenUpgradeToCorrectVersion(boolean onAzure) {
        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setCloudStorageFileSystemType(onAzure ? FileSystemType.ADLS_GEN_2 : FileSystemType.S3);
        TargetMajorVersion desiredVersion = onAzure ? VERSION_11 : VERSION_14;
        ReflectionTestUtils.setField(underTest, "defaultTargetMajorVersion", VERSION_14);
        ReflectionTestUtils.setField(underTest, "defaultAzureTargetMajorVersion", VERSION_11);
        when(sdxService.getByNameOrCrn(any(), eq(NAME_OR_CRN))).thenReturn(sdxCluster);
        SdxStatusEntity status = getDatalakeStatus(DatalakeStatusEnum.RUNNING);
        when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(status);
        sdxCluster.getSdxDatabase().setDatabaseCrn(DB_CRN);
        when(databaseService.getDatabaseServer(DB_CRN)).thenReturn(databaseResponse);
        when(sdxDatabaseServerUpgradeAvailabilityService.isUpgradeNeeded(databaseResponse, desiredVersion)).thenReturn(true);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, POLLABLE_ID);
        when(reactorFlowManager.triggerDatabaseServerUpgradeFlow(sdxCluster, desiredVersion)).thenReturn(flowIdentifier);
        when(databaseUpgradeRuntimeValidator.isRuntimeVersionAllowedForUpgrade(any())).thenReturn(true);
        when(databaseResponse.getStatus()).thenReturn(DatabaseServerStatus.AVAILABLE);

        SdxUpgradeDatabaseServerResponse response = underTest.upgrade(NAME_OR_CRN, null);

        assertEquals(flowIdentifier, response.getFlowIdentifier());

        assertEquals(desiredVersion, response.getTargetMajorVersion());
    }

    @Test
    void testUpgradeWhenUpgradeFailedThenUpgradeTriggered() {
        TargetMajorVersion targetMajorVersion = VERSION_11;
        SdxCluster sdxCluster = getSdxCluster();
        when(sdxService.getByNameOrCrn(any(), eq(NAME_OR_CRN))).thenReturn(sdxCluster);
        SdxStatusEntity status = getDatalakeStatus(DatalakeStatusEnum.DATALAKE_UPGRADE_DATABASE_SERVER_FAILED);
        when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(status);
        sdxCluster.getSdxDatabase().setDatabaseCrn(DB_CRN);
        when(databaseService.getDatabaseServer(DB_CRN)).thenReturn(databaseResponse);
        when(sdxDatabaseServerUpgradeAvailabilityService.isUpgradeNeeded(databaseResponse, targetMajorVersion)).thenReturn(true);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, POLLABLE_ID);
        when(reactorFlowManager.triggerDatabaseServerUpgradeFlow(sdxCluster, targetMajorVersion)).thenReturn(flowIdentifier);
        when(databaseUpgradeRuntimeValidator.isRuntimeVersionAllowedForUpgrade(any())).thenReturn(true);
        when(databaseResponse.getStatus()).thenReturn(DatabaseServerStatus.AVAILABLE);

        SdxUpgradeDatabaseServerResponse response = underTest.upgrade(NAME_OR_CRN, targetMajorVersion);

        assertEquals(flowIdentifier, response.getFlowIdentifier());
        assertEquals(targetMajorVersion, response.getTargetMajorVersion());
    }

    @ParameterizedTest
    @EnumSource(value = DatalakeStatusEnum.class,
            names = {"RUNNING",
                    "DATALAKE_UPGRADE_DATABASE_SERVER_FAILED",
                    "DATALAKE_UPGRADE_DATABASE_SERVER_REQUESTED",
                    "DATALAKE_UPGRADE_DATABASE_SERVER_IN_PROGRESS"},
            mode = EnumSource.Mode.EXCLUDE)
    void testUpgradeWhenClusterNotAvailableThenUpgradeNotTriggered(DatalakeStatusEnum datalakeStatusEnum) {
        TargetMajorVersion targetMajorVersion = VERSION_11;
        SdxCluster sdxCluster = getSdxCluster();
        when(sdxService.getByNameOrCrn(any(), eq(NAME_OR_CRN))).thenReturn(sdxCluster);
        SdxStatusEntity status = getDatalakeStatus(datalakeStatusEnum);
        when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(status);

        Assertions.assertThatCode(() -> underTest.upgrade(NAME_OR_CRN, targetMajorVersion))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(String.format("Data Lake %s is not available for database server upgrade", SDX_CLUSTER_NAME));

        verify(sdxDatabaseServerUpgradeAvailabilityService, never()).isUpgradeNeeded(any(), any());
        verify(databaseService, never()).getDatabaseServer(any());
        verify(reactorFlowManager, never()).triggerDatabaseServerUpgradeFlow(any(), any());
    }

    @Test
    void testUpgradeWhenClusterNotExistThenExceptionIsThrown() {
        TargetMajorVersion targetMajorVersion = VERSION_11;
        doThrow(new NotFoundException("SDX cluster 'testCluster' not found."))
                .when(sdxService).getByNameOrCrn(any(), eq(NAME_OR_CRN));

        Assertions.assertThatCode(() -> underTest.upgrade(NAME_OR_CRN, targetMajorVersion))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("SDX cluster 'testCluster' not found.");

        verify(sdxDatabaseServerUpgradeAvailabilityService, never()).isUpgradeNeeded(any(), any());
        verify(databaseService, never()).getDatabaseServer(any());
        verify(reactorFlowManager, never()).triggerDatabaseServerUpgradeFlow(any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = DatalakeStatusEnum.class, names = {"DATALAKE_UPGRADE_DATABASE_SERVER_REQUESTED", "DATALAKE_UPGRADE_DATABASE_SERVER_IN_PROGRESS"})
    void testUpgradeWhenUpgradeInProgressThenUpgradeNotTriggered(DatalakeStatusEnum datalakeStatusEnum) {
        TargetMajorVersion targetMajorVersion = VERSION_11;
        SdxCluster sdxCluster = getSdxCluster();
        when(sdxService.getByNameOrCrn(any(), eq(NAME_OR_CRN))).thenReturn(sdxCluster);
        SdxStatusEntity status = getDatalakeStatus(datalakeStatusEnum);
        when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(status);

        Assertions.assertThatCode(() -> underTest.upgrade(NAME_OR_CRN, targetMajorVersion))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(String.format("Database server upgrade for Data Lake %s is already in progress", SDX_CLUSTER_NAME));

        verify(sdxDatabaseServerUpgradeAvailabilityService, never()).isUpgradeNeeded(any(), any());
        verify(databaseService, never()).getDatabaseServer(any());
        verify(reactorFlowManager, never()).triggerDatabaseServerUpgradeFlow(any(), any());
    }

    @Test
    void testUpgradeWhenDatabaseRuntimeTooLowThenNotTriggered() {
        TargetMajorVersion targetMajorVersion = VERSION_11;
        SdxCluster sdxCluster = getSdxCluster("tooLow");
        when(sdxService.getByNameOrCrn(any(), eq(NAME_OR_CRN))).thenReturn(sdxCluster);
        SdxStatusEntity status = getDatalakeStatus(DatalakeStatusEnum.RUNNING);
        when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(status);
        when(databaseUpgradeRuntimeValidator.isRuntimeVersionAllowedForUpgrade(any())).thenReturn(false);
        when(databaseUpgradeRuntimeValidator.getMinRuntimeVersion()).thenReturn("minimumVersion");

        Assertions.assertThatCode(() -> underTest.upgrade(NAME_OR_CRN, targetMajorVersion))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(String.format("The database upgrade of Data Lake %s is not permitted for runtime version tooLow. The minimum supported runtime " +
                        "version is minimumVersion", SDX_CLUSTER_NAME));
    }

    @Test
    void testUpgradeWhenUpgradeCheckThrowsExceptionThenNotTriggered() {
        TargetMajorVersion targetMajorVersion = VERSION_11;
        SdxCluster sdxCluster = getSdxCluster();
        when(sdxService.getByNameOrCrn(any(), eq(NAME_OR_CRN))).thenReturn(sdxCluster);
        SdxStatusEntity status = getDatalakeStatus(DatalakeStatusEnum.RUNNING);
        when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(status);
        sdxCluster.getSdxDatabase().setDatabaseCrn(DB_CRN);
        when(databaseService.getDatabaseServer(DB_CRN)).thenReturn(databaseResponse);
        when(sdxDatabaseServerUpgradeAvailabilityService.isUpgradeNeeded(databaseResponse, targetMajorVersion)).thenReturn(true);
        when(databaseUpgradeRuntimeValidator.isRuntimeVersionAllowedForUpgrade(any())).thenReturn(true);
        when(databaseResponse.getStatus()).thenReturn(DatabaseServerStatus.AVAILABLE);
        doThrow(new BadRequestException("badrequest")).when(cloudbreakStackService).checkUpgradeRdsByClusterNameInternal(sdxCluster, targetMajorVersion);

        Assertions.assertThatCode(() -> underTest.upgrade(NAME_OR_CRN, targetMajorVersion))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("badrequest");

        verify(reactorFlowManager, never()).triggerDatabaseServerUpgradeFlow(any(), any());
    }

    @Test
    void testInitUpgradeInCb() {
        TargetMajorVersion targetMajorVersion = VERSION_11;
        SdxCluster sdxCluster = getSdxCluster();

        underTest.initUpgradeInCb(sdxCluster, targetMajorVersion);

        verify(cloudbreakStackService).upgradeRdsByClusterNameInternal(sdxCluster, targetMajorVersion);
    }

    @Test
    void testInitUpgradeWhenNotAlreadyUpgradedExceptionInCb() {
        TargetMajorVersion targetMajorVersion = VERSION_11;
        SdxCluster sdxCluster = getSdxCluster();
        String expectedCoreMessage = "Upgrading database server is not needed as it is already on the latest version (11).";
        when(cloudbreakMessagesService.getMessage(ResourceEvent.CLUSTER_RDS_UPGRADE_ALREADY_UPGRADED.getMessage(),
                List.of(targetMajorVersion.getMajorVersion()))).thenReturn(expectedCoreMessage);
        doThrow(new CloudbreakApiException("badrequest")).when(cloudbreakStackService).upgradeRdsByClusterNameInternal(sdxCluster, targetMajorVersion);

        Assertions.assertThatCode(() -> underTest.initUpgradeInCb(sdxCluster, targetMajorVersion)
                )
                .isInstanceOf(CloudbreakApiException.class)
                .hasMessage("badrequest");

        verify(cloudbreakStackService).upgradeRdsByClusterNameInternal(sdxCluster, targetMajorVersion);
        verify(cloudbreakMessagesService, times(1)).getMessage(
                eq(ResourceEvent.CLUSTER_RDS_UPGRADE_ALREADY_UPGRADED.getMessage()),
                eq(List.of(targetMajorVersion.getMajorVersion())));
        verifyNoInteractions(databaseEngineVersionReaderService);
        verifyNoInteractions(sdxService);
    }

    @Test
    void testInitUpgradeWhenAlreadyUpgradedExceptionInCb() {
        TargetMajorVersion targetMajorVersion = VERSION_11;
        SdxCluster sdxCluster = getSdxCluster();
        String expectedCoreMessage = "Upgrading database server is not needed as it is already on the latest version (11).";
        String mappedSdxMessage = String.format("Database server is already on the latest version for data lake %s", sdxCluster.getName());
        when(cloudbreakMessagesService.getMessage(ResourceEvent.CLUSTER_RDS_UPGRADE_ALREADY_UPGRADED.getMessage(),
                List.of(targetMajorVersion.getMajorVersion()))).thenReturn(expectedCoreMessage);
        when(databaseEngineVersionReaderService.getDatabaseServerMajorVersion(sdxCluster)).thenReturn(Optional.of(MajorVersion.VERSION_11));

        doThrow(new CloudbreakApiException(expectedCoreMessage)).when(cloudbreakStackService).upgradeRdsByClusterNameInternal(sdxCluster, targetMajorVersion);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.initUpgradeInCb(sdxCluster, targetMajorVersion));

        assertEquals(mappedSdxMessage, exception.getMessage());

        verify(cloudbreakStackService).upgradeRdsByClusterNameInternal(sdxCluster, targetMajorVersion);
        verify(cloudbreakMessagesService, times(1)).getMessage(
                eq(ResourceEvent.CLUSTER_RDS_UPGRADE_ALREADY_UPGRADED.getMessage()),
                eq(List.of(targetMajorVersion.getMajorVersion())));
        verify(databaseEngineVersionReaderService, times(1)).getDatabaseServerMajorVersion(sdxCluster);
        verify(sdxService, times(1)).updateDatabaseEngineVersion(eq(sdxCluster.getCrn()), eq(targetMajorVersion.getMajorVersion()));
    }

    @ParameterizedTest
    @EnumSource(value = DatabaseServerStatus.class, names = {"AVAILABLE", "UPGRADE_FAILED"})
    void testUpgradeWhenDatabaseIsAvailableForUpgradeThenUpgradeTriggered(DatabaseServerStatus dbStatus) {
        TargetMajorVersion targetMajorVersion = VERSION_11;
        SdxCluster sdxCluster = getSdxCluster();
        when(sdxService.getByNameOrCrn(any(), eq(NAME_OR_CRN))).thenReturn(sdxCluster);
        SdxStatusEntity status = getDatalakeStatus(DatalakeStatusEnum.RUNNING);
        when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(status);
        sdxCluster.getSdxDatabase().setDatabaseCrn(DB_CRN);
        when(databaseService.getDatabaseServer(DB_CRN)).thenReturn(databaseResponse);
        when(sdxDatabaseServerUpgradeAvailabilityService.isUpgradeNeeded(databaseResponse, targetMajorVersion)).thenReturn(true);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, POLLABLE_ID);
        when(reactorFlowManager.triggerDatabaseServerUpgradeFlow(sdxCluster, targetMajorVersion)).thenReturn(flowIdentifier);
        when(databaseUpgradeRuntimeValidator.isRuntimeVersionAllowedForUpgrade(any())).thenReturn(true);
        when(databaseResponse.getStatus()).thenReturn(dbStatus);

        SdxUpgradeDatabaseServerResponse response = underTest.upgrade(NAME_OR_CRN, targetMajorVersion);

        assertEquals(flowIdentifier, response.getFlowIdentifier());
        assertEquals(targetMajorVersion, response.getTargetMajorVersion());
    }

    @Test
    void testUpgradeWhenDatabaseStatusIsNullThenUpgradeNotTriggered() {
        TargetMajorVersion targetMajorVersion = VERSION_11;
        SdxCluster sdxCluster = getSdxCluster();
        when(sdxService.getByNameOrCrn(any(), eq(NAME_OR_CRN))).thenReturn(sdxCluster);
        SdxStatusEntity status = getDatalakeStatus(DatalakeStatusEnum.RUNNING);
        when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(status);
        sdxCluster.getSdxDatabase().setDatabaseCrn(DB_CRN);
        when(databaseService.getDatabaseServer(DB_CRN)).thenReturn(databaseResponse);
        when(sdxDatabaseServerUpgradeAvailabilityService.isUpgradeNeeded(databaseResponse, targetMajorVersion)).thenReturn(true);
        when(databaseUpgradeRuntimeValidator.isRuntimeVersionAllowedForUpgrade(any())).thenReturn(true);
        when(databaseResponse.getStatus()).thenReturn(null);

        Assertions.assertThatCode(() -> underTest.upgrade(NAME_OR_CRN, targetMajorVersion))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(String.format("Upgrading database server of Data Lake %s is not possible as database server is not available.", SDX_CLUSTER_NAME));

        verify(reactorFlowManager, never()).triggerDatabaseServerUpgradeFlow(any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = DatabaseServerStatus.class, names = {"AVAILABLE", "UPGRADE_FAILED"}, mode = EnumSource.Mode.EXCLUDE)
    void testUpgradeWhenDatabaseNotAvailableThenUpgradeNotTriggered(DatabaseServerStatus dbStatus) {
        TargetMajorVersion targetMajorVersion = VERSION_11;
        SdxCluster sdxCluster = getSdxCluster();
        when(sdxService.getByNameOrCrn(any(), eq(NAME_OR_CRN))).thenReturn(sdxCluster);
        SdxStatusEntity status = getDatalakeStatus(DatalakeStatusEnum.RUNNING);
        when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(status);
        sdxCluster.getSdxDatabase().setDatabaseCrn(DB_CRN);
        when(databaseService.getDatabaseServer(DB_CRN)).thenReturn(databaseResponse);
        when(sdxDatabaseServerUpgradeAvailabilityService.isUpgradeNeeded(databaseResponse, targetMajorVersion)).thenReturn(true);
        when(databaseUpgradeRuntimeValidator.isRuntimeVersionAllowedForUpgrade(any())).thenReturn(true);
        when(databaseResponse.getStatus()).thenReturn(dbStatus);

        Assertions.assertThatCode(() -> underTest.upgrade(NAME_OR_CRN, targetMajorVersion))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(String.format("Upgrading database server of Data Lake %s is not possible as database server is not available, it is in %s state.",
                        SDX_CLUSTER_NAME, dbStatus));

        verify(reactorFlowManager, never()).triggerDatabaseServerUpgradeFlow(any(), any());
    }

    @Test
    void testWaitDatabaseUpgradeInCb() {
        SdxCluster sdxCluster = getSdxCluster();
        PollingConfig pollingConfig = mock(PollingConfig.class);

        underTest.waitDatabaseUpgradeInCb(sdxCluster, pollingConfig);

        verify(cloudbreakPoller).pollDatabaseServerUpgradeUntilAvailable(sdxCluster, pollingConfig);
    }

    private SdxCluster getSdxCluster() {
        return getSdxCluster(null);
    }

    private SdxCluster getSdxCluster(String runtimeVersion) {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setCrn(SDX_CRN);
        sdxCluster.setClusterName(SDX_CLUSTER_NAME);
        if (runtimeVersion != null) {
            sdxCluster.setRuntime(runtimeVersion);
        }
        sdxCluster.setSdxDatabase(new SdxDatabase());
        return sdxCluster;
    }

    private SdxStatusEntity getDatalakeStatus(DatalakeStatusEnum statusEnum) {
        SdxStatusEntity status = new SdxStatusEntity();
        status.setStatus(statusEnum);
        return status;
    }

}
