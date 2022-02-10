package com.sequenceiq.datalake.controller.sdx;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.doAs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.datalake.service.upgrade.SdxRuntimeUpgradeService;
import com.sequenceiq.datalake.service.upgrade.ccm.SdxCcmUpgradeService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.sdx.api.model.CcmUpgradeResponseType;
import com.sequenceiq.sdx.api.model.SdxCcmUpgradeResponse;
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeResponse;
import com.sequenceiq.sdx.api.model.SdxUpgradeShowAvailableImages;

@ExtendWith(MockitoExtension.class)
class SdxUpgradeControllerTest {

    private static final String ACCOUNT_ID = "6f53f8a0-d5e8-45e6-ab11-cce9b53f7aad";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":user:" + UUID.randomUUID();

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:" + ACCOUNT_ID + ":environment:" + UUID.randomUUID();

    private static final String CLUSTER_NAME = "clusterName";

    @Mock
    private SdxRuntimeUpgradeService sdxRuntimeUpgradeService;

    @Mock
    private SdxCcmUpgradeService sdxCcmUpgradeService;

    @InjectMocks
    private SdxUpgradeController underTest;

    @Captor
    private ArgumentCaptor<SdxUpgradeRequest> upgradeRequestArgumentCaptor;

    @Test
    void testUpgradeClusterByNameWhenRequestIsEmptyAndRuntimeIsDisabled() {
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        doAs(USER_CRN, () -> underTest.upgradeClusterByName(CLUSTER_NAME, request));

        verify(sdxRuntimeUpgradeService, times(1)).triggerUpgradeByName(USER_CRN, CLUSTER_NAME, request,
                Crn.fromString(USER_CRN).getAccountId());
    }

    @Test
    void testUpgradeClusterByNameWhenRequestIsEmptyAndRuntimeIsEnabled() {
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        doAs(USER_CRN, () -> underTest.upgradeClusterByName(CLUSTER_NAME, request));

        verify(sdxRuntimeUpgradeService).triggerUpgradeByName(USER_CRN, CLUSTER_NAME, request, Crn.fromString(USER_CRN).getAccountId());
    }

    @Test
    void testUpgradeClusterByNameWhenRequestIsDryRunAndRuntimeIsDisabled() {
        SdxUpgradeResponse sdxUpgradeResponse = new SdxUpgradeResponse();
        sdxUpgradeResponse.setReason("No image available to upgrade");

        SdxUpgradeRequest request = new SdxUpgradeRequest();
        request.setDryRun(true);

        when(sdxRuntimeUpgradeService.checkForUpgradeByName(eq(USER_CRN), eq(CLUSTER_NAME), eq(request), anyString())).thenReturn(sdxUpgradeResponse);

        SdxUpgradeResponse response = doAs(USER_CRN, () -> underTest.upgradeClusterByName(CLUSTER_NAME, request));

        verify(sdxRuntimeUpgradeService, times(1)).checkForUpgradeByName(eq(USER_CRN), eq(CLUSTER_NAME), eq(request), anyString());
        assertEquals("No image available to upgrade", response.getReason());
    }

    @Test
    void testUpgradeClusterByNameWhenRequestIsDryRunAndLockComponents() {
        SdxUpgradeResponse sdxUpgradeResponse = new SdxUpgradeResponse();
        sdxUpgradeResponse.setReason("No image available to upgrade");

        SdxUpgradeRequest request = new SdxUpgradeRequest();
        request.setDryRun(true);
        request.setLockComponents(true);

        when(sdxRuntimeUpgradeService.checkForUpgradeByName(eq(USER_CRN), eq(CLUSTER_NAME), eq(request), anyString())).thenReturn(sdxUpgradeResponse);

        SdxUpgradeResponse response = doAs(USER_CRN, () -> underTest.upgradeClusterByName(CLUSTER_NAME, request));

        verify(sdxRuntimeUpgradeService, times(1)).checkForUpgradeByName(eq(USER_CRN), eq(CLUSTER_NAME), eq(request), anyString());
        assertEquals("No image available to upgrade", response.getReason());
    }

    @Test
    void testUpgradeClusterByNameWhenRequestIsDryRunAndRuntimeAndRuntimeIsDisabled() {
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        request.setDryRun(true);
        request.setRuntime("7.1.0");
        doThrow(new BadRequestException("Runtime upgrade feature is not enabled"))
                .when(sdxRuntimeUpgradeService).checkForUpgradeByName(eq(USER_CRN), eq(CLUSTER_NAME), eq(request), anyString());

        BadRequestException exception = doAs(USER_CRN, () -> Assertions.assertThrows(BadRequestException.class,
                () -> underTest.upgradeClusterByName(CLUSTER_NAME, request)));

        assertEquals("Runtime upgrade feature is not enabled", exception.getMessage());
    }

