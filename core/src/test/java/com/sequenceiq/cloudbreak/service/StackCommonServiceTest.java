package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_UPSCALE_ADJUSTMENT_TYPE_FALLBACK;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.base.ScalingStrategy;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.UpdateStackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StatusRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackAddVolumesRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackDeleteVolumesRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackImageChangeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.SaltPasswordStatus;
import com.sequenceiq.cloudbreak.api.model.RotateSaltPasswordReason;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterCache;
import com.sequenceiq.cloudbreak.common.ScalingHardLimitsService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.controller.validation.network.MultiAzValidator;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackScaleV4RequestToUpdateStackV4RequestConverter;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterOperationService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.salt.SaltVersionUpgradeService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackOperationService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
class StackCommonServiceTest {

    private static final long WORKSPACE_ID = 1L;

    private static final String INSTANCE_ID_PREFIX = "i-";

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final long STACK_ID = 2L;

    private static final NameOrCrn STACK_NAME = NameOrCrn.ofName("stackName");

    private static final NameOrCrn STACK_CRN =
            NameOrCrn.ofCrn("crn:cdp:datahub:us-west-1:" + ACCOUNT_ID + ":cluster:6b5a9aa7-223a-4d6a-93ca-27627be773b5");

    private static final String ACTOR_CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":user:" + UUID.randomUUID();

    private static final String SUBNET_ID = "aSubnetId";

    private static final String SUBNET_ID2 = "otherSubnetId";

    private static final FlowIdentifier FLOW_IDENTIFIER = new FlowIdentifier(FlowType.FLOW, "flowId");

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private StackService stackService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private StackOperationService stackOperationService;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private CloudbreakEventService eventService;

    @Mock
    private VerticalScalingValidatorService verticalScalingValidatorService;

    @Mock
    private MultiAzValidator multiAzValidator;

    @Mock
    private StackScaleV4RequestToUpdateStackV4RequestConverter stackScaleV4RequestToUpdateStackV4RequestConverter;

    @Mock
    private CloudParameterCache cloudParameterCache;

    @Mock
    private NodeCountLimitValidator nodeCountLimitValidator;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private ScalingHardLimitsService scalingHardLimitsService;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private ClusterBootstrapper clusterBootstrapper;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ClusterCommonService clusterCommonService;

    @Mock
    private ClusterOperationService clusterOperationService;

    @Mock
    private SaltVersionUpgradeService saltVersionUpgradeService;

    @InjectMocks
    private StackCommonService underTest;

    static Object[][] scalingAdjustmentProvider() {
        return new Object[][]{

                // platformVariant, inputAdjustmentType, finalAdjustmentType

                {"AWS", CloudConstants.AWS, AdjustmentType.BEST_EFFORT, AdjustmentType.EXACT},
                {"AWS", CloudConstants.AWS_NATIVE, AdjustmentType.BEST_EFFORT, AdjustmentType.BEST_EFFORT},
                {"AWS", CloudConstants.AWS_NATIVE_GOV, AdjustmentType.BEST_EFFORT, AdjustmentType.BEST_EFFORT},
                {"AZURE", CloudConstants.AZURE, AdjustmentType.BEST_EFFORT, AdjustmentType.EXACT},
                {"GCP", CloudConstants.GCP, AdjustmentType.BEST_EFFORT, AdjustmentType.BEST_EFFORT},

                {"AWS", CloudConstants.AWS, AdjustmentType.PERCENTAGE, AdjustmentType.EXACT},
                {"AWS", CloudConstants.AWS_NATIVE, AdjustmentType.PERCENTAGE, AdjustmentType.PERCENTAGE},
                {"AWS", CloudConstants.AWS_NATIVE_GOV, AdjustmentType.PERCENTAGE, AdjustmentType.PERCENTAGE},
                {"AZURE", CloudConstants.AZURE, AdjustmentType.PERCENTAGE, AdjustmentType.EXACT},
                {"GCP", CloudConstants.GCP, AdjustmentType.PERCENTAGE, AdjustmentType.PERCENTAGE},

                {"AWS", CloudConstants.AWS, AdjustmentType.EXACT, AdjustmentType.EXACT},
                {"AWS", CloudConstants.AWS_NATIVE, AdjustmentType.EXACT, AdjustmentType.EXACT},
                {"AWS", CloudConstants.AWS_NATIVE_GOV, AdjustmentType.EXACT, AdjustmentType.EXACT},
                {"AZURE", CloudConstants.AZURE, AdjustmentType.EXACT, AdjustmentType.EXACT},
                {"GCP", CloudConstants.GCP, AdjustmentType.EXACT, AdjustmentType.EXACT},
        };
    }

    @ParameterizedTest
    @MethodSource("scalingAdjustmentProvider")
    void testUpScalingForAdjustmentType(String cloudPlatform, String platformVariant,
            AdjustmentType inputAdjustmentType, AdjustmentType finalAdjustmentType) {
        String group = "master";
        StackDto stack = mock(StackDto.class);
        StackView stackView = mock(StackView.class);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.getCloudPlatform()).thenReturn(cloudPlatform);
        when(stack.getPlatformVariant()).thenReturn(platformVariant);
        when(stackView.getResourceCrn()).thenReturn(STACK_CRN.getCrn());
        when(stackDtoService.getByNameOrCrn(STACK_NAME, ACCOUNT_ID)).thenReturn(stack);
        StackScaleV4Request updateRequest = new StackScaleV4Request();
        updateRequest.setGroup(group);
        updateRequest.setAdjustmentType(inputAdjustmentType);
        UpdateStackV4Request updateStackV4Request = new UpdateStackV4Request();
        InstanceGroupAdjustmentV4Request instanceGroupAdjustment = new InstanceGroupAdjustmentV4Request();
        instanceGroupAdjustment.setScalingAdjustment(1);
        instanceGroupAdjustment.setInstanceGroup(group);
        updateStackV4Request.setInstanceGroupAdjustment(instanceGroupAdjustment);
        ArgumentCaptor<StackScaleV4Request> scaleRequestCaptor = ArgumentCaptor.forClass(StackScaleV4Request.class);
        when(stackScaleV4RequestToUpdateStackV4RequestConverter.convert(scaleRequestCaptor.capture())).thenReturn(updateStackV4Request);
        when(cloudParameterCache.isUpScalingSupported(anyString())).thenReturn(Boolean.TRUE);
        CloudbreakUser cloudbreakUser = mock(CloudbreakUser.class);
        when(cloudbreakUser.getUserCrn()).thenReturn("crn:cdp:" + Crn.Service.AUTOSCALE.getName() + ":us-west-1:altus:user:__internal__actor__");
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(cloudbreakUser);
        when(regionAwareInternalCrnGeneratorFactory.autoscale()).thenReturn(regionAwareInternalCrnGenerator);
        lenient().doNothing().when(eventService).fireCloudbreakEvent(any(), any(), eq(STACK_UPSCALE_ADJUSTMENT_TYPE_FALLBACK));
        when(multiAzValidator.validateNetworkScaleRequest(any(), any(), anyString())).thenReturn(ValidationResult.builder().build());

