package com.sequenceiq.datalake.controller.sdx;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.doAs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.datalake.service.upgrade.SdxRuntimeUpgradeService;
import com.sequenceiq.datalake.service.upgrade.ccm.SdxCcmUpgradeService;
import com.sequenceiq.datalake.service.upgrade.database.SdxDatabaseServerUpgradeService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.sdx.api.model.CcmUpgradeResponseType;
import com.sequenceiq.sdx.api.model.SdxCcmUpgradeResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseUpgradeStatus;
import com.sequenceiq.sdx.api.model.SdxUpgradeDatabaseServerRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeDatabaseServerResponse;
import com.sequenceiq.sdx.api.model.SdxUpgradeReinitiableResponse;
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeResponse;
import com.sequenceiq.sdx.api.model.SdxUpgradeShowAvailableImages;

@ExtendWith(MockitoExtension.class)
class SdxUpgradeControllerTest {

    private static final String ACCOUNT_ID = "6f53f8a0-d5e8-45e6-ab11-cce9b53f7aad";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":user:" + UUID.randomUUID();

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:" + ACCOUNT_ID + ":environment:" + UUID.randomUUID();

    private static final String CLUSTER_NAME = "clusterName";

    private static final String CLUSTER_CRN = "clusterCrn";

    @Mock
    private SdxRuntimeUpgradeService sdxRuntimeUpgradeService;

    @Mock
    private SdxCcmUpgradeService sdxCcmUpgradeService;

    @Mock
    private SdxDatabaseServerUpgradeService sdxDatabaseServerUpgradeService;

    @InjectMocks
    private SdxUpgradeController underTest;

    @Captor
    private ArgumentCaptor<SdxUpgradeRequest> upgradeRequestArgumentCaptor;

    @Test
    void testUpgradeClusterByNameWhenRequestIsEmptyAndRuntimeIsDisabled() {
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        doAs(USER_CRN, () -> underTest.upgradeClusterByName(CLUSTER_NAME, request));

        verify(sdxRuntimeUpgradeService, times(1)).triggerUpgradeByName(USER_CRN, CLUSTER_NAME, request, false);
    }

    @Test
    void testUpgradeClusterByNameWhenRequestIsEmptyAndRuntimeIsEnabled() {
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        doAs(USER_CRN, () -> underTest.upgradeClusterByName(CLUSTER_NAME, request));

        verify(sdxRuntimeUpgradeService).triggerUpgradeByName(USER_CRN, CLUSTER_NAME, request, false);
    }

    @Test
    void testUpgradeClusterByNameWhenRequestIsDryRunAndRuntimeIsDisabled() {
        SdxUpgradeResponse sdxUpgradeResponse = new SdxUpgradeResponse();
        sdxUpgradeResponse.setReason("No image available to upgrade");

        SdxUpgradeRequest request = new SdxUpgradeRequest();
        request.setDryRun(true);

        when(sdxRuntimeUpgradeService.checkForUpgradeByName(USER_CRN, CLUSTER_NAME, request, false)).thenReturn(sdxUpgradeResponse);

        SdxUpgradeResponse response = doAs(USER_CRN, () -> underTest.upgradeClusterByName(CLUSTER_NAME, request));

        verify(sdxRuntimeUpgradeService, times(1)).checkForUpgradeByName(USER_CRN, CLUSTER_NAME, request, false);
        assertEquals("No image available to upgrade", response.getReason());
    }

    @Test
    void testUpgradeClusterByNameWhenRequestIsDryRunAndLockComponents() {
        SdxUpgradeResponse sdxUpgradeResponse = new SdxUpgradeResponse();
        sdxUpgradeResponse.setReason("No image available to upgrade");

        SdxUpgradeRequest request = new SdxUpgradeRequest();
        request.setDryRun(true);
        request.setLockComponents(true);

        when(sdxRuntimeUpgradeService.checkForUpgradeByName(USER_CRN, CLUSTER_NAME, request, false)).thenReturn(sdxUpgradeResponse);

        SdxUpgradeResponse response = doAs(USER_CRN, () -> underTest.upgradeClusterByName(CLUSTER_NAME, request));

        verify(sdxRuntimeUpgradeService, times(1)).checkForUpgradeByName(USER_CRN, CLUSTER_NAME, request, false);
        assertEquals("No image available to upgrade", response.getReason());
    }

