package com.sequenceiq.datalake.controller.sdx;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.doAs;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.service.upgrade.SdxRuntimeUpgradeService;
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeResponse;

@ExtendWith(MockitoExtension.class)
public class SdxUpgradeControllerTest {

    private static final String ACCOUNT_ID = "6f53f8a0-d5e8-45e6-ab11-cce9b53f7aad";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:"
            + ACCOUNT_ID + ":user:" + UUID.randomUUID();

    private static final String CLUSTER_NAME = "clusterName";

    @Mock
    private SdxRuntimeUpgradeService sdxRuntimeUpgradeService;

    @InjectMocks
    private SdxUpgradeController underTest;

    @Test
    public void testUpgradeClusterByNameWhenRequestIsEmptyAndRuntimeIsDisabled() {
        when(sdxRuntimeUpgradeService.isRuntimeUpgradeEnabled(USER_CRN)).thenReturn(false);

        SdxUpgradeRequest request = new SdxUpgradeRequest();
        doAs(USER_CRN, () -> underTest.upgradeClusterByName(CLUSTER_NAME, request));

        verify(sdxRuntimeUpgradeService, times(1)).triggerUpgradeByName(USER_CRN, CLUSTER_NAME, request,
                Crn.fromString(USER_CRN).getAccountId());
    }

    @Test
    public void testUpgradeClusterByNameWhenRequestIsEmptyAndRuntimeIsEnabled() {
        when(sdxRuntimeUpgradeService.isRuntimeUpgradeEnabled(USER_CRN)).thenReturn(true);

        SdxUpgradeRequest request = new SdxUpgradeRequest();
        doAs(USER_CRN, () -> underTest.upgradeClusterByName(CLUSTER_NAME, request));

        verify(sdxRuntimeUpgradeService).triggerUpgradeByName(USER_CRN, CLUSTER_NAME, request, Crn.fromString(USER_CRN).getAccountId());
    }

    @Test
    public void testUpgradeClusterByNameWhenRequestIsDryRunAndRuntimeIsDisabled() {
        when(sdxRuntimeUpgradeService.isRuntimeUpgradeEnabled(USER_CRN)).thenReturn(false);
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
    public void testUpgradeClusterByNameWhenRequestIsDryRunAndLockComponents() {
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
    public void testUpgradeClusterByNameWhenRequestIsDryRunAndRuntimeAndRuntimeIsDisabled() {
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        request.setDryRun(true);
        request.setRuntime("7.1.0");
        doThrow(new BadRequestException("Runtime upgrade feature is not enabled"))
                .when(sdxRuntimeUpgradeService).checkForUpgradeByName(eq(USER_CRN), eq(CLUSTER_NAME), eq(request), anyString());

        BadRequestException exception = doAs(USER_CRN, () -> Assertions.assertThrows(BadRequestException.class,
                () -> underTest.upgradeClusterByName(CLUSTER_NAME, request)));

        Assertions.assertEquals("Runtime upgrade feature is not enabled", exception.getMessage());
    }

    @Test
    public void testUpgradeClusterByNameWhenLockComponentsIsSet() {
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
    public void testUpgradeClusterByNameRuntimeIsSetAndDisabled() {
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        request.setRuntime("7.1.0");
        doThrow(new BadRequestException("Runtime upgrade feature is not enabled"))
                .when(sdxRuntimeUpgradeService).triggerUpgradeByName(USER_CRN, CLUSTER_NAME, request, Crn.fromString(USER_CRN).getAccountId());

        BadRequestException exception = doAs(USER_CRN, () -> Assertions.assertThrows(BadRequestException.class,
                () -> underTest.upgradeClusterByName(CLUSTER_NAME, request)));

        Assertions.assertEquals("Runtime upgrade feature is not enabled", exception.getMessage());
    }

    @Test
    public void testUpgradeClusterByNameWhenRuntimeIsSetAndEnabled() {
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
    public void testUpgradeClusterByNameWhenImageIsSetAndRuntimeIsDisabled() {
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        request.setImageId("imageId");
        doThrow(new BadRequestException("Runtime upgrade feature is not enabled"))
                .when(sdxRuntimeUpgradeService).triggerUpgradeByName(USER_CRN, CLUSTER_NAME, request, Crn.fromString(USER_CRN).getAccountId());

        BadRequestException exception = doAs(USER_CRN, () -> Assertions.assertThrows(BadRequestException.class,
                () -> underTest.upgradeClusterByName(CLUSTER_NAME, request)));

        Assertions.assertEquals("Runtime upgrade feature is not enabled", exception.getMessage());
    }

    @Test
    public void testUpgradeClusterByNameWhenImageIsSetAndRuntimeIsEnabled() {
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
}