    @Test
    void testUpgradeClusterByNameWhenLockComponentsIsSet() {
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        request.setLockComponents(true);
        SdxUpgradeResponse sdxUpgradeResponse = new SdxUpgradeResponse();
        sdxUpgradeResponse.setReason("No image available to upgrade");
        when(sdxRuntimeUpgradeService.triggerUpgradeByName(USER_CRN, CLUSTER_NAME, request, Crn.fromString(USER_CRN).getAccountId()))
                .thenReturn(sdxUpgradeResponse);

        SdxUpgradeResponse response = doAs(USER_CRN, () -> underTest.upgradeClusterByName(CLUSTER_NAME, request));

        verify(sdxRuntimeUpgradeService, times(0)).checkForUpgradeByName(any(), any(), any(), anyString());
        verify(sdxRuntimeUpgradeService, times(1)).triggerUpgradeByName(any(), any(), any(), anyString());
        assertEquals("No image available to upgrade", response.getReason());
    }

    @Test
    void testUpgradeClusterByNameRuntimeIsSetAndDisabled() {
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        request.setRuntime("7.1.0");
        doThrow(new BadRequestException("Runtime upgrade feature is not enabled"))
                .when(sdxRuntimeUpgradeService).triggerUpgradeByName(USER_CRN, CLUSTER_NAME, request, Crn.fromString(USER_CRN).getAccountId());

        BadRequestException exception = doAs(USER_CRN, () -> Assertions.assertThrows(BadRequestException.class,
                () -> underTest.upgradeClusterByName(CLUSTER_NAME, request)));

        assertEquals("Runtime upgrade feature is not enabled", exception.getMessage());
    }

    @Test
    void testUpgradeClusterByNameWhenRuntimeIsSetAndEnabled() {
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        request.setRuntime("7.1.0");
        SdxUpgradeResponse sdxUpgradeResponse = new SdxUpgradeResponse();
        sdxUpgradeResponse.setReason("No image available to upgrade");
        when(sdxRuntimeUpgradeService.triggerUpgradeByName(USER_CRN, CLUSTER_NAME, request, Crn.fromString(USER_CRN).getAccountId()))
                .thenReturn(sdxUpgradeResponse);

        SdxUpgradeResponse response = doAs(USER_CRN, () -> underTest.upgradeClusterByName(CLUSTER_NAME, request));

        verify(sdxRuntimeUpgradeService, times(0)).checkForUpgradeByName(any(), any(), any(), anyString());
        verify(sdxRuntimeUpgradeService).triggerUpgradeByName(USER_CRN, CLUSTER_NAME, request, Crn.fromString(USER_CRN).getAccountId());
        assertEquals("No image available to upgrade", response.getReason());
    }

    @Test
    void testUpgradeClusterByNameWhenImageIsSetAndRuntimeIsDisabled() {
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        request.setImageId("imageId");
        doThrow(new BadRequestException("Runtime upgrade feature is not enabled"))
                .when(sdxRuntimeUpgradeService).triggerUpgradeByName(USER_CRN, CLUSTER_NAME, request, Crn.fromString(USER_CRN).getAccountId());

        BadRequestException exception = doAs(USER_CRN, () -> Assertions.assertThrows(BadRequestException.class,
                () -> underTest.upgradeClusterByName(CLUSTER_NAME, request)));

        assertEquals("Runtime upgrade feature is not enabled", exception.getMessage());
    }

    @Test
    void testUpgradeClusterByNameWhenImageIsSetAndRuntimeIsEnabled() {
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        request.setImageId("imageId");
        SdxUpgradeResponse sdxUpgradeResponse = new SdxUpgradeResponse();
        sdxUpgradeResponse.setReason("No image available to upgrade");
        when(sdxRuntimeUpgradeService.triggerUpgradeByName(USER_CRN, CLUSTER_NAME, request, Crn.fromString(USER_CRN).getAccountId()))
                .thenReturn(sdxUpgradeResponse);

        SdxUpgradeResponse response = doAs(USER_CRN, () -> underTest.upgradeClusterByName(CLUSTER_NAME, request));

        verify(sdxRuntimeUpgradeService, times(0)).checkForUpgradeByName(any(), any(), any(), anyString());
        verify(sdxRuntimeUpgradeService).triggerUpgradeByName(USER_CRN, CLUSTER_NAME, request, Crn.fromString(USER_CRN).getAccountId());
        assertEquals("No image available to upgrade", response.getReason());
    }