    @Test
    void testUpgradeClusterByNameWhenRequestIsDryRunAndRuntimeAndRuntimeIsDisabled() {
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        request.setDryRun(true);
        request.setRuntime("7.1.0");
        doThrow(new BadRequestException("Runtime upgrade feature is not enabled"))
                .when(sdxRuntimeUpgradeService).checkForUpgradeByName(USER_CRN, CLUSTER_NAME, request, false);

        BadRequestException exception = doAs(USER_CRN, () -> assertThrows(BadRequestException.class,
                () -> underTest.upgradeClusterByName(CLUSTER_NAME, request)));

        assertEquals("Runtime upgrade feature is not enabled", exception.getMessage());
    }

    @Test
    void testUpgradeClusterByNameWhenLockComponentsIsSet() {
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        request.setLockComponents(true);
        SdxUpgradeResponse sdxUpgradeResponse = new SdxUpgradeResponse();
        sdxUpgradeResponse.setReason("No image available to upgrade");
        when(sdxRuntimeUpgradeService.triggerUpgradeByName(USER_CRN, CLUSTER_NAME, request, false))
                .thenReturn(sdxUpgradeResponse);

        SdxUpgradeResponse response = doAs(USER_CRN, () -> underTest.upgradeClusterByName(CLUSTER_NAME, request));

        verify(sdxRuntimeUpgradeService, times(0)).checkForUpgradeByName(USER_CRN, CLUSTER_NAME, request, false);
        verify(sdxRuntimeUpgradeService, times(1)).triggerUpgradeByName(USER_CRN, CLUSTER_NAME, request, false);
        assertEquals("No image available to upgrade", response.getReason());
    }

    @Test
    void testUpgradeClusterByNameRuntimeIsSetAndDisabled() {
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        request.setRuntime("7.1.0");
        doThrow(new BadRequestException("Runtime upgrade feature is not enabled"))
                .when(sdxRuntimeUpgradeService).triggerUpgradeByName(USER_CRN, CLUSTER_NAME, request, false);

        BadRequestException exception = doAs(USER_CRN, () -> assertThrows(BadRequestException.class,
                () -> underTest.upgradeClusterByName(CLUSTER_NAME, request)));

        assertEquals("Runtime upgrade feature is not enabled", exception.getMessage());
    }

    @Test
    void testUpgradeClusterByNameWhenRuntimeIsSetAndEnabled() {
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        request.setRuntime("7.1.0");
        SdxUpgradeResponse sdxUpgradeResponse = new SdxUpgradeResponse();
        sdxUpgradeResponse.setReason("No image available to upgrade");
        when(sdxRuntimeUpgradeService.triggerUpgradeByName(USER_CRN, CLUSTER_NAME, request, false))
                .thenReturn(sdxUpgradeResponse);

        SdxUpgradeResponse response = doAs(USER_CRN, () -> underTest.upgradeClusterByName(CLUSTER_NAME, request));

        verify(sdxRuntimeUpgradeService, times(0)).checkForUpgradeByName(USER_CRN, CLUSTER_NAME, request, false);
        verify(sdxRuntimeUpgradeService).triggerUpgradeByName(USER_CRN, CLUSTER_NAME, request, false);
        assertEquals("No image available to upgrade", response.getReason());
    }

    @Test
    void testUpgradeClusterByNameWhenImageIsSetAndRuntimeIsDisabled() {
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        request.setImageId("imageId");
        doThrow(new BadRequestException("Runtime upgrade feature is not enabled"))
                .when(sdxRuntimeUpgradeService).triggerUpgradeByName(USER_CRN, CLUSTER_NAME, request, false);

        BadRequestException exception = doAs(USER_CRN, () -> assertThrows(BadRequestException.class,
                () -> underTest.upgradeClusterByName(CLUSTER_NAME, request)));

        assertEquals("Runtime upgrade feature is not enabled", exception.getMessage());
    }

