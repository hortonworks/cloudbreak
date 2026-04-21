package com.sequenceiq.distrox.v1.distrox.service.upgrade;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.doAs;
import static com.sequenceiq.cloudbreak.util.TestConstants.DO_NOT_KEEP_VARIANT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackImageChangeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.osupgrade.OrderedOSUpgradeSet;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageComponentVersions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeReinitiableV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.ClouderaManagerLicenseProvider;
import com.sequenceiq.cloudbreak.auth.JsonCMLicense;
import com.sequenceiq.cloudbreak.auth.PaywallAccessChecker;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.StackCommonService;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.StackUpgradeService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeAvailabilityService;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeReinitiateService;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeService;
import com.sequenceiq.cloudbreak.tag.ClusterTemplateApplicationTag;
import com.sequenceiq.cloudbreak.util.CodUtil;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.model.UpgradeShowAvailableImages;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeShowAvailableImages;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Response;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.reinit.DistroXUpgradeReinitiableV1Response;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.reinit.UpgradeReinitiateStatus;
import com.sequenceiq.distrox.v1.distrox.converter.UpgradeConverter;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
class DistroXUpgradeServiceTest {

    private static final String ACCOUNT_ID = "9d74eee4-1cad-45d7-b645-7ccf9edbb73d";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":user:f3b8ed82-e712-4f89-bda7-be07183720d3";

    private static final String RESOURCE_CRN = "crn:cdp:datahub:us-west-1:" + ACCOUNT_ID + ":cluster:f3b8ed82-e712-4f89-bda7-be07183720d3";

    private static final NameOrCrn CLUSTER = NameOrCrn.ofName("cluster");

    private static final Long WS_ID = 1L;

    private static final Long STACK_ID = 3L;

    private static final boolean LOCK_COMPONENTS = false;

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
    private StackUpgradeService stackUpgradeService;

    @Mock
    private UpgradeService upgradeService;

    @Mock
    private ClusterUpgradeAvailabilityService clusterUpgradeAvailabilityService;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private UpgradeConverter upgradeConverter;

    @Mock
    private UpgradeReinitiateService upgradeReinitiateService;

    @InjectMocks
    private DistroXUpgradeService underTest;

    @Mock
    private StackDto stack;

    @Mock
    private ClusterView clusterView;

    @Mock
    private StackView stackView;

    @BeforeEach
    void setup() {
        lenient().when(stack.getId()).thenReturn(STACK_ID);
        lenient().when(stack.getPlatformVariant()).thenReturn("variant");
        lenient().when(stack.getWorkspaceId()).thenReturn(WS_ID);
        lenient().when(stackView.getId()).thenReturn(STACK_ID);
        lenient().when(stackView.getPlatformVariant()).thenReturn("variant");
        lenient().when(stackView.getWorkspaceId()).thenReturn(WS_ID);
        lenient().when(stack.getStack()).thenReturn(stackView);
        lenient().when(stack.getCluster()).thenReturn(clusterView);
        lenient().when(clusterView.getId()).thenReturn(STACK_ID);
        lenient().when(stackView.getResourceCrn()).thenReturn(RESOURCE_CRN);
        lenient().when(stack.getResourceCrn()).thenReturn(RESOURCE_CRN);
        lenient().when(upgradeConverter.convert(any(UpgradeV4Response.class))).thenCallRealMethod();
        lenient().when(upgradeConverter.convert(any(DistroXUpgradeV1Request.class), anyBoolean())).thenCallRealMethod();
    }

    @Test
    void testUpgradeResponseHasReason() {
        UpgradeV4Request request = new UpgradeV4Request();
        when(upgradeAvailabilityService.checkForUpgrade(CLUSTER, WS_ID, request, USER_CRN)).thenReturn(new UpgradeV4Response("reason", null, false));

        assertThrows(BadRequestException.class, () -> doAs(USER_CRN, () -> underTest.upgradeCluster(request, CLUSTER, false, WS_ID)));
    }