        underTest.putScalingInWorkspace(STACK_NAME, ACCOUNT_ID, updateRequest);

        assertEquals(finalAdjustmentType, scaleRequestCaptor.getValue().getAdjustmentType());
        if (!inputAdjustmentType.equals(finalAdjustmentType)) {
            verify(eventService).fireCloudbreakEvent(any(), any(), eq(STACK_UPSCALE_ADJUSTMENT_TYPE_FALLBACK));
        }
        verify(saltVersionUpgradeService, times(1)).validateSaltVersion(eq(stack));
    }

    @Test
    void testCreateImageChangeDtoWithCatalog() {
        StackImageChangeV4Request stackImageChangeRequest = new StackImageChangeV4Request();
        stackImageChangeRequest.setImageCatalogName("catalog");
        stackImageChangeRequest.setImageId("imageId");
        ImageCatalog catalog = new ImageCatalog();
        catalog.setName(stackImageChangeRequest.getImageCatalogName());
        catalog.setImageCatalogUrl("catalogUrl");
        when(imageCatalogService.getImageCatalogByName(WORKSPACE_ID, stackImageChangeRequest.getImageCatalogName())).thenReturn(catalog);
        when(stackService.getIdByNameOrCrnInWorkspace(STACK_NAME, WORKSPACE_ID)).thenReturn(STACK_ID);

        ImageChangeDto result = underTest.createImageChangeDto(STACK_NAME, WORKSPACE_ID, stackImageChangeRequest);

        assertEquals(STACK_ID, result.getStackId());
        assertEquals(stackImageChangeRequest.getImageId(), result.getImageId());
        assertEquals(catalog.getName(), result.getImageCatalogName());
        assertEquals(catalog.getImageCatalogUrl(), result.getImageCatalogUrl());
    }

    @Test
    void testCreateImageChangeDtoWithoutCatalog() {
        StackImageChangeV4Request stackImageChangeRequest = new StackImageChangeV4Request();
        stackImageChangeRequest.setImageId("imageId");
        when(stackService.getIdByNameOrCrnInWorkspace(STACK_NAME, WORKSPACE_ID)).thenReturn(STACK_ID);

        ImageChangeDto result = underTest.createImageChangeDto(STACK_NAME, WORKSPACE_ID, stackImageChangeRequest);

        assertEquals(STACK_ID, result.getStackId());
        assertEquals(stackImageChangeRequest.getImageId(), result.getImageId());
        assertNull(result.getImageCatalogName());
        assertNull(result.getImageCatalogUrl());
    }

    @Test
    void testChangeImageInWorkspace() {
        StackImageChangeV4Request stackImageChangeRequest = new StackImageChangeV4Request();
        when(stackService.getIdByNameOrCrnInWorkspace(STACK_NAME, WORKSPACE_ID)).thenReturn(STACK_ID);
        when(stackOperationService.updateImage(any(ImageChangeDto.class))).thenReturn(new FlowIdentifier(FlowType.FLOW, "id"));

        FlowIdentifier result = underTest.changeImageInWorkspace(STACK_NAME, WORKSPACE_ID, stackImageChangeRequest);

        assertEquals(FlowType.FLOW, result.getType());
        assertEquals("id", result.getPollableId());
    }

    @Test
    void testThrowsExceptionWhenDeleteInstanceFromDataLake() {
        StackDto stack = mock(StackDto.class);
        StackView stackView = mock(StackView.class);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.getType()).thenReturn(StackType.DATALAKE);
        when(stackDtoService.getByNameOrCrn(STACK_NAME, ACCOUNT_ID)).thenReturn(stack);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.deleteInstanceInWorkspace(STACK_NAME, ACCOUNT_ID, "node1", true));

        assertEquals("node1 is a node of a data lake cluster, therefore it's not allowed to delete/stop it.", exception.getMessage());
        verifyNoInteractions(stackOperationService);
    }

    @Test
    void testDeleteInstanceFromDataHub() {
        StackDto stack = mock(StackDto.class);
        StackView stackView = mock(StackView.class);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.getType()).thenReturn(StackType.WORKLOAD);
        when(stackDtoService.getByNameOrCrn(STACK_NAME, ACCOUNT_ID)).thenReturn(stack);

        underTest.deleteInstanceInWorkspace(STACK_NAME, ACCOUNT_ID, "node1", true);

        verify(stackOperationService).removeInstance(stack, "node1", true);
    }

    @Test
    void testThrowsExceptionWhenDeleteInstancesFromDataLake() {
        StackDto stack = mock(StackDto.class);
        StackView stackView = mock(StackView.class);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.getType()).thenReturn(StackType.DATALAKE);
        when(stackDtoService.getByNameOrCrn(STACK_NAME, ACCOUNT_ID)).thenReturn(stack);

        Set<String> nodes = new LinkedHashSet<>();
        nodes.add("node1");
        nodes.add("node2");

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.deleteMultipleInstancesInWorkspace(STACK_NAME, ACCOUNT_ID, nodes, true));

        assertEquals("node1, node2 are nodes of a data lake cluster, therefore it's not allowed to delete/stop them.", exception.getMessage());
        verifyNoInteractions(stackOperationService);
    }

    @Test
    void testDeleteInstancesFromDataHub() {
        StackDto stack = mock(StackDto.class);
        StackView stackView = mock(StackView.class);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.getType()).thenReturn(StackType.WORKLOAD);
        when(stackDtoService.getByNameOrCrn(STACK_NAME, ACCOUNT_ID)).thenReturn(stack);

        Set<String> nodes = new LinkedHashSet<>();
        nodes.add("node1");
        nodes.add("node2");

        underTest.deleteMultipleInstancesInWorkspace(STACK_NAME, ACCOUNT_ID, nodes, true);

        verify(stackOperationService).removeInstances(stack, nodes, true);
    }

    @Test
    void testStopInstancesInDatahub() {
        StackDto stack = mock(StackDto.class);
        StackView stackView = mock(StackView.class);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.getType()).thenReturn(StackType.WORKLOAD);
        when(stackDtoService.getByNameOrCrn(STACK_NAME, ACCOUNT_ID)).thenReturn(stack);
        when(stackUtil.stopStartScalingEntitlementEnabled(any())).thenReturn(true);

        Set<String> instances = new LinkedHashSet<>();
        instances.add("i-09855f4f334550bce");
        instances.add("i-0c06d3e9d07bacad8");

        underTest.stopMultipleInstancesInWorkspace(STACK_NAME, ACCOUNT_ID, instances, true);

        verify(stackOperationService).stopInstances(stack, instances, true);
    }

    @Test
    void testThrowsExceptionWhenStopInstancesInDatalake() {
        StackDto stack = mock(StackDto.class);
        StackView stackView = mock(StackView.class);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.getType()).thenReturn(StackType.DATALAKE);
        when(stackDtoService.getByNameOrCrn(STACK_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(stackUtil.stopStartScalingEntitlementEnabled(any())).thenReturn(true);

        Set<String> instances = new LinkedHashSet<>();
        instances.add("i-09855f4f334550bce");
        instances.add("i-0c06d3e9d07bacad8");

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.stopMultipleInstancesInWorkspace(STACK_CRN, ACCOUNT_ID, instances, true));
        assertEquals("i-09855f4f334550bce, i-0c06d3e9d07bacad8 are nodes of a data lake cluster, therefore it's not allowed to delete/stop them.",
                exception.getMessage());
        verifyNoInteractions(stackOperationService);
    }

    @Test
    void testThrowsExceptionWhenStackIsStopped() {
        StackDto stack = mock(StackDto.class);
        StackView stackView = mock(StackView.class);
        List<InstanceMetadataView> instanceMetadataViews = new ArrayList<>();
        InstanceMetaData im1 = new InstanceMetaData();
        im1.setInstanceId("i-1");
        im1.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        instanceMetadataViews.add(im1);
        when(stack.getAllNotTerminatedInstanceMetaData()).thenReturn(instanceMetadataViews);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.isStopped()).thenReturn(TRUE);
        when(stackView.getName()).thenReturn(STACK_NAME.getName());
        when(stackDtoService.getByNameOrCrn(STACK_CRN, ACCOUNT_ID)).thenReturn(stack);
        List<String> instanceIds = List.of("i-1", "i-2", "i-3");
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.restartMultipleInstances(STACK_CRN, ACCOUNT_ID, instanceIds));
        assertEquals("Cannot restart instances because the stackName is in stopped state.", exception.getMessage());
        verifyNoInteractions(stackOperationService);
    }

    @Test
    void testDoesThrowExceptionWhenAllNodesAreStoppedAndInstanceIdsDoesNotContainCmServer() {
        StackDto stack = mock(StackDto.class);
        List<InstanceMetadataView> instanceMetadataViews = new ArrayList<>();
        InstanceMetaData im1 = new InstanceMetaData();
        im1.setInstanceId("i-1");
        im1.setInstanceStatus(InstanceStatus.STOPPED);
        instanceMetadataViews.add(im1);
        InstanceMetaData im2 = new InstanceMetaData();
        im2.setInstanceId("i-2");
        im2.setInstanceStatus(InstanceStatus.STOPPED);
        instanceMetadataViews.add(im2);
        InstanceMetaData im3 = new InstanceMetaData();
        im3.setInstanceId("i-3");
        im3.setInstanceStatus(InstanceStatus.STOPPED);
        im3.setClusterManagerServer(true);
        instanceMetadataViews.add(im3);
        when(stack.getAllNotTerminatedInstanceMetaData()).thenReturn(instanceMetadataViews);
        when(stackDtoService.getByNameOrCrn(STACK_CRN, ACCOUNT_ID)).thenReturn(stack);
        List<String> instanceIds = List.of("i-1", "i-2");
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.restartMultipleInstances(STACK_CRN, ACCOUNT_ID, instanceIds));
        assertEquals("All cluster nodes are stopped. Initial start request need to contain CM server node. CM server node ids: [i-3]", exception.getMessage());
        verifyNoInteractions(stackOperationService);
    }

    @Test
    void testDoesNotThrowExceptionWhenAllNodesAreStoppedAndInstanceIdsContainCmServer() {
        StackDto stack = mock(StackDto.class);
        StackView stackView = mock(StackView.class);
        lenient().when(stack.getStack()).thenReturn(stackView);
        lenient().when(stackView.isStopped()).thenReturn(FALSE);
        lenient().when(stackView.getName()).thenReturn(STACK_NAME.getName());
        when(stack.getCloudPlatform()).thenReturn(CloudConstants.AWS);
        List<InstanceMetadataView> instanceMetadataViews = new ArrayList<>();
        InstanceMetaData im1 = new InstanceMetaData();
        im1.setInstanceId("i-1");
        im1.setInstanceStatus(InstanceStatus.STOPPED);
        instanceMetadataViews.add(im1);
        InstanceMetaData im2 = new InstanceMetaData();
        im2.setInstanceId("i-2");
        im2.setInstanceStatus(InstanceStatus.STOPPED);
        instanceMetadataViews.add(im2);
        InstanceMetaData im3 = new InstanceMetaData();
        im3.setInstanceId("i-3");
        im3.setInstanceStatus(InstanceStatus.STOPPED);
        im3.setClusterManagerServer(true);
        instanceMetadataViews.add(im3);
        when(stack.getAllNotTerminatedInstanceMetaData()).thenReturn(instanceMetadataViews);
        when(stack.getAllAvailableInstances()).thenReturn(instanceMetadataViews);
        when(stackDtoService.getByNameOrCrn(STACK_CRN, ACCOUNT_ID)).thenReturn(stack);
        List<String> instanceIds = List.of("i-1", "i-3");
        underTest.restartMultipleInstances(STACK_CRN, ACCOUNT_ID, instanceIds);
        verify(stackOperationService).restartInstances(stack, instanceIds);
    }

    @Test
    void testThrowsExceptionWhenInstancesNotOfGivenStack() {
        StackDto stack = mock(StackDto.class);
        StackView stackView = mock(StackView.class);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.isStopped()).thenReturn(FALSE);
        when(stackView.getDisplayName()).thenReturn(STACK_NAME.getName());
        when(stackView.getType()).thenReturn(StackType.DATALAKE);
        when(stackDtoService.getByNameOrCrn(STACK_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(stack.getAllNotTerminatedInstanceMetaData()).thenReturn(generateInstances(2, InstanceStatus.SERVICES_HEALTHY));
        List<String> instanceIds = List.of("i-1", "i-2", "i-3");
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.restartMultipleInstances(STACK_CRN, ACCOUNT_ID, instanceIds));
        assertEquals("Instance(s) Restart Failed for stackName DATALAKE. Invalid instanceIds in request: i-3", exception.getMessage());
        verifyNoInteractions(stackOperationService);
    }

    @Test
    void testThrowsExceptionWhenMoreThan20InstancesGivenForRestart() {
        StackDto stack = mock(StackDto.class);
        StackView stackView = mock(StackView.class);
        List<InstanceMetadataView> allInstanceMetadataView = generateInstances(25, InstanceStatus.SERVICES_HEALTHY);
        List<String> instanceIds = allInstanceMetadataView.stream().map(InstanceMetadataView::getInstanceId).collect(Collectors.toList());
        when(stackDtoService.getByNameOrCrn(STACK_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.isStopped()).thenReturn(FALSE);
        when(stack.getAllNotTerminatedInstanceMetaData()).thenReturn(allInstanceMetadataView);
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.restartMultipleInstances(STACK_CRN, ACCOUNT_ID, instanceIds));
        assertEquals("Cannot restart more than 20 instances at the same time.", exception.getMessage());
        verifyNoInteractions(stackOperationService);
    }

    @Test
    void testThrowsExceptionWhenOneOrMoreInstancesAreNotAvailable() {
        StackDto stack = mock(StackDto.class);
        StackView stackView = mock(StackView.class);
        List<InstanceMetadataView> allInstanceMetadataView = generateInstances(5, InstanceStatus.SERVICES_HEALTHY);
        List<InstanceMetadataView> healthyInstanceMetadataView = generateInstances(5, InstanceStatus.SERVICES_HEALTHY);
        InstanceGroup instanceGroup = new InstanceGroup();
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceId(INSTANCE_ID_PREFIX + (6));
        instanceMetaData.setInstanceStatus(InstanceStatus.ZOMBIE);
        instanceMetaData.setInstanceGroup(instanceGroup);
        instanceMetaData.setDiscoveryFQDN(INSTANCE_ID_PREFIX + (6));
        allInstanceMetadataView.add(instanceMetaData);
        List<String> instanceIds = allInstanceMetadataView.stream().map(InstanceMetadataView::getInstanceId).collect(Collectors.toList());
        when(stackDtoService.getByNameOrCrn(STACK_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.isStopped()).thenReturn(FALSE);
        when(stackView.getDisplayName()).thenReturn(STACK_NAME.getName());
        when(stackView.getType()).thenReturn(StackType.DATALAKE);
        when(stack.getAllAvailableInstances()).thenReturn(healthyInstanceMetadataView);
        when(stack.getAllNotTerminatedInstanceMetaData()).thenReturn(allInstanceMetadataView);
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.restartMultipleInstances(STACK_CRN, ACCOUNT_ID, instanceIds));
        assertEquals("Instance(s) Restart Failed for stackName DATALAKE. This Instance(s): i-6 are either terminated or are in zombie state.",
                exception.getMessage());
        verifyNoInteractions(stackOperationService);
    }

    @Test
    void testThrowsExceptionWhenTriedRestartingWithInvalidCloudProvider() {
        StackDto stack = mock(StackDto.class);
        StackView stackView = mock(StackView.class);
        List<InstanceMetadataView> allInstanceMetadataView = generateInstances(5, InstanceStatus.SERVICES_HEALTHY);
        List<String> instanceIds = allInstanceMetadataView.stream().map(InstanceMetadataView::getInstanceId).collect(Collectors.toList());
        when(stackDtoService.getByNameOrCrn(STACK_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.isStopped()).thenReturn(FALSE);
        when(stack.getAllNotTerminatedInstanceMetaData()).thenReturn(allInstanceMetadataView);
        when(stack.getAllAvailableInstances()).thenReturn(allInstanceMetadataView);
        when(stack.getCloudPlatform()).thenReturn("YCLOUD");
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.restartMultipleInstances(STACK_CRN, ACCOUNT_ID, instanceIds));
        assertEquals("Restart instances is not supported for YCLOUD Cloud Platform", exception.getMessage());
        verifyNoInteractions(stackOperationService);
    }

    @Test
    void testRestartMultipleInstances() {
        StackDto stack = mock(StackDto.class);
        StackView stackView = mock(StackView.class);
        List<InstanceMetadataView> allInstanceMetadataView = generateInstances(5, InstanceStatus.SERVICES_HEALTHY);
        List<String> instanceIds = allInstanceMetadataView.stream().map(InstanceMetadataView::getInstanceId).collect(Collectors.toList());
        when(stackDtoService.getByNameOrCrn(STACK_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.isStopped()).thenReturn(FALSE);
        when(stack.getAllNotTerminatedInstanceMetaData()).thenReturn(allInstanceMetadataView);
        when(stack.getAllAvailableInstances()).thenReturn(allInstanceMetadataView);
        when(stack.getCloudPlatform()).thenReturn("AWS");
        underTest.restartMultipleInstances(STACK_CRN, ACCOUNT_ID, instanceIds);
        verify(stackOperationService).restartInstances(stack, instanceIds);
    }

    @Test
    void testStartInstancesInDefaultWorkspace() {
        StackDto stack = mock(StackDto.class);

        when(stackDtoService.getByNameOrCrn(STACK_CRN, ACCOUNT_ID)).thenReturn(stack);
        CloudbreakUser cloudbreakUser = mock(CloudbreakUser.class);
        when(cloudbreakUser.getUserCrn()).thenReturn("crn:cdp:" + Crn.Service.AUTOSCALE.getName() + ":us-west-1:altus:user:__internal__actor__");
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(cloudbreakUser);
        when(stackUtil.stopStartScalingEntitlementEnabled(any())).thenReturn(true);

        UpdateStackV4Request updateStackV4Request = new UpdateStackV4Request();
        updateStackV4Request.setWithClusterEvent(true);
        InstanceGroupAdjustmentV4Request instanceGroupAdjustmentV4Request = new InstanceGroupAdjustmentV4Request();
        instanceGroupAdjustmentV4Request.setInstanceGroup("instanceGroup");
        instanceGroupAdjustmentV4Request.setScalingAdjustment(5);
        updateStackV4Request.setInstanceGroupAdjustment(instanceGroupAdjustmentV4Request);
        when(regionAwareInternalCrnGeneratorFactory.autoscale()).thenReturn(regionAwareInternalCrnGenerator);
        // Regular flow
        underTest.putStartInstancesInDefaultWorkspace(STACK_CRN, ACCOUNT_ID, updateStackV4Request, ScalingStrategy.STOPSTART_FALLBACK_TO_REGULAR);
        verify(stackOperationService).updateNodeCountStartInstances(stack, updateStackV4Request.getInstanceGroupAdjustment(),
                true, ScalingStrategy.STOPSTART_FALLBACK_TO_REGULAR);

        // Null scaling strategy
        underTest.putStartInstancesInDefaultWorkspace(STACK_CRN, ACCOUNT_ID, updateStackV4Request, null);
        verify(stackOperationService).updateNodeCountStartInstances(stack, updateStackV4Request.getInstanceGroupAdjustment(),
                true, ScalingStrategy.STOPSTART);

        // Status is set - Bad Request
        updateStackV4Request.setStatus(StatusRequest.FULL_SYNC);
        assertThrows(BadRequestException.class, () -> underTest.putStartInstancesInDefaultWorkspace(STACK_CRN, ACCOUNT_ID, updateStackV4Request,
                null));
    }

    @Test
    void testValidateNetworkScaleRequestWhenValidatorDoesNotFindError() {
        String groupName = "aGroupName";
        StackDto stack = mock(StackDto.class);
        NetworkScaleV4Request networkScaleV4Request = new NetworkScaleV4Request();
        when(multiAzValidator.validateNetworkScaleRequest(stack, networkScaleV4Request, groupName)).thenReturn(ValidationResult.builder().build());

        assertDoesNotThrow(() -> underTest.validateNetworkScaleRequest(stack, networkScaleV4Request, groupName));

        verify(multiAzValidator, times(1)).validateNetworkScaleRequest(stack, networkScaleV4Request, groupName);
    }

    @Test
    void testValidateNetworkScaleRequestWhenValidatorDoesFindError() {
        String groupName = "aGroupName";
        StackDto stack = mock(StackDto.class);
        NetworkScaleV4Request networkScaleV4Request = new NetworkScaleV4Request();
        ValidationResult validationResult = ValidationResult.builder()
                .error("Network Scale Request Error")
                .build();
        when(multiAzValidator.validateNetworkScaleRequest(stack, networkScaleV4Request, groupName)).thenReturn(validationResult);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.validateNetworkScaleRequest(stack, networkScaleV4Request, groupName));

        verify(multiAzValidator, times(1)).validateNetworkScaleRequest(stack, networkScaleV4Request, groupName);
        assertThat(badRequestException.getMessage())
                .startsWith("The provided network scale request is not valid:")
                .contains("Network Scale Request Error");
    }

    @Test
    void testPutScalingInWorkspaceWhenThereIsPreferredAzAndStackProvisionedInMultipleSubnetButScalingIsNotSupportedOnPlatform() {
        StackDto stack = mock(StackDto.class);
        StackView stackView = mock(StackView.class);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.getCloudPlatform()).thenReturn(AwsConstants.AWS_PLATFORM.value());
        String variant = AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.name();
        when(stack.getPlatformVariant()).thenReturn(variant);
        when(stackDtoService.getByNameOrCrn(STACK_NAME, ACCOUNT_ID)).thenReturn(stack);
        NetworkScaleV4Request networkScaleV4Request = new NetworkScaleV4Request();
        StackScaleV4Request updateRequest = new StackScaleV4Request();
        updateRequest.setStackNetworkScaleV4Request(networkScaleV4Request);
        UpdateStackV4Request updateStackV4Request = new UpdateStackV4Request();
        InstanceGroupAdjustmentV4Request instanceGroupAdjustment = new InstanceGroupAdjustmentV4Request();
        instanceGroupAdjustment.setScalingAdjustment(1);
        updateStackV4Request.setInstanceGroupAdjustment(instanceGroupAdjustment);
        when(stackScaleV4RequestToUpdateStackV4RequestConverter.convert(any())).thenReturn(updateStackV4Request);
        when(cloudParameterCache.isUpScalingSupported(anyString())).thenReturn(FALSE);

        BadRequestException actual = assertThrows(BadRequestException.class, () -> underTest.putScalingInWorkspace(STACK_NAME, ACCOUNT_ID, updateRequest));
        assertEquals(actual.getMessage(), "Upscaling is not supported on AWS cloudplatform");
    }

    @ParameterizedTest
    @MethodSource("scalingAdjustmentProvider")
    void testPutScalingInWorkspaceWhenUpscalingKraftHostGroup(String cloudPlatform, String platformVariant,
            AdjustmentType inputAdjustmentType, AdjustmentType finalAdjustmentType) {
        String group = "kraft";
        StackDto stack = mock(StackDto.class);
        StackView stackView = mock(StackView.class);
        when(stack.getStack()).thenReturn(stackView);
        when(stack.getPlatformVariant()).thenReturn(platformVariant);
        when(stackDtoService.getByNameOrCrn(STACK_NAME, ACCOUNT_ID)).thenReturn(stack);
        StackScaleV4Request updateRequest = new StackScaleV4Request();
        updateRequest.setGroup(group);
        updateRequest.setAdjustmentType(inputAdjustmentType);
        UpdateStackV4Request updateStackV4Request = new UpdateStackV4Request();
        InstanceGroupAdjustmentV4Request instanceGroupAdjustment = new InstanceGroupAdjustmentV4Request();
        instanceGroupAdjustment.setScalingAdjustment(1);
        instanceGroupAdjustment.setInstanceGroup(group);
        updateStackV4Request.setInstanceGroupAdjustment(instanceGroupAdjustment);
        ArgumentCaptor<StackScaleV4Request> scaleRequestCaptor = ArgumentCaptor.forClass(StackScaleV4Request.class);
        when(stackScaleV4RequestToUpdateStackV4RequestConverter.convert(scaleRequestCaptor.capture())).thenReturn(updateStackV4Request);
        CloudbreakUser cloudbreakUser = mock(CloudbreakUser.class);
        lenient().doNothing().when(eventService).fireCloudbreakEvent(any(), any(), eq(STACK_UPSCALE_ADJUSTMENT_TYPE_FALLBACK));

        BadRequestException actual = assertThrows(BadRequestException.class, () -> underTest.putScalingInWorkspace(STACK_NAME, ACCOUNT_ID, updateRequest));
        assertEquals(actual.getMessage(), "Resizing is not supported for kraft host group");
    }

    @ParameterizedTest
    @MethodSource("scalingAdjustmentProvider")
    void testPutScalingInWorkspaceWhenDownscalingKraftHostGroup(String cloudPlatform, String platformVariant,
            AdjustmentType inputAdjustmentType, AdjustmentType finalAdjustmentType) {
        String group = "kraft";
        StackDto stack = mock(StackDto.class);
        StackView stackView = mock(StackView.class);
        when(stack.getStack()).thenReturn(stackView);
        when(stack.getPlatformVariant()).thenReturn(platformVariant);
        when(stackDtoService.getByNameOrCrn(STACK_NAME, ACCOUNT_ID)).thenReturn(stack);
        StackScaleV4Request updateRequest = new StackScaleV4Request();
        updateRequest.setGroup(group);
        updateRequest.setAdjustmentType(inputAdjustmentType);
        UpdateStackV4Request updateStackV4Request = new UpdateStackV4Request();
        InstanceGroupAdjustmentV4Request instanceGroupAdjustment = new InstanceGroupAdjustmentV4Request();
        instanceGroupAdjustment.setScalingAdjustment(-1);
        instanceGroupAdjustment.setInstanceGroup(group);
        updateStackV4Request.setInstanceGroupAdjustment(instanceGroupAdjustment);
        ArgumentCaptor<StackScaleV4Request> scaleRequestCaptor = ArgumentCaptor.forClass(StackScaleV4Request.class);
        when(stackScaleV4RequestToUpdateStackV4RequestConverter.convert(scaleRequestCaptor.capture())).thenReturn(updateStackV4Request);
        CloudbreakUser cloudbreakUser = mock(CloudbreakUser.class);
        lenient().doNothing().when(eventService).fireCloudbreakEvent(any(), any(), eq(STACK_UPSCALE_ADJUSTMENT_TYPE_FALLBACK));

        BadRequestException actual = assertThrows(BadRequestException.class, () -> underTest.putScalingInWorkspace(STACK_NAME, ACCOUNT_ID, updateRequest));
        assertEquals(actual.getMessage(), "Resizing is not supported for kraft host group");
    }

    @Test
    void testRotateSaltPassword() {
        ThreadBasedUserCrnProvider.doAs(ACTOR_CRN, () -> underTest.rotateSaltPassword(STACK_CRN, ACCOUNT_ID, RotateSaltPasswordReason.MANUAL));

        verify(stackOperationService).rotateSaltPassword(STACK_CRN, ACCOUNT_ID, RotateSaltPasswordReason.MANUAL);
    }

    @Test
    void testModifyProxyConfig() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName("name");
        String previousProxyConfigCrn = "prev-proxy-crn";

        underTest.modifyProxyConfig(nameOrCrn, ACCOUNT_ID, previousProxyConfigCrn);

        verify(stackOperationService).modifyProxyConfig(nameOrCrn, ACCOUNT_ID, previousProxyConfigCrn);
    }

    @Test
    void testSyncWhenInvalidStackStatusThenBadRequest() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setName(STACK_NAME.getName());
        Workspace workspace = new Workspace();
        workspace.setTenant(new Tenant());
        stack.setWorkspace(workspace);
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.STOPPED));
        when(stackDtoService.getStackViewByNameOrCrn(STACK_NAME, ACCOUNT_ID)).thenReturn(stack);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.syncComponentVersionsFromCmInWorkspace(STACK_NAME, ACCOUNT_ID, Set.of()));

        assertEquals("Reading CM and parcel versions from CM cannot be initiated as the cluster is in STOPPED state.",
                badRequestException.getMessage());
        verify(instanceMetaDataService, never()).anyInstanceStopped(STACK_ID);
    }

    @Test
    void testSyncWhenInvalidInstanceStatusThenBadRequest() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        Workspace workspace = new Workspace();
        workspace.setTenant(new Tenant());
        stack.setWorkspace(workspace);
        stack.setName(STACK_NAME.getName());
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.NODE_FAILURE));
        when(stackDtoService.getStackViewByNameOrCrn(STACK_NAME, ACCOUNT_ID)).thenReturn(stack);
        when(instanceMetaDataService.anyInstanceStopped(STACK_ID)).thenReturn(true);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.syncComponentVersionsFromCmInWorkspace(STACK_NAME, ACCOUNT_ID, Set.of()));

        assertEquals("Please start all stopped instances. Reading CM and parcel versions from CM can only be made when all your nodes in running state.",
                badRequestException.getMessage());
        verify(instanceMetaDataService, times(1)).anyInstanceStopped(STACK_ID);
    }

    @Test
    void testSyncWhenValidationsPassThenSuccess() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setName(STACK_NAME.getName());
        Workspace workspace = new Workspace();
        workspace.setTenant(new Tenant());
        stack.setWorkspace(workspace);
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.NODE_FAILURE));
        when(stackDtoService.getStackViewByNameOrCrn(STACK_NAME, ACCOUNT_ID)).thenReturn(stack);
        when(instanceMetaDataService.anyInstanceStopped(STACK_ID)).thenReturn(false);
        when(stackOperationService.syncComponentVersionsFromCm(stack, Set.of())).thenReturn(new FlowIdentifier(FlowType.FLOW, "1"));

        FlowIdentifier flowIdentifier = underTest.syncComponentVersionsFromCmInWorkspace(STACK_NAME, ACCOUNT_ID, Set.of());

        assertEquals("1", flowIdentifier.getPollableId());
        verify(instanceMetaDataService, times(1)).anyInstanceStopped(STACK_ID);
    }

    @Test
    void checkIfSaltPasswordRotationNeeded() {
        when(stackOperationService.getSaltPasswordStatus(STACK_CRN, ACCOUNT_ID)).thenReturn(SaltPasswordStatus.OK);

        SaltPasswordStatus result = underTest.getSaltPasswordStatus(STACK_CRN, ACCOUNT_ID);

        assertEquals(SaltPasswordStatus.OK, result);
        verify(stackOperationService).getSaltPasswordStatus(STACK_CRN, ACCOUNT_ID);
    }

    @Test
    void testPutDeleteVolumesInWorkspaceSuccess() {
        Stack stack = mock(Stack.class);
        StackView stackView = mock(StackView.class);
        when(stackView.getId()).thenReturn(STACK_ID);
        when(stackView.getResourceCrn()).thenReturn("CRN");
        when(stackService.getByIdWithLists(STACK_ID)).thenReturn(stack);
        when(stackDtoService.getStackViewByNameOrCrn(STACK_NAME, ACCOUNT_ID)).thenReturn(stackView);

        StackDeleteVolumesRequest stackDeleteVolumesRequest = new StackDeleteVolumesRequest();
        stackDeleteVolumesRequest.setStackId(1L);
        stackDeleteVolumesRequest.setGroup("COMPUTE");

        underTest.putDeleteVolumesInWorkspace(STACK_NAME, ACCOUNT_ID, stackDeleteVolumesRequest);

        verify(clusterCommonService).putDeleteVolumes("CRN", stackDeleteVolumesRequest);
    }

    @Test
    void testPutDeleteVolumesInWorkspaceFailure() {
        Stack stack = mock(Stack.class);
        StackView stackView = mock(StackView.class);
        when(stackView.getId()).thenReturn(STACK_ID);
        when(stackView.getResourceCrn()).thenReturn("CRN");
        when(stackService.getByIdWithLists(STACK_ID)).thenReturn(stack);
        when(stackDtoService.getStackViewByNameOrCrn(STACK_NAME, ACCOUNT_ID)).thenReturn(stackView);
        doThrow(new BadRequestException("Deleting volumes is not supported on MOCK cloudplatform")).when(verticalScalingValidatorService)
                .validateProviderForDelete(any(), anyString(), eq(false));
        StackDeleteVolumesRequest stackDeleteVolumesRequest = new StackDeleteVolumesRequest();
        stackDeleteVolumesRequest.setStackId(1L);
        stackDeleteVolumesRequest.setGroup("COMPUTE");

        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.putDeleteVolumesInWorkspace(STACK_NAME,
                ACCOUNT_ID, stackDeleteVolumesRequest));

        assertEquals("Deleting volumes is not supported on MOCK cloudplatform", exception.getMessage());
    }

    @Test
    void testPutDeleteVolumesInWorkspaceFailureNotEntitled() {
        Stack stack = mock(Stack.class);
        StackView stackView = mock(StackView.class);
        when(stackView.getId()).thenReturn(STACK_ID);
        when(stackView.getResourceCrn()).thenReturn("CRN");
        when(stackService.getByIdWithLists(STACK_ID)).thenReturn(stack);
        when(stackDtoService.getStackViewByNameOrCrn(STACK_NAME, ACCOUNT_ID)).thenReturn(stackView);
        String errorMessage = "Deleting Disk for Azure is not enabled for this account";
        doThrow(new BadRequestException(errorMessage)).when(verticalScalingValidatorService)
                .validateEntitlementForDelete(stack);
        StackDeleteVolumesRequest stackDeleteVolumesRequest = new StackDeleteVolumesRequest();
        stackDeleteVolumesRequest.setStackId(1L);
        stackDeleteVolumesRequest.setGroup("COMPUTE");

        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.putDeleteVolumesInWorkspace(STACK_NAME,
                ACCOUNT_ID, stackDeleteVolumesRequest));

        assertEquals(errorMessage, exception.getMessage());
    }

    @Test
    void syncInWorkspaceTestWhenSuccess() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName("myStack");
        StackDto stack = mock(StackDto.class);
        when(stackDtoService.getByNameOrCrn(nameOrCrn, ACCOUNT_ID)).thenReturn(stack);
        StackView stackView = mock(StackView.class);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.getType()).thenReturn(StackType.DATALAKE);

        FlowIdentifier flowIdentifier = mock(FlowIdentifier.class);
        when(stackOperationService.updateStatus(stack, StatusRequest.FULL_SYNC, true)).thenReturn(flowIdentifier);

        FlowIdentifier result = underTest.syncInWorkspace(nameOrCrn, ACCOUNT_ID, EnumSet.of(StackType.DATALAKE));

        assertThat(result).isSameAs(flowIdentifier);
    }

    @Test
    void syncInWorkspaceTestWhenWrongType() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName("myStack");
        StackDto stack = mock(StackDto.class);
        when(stackDtoService.getByNameOrCrn(nameOrCrn, ACCOUNT_ID)).thenReturn(stack);
        StackView stackView = mock(StackView.class);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.getType()).thenReturn(StackType.TEMPLATE);

        Set<StackType> permittedStackTypes = new LinkedHashSet<>();
        permittedStackTypes.add(StackType.DATALAKE);
        permittedStackTypes.add(StackType.WORKLOAD);
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.syncInWorkspace(nameOrCrn, ACCOUNT_ID, permittedStackTypes));

        assertThat(exception).hasMessage("Sync is not supported for stack '[NameOrCrn of name: 'myStack']'. " +
                "Its type is 'template', whereas the operation is permitted only for the following types: datalake, datahub.");
    }

    @Test
    void testPutAddVolumesInWorkspaceSuccess() {
        StackView stackDto = mock(StackView.class);
        doReturn(STACK_ID).when(stackDto).getId();
        doReturn(stackDto).when(stackDtoService).getStackViewByNameOrCrn(STACK_NAME, "accid");
        Stack stack = mock(Stack.class);
        when(stack.getResourceCrn()).thenReturn("CRN");
        when(stackService.getByIdWithLists(STACK_ID)).thenReturn(stack);

        StackAddVolumesRequest stackAddVolumesRequest = new StackAddVolumesRequest();
        stackAddVolumesRequest.setInstanceGroup("COMPUTE");
        stackAddVolumesRequest.setCloudVolumeUsageType("GENERAL");
        stackAddVolumesRequest.setSize(200L);
        stackAddVolumesRequest.setType("gp2");
        stackAddVolumesRequest.setNumberOfDisks(2L);

        underTest.putAddVolumesInWorkspace(STACK_NAME, "accid", stackAddVolumesRequest);

        verify(clusterCommonService).putAddVolumes("CRN", stackAddVolumesRequest);
    }

    @Test
    void testPutAddVolumesInWorkspaceFailure() {
        StackView stackDto = mock(StackView.class);
        doReturn(STACK_ID).when(stackDto).getId();
        doReturn(stackDto).when(stackDtoService).getStackViewByNameOrCrn(STACK_CRN, "accid");
        Stack stack = mock(Stack.class);
        when(stackService.getByIdWithLists(STACK_ID)).thenReturn(stack);

        StackAddVolumesRequest stackAddVolumesRequest = new StackAddVolumesRequest();
        stackAddVolumesRequest.setInstanceGroup("COMPUTE");
        stackAddVolumesRequest.setCloudVolumeUsageType("GENERAL");
        stackAddVolumesRequest.setSize(200L);
        stackAddVolumesRequest.setType("gp2");
        stackAddVolumesRequest.setNumberOfDisks(2L);

        doThrow(new BadRequestException("Adding volumes is not supported on MOCK cloudplatform")).when(verticalScalingValidatorService)
                .validateProviderForAddVolumes(any(), anyString());

        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.putAddVolumesInWorkspace(STACK_CRN, "accid",
                stackAddVolumesRequest));

        assertEquals("Adding volumes is not supported on MOCK cloudplatform", exception.getMessage());
    }

    @Test
    void testPutAddVolumesInWorkspaceFailureForEntitlement() {
        StackView stackDto = mock(StackView.class);
        doReturn(STACK_ID).when(stackDto).getId();
        doReturn(stackDto).when(stackDtoService).getStackViewByNameOrCrn(STACK_CRN, "accid");
        Stack stack = mock(Stack.class);
        when(stackService.getByIdWithLists(STACK_ID)).thenReturn(stack);

        StackAddVolumesRequest stackAddVolumesRequest = new StackAddVolumesRequest();
        stackAddVolumesRequest.setInstanceGroup("COMPUTE");
        stackAddVolumesRequest.setCloudVolumeUsageType("GENERAL");
        stackAddVolumesRequest.setSize(200L);
        stackAddVolumesRequest.setType("gp2");
        stackAddVolumesRequest.setNumberOfDisks(2L);

        doThrow(new BadRequestException("Adding Disk for Azure is not enabled for this account")).when(verticalScalingValidatorService)
                .validateEntitlementForAddVolumes(any());

        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.putAddVolumesInWorkspace(STACK_CRN, "accid",
                stackAddVolumesRequest));

        assertEquals("Adding Disk for Azure is not enabled for this account", exception.getMessage());
    }

    @Test
    void testRotateRdsCertificate() {
        Stack stack = mock(Stack.class);
        when(clusterOperationService.rotateRdsCertificate(stack)).thenReturn(FLOW_IDENTIFIER);
        FlowIdentifier result = underTest.rotateRdsCertificate(stack);
        verify(clusterOperationService).rotateRdsCertificate(stack);
        assertThat(result).isEqualTo(FLOW_IDENTIFIER);
    }

    private List<InstanceMetadataView> generateInstances(int count,
            com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus instanceStatus) {
        List<InstanceMetadataView> instances = new ArrayList<>(count);
        InstanceGroup instanceGroup = new InstanceGroup();
        for (int i = 1; i <= count; i++) {
            InstanceMetaData instanceMetaData = new InstanceMetaData();
            instanceMetaData.setInstanceId(INSTANCE_ID_PREFIX + (i));
            instanceMetaData.setInstanceStatus(instanceStatus);
            instanceMetaData.setInstanceGroup(instanceGroup);
            instanceMetaData.setDiscoveryFQDN(INSTANCE_ID_PREFIX + (i));
            instances.add(instanceMetaData);
        }
        return instances;
    }
}
