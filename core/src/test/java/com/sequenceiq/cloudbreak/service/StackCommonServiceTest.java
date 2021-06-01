package com.sequenceiq.cloudbreak.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.UpdateStackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackImageChangeV4Request;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.ScalingHardLimitsService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackOperationService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
class StackCommonServiceTest {

    private static final long WORKSPACE_ID = 1L;

    private static final long STACK_ID = 2L;

    private static final NameOrCrn STACK_NAME = NameOrCrn.ofName("stackName");

    private static final NameOrCrn STACK_CRN = NameOrCrn.ofCrn("stackCrn");

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private StackService stackService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private StackOperationService stackOperationService;

    @Mock
    private ScalingHardLimitsService scalingHardLimitsService;

    @Mock
    private UserService userService;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

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
        when(imageCatalogService.get(WORKSPACE_ID, stackImageChangeRequest.getImageCatalogName())).thenReturn(catalog);
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
        when(stackService.getByNameOrCrnInWorkspace(STACK_NAME, WORKSPACE_ID)).thenReturn(stack);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.deleteInstanceInWorkspace(STACK_NAME, WORKSPACE_ID, "node1", true));

        assertEquals("node1 is a node of a data lake cluster, therefore it's not allowed to delete it.", exception.getMessage());
        verifyNoInteractions(stackOperationService);
    }

    @Test
    public void testDeleteInstanceFromDataHub() {
        Stack stack = new Stack();
        stack.setType(StackType.WORKLOAD);
        User user = new User();
        when(userService.getOrCreate(any())).thenReturn(user);
        when(stackService.getByNameOrCrnInWorkspace(STACK_NAME, WORKSPACE_ID)).thenReturn(stack);

        underTest.deleteInstanceInWorkspace(STACK_NAME, WORKSPACE_ID, "node1", true);

        verify(stackOperationService).removeInstance(stack, WORKSPACE_ID, "node1", true, user);
    }

    @Test
    public void testThrowsExceptionWhenDeleteInstancesFromDataLake() {
        Stack stack = new Stack();
        stack.setType(StackType.DATALAKE);
        when(stackService.getByNameOrCrnInWorkspace(STACK_NAME, WORKSPACE_ID)).thenReturn(stack);

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
        User user = new User();
        when(userService.getOrCreate(any())).thenReturn(user);
        when(stackService.getByNameOrCrnInWorkspace(STACK_NAME, WORKSPACE_ID)).thenReturn(stack);

        Set<String> nodes = new LinkedHashSet<>();
        nodes.add("node1");
        nodes.add("node2");

        underTest.deleteMultipleInstancesInWorkspace(STACK_NAME, WORKSPACE_ID, nodes, true);

        verify(stackOperationService).removeInstances(stack, WORKSPACE_ID, nodes, true, user);
    }

    @Test
    public void testPutInDefaultWorkspaceWhenScalingStepEntitledAndScalingBeyond200() {
        UpdateStackV4Request updateStackV4Request = new UpdateStackV4Request();
        InstanceGroupAdjustmentV4Request instanceGroupAdjustmentV4Request = new InstanceGroupAdjustmentV4Request();
        instanceGroupAdjustmentV4Request.setScalingAdjustment(250);
        updateStackV4Request.setInstanceGroupAdjustment(instanceGroupAdjustmentV4Request);

        Stack stack = new Stack();
        stack.setType(StackType.WORKLOAD);

        User user = new User();
        Tenant tenant = new Tenant();
        tenant.setName("accountId");
        user.setTenant(tenant);

        when(userService.getOrCreate(any())).thenReturn(user);
        when(stackService.getByCrn(STACK_CRN.getCrn())).thenReturn(stack);
        when(entitlementService.dataHubScalingStepSizeEnabled("accountId")).thenReturn(true);
        when(scalingHardLimitsService.getMaxUpscaleStepInNodeCountWhenScalingStepEntitled()).thenReturn(200);
        when(scalingHardLimitsService.isViolatingMaxUpscaleStepInNodeCount(anyInt(), anyInt())).thenCallRealMethod();

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.putInDefaultWorkspace(STACK_CRN.getCrn(), updateStackV4Request));

        assertEquals("Upscaling by more than 200 nodes is not supported", exception.getMessage());
        verifyNoInteractions(stackOperationService);
    }

    @Test
    public void testPutInDefaultWorkspaceWhenScalingStepEntitledAndValidScaling() {
        UpdateStackV4Request updateStackV4Request = new UpdateStackV4Request();
        InstanceGroupAdjustmentV4Request instanceGroupAdjustmentV4Request = new InstanceGroupAdjustmentV4Request();
        instanceGroupAdjustmentV4Request.setScalingAdjustment(150);
        updateStackV4Request.setInstanceGroupAdjustment(instanceGroupAdjustmentV4Request);

        Stack stack = new Stack();
        stack.setType(StackType.WORKLOAD);

        User user = new User();
        Tenant tenant = new Tenant();
        tenant.setName("accountId");
        user.setTenant(tenant);

        when(userService.getOrCreate(any())).thenReturn(user);
        when(stackService.getByCrn(STACK_CRN.getCrn())).thenReturn(stack);
        when(entitlementService.dataHubScalingStepSizeEnabled("accountId")).thenReturn(true);
        when(scalingHardLimitsService.getMaxUpscaleStepInNodeCountWhenScalingStepEntitled()).thenReturn(200);
        when(scalingHardLimitsService.isViolatingMaxUpscaleStepInNodeCount(anyInt(), anyInt())).thenCallRealMethod();

        underTest.putInDefaultWorkspace(STACK_CRN.getCrn(), updateStackV4Request);
        verify(stackOperationService).updateNodeCount(stack, instanceGroupAdjustmentV4Request, false);
    }

    @Test
    public void testPutInDefaultWorkspaceWhenScalingStepNotEntitledAndScalingBeyond100() {
        UpdateStackV4Request updateStackV4Request = new UpdateStackV4Request();
        InstanceGroupAdjustmentV4Request instanceGroupAdjustmentV4Request = new InstanceGroupAdjustmentV4Request();
        instanceGroupAdjustmentV4Request.setScalingAdjustment(101);
        updateStackV4Request.setInstanceGroupAdjustment(instanceGroupAdjustmentV4Request);

        Stack stack = new Stack();
        stack.setType(StackType.WORKLOAD);

        User user = new User();
        Tenant tenant = new Tenant();
        tenant.setName("accountId");
        user.setTenant(tenant);

        when(userService.getOrCreate(any())).thenReturn(user);
        when(stackService.getByCrn(STACK_CRN.getCrn())).thenReturn(stack);
        when(entitlementService.dataHubScalingStepSizeEnabled("accountId")).thenReturn(false);
        when(scalingHardLimitsService.getMaxUpscaleStepInNodeCount()).thenReturn(100);
        when(scalingHardLimitsService.isViolatingMaxUpscaleStepInNodeCount(anyInt(), anyInt())).thenCallRealMethod();

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.putInDefaultWorkspace(STACK_CRN.getCrn(), updateStackV4Request));
        assertEquals("Upscaling by more than 100 nodes is not supported", exception.getMessage());
    }

    @Test
    public void testPutInDefaultWorkspaceWhenScalingStepNotEntitledAndValidScaling() {
        UpdateStackV4Request updateStackV4Request = new UpdateStackV4Request();
        InstanceGroupAdjustmentV4Request instanceGroupAdjustmentV4Request = new InstanceGroupAdjustmentV4Request();
        instanceGroupAdjustmentV4Request.setScalingAdjustment(95);
        updateStackV4Request.setInstanceGroupAdjustment(instanceGroupAdjustmentV4Request);

        Stack stack = new Stack();
        stack.setType(StackType.WORKLOAD);

        User user = new User();
        Tenant tenant = new Tenant();
        tenant.setName("accountId");
        user.setTenant(tenant);

        when(userService.getOrCreate(any())).thenReturn(user);
        when(stackService.getByCrn(STACK_CRN.getCrn())).thenReturn(stack);
        when(entitlementService.dataHubScalingStepSizeEnabled("accountId")).thenReturn(false);
        when(scalingHardLimitsService.getMaxUpscaleStepInNodeCount()).thenReturn(100);
        when(scalingHardLimitsService.isViolatingMaxUpscaleStepInNodeCount(anyInt(), anyInt())).thenCallRealMethod();

        underTest.putInDefaultWorkspace(STACK_CRN.getCrn(), updateStackV4Request);
        verify(stackOperationService).updateNodeCount(stack, instanceGroupAdjustmentV4Request, false);
    }
}