    @Test
    void testUpgradeResponseHasNoCandidates() {
        UpgradeV4Request request = new UpgradeV4Request();
        when(upgradeAvailabilityService.checkForUpgrade(CLUSTER, WS_ID, request, USER_CRN)).thenReturn(new UpgradeV4Response());

        assertThrows(BadRequestException.class, () -> doAs(USER_CRN, () -> underTest.upgradeCluster(request, CLUSTER, false, WS_ID)));
    }

    @Test
    void testCmLicenseMissing() {
        UpgradeV4Request request = new UpgradeV4Request();
        UpgradeV4Response response = new UpgradeV4Response();
        response.setUpgradeCandidates(List.of(mock(ImageInfoV4Response.class)));
        when(upgradeAvailabilityService.checkForUpgrade(CLUSTER, WS_ID, request, USER_CRN)).thenReturn(response);
        when(entitlementService.isInternalRepositoryForUpgradeAllowed(ACCOUNT_ID)).thenReturn(Boolean.FALSE);
        when(clouderaManagerLicenseProvider.getLicense(any())).thenThrow(new BadRequestException("No valid CM license is present"));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                doAs(USER_CRN, () -> underTest.upgradeCluster(request, CLUSTER, false, WS_ID)));
        assertEquals(exception.getMessage(), "No valid CM license is present");
    }

    @Test
    void testTriggerFlowWithPaywallCheck() {
        UpgradeV4Request request = createRequest(false, ROLLING_UPGRADE_ENABLED);
        UpgradeV4Response response = new UpgradeV4Response();
        response.setUpgradeCandidates(List.of(mock(ImageInfoV4Response.class)));
        when(upgradeAvailabilityService.checkForUpgrade(CLUSTER, WS_ID, request, USER_CRN)).thenReturn(response);
        when(entitlementService.isInternalRepositoryForUpgradeAllowed(ACCOUNT_ID)).thenReturn(Boolean.FALSE);
        when(clouderaManagerLicenseProvider.getLicense(any())).thenReturn(new JsonCMLicense());
        ImageInfoV4Response imageInfoV4Response = getImage();
        when(imageSelector.determineImageId(request, response)).thenReturn(imageInfoV4Response);
        ArgumentCaptor<StackImageChangeV4Request> imageChangeRequestArgumentCaptor = ArgumentCaptor.forClass(StackImageChangeV4Request.class);
        ImageChangeDto imageChangeDto = new ImageChangeDto(STACK_ID, imageInfoV4Response.getImageId());
        when(stackCommonService.createImageChangeDto(eq(CLUSTER), eq(WS_ID), imageChangeRequestArgumentCaptor.capture()))
                .thenReturn(imageChangeDto);
        when(stackDtoService.getByNameOrCrn(CLUSTER, ACCOUNT_ID)).thenReturn(stack);
        when(stackUpgradeService.calculateUpgradeVariant(stackView, USER_CRN, DO_NOT_KEEP_VARIANT)).thenReturn("variant");
        when(clusterUpgradeAvailabilityService.determineLockComponentsParam(any(), any(), eq(stack))).thenReturn(LOCK_COMPONENTS);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW_CHAIN, "asdf");
        when(reactorFlowManager.triggerDistroXUpgrade(eq(STACK_ID), eq(imageChangeDto), anyBoolean(), eq(LOCK_COMPONENTS), anyString(),
                eq(ROLLING_UPGRADE_ENABLED), eq("aRuntime"))).thenReturn(flowIdentifier);

        DistroXUpgradeV1Response result = doAs(USER_CRN, () -> underTest.upgradeCluster(request, CLUSTER, false, WS_ID));

        verify(paywallAccessChecker).checkPaywallAccess(any(), any());
        assertEquals(flowIdentifier, result.flowIdentifier());
        assertTrue(result.reason().contains(imageInfoV4Response.getImageId()));
        StackImageChangeV4Request imageChangeV4Request = imageChangeRequestArgumentCaptor.getValue();
        assertEquals(imageInfoV4Response.getImageId(), imageChangeV4Request.getImageId());
        assertEquals(imageInfoV4Response.getImageCatalogName(), imageChangeV4Request.getImageCatalogName());
    }

    @Test
    void testTriggerFlowWithoutPaywallCheck() {
        UpgradeV4Request request = createRequest(false, false);
        UpgradeV4Response response = new UpgradeV4Response();
        response.setUpgradeCandidates(List.of(mock(ImageInfoV4Response.class)));
        when(upgradeAvailabilityService.checkForUpgrade(CLUSTER, WS_ID, request, USER_CRN)).thenReturn(response);
        when(entitlementService.isInternalRepositoryForUpgradeAllowed(ACCOUNT_ID)).thenReturn(Boolean.TRUE);
        ImageInfoV4Response imageInfoV4Response = getImage();
        when(imageSelector.determineImageId(request, response)).thenReturn(imageInfoV4Response);
        ArgumentCaptor<StackImageChangeV4Request> imageChangeRequestArgumentCaptor = ArgumentCaptor.forClass(StackImageChangeV4Request.class);
        ImageChangeDto imageChangeDto = new ImageChangeDto(STACK_ID, imageInfoV4Response.getImageId());
        when(stackCommonService.createImageChangeDto(eq(CLUSTER), eq(WS_ID), imageChangeRequestArgumentCaptor.capture()))
                .thenReturn(imageChangeDto);
        when(stackDtoService.getByNameOrCrn(CLUSTER, ACCOUNT_ID)).thenReturn(stack);
        when(clusterUpgradeAvailabilityService.determineLockComponentsParam(any(), any(), eq(stack))).thenReturn(LOCK_COMPONENTS);
        when(stackUpgradeService.calculateUpgradeVariant(stackView, USER_CRN, DO_NOT_KEEP_VARIANT)).thenReturn("variant");
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW_CHAIN, "asdf");
        when(reactorFlowManager.triggerDistroXUpgrade(eq(STACK_ID), eq(imageChangeDto), anyBoolean(), eq(LOCK_COMPONENTS), anyString(), eq(false),
                eq("aRuntime"))).thenReturn(flowIdentifier);

        DistroXUpgradeV1Response result = doAs(USER_CRN, () -> underTest.upgradeCluster(request, CLUSTER, false, WS_ID));

        verifyNoInteractions(paywallAccessChecker);
        assertEquals(flowIdentifier, result.flowIdentifier());
        assertTrue(result.reason().contains(imageInfoV4Response.getImageId()));
        StackImageChangeV4Request imageChangeV4Request = imageChangeRequestArgumentCaptor.getValue();
        assertEquals(imageInfoV4Response.getImageId(), imageChangeV4Request.getImageId());
        assertEquals(imageInfoV4Response.getImageCatalogName(), imageChangeV4Request.getImageCatalogName());
    }

    @Test
    void testTriggerFlowWithTurnOffReplaceVmsParam() {
        // GIVEN
        UpgradeV4Request request = createRequest(false, ROLLING_UPGRADE_ENABLED);
        UpgradeV4Response response = new UpgradeV4Response();
        response.setReplaceVms(true);
        response.setUpgradeCandidates(List.of(mock(ImageInfoV4Response.class)));
        when(upgradeAvailabilityService.checkForUpgrade(CLUSTER, WS_ID, request, USER_CRN)).thenReturn(response);
        when(entitlementService.isInternalRepositoryForUpgradeAllowed(ACCOUNT_ID)).thenReturn(Boolean.FALSE);
        when(clouderaManagerLicenseProvider.getLicense(any())).thenReturn(new JsonCMLicense());
        ImageInfoV4Response imageInfoV4Response = getImage();
        when(imageSelector.determineImageId(request, response)).thenReturn(imageInfoV4Response);
        ArgumentCaptor<StackImageChangeV4Request> imageChangeRequestArgumentCaptor = ArgumentCaptor.forClass(StackImageChangeV4Request.class);
        ImageChangeDto imageChangeDto = new ImageChangeDto(STACK_ID, imageInfoV4Response.getImageId());
        when(stackCommonService.createImageChangeDto(eq(CLUSTER), eq(WS_ID), imageChangeRequestArgumentCaptor.capture())).thenReturn(imageChangeDto);
        when(stackDtoService.getByNameOrCrn(CLUSTER, ACCOUNT_ID)).thenReturn(stack);
        when(clusterUpgradeAvailabilityService.determineLockComponentsParam(any(), any(), eq(stack))).thenReturn(LOCK_COMPONENTS);
        when(stackUpgradeService.calculateUpgradeVariant(stackView, USER_CRN, DO_NOT_KEEP_VARIANT)).thenReturn("variant");
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW_CHAIN, "asdf");
        when(reactorFlowManager.triggerDistroXUpgrade(eq(STACK_ID), eq(imageChangeDto), anyBoolean(), eq(LOCK_COMPONENTS), anyString(),
                eq(ROLLING_UPGRADE_ENABLED), eq("aRuntime"))).thenReturn(flowIdentifier);
        // WHEN
        DistroXUpgradeV1Response result = doAs(USER_CRN, () -> underTest.upgradeCluster(request, CLUSTER, false, WS_ID));
        // THEN
        verify(reactorFlowManager).triggerDistroXUpgrade(STACK_ID, imageChangeDto, false, LOCK_COMPONENTS, "variant", ROLLING_UPGRADE_ENABLED, "aRuntime");
    }

    @Test
    void testTriggerFlowWhenOsUpgradeFalseReplaceVmsParamTrue() {
        // GIVEN
        UpgradeV4Request request = createRequest(false, ROLLING_UPGRADE_ENABLED);
        UpgradeV4Response response = new UpgradeV4Response();
        response.setReplaceVms(true);
        response.setUpgradeCandidates(List.of(mock(ImageInfoV4Response.class)));
        when(upgradeAvailabilityService.checkForUpgrade(CLUSTER, WS_ID, request, USER_CRN)).thenReturn(response);
        when(entitlementService.isInternalRepositoryForUpgradeAllowed(ACCOUNT_ID)).thenReturn(Boolean.FALSE);
        when(clouderaManagerLicenseProvider.getLicense(any())).thenReturn(new JsonCMLicense());
        when(clusterUpgradeAvailabilityService.determineLockComponentsParam(eq(request), any(), eq(stack))).thenReturn(LOCK_COMPONENTS);
        when(clusterUpgradeAvailabilityService.determineReplaceVmsParameter(eq(stack), isNull(), eq(false), eq(false))).thenReturn(Boolean.TRUE);
        ImageInfoV4Response imageInfoV4Response = getImage();
        when(imageSelector.determineImageId(request, response)).thenReturn(imageInfoV4Response);
        ArgumentCaptor<StackImageChangeV4Request> imageChangeRequestArgumentCaptor = ArgumentCaptor.forClass(StackImageChangeV4Request.class);
        ImageChangeDto imageChangeDto = new ImageChangeDto(STACK_ID, imageInfoV4Response.getImageId());
        when(stackCommonService.createImageChangeDto(eq(CLUSTER), eq(WS_ID), imageChangeRequestArgumentCaptor.capture())).thenReturn(imageChangeDto);
        when(stackDtoService.getByNameOrCrn(CLUSTER, ACCOUNT_ID)).thenReturn(stack);
        when(stackUpgradeService.calculateUpgradeVariant(stackView, USER_CRN, DO_NOT_KEEP_VARIANT)).thenReturn("variant");
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW_CHAIN, "asdf");
        when(reactorFlowManager.triggerDistroXUpgrade(eq(STACK_ID), eq(imageChangeDto), anyBoolean(), eq(LOCK_COMPONENTS), anyString(),
                eq(ROLLING_UPGRADE_ENABLED), eq("aRuntime"))).thenReturn(flowIdentifier);
        // WHEN
        DistroXUpgradeV1Response result = doAs(USER_CRN, () -> underTest.upgradeCluster(request, CLUSTER, false, WS_ID));
        // THEN
        verify(reactorFlowManager).triggerDistroXUpgrade(STACK_ID, imageChangeDto, true, LOCK_COMPONENTS, "variant", ROLLING_UPGRADE_ENABLED, "aRuntime");
    }

    @Test
    void testTriggerFlowWhenReplaceVmsParamAndLockComponentsEnabled() {
        // GIVEN
        UpgradeV4Request request = createRequest(true, false);
        request.setReplaceVms(Boolean.TRUE);
        UpgradeV4Response response = new UpgradeV4Response();
        response.setReplaceVms(true);
        response.setUpgradeCandidates(List.of(mock(ImageInfoV4Response.class)));
        when(upgradeAvailabilityService.checkForUpgrade(CLUSTER, WS_ID, request, USER_CRN)).thenReturn(response);
        when(entitlementService.isInternalRepositoryForUpgradeAllowed(ACCOUNT_ID)).thenReturn(Boolean.FALSE);
        when(clouderaManagerLicenseProvider.getLicense(any())).thenReturn(new JsonCMLicense());
        ImageInfoV4Response imageInfoV4Response = getImage();
        when(imageSelector.determineImageId(request, response)).thenReturn(imageInfoV4Response);
        ArgumentCaptor<StackImageChangeV4Request> imageChangeRequestArgumentCaptor = ArgumentCaptor.forClass(StackImageChangeV4Request.class);
        ImageChangeDto imageChangeDto = new ImageChangeDto(STACK_ID, imageInfoV4Response.getImageId());
        when(stackCommonService.createImageChangeDto(eq(CLUSTER), eq(WS_ID), imageChangeRequestArgumentCaptor.capture())).thenReturn(imageChangeDto);
        when(stackDtoService.getByNameOrCrn(CLUSTER, ACCOUNT_ID)).thenReturn(stack);
        when(stackUpgradeService.calculateUpgradeVariant(stackView, USER_CRN, DO_NOT_KEEP_VARIANT)).thenReturn("variant");
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW_CHAIN, "asdf");
        when(reactorFlowManager.triggerDistroXUpgrade(eq(STACK_ID), eq(imageChangeDto), anyBoolean(), anyBoolean(), anyString(), eq(false), eq("aRuntime")))
                .thenReturn(flowIdentifier);
        when(clusterUpgradeAvailabilityService.determineLockComponentsParam(eq(request), any(), eq(stack))).thenReturn(Boolean.TRUE);
        when(clusterUpgradeAvailabilityService.determineReplaceVmsParameter(eq(stack), eq(true), eq(true), eq(false))).thenReturn(Boolean.TRUE);
        // WHEN
        DistroXUpgradeV1Response result = doAs(USER_CRN, () -> underTest.upgradeCluster(request, CLUSTER, false, WS_ID));
        // THEN
        verify(reactorFlowManager).triggerDistroXUpgrade(STACK_ID, imageChangeDto, true, true, "variant", false, "aRuntime");
    }

    @Test
    void testTriggerFlowWhenReplaceVmsParamIsFalse() {
        // GIVEN
        UpgradeV4Request request = createRequest(false, ROLLING_UPGRADE_ENABLED);
        UpgradeV4Response response = new UpgradeV4Response();
        response.setReplaceVms(false);
        response.setUpgradeCandidates(List.of(mock(ImageInfoV4Response.class)));
        when(upgradeAvailabilityService.checkForUpgrade(CLUSTER, WS_ID, request, USER_CRN)).thenReturn(response);
        when(entitlementService.isInternalRepositoryForUpgradeAllowed(ACCOUNT_ID)).thenReturn(Boolean.FALSE);
        when(clouderaManagerLicenseProvider.getLicense(any())).thenReturn(new JsonCMLicense());
        ImageInfoV4Response imageInfoV4Response = getImage();
        when(imageSelector.determineImageId(request, response)).thenReturn(imageInfoV4Response);
        ArgumentCaptor<StackImageChangeV4Request> imageChangeRequestArgumentCaptor = ArgumentCaptor.forClass(StackImageChangeV4Request.class);
        ImageChangeDto imageChangeDto = new ImageChangeDto(STACK_ID, imageInfoV4Response.getImageId());
        when(stackCommonService.createImageChangeDto(eq(CLUSTER), eq(WS_ID), imageChangeRequestArgumentCaptor.capture())).thenReturn(imageChangeDto);
        when(stackDtoService.getByNameOrCrn(CLUSTER, ACCOUNT_ID)).thenReturn(stack);
        when(stackUpgradeService.calculateUpgradeVariant(stackView, USER_CRN, DO_NOT_KEEP_VARIANT)).thenReturn("variant");
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW_CHAIN, "asdf");
        when(reactorFlowManager.triggerDistroXUpgrade(eq(STACK_ID), eq(imageChangeDto), anyBoolean(), eq(LOCK_COMPONENTS), anyString(),
                eq(ROLLING_UPGRADE_ENABLED), eq("aRuntime"))).thenReturn(flowIdentifier);
        // WHEN
        DistroXUpgradeV1Response result = doAs(USER_CRN, () -> underTest.upgradeCluster(request, CLUSTER, false, WS_ID));
        // THEN
        verify(reactorFlowManager).triggerDistroXUpgrade(STACK_ID, imageChangeDto, false, LOCK_COMPONENTS, "variant", ROLLING_UPGRADE_ENABLED, "aRuntime");
    }

    @Test
    void testTriggerOsUpgradeByUpgradeSets() {
        Stack stack = new Stack();
        stack.setResourceCrn("crn:cdp:datalake:us-west-1:tenant:datalake:resourceCrn1");
        Workspace workspace = new Workspace();
        workspace.setId(WS_ID);
        stack.setWorkspace(workspace);
        when(stackService.getByNameOrCrnInWorkspace(CLUSTER, WS_ID)).thenReturn(stack);
        UpgradeV4Response response = new UpgradeV4Response();
        response.setReplaceVms(true);
        response.setUpgradeCandidates(List.of(mock(ImageInfoV4Response.class)));
        when(clusterUpgradeAvailabilityService.checkForUpgrades(eq(stack), eq(true), eq(true), any(), eq(true), any())).thenReturn(response);
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
        verify(clusterUpgradeAvailabilityService, times(1)).checkForUpgrades(eq(stack), eq(true), eq(true),
                internalUpgradeSettingsArgumentCaptor.capture(), eq(true), eq("imageID"));
        assertFalse(internalUpgradeSettingsArgumentCaptor.getValue().isSkipValidations());
        ArgumentCaptor<ImageChangeDto> imageChangeDtoCaptor = ArgumentCaptor.forClass(ImageChangeDto.class);
        verify(upgradeService, times(1)).upgradeOsByUpgradeSets(eq(stack), imageChangeDtoCaptor.capture(), eq(upgradeSets));
        assertEquals("imageID", imageChangeDtoCaptor.getValue().getImageId());
        assertEquals("imageID", imageChangeRequestArgumentCaptor.getValue().getImageId());
    }

    @Test
    void testGracefulStopServiceIsNotSupportedWhenStackTypeIsDataLake() {
        when(stack.getType()).thenReturn(StackType.DATALAKE);

        assertFalse(underTest.isGracefulStopServicesNeeded(stack));
    }

    @Test
    void testGracefulStopServiceIsNotSupportedBefore7218() {
        when(stack.getType()).thenReturn(StackType.WORKLOAD);
        ClouderaManagerProduct clouderaManagerProduct = new ClouderaManagerProduct();
        clouderaManagerProduct.setVersion("7.2.17");
        when(clusterComponentConfigProvider.getCdhProduct(any())).thenReturn(Optional.of(clouderaManagerProduct));

        assertFalse(underTest.isGracefulStopServicesNeeded(stack));
    }

    @Test
    void testGracefulStopServiceIsNotSupportedBefore7218WhenVersionIsPatchVersion() {
        when(stack.getType()).thenReturn(StackType.WORKLOAD);
        ClouderaManagerProduct clouderaManagerProduct = new ClouderaManagerProduct();
        clouderaManagerProduct.setVersion("7.2.17-1.cdh7.2.18.p0.49779745");
        when(clusterComponentConfigProvider.getCdhProduct(any())).thenReturn(Optional.of(clouderaManagerProduct));

        assertFalse(underTest.isGracefulStopServicesNeeded(stack));
    }

    @Test
    void testGracefulStopServiceIsSupportedAfter7218() {
        when(stack.getType()).thenReturn(StackType.WORKLOAD);
        ClouderaManagerProduct clouderaManagerProduct = new ClouderaManagerProduct();
        clouderaManagerProduct.setVersion("7.2.18");
        when(clusterComponentConfigProvider.getCdhProduct(any())).thenReturn(Optional.of(clouderaManagerProduct));

        assertTrue(underTest.isGracefulStopServicesNeeded(stack));
    }

    @Test
    void testGracefulStopServiceIsSupportedAfter7218WhenVersionIsPatchVersion() {
        when(stack.getType()).thenReturn(StackType.WORKLOAD);
        ClouderaManagerProduct clouderaManagerProduct = new ClouderaManagerProduct();
        clouderaManagerProduct.setVersion("7.2.18-1.cdh7.2.18.p0.49779745");
        when(clusterComponentConfigProvider.getCdhProduct(any())).thenReturn(Optional.of(clouderaManagerProduct));

        assertTrue(underTest.isGracefulStopServicesNeeded(stack));
    }

    static Stream<Arguments> testUpgradeClusterOnlyCheckArguments() {
        return Stream.of(
                Arguments.of(true, UpgradeShowAvailableImages.SHOW),
                Arguments.of(true, UpgradeShowAvailableImages.LATEST_ONLY),
                Arguments.of(true, UpgradeShowAvailableImages.DO_NOT_SHOW),
                Arguments.of(false, UpgradeShowAvailableImages.SHOW),
                Arguments.of(false, UpgradeShowAvailableImages.LATEST_ONLY)
        );
    }

    @MethodSource("testUpgradeClusterOnlyCheckArguments")
    @ParameterizedTest
    void testUpgradeClusterOnlyCheck(boolean dryRun, UpgradeShowAvailableImages showAvailableImages) {
        UpgradeV4Request upgradeV4Request = new UpgradeV4Request();
        upgradeV4Request.setDryRun(dryRun);
        upgradeV4Request.setShowAvailableImages(showAvailableImages);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, "asdf");
        UpgradeV4Response upgradeV4Response = new UpgradeV4Response("reason", flowIdentifier, false);
        when(upgradeAvailabilityService.checkForUpgrade(CLUSTER, WS_ID, upgradeV4Request, USER_CRN)).thenReturn(upgradeV4Response);

        DistroXUpgradeV1Response result = doAs(USER_CRN, () -> underTest.upgradeCluster(upgradeV4Request, CLUSTER, false, WS_ID));

        assertEquals(flowIdentifier, result.flowIdentifier());
        assertEquals("reason", result.reason());
    }

    @Test
    void testValidateCodCluster() {
        Stack stack = new Stack();
        StackTags stackTags = new StackTags(Map.of(), Map.of(ClusterTemplateApplicationTag.SERVICE_TYPE.key(), CodUtil.OPERATIONAL_DB), Map.of());
        stack.setTags(new Json(stackTags));
        when(stackService.getByNameOrCrnInWorkspace(any(), anyLong())).thenReturn(stack);

        assertThrows(BadRequestException.class, () -> underTest.validateCodCluster(CLUSTER, DistroXUpgradeShowAvailableImages.DO_NOT_SHOW, WS_ID),
                "Please note that COD cluster upgrades are supported only through the Operational Database UI or CLI!");
    }

    @Test
    void testCheckClusterUpgradeReinitiable() {
        when(stackService.getIdByNameOrCrnInWorkspace(CLUSTER, WS_ID)).thenReturn(STACK_ID);
        when(upgradeReinitiateService.checkClusterUpgradeReinitiable(STACK_ID)).thenReturn(getUpgradeReinitiableV4Response());

        DistroXUpgradeReinitiableV1Response result = underTest.checkClusterUpgradeReinitiable(CLUSTER, WS_ID);

        assertEquals(UpgradeReinitiateStatus.NON_REINITIABLE, result.status());
        assertEquals("There were no upgrades for this cluster, therefore upgrade reinitiation is not needed.", result.reason());
    }

    @Test
    void testReinitiateClusterUpgradeWhenNotReinitiable() {
        when(stackService.getIdByNameOrCrnInWorkspace(CLUSTER, WS_ID)).thenReturn(STACK_ID);
        when(upgradeReinitiateService.checkClusterUpgradeReinitiable(STACK_ID)).thenReturn(getUpgradeReinitiableV4Response());

        assertThrows(BadRequestException.class, () -> underTest.reinitiateClusterUpgrade(CLUSTER, WS_ID),
                "Cluster upgrade cannot be reinitiated: There were no upgrades for this cluster, therefore upgrade reinitiation is not needed.");

        verify(upgradeReinitiateService, never()).tryRetrieveLastDistroxUpgradeV1Request(anyLong());
        verifyNoInteractions(upgradeAvailabilityService);
        verifyNoInteractions(reactorFlowManager);
    }

    @Test
    void testReinitiateClusterUpgradeWhenReinitiable() {
        when(stackService.getIdByNameOrCrnInWorkspace(CLUSTER, WS_ID)).thenReturn(STACK_ID);
        when(upgradeReinitiateService.checkClusterUpgradeReinitiable(STACK_ID)).thenReturn(getUpgradeReinitiableV4Response(
                UpgradeReinitiateStatus.REINITIABLE,
                "The last upgrade for this cluster finished with a failure, therefore the cluster is eligible for upgrade reinitiation."
        ));
        when(upgradeReinitiateService.tryRetrieveLastDistroxUpgradeV1Request(STACK_ID)).thenReturn(Optional.of(new DistroXUpgradeV1Request()));
        UpgradeV4Response upgradeV4Response = new UpgradeV4Response();
        ImageInfoV4Response imageInfoV4Response = new ImageInfoV4Response();
        imageInfoV4Response.setComponentVersions(new ImageComponentVersions());
        upgradeV4Response.setUpgradeCandidates(List.of(imageInfoV4Response));
        when(upgradeAvailabilityService.checkForUpgrade(eq(CLUSTER), eq(WS_ID), any(UpgradeV4Request.class), eq(USER_CRN))).thenReturn(upgradeV4Response);
        when(imageSelector.determineImageId(any(UpgradeV4Request.class), eq(upgradeV4Response))).thenReturn(imageInfoV4Response);
        when(stackCommonService.createImageChangeDto(eq(CLUSTER), eq(WS_ID), any(StackImageChangeV4Request.class)))
                .thenReturn(new ImageChangeDto(STACK_ID, imageInfoV4Response.getImageId()));
        StackDto stackDto = mock();
        when(stackDto.getStack()).thenReturn(new Stack());
        when(stackDtoService.getByNameOrCrn(CLUSTER, ACCOUNT_ID)).thenReturn(stackDto);

        DistroXUpgradeV1Response result = doAs(USER_CRN, () -> underTest.reinitiateClusterUpgrade(CLUSTER, WS_ID));

        verify(reactorFlowManager).triggerDistroXUpgrade(any(), any(ImageChangeDto.class), anyBoolean(), anyBoolean(), any(), anyBoolean(), any());
    }

    private UpgradeV4Request createRequest(boolean osUpgradeEnabled, boolean rollingUpgradeEnabled) {
        UpgradeV4Request request = new UpgradeV4Request();
        request.setInternalUpgradeSettings(new InternalUpgradeSettings(true, osUpgradeEnabled, rollingUpgradeEnabled));
        return request;
    }

    private ImageInfoV4Response getImage() {
        ImageInfoV4Response imageInfoV4Response = new ImageInfoV4Response();
        imageInfoV4Response.setImageId("imgId");
        imageInfoV4Response.setImageCatalogName("catalogName");
        ImageComponentVersions imageComponentVersions = new ImageComponentVersions();
        imageComponentVersions.setCdp("aRuntime");
        imageInfoV4Response.setComponentVersions(imageComponentVersions);
        return imageInfoV4Response;
    }

    private static UpgradeReinitiableV4Response getUpgradeReinitiableV4Response(UpgradeReinitiateStatus status, String reason) {
        return new UpgradeReinitiableV4Response(status, reason);
    }

    private static UpgradeReinitiableV4Response getUpgradeReinitiableV4Response() {
        return getUpgradeReinitiableV4Response(
                UpgradeReinitiateStatus.NON_REINITIABLE,
                "There were no upgrades for this cluster, therefore upgrade reinitiation is not needed."
        );
    }
}