    @Test
    void testUpgradeClusterByNameWhenImageIsSetAndRuntimeIsEnabled() {
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        request.setImageId("imageId");
        SdxUpgradeResponse sdxUpgradeResponse = new SdxUpgradeResponse();
        sdxUpgradeResponse.setReason("No image available to upgrade");
        when(sdxRuntimeUpgradeService.triggerUpgradeByName(USER_CRN, CLUSTER_NAME, request, false))
                .thenReturn(sdxUpgradeResponse);

        SdxUpgradeResponse response = doAs(USER_CRN, () -> underTest.upgradeClusterByName(CLUSTER_NAME, request));

        verify(sdxRuntimeUpgradeService, times(0)).checkForUpgradeByName(USER_CRN, CLUSTER_NAME, request, false);
        verify(sdxRuntimeUpgradeService).triggerUpgradeByName(USER_CRN, CLUSTER_NAME, request, false);
        assertEquals("No image available to upgrade", response.getReason());
    }

    @Test
    @DisplayName("when show images is requested and runtime upgrade is disabled it should set the lock components flag")
    void testUpgradeClusterByNameWhenRequestIsShowImagesAndRuntimeIsDisabledShouldSetLockComponent() {
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        request.setShowAvailableImages(SdxUpgradeShowAvailableImages.SHOW);

        SdxUpgradeResponse sdxUpgradeResponse = new SdxUpgradeResponse();
        sdxUpgradeResponse.setReason("No image available to upgrade");
        when(sdxRuntimeUpgradeService.checkForUpgradeByName(USER_CRN, CLUSTER_NAME, request, false)).thenReturn(sdxUpgradeResponse);

        SdxUpgradeResponse response = doAs(USER_CRN, () -> underTest.upgradeClusterByName(CLUSTER_NAME, request));

        verify(sdxRuntimeUpgradeService, times(1)).checkForUpgradeByName(USER_CRN, CLUSTER_NAME, request, false);
        assertEquals("No image available to upgrade", response.getReason());
    }

    @Test
    @DisplayName("when show latest images is requested and runtime upgrade is disabled it should set the lock components flag")
    void testUpgradeClusterByNameWhenRequestIsShowLatestImagesAndRuntimeIsDisabledShouldSetLockComponent() {
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        request.setShowAvailableImages(SdxUpgradeShowAvailableImages.LATEST_ONLY);

        SdxUpgradeResponse sdxUpgradeResponse = new SdxUpgradeResponse();
        sdxUpgradeResponse.setReason("No image available to upgrade");
        when(sdxRuntimeUpgradeService.checkForUpgradeByName(USER_CRN, CLUSTER_NAME, request, false)).thenReturn(sdxUpgradeResponse);

        SdxUpgradeResponse response = doAs(USER_CRN, () -> underTest.upgradeClusterByName(CLUSTER_NAME, request));

        verify(sdxRuntimeUpgradeService, times(1)).checkForUpgradeByName(USER_CRN, CLUSTER_NAME, request, false);
        assertEquals("No image available to upgrade", response.getReason());
    }

    @Test
    @DisplayName("when show images is requested and runtime upgrade is enabled it should not set the lock components flag")
    void testUpgradeClusterByNameWhenRequestIsShowImagesAndRuntimeIsDisabledShouldNotSetLockComponent() {
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        request.setShowAvailableImages(SdxUpgradeShowAvailableImages.SHOW);

        SdxUpgradeResponse sdxUpgradeResponse = new SdxUpgradeResponse();
        sdxUpgradeResponse.setReason("No image available to upgrade");
        when(sdxRuntimeUpgradeService.checkForUpgradeByName(USER_CRN, CLUSTER_NAME, request, false)).thenReturn(sdxUpgradeResponse);

        SdxUpgradeResponse response = doAs(USER_CRN, () -> underTest.upgradeClusterByName(CLUSTER_NAME, request));

        verify(sdxRuntimeUpgradeService, times(1)).checkForUpgradeByName(eq(USER_CRN), eq(CLUSTER_NAME), upgradeRequestArgumentCaptor.capture(), eq(false));
        SdxUpgradeRequest capturedRequest = upgradeRequestArgumentCaptor.getValue();
        assertNull(capturedRequest.getLockComponents());
        assertEquals("No image available to upgrade", response.getReason());
    }

