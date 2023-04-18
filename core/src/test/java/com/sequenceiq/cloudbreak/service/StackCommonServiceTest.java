package com.sequenceiq.cloudbreak.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.base.ScalingStrategy;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.UpdateStackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StatusRequest;
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
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.controller.validation.network.MultiAzValidator;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackScaleV4RequestToUpdateStackV4RequestConverter;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackOperationService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
class StackCommonServiceTest {

    private static final String AWS = CloudPlatform.AWS.name();

    private static final long WORKSPACE_ID = 1L;

    private static final String ACCOUNT_ID = "accountId";

    private static final long STACK_ID = 2L;

    private static final NameOrCrn STACK_NAME = NameOrCrn.ofName("stackName");

    private static final NameOrCrn STACK_CRN =
            NameOrCrn.ofCrn("crn:cdp:datahub:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:cluster:6b5a9aa7-223a-4d6a-93ca-27627be773b5");

    private static final String ACTOR_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    private static final String SUBNET_ID = "aSubnetId";

    private static final String SUBNET_ID2 = "otherSubnetId";

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

    @InjectMocks
    private StackCommonService underTest;

    @Test
    public void testCreateImageChangeDtoWithCatalog() {
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
    public void testCreateImageChangeDtoWithoutCatalog() {
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
    public void testChangeImageInWorkspace() {
        StackImageChangeV4Request stackImageChangeRequest = new StackImageChangeV4Request();
        when(stackService.getIdByNameOrCrnInWorkspace(STACK_NAME, WORKSPACE_ID)).thenReturn(STACK_ID);
        when(stackOperationService.updateImage(any(ImageChangeDto.class))).thenReturn(new FlowIdentifier(FlowType.FLOW, "id"));

        FlowIdentifier result = underTest.changeImageInWorkspace(STACK_NAME, WORKSPACE_ID, stackImageChangeRequest);

        assertEquals(FlowType.FLOW, result.getType());
        assertEquals("id", result.getPollableId());
    }

    @Test
    public void testThrowsExceptionWhenDeleteInstanceFromDataLake() {
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
    public void testDeleteInstanceFromDataHub() {
        StackDto stack = mock(StackDto.class);
        StackView stackView = mock(StackView.class);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.getType()).thenReturn(StackType.WORKLOAD);
        when(stackDtoService.getByNameOrCrn(STACK_NAME, ACCOUNT_ID)).thenReturn(stack);

        underTest.deleteInstanceInWorkspace(STACK_NAME, ACCOUNT_ID, "node1", true);

        verify(stackOperationService).removeInstance(stack, "node1", true);
    }

    @Test
    public void testThrowsExceptionWhenDeleteInstancesFromDataLake() {
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
    public void testDeleteInstancesFromDataHub() {
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
    public void testStopInstancesInDatahub() {
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
    public void testThrowsExceptionWhenStopInstancesInDatalake() {
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
    public void testStartInstancesInDefaultWorkspace() {
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
    public void testvalidateNetworkScaleRequestWhenVariantIsNotSupportedForMultiAzButAzHasBeenPreferredInTheRequest() {
        StackDto stack = mock(StackDto.class);
        String variant = AwsConstants.AwsVariant.AWS_VARIANT.name();
        when(stack.getPlatformVariant()).thenReturn(variant);
        when(multiAzValidator.supportedVariant(variant)).thenReturn(Boolean.FALSE);
        NetworkScaleV4Request networkScaleV4Request = new NetworkScaleV4Request();
        networkScaleV4Request.setPreferredSubnetIds(List.of(SUBNET_ID));

        Assertions.assertThrows(BadRequestException.class, () -> underTest.validateNetworkScaleRequest(stack, networkScaleV4Request));

        Mockito.verify(multiAzValidator, times(1)).supportedVariant(variant);
        Mockito.verify(multiAzValidator, times(0)).collectSubnetIds(any());
    }

    @Test
    public void testValidateNetworkScaleRequestWhenThereIsPreferredAzAndStackProvisionedToASingleSubnet() {
        StackDto stack = mock(StackDto.class);
        String variant = AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.name();
        when(stack.getPlatformVariant()).thenReturn(variant);
        when(multiAzValidator.supportedVariant(variant)).thenReturn(Boolean.TRUE);
        when(multiAzValidator.collectSubnetIds(any())).thenReturn(Set.of(SUBNET_ID));
        NetworkScaleV4Request networkScaleV4Request = new NetworkScaleV4Request();
        networkScaleV4Request.setPreferredSubnetIds(List.of(SUBNET_ID));

        Assertions.assertThrows(BadRequestException.class, () -> underTest.validateNetworkScaleRequest(stack, networkScaleV4Request));

        Mockito.verify(multiAzValidator, times(1)).supportedVariant(variant);
        Mockito.verify(multiAzValidator, times(1)).collectSubnetIds(any());
    }

    @Test
    public void testPutScalingInWorkspaceWhenThereIsPreferredAzAndStackProvisionedInMultipleSubnetButScalingIsNotSupportedOnPlatform() {
        StackDto stack = mock(StackDto.class);
        StackView stackView = mock(StackView.class);
        when(stack.getStack()).thenReturn(stackView);
        when(stackView.getCloudPlatform()).thenReturn(AwsConstants.AWS_PLATFORM.value());
        String variant = AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.name();
        when(stack.getPlatformVariant()).thenReturn(variant);
        when(stackDtoService.getByNameOrCrn(STACK_NAME, ACCOUNT_ID)).thenReturn(stack);
        when(multiAzValidator.supportedVariant(variant)).thenReturn(Boolean.TRUE);
        when(multiAzValidator.collectSubnetIds(any())).thenReturn(Set.of(SUBNET_ID, SUBNET_ID2));
        NetworkScaleV4Request networkScaleV4Request = new NetworkScaleV4Request();
        networkScaleV4Request.setPreferredSubnetIds(List.of(SUBNET_ID));
        StackScaleV4Request updateRequest = new StackScaleV4Request();
        updateRequest.setStackNetworkScaleV4Request(networkScaleV4Request);
        UpdateStackV4Request updateStackV4Request = new UpdateStackV4Request();
        InstanceGroupAdjustmentV4Request instanceGroupAdjustment = new InstanceGroupAdjustmentV4Request();
        instanceGroupAdjustment.setScalingAdjustment(1);
        updateStackV4Request.setInstanceGroupAdjustment(instanceGroupAdjustment);
        when(stackScaleV4RequestToUpdateStackV4RequestConverter.convert(any())).thenReturn(updateStackV4Request);
        when(cloudParameterCache.isUpScalingSupported(anyString())).thenReturn(Boolean.FALSE);

        BadRequestException actual = assertThrows(BadRequestException.class, () -> underTest.putScalingInWorkspace(STACK_NAME, ACCOUNT_ID, updateRequest));
        assertEquals(actual.getMessage(), "Upscaling is not supported on AWS cloudplatform");

    }

    @Test
    public void testRotateSaltPassword() throws CloudbreakOrchestratorException {
        ThreadBasedUserCrnProvider.doAs(ACTOR_CRN, () -> underTest.rotateSaltPassword(STACK_CRN, ACCOUNT_ID, RotateSaltPasswordReason.MANUAL));

        verify(stackOperationService).rotateSaltPassword(STACK_CRN, ACCOUNT_ID, RotateSaltPasswordReason.MANUAL);
    }

    @Test
    public void testModifyProxyConfig() {
        NameOrCrn nameOrCrn = NameOrCrn.ofName("name");
        String previousProxyConfigCrn = "prev-proxy-crn";

        underTest.modifyProxyConfig(nameOrCrn, ACCOUNT_ID, previousProxyConfigCrn);

        verify(stackOperationService).modifyProxyConfig(nameOrCrn, ACCOUNT_ID, previousProxyConfigCrn);
    }

    @Test
    public void testSyncWhenInvalidStackStatusThenBadRequest() {
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
    public void testSyncWhenInvalidInstanceStatusThenBadRequest() {
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
    public void testSyncWhenValidationsPassThenSuccess() {
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
    public void checkIfSaltPasswordRotationNeeded() {
        when(stackOperationService.getSaltPasswordStatus(STACK_CRN, ACCOUNT_ID)).thenReturn(SaltPasswordStatus.OK);

        SaltPasswordStatus result = underTest.getSaltPasswordStatus(STACK_CRN, ACCOUNT_ID);

        assertEquals(SaltPasswordStatus.OK, result);
        verify(stackOperationService).getSaltPasswordStatus(STACK_CRN, ACCOUNT_ID);
    }

    @Test
    public void testPutDeleteVolumesInWorkspaceSuccess() {
        Stack stack = mock(Stack.class);
        StackView stackView = mock(StackView.class);
        when(stackView.getId()).thenReturn(STACK_ID);
        when(stackView.getResourceCrn()).thenReturn("CRN");
        when(stackService.getByIdWithLists(STACK_ID)).thenReturn(stack);
        when(stackDtoService.getStackViewByNameOrCrn(STACK_NAME, ACCOUNT_ID)).thenReturn(stackView);
        Template template = new Template();
        template.setInstanceStorageCount(1);

        StackDeleteVolumesRequest stackDeleteVolumesRequest = new StackDeleteVolumesRequest();
        stackDeleteVolumesRequest.setStackId(1L);
        stackDeleteVolumesRequest.setGroup("COMPUTE");

        underTest.putDeleteVolumesInWorkspace(STACK_NAME, ACCOUNT_ID, stackDeleteVolumesRequest);

        verify(clusterCommonService).putDeleteVolumes("CRN", stackDeleteVolumesRequest);
    }

    @Test
    public void testPutDeleteVolumesInWorkspaceFailure() {
        Stack stack = mock(Stack.class);
        StackView stackView = mock(StackView.class);
        when(stackView.getId()).thenReturn(STACK_ID);
        when(stackView.getResourceCrn()).thenReturn("CRN");
        when(stackService.getByIdWithLists(STACK_ID)).thenReturn(stack);
        when(stackDtoService.getStackViewByNameOrCrn(STACK_NAME, ACCOUNT_ID)).thenReturn(stackView);
        Template template = new Template();
        template.setInstanceStorageCount(1);
        doThrow(new BadRequestException("Deleting volumes is not supported on MOCK cloudplatform")).when(verticalScalingValidatorService)
                .validateProviderForDelete(any(), anyString(), eq(false));
        StackDeleteVolumesRequest stackDeleteVolumesRequest = new StackDeleteVolumesRequest();
        stackDeleteVolumesRequest.setStackId(1L);
        stackDeleteVolumesRequest.setGroup("COMPUTE");

        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.putDeleteVolumesInWorkspace(STACK_NAME,
                ACCOUNT_ID, stackDeleteVolumesRequest));

        assertEquals("Deleting volumes is not supported on MOCK cloudplatform", exception.getMessage());
    }

    private InstanceGroup instanceGroup(String name, String instanceType, Template template) {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(name);
        template.setInstanceType(instanceType);
        instanceGroup.setTemplate(template);
        return instanceGroup;
    }
}
