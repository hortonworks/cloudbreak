package com.sequenceiq.datalake.service.upgrade;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.doAs;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageComponentVersions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.client.RestClientFactory;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.datalakedr.DatalakeDrSkipOptions;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.util.TestConstants;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.datalake.controller.sdx.SdxUpgradeClusterConverter;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.validation.upgrade.SdxUpgradeValidator;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxUpgradeReplaceVms;
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeResponse;

@ExtendWith(MockitoExtension.class)
public class SdxRuntimeUpgradeServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String ACCOUNT_ID = Crn.fromString(USER_CRN).getAccountId();

    private static final String CLUSTER_CRN = "crn:cdp:sdx:us-west-1:1234:sdxcluster:mystack";

    private static final String STACK_NAME = "mystack";

    private static final String IMAGE_ID = "image-id-first";

    private static final String IMAGE_ID_LAST = "image-id-last";

    private static final String ANOTHER_IMAGE_ID = "another-image-id";

    private static final String MATCHING_TARGET_RUNTIME = "7.0.2";

    private static final String ANOTHER_TARGET_RUNTIME = "7.2.0";

    private static final String V_7_0_3 = "7.0.3";

    private static final String V_7_0_2 = "7.0.2";

    private static final SdxUpgradeReplaceVms REPAIR_AFTER_UPGRADE = SdxUpgradeReplaceVms.ENABLED;

    private static final boolean SKIP_BACKUP = false;

    private static final boolean ROLLING_UPGRADE_ENABLED = true;

    private static final DatalakeDrSkipOptions SKIP_OPTIONS =
            new DatalakeDrSkipOptions(false, false, false, false);

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

    @Mock
    private RestClientFactory restClientFactory;

    @Mock
    private SdxUpgradeValidator sdxUpgradeValidator;

    private SdxUpgradeFilter sdxUpgradeFilter;

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
        sdxUpgradeResponse.setCurrent(createCurrentImage());
        sdxCluster = getValidEnterpriseCluster();
        sdxUpgradeRequest = getFullSdxUpgradeRequest();
        sdxUpgradeFilter = new SdxUpgradeFilter();
        ReflectionTestUtils.setField(sdxUpgradeFilter, "entitlementService", entitlementService, EntitlementService.class);
        ReflectionTestUtils.setField(underTest, "upgradeFilter", sdxUpgradeFilter, SdxUpgradeFilter.class);
    }

    @Test
    public void testNoImageFound() {
        when(sdxService.getByCrn(USER_CRN, CLUSTER_CRN)).thenReturn(sdxCluster);
        ArgumentCaptor<UpgradeV4Request> upgradeV4RequestCaptor = ArgumentCaptor.forClass(UpgradeV4Request.class);
        when(stackV4Endpoint.checkForClusterUpgradeByName(eq(0L), eq(STACK_NAME), upgradeV4RequestCaptor.capture(), eq(ACCOUNT_ID))).thenReturn(response);
        when(sdxUpgradeClusterConverter.upgradeResponseToSdxUpgradeResponse(response)).thenReturn(sdxUpgradeResponse);
        when(sdxUpgradeClusterConverter.sdxUpgradeRequestToUpgradeV4Request(sdxUpgradeRequest)).thenCallRealMethod();
        response.setUpgradeCandidates(new ArrayList<>());
        sdxUpgradeResponse.setUpgradeCandidates(new ArrayList<>());
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> doAs(USER_CRN, () ->
                        underTest.triggerUpgradeByCrn(USER_CRN, CLUSTER_CRN, sdxUpgradeRequest, false)));

        assertEquals("There is no compatible image to upgrade for stack " + sdxCluster.getClusterName(), exception.getMessage());
        assertFalse(upgradeV4RequestCaptor.getValue().getInternalUpgradeSettings().isRollingUpgradeEnabled());
    }

    @Test
    public void testInvalidImageIdShouldReturnNoCompatibleImageFound() {
        when(sdxService.getByCrn(anyString(), anyString())).thenReturn(sdxCluster);
        ArgumentCaptor<UpgradeV4Request> upgradeV4RequestCaptor = ArgumentCaptor.forClass(UpgradeV4Request.class);
        when(stackV4Endpoint.checkForClusterUpgradeByName(eq(0L), eq(STACK_NAME), upgradeV4RequestCaptor.capture(), eq(ACCOUNT_ID))).thenReturn(response);
        when(sdxUpgradeClusterConverter.sdxUpgradeRequestToUpgradeV4Request(sdxUpgradeRequest)).thenCallRealMethod();
        when(sdxUpgradeClusterConverter.upgradeResponseToSdxUpgradeResponse(any(UpgradeV4Response.class))).thenReturn(sdxUpgradeResponse);
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(ANOTHER_IMAGE_ID);
        imageInfo.setComponentVersions(new ImageComponentVersions("", "", "", "", "", "", List.of()));
        response.setUpgradeCandidates(List.of(imageInfo));
        sdxUpgradeResponse.setUpgradeCandidates(List.of(imageInfo));
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> doAs(USER_CRN, () ->
                        underTest.triggerUpgradeByCrn(USER_CRN, CLUSTER_CRN, sdxUpgradeRequest, false)));

        assertEquals(String.format("The given image (%s) is not eligible for the cluster upgrade. "
                + "Please choose an id from the following: %s", IMAGE_ID, ANOTHER_IMAGE_ID), exception.getMessage());
        assertFalse(upgradeV4RequestCaptor.getValue().getInternalUpgradeSettings().isRollingUpgradeEnabled());
    }

    @Test
    public void testOtherError() {
        when(sdxService.getByCrn(anyString(), anyString())).thenReturn(sdxCluster);
        ArgumentCaptor<UpgradeV4Request> upgradeV4RequestCaptor = ArgumentCaptor.forClass(UpgradeV4Request.class);
        when(stackV4Endpoint.checkForClusterUpgradeByName(eq(0L), eq(STACK_NAME), upgradeV4RequestCaptor.capture(), eq(ACCOUNT_ID))).thenReturn(response);
        when(sdxUpgradeClusterConverter.sdxUpgradeRequestToUpgradeV4Request(sdxUpgradeRequest)).thenCallRealMethod();
        when(sdxUpgradeClusterConverter.upgradeResponseToSdxUpgradeResponse(any(UpgradeV4Response.class))).thenReturn(sdxUpgradeResponse);
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setComponentVersions(new ImageComponentVersions("", "", "", "", "", "", List.of()));
        response.setUpgradeCandidates(List.of(imageInfo));
        response.setReason("error reason");
        sdxUpgradeResponse.setUpgradeCandidates(List.of(imageInfo));
        sdxUpgradeResponse.setReason("error reason");

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> doAs(USER_CRN, () ->
                        underTest.triggerUpgradeByCrn(USER_CRN, CLUSTER_CRN, sdxUpgradeRequest, false)));

        assertEquals(String.format("The following error prevents the cluster upgrade process, please fix it and try again: %s",
                "error reason"), exception.getMessage());
        assertFalse(upgradeV4RequestCaptor.getValue().getInternalUpgradeSettings().isRollingUpgradeEnabled());
    }

    @Test
    public void testNoCompatibleRuntimeFound() {
        sdxCluster.setClusterShape(SdxClusterShape.ENTERPRISE);
        when(sdxService.getByCrn(anyString(), anyString())).thenReturn(sdxCluster);
        ArgumentCaptor<UpgradeV4Request> upgradeV4RequestCaptor = ArgumentCaptor.forClass(UpgradeV4Request.class);
        when(stackV4Endpoint.checkForClusterUpgradeByName(eq(0L), eq(STACK_NAME), upgradeV4RequestCaptor.capture(), eq(ACCOUNT_ID))).thenReturn(response);
        when(sdxUpgradeClusterConverter.sdxUpgradeRequestToUpgradeV4Request(sdxUpgradeRequest)).thenCallRealMethod();
        when(sdxUpgradeClusterConverter.upgradeResponseToSdxUpgradeResponse(any(UpgradeV4Response.class))).thenReturn(sdxUpgradeResponse);
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setComponentVersions(createExpectedPackageVersions());
        response.setUpgradeCandidates(List.of(imageInfo));
        sdxUpgradeResponse.setUpgradeCandidates(List.of(imageInfo));

        sdxUpgradeRequest.setRuntime(ANOTHER_TARGET_RUNTIME);
        sdxUpgradeRequest.setLockComponents(false);
        sdxUpgradeRequest.setImageId(null);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> doAs(USER_CRN, () ->
                        underTest.triggerUpgradeByCrn(USER_CRN, CLUSTER_CRN, sdxUpgradeRequest, false)));

        assertEquals(String.format("There is no image eligible for the cluster upgrade with runtime: %s. "
                + "Please choose a runtime from the following: %s", ANOTHER_TARGET_RUNTIME, MATCHING_TARGET_RUNTIME), exception.getMessage());
        assertFalse(upgradeV4RequestCaptor.getValue().getInternalUpgradeSettings().isRollingUpgradeEnabled());
    }

    @Test
    public void testCompatibleRuntimeFoundShouldReturnLatestImage() {
        when(sdxService.getByCrn(anyString(), anyString())).thenReturn(sdxCluster);
        ArgumentCaptor<UpgradeV4Request> upgradeV4RequestCaptor = ArgumentCaptor.forClass(UpgradeV4Request.class);
        when(stackV4Endpoint.checkForClusterUpgradeByName(eq(0L), eq(STACK_NAME), upgradeV4RequestCaptor.capture(), eq(ACCOUNT_ID))).thenReturn(response);
        when(sdxUpgradeClusterConverter.sdxUpgradeRequestToUpgradeV4Request(sdxUpgradeRequest)).thenCallRealMethod();
        when(sdxUpgradeClusterConverter.upgradeResponseToSdxUpgradeResponse(any(UpgradeV4Response.class))).thenReturn(sdxUpgradeResponse);
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setCreated(1L);
        imageInfo.setComponentVersions(createExpectedPackageVersions());
        ImageInfoV4Response lastImageInfo = new ImageInfoV4Response();
        lastImageInfo.setImageId(IMAGE_ID_LAST);
        lastImageInfo.setCreated(2L);
        lastImageInfo.setComponentVersions(createExpectedPackageVersions());
        response.setUpgradeCandidates(List.of(imageInfo, lastImageInfo));
        sdxUpgradeResponse.setUpgradeCandidates(List.of(imageInfo, lastImageInfo));

        sdxUpgradeRequest.setLockComponents(false);
        sdxUpgradeRequest.setImageId(null);
        sdxUpgradeRequest.setReplaceVms(REPAIR_AFTER_UPGRADE);
        sdxUpgradeRequest.setRollingUpgradeEnabled(ROLLING_UPGRADE_ENABLED);

        doAs(USER_CRN, () ->
                underTest.triggerUpgradeByCrn(USER_CRN, CLUSTER_CRN, sdxUpgradeRequest, false));

        verify(sdxReactorFlowManager, times(1)).triggerDatalakeRuntimeUpgradeFlow(sdxCluster, IMAGE_ID_LAST, REPAIR_AFTER_UPGRADE, SKIP_BACKUP,
                SKIP_OPTIONS, ROLLING_UPGRADE_ENABLED, TestConstants.DO_NOT_KEEP_VARIANT);
        verify(sdxReactorFlowManager, times(0)).triggerDatalakeRuntimeUpgradeFlow(sdxCluster, IMAGE_ID, REPAIR_AFTER_UPGRADE, SKIP_BACKUP,
                SKIP_OPTIONS, ROLLING_UPGRADE_ENABLED, TestConstants.DO_NOT_KEEP_VARIANT);
        assertTrue(upgradeV4RequestCaptor.getValue().getInternalUpgradeSettings().isRollingUpgradeEnabled());
    }

    @Test
    public void testRollingUpgradeForMediumDutyDataLakeShouldBeEnabledWhenSkipRollingValidationEntitlementIsEnabled() {
        sdxCluster.setClusterShape(SdxClusterShape.MEDIUM_DUTY_HA);
        when(sdxService.getByCrn(anyString(), anyString())).thenReturn(sdxCluster);
        ArgumentCaptor<UpgradeV4Request> upgradeV4RequestCaptor = ArgumentCaptor.forClass(UpgradeV4Request.class);
        when(stackV4Endpoint.checkForClusterUpgradeByName(eq(0L), eq(STACK_NAME), upgradeV4RequestCaptor.capture(), eq(ACCOUNT_ID))).thenReturn(response);
        when(sdxUpgradeClusterConverter.sdxUpgradeRequestToUpgradeV4Request(sdxUpgradeRequest)).thenCallRealMethod();
        when(sdxUpgradeClusterConverter.upgradeResponseToSdxUpgradeResponse(any(UpgradeV4Response.class))).thenReturn(sdxUpgradeResponse);
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setCreated(1L);
        imageInfo.setComponentVersions(createExpectedPackageVersions());
        ImageInfoV4Response lastImageInfo = new ImageInfoV4Response();
        lastImageInfo.setImageId(IMAGE_ID_LAST);
        lastImageInfo.setCreated(2L);
        lastImageInfo.setComponentVersions(createExpectedPackageVersions());
        response.setUpgradeCandidates(List.of(imageInfo, lastImageInfo));
        sdxUpgradeResponse.setUpgradeCandidates(List.of(imageInfo, lastImageInfo));

        sdxUpgradeRequest.setLockComponents(false);
        sdxUpgradeRequest.setImageId(null);
        sdxUpgradeRequest.setReplaceVms(REPAIR_AFTER_UPGRADE);
        sdxUpgradeRequest.setRollingUpgradeEnabled(ROLLING_UPGRADE_ENABLED);

        doAs(USER_CRN, () ->
                underTest.triggerUpgradeByCrn(USER_CRN, CLUSTER_CRN, sdxUpgradeRequest, false));

        verify(sdxReactorFlowManager, times(1)).triggerDatalakeRuntimeUpgradeFlow(sdxCluster, IMAGE_ID_LAST, REPAIR_AFTER_UPGRADE, SKIP_BACKUP,
                SKIP_OPTIONS, ROLLING_UPGRADE_ENABLED, TestConstants.DO_NOT_KEEP_VARIANT);
        verify(sdxReactorFlowManager, times(0)).triggerDatalakeRuntimeUpgradeFlow(sdxCluster, IMAGE_ID, REPAIR_AFTER_UPGRADE, SKIP_BACKUP,
                SKIP_OPTIONS, ROLLING_UPGRADE_ENABLED, TestConstants.DO_NOT_KEEP_VARIANT);
        assertTrue(upgradeV4RequestCaptor.getValue().getInternalUpgradeSettings().isRollingUpgradeEnabled());
    }

    @Test
    public void testSkipOptionsShouldBePassedToDrService() {
        DatalakeDrSkipOptions skipOptions = new DatalakeDrSkipOptions(true, true, true, true);
        sdxUpgradeRequest.setSkipValidation(true);
        sdxUpgradeRequest.setSkipAtlasMetadata(true);
        sdxUpgradeRequest.setSkipRangerAudits(true);
        sdxUpgradeRequest.setSkipRangerMetadata(true);
        sdxUpgradeRequest.setRollingUpgradeEnabled(ROLLING_UPGRADE_ENABLED);

        when(sdxService.getByCrn(anyString(), anyString())).thenReturn(sdxCluster);
        ArgumentCaptor<UpgradeV4Request> upgradeV4RequestCaptor = ArgumentCaptor.forClass(UpgradeV4Request.class);
        when(stackV4Endpoint.checkForClusterUpgradeByName(eq(0L), eq(STACK_NAME), upgradeV4RequestCaptor.capture(), eq(ACCOUNT_ID))).thenReturn(response);
        when(sdxUpgradeClusterConverter.sdxUpgradeRequestToUpgradeV4Request(sdxUpgradeRequest)).thenCallRealMethod();
        when(sdxUpgradeClusterConverter.upgradeResponseToSdxUpgradeResponse(any(UpgradeV4Response.class))).thenReturn(sdxUpgradeResponse);
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setComponentVersions(createExpectedPackageVersions());
        response.setUpgradeCandidates(List.of(imageInfo));
        sdxUpgradeResponse.setUpgradeCandidates(List.of(imageInfo));

        doAs(USER_CRN, () ->
                underTest.triggerUpgradeByCrn(USER_CRN, CLUSTER_CRN, sdxUpgradeRequest, false));
        verify(sdxReactorFlowManager, times(1)).triggerDatalakeRuntimeUpgradeFlow(sdxCluster, IMAGE_ID, REPAIR_AFTER_UPGRADE, SKIP_BACKUP,
                skipOptions, ROLLING_UPGRADE_ENABLED, TestConstants.DO_NOT_KEEP_VARIANT);
        assertTrue(upgradeV4RequestCaptor.getValue().getInternalUpgradeSettings().isRollingUpgradeEnabled());
    }

    @Test
    public void testNoError() {
        when(sdxService.getByCrn(anyString(), anyString())).thenReturn(sdxCluster);
        ArgumentCaptor<UpgradeV4Request> upgradeV4RequestCaptor = ArgumentCaptor.forClass(UpgradeV4Request.class);
        when(stackV4Endpoint.checkForClusterUpgradeByName(eq(0L), eq(STACK_NAME), upgradeV4RequestCaptor.capture(), eq(ACCOUNT_ID))).thenReturn(response);
        when(sdxUpgradeClusterConverter.sdxUpgradeRequestToUpgradeV4Request(sdxUpgradeRequest)).thenCallRealMethod();
        when(sdxUpgradeClusterConverter.upgradeResponseToSdxUpgradeResponse(any(UpgradeV4Response.class))).thenReturn(sdxUpgradeResponse);
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setComponentVersions(createExpectedPackageVersions());
        response.setUpgradeCandidates(List.of(imageInfo));
        sdxUpgradeResponse.setUpgradeCandidates(List.of(imageInfo));

        assertDoesNotThrow(() -> doAs(USER_CRN, () ->
                underTest.triggerUpgradeByCrn(USER_CRN, CLUSTER_CRN, sdxUpgradeRequest, false)));
        assertFalse(upgradeV4RequestCaptor.getValue().getInternalUpgradeSettings().isRollingUpgradeEnabled());
    }

    @Test
    public void testNoErrorWithEnterpriseShape() {
        sdxCluster.setClusterShape(SdxClusterShape.ENTERPRISE);
        when(sdxService.getByCrn(anyString(), anyString())).thenReturn(sdxCluster);
        ArgumentCaptor<UpgradeV4Request> upgradeV4RequestCaptor = ArgumentCaptor.forClass(UpgradeV4Request.class);
        when(stackV4Endpoint.checkForClusterUpgradeByName(eq(0L), eq(STACK_NAME), upgradeV4RequestCaptor.capture(), eq(ACCOUNT_ID))).thenReturn(response);
        when(sdxUpgradeClusterConverter.sdxUpgradeRequestToUpgradeV4Request(sdxUpgradeRequest)).thenCallRealMethod();
        when(sdxUpgradeClusterConverter.upgradeResponseToSdxUpgradeResponse(any(UpgradeV4Response.class))).thenReturn(sdxUpgradeResponse);
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setComponentVersions(createExpectedPackageVersions());
        response.setUpgradeCandidates(List.of(imageInfo));
        sdxUpgradeResponse.setUpgradeCandidates(List.of(imageInfo));

        assertDoesNotThrow(() -> doAs(USER_CRN, () ->
                underTest.triggerUpgradeByCrn(USER_CRN, CLUSTER_CRN, sdxUpgradeRequest, false)));
        assertFalse(upgradeV4RequestCaptor.getValue().getInternalUpgradeSettings().isRollingUpgradeEnabled());
    }

    @Test
    public void testPrepare() {
        when(sdxService.getByCrn(anyString(), anyString())).thenReturn(sdxCluster);
        ArgumentCaptor<UpgradeV4Request> upgradeV4RequestCaptor = ArgumentCaptor.forClass(UpgradeV4Request.class);
        when(stackV4Endpoint.checkForClusterUpgradeByName(eq(0L), eq(STACK_NAME), upgradeV4RequestCaptor.capture(), eq(ACCOUNT_ID))).thenReturn(response);
        when(sdxUpgradeClusterConverter.sdxUpgradeRequestToUpgradeV4Request(sdxUpgradeRequest)).thenCallRealMethod();
        when(sdxUpgradeClusterConverter.upgradeResponseToSdxUpgradeResponse(any(UpgradeV4Response.class))).thenReturn(sdxUpgradeResponse);
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setComponentVersions(createExpectedPackageVersions());
        response.setUpgradeCandidates(List.of(imageInfo));
        sdxUpgradeResponse.setUpgradeCandidates(List.of(imageInfo));

        assertDoesNotThrow(() -> doAs(USER_CRN, () ->
                underTest.triggerUpgradeByCrn(USER_CRN, CLUSTER_CRN, sdxUpgradeRequest, true)));
        assertTrue(upgradeV4RequestCaptor.getValue().getInternalUpgradeSettings().isUpgradePreparation());
        assertFalse(upgradeV4RequestCaptor.getValue().getInternalUpgradeSettings().isSkipValidations());
    }

    @Test
    public void testTriggerUpgradeWithValidPaywallLicense() {
        when(sdxService.getByCrn(anyString(), anyString())).thenReturn(sdxCluster);
        ArgumentCaptor<UpgradeV4Request> upgradeV4RequestCaptor = ArgumentCaptor.forClass(UpgradeV4Request.class);
        when(stackV4Endpoint.checkForClusterUpgradeByName(eq(0L), eq(STACK_NAME), upgradeV4RequestCaptor.capture(), eq(ACCOUNT_ID))).thenReturn(response);
        when(sdxUpgradeClusterConverter.sdxUpgradeRequestToUpgradeV4Request(sdxUpgradeRequest)).thenCallRealMethod();
        when(sdxUpgradeClusterConverter.upgradeResponseToSdxUpgradeResponse(any(UpgradeV4Response.class))).thenReturn(sdxUpgradeResponse);
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setCreated(1L);
        imageInfo.setComponentVersions(createExpectedPackageVersions());
        ImageInfoV4Response lastImageInfo = new ImageInfoV4Response();
        lastImageInfo.setImageId(IMAGE_ID_LAST);
        lastImageInfo.setCreated(2L);
        lastImageInfo.setComponentVersions(createExpectedPackageVersions());
        response.setUpgradeCandidates(List.of(imageInfo, lastImageInfo));
        sdxUpgradeResponse.setUpgradeCandidates(List.of(imageInfo, lastImageInfo));

        sdxUpgradeRequest.setLockComponents(false);
        sdxUpgradeRequest.setImageId(null);
        sdxUpgradeRequest.setReplaceVms(REPAIR_AFTER_UPGRADE);
        sdxUpgradeRequest.setRollingUpgradeEnabled(ROLLING_UPGRADE_ENABLED);

        doAs(USER_CRN, () ->
                underTest.triggerUpgradeByCrn(USER_CRN, CLUSTER_CRN, sdxUpgradeRequest, false));
        verify(sdxReactorFlowManager, times(1)).triggerDatalakeRuntimeUpgradeFlow(sdxCluster, IMAGE_ID_LAST, REPAIR_AFTER_UPGRADE,
                SKIP_BACKUP, SKIP_OPTIONS, ROLLING_UPGRADE_ENABLED, TestConstants.DO_NOT_KEEP_VARIANT);
        verify(sdxReactorFlowManager, times(0)).triggerDatalakeRuntimeUpgradeFlow(sdxCluster, IMAGE_ID, REPAIR_AFTER_UPGRADE,
                SKIP_BACKUP, SKIP_OPTIONS, ROLLING_UPGRADE_ENABLED, TestConstants.DO_NOT_KEEP_VARIANT);
        assertTrue(upgradeV4RequestCaptor.getValue().getInternalUpgradeSettings().isRollingUpgradeEnabled());
    }

    @Test
    public void testTriggerRuntimeUpgradeByCrnWhenNotEnabledAndNoPatchUpgrades() {
        when(sdxService.getByCrn(anyString(), anyString())).thenReturn(sdxCluster);
        ImageInfoV4Response currentImageInfo = new ImageInfoV4Response();
        currentImageInfo.setImageId(IMAGE_ID);
        currentImageInfo.setCreated(1L);
        currentImageInfo.setComponentVersions(creatImageComponentVersions(MATCHING_TARGET_RUNTIME, MATCHING_TARGET_RUNTIME));
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setCreated(1L);
        imageInfo.setComponentVersions(creatImageComponentVersions("7.2.0", "7.2.0"));
        ImageInfoV4Response imageInfo2 = new ImageInfoV4Response();
        imageInfo2.setImageId(IMAGE_ID_LAST);
        imageInfo2.setCreated(2L);
        imageInfo2.setComponentVersions(creatImageComponentVersions("7.3.0", "7.3.0"));
        List<ImageInfoV4Response> candidates = List.of(imageInfo, imageInfo2);
        response.setCurrent(currentImageInfo);
        response.setUpgradeCandidates(candidates);
        SdxUpgradeResponse expectedResponse = new SdxUpgradeResponse(response.getCurrent(), List.of(), "Something went wrong", response.getFlowIdentifier());

        ArgumentCaptor<UpgradeV4Request> upgradeV4RequestCaptor = ArgumentCaptor.forClass(UpgradeV4Request.class);
        when(stackV4Endpoint.checkForClusterUpgradeByName(eq(0L), eq(STACK_NAME), upgradeV4RequestCaptor.capture(), eq(ACCOUNT_ID))).thenReturn(response);
        when(sdxUpgradeClusterConverter.sdxUpgradeRequestToUpgradeV4Request(sdxUpgradeRequest)).thenCallRealMethod();
        when(sdxUpgradeClusterConverter.upgradeResponseToSdxUpgradeResponse(any(UpgradeV4Response.class))).thenReturn(expectedResponse);
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> underTest.triggerUpgradeByCrn(USER_CRN, STACK_NAME, sdxUpgradeRequest, false));

        assertTrue(exception.getMessage().contains("Something went wrong"));
        assertFalse(upgradeV4RequestCaptor.getValue().getInternalUpgradeSettings().isRollingUpgradeEnabled());
    }

    @Test
    public void testTriggerRuntimeUpgradeByNameWhenNotEnabledAndNoPatchUpgrades() {
        when(sdxService.getByNameInAccount(USER_CRN, STACK_NAME)).thenReturn(sdxCluster);
        ImageInfoV4Response currentImageInfo = new ImageInfoV4Response();
        currentImageInfo.setImageId(IMAGE_ID);
        currentImageInfo.setCreated(1L);
        currentImageInfo.setComponentVersions(creatImageComponentVersions(MATCHING_TARGET_RUNTIME, MATCHING_TARGET_RUNTIME));
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setCreated(1L);
        imageInfo.setComponentVersions(creatImageComponentVersions("7.2.0", "7.2.0"));
        ImageInfoV4Response imageInfo2 = new ImageInfoV4Response();
        imageInfo2.setImageId(IMAGE_ID_LAST);
        imageInfo2.setCreated(2L);
        imageInfo2.setComponentVersions(creatImageComponentVersions("7.3.0", "7.3.0"));
        List<ImageInfoV4Response> candidates = List.of(imageInfo, imageInfo2);
        response.setCurrent(currentImageInfo);
        response.setUpgradeCandidates(candidates);
        SdxUpgradeResponse expectedResponse = new SdxUpgradeResponse(response.getCurrent(), List.of(), "Something went wrong", response.getFlowIdentifier());

        ArgumentCaptor<UpgradeV4Request> upgradeV4RequestCaptor = ArgumentCaptor.forClass(UpgradeV4Request.class);
        when(stackV4Endpoint.checkForClusterUpgradeByName(eq(0L), eq(STACK_NAME), upgradeV4RequestCaptor.capture(), eq(ACCOUNT_ID))).thenReturn(response);
        when(sdxUpgradeClusterConverter.sdxUpgradeRequestToUpgradeV4Request(sdxUpgradeRequest)).thenCallRealMethod();
        when(sdxUpgradeClusterConverter.upgradeResponseToSdxUpgradeResponse(any(UpgradeV4Response.class))).thenReturn(expectedResponse);
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> underTest.triggerUpgradeByName(USER_CRN, STACK_NAME, sdxUpgradeRequest, false));

        assertTrue(exception.getMessage().contains("Something went wrong"));
        assertFalse(upgradeV4RequestCaptor.getValue().getInternalUpgradeSettings().isRollingUpgradeEnabled());
    }

    @Test
    @DisplayName("Test checkForUpgradeByCrn() when Runtime Upgrade is enabled and no current image information is provided in upgrade response")
    public void testCheckForUpgradeByCrnWhenDisabledAndNoCurrentImage() {
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setCreated(1L);
        imageInfo.setComponentVersions(createExpectedPackageVersions());
        ImageInfoV4Response lastImageInfo = new ImageInfoV4Response();
        lastImageInfo.setImageId(IMAGE_ID_LAST);
        lastImageInfo.setCreated(2L);
        lastImageInfo.setComponentVersions(createExpectedPackageVersions());
        List<ImageInfoV4Response> candidates = List.of(imageInfo, lastImageInfo);
        response.setUpgradeCandidates(candidates);
        SdxUpgradeResponse expectedResponse = new SdxUpgradeResponse(response.getCurrent(), candidates, response.getReason(), response.getFlowIdentifier());

        when(sdxService.getByCrn(anyString(), anyString())).thenReturn(sdxCluster);
        ArgumentCaptor<UpgradeV4Request> upgradeV4RequestCaptor = ArgumentCaptor.forClass(UpgradeV4Request.class);
        when(stackV4Endpoint.checkForClusterUpgradeByName(eq(0L), eq(STACK_NAME), upgradeV4RequestCaptor.capture(), eq(ACCOUNT_ID))).thenReturn(response);
        when(sdxUpgradeClusterConverter.sdxUpgradeRequestToUpgradeV4Request(sdxUpgradeRequest)).thenCallRealMethod();
        ArgumentCaptor<UpgradeV4Response> upgradeV4ResponseCaptor = ArgumentCaptor.forClass(UpgradeV4Response.class);
        when(sdxUpgradeClusterConverter.upgradeResponseToSdxUpgradeResponse(upgradeV4ResponseCaptor.capture())).thenReturn(expectedResponse);

        SdxUpgradeResponse actualResponse = underTest.checkForUpgradeByCrn(USER_CRN, CLUSTER_CRN, sdxUpgradeRequest, false);

        UpgradeV4Response capturedUpgradeV4Response = upgradeV4ResponseCaptor.getValue();
        assertEquals(actualResponse, expectedResponse);
        assertFalse(capturedUpgradeV4Response.getUpgradeCandidates().isEmpty());
        assertFalse(upgradeV4RequestCaptor.getValue().getInternalUpgradeSettings().isRollingUpgradeEnabled());
    }

    @Test
    @DisplayName("Test checkForUpgradeByCrn() when Runtime Upgrade is enabled and patch updates are available")
    public void testCheckForUpgradeByCrnWhenDisabledAndPatchUpdatesAvailable() {
        ImageInfoV4Response currentImageInfo = new ImageInfoV4Response();
        currentImageInfo.setImageId(IMAGE_ID);
        currentImageInfo.setCreated(1L);
        currentImageInfo.setComponentVersions(creatImageComponentVersions(MATCHING_TARGET_RUNTIME, MATCHING_TARGET_RUNTIME));
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setCreated(1L);
        imageInfo.setComponentVersions(creatImageComponentVersions(MATCHING_TARGET_RUNTIME, MATCHING_TARGET_RUNTIME));
        ImageInfoV4Response imageInfo2 = new ImageInfoV4Response();
        imageInfo2.setImageId(IMAGE_ID_LAST);
        imageInfo2.setCreated(2L);
        imageInfo2.setComponentVersions(creatImageComponentVersions("7.3.0", "7.3.0"));
        List<ImageInfoV4Response> candidates = List.of(imageInfo, imageInfo2);
        response.setCurrent(currentImageInfo);
        response.setUpgradeCandidates(candidates);
        SdxUpgradeResponse expectedResponse = new SdxUpgradeResponse(response.getCurrent(), candidates, response.getReason(), response.getFlowIdentifier());
        SdxUpgradeRequest upgradeRequest = new SdxUpgradeRequest();

        when(sdxService.getByCrn(anyString(), anyString())).thenReturn(sdxCluster);
        sdxCluster.setClusterShape(SdxClusterShape.ENTERPRISE);
        ArgumentCaptor<UpgradeV4Request> upgradeV4RequestCaptor = ArgumentCaptor.forClass(UpgradeV4Request.class);
        when(stackV4Endpoint.checkForClusterUpgradeByName(eq(0L), eq(STACK_NAME), upgradeV4RequestCaptor.capture(), eq(ACCOUNT_ID))).thenReturn(response);
        when(sdxUpgradeClusterConverter.sdxUpgradeRequestToUpgradeV4Request(upgradeRequest)).thenCallRealMethod();
        ArgumentCaptor<UpgradeV4Response> upgradeV4ResponseCaptor = ArgumentCaptor.forClass(UpgradeV4Response.class);
        when(sdxUpgradeClusterConverter.upgradeResponseToSdxUpgradeResponse(upgradeV4ResponseCaptor.capture())).thenReturn(expectedResponse);

        SdxUpgradeResponse actualResponse = underTest.checkForUpgradeByCrn(USER_CRN, CLUSTER_CRN, upgradeRequest, false);

        UpgradeV4Response capturedUpgradeV4Response = upgradeV4ResponseCaptor.getValue();
        assertEquals(expectedResponse, actualResponse);
        assertTrue(StringUtils.isEmpty(capturedUpgradeV4Response.getReason()));
        assertEquals(2, capturedUpgradeV4Response.getUpgradeCandidates().size());
        assertEquals(MATCHING_TARGET_RUNTIME, capturedUpgradeV4Response.getUpgradeCandidates().get(0).getComponentVersions().getCdp());
        assertFalse(upgradeV4RequestCaptor.getValue().getInternalUpgradeSettings().isRollingUpgradeEnabled());
    }

    @Test
    @DisplayName("Test checkForUpgradeByCrn() when Runtime Upgrade is enabled and request imageid param is wrong")
    public void testCheckForUpgradeByCrnWhenDisabledAndRequestImageIdParamIsWrong() {
        SdxUpgradeRequest sdxUpgradeRequest = new SdxUpgradeRequest();
        sdxUpgradeRequest.setImageId(IMAGE_ID_LAST);
        sdxUpgradeRequest.setRuntime("7.2.17");
        ImageInfoV4Response currentImageInfo = new ImageInfoV4Response();
        currentImageInfo.setImageId(IMAGE_ID);
        currentImageInfo.setCreated(1L);
        currentImageInfo.setComponentVersions(creatImageComponentVersions(MATCHING_TARGET_RUNTIME, MATCHING_TARGET_RUNTIME));
        ImageInfoV4Response imageInfo = new ImageInfoV4Response();
        imageInfo.setImageId(IMAGE_ID);
        imageInfo.setCreated(1L);
        imageInfo.setComponentVersions(creatImageComponentVersions("7.2.0", "7.2.0"));
        ImageInfoV4Response imageInfo2 = new ImageInfoV4Response();
        imageInfo2.setImageId(IMAGE_ID_LAST);
        imageInfo2.setCreated(2L);
        imageInfo2.setComponentVersions(creatImageComponentVersions("7.3.0", "7.3.0"));
        List<ImageInfoV4Response> candidates = List.of(imageInfo, imageInfo2);
        response.setCurrent(currentImageInfo);
        response.setUpgradeCandidates(candidates);
        SdxUpgradeResponse expectedResponse = new SdxUpgradeResponse(response.getCurrent(), candidates, response.getReason(), response.getFlowIdentifier());

        when(sdxService.getByCrn(anyString(), anyString())).thenReturn(sdxCluster);
        ArgumentCaptor<UpgradeV4Request> upgradeV4RequestCaptor = ArgumentCaptor.forClass(UpgradeV4Request.class);
        when(stackV4Endpoint.checkForClusterUpgradeByName(eq(0L), eq(STACK_NAME), upgradeV4RequestCaptor.capture(), eq(ACCOUNT_ID))).thenReturn(response);
        when(sdxUpgradeClusterConverter.sdxUpgradeRequestToUpgradeV4Request(sdxUpgradeRequest)).thenCallRealMethod();
        ArgumentCaptor<UpgradeV4Response> upgradeV4ResponseCaptor = ArgumentCaptor.forClass(UpgradeV4Response.class);
        when(sdxUpgradeClusterConverter.upgradeResponseToSdxUpgradeResponse(upgradeV4ResponseCaptor.capture())).thenReturn(expectedResponse);
        SdxUpgradeResponse actualResponse = underTest.checkForUpgradeByCrn(USER_CRN, CLUSTER_CRN, sdxUpgradeRequest, false);

        UpgradeV4Response capturedUpgradeV4Response = upgradeV4ResponseCaptor.getValue();
        assertEquals(expectedResponse, actualResponse);
        assertEquals(2, capturedUpgradeV4Response.getUpgradeCandidates().size());
        assertNull(capturedUpgradeV4Response.getReason());
        assertFalse(upgradeV4RequestCaptor.getValue().getInternalUpgradeSettings().isRollingUpgradeEnabled());
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testCheckForUpgradeByName() {
        sdxCluster.setClusterShape(SdxClusterShape.MEDIUM_DUTY_HA);
        when(sdxService.getByNameInAccount(USER_CRN, STACK_NAME)).thenReturn(sdxCluster);
        setupInternalActorCrnAndUpgradeClusterConverterMock();
        constructUpgradeV4ResponseAndSetupStackV4EndpointMock("7.2.16", "7.2.17", "7.2.18");
        SdxUpgradeRequest sdxUpgradeRequest = new SdxUpgradeRequest();

        SdxUpgradeResponse response = doAs(USER_CRN, () -> underTest.checkForUpgradeByName(USER_CRN, STACK_NAME, sdxUpgradeRequest, false));

        List<ImageInfoV4Response> upgradeCandidates = response.getUpgradeCandidates();
        assertEquals(2, upgradeCandidates.size());
        List<String> componentVersions = upgradeCandidates.stream()
            .map(candidates -> candidates.getComponentVersions().getCdp())
            .toList();
        assertTrue(componentVersions.contains("7.2.16"));
        assertTrue(componentVersions.contains("7.2.17"));
        assertFalse(componentVersions.contains("7.2.18"));
    }

    @Test
    void testLightDutyUpgradeCandidates() {
        SdxCluster validSdxCluster = getValidSdxCluster();
        validSdxCluster.setRuntime("7.2.17");
        validSdxCluster.setClusterShape(SdxClusterShape.LIGHT_DUTY);
        when(sdxService.getByNameInAccount(USER_CRN, STACK_NAME)).thenReturn(sdxCluster);
        setupInternalActorCrnAndUpgradeClusterConverterMock();
        constructUpgradeV4ResponseAndSetupStackV4EndpointMock("7.2.16", "7.2.17", "7.2.18", "7.3.0");
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        request.setRuntime(null);
        request.setImageId(null);

        SdxUpgradeResponse response = underTest.checkForUpgradeByName(USER_CRN, STACK_NAME, request, false);

        assertEquals(4, response.getUpgradeCandidates().size());
        List<String> candidates = response.getUpgradeCandidates().stream().map(candidate -> candidate.getComponentVersions().getCdp()).toList();
        assertTrue(candidates.contains("7.2.18"));
        assertTrue(candidates.contains("7.3.0"));
        assertTrue(candidates.contains("7.2.16"));
        assertTrue(candidates.contains("7.2.17"));
    }

    @Test
    void testLightDutyCheckForUpgradeByCrnNoRuntimeAndNoImageId() {
        SdxCluster validSdxCluster = getValidSdxCluster();
        validSdxCluster.setRuntime("7.2.17");
        validSdxCluster.setClusterShape(SdxClusterShape.LIGHT_DUTY);
        when(sdxService.getByCrn(eq(USER_CRN), eq(CLUSTER_CRN))).thenReturn(sdxCluster);
        setupInternalActorCrnAndUpgradeClusterConverterMock();
        constructUpgradeV4ResponseAndSetupStackV4EndpointMock("7.2.16", "7.2.17", "7.2.18", "7.3.0");
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        request.setRuntime(null);
        request.setImageId(null);

        SdxUpgradeResponse response = underTest.checkForUpgradeByCrn(USER_CRN, CLUSTER_CRN, request, false);

        assertEquals(4, response.getUpgradeCandidates().size());
        List<String> candidates = response.getUpgradeCandidates().stream().map(candidate -> candidate.getComponentVersions().getCdp()).toList();
        assertTrue(candidates.contains("7.2.18"));
        assertTrue(candidates.contains("7.3.0"));
        assertTrue(candidates.contains("7.2.16"));
        assertTrue(candidates.contains("7.2.17"));
    }

    @Test
    void testEnterpriseCheckForUpgradeByCrnNoRuntimeAndNoImageId() {
        SdxCluster validSdxCluster = getValidSdxCluster();
        validSdxCluster.setRuntime("7.2.17");
        validSdxCluster.setClusterShape(SdxClusterShape.ENTERPRISE);
        when(sdxService.getByCrn(eq(USER_CRN), eq(CLUSTER_CRN))).thenReturn(validSdxCluster);
        setupInternalActorCrnAndUpgradeClusterConverterMock();
        constructUpgradeV4ResponseAndSetupStackV4EndpointMock("7.2.16", "7.2.17", "7.2.18", "7.3.0");
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        request.setRuntime(null);
        request.setImageId(null);

        SdxUpgradeResponse response = underTest.checkForUpgradeByCrn(USER_CRN, CLUSTER_CRN, request, false);

        assertEquals(4, response.getUpgradeCandidates().size());
        List<String> candidates = response.getUpgradeCandidates().stream().map(candidate -> candidate.getComponentVersions().getCdp()).toList();
        assertTrue(candidates.contains("7.2.18"));
        assertTrue(candidates.contains("7.3.0"));
        assertTrue(candidates.contains("7.2.16"));
        assertTrue(candidates.contains("7.2.17"));
    }

    @Test
    void testLightDutyCheckForUpgradeByCrnAndNoImageId() {
        SdxCluster validSdxCluster = getValidSdxCluster();
        validSdxCluster.setRuntime("7.2.17");
        validSdxCluster.setClusterShape(SdxClusterShape.LIGHT_DUTY);
        when(sdxService.getByCrn(eq(USER_CRN), eq(CLUSTER_CRN))).thenReturn(validSdxCluster);
        setupInternalActorCrnAndUpgradeClusterConverterMock();
        constructUpgradeV4ResponseAndSetupStackV4EndpointMock("7.2.16", "7.2.17", "7.2.18", "7.3.0");
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        request.setRuntime("7.2.18");
        request.setImageId(null);

        SdxUpgradeResponse response = underTest.checkForUpgradeByCrn(USER_CRN, CLUSTER_CRN, request, false);

        assertEquals(4, response.getUpgradeCandidates().size());
        List<String> candidates = response.getUpgradeCandidates().stream().map(candidate -> candidate.getComponentVersions().getCdp()).toList();
        assertTrue(candidates.contains("7.2.18"));
        assertTrue(candidates.contains("7.3.0"));
        assertTrue(candidates.contains("7.2.16"));
        assertTrue(candidates.contains("7.2.17"));
    }

    @Test
    void testMediumDutyCheckForUpgradeByCrnAndNoImageId() {
        SdxCluster validSdxCluster = getValidSdxCluster();
        validSdxCluster.setRuntime("7.2.17");
        validSdxCluster.setClusterShape(SdxClusterShape.MEDIUM_DUTY_HA);
        when(sdxService.getByCrn(eq(USER_CRN), eq(CLUSTER_CRN))).thenReturn(validSdxCluster);
        setupInternalActorCrnAndUpgradeClusterConverterMock();
        constructUpgradeV4ResponseAndSetupStackV4EndpointMock("7.2.16", "7.2.17", "7.2.18", "7.3.0");
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        request.setRuntime("7.2.18");
        request.setImageId(null);

        SdxUpgradeResponse response = doAs(USER_CRN, () -> underTest.checkForUpgradeByCrn(USER_CRN, CLUSTER_CRN, request, false));

        assertEquals(2, response.getUpgradeCandidates().size());
        List<String> candidates = response.getUpgradeCandidates().stream().map(candidate -> candidate.getComponentVersions().getCdp()).toList();
        assertFalse(candidates.contains("7.2.18"));
        assertFalse(candidates.contains("7.3.0"));
        assertTrue(candidates.contains("7.2.16"));
        assertTrue(candidates.contains("7.2.17"));
    }

    @Test
    void testMediumDutyCheckForUpgradeByCrnImageIdProvided() {
        SdxCluster validSdxCluster = getValidSdxCluster();
        validSdxCluster.setRuntime("7.2.17");
        validSdxCluster.setClusterShape(SdxClusterShape.MEDIUM_DUTY_HA);
        when(sdxService.getByCrn(eq(USER_CRN), eq(CLUSTER_CRN))).thenReturn(validSdxCluster);
        setupInternalActorCrnAndUpgradeClusterConverterMock();
        constructUpgradeV4ResponseAndSetupStackV4EndpointMock("7.2.16", "7.2.17", "7.2.18", "7.3.0");
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        request.setRuntime(null);
        request.setImageId("image-id");

        SdxUpgradeResponse response = doAs(USER_CRN, () -> underTest.checkForUpgradeByCrn(USER_CRN, CLUSTER_CRN, request, false));

        assertEquals(2, response.getUpgradeCandidates().size());
        List<String> candidates = response.getUpgradeCandidates().stream().map(candidate -> candidate.getComponentVersions().getCdp()).toList();
        assertFalse(candidates.contains("7.2.18"));
        assertFalse(candidates.contains("7.3.0"));
        assertTrue(candidates.contains("7.2.16"));
        assertTrue(candidates.contains("7.2.17"));
    }

    @Test
    void testMediumDutyCheckForUpgradeWhenTheEntitlementIsEnabled() {
        when(entitlementService.isSdxRuntimeUpgradeEnabledOnMediumDuty(eq(ACCOUNT_ID))).thenReturn(Boolean.TRUE);

        SdxCluster validSdxCluster = getValidSdxCluster();
        validSdxCluster.setRuntime("7.2.17");
        validSdxCluster.setClusterShape(SdxClusterShape.MEDIUM_DUTY_HA);
        when(sdxService.getByCrn(eq(USER_CRN), eq(CLUSTER_CRN))).thenReturn(validSdxCluster);
        setupInternalActorCrnAndUpgradeClusterConverterMock();
        constructUpgradeV4ResponseAndSetupStackV4EndpointMock("7.2.16", "7.2.16", "7.2.17", "7.2.18", "7.3.0");
        SdxUpgradeRequest request = new SdxUpgradeRequest();
        request.setRuntime("7.2.18");
        request.setImageId(null);
        SdxUpgradeResponse response = doAs(USER_CRN, () -> underTest.checkForUpgradeByCrn(USER_CRN, CLUSTER_CRN, request, false));

        assertEquals(5, response.getUpgradeCandidates().size());
        List<String> candidates = response.getUpgradeCandidates().stream().map(candidate -> candidate.getComponentVersions().getCdp()).toList();
        assertTrue(candidates.contains("7.2.18"));
        assertTrue(candidates.contains("7.3.0"));
        assertTrue(candidates.contains("7.2.16"));
        assertTrue(candidates.contains("7.2.17"));
    }

    private void constructUpgradeV4ResponseAndSetupStackV4EndpointMock(String... runtimes) {
        UpgradeV4Response upgradeV4Response = new UpgradeV4Response();
        List<ImageInfoV4Response> imageInfoV4Responses = Arrays.stream(runtimes)
            .map(runtime -> {
                ImageInfoV4Response imageInfoV4Response1 = new ImageInfoV4Response();
                imageInfoV4Response1.setComponentVersions(new ImageComponentVersions("7.10", "1234", runtime, "", "", "", List.of()));
                return imageInfoV4Response1;
            })
            .toList();
        upgradeV4Response.setUpgradeCandidates(imageInfoV4Responses);
        when(stackV4Endpoint.checkForClusterUpgradeByName(anyLong(), anyString(), any(), eq(ACCOUNT_ID))).thenReturn(upgradeV4Response);
    }

    private void setupInternalActorCrnAndUpgradeClusterConverterMock() {
        doCallRealMethod().when(sdxUpgradeClusterConverter).sdxUpgradeRequestToUpgradeV4Request(any(SdxUpgradeRequest.class));
        doCallRealMethod().when(sdxUpgradeClusterConverter).upgradeResponseToSdxUpgradeResponse(any());
    }

    private SdxCluster getValidSdxCluster() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName(STACK_NAME);
        sdxCluster.setCrn(CLUSTER_CRN);
        sdxCluster.setClusterShape(SdxClusterShape.ENTERPRISE);
        sdxCluster.setEnvName("test-env");
        sdxCluster.setId(1L);
        sdxCluster.setAccountId("accountid");
        return sdxCluster;
    }

    private SdxUpgradeRequest getFullSdxUpgradeRequest() {
        SdxUpgradeRequest sdxUpgradeRequest = new SdxUpgradeRequest();
        sdxUpgradeRequest.setImageId(IMAGE_ID);
        sdxUpgradeRequest.setRuntime(MATCHING_TARGET_RUNTIME);
        return sdxUpgradeRequest;
    }

    private ImageComponentVersions createExpectedPackageVersions() {
        ImageComponentVersions imageComponentVersions = new ImageComponentVersions();
        imageComponentVersions.setCm(V_7_0_3);
        imageComponentVersions.setCdp(V_7_0_2);
        imageComponentVersions.setOs(OsType.CENTOS7.getOs());
        return imageComponentVersions;
    }

    private ImageComponentVersions creatImageComponentVersions(String cmVersion, String cdpVersion) {
        ImageComponentVersions imageComponentVersions = new ImageComponentVersions();
        imageComponentVersions.setCm(cmVersion);
        imageComponentVersions.setCdp(cdpVersion);
        imageComponentVersions.setOs(OsType.CENTOS7.getOs());
        return imageComponentVersions;
    }

    private ImageInfoV4Response createCurrentImage() {
        ImageInfoV4Response imageInfoV4Response = new ImageInfoV4Response();
        ImageComponentVersions imageComponentVersions = new ImageComponentVersions();
        imageComponentVersions.setOs(OsType.CENTOS7.getOs());
        imageInfoV4Response.setComponentVersions(imageComponentVersions);
        return imageInfoV4Response;
    }

    private SdxCluster getValidEnterpriseCluster() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName(STACK_NAME);
        sdxCluster.setCrn(CLUSTER_CRN);
        sdxCluster.setClusterShape(SdxClusterShape.ENTERPRISE);
        sdxCluster.setEnvName("test-env");
        sdxCluster.setId(1L);
        sdxCluster.setAccountId("accountid");
        return sdxCluster;
    }
}