    @Test
    void testGetClusterUpgradeReinitiableByName() {
        SdxUpgradeReinitiableResponse sdxUpgradeReinitiableResponse = mock();
        when(sdxRuntimeUpgradeService.checkClusterUpgradeReinitiable(NameOrCrn.ofName(CLUSTER_NAME))).thenReturn(sdxUpgradeReinitiableResponse);

        SdxUpgradeReinitiableResponse result = underTest.getClusterUpgradeReinitiableByName(CLUSTER_NAME);

        assertEquals(sdxUpgradeReinitiableResponse, result);
    }

    @Test
    void testGetClusterUpgradeReinitiableByCrn() {
        SdxUpgradeReinitiableResponse sdxUpgradeReinitiableResponse = mock();
        when(sdxRuntimeUpgradeService.checkClusterUpgradeReinitiable(NameOrCrn.ofCrn(CLUSTER_CRN))).thenReturn(sdxUpgradeReinitiableResponse);

        SdxUpgradeReinitiableResponse result = underTest.getClusterUpgradeReinitiableByCrn(CLUSTER_CRN);

        assertEquals(sdxUpgradeReinitiableResponse, result);
    }

    @Test
    void testReinitiateClusterUpgradeByName() {
        SdxUpgradeResponse expected = new SdxUpgradeResponse();
        when(sdxRuntimeUpgradeService.reinitiateClusterUpgrade(NameOrCrn.ofName(CLUSTER_NAME))).thenReturn(expected);

        SdxUpgradeResponse result = underTest.reinitiateClusterUpgradeByName(CLUSTER_NAME);

        assertEquals(expected, result);
    }

    @Test
    void testReinitiateClusterUpgradeByCrn() {
        SdxUpgradeResponse expected = new SdxUpgradeResponse();
        when(sdxRuntimeUpgradeService.reinitiateClusterUpgrade(NameOrCrn.ofCrn(CLUSTER_CRN))).thenReturn(expected);

        SdxUpgradeResponse result = underTest.reinitiateClusterUpgradeByCrn(CLUSTER_CRN);

        assertEquals(expected, result);
    }

