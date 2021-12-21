package com.sequenceiq.cloudbreak.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackImageChangeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkScaleV4Request;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.common.ScalingHardLimitsService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.controller.validation.network.MultiAzValidator;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackScaleV4RequestToUpdateStackV4RequestConverter;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterCache;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackOperationService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.service.FlowCancelService;

@ExtendWith(MockitoExtension.class)
class StackCommonServiceTest {

    private static final long WORKSPACE_ID = 1L;

    private static final long STACK_ID = 2L;

    private static final NameOrCrn STACK_NAME = NameOrCrn.ofName("stackName");

    private static final NameOrCrn STACK_CRN =
            NameOrCrn.ofCrn("crn:cdp:datahub:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:cluster:6b5a9aa7-223a-4d6a-93ca-27627be773b5");

    private static final String SUBNET_ID = "aSubnetId";

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private StackService stackService;

    @Mock
    private StackOperationService stackOperationService;

    @Mock
    private UserService userService;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private CloudbreakEventService eventService;

    @Mock
    private MultiAzValidator multiAzValidator;

    @Mock
    private StackScaleV4RequestToUpdateStackV4RequestConverter stackScaleV4RequestToUpdateStackV4RequestConverter;

    @Mock
    private CloudParameterCache cloudParameterCache;

    @Mock
    private NodeCountLimitValidator nodeCountLimitValidator;

    @Mock
    private ScalingHardLimitsService scalingHardLimitsService;

