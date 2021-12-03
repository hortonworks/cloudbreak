package com.sequenceiq.distrox.v1.distrox.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackImageChangeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.ClouderaManagerLicenseProvider;
import com.sequenceiq.cloudbreak.auth.JsonCMLicense;
import com.sequenceiq.cloudbreak.auth.PaywallAccessChecker;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.StackCommonService;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.image.locked.LockedComponentService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
class DistroXUpgradeServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:f3b8ed82-e712-4f89-bda7-be07183720d3";

    private static final NameOrCrn CLUSTER = NameOrCrn.ofName("cluster");

    private static final Long WS_ID = 1L;

    private static final Long STACK_ID = 3L;

    private static final boolean LOCK_COMPONENTS = false;

    private static final String ACCOUNT_ID = "9d74eee4-1cad-45d7-b645-7ccf9edbb73d";

    @Mock
    private DistroXUpgradeAvailabilityService upgradeAvailabilityService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private PaywallAccessChecker paywallAccessChecker;

    @Mock
    private DistroXUpgradeImageSelector imageSelector;

    @Mock
    private StackCommonService stackCommonService;

    @Mock
    private ReactorFlowManager reactorFlowManager;

    @Mock
    private StackService stackService;

    @Mock
    private ClouderaManagerLicenseProvider clouderaManagerLicenseProvider;

    @Mock
    private LockedComponentService lockedComponentService;

    @InjectMocks
    private DistroXUpgradeService underTest;

    private Stack stack;

    @BeforeEach
    public void setup() {
        stack = new Stack();
        stack.setId(STACK_ID);
        stack.setPlatformVariant("variant");
    }

    @Test
    public void testUpgradeResponseHasReason() {
        UpgradeV4Request request = new UpgradeV4Request();
        when(upgradeAvailabilityService.checkForUpgrade(CLUSTER, WS_ID, request, USER_CRN)).thenReturn(new UpgradeV4Response("reason", null));

        Assertions.assertThrows(BadRequestException.class, () -> underTest.triggerUpgrade(CLUSTER, WS_ID, USER_CRN, request));
    }

    @Test
    public void testUpgradeResponseHasNoCandidates() {
        UpgradeV4Request request = new UpgradeV4Request();
        when(upgradeAvailabilityService.checkForUpgrade(CLUSTER, WS_ID, request, USER_CRN)).thenReturn(new UpgradeV4Response());

        Assertions.assertThrows(BadRequestException.class, () -> underTest.triggerUpgrade(CLUSTER, WS_ID, USER_CRN, request));
    }

    @Test
    public void testCmLicenseMissing() {
        UpgradeV4Request request = new UpgradeV4Request();
        UpgradeV4Response response = new UpgradeV4Response();
        response.setUpgradeCandidates(List.of(mock(ImageInfoV4Response.class)));
        when(upgradeAvailabilityService.checkForUpgrade(CLUSTER, WS_ID, request, USER_CRN)).thenReturn(response);
        when(entitlementService.isInternalRepositoryForUpgradeAllowed(ACCOUNT_ID)).thenReturn(Boolean.FALSE);
        when(clouderaManagerLicenseProvider.getLicense(any())).thenThrow(new BadRequestException("No valid CM license is present"));

        BadRequestException exception = Assertions.assertThrows(BadRequestException.class, () -> underTest.triggerUpgrade(CLUSTER, WS_ID, USER_CRN, request));
        assertEquals(exception.getMessage(), "No valid CM license is present");
    }

    @Test
    public void testTriggerFlowWithPaywallCheck() {
        UpgradeV4Request request = new UpgradeV4Request();
        UpgradeV4Response response = new UpgradeV4Response();
        response.setUpgradeCandidates(List.of(mock(ImageInfoV4Response.class)));
        when(upgradeAvailabilityService.checkForUpgrade(CLUSTER, WS_ID, request, USER_CRN)).thenReturn(response);
        when(entitlementService.isInternalRepositoryForUpgradeAllowed(ACCOUNT_ID)).thenReturn(Boolean.FALSE);
        when(clouderaManagerLicenseProvider.getLicense(any())).thenReturn(new JsonCMLicense());
        ImageInfoV4Response imageInfoV4Response = new ImageInfoV4Response();
        imageInfoV4Response.setImageId("imgId");
        imageInfoV4Response.setImageCatalogName("catalogName");
        when(imageSelector.determineImageId(request, response.getUpgradeCandidates())).thenReturn(imageInfoV4Response);
        ArgumentCaptor<StackImageChangeV4Request> imageChangeRequestArgumentCaptor = ArgumentCaptor.forClass(StackImageChangeV4Request.class);
        ImageChangeDto imageChangeDto = new ImageChangeDto(STACK_ID, imageInfoV4Response.getImageId());
        when(stackCommonService.createImageChangeDto(eq(CLUSTER), eq(WS_ID), imageChangeRequestArgumentCaptor.capture()))
                .thenReturn(imageChangeDto);
        when(stackService.getByNameOrCrnInWorkspace(CLUSTER, WS_ID)).thenReturn(stack);
        when(lockedComponentService.isComponentsLocked(stack, imageInfoV4Response.getImageId())).thenReturn(LOCK_COMPONENTS);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW_CHAIN, "asdf");
        when(reactorFlowManager.triggerDistroXUpgrade(eq(STACK_ID), eq(imageChangeDto), anyBoolean(), eq(LOCK_COMPONENTS), anyString()))
                .thenReturn(flowIdentifier);

        UpgradeV4Response result = underTest.triggerUpgrade(CLUSTER, WS_ID, USER_CRN, request);

        verify(paywallAccessChecker).checkPaywallAccess(any(), any());
        assertEquals(flowIdentifier, result.getFlowIdentifier());
        assertTrue(result.getReason().contains(imageInfoV4Response.getImageId()));
        StackImageChangeV4Request imageChangeV4Request = imageChangeRequestArgumentCaptor.getValue();
        assertEquals(imageInfoV4Response.getImageId(), imageChangeV4Request.getImageId());
        assertEquals(imageInfoV4Response.getImageCatalogName(), imageChangeV4Request.getImageCatalogName());
    }

    @Test
    public void testTriggerFlowWithoutPaywallCheck() {
        UpgradeV4Request request = new UpgradeV4Request();
        UpgradeV4Response response = new UpgradeV4Response();
        response.setUpgradeCandidates(List.of(mock(ImageInfoV4Response.class)));
        when(upgradeAvailabilityService.checkForUpgrade(CLUSTER, WS_ID, request, USER_CRN)).thenReturn(response);
        when(entitlementService.isInternalRepositoryForUpgradeAllowed(ACCOUNT_ID)).thenReturn(Boolean.TRUE);
        ImageInfoV4Response imageInfoV4Response = new ImageInfoV4Response();
        imageInfoV4Response.setImageId("imgId");
        imageInfoV4Response.setImageCatalogName("catalogName");
        when(imageSelector.determineImageId(request, response.getUpgradeCandidates())).thenReturn(imageInfoV4Response);
        ArgumentCaptor<StackImageChangeV4Request> imageChangeRequestArgumentCaptor = ArgumentCaptor.forClass(StackImageChangeV4Request.class);
        ImageChangeDto imageChangeDto = new ImageChangeDto(STACK_ID, imageInfoV4Response.getImageId());
        when(stackCommonService.createImageChangeDto(eq(CLUSTER), eq(WS_ID), imageChangeRequestArgumentCaptor.capture()))
                .thenReturn(imageChangeDto);
        when(stackService.getByNameOrCrnInWorkspace(CLUSTER, WS_ID)).thenReturn(stack);
        when(lockedComponentService.isComponentsLocked(stack, imageInfoV4Response.getImageId())).thenReturn(LOCK_COMPONENTS);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW_CHAIN, "asdf");
        when(reactorFlowManager.triggerDistroXUpgrade(eq(STACK_ID), eq(imageChangeDto), anyBoolean(), eq(LOCK_COMPONENTS), anyString()))
                .thenReturn(flowIdentifier);

        UpgradeV4Response result = underTest.triggerUpgrade(CLUSTER, WS_ID, USER_CRN, request);

        verifyNoInteractions(paywallAccessChecker);
        assertEquals(flowIdentifier, result.getFlowIdentifier());
        assertTrue(result.getReason().contains(imageInfoV4Response.getImageId()));
        StackImageChangeV4Request imageChangeV4Request = imageChangeRequestArgumentCaptor.getValue();
        assertEquals(imageInfoV4Response.getImageId(), imageChangeV4Request.getImageId());
        assertEquals(imageInfoV4Response.getImageCatalogName(), imageChangeV4Request.getImageCatalogName());
    }

    @Test
    public void testTriggerFlowWithTurnOffReplaceVmsParam() {
        // GIVEN
        UpgradeV4Request request = new UpgradeV4Request();
        UpgradeV4Response response = new UpgradeV4Response();
        response.setReplaceVms(true);
        response.setUpgradeCandidates(List.of(mock(ImageInfoV4Response.class)));
        when(upgradeAvailabilityService.checkForUpgrade(CLUSTER, WS_ID, request, USER_CRN)).thenReturn(response);
        when(entitlementService.isInternalRepositoryForUpgradeAllowed(ACCOUNT_ID)).thenReturn(Boolean.FALSE);
        when(clouderaManagerLicenseProvider.getLicense(any())).thenReturn(new JsonCMLicense());
        ImageInfoV4Response imageInfoV4Response = new ImageInfoV4Response();
        imageInfoV4Response.setImageId("imgId");
        imageInfoV4Response.setImageCatalogName("catalogName");
        when(imageSelector.determineImageId(request, response.getUpgradeCandidates())).thenReturn(imageInfoV4Response);
        ArgumentCaptor<StackImageChangeV4Request> imageChangeRequestArgumentCaptor = ArgumentCaptor.forClass(StackImageChangeV4Request.class);
        ImageChangeDto imageChangeDto = new ImageChangeDto(STACK_ID, imageInfoV4Response.getImageId());
        when(stackCommonService.createImageChangeDto(eq(CLUSTER), eq(WS_ID), imageChangeRequestArgumentCaptor.capture())).thenReturn(imageChangeDto);
        when(stackService.getByNameOrCrnInWorkspace(CLUSTER, WS_ID)).thenReturn(stack);
        when(lockedComponentService.isComponentsLocked(stack, imageInfoV4Response.getImageId())).thenReturn(LOCK_COMPONENTS);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW_CHAIN, "asdf");
        when(reactorFlowManager.triggerDistroXUpgrade(eq(STACK_ID), eq(imageChangeDto), anyBoolean(), eq(LOCK_COMPONENTS), anyString()))
                .thenReturn(flowIdentifier);
        // WHEN
        UpgradeV4Response result = underTest.triggerUpgrade(CLUSTER, WS_ID, USER_CRN, request);
        // THEN
        verify(reactorFlowManager).triggerDistroXUpgrade(STACK_ID, imageChangeDto, false, LOCK_COMPONENTS, "variant");
        assertFalse(result.isReplaceVms());
    }

    @Test
    public void testTriggerFlowWithoutTurnOffReplaceVmsParam() {
        // GIVEN
        UpgradeV4Request request = createRequest(true);
        UpgradeV4Response response = new UpgradeV4Response();
        response.setReplaceVms(true);
        response.setUpgradeCandidates(List.of(mock(ImageInfoV4Response.class)));
        when(upgradeAvailabilityService.checkForUpgrade(CLUSTER, WS_ID, request, USER_CRN)).thenReturn(response);
        when(entitlementService.isInternalRepositoryForUpgradeAllowed(ACCOUNT_ID)).thenReturn(Boolean.FALSE);
        when(clouderaManagerLicenseProvider.getLicense(any())).thenReturn(new JsonCMLicense());
        ImageInfoV4Response imageInfoV4Response = new ImageInfoV4Response();
        imageInfoV4Response.setImageId("imgId");
        imageInfoV4Response.setImageCatalogName("catalogName");
        when(imageSelector.determineImageId(request, response.getUpgradeCandidates())).thenReturn(imageInfoV4Response);
        ArgumentCaptor<StackImageChangeV4Request> imageChangeRequestArgumentCaptor = ArgumentCaptor.forClass(StackImageChangeV4Request.class);
        ImageChangeDto imageChangeDto = new ImageChangeDto(STACK_ID, imageInfoV4Response.getImageId());
        when(stackCommonService.createImageChangeDto(eq(CLUSTER), eq(WS_ID), imageChangeRequestArgumentCaptor.capture())).thenReturn(imageChangeDto);
        when(stackService.getByNameOrCrnInWorkspace(CLUSTER, WS_ID)).thenReturn(stack);
        when(lockedComponentService.isComponentsLocked(stack, imageInfoV4Response.getImageId())).thenReturn(true);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW_CHAIN, "asdf");
        when(reactorFlowManager.triggerDistroXUpgrade(eq(STACK_ID), eq(imageChangeDto), anyBoolean(), anyBoolean(), anyString())).thenReturn(flowIdentifier);
        // WHEN
        UpgradeV4Response result = underTest.triggerUpgrade(CLUSTER, WS_ID, USER_CRN, request);
        // THEN
        verify(reactorFlowManager).triggerDistroXUpgrade(STACK_ID, imageChangeDto, true, true, "variant");
        assertTrue(result.isReplaceVms());
    }

    @Test
    public void testTriggerDistroXUpgradeShouldThrowBadRequestExceptionWhenTheOsUpgradeIsDisabledByEntitlement() {
        UpgradeV4Request request = createRequest(false);
        request.setLockComponents(true);
        UpgradeV4Response response = new UpgradeV4Response();
        response.setReplaceVms(true);
        response.setUpgradeCandidates(List.of(mock(ImageInfoV4Response.class)));
        when(upgradeAvailabilityService.checkForUpgrade(CLUSTER, WS_ID, request, USER_CRN)).thenReturn(response);
        ImageInfoV4Response imageInfoV4Response = new ImageInfoV4Response();
        imageInfoV4Response.setImageId("imgId");
        imageInfoV4Response.setImageCatalogName("catalogName");
        when(imageSelector.determineImageId(request, response.getUpgradeCandidates())).thenReturn(imageInfoV4Response);
        ArgumentCaptor<StackImageChangeV4Request> imageChangeRequestArgumentCaptor = ArgumentCaptor.forClass(StackImageChangeV4Request.class);
        ImageChangeDto imageChangeDto = new ImageChangeDto(STACK_ID, imageInfoV4Response.getImageId());
        when(stackCommonService.createImageChangeDto(eq(CLUSTER), eq(WS_ID), imageChangeRequestArgumentCaptor.capture())).thenReturn(imageChangeDto);
        when(stackService.getByNameOrCrnInWorkspace(CLUSTER, WS_ID)).thenReturn(stack);

        Assertions.assertThrows(BadRequestException.class, () -> underTest.triggerUpgrade(CLUSTER, WS_ID, USER_CRN, request));
    }

    @Test
    public void testTriggerFlowWhenReplaceVmsParamIsFalse() {
        // GIVEN
        UpgradeV4Request request = new UpgradeV4Request();
        UpgradeV4Response response = new UpgradeV4Response();
        response.setReplaceVms(false);
        response.setUpgradeCandidates(List.of(mock(ImageInfoV4Response.class)));
        when(upgradeAvailabilityService.checkForUpgrade(CLUSTER, WS_ID, request, USER_CRN)).thenReturn(response);
        when(entitlementService.isInternalRepositoryForUpgradeAllowed(ACCOUNT_ID)).thenReturn(Boolean.FALSE);
        when(clouderaManagerLicenseProvider.getLicense(any())).thenReturn(new JsonCMLicense());
        ImageInfoV4Response imageInfoV4Response = new ImageInfoV4Response();
        imageInfoV4Response.setImageId("imgId");
        imageInfoV4Response.setImageCatalogName("catalogName");
        when(imageSelector.determineImageId(request, response.getUpgradeCandidates())).thenReturn(imageInfoV4Response);
        ArgumentCaptor<StackImageChangeV4Request> imageChangeRequestArgumentCaptor = ArgumentCaptor.forClass(StackImageChangeV4Request.class);
        ImageChangeDto imageChangeDto = new ImageChangeDto(STACK_ID, imageInfoV4Response.getImageId());
        when(stackCommonService.createImageChangeDto(eq(CLUSTER), eq(WS_ID), imageChangeRequestArgumentCaptor.capture())).thenReturn(imageChangeDto);
        when(stackService.getByNameOrCrnInWorkspace(CLUSTER, WS_ID)).thenReturn(stack);
        when(lockedComponentService.isComponentsLocked(stack, imageInfoV4Response.getImageId())).thenReturn(LOCK_COMPONENTS);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW_CHAIN, "asdf");
        when(reactorFlowManager.triggerDistroXUpgrade(eq(STACK_ID), eq(imageChangeDto), anyBoolean(), eq(LOCK_COMPONENTS), anyString()))
                .thenReturn(flowIdentifier);
        // WHEN
        UpgradeV4Response result = underTest.triggerUpgrade(CLUSTER, WS_ID, USER_CRN, request);
        // THEN
        verify(reactorFlowManager).triggerDistroXUpgrade(STACK_ID, imageChangeDto, false, LOCK_COMPONENTS, "variant");
        assertFalse(result.isReplaceVms());
    }

    @Test
    public void testCalculateUpgradeVariantWhenMigrationDisabled() {
        stack.setPlatformVariant("AWS");
        when(entitlementService.awsVariantMigrationEnable(ACCOUNT_ID)).thenReturn(false);
        String actual = underTest.calculateUpgradeVariant(stack, USER_CRN);
        Assertions.assertEquals("AWS", actual);
    }

    @Test
    public void testCalculateUpgradeVariantWhenMigrationEnabledAndVariantIsAws() {
        stack.setPlatformVariant("AWS");
        when(entitlementService.awsVariantMigrationEnable(ACCOUNT_ID)).thenReturn(true);
        String actual = underTest.calculateUpgradeVariant(stack, USER_CRN);
        Assertions.assertEquals("AWS_NATIVE", actual);
    }

    @Test
    public void testCalculateUpgradeVariantWhenMigrationEnabledWhenVariantIsNotAWS() {
        stack.setPlatformVariant("GCP");
        when(entitlementService.awsVariantMigrationEnable(ACCOUNT_ID)).thenReturn(true);
        String actual = underTest.calculateUpgradeVariant(stack, USER_CRN);
        Assertions.assertEquals("GCP", actual);
    }

    private UpgradeV4Request createRequest(boolean osUpgradeEnabled) {
        UpgradeV4Request request = new UpgradeV4Request();
        request.setInternalUpgradeSettings(new InternalUpgradeSettings(true, true, osUpgradeEnabled));
        return request;
    }
}