    @Test
    @DisplayName("when show images is requested and runtime upgrade is disabled it should set the lock components flag")
    void testUpgradeClusterByNameWhenRequestIsShowImagesAndRuntimeIsDisabledShouldSetLockComponent() {
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        request.setShowAvailableImages(SdxUpgradeShowAvailableImages.SHOW);

        SdxUpgradeResponse sdxUpgradeResponse = new SdxUpgradeResponse();
        sdxUpgradeResponse.setReason("No image available to upgrade");
        when(sdxRuntimeUpgradeService.checkForUpgradeByName(USER_CRN, CLUSTER_NAME, request, ACCOUNT_ID)).thenReturn(sdxUpgradeResponse);

        SdxUpgradeResponse response = doAs(USER_CRN, () -> underTest.upgradeClusterByName(CLUSTER_NAME, request));

        verify(sdxRuntimeUpgradeService, times(1))
                .checkForUpgradeByName(any(), any(), upgradeRequestArgumentCaptor.capture(), anyString());
        //SdxUpgradeRequest capturedRequest = upgradeRequestArgumentCaptor.getValue();
        //assertTrue(capturedRequest.getLockComponents());
        assertEquals("No image available to upgrade", response.getReason());
    }

    @Test
    @DisplayName("when show latest images is requested and runtime upgrade is disabled it should set the lock components flag")
    void testUpgradeClusterByNameWhenRequestIsShowLatestImagesAndRuntimeIsDisabledShouldSetLockComponent() {
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        request.setShowAvailableImages(SdxUpgradeShowAvailableImages.LATEST_ONLY);

        SdxUpgradeResponse sdxUpgradeResponse = new SdxUpgradeResponse();
        sdxUpgradeResponse.setReason("No image available to upgrade");
        when(sdxRuntimeUpgradeService.checkForUpgradeByName(USER_CRN, CLUSTER_NAME, request, ACCOUNT_ID)).thenReturn(sdxUpgradeResponse);

        SdxUpgradeResponse response = doAs(USER_CRN, () -> underTest.upgradeClusterByName(CLUSTER_NAME, request));

        verify(sdxRuntimeUpgradeService, times(1))
                .checkForUpgradeByName(any(), any(), upgradeRequestArgumentCaptor.capture(), anyString());
        //SdxUpgradeRequest capturedRequest = upgradeRequestArgumentCaptor.getValue();
        //assertTrue(capturedRequest.getLockComponents());
        assertEquals("No image available to upgrade", response.getReason());
    }

    @Test
    @DisplayName("when show images is requested and runtime upgrade is enabled it should not set the lock components flag")
    void testUpgradeClusterByNameWhenRequestIsShowImagesAndRuntimeIsDisabledShouldNotSetLockComponent() {
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        request.setShowAvailableImages(SdxUpgradeShowAvailableImages.SHOW);

        SdxUpgradeResponse sdxUpgradeResponse = new SdxUpgradeResponse();
        sdxUpgradeResponse.setReason("No image available to upgrade");
        when(sdxRuntimeUpgradeService.checkForUpgradeByName(USER_CRN, CLUSTER_NAME, request, ACCOUNT_ID)).thenReturn(sdxUpgradeResponse);

        SdxUpgradeResponse response = doAs(USER_CRN, () -> underTest.upgradeClusterByName(CLUSTER_NAME, request));

        verify(sdxRuntimeUpgradeService, times(1))
                .checkForUpgradeByName(any(), any(), upgradeRequestArgumentCaptor.capture(), anyString());
        SdxUpgradeRequest capturedRequest = upgradeRequestArgumentCaptor.getValue();
        assertNull(capturedRequest.getLockComponents());
        assertEquals("No image available to upgrade", response.getReason());
    }

    @Test
    void testUpgradeCcm() {
        SdxCcmUpgradeResponse response = new SdxCcmUpgradeResponse(CcmUpgradeResponseType.TRIGGERED, new FlowIdentifier(FlowType.FLOW, "FlowId"), "OK");
        when(sdxCcmUpgradeService.upgradeCcm(ENV_CRN)).thenReturn(response);
        SdxCcmUpgradeResponse sdxCcmUpgradeResponse = underTest.upgradeCcm(ENV_CRN, USER_CRN);
        assertThat(sdxCcmUpgradeResponse).isEqualTo(response);
    }
}