    @Mock
    private FlowCancelService flowCancelService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private StackUpdater stackUpdater;

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
        Stack stack = new Stack();
        stack.setType(StackType.DATALAKE);
        when(stackService.findStackByNameOrCrnAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.of(stack));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.deleteInstanceInWorkspace(STACK_NAME, WORKSPACE_ID, "node1", true));

        assertEquals("node1 is a node of a data lake cluster, therefore it's not allowed to delete it.", exception.getMessage());
        verifyNoInteractions(stackOperationService);
    }

    @Test
    public void testDeleteInstanceFromDataHub() {
        Stack stack = new Stack();
        stack.setType(StackType.WORKLOAD);
        when(stackService.findStackByNameOrCrnAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.of(stack));

        underTest.deleteInstanceInWorkspace(STACK_NAME, WORKSPACE_ID, "node1", true);

        verify(stackOperationService).removeInstance(stack, "node1", true);
    }

    @Test
    public void testThrowsExceptionWhenDeleteInstancesFromDataLake() {
        Stack stack = new Stack();
        stack.setType(StackType.DATALAKE);
        when(stackService.findStackByNameOrCrnAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.of(stack));

        Set<String> nodes = new LinkedHashSet<>();
        nodes.add("node1");
        nodes.add("node2");

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.deleteMultipleInstancesInWorkspace(STACK_NAME, WORKSPACE_ID, nodes, true));

        assertEquals("node1, node2 are nodes of a data lake cluster, therefore it's not allowed to delete them.", exception.getMessage());
        verifyNoInteractions(stackOperationService);
    }

    @Test
    public void testDeleteInstancesFromDataHub() {
        Stack stack = new Stack();
        stack.setType(StackType.WORKLOAD);
        when(stackService.findStackByNameOrCrnAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.of(stack));

        Set<String> nodes = new LinkedHashSet<>();
        nodes.add("node1");
        nodes.add("node2");

        underTest.deleteMultipleInstancesInWorkspace(STACK_NAME, WORKSPACE_ID, nodes, true);

        verify(stackOperationService).removeInstances(stack, nodes, true);
    }

    @Test
    public void testCancelInWorkspace() {
        when(stackService.getIdByNameOrCrnInWorkspace(STACK_NAME, WORKSPACE_ID)).thenReturn(1L);
        doNothing().when(flowCancelService).cancelRunningFlows(1L);

        underTest.cancelInWorkspace(STACK_NAME, WORKSPACE_ID);

        verify(stackService).getIdByNameOrCrnInWorkspace(STACK_NAME, WORKSPACE_ID);
        verify(flowCancelService).cancelRunningFlows(1L);
        verify(clusterService).updateClusterStatusByStackId(1L, DetailedStackStatus.AVAILABLE,
                "fake update after cancelling the running flows");
        verify(stackUpdater).updateStackStatus(1L, DetailedStackStatus.AVAILABLE,
                "fake update after cancelling the running flows");
    }

    @Test
    public void testStartInstancesInDefaultWorkspace() {
        Stack stack = new Stack();
        stack.setType(StackType.WORKLOAD);

        when(stackService.getByCrn(STACK_CRN.getCrn())).thenReturn(stack);

        CloudbreakUser cloudbreakUser = mock(CloudbreakUser.class);
        when(cloudbreakUser.getUserCrn()).thenReturn("crn:cdp:" + Crn.Service.AUTOSCALE.getName() + ":us-west-1:altus:user:__internal__actor__");
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(cloudbreakUser);

        UpdateStackV4Request updateStackV4Request = new UpdateStackV4Request();
        updateStackV4Request.setWithClusterEvent(true);
        InstanceGroupAdjustmentV4Request instanceGroupAdjustmentV4Request = new InstanceGroupAdjustmentV4Request();
        instanceGroupAdjustmentV4Request.setInstanceGroup("instanceGroup");
        instanceGroupAdjustmentV4Request.setScalingAdjustment(5);
        updateStackV4Request.setInstanceGroupAdjustment(instanceGroupAdjustmentV4Request);

        // Regular flow
        underTest.putStartInstancesInDefaultWorkspace(STACK_CRN.getCrn(), updateStackV4Request, ScalingStrategy.STOPSTART_FALLBACK_TO_REGULAR);
        verify(stackOperationService).updateNodeCountStartInstances(stack, updateStackV4Request.getInstanceGroupAdjustment(),
                true, ScalingStrategy.STOPSTART_FALLBACK_TO_REGULAR);

        // Null scaling strategy
        underTest.putStartInstancesInDefaultWorkspace(STACK_CRN.getCrn(), updateStackV4Request, null);
        verify(stackOperationService).updateNodeCountStartInstances(stack, updateStackV4Request.getInstanceGroupAdjustment(),
                true, ScalingStrategy.STOPSTART);

        // Status is set - Bad Request
        updateStackV4Request.setStatus(StatusRequest.FULL_SYNC);
        assertThrows(BadRequestException.class, () -> underTest.putStartInstancesInDefaultWorkspace(STACK_CRN.getCrn(), updateStackV4Request, null));
    }

    @Test
    public void testPutScalingInWorkspaceWhenVariantIsNotSupportedForMultiAzButAzHasBeenPreferredInTheRequest() {
        Stack stack = new Stack();
        String variant = AwsConstants.AwsVariant.AWS_VARIANT.name();
        stack.setPlatformVariant(variant);
        when(stackService.getByNameOrCrnInWorkspace(STACK_NAME, WORKSPACE_ID)).thenReturn(stack);
        when(multiAzValidator.supportedVariant(variant)).thenReturn(Boolean.FALSE);
        NetworkScaleV4Request networkScaleV4Request = new NetworkScaleV4Request();
        networkScaleV4Request.setPreferredSubnetIds(List.of(SUBNET_ID));
        StackScaleV4Request updateRequest = new StackScaleV4Request();
        updateRequest.setStackNetworkScaleV4Request(networkScaleV4Request);

        Assertions.assertThrows(BadRequestException.class, () -> underTest.putScalingInWorkspace(STACK_NAME, WORKSPACE_ID, updateRequest));

        Mockito.verify(multiAzValidator, times(1)).supportedVariant(variant);
        Mockito.verify(multiAzValidator, times(0)).collectSubnetIds(any());
    }

    @Test
    public void testPutScalingInWorkspaceWhenThereIsPreferredAzAndStackProvisionedToASingleSubnet() {
        Stack stack = new Stack();
        String variant = AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.name();
        stack.setPlatformVariant(variant);
        when(stackService.getByNameOrCrnInWorkspace(STACK_NAME, WORKSPACE_ID)).thenReturn(stack);
        when(multiAzValidator.supportedVariant(variant)).thenReturn(Boolean.TRUE);
        when(multiAzValidator.collectSubnetIds(any())).thenReturn(Set.of(SUBNET_ID));
        NetworkScaleV4Request networkScaleV4Request = new NetworkScaleV4Request();
        networkScaleV4Request.setPreferredSubnetIds(List.of(SUBNET_ID));
        StackScaleV4Request updateRequest = new StackScaleV4Request();
        updateRequest.setStackNetworkScaleV4Request(networkScaleV4Request);

        Assertions.assertThrows(BadRequestException.class, () -> underTest.putScalingInWorkspace(STACK_NAME, WORKSPACE_ID, updateRequest));

        Mockito.verify(multiAzValidator, times(1)).supportedVariant(variant);
        Mockito.verify(multiAzValidator, times(1)).collectSubnetIds(any());
    }

    @Test
    public void testPutScalingInWorkspaceWhenThereIsPreferredAzAndStackProvisionedInMultipleSubnetButScalingIsNotSupportedOnPlatform() {
        Stack stack = new Stack();
        stack.setCloudPlatform(AwsConstants.AWS_PLATFORM.value());
        String variant = AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.name();
        stack.setPlatformVariant(variant);
        when(stackService.getByNameOrCrnInWorkspace(STACK_NAME, WORKSPACE_ID)).thenReturn(stack);
        when(multiAzValidator.supportedVariant(variant)).thenReturn(Boolean.TRUE);
        when(multiAzValidator.collectSubnetIds(any())).thenReturn(Set.of(SUBNET_ID, "anotherSubnetId"));
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

        Assertions.assertThrows(BadRequestException.class, () -> underTest.putScalingInWorkspace(STACK_NAME, WORKSPACE_ID, updateRequest));

        Mockito.verify(multiAzValidator, times(1)).supportedVariant(variant);
        Mockito.verify(multiAzValidator, times(1)).collectSubnetIds(any());
    }
}
