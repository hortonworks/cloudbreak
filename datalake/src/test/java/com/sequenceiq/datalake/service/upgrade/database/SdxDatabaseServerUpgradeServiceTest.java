package com.sequenceiq.datalake.service.upgrade.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.sdx.CloudbreakPoller;
import com.sequenceiq.datalake.service.sdx.CloudbreakStackService;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.datalake.service.validation.database.DatabaseUpgradeRuntimeValidator;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.sdx.api.model.SdxUpgradeDatabaseServerResponse;

@ExtendWith(MockitoExtension.class)
public class SdxDatabaseServerUpgradeServiceTest {

    private static final String SDX_CRN = "sdxCrn";

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

    @InjectMocks
    private SdxDatabaseServerUpgradeService underTest;

    @Test
    void testUpgradeWhenClusterRunningThenUpgradeTriggered() {
        TargetMajorVersion targetMajorVersion = TargetMajorVersion.VERSION_11;
        SdxCluster sdxCluster = getSdxCluster();
        when(sdxService.getByNameOrCrn(any(), eq(NAME_OR_CRN))).thenReturn(sdxCluster);
        SdxStatusEntity status = getDatalakeStatus(DatalakeStatusEnum.RUNNING);
        when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(status);
        when(sdxDatabaseServerUpgradeAvailabilityService.isUpgradeNeeded(sdxCluster, targetMajorVersion)).thenReturn(true);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, POLLABLE_ID);
        when(reactorFlowManager.triggerDatabaseServerUpgradeFlow(sdxCluster, targetMajorVersion)).thenReturn(flowIdentifier);
        when(databaseUpgradeRuntimeValidator.isRuntimeVersionAllowedForUpgrade(any())).thenReturn(true);

        SdxUpgradeDatabaseServerResponse response = underTest.upgrade(NAME_OR_CRN, targetMajorVersion);

        assertEquals(flowIdentifier, response.getFlowIdentifier());
        assertEquals(targetMajorVersion, response.getTargetMajorVersion());
    }

    @Test
    void testUpgradeWhenNoTargetMajorIsDefinedThenUpgradeTo11() {
        SdxCluster sdxCluster = getSdxCluster();
        ReflectionTestUtils.setField(underTest, "defaultTargetMajorVersion", TargetMajorVersion.VERSION_11);
        when(sdxService.getByNameOrCrn(any(), eq(NAME_OR_CRN))).thenReturn(sdxCluster);
        SdxStatusEntity status = getDatalakeStatus(DatalakeStatusEnum.RUNNING);
        when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(status);
        when(sdxDatabaseServerUpgradeAvailabilityService.isUpgradeNeeded(sdxCluster, TargetMajorVersion.VERSION_11)).thenReturn(true);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, POLLABLE_ID);
        when(reactorFlowManager.triggerDatabaseServerUpgradeFlow(sdxCluster, TargetMajorVersion.VERSION_11)).thenReturn(flowIdentifier);
        when(databaseUpgradeRuntimeValidator.isRuntimeVersionAllowedForUpgrade(any())).thenReturn(true);

        SdxUpgradeDatabaseServerResponse response = underTest.upgrade(NAME_OR_CRN, null);

        assertEquals(flowIdentifier, response.getFlowIdentifier());
        assertEquals(TargetMajorVersion.VERSION_11, response.getTargetMajorVersion());
    }

    @Test
    void testUpgradeWhenUpgradeFailedThenUpgradeTriggered() {
        TargetMajorVersion targetMajorVersion = TargetMajorVersion.VERSION_11;
        SdxCluster sdxCluster = getSdxCluster();
        when(sdxService.getByNameOrCrn(any(), eq(NAME_OR_CRN))).thenReturn(sdxCluster);
        SdxStatusEntity status = getDatalakeStatus(DatalakeStatusEnum.DATALAKE_UPGRADE_DATABASE_SERVER_FAILED);
        when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(status);
        when(sdxDatabaseServerUpgradeAvailabilityService.isUpgradeNeeded(sdxCluster, targetMajorVersion)).thenReturn(true);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, POLLABLE_ID);
        when(reactorFlowManager.triggerDatabaseServerUpgradeFlow(sdxCluster, targetMajorVersion)).thenReturn(flowIdentifier);
        when(databaseUpgradeRuntimeValidator.isRuntimeVersionAllowedForUpgrade(any())).thenReturn(true);

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
        TargetMajorVersion targetMajorVersion = TargetMajorVersion.VERSION_11;
        SdxCluster sdxCluster = getSdxCluster();
        when(sdxService.getByNameOrCrn(any(), eq(NAME_OR_CRN))).thenReturn(sdxCluster);
        SdxStatusEntity status = getDatalakeStatus(datalakeStatusEnum);
        when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(status);

        Assertions.assertThatCode(() -> underTest.upgrade(NAME_OR_CRN, targetMajorVersion))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(String.format("Data Lake %s is not available for database server upgrade", SDX_CLUSTER_NAME));

        verify(sdxDatabaseServerUpgradeAvailabilityService, never()).isUpgradeNeeded(any(), any());
        verify(reactorFlowManager, never()).triggerDatabaseServerUpgradeFlow(any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = DatalakeStatusEnum.class, names = {"DATALAKE_UPGRADE_DATABASE_SERVER_REQUESTED", "DATALAKE_UPGRADE_DATABASE_SERVER_IN_PROGRESS"})
    void testUpgradeWhenUpgradeInProgressThenUpgradeNotTriggered(DatalakeStatusEnum datalakeStatusEnum) {
        TargetMajorVersion targetMajorVersion = TargetMajorVersion.VERSION_11;
        SdxCluster sdxCluster = getSdxCluster();
        when(sdxService.getByNameOrCrn(any(), eq(NAME_OR_CRN))).thenReturn(sdxCluster);
        SdxStatusEntity status = getDatalakeStatus(datalakeStatusEnum);
        when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(status);

        Assertions.assertThatCode(() -> underTest.upgrade(NAME_OR_CRN, targetMajorVersion))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(String.format("Database server upgrade for Data Lake %s is already in progress", SDX_CLUSTER_NAME));

        verify(sdxDatabaseServerUpgradeAvailabilityService, never()).isUpgradeNeeded(any(), any());
        verify(reactorFlowManager, never()).triggerDatabaseServerUpgradeFlow(any(), any());
    }

    @Test
    void testUpgradeWhenDatabaseRuntimeTooLowThenNotTriggered() {
        TargetMajorVersion targetMajorVersion = TargetMajorVersion.VERSION_11;
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
    void testInitUpgradeInCb() {
        TargetMajorVersion targetMajorVersion = TargetMajorVersion.VERSION_11;
        SdxCluster sdxCluster = getSdxCluster();

        underTest.initUpgradeInCb(sdxCluster, targetMajorVersion);

        verify(cloudbreakStackService).upgradeRdsByClusterNameInternal(sdxCluster, targetMajorVersion);
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
        return sdxCluster;
    }

    private SdxStatusEntity getDatalakeStatus(DatalakeStatusEnum statusEnum) {
        SdxStatusEntity status = new SdxStatusEntity();
        status.setStatus(statusEnum);
        return status;
    }

}
