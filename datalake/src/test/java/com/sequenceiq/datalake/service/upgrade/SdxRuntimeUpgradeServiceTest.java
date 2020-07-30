package com.sequenceiq.datalake.service.upgrade;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageComponentVersions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.controller.sdx.SdxUpgradeClusterConverter;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxUpgradeReplaceVms;
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeResponse;
import com.sequenceiq.sdx.api.model.SdxUpgradeShowAvailableImages;

@ExtendWith(MockitoExtension.class)
public class SdxRuntimeUpgradeServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String STACK_CRN = "crn:cdp:sdx:us-west-1:1234:sdxcluster:mystack";

    private static final String STACK_NAME = "mystack";

    private static final String IMAGE_ID = "image-id-first";

    private static final String IMAGE_ID_LAST = "image-id-last";

    private static final String ANOTHER_IMAGE_ID = "another-image-id";

    private static final String MATCHING_TARGET_RUNTIME = "7.0.2";

    private static final String ANOTHER_TARGET_RUNTIME = "7.2.0";

    private static final String V_7_0_3 = "7.0.3";

    private static final String V_7_0_2 = "7.0.2";

    private static final SdxUpgradeReplaceVms REPAIR_AFTER_UPGRADE = SdxUpgradeReplaceVms.ENABLED;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private SdxService sdxService;

    @Mock
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Mock
    private CloudbreakMessagesService messagesService;

    @Mock
    private SdxUpgradeClusterConverter sdxUpgradeClusterConverter;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private SdxRuntimeUpgradeService underTest;

    private UpgradeV4Response response;

    private SdxCluster sdxCluster;

    private SdxUpgradeRequest sdxUpgradeRequest;

    private SdxUpgradeResponse sdxUpgradeResponse;

    @BeforeEach
    public void setUp() {
        response = new UpgradeV4Response();
        sdxUpgradeResponse = new SdxUpgradeResponse();
        sdxCluster = getValidSdxCluster();
        sdxUpgradeRequest = getFullSdxUpgradeRequest();
    }

    @Test
    public void testNoImageFound() {
        when(sdxService.getByCrn(anyString(), anyString())).thenReturn(sdxCluster);
        when(stackV4Endpoint.checkForClusterUpgradeByName(anyLong(), anyString(), any())).thenReturn(response);
        when(sdxUpgradeClusterConverter.upgradeResponseToSdxUpgradeResponse(response)).thenReturn(sdxUpgradeResponse);
        when(entitlementService.runtimeUpgradeEnabled(any(), any())).thenReturn(true);

        response.setUpgradeCandidates(new ArrayList<>());
        sdxUpgradeResponse.setUpgradeCandidates(new ArrayList<>());
        BadRequestException exception = Assertions.assertThrows(
                BadRequestException.class,
                () -> underTest.triggerUpgradeByCrn(USER_CRN, STACK_CRN, sdxUpgradeRequest));

        assertEquals("There is no compatible image to upgrade for stack " + sdxCluster.getClusterName(), exception.getMessage());
    }

    @Test
    public void testInvalidImageIdShouldReturnNoCompatibleImageFound() {
        when(sdxService.getByCrn(anyString(), anyString())).thenReturn(sdxCluster);
        when(stackV4Endpoint.checkForClusterUpgradeByName(anyLong(), anyString(), any())).thenReturn(response);
        when(sdxUpgradeClusterConverter.upgradeResponseToSdxUpgradeResponse(response)).thenReturn(sdxUpgradeResponse);
        when(entitlementService.runtimeUpgradeEnabled(any(), any())).thenReturn(true);

        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(ANOTHER_IMAGE_ID);
        response.setUpgradeCandidates(List.of(imageInfo));
        sdxUpgradeResponse.setUpgradeCandidates(List.of(imageInfo));
        BadRequestException exception = Assertions.assertThrows(
                BadRequestException.class,
                () -> underTest.triggerUpgradeByCrn(USER_CRN, STACK_CRN, sdxUpgradeRequest));

        assertEquals(String.format("The given image (%s) is not eligible for upgrading the cluster. "
                + "Please choose an id from the following image(s): %s", IMAGE_ID, ANOTHER_IMAGE_ID), exception.getMessage());
    }

    @Test
    public void testOtherError() {
        when(sdxService.getByCrn(anyString(), anyString())).thenReturn(sdxCluster);
        when(stackV4Endpoint.checkForClusterUpgradeByName(anyLong(), anyString(), any())).thenReturn(response);
        when(sdxUpgradeClusterConverter.upgradeResponseToSdxUpgradeResponse(response)).thenReturn(sdxUpgradeResponse);
        when(entitlementService.runtimeUpgradeEnabled(any(), any())).thenReturn(true);

        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        response.setUpgradeCandidates(List.of(imageInfo));
        response.setReason("error reason");
        sdxUpgradeResponse.setUpgradeCandidates(List.of(imageInfo));
        sdxUpgradeResponse.setReason("error reason");

        BadRequestException exception = Assertions.assertThrows(
                BadRequestException.class,
                () -> underTest.triggerUpgradeByCrn(USER_CRN, STACK_CRN, sdxUpgradeRequest));

        assertEquals(String.format("The following error prevents the cluster upgrade process, please fix it and try again: %s",
                "error reason"), exception.getMessage());
    }

    @Test
    public void testNoCompatibleRuntimeFound() {
        when(sdxService.getByCrn(anyString(), anyString())).thenReturn(sdxCluster);
        when(stackV4Endpoint.checkForClusterUpgradeByName(anyLong(), anyString(), any())).thenReturn(response);
        when(sdxUpgradeClusterConverter.upgradeResponseToSdxUpgradeResponse(response)).thenReturn(sdxUpgradeResponse);
        when(entitlementService.runtimeUpgradeEnabled(any(), any())).thenReturn(true);

        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setComponentVersions(creatExpectedPackageVersions());
        response.setUpgradeCandidates(List.of(imageInfo));
        sdxUpgradeResponse.setUpgradeCandidates(List.of(imageInfo));

        sdxUpgradeRequest.setRuntime(ANOTHER_TARGET_RUNTIME);
        sdxUpgradeRequest.setLockComponents(false);
        sdxUpgradeRequest.setImageId(null);

        BadRequestException exception = Assertions.assertThrows(
                BadRequestException.class,
                () -> underTest.triggerUpgradeByCrn(USER_CRN, STACK_CRN, sdxUpgradeRequest));

        assertEquals(String.format("There is no image eligible for upgrading the cluster with runtime: %s. "
                + "Please choose a runtime from the following image(s): %s", ANOTHER_TARGET_RUNTIME, MATCHING_TARGET_RUNTIME), exception.getMessage());
    }

    @Test
    public void testCompatibleRuntimeFoundShouldReturnLatestImage() {
        when(sdxService.getByCrn(anyString(), anyString())).thenReturn(sdxCluster);
        when(stackV4Endpoint.checkForClusterUpgradeByName(anyLong(), anyString(), any())).thenReturn(response);
        when(sdxUpgradeClusterConverter.upgradeResponseToSdxUpgradeResponse(response)).thenReturn(sdxUpgradeResponse);
        when(entitlementService.runtimeUpgradeEnabled(any(), any())).thenReturn(true);

        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setCreated(1);
        imageInfo.setComponentVersions(creatExpectedPackageVersions());
        ImageInfoV4Response lastImageInfo = new ImageInfoV4Response();
        lastImageInfo.setImageId(IMAGE_ID_LAST);
        lastImageInfo.setCreated(2);
        lastImageInfo.setComponentVersions(creatExpectedPackageVersions());
        response.setUpgradeCandidates(List.of(imageInfo, lastImageInfo));
        sdxUpgradeResponse.setUpgradeCandidates(List.of(imageInfo, lastImageInfo));

        sdxUpgradeRequest.setLockComponents(false);
        sdxUpgradeRequest.setImageId(null);
        sdxUpgradeRequest.setReplaceVms(REPAIR_AFTER_UPGRADE);

        underTest.triggerUpgradeByCrn(USER_CRN, STACK_CRN, sdxUpgradeRequest);

        verify(sdxReactorFlowManager, times(1)).triggerDatalakeRuntimeUpgradeFlow(sdxCluster, IMAGE_ID_LAST, REPAIR_AFTER_UPGRADE);
        verify(sdxReactorFlowManager, times(0)).triggerDatalakeRuntimeUpgradeFlow(sdxCluster, IMAGE_ID, REPAIR_AFTER_UPGRADE);
    }

    @Test
    public void testNoUpgradeRequestPresentShouldReturnLatestImage() {
        when(sdxService.getByCrn(anyString(), anyString())).thenReturn(sdxCluster);
        when(stackV4Endpoint.checkForClusterUpgradeByName(anyLong(), anyString(), any())).thenReturn(response);
        when(sdxUpgradeClusterConverter.upgradeResponseToSdxUpgradeResponse(response)).thenReturn(sdxUpgradeResponse);
        when(entitlementService.runtimeUpgradeEnabled(any(), any())).thenReturn(true);

        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setCreated(1);
        imageInfo.setComponentVersions(creatExpectedPackageVersions());
        ImageInfoV4Response lastImageInfo = new ImageInfoV4Response();
        lastImageInfo.setImageId(IMAGE_ID_LAST);
        lastImageInfo.setCreated(2);
        lastImageInfo.setComponentVersions(creatExpectedPackageVersions());
        response.setUpgradeCandidates(List.of(imageInfo, lastImageInfo));
        sdxUpgradeResponse.setUpgradeCandidates(List.of(imageInfo, lastImageInfo));

        underTest.triggerUpgradeByCrn(USER_CRN, STACK_CRN, null);

        verify(sdxReactorFlowManager, times(1)).triggerDatalakeRuntimeUpgradeFlow(sdxCluster, IMAGE_ID_LAST, SdxUpgradeReplaceVms.DISABLED);
        verify(sdxReactorFlowManager, times(0)).triggerDatalakeRuntimeUpgradeFlow(sdxCluster, IMAGE_ID_LAST, SdxUpgradeReplaceVms.ENABLED);
        verify(sdxReactorFlowManager, times(0)).triggerDatalakeRuntimeUpgradeFlow(sdxCluster, IMAGE_ID, SdxUpgradeReplaceVms.ENABLED);
    }

    @Test
    public void testEmptySdxUpgradeRequest() {
        when(sdxService.getByCrn(anyString(), anyString())).thenReturn(sdxCluster);
        when(stackV4Endpoint.checkForClusterUpgradeByName(anyLong(), anyString(), any())).thenReturn(response);
        when(sdxUpgradeClusterConverter.upgradeResponseToSdxUpgradeResponse(response)).thenReturn(sdxUpgradeResponse);
        when(entitlementService.runtimeUpgradeEnabled(any(), any())).thenReturn(true);

        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setCreated(1);
        imageInfo.setComponentVersions(creatExpectedPackageVersions());
        ImageInfoV4Response lastImageInfo = new ImageInfoV4Response();
        lastImageInfo.setImageId(IMAGE_ID_LAST);
        lastImageInfo.setCreated(2);
        lastImageInfo.setComponentVersions(creatExpectedPackageVersions());
        response.setUpgradeCandidates(List.of(imageInfo, lastImageInfo));
        sdxUpgradeResponse.setUpgradeCandidates(List.of(imageInfo, lastImageInfo));

        sdxUpgradeRequest.setRuntime(null);
        sdxUpgradeRequest.setImageId(null);

        underTest.triggerUpgradeByCrn(USER_CRN, STACK_CRN, null);

        verify(sdxReactorFlowManager, times(1)).triggerDatalakeRuntimeUpgradeFlow(sdxCluster, IMAGE_ID_LAST, SdxUpgradeReplaceVms.DISABLED);
        verify(sdxReactorFlowManager, times(0)).triggerDatalakeRuntimeUpgradeFlow(sdxCluster, IMAGE_ID_LAST, SdxUpgradeReplaceVms.ENABLED);
        verify(sdxReactorFlowManager, times(0)).triggerDatalakeRuntimeUpgradeFlow(sdxCluster, IMAGE_ID, SdxUpgradeReplaceVms.ENABLED);
    }

    @Test
    public void testNoError() {
        when(sdxService.getByCrn(anyString(), anyString())).thenReturn(sdxCluster);
        when(stackV4Endpoint.checkForClusterUpgradeByName(anyLong(), anyString(), any())).thenReturn(response);
        when(sdxUpgradeClusterConverter.upgradeResponseToSdxUpgradeResponse(response)).thenReturn(sdxUpgradeResponse);
        when(entitlementService.runtimeUpgradeEnabled(any(), any())).thenReturn(true);

        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        response.setUpgradeCandidates(List.of(imageInfo));
        sdxUpgradeResponse.setUpgradeCandidates(List.of(imageInfo));

        assertDoesNotThrow(() -> underTest.triggerUpgradeByCrn(USER_CRN, STACK_CRN, sdxUpgradeRequest));
    }

    @Test
    public void testDryRunShouldReturnOneUpgradeCandidate() {
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setCreated(1);
        imageInfo.setComponentVersions(creatExpectedPackageVersions());
        ImageInfoV4Response lastImageInfo = new ImageInfoV4Response();
        lastImageInfo.setImageId(IMAGE_ID_LAST);
        lastImageInfo.setCreated(2);
        lastImageInfo.setComponentVersions(creatExpectedPackageVersions());
        UpgradeV4Response upgradeV4Response = new UpgradeV4Response();
        upgradeV4Response.setUpgradeCandidates(List.of(imageInfo, lastImageInfo));
        sdxUpgradeRequest.setDryRun(true);

        underTest.filterSdxUpgradeResponse(sdxUpgradeRequest, upgradeV4Response);

        assertEquals(1, upgradeV4Response.getUpgradeCandidates().size());
        assertEquals(IMAGE_ID_LAST, upgradeV4Response.getUpgradeCandidates().get(0).getImageId());
    }

    @Test
    public void testShowLatestOnlyShouldReturnLatestUpgradeCandidatesPerRuntime() {

        ImageComponentVersions imageComponentVersionsFor702 = new ImageComponentVersions();
        imageComponentVersionsFor702.setCm(V_7_0_2);
        imageComponentVersionsFor702.setCdp(V_7_0_2);

        ImageComponentVersions imageComponentVersionsFor703 = new ImageComponentVersions();
        imageComponentVersionsFor703.setCm(V_7_0_3);
        imageComponentVersionsFor703.setCdp(V_7_0_3);

        ImageInfoV4Response imageInfo1 = new ImageInfoV4Response();
        imageInfo1.setImageId(IMAGE_ID + 1);
        imageInfo1.setCreated(1);
        imageInfo1.setComponentVersions(imageComponentVersionsFor702);

        ImageInfoV4Response imageInfo2 = new ImageInfoV4Response();
        imageInfo2.setImageId(IMAGE_ID + 2);
        imageInfo2.setCreated(2);
        imageInfo2.setComponentVersions(imageComponentVersionsFor702);

        ImageInfoV4Response imageInfo3 = new ImageInfoV4Response();
        imageInfo3.setImageId(IMAGE_ID + 3);
        imageInfo3.setCreated(3);
        imageInfo3.setComponentVersions(imageComponentVersionsFor703);

        UpgradeV4Response upgradeV4Response = new UpgradeV4Response();
        upgradeV4Response.setUpgradeCandidates(List.of(imageInfo1, imageInfo2, imageInfo3));
        sdxUpgradeRequest.setShowAvailableImages(SdxUpgradeShowAvailableImages.LATEST_ONLY);

        underTest.filterSdxUpgradeResponse(sdxUpgradeRequest, upgradeV4Response);

        assertEquals(2, upgradeV4Response.getUpgradeCandidates().size());
        assertFalse(upgradeV4Response.getUpgradeCandidates().stream().anyMatch(imageInfoV4Response -> imageInfoV4Response.getImageId().equals(IMAGE_ID + 1)));
        assertTrue(upgradeV4Response.getUpgradeCandidates().stream().anyMatch(imageInfoV4Response -> imageInfoV4Response.getImageId().equals(IMAGE_ID + 2)));
        assertTrue(upgradeV4Response.getUpgradeCandidates().stream().anyMatch(imageInfoV4Response -> imageInfoV4Response.getImageId().equals(IMAGE_ID + 3)));
    }

    @Test
    public void testShowAvailableImagesShouldReturnAllUpgradeCandidates() {

        ImageComponentVersions imageComponentVersionsFor702 = new ImageComponentVersions();
        imageComponentVersionsFor702.setCm(V_7_0_2);
        imageComponentVersionsFor702.setCdp(V_7_0_2);

        ImageComponentVersions imageComponentVersionsFor703 = new ImageComponentVersions();
        imageComponentVersionsFor703.setCm(V_7_0_3);
        imageComponentVersionsFor703.setCdp(V_7_0_3);

        ImageInfoV4Response imageInfo1 = new ImageInfoV4Response();
        imageInfo1.setImageId(IMAGE_ID + 1);
        imageInfo1.setCreated(1);
        imageInfo1.setComponentVersions(imageComponentVersionsFor702);

        ImageInfoV4Response imageInfo2 = new ImageInfoV4Response();
        imageInfo2.setImageId(IMAGE_ID + 2);
        imageInfo2.setCreated(2);
        imageInfo2.setComponentVersions(imageComponentVersionsFor702);

        ImageInfoV4Response imageInfo3 = new ImageInfoV4Response();
        imageInfo3.setImageId(IMAGE_ID + 3);
        imageInfo3.setCreated(3);
        imageInfo3.setComponentVersions(imageComponentVersionsFor703);

        UpgradeV4Response upgradeV4Response = new UpgradeV4Response();
        upgradeV4Response.setUpgradeCandidates(List.of(imageInfo1, imageInfo2, imageInfo3));
        sdxUpgradeRequest.setShowAvailableImages(SdxUpgradeShowAvailableImages.SHOW);

        underTest.filterSdxUpgradeResponse(sdxUpgradeRequest, upgradeV4Response);

        assertEquals(3, upgradeV4Response.getUpgradeCandidates().size());
        assertTrue(upgradeV4Response.getUpgradeCandidates().stream().anyMatch(imageInfoV4Response -> imageInfoV4Response.getImageId().equals(IMAGE_ID + 1)));
        assertTrue(upgradeV4Response.getUpgradeCandidates().stream().anyMatch(imageInfoV4Response -> imageInfoV4Response.getImageId().equals(IMAGE_ID + 2)));
        assertTrue(upgradeV4Response.getUpgradeCandidates().stream().anyMatch(imageInfoV4Response -> imageInfoV4Response.getImageId().equals(IMAGE_ID + 3)));
    }

    @Test
    public void testCheckForRuntimeUpgradeByNameWhenNotEnabled() {
        when(entitlementService.runtimeUpgradeEnabled(any(), any())).thenReturn(false);

        BadRequestException exception = Assertions.assertThrows(BadRequestException.class,
                () -> underTest.checkForUpgradeByName(USER_CRN, STACK_NAME, sdxUpgradeRequest));

        assertEquals("Runtime upgrade feature is not enabled", exception.getMessage());
    }

    @Test
    public void testCheckForRuntimeUpgradeByCrnWhenNotEnabled() {
        when(sdxService.getByCrn(anyString(), anyString())).thenReturn(sdxCluster);
        when(entitlementService.runtimeUpgradeEnabled(any(), any())).thenReturn(false);

        BadRequestException exception = Assertions.assertThrows(BadRequestException.class,
                () -> underTest.checkForUpgradeByCrn(USER_CRN, STACK_CRN, sdxUpgradeRequest));

        assertEquals("Runtime upgrade feature is not enabled", exception.getMessage());
    }

    @Test
    public void testTriggerRuntimeUpgradeByCrnWhenNotEnabled() {
        when(sdxService.getByCrn(anyString(), anyString())).thenReturn(sdxCluster);
        when(entitlementService.runtimeUpgradeEnabled(any(), any())).thenReturn(false);

        BadRequestException exception = Assertions.assertThrows(
                BadRequestException.class,
                () -> underTest.triggerUpgradeByCrn(USER_CRN, STACK_CRN, sdxUpgradeRequest));

        assertEquals("Runtime upgrade feature is not enabled", exception.getMessage());
    }

    @Test
    public void testTriggerRuntimeUpgradeByNameWhenNotEnabled() {
        when(sdxService.getByNameInAccount(anyString(), anyString())).thenReturn(sdxCluster);
        when(entitlementService.runtimeUpgradeEnabled(any(), any())).thenReturn(false);

        BadRequestException exception = Assertions.assertThrows(
                BadRequestException.class,
                () -> underTest.triggerUpgradeByName(USER_CRN, STACK_NAME, sdxUpgradeRequest));

        assertEquals("Runtime upgrade feature is not enabled", exception.getMessage());
    }

    private SdxCluster getValidSdxCluster() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("test-sdx-cluster");
        sdxCluster.setClusterShape(SdxClusterShape.MEDIUM_DUTY_HA);
        sdxCluster.setEnvName("test-env");
        sdxCluster.setCrn("crn:sdxcluster");
        sdxCluster.setId(1L);
        return sdxCluster;
    }

    private SdxUpgradeRequest getFullSdxUpgradeRequest() {
        SdxUpgradeRequest sdxUpgradeRequest = new SdxUpgradeRequest();
        sdxUpgradeRequest.setImageId(IMAGE_ID);
        sdxUpgradeRequest.setRuntime(MATCHING_TARGET_RUNTIME);
        return sdxUpgradeRequest;
    }

    private ImageComponentVersions creatExpectedPackageVersions() {
        ImageComponentVersions imageComponentVersions = new ImageComponentVersions();
        imageComponentVersions.setCm(V_7_0_3);
        imageComponentVersions.setCdp(V_7_0_2);
        return imageComponentVersions;
    }
}