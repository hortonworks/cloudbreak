package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.AVAILABLE;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.freeipa.flow.chain.FlowChainTriggers.REPAIR_TRIGGER_EVENT;
import static com.sequenceiq.freeipa.flow.instance.reboot.RebootEvent.REBOOT_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.CreateFreeIpaV1Response;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.HealthDetailsFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.NodeHealthDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.reboot.RebootInstancesRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rebuild.RebuildRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.repair.RepairInstancesRequest;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.freeipa.converter.operation.OperationToOperationStatusConverter;
import com.sequenceiq.freeipa.converter.stack.StackToCreateFreeIpaRequestConverter;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.flow.freeipa.repair.event.RepairEvent;
import com.sequenceiq.freeipa.flow.instance.InstanceEvent;
import com.sequenceiq.freeipa.flow.stack.termination.action.TerminationService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;

@ExtendWith(MockitoExtension.class)
class RepairInstancesServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String ENVIRONMENT_ID1 = "crn:cdp:environments:us-west-1:f39af961-e0ce-4f79-826c-45502efb9ca3:environment:12345-6789";

    private static final String ENVIRONMENT_ID2 = "crn:cdp:environments:us-west-1:f39af961-e0ce-4f79-826c-45502efb9ca3:environment:98765-4321";

    private static final String FREEIPA_CRN = "crn:cdp:freeipa:us-west-1:f39af961-e0ce-4f79-826c-45502efb9ca3:environment:11111-2222";

    private static final String ACCOUNT_ID = "accountId";

    private static Stack stack1;

    private static Stack stack2;

    @Mock
    private StackService stackService;

    @Mock
    private FreeIpaFlowManager flowManager;

    @Mock
    private FreeIpaStackHealthDetailsService healthDetailsService;

    @Mock
    private OperationService operationService;

    @Mock
    private OperationToOperationStatusConverter operationToOperationStatusConverter;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private FreeIpaCreationService freeIpaCreationService;

    @Mock
    private StackToCreateFreeIpaRequestConverter stackToCreateFreeIpaRequestConverter;

    @Mock
    private TerminationService terminationService;

    @InjectMocks
    private RepairInstancesService underTest;

    @BeforeAll
    public static void init() {
        stack1 = new Stack();
        stack1.setResourceCrn(ENVIRONMENT_ID1);
        stack1.setEnvironmentCrn(ENVIRONMENT_ID1);
        InstanceGroup instanceGroup = new InstanceGroup();
        stack1.getInstanceGroups().add(instanceGroup);
        instanceGroup.setInstanceGroupType(InstanceGroupType.MASTER);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceGroup.setInstanceMetaData(Sets.newHashSet(instanceMetaData));
        instanceMetaData.setDiscoveryFQDN("host.domain");
        instanceMetaData.setInstanceId("instance_1");

        stack2 = new Stack();
        stack2.setResourceCrn(ENVIRONMENT_ID2);
        stack2.setEnvironmentCrn(ENVIRONMENT_ID2);
        instanceGroup = new InstanceGroup();
        stack2.getInstanceGroups().add(instanceGroup);
        instanceGroup.setInstanceGroupType(InstanceGroupType.MASTER);
        instanceMetaData = new InstanceMetaData();
        instanceGroup.setInstanceMetaData(Sets.newHashSet(instanceMetaData));
        instanceMetaData.setDiscoveryFQDN("host1.domain");
        instanceMetaData.setInstanceId("instance_1");
        instanceMetaData = new InstanceMetaData();
        instanceGroup.getInstanceMetaData().add(instanceMetaData);
        instanceMetaData.setDiscoveryFQDN("host2.domain");
        instanceMetaData.setInstanceId("instance_2");
    }

    @Test
    void testRepairInstancesWithBadHealthInstance() {
        Stack stack = createStack(Status.UNHEALTHY, List.of(InstanceStatus.CREATED, InstanceStatus.UNREACHABLE));
        List<String> instanceIds = List.of("i-2");
        OperationStatus operationStatus = new OperationStatus();

        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_ID1, ACCOUNT_ID)).thenReturn(stack);
        when(healthDetailsService.getHealthDetails(ENVIRONMENT_ID1, ACCOUNT_ID))
                .thenReturn(createHealthDetails(InstanceStatus.CREATED, InstanceStatus.UNREACHABLE));
        Operation operation = createOperation();
        when(operationService.startOperation(any(), any(), any(), any())).thenReturn(operation);
        when(operationToOperationStatusConverter.convert(operation)).thenReturn(operationStatus);

        RepairInstancesRequest request = new RepairInstancesRequest();
        request.setForceRepair(false);
        request.setInstanceIds(instanceIds);
        request.setEnvironmentCrn(ENVIRONMENT_ID1);
        assertEquals(operationStatus, underTest.repairInstances(ACCOUNT_ID, request));

        ArgumentCaptor acAcceptable = ArgumentCaptor.forClass(Acceptable.class);
        verify(flowManager).notify(eq("REPAIR_TRIGGER_EVENT"), (Acceptable) acAcceptable.capture());
        assertTrue(acAcceptable.getValue() instanceof RepairEvent);
        RepairEvent repairEvent = (RepairEvent) acAcceptable.getValue();
        assertEquals(instanceIds, repairEvent.getRepairInstanceIds());
    }

    @Test
    void testRepairInstancesWithBadHealthInstanceTriggerFailure() {
        Stack stack = createStack(Status.UNHEALTHY, List.of(InstanceStatus.CREATED, InstanceStatus.UNREACHABLE));
        List<String> instanceIds = List.of("i-2");
        OperationStatus operationStatus = new OperationStatus();

        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_ID1, ACCOUNT_ID)).thenReturn(stack);
        when(healthDetailsService.getHealthDetails(ENVIRONMENT_ID1, ACCOUNT_ID))
                .thenReturn(createHealthDetails(InstanceStatus.CREATED, InstanceStatus.UNREACHABLE));
        Operation operation = createOperation();
        when(operationService.startOperation(any(), any(), any(), any())).thenReturn(operation);
        Operation failedOp = createOperation();
        failedOp.setStatus(OperationState.FAILED);
        when(operationService.failOperation(ACCOUNT_ID, operation.getOperationId(), "Couldn't start Freeipa repair flow: bumm"))
                .thenReturn(failedOp);
        when(operationToOperationStatusConverter.convert(failedOp)).thenReturn(operationStatus);
        ArgumentCaptor acAcceptable = ArgumentCaptor.forClass(Acceptable.class);
        when(flowManager.notify(eq("REPAIR_TRIGGER_EVENT"), (Acceptable) acAcceptable.capture())).thenThrow(new RuntimeException("bumm"));

        RepairInstancesRequest request = new RepairInstancesRequest();
        request.setForceRepair(false);
        request.setInstanceIds(instanceIds);
        request.setEnvironmentCrn(ENVIRONMENT_ID1);
        assertEquals(operationStatus, underTest.repairInstances(ACCOUNT_ID, request));
        assertTrue(acAcceptable.getValue() instanceof RepairEvent);
        RepairEvent repairEvent = (RepairEvent) acAcceptable.getValue();
        assertEquals(instanceIds, repairEvent.getRepairInstanceIds());
    }

    @Test
    void testRepairInstancesWithGoodInstancesShouldThrowException() {
        Stack stack = createStack(Status.AVAILABLE, List.of(InstanceStatus.CREATED, InstanceStatus.CREATED));
        List<String> instanceIds = List.of("i-2");

        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_ID1, ACCOUNT_ID)).thenReturn(stack);
        when(healthDetailsService.getHealthDetails(ENVIRONMENT_ID1, ACCOUNT_ID))
                .thenReturn(createHealthDetails(InstanceStatus.CREATED, InstanceStatus.CREATED));

        RepairInstancesRequest request = new RepairInstancesRequest();
        request.setForceRepair(false);
        request.setInstanceIds(instanceIds);
        request.setEnvironmentCrn(ENVIRONMENT_ID1);
        assertThrows(NotFoundException.class, () -> underTest.repairInstances(ACCOUNT_ID, request));
    }

    @Test
    void testRepairInstancesWithForce() {
        Stack stack = createStack(Status.AVAILABLE, List.of(InstanceStatus.CREATED, InstanceStatus.CREATED));
        List<String> instanceIds = List.of("i-2");
        OperationStatus operationStatus = new OperationStatus();

        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_ID1, ACCOUNT_ID)).thenReturn(stack);
        when(operationService.startOperation(any(), any(), any(), any())).thenReturn(createOperation());
        when(operationToOperationStatusConverter.convert(any())).thenReturn(operationStatus);

        RepairInstancesRequest request = new RepairInstancesRequest();
        request.setForceRepair(true);
        request.setInstanceIds(instanceIds);
        request.setEnvironmentCrn(ENVIRONMENT_ID1);
        assertEquals(operationStatus, underTest.repairInstances(ACCOUNT_ID, request));

        ArgumentCaptor acAcceptable = ArgumentCaptor.forClass(Acceptable.class);
        verify(flowManager).notify(eq("REPAIR_TRIGGER_EVENT"), (Acceptable) acAcceptable.capture());
        assertTrue(acAcceptable.getValue() instanceof RepairEvent);
        RepairEvent repairEvent = (RepairEvent) acAcceptable.getValue();
        assertEquals(instanceIds, repairEvent.getRepairInstanceIds());
    }

    @Test
    void testRepairInstancesWithNoneSpecified() {
        Stack stack = createStack(Status.UNHEALTHY, List.of(InstanceStatus.CREATED, InstanceStatus.UNREACHABLE));
        List<String> instanceIds = List.of("i-2");
        OperationStatus operationStatus = new OperationStatus();

        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_ID1, ACCOUNT_ID)).thenReturn(stack);
        when(healthDetailsService.getHealthDetails(ENVIRONMENT_ID1, ACCOUNT_ID))
                .thenReturn(createHealthDetails(InstanceStatus.CREATED, InstanceStatus.UNREACHABLE));
        when(operationService.startOperation(any(), any(), any(), any())).thenReturn(createOperation());
        when(operationToOperationStatusConverter.convert(any())).thenReturn(operationStatus);

        RepairInstancesRequest request = new RepairInstancesRequest();
        request.setEnvironmentCrn(ENVIRONMENT_ID1);
        assertEquals(operationStatus, underTest.repairInstances(ACCOUNT_ID, request));

        ArgumentCaptor acAcceptable = ArgumentCaptor.forClass(Acceptable.class);
        verify(flowManager).notify(eq("REPAIR_TRIGGER_EVENT"), (Acceptable) acAcceptable.capture());
        assertTrue(acAcceptable.getValue() instanceof RepairEvent);
        RepairEvent repairEvent = (RepairEvent) acAcceptable.getValue();
        assertEquals(instanceIds, repairEvent.getRepairInstanceIds());
    }

    @Test
    void testRepairThrowsWhenOnlyBadInstancesRemain() {
        Stack stack = createStack(Status.UNHEALTHY, List.of(InstanceStatus.UNHEALTHY, InstanceStatus.DELETED_ON_PROVIDER_SIDE, InstanceStatus.CREATED));

        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_ID1, ACCOUNT_ID)).thenReturn(stack);

        RepairInstancesRequest request = new RepairInstancesRequest();
        request.setForceRepair(true);
        request.setEnvironmentCrn(ENVIRONMENT_ID1);
        request.setInstanceIds(List.of("i-1"));
        assertThrows(BadRequestException.class, () -> {
            underTest.repairInstances(ACCOUNT_ID, request);
        });
    }

    @Test
    void testRepairThrowsWhenOneBadInstancesRemain() {
        Stack stack = createStack(Status.UNHEALTHY, List.of(InstanceStatus.UNREACHABLE, InstanceStatus.UNREACHABLE));

        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_ID1, ACCOUNT_ID)).thenReturn(stack);
        when(healthDetailsService.getHealthDetails(ENVIRONMENT_ID1, ACCOUNT_ID))
                .thenReturn(createHealthDetails(InstanceStatus.UNREACHABLE, InstanceStatus.UNREACHABLE));

        RepairInstancesRequest request = new RepairInstancesRequest();
        request.setEnvironmentCrn(ENVIRONMENT_ID1);
        assertThrows(BadRequestException.class, () -> {
            underTest.repairInstances(ACCOUNT_ID, request);
        });
    }

    @Test
    void testRepairWithForceThrowsWhenNoInstanceIdsAreProvided() {
        Stack stack = createStack(Status.AVAILABLE, List.of(InstanceStatus.CREATED, InstanceStatus.CREATED));

        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_ID1, ACCOUNT_ID)).thenReturn(stack);

        RepairInstancesRequest request = new RepairInstancesRequest();
        request.setForceRepair(true);
        request.setEnvironmentCrn(ENVIRONMENT_ID1);
        assertThrows(BadRequestException.class, () -> {
            underTest.repairInstances(ACCOUNT_ID, request);
        });
    }

    @Test
    void testRepairForceWhenOnlyBadInstancesRemain() {
        Stack stack = createStack(Status.UNREACHABLE, List.of(InstanceStatus.UNREACHABLE, InstanceStatus.UNREACHABLE));
        List<String> instanceIds = List.of("i-2");
        OperationStatus operationStatus = new OperationStatus();

        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_ID1, ACCOUNT_ID)).thenReturn(stack);
        when(operationService.startOperation(any(), any(), any(), any())).thenReturn(createOperation());
        when(operationToOperationStatusConverter.convert(any())).thenReturn(operationStatus);

        RepairInstancesRequest request = new RepairInstancesRequest();
        request.setForceRepair(true);
        request.setInstanceIds(instanceIds);
        request.setEnvironmentCrn(ENVIRONMENT_ID1);
        assertEquals(operationStatus, underTest.repairInstances(ACCOUNT_ID, request));

        ArgumentCaptor acAcceptable = ArgumentCaptor.forClass(Acceptable.class);
        verify(flowManager).notify(eq("REPAIR_TRIGGER_EVENT"), (Acceptable) acAcceptable.capture());
        assertTrue(acAcceptable.getValue() instanceof RepairEvent);
        RepairEvent repairEvent = (RepairEvent) acAcceptable.getValue();
        assertEquals(instanceIds, repairEvent.getRepairInstanceIds());
    }

    @Test
    void testRepairThrowsWhenOnlyDeletedInstancesAndForceIsCalledRemain() {
        Stack stack = createStack(Status.DELETED_ON_PROVIDER_SIDE, List.of(InstanceStatus.DELETED_BY_PROVIDER, InstanceStatus.DELETED_BY_PROVIDER));

        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_ID1, ACCOUNT_ID)).thenReturn(stack);
        when(healthDetailsService.getHealthDetails(ENVIRONMENT_ID1, ACCOUNT_ID))
                .thenReturn(createHealthDetails(InstanceStatus.UNREACHABLE, InstanceStatus.UNREACHABLE));

        RepairInstancesRequest request = new RepairInstancesRequest();
        request.setEnvironmentCrn(ENVIRONMENT_ID1);
        assertThrows(BadRequestException.class, () -> {
            underTest.repairInstances(ACCOUNT_ID, request);
        });
    }

    @Test
    public void testBasicSuccessReboot() throws Exception {
        OperationStatus operationStatus = new OperationStatus();
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(stack1);
        Mockito.when(healthDetailsService.getHealthDetails(any(), any())).thenReturn(getMockDetails1());
        Mockito.when(operationService.startOperation(any(), any(), any(), any())).thenReturn(createOperation());
        Mockito.when(operationToOperationStatusConverter.convert(any())).thenReturn(operationStatus);
        RebootInstancesRequest rebootInstancesRequest = new RebootInstancesRequest();
        rebootInstancesRequest.setEnvironmentCrn(ENVIRONMENT_ID1);
        assertEquals(operationStatus, underTest.rebootInstances(ACCOUNT_ID, rebootInstancesRequest));

        ArgumentCaptor<InstanceEvent> terminationEventArgumentCaptor = ArgumentCaptor.forClass(InstanceEvent.class);
        verify(flowManager, times(1)).notify(eq(REBOOT_EVENT.event()), terminationEventArgumentCaptor.capture());
    }

    @Test
    public void testBasicSuccessRebootTriggerFailed() throws Exception {
        OperationStatus operationStatus = new OperationStatus();
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(stack1);
        Mockito.when(healthDetailsService.getHealthDetails(any(), any())).thenReturn(getMockDetails1());
        Operation operation = createOperation();
        Mockito.when(operationService.startOperation(any(), any(), any(), any())).thenReturn(operation);
        Operation failedOp = createOperation();
        failedOp.setStatus(OperationState.FAILED);
        when(operationService.failOperation(ACCOUNT_ID, operation.getOperationId(), "Couldn't start Freeipa reboot flow: bumm"))
                .thenReturn(failedOp);
        when(operationToOperationStatusConverter.convert(failedOp)).thenReturn(operationStatus);
        RebootInstancesRequest rebootInstancesRequest = new RebootInstancesRequest();
        rebootInstancesRequest.setEnvironmentCrn(ENVIRONMENT_ID1);
        ArgumentCaptor<InstanceEvent> terminationEventArgumentCaptor = ArgumentCaptor.forClass(InstanceEvent.class);
        when(flowManager.notify(eq(REBOOT_EVENT.event()), terminationEventArgumentCaptor.capture()))
                .thenThrow(new RuntimeException("bumm"));

        assertEquals(operationStatus, underTest.rebootInstances(ACCOUNT_ID, rebootInstancesRequest));
    }

    @Test
    public void testInstancesSuccessReboot() throws Exception {
        OperationStatus operationStatus = new OperationStatus();
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(stack1);
        Mockito.when(healthDetailsService.getHealthDetails(any(), any())).thenReturn(getMockDetails1());
        Mockito.when(operationService.startOperation(any(), any(), any(), any())).thenReturn(createOperation());
        Mockito.when(operationToOperationStatusConverter.convert(any())).thenReturn(operationStatus);
        RebootInstancesRequest rebootInstancesRequest = new RebootInstancesRequest();
        rebootInstancesRequest.setEnvironmentCrn(ENVIRONMENT_ID1);
        rebootInstancesRequest.setInstanceIds(Arrays.asList("instance_1"));
        assertEquals(operationStatus, underTest.rebootInstances(ACCOUNT_ID, rebootInstancesRequest));

        ArgumentCaptor<InstanceEvent> terminationEventArgumentCaptor = ArgumentCaptor.forClass(InstanceEvent.class);
        verify(flowManager, times(1)).notify(eq(REBOOT_EVENT.event()), terminationEventArgumentCaptor.capture());
    }

    @Test
    public void testInvalidInstancesReboot() throws Exception {
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(stack1);
        Mockito.when(healthDetailsService.getHealthDetails(any(), any())).thenReturn(getMockDetails1());
        RebootInstancesRequest rebootInstancesRequest = new RebootInstancesRequest();
        rebootInstancesRequest.setEnvironmentCrn(ENVIRONMENT_ID1);
        rebootInstancesRequest.setInstanceIds(Arrays.asList("bad_instance"));
        Exception expected = assertThrows(BadRequestException.class, () -> {
            underTest.rebootInstances(ACCOUNT_ID, rebootInstancesRequest);
        });
        Assert.assertTrue(expected.getLocalizedMessage().equals("Invalid instanceIds in request bad_instance."));
    }

    @Test
    public void testForceInstancesSuccessReboot() throws Exception {
        OperationStatus operationStatus = new OperationStatus();
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(stack1);
        Mockito.when(operationService.startOperation(any(), any(), any(), any())).thenReturn(createOperation());
        Mockito.when(operationToOperationStatusConverter.convert(any())).thenReturn(operationStatus);
        RebootInstancesRequest rebootInstancesRequest = new RebootInstancesRequest();
        rebootInstancesRequest.setEnvironmentCrn(ENVIRONMENT_ID1);
        rebootInstancesRequest.setInstanceIds(Arrays.asList("instance_1"));
        rebootInstancesRequest.setForceReboot(true);
        assertEquals(operationStatus, underTest.rebootInstances(ACCOUNT_ID, rebootInstancesRequest));

        ArgumentCaptor<InstanceEvent> terminationEventArgumentCaptor = ArgumentCaptor.forClass(InstanceEvent.class);
        verify(flowManager, times(1)).notify(eq(REBOOT_EVENT.event()), terminationEventArgumentCaptor.capture());
    }

    @Test
    public void testNonForceAvailableInstanceReboot() throws Exception {
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(stack2);
        Mockito.when(healthDetailsService.getHealthDetails(any(), any())).thenReturn(getMockDetails2());
        RebootInstancesRequest rebootInstancesRequest = new RebootInstancesRequest();
        rebootInstancesRequest.setEnvironmentCrn(ENVIRONMENT_ID2);
        rebootInstancesRequest.setInstanceIds(Arrays.asList("instance_1"));
        Exception expected = assertThrows(NotFoundException.class, () -> {
            underTest.rebootInstances(ACCOUNT_ID, rebootInstancesRequest);
        });
        Assert.assertTrue(expected.getLocalizedMessage().equals("No unhealthy instances to reboot. You can try to use the force option to enforce " +
                "the repair process."));
        ArgumentCaptor<InstanceEvent> terminationEventArgumentCaptor = ArgumentCaptor.forClass(InstanceEvent.class);
        verify(flowManager, times(0)).notify(eq(REBOOT_EVENT.event()), terminationEventArgumentCaptor.capture());
    }

    @Test
    public void testNonForceMultiInstanceReboot() throws Exception {
        OperationStatus operationStatus = new OperationStatus();
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(stack2);
        Mockito.when(healthDetailsService.getHealthDetails(any(), any())).thenReturn(getMockDetails2());
        Mockito.when(operationService.startOperation(any(), any(), any(), any())).thenReturn(createOperation());
        Mockito.when(operationToOperationStatusConverter.convert(any())).thenReturn(operationStatus);
        RebootInstancesRequest rebootInstancesRequest = new RebootInstancesRequest();
        rebootInstancesRequest.setEnvironmentCrn(ENVIRONMENT_ID2);
        assertEquals(operationStatus, underTest.rebootInstances(ACCOUNT_ID, rebootInstancesRequest));
        ArgumentCaptor<InstanceEvent> terminationEventArgumentCaptor = ArgumentCaptor.forClass(InstanceEvent.class);
        verify(flowManager, times(1)).notify(eq(REBOOT_EVENT.event()), terminationEventArgumentCaptor.capture());
    }

    @Test
    public void testForceMultiInstanceReboot() throws Exception {
        OperationStatus operationStatus = new OperationStatus();
        Mockito.when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(stack2);
        Mockito.when(operationService.startOperation(any(), any(), any(), any())).thenReturn(createOperation());
        Mockito.when(operationToOperationStatusConverter.convert(any())).thenReturn(operationStatus);
        RebootInstancesRequest rebootInstancesRequest = new RebootInstancesRequest();
        rebootInstancesRequest.setEnvironmentCrn(ENVIRONMENT_ID2);
        rebootInstancesRequest.setForceReboot(true);
        assertEquals(operationStatus, underTest.rebootInstances(ACCOUNT_ID, rebootInstancesRequest));
        ArgumentCaptor<InstanceEvent> terminationEventArgumentCaptor = ArgumentCaptor.forClass(InstanceEvent.class);
        verify(flowManager, times(1)).notify(eq(REBOOT_EVENT.event()), terminationEventArgumentCaptor.capture());
    }

    @Test
    public void testRepairAndAutodetectBadInstances() throws Exception {
        Stack stack = createStack(Status.UNHEALTHY, List.of(InstanceStatus.DELETED_ON_PROVIDER_SIDE, InstanceStatus.CREATED));
        OperationStatus operationStatus = new OperationStatus();
        RepairInstancesRequest repairInstancesRequest = new RepairInstancesRequest();
        repairInstancesRequest.setEnvironmentCrn(ENVIRONMENT_ID1);
        repairInstancesRequest.setForceRepair(false);

        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_ID1, ACCOUNT_ID)).thenReturn(stack);
        when(healthDetailsService.getHealthDetails(ENVIRONMENT_ID1, ACCOUNT_ID))
                .thenReturn(createHealthDetails(InstanceStatus.DELETED_ON_PROVIDER_SIDE, InstanceStatus.CREATED));
        when(operationService.startOperation(any(), any(), any(), any())).thenReturn(createOperation());
        when(operationToOperationStatusConverter.convert(any())).thenReturn(operationStatus);

        assertEquals(operationStatus, underTest.repairInstances(ACCOUNT_ID, repairInstancesRequest));

        verify(flowManager, times(1)).notify(eq(REPAIR_TRIGGER_EVENT), any());
    }

    @Test
    public void testRepairAndAutodetectWith2InstancesWithNoInstanceIds() throws Exception {
        Stack stack = createStack(Status.UNHEALTHY, List.of(InstanceStatus.DELETED_ON_PROVIDER_SIDE, InstanceStatus.CREATED), 2);
        OperationStatus operationStatus = new OperationStatus();
        RepairInstancesRequest repairInstancesRequest = new RepairInstancesRequest();
        repairInstancesRequest.setEnvironmentCrn(ENVIRONMENT_ID1);
        repairInstancesRequest.setForceRepair(false);

        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_ID1, ACCOUNT_ID)).thenReturn(stack);
        when(healthDetailsService.getHealthDetails(ENVIRONMENT_ID1, ACCOUNT_ID))
                .thenReturn(createHealthDetails(InstanceStatus.DELETED_ON_PROVIDER_SIDE, InstanceStatus.CREATED));
        when(operationService.startOperation(any(), any(), any(), any())).thenReturn(createOperation());
        when(operationToOperationStatusConverter.convert(any())).thenReturn(operationStatus);

        assertEquals(operationStatus, underTest.repairInstances(ACCOUNT_ID, repairInstancesRequest));

        verify(flowManager, times(1)).notify(eq(REPAIR_TRIGGER_EVENT), any());
    }

    @Test
    public void testRebuildThrowsWhenSourceStackIsRunning() throws Exception {
        Stack stack = createStack(Status.AVAILABLE, List.of(InstanceStatus.CREATED, InstanceStatus.CREATED), 2);

        when(stackService.getByCrnAndAccountIdEvenIfTerminated(ENVIRONMENT_ID1, ACCOUNT_ID, FREEIPA_CRN)).thenReturn(stack);
        when(stackService.findByEnvironmentCrnAndAccountId(ENVIRONMENT_ID1, ACCOUNT_ID)).thenReturn(Optional.of(stack));
        when(entitlementService.isFreeIpaRebuildEnabled(eq(ACCOUNT_ID))).thenReturn(true);

        RebuildRequest rebuildRequest = new RebuildRequest();
        rebuildRequest.setEnvironmentCrn(ENVIRONMENT_ID1);
        rebuildRequest.setSourceCrn(FREEIPA_CRN);

        assertThrows(BadRequestException.class, () -> underTest.rebuild(ACCOUNT_ID, rebuildRequest));
    }

    @Test
    public void testRebuildThrowsWhenNoEntitlement() throws Exception {
        Stack stack = createStack(Status.AVAILABLE, List.of(InstanceStatus.CREATED, InstanceStatus.CREATED), 2);

        when(entitlementService.isFreeIpaRebuildEnabled(eq(ACCOUNT_ID))).thenReturn(false);

        RebuildRequest rebuildRequest = new RebuildRequest();
        rebuildRequest.setEnvironmentCrn(ENVIRONMENT_ID1);
        rebuildRequest.setSourceCrn(FREEIPA_CRN);

        assertThrows(BadRequestException.class, () -> underTest.rebuild(ACCOUNT_ID, rebuildRequest));
    }

    @Test
    public void testRebuild() throws Exception {
        Stack stack = createStack(Status.DELETE_COMPLETED, List.of(InstanceStatus.TERMINATED, InstanceStatus.TERMINATED), 2);
        CreateFreeIpaRequest createFreeIpaRequest = new CreateFreeIpaRequest();
        CreateFreeIpaV1Response response = new CreateFreeIpaV1Response();

        when(stackService.getByCrnAndAccountIdEvenIfTerminated(ENVIRONMENT_ID1, ACCOUNT_ID, FREEIPA_CRN)).thenReturn(stack);
        when(stackService.findByEnvironmentCrnAndAccountId(ENVIRONMENT_ID1, ACCOUNT_ID)).thenReturn(Optional.empty());
        when(stackToCreateFreeIpaRequestConverter.convert(eq(stack))).thenReturn(createFreeIpaRequest);
        when(freeIpaCreationService.launchFreeIpa(eq(createFreeIpaRequest), eq(ACCOUNT_ID))).thenReturn(response);
        when(entitlementService.isFreeIpaRebuildEnabled(eq(ACCOUNT_ID))).thenReturn(true);

        RebuildRequest rebuildRequest = new RebuildRequest();
        rebuildRequest.setEnvironmentCrn(ENVIRONMENT_ID1);
        rebuildRequest.setSourceCrn(FREEIPA_CRN);

        assertEquals(response, underTest.rebuild(ACCOUNT_ID, rebuildRequest));
    }

    @Test
    public void testRenameStackIfNeededWhenNotTerminated() {
        Stack stack = createStack(AVAILABLE, List.of(InstanceStatus.TERMINATED, InstanceStatus.TERMINATED), 2);

        assertThrows(BadRequestException.class, () -> underTest.renameStackIfNeeded(stack));
    }

    @Test
    public void testRenameStackIfNeededWhenAlreadyRenamed() {
        Stack stack = createStack(DELETE_COMPLETED, List.of(InstanceStatus.TERMINATED, InstanceStatus.TERMINATED), 2);
        Long terminationTime = 1638311233640L;
        String stackName = "freeipa_" + terminationTime;

        stack.setName(stackName);
        stack.setTerminated(terminationTime);

        underTest.renameStackIfNeeded(stack);

        verify(terminationService, never()).finalizeTermination(any());
    }

    @Test
    public void testRenameStackIfNeededWhenNotAlreadyRenamed() {
        Stack stack = createStack(DELETE_COMPLETED, List.of(InstanceStatus.TERMINATED, InstanceStatus.TERMINATED), 2);
        Long terminationTime = 1638311233640L;
        String stackName = "freeipa";

        stack.setName(stackName);
        stack.setTerminated(terminationTime);

        underTest.renameStackIfNeeded(stack);

        verify(terminationService).finalizeTermination(STACK_ID);
    }

    @Test
    public void testRebuildThrowsWhenEntitlementIsDisabled() throws Exception {
        when(entitlementService.isFreeIpaRebuildEnabled(eq(ACCOUNT_ID))).thenReturn(false);

        RebuildRequest rebuildRequest = new RebuildRequest();
        rebuildRequest.setEnvironmentCrn(ENVIRONMENT_ID1);
        rebuildRequest.setSourceCrn(FREEIPA_CRN);

        assertThrows(BadRequestException.class, () -> underTest.rebuild(ACCOUNT_ID, rebuildRequest));
    }

    private HealthDetailsFreeIpaResponse getMockDetails1() {
        HealthDetailsFreeIpaResponse healthDetailsFreeIpaResponse = new HealthDetailsFreeIpaResponse();
        healthDetailsFreeIpaResponse.setCrn(ENVIRONMENT_ID1);
        healthDetailsFreeIpaResponse.setName("test");
        healthDetailsFreeIpaResponse.setStatus(Status.AVAILABLE);
        NodeHealthDetails nodeHealthDetails = new NodeHealthDetails();
        nodeHealthDetails.setInstanceId("instance_1");
        nodeHealthDetails.setStatus(InstanceStatus.TERMINATED);
        healthDetailsFreeIpaResponse.addNodeHealthDetailsFreeIpaResponses(nodeHealthDetails);
        return healthDetailsFreeIpaResponse;
    }

    private HealthDetailsFreeIpaResponse getMockDetails2() {
        HealthDetailsFreeIpaResponse healthDetailsFreeIpaResponse = new HealthDetailsFreeIpaResponse();
        healthDetailsFreeIpaResponse.setCrn(ENVIRONMENT_ID2);
        healthDetailsFreeIpaResponse.setName("test");
        healthDetailsFreeIpaResponse.setStatus(Status.AVAILABLE);
        NodeHealthDetails nodeHealthDetails = new NodeHealthDetails();
        nodeHealthDetails.setInstanceId("instance_1");
        nodeHealthDetails.setStatus(InstanceStatus.CREATED);
        healthDetailsFreeIpaResponse.addNodeHealthDetailsFreeIpaResponses(nodeHealthDetails);
        nodeHealthDetails = new NodeHealthDetails();
        nodeHealthDetails.setInstanceId("instance_2");
        nodeHealthDetails.setStatus(InstanceStatus.UNREACHABLE);
        healthDetailsFreeIpaResponse.addNodeHealthDetailsFreeIpaResponses(nodeHealthDetails);
        return healthDetailsFreeIpaResponse;
    }

    private Stack createStack(Status stackStatus, List<InstanceStatus> instanceStatuses) {
        return createStack(stackStatus, instanceStatuses, 0);
    }

    private Stack createStack(Status stackStatus, List<InstanceStatus> instanceStatuses, int requestedInstancesWithoutInstanceIds) {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setEnvironmentCrn(ENVIRONMENT_ID1);
        StackStatus s = new StackStatus();
        s.setStatus(stackStatus);
        stack.setStackStatus(s);
        int i = 1;
        Set<InstanceMetaData> instanceMetaDataSet = new HashSet<>();
        for (InstanceStatus instanceStatus : instanceStatuses) {
            InstanceMetaData instanceMetaData = new InstanceMetaData();
            instanceMetaData.setInstanceId("i-" + i);
            instanceMetaData.setInstanceStatus(instanceStatus);
            instanceMetaDataSet.add(instanceMetaData);
            i++;
        }
        for (i = 0; i < requestedInstancesWithoutInstanceIds; i++) {
            InstanceMetaData instanceMetaData = new InstanceMetaData();
            instanceMetaData.setInstanceStatus(InstanceStatus.REQUESTED);
            instanceMetaDataSet.add(instanceMetaData);
        }
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceMetaData(instanceMetaDataSet);
        instanceGroup.setNodeCount(instanceMetaDataSet.size());
        stack.setInstanceGroups(Set.of(instanceGroup));
        return stack;
    }

    private HealthDetailsFreeIpaResponse createHealthDetails(InstanceStatus status1, InstanceStatus status2) {
        HealthDetailsFreeIpaResponse response = new HealthDetailsFreeIpaResponse();
        NodeHealthDetails healthDetails1 = new NodeHealthDetails();
        healthDetails1.setInstanceId("i-1");
        healthDetails1.setStatus(status1);
        NodeHealthDetails healthDetails2 = new NodeHealthDetails();
        healthDetails2.setInstanceId("i-2");
        healthDetails2.setStatus(status2);
        response.setNodeHealthDetails(List.of(healthDetails1, healthDetails2));
        return response;
    }

    private Operation createOperation() {
        Operation operation = new Operation();
        operation.setId(1L);
        operation.setStatus(OperationState.RUNNING);
        return operation;
    }

}