package com.sequenceiq.distrox.v1.distrox.service.upgrade;

import static com.sequenceiq.cloudbreak.util.TestConstants.DO_NOT_KEEP_VARIANT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.osupgrade.OrderedOSUpgradeSet;
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
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.StackCommonService;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.StackUpgradeService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeAvailabilityService;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeService;
import com.sequenceiq.cloudbreak.service.upgrade.image.locked.LockedComponentService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
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

    private static final boolean ROLLING_UPGRADE_ENABLED = true;

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
    private StackDtoService stackDtoService;

    @Mock
    private ClouderaManagerLicenseProvider clouderaManagerLicenseProvider;

    @Mock
    private LockedComponentService lockedComponentService;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private StackUpgradeService stackUpgradeService;

    @Mock
    private UpgradeService upgradeService;

    @Mock
    private ClusterUpgradeAvailabilityService clusterUpgradeAvailabilityService;

    @InjectMocks
    private DistroXUpgradeService underTest;

    private StackDto stack;

    @Mock
    private StackView stackView;

    @BeforeEach
    public void setup() {
        stack = mock(StackDto.class);
        lenient().when(stack.getId()).thenReturn(STACK_ID);
        lenient().when(stack.getPlatformVariant()).thenReturn("variant");
        lenient().when(stack.getWorkspaceId()).thenReturn(WS_ID);
        lenient().when(stackView.getId()).thenReturn(STACK_ID);
        lenient().when(stackView.getPlatformVariant()).thenReturn("variant");
        lenient().when(stackView.getWorkspaceId()).thenReturn(WS_ID);
        lenient().when(stack.getStack()).thenReturn(stackView);
        lenient().when(restRequestThreadLocalService.getAccountId()).thenReturn(ACCOUNT_ID);
    }

    @Test
    public void testUpgradeResponseHasReason() {
        UpgradeV4Request request = new UpgradeV4Request();
        when(upgradeAvailabilityService.checkForUpgrade(CLUSTER, WS_ID, request, USER_CRN)).thenReturn(new UpgradeV4Response("reason", null, false));

        Assertions.assertThrows(BadRequestException.class, () -> underTest.triggerUpgrade(CLUSTER, WS_ID, USER_CRN, request, false));
    }

    @Test
    public void testUpgradeResponseHasNoCandidates() {
        UpgradeV4Request request = new UpgradeV4Request();
        when(upgradeAvailabilityService.checkForUpgrade(CLUSTER, WS_ID, request, USER_CRN)).thenReturn(new UpgradeV4Response());

        Assertions.assertThrows(BadRequestException.class, () -> underTest.triggerUpgrade(CLUSTER, WS_ID, USER_CRN, request, false));
    }

    @Test
    public void testCmLicenseMissing() {
        UpgradeV4Request request = new UpgradeV4Request();
        UpgradeV4Response response = new UpgradeV4Response();
        response.setUpgradeCandidates(List.of(mock(ImageInfoV4Response.class)));
        when(upgradeAvailabilityService.checkForUpgrade(CLUSTER, WS_ID, request, USER_CRN)).thenReturn(response);
        when(entitlementService.isInternalRepositoryForUpgradeAllowed(ACCOUNT_ID)).thenReturn(Boolean.FALSE);
        when(clouderaManagerLicenseProvider.getLicense(any())).thenThrow(new BadRequestException("No valid CM license is present"));

        BadRequestException exception = Assertions.assertThrows(BadRequestException.class,
                () -> underTest.triggerUpgrade(CLUSTER, WS_ID, USER_CRN, request, false));
        assertEquals(exception.getMessage(), "No valid CM license is present");
    }

    @Test
    public void testTriggerFlowWithPaywallCheck() {
        UpgradeV4Request request = createRequest(false, ROLLING_UPGRADE_ENABLED);
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
        when(stackDtoService.getByNameOrCrn(CLUSTER, ACCOUNT_ID)).thenReturn(stack);
        when(stackUpgradeService.calculateUpgradeVariant(stackView, USER_CRN, DO_NOT_KEEP_VARIANT)).thenReturn("variant");
        when(lockedComponentService.isComponentsLocked(stack, imageInfoV4Response.getImageId())).thenReturn(LOCK_COMPONENTS);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW_CHAIN, "asdf");
        when(reactorFlowManager.triggerDistroXUpgrade(eq(STACK_ID), eq(imageChangeDto), anyBoolean(), eq(LOCK_COMPONENTS), anyString(),
                eq(ROLLING_UPGRADE_ENABLED))).thenReturn(flowIdentifier);

        UpgradeV4Response result = underTest.triggerUpgrade(CLUSTER, WS_ID, USER_CRN, request, false);

        verify(paywallAccessChecker).checkPaywallAccess(any(), any());
        assertEquals(flowIdentifier, result.getFlowIdentifier());
        assertTrue(result.getReason().contains(imageInfoV4Response.getImageId()));
        StackImageChangeV4Request imageChangeV4Request = imageChangeRequestArgumentCaptor.getValue();
        assertEquals(imageInfoV4Response.getImageId(), imageChangeV4Request.getImageId());
        assertEquals(imageInfoV4Response.getImageCatalogName(), imageChangeV4Request.getImageCatalogName());
    }

    @Test
    public void testTriggerFlowWithoutPaywallCheck() {
        UpgradeV4Request request = createRequest(false, false);
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
        when(stackDtoService.getByNameOrCrn(CLUSTER, ACCOUNT_ID)).thenReturn(stack);
        when(lockedComponentService.isComponentsLocked(stack, imageInfoV4Response.getImageId())).thenReturn(LOCK_COMPONENTS);
        when(stackUpgradeService.calculateUpgradeVariant(stackView, USER_CRN, DO_NOT_KEEP_VARIANT)).thenReturn("variant");
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW_CHAIN, "asdf");
        when(reactorFlowManager.triggerDistroXUpgrade(eq(STACK_ID), eq(imageChangeDto), anyBoolean(), eq(LOCK_COMPONENTS), anyString(), eq(false)))
                .thenReturn(flowIdentifier);

        UpgradeV4Response result = underTest.triggerUpgrade(CLUSTER, WS_ID, USER_CRN, request, false);

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
        UpgradeV4Request request = createRequest(false, ROLLING_UPGRADE_ENABLED);
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
        when(stackDtoService.getByNameOrCrn(CLUSTER, ACCOUNT_ID)).thenReturn(stack);
        when(lockedComponentService.isComponentsLocked(stack, imageInfoV4Response.getImageId())).thenReturn(LOCK_COMPONENTS);
        when(stackUpgradeService.calculateUpgradeVariant(stackView, USER_CRN, DO_NOT_KEEP_VARIANT)).thenReturn("variant");
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW_CHAIN, "asdf");
        when(reactorFlowManager.triggerDistroXUpgrade(eq(STACK_ID), eq(imageChangeDto), anyBoolean(), eq(LOCK_COMPONENTS), anyString(),
                eq(ROLLING_UPGRADE_ENABLED))).thenReturn(flowIdentifier);
        // WHEN
        UpgradeV4Response result = underTest.triggerUpgrade(CLUSTER, WS_ID, USER_CRN, request, false);
        // THEN
        verify(reactorFlowManager).triggerDistroXUpgrade(STACK_ID, imageChangeDto, false, LOCK_COMPONENTS, "variant", ROLLING_UPGRADE_ENABLED);
        assertFalse(result.isReplaceVms());
    }

    @Test
    public void testTriggerFlowWithoutTurnOffReplaceVmsParam() {
        // GIVEN
        UpgradeV4Request request = createRequest(true, false);
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
        when(stackDtoService.getByNameOrCrn(CLUSTER, ACCOUNT_ID)).thenReturn(stack);
        when(stackUpgradeService.calculateUpgradeVariant(stackView, USER_CRN, DO_NOT_KEEP_VARIANT)).thenReturn("variant");
        when(lockedComponentService.isComponentsLocked(stack, imageInfoV4Response.getImageId())).thenReturn(true);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW_CHAIN, "asdf");
        when(reactorFlowManager.triggerDistroXUpgrade(eq(STACK_ID), eq(imageChangeDto), anyBoolean(), anyBoolean(), anyString(), eq(false)))
                .thenReturn(flowIdentifier);
        // WHEN
        UpgradeV4Response result = underTest.triggerUpgrade(CLUSTER, WS_ID, USER_CRN, request, false);
        // THEN
        verify(reactorFlowManager).triggerDistroXUpgrade(STACK_ID, imageChangeDto, true, true, "variant", false);
        assertTrue(result.isReplaceVms());
    }

    @Test
    public void testTriggerFlowWhenReplaceVmsParamIsFalse() {
        // GIVEN
        UpgradeV4Request request = createRequest(false, ROLLING_UPGRADE_ENABLED);
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
        when(stackDtoService.getByNameOrCrn(CLUSTER, ACCOUNT_ID)).thenReturn(stack);
        when(stackUpgradeService.calculateUpgradeVariant(stackView, USER_CRN, DO_NOT_KEEP_VARIANT)).thenReturn("variant");
        when(lockedComponentService.isComponentsLocked(stack, imageInfoV4Response.getImageId())).thenReturn(LOCK_COMPONENTS);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW_CHAIN, "asdf");
        when(reactorFlowManager.triggerDistroXUpgrade(eq(STACK_ID), eq(imageChangeDto), anyBoolean(), eq(LOCK_COMPONENTS), anyString(),
                eq(ROLLING_UPGRADE_ENABLED))).thenReturn(flowIdentifier);
        // WHEN
        UpgradeV4Response result = underTest.triggerUpgrade(CLUSTER, WS_ID, USER_CRN, request, false);
        // THEN
        verify(reactorFlowManager).triggerDistroXUpgrade(STACK_ID, imageChangeDto, false, LOCK_COMPONENTS, "variant", ROLLING_UPGRADE_ENABLED);
        assertFalse(result.isReplaceVms());
    }

    @Test
    public void testTriggerOsUpgradeByUpgradeSets() {
        Stack stack = new Stack();
        stack.setResourceCrn("crn:cdp:datalake:us-west-1:tenant:datalake:resourceCrn1");
        Workspace workspace = new Workspace();
        workspace.setId(WS_ID);
        stack.setWorkspace(workspace);
        when(stackService.getByNameOrCrnInWorkspace(CLUSTER, WS_ID)).thenReturn(stack);
        UpgradeV4Response response = new UpgradeV4Response();
        response.setReplaceVms(true);
        response.setUpgradeCandidates(List.of(mock(ImageInfoV4Response.class)));
        when(clusterUpgradeAvailabilityService.checkForUpgrades(eq(stack), eq(true), any(), eq(true))).thenReturn(response);
        ImageInfoV4Response imageInfoV4Response = new ImageInfoV4Response();
        imageInfoV4Response.setImageId("imageID");
        imageInfoV4Response.setImageCatalogName("catalogName");
        when(imageSelector.determineImage(eq(Optional.of("imageID")), eq(response.getUpgradeCandidates()))).thenReturn(imageInfoV4Response);
        List<OrderedOSUpgradeSet> upgradeSets = List.of(new OrderedOSUpgradeSet(0, Set.of("i-1", "i-2")),
                new OrderedOSUpgradeSet(1, Set.of("i-3", "i-4")));
        ArgumentCaptor<StackImageChangeV4Request> imageChangeRequestArgumentCaptor = ArgumentCaptor.forClass(StackImageChangeV4Request.class);
        ImageChangeDto imageChangeDto = new ImageChangeDto(STACK_ID, imageInfoV4Response.getImageId());
        when(stackCommonService.createImageChangeDto(eq(CLUSTER), eq(WS_ID), imageChangeRequestArgumentCaptor.capture())).thenReturn(imageChangeDto);
        underTest.triggerOsUpgradeByUpgradeSets(CLUSTER, WS_ID, "imageID", upgradeSets);
        ArgumentCaptor<InternalUpgradeSettings> internalUpgradeSettingsArgumentCaptor = ArgumentCaptor.forClass(InternalUpgradeSettings.class);
        verify(clusterUpgradeAvailabilityService, times(1)).checkForUpgrades(eq(stack), eq(true),
                internalUpgradeSettingsArgumentCaptor.capture(), eq(true));
        assertFalse(internalUpgradeSettingsArgumentCaptor.getValue().isSkipValidations());
        ArgumentCaptor<ImageChangeDto> imageChangeDtoCaptor = ArgumentCaptor.forClass(ImageChangeDto.class);
        verify(upgradeService, times(1)).upgradeOsByUpgradeSets(eq(stack), imageChangeDtoCaptor.capture(), eq(upgradeSets));
        assertEquals("imageID", imageChangeDtoCaptor.getValue().getImageId());
        assertEquals("imageID", imageChangeRequestArgumentCaptor.getValue().getImageId());
    }

    private UpgradeV4Request createRequest(boolean osUpgradeEnabled, boolean rollingUpgradeEnabled) {
        UpgradeV4Request request = new UpgradeV4Request();
        request.setInternalUpgradeSettings(new InternalUpgradeSettings(true, true, osUpgradeEnabled, rollingUpgradeEnabled));
        return request;
    }
}