    @Test
    void testUpgradeCcm() {
        SdxCcmUpgradeResponse response = new SdxCcmUpgradeResponse(CcmUpgradeResponseType.TRIGGERED, new FlowIdentifier(FlowType.FLOW, "FlowId"),
                "OK", "crn");
        when(sdxCcmUpgradeService.upgradeCcm(ENV_CRN)).thenReturn(response);
        SdxCcmUpgradeResponse sdxCcmUpgradeResponse = underTest.upgradeCcm(ENV_CRN, USER_CRN);
        assertThat(sdxCcmUpgradeResponse).isEqualTo(response);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testUpgradeDatabaseServerByName(boolean forced) {
        SdxUpgradeDatabaseServerRequest request = new SdxUpgradeDatabaseServerRequest();
        request.setForced(forced);
        TargetMajorVersion targetMajorVersion = TargetMajorVersion.VERSION_11;
        request.setTargetMajorVersion(targetMajorVersion);
        SdxUpgradeDatabaseServerResponse sdxUpgradeDatabaseServerResponse = new SdxUpgradeDatabaseServerResponse();
        when(sdxDatabaseServerUpgradeService.upgrade(NameOrCrn.ofName(CLUSTER_NAME), targetMajorVersion, forced)).thenReturn(sdxUpgradeDatabaseServerResponse);

        SdxUpgradeDatabaseServerResponse response = underTest.upgradeDatabaseServerByName(CLUSTER_NAME, request);

        assertEquals(response, sdxUpgradeDatabaseServerResponse);
        verify(sdxDatabaseServerUpgradeService).upgrade(NameOrCrn.ofName(CLUSTER_NAME), targetMajorVersion, forced);
    }

    @Test
    void testUpgradeDatabaseServerByNameSdxClusterDoesNotExist() {
        SdxUpgradeDatabaseServerRequest request = new SdxUpgradeDatabaseServerRequest();
        TargetMajorVersion targetMajorVersion = TargetMajorVersion.VERSION_11;
        request.setTargetMajorVersion(targetMajorVersion);

        doThrow(new NotFoundException("SDX cluster 'testCluster' not found."))
                .when(sdxDatabaseServerUpgradeService).upgrade(eq(NameOrCrn.ofName(CLUSTER_NAME)), eq(targetMajorVersion), eq(false));

        NotFoundException exception = doAs(USER_CRN, () -> assertThrows(NotFoundException.class,
                () -> underTest.upgradeDatabaseServerByName(CLUSTER_NAME, request)));

        assertEquals(exception.getMessage(), "SDX cluster 'testCluster' not found.");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testUpgradeDatabaseServerByCrn(boolean forced) {
        SdxUpgradeDatabaseServerRequest request = new SdxUpgradeDatabaseServerRequest();
        request.setForced(forced);
        TargetMajorVersion targetMajorVersion = TargetMajorVersion.VERSION_11;
        request.setTargetMajorVersion(targetMajorVersion);
        SdxUpgradeDatabaseServerResponse sdxUpgradeDatabaseServerResponse = new SdxUpgradeDatabaseServerResponse();
        when(sdxDatabaseServerUpgradeService.upgrade(NameOrCrn.ofCrn(CLUSTER_CRN), targetMajorVersion, forced)).thenReturn(sdxUpgradeDatabaseServerResponse);

        SdxUpgradeDatabaseServerResponse response = underTest.upgradeDatabaseServerByCrn(CLUSTER_CRN, request);

        verify(sdxDatabaseServerUpgradeService).upgrade(NameOrCrn.ofCrn(CLUSTER_CRN), targetMajorVersion, forced);
        assertEquals(response, sdxUpgradeDatabaseServerResponse);
    }

    @Test
    public void testPrepareUpgradeByNameCheckForUpgrade() {
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        request.setDryRun(true);
        SdxUpgradeResponse response = new SdxUpgradeResponse();
        when(sdxRuntimeUpgradeService.checkForUpgradeByName(USER_CRN, CLUSTER_NAME, request, true)).thenReturn(response);

        SdxUpgradeResponse result = doAs(USER_CRN, () -> underTest.prepareClusterUpgradeByName(CLUSTER_NAME, request));

        assertEquals(response, result);
    }

    @Test
    public void testPrepareUpgradeByCrnCheckForUpgrade() {
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        request.setDryRun(true);
        SdxUpgradeResponse response = new SdxUpgradeResponse();
        when(sdxRuntimeUpgradeService.checkForUpgradeByCrn(USER_CRN, CLUSTER_NAME, request, true)).thenReturn(response);

        SdxUpgradeResponse result = doAs(USER_CRN, () -> underTest.prepareClusterUpgradeByCrn(CLUSTER_NAME, request));

        assertEquals(response, result);
    }

    @Test
    public void testPrepareUpgradeByNameTriggerUpgrade() {
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        SdxUpgradeResponse response = new SdxUpgradeResponse();
        when(sdxRuntimeUpgradeService.triggerUpgradeByName(USER_CRN, CLUSTER_NAME, request, true)).thenReturn(response);

        SdxUpgradeResponse result = doAs(USER_CRN, () -> underTest.prepareClusterUpgradeByName(CLUSTER_NAME, request));

        assertEquals(response, result);
    }

    @Test
    public void testPrepareUpgradeByCrnTriggerUpgrade() {
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        SdxUpgradeResponse response = new SdxUpgradeResponse();
        when(sdxRuntimeUpgradeService.triggerUpgradeByCrn(USER_CRN, CLUSTER_NAME, request, true)).thenReturn(response);

        SdxUpgradeResponse result = doAs(USER_CRN, () -> underTest.prepareClusterUpgradeByCrn(CLUSTER_NAME, request));

        assertEquals(response, result);
    }

    @Test
    void testGetDatabaseServerUpgradeStatusByDatalakeCrns() {
        List<String> datalakeCrns = List.of(CLUSTER_CRN, "crn:cdp:datalake:us-west-1:" + ACCOUNT_ID + ":datalake:" + UUID.randomUUID());
        List<SdxDatabaseUpgradeStatus> expected = List.of(
                SdxDatabaseUpgradeStatus.upgradeRequired(datalakeCrns.get(0), "14", "11"),
                SdxDatabaseUpgradeStatus.upgradeNotRequired(datalakeCrns.get(1), "14"));
        when(sdxDatabaseServerUpgradeService.getDatabaseServerUpgradeStatusByDatalakeCrns(USER_CRN, datalakeCrns)).thenReturn(expected);

        List<SdxDatabaseUpgradeStatus> result = doAs(USER_CRN, () -> underTest.getDatabaseServerUpgradeStatusByDatalakeCrns(datalakeCrns));

        verify(sdxDatabaseServerUpgradeService).getDatabaseServerUpgradeStatusByDatalakeCrns(USER_CRN, datalakeCrns);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void testGetDatabaseServerUpgradeStatusByDatalakeCrn() {
        SdxDatabaseUpgradeStatus expected = SdxDatabaseUpgradeStatus.upgradeRequired(CLUSTER_CRN, "14", "11");
        when(sdxDatabaseServerUpgradeService.getDatabaseServerUpgradeStatus(USER_CRN, NameOrCrn.ofCrn(CLUSTER_CRN))).thenReturn(expected);

        SdxDatabaseUpgradeStatus result = doAs(USER_CRN, () -> underTest.getDatabaseServerUpgradeStatusByDatalakeCrn(CLUSTER_CRN));

        verify(sdxDatabaseServerUpgradeService).getDatabaseServerUpgradeStatus(USER_CRN, NameOrCrn.ofCrn(CLUSTER_CRN));
        assertEquals(expected, result);
    }

    @Test
    void testGetDatabaseServerUpgradeStatusByDatalakeName() {
        SdxDatabaseUpgradeStatus expected = SdxDatabaseUpgradeStatus.upgradeNotRequired(CLUSTER_CRN, "11");
        when(sdxDatabaseServerUpgradeService.getDatabaseServerUpgradeStatus(USER_CRN, NameOrCrn.ofName(CLUSTER_NAME))).thenReturn(expected);

        SdxDatabaseUpgradeStatus result = doAs(USER_CRN, () -> underTest.getDatabaseServerUpgradeStatusByDatalakeName(CLUSTER_NAME));

        verify(sdxDatabaseServerUpgradeService).getDatabaseServerUpgradeStatus(USER_CRN, NameOrCrn.ofName(CLUSTER_NAME));
        assertEquals(expected, result);
    }

    @Test
    void testGetDatabaseServerUpgradeStatusByDatalakeCrnPropagatesFullContract() {
        SdxDatabaseUpgradeStatus expected = SdxDatabaseUpgradeStatus.upgradeRequired(CLUSTER_CRN, "14", "11");
        expected.setEolDate("2023-11-09");
        when(sdxDatabaseServerUpgradeService.getDatabaseServerUpgradeStatus(USER_CRN, NameOrCrn.ofCrn(CLUSTER_CRN))).thenReturn(expected);

        SdxDatabaseUpgradeStatus result = doAs(USER_CRN, () -> underTest.getDatabaseServerUpgradeStatusByDatalakeCrn(CLUSTER_CRN));

        assertThat(result.getDatalakeCrn()).isEqualTo(CLUSTER_CRN);
        assertThat(result.getUpgradeStatus()).isEqualTo("UPGRADE_REQUIRED");
        assertThat(result.getTargetMajorVersion()).isEqualTo("14");
        assertThat(result.getCurrentMajorVersion()).isEqualTo("11");
        assertThat(result.getEolDate()).isEqualTo("2023-11-09");
    }

    @Test
    void testGetDatabaseServerUpgradeStatusByDatalakeCrnPropagatesUnknownStatus() {
        SdxDatabaseUpgradeStatus expected = SdxDatabaseUpgradeStatus.unknown(CLUSTER_CRN);
        when(sdxDatabaseServerUpgradeService.getDatabaseServerUpgradeStatus(USER_CRN, NameOrCrn.ofCrn(CLUSTER_CRN))).thenReturn(expected);

        SdxDatabaseUpgradeStatus result = doAs(USER_CRN, () -> underTest.getDatabaseServerUpgradeStatusByDatalakeCrn(CLUSTER_CRN));

        assertThat(result.getUpgradeStatus()).isEqualTo("UNKNOWN");
    }
}
