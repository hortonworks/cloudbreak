package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityType.HA;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityType.NON_HA;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityType.TWO_NODE_BASED;
import static com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScaleEvent.STACK_VERTICALSCALE_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.common.api.type.OrchestratorType;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityInfo;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceTemplateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.DownscaleRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.DownscaleResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.ScalingPath;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.UpscaleRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.UpscaleResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleResponse;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.flow.chain.FlowChainTriggers;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleEvent;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.FreeIpaRollingVerticalScaleChainTriggerEvent;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScalingTriggerEvent;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;

@ExtendWith(MockitoExtension.class)
class FreeIpaScalingServiceTest {

    private static final String ACCOUNT_ID = "accountId";

    private static final String ENV_CRN = "envCrn";

    private static final String OPERATION_ID = "operationId";

    private static final String POLLABLE_ID = "pollableId";

    private static final String OPERATION_ERROR = "operationError";

    @Mock
    private OperationService operationService;

    @Mock
    private StackService stackService;

    @Mock
    private FreeIpaFlowManager flowManager;

    @Mock
    private FreeIpaScalingValidationService validationService;

    @Mock
    private FreeipaDownscaleNodeCalculatorService freeipaDownscaleNodeCalculatorService;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private FreeIpaScalingService underTest;

    @Test
    public void testUpscaleIfValidationFailsThenErrorThrown() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> allInstances = createValidImSet();

        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(allInstances);
        UpscaleRequest request = createUpscaleRequest();
        doThrow(new BadRequestException("validation failed")).when(validationService).validateStackForUpscale(allInstances, stack,
                new ScalingPath(TWO_NODE_BASED, HA));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.upscale(ACCOUNT_ID, request));

        assertEquals(exception.getMessage(), "validation failed");
    }

    @Test
    public void testVerticalScaleIfValidationSuccessShouldReturnFlowId() {
        Stack stack = mock(Stack.class);
        VerticalScaleRequest request = createVerticalScaleRequest();

        when(stack.getId()).thenReturn(1L);
        when(stack.getStackStatus()).thenReturn(createStackStatus(Status.AVAILABLE));
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(anyString(), anyString())).thenReturn(stack);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, POLLABLE_ID);
        when(flowManager.notify(anyString(), any())).thenReturn(flowIdentifier);
        doNothing().when(validationService).validateStackForVerticalUpscale(any(), any());

        underTest.verticalScale(ACCOUNT_ID, ENV_CRN, request);
    }

    @Test
    public void testUpscaleIfValidationPassesAndOperationRunningThenSucceed() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> allInstances = createValidImSet();
        Operation operation = createOperation(true);

        when(stack.getAccountId()).thenReturn(ACCOUNT_ID);
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(operationService.startOperation(ACCOUNT_ID, OperationType.UPSCALE, List.of(ENV_CRN), List.of())).thenReturn(operation);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(allInstances);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, POLLABLE_ID);
        when(flowManager.notify(anyString(), any())).thenReturn(flowIdentifier);
        UpscaleRequest request = createUpscaleRequest();

        UpscaleResponse response = underTest.upscale(ACCOUNT_ID, request);

        assertEquals(response.getOperationId(), OPERATION_ID);
        assertEquals(response.getOriginalAvailabilityType(), TWO_NODE_BASED);
        assertEquals(response.getTargetAvailabilityType(), HA);
        assertEquals(response.getFlowIdentifier(), flowIdentifier);
    }

    @Test
    public void testUpscaleIfValidationPassesAndOperationFailedThenThrowException() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> allInstances = createValidImSet();
        Operation operation = createOperation(false);

        when(stack.getAccountId()).thenReturn(ACCOUNT_ID);
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(operationService.startOperation(ACCOUNT_ID, OperationType.UPSCALE, List.of(ENV_CRN), List.of())).thenReturn(operation);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(allInstances);
        UpscaleRequest request = createUpscaleRequest();

        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.upscale(ACCOUNT_ID, request));

        assertEquals(exception.getMessage(), "upscale operation couldn't be started with: operationError");
    }

    @Test
    public void testDownscaleIfValidationFailsThenErrorThrown() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> allInstances = createValidImSet();
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(stack.getNotTerminatedInstanceMetaDataSet()).thenReturn(allInstances);
        DownscaleRequest request = createDownscaleRequest();
        when(freeipaDownscaleNodeCalculatorService.calculateTargetAvailabilityType(request, allInstances.size())).thenReturn(NON_HA);
        doThrow(new BadRequestException("validation failed")).when(validationService).validateStackForDownscale(allInstances, stack,
                new ScalingPath(TWO_NODE_BASED, NON_HA), null, false);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.downscale(ACCOUNT_ID, request));

        assertEquals(exception.getMessage(), "validation failed");
    }

    @Test
    public void testDownscaleIfValidationPassesAndOperationRunningThenSucceed() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> allInstances = createValidImSet();
        Operation operation = createOperation(true);
        AvailabilityInfo originalAvailabilityInfo = new AvailabilityInfo(allInstances.size());

        when(stack.getAccountId()).thenReturn(ACCOUNT_ID);
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(operationService.startOperation(ACCOUNT_ID, OperationType.DOWNSCALE, List.of(ENV_CRN), List.of())).thenReturn(operation);
        when(stack.getNotTerminatedInstanceMetaDataSet()).thenReturn(allInstances);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, POLLABLE_ID);
        when(flowManager.notify(anyString(), any())).thenReturn(flowIdentifier);
        DownscaleRequest request = createDownscaleRequest();
        when(freeipaDownscaleNodeCalculatorService.calculateTargetAvailabilityType(request, allInstances.size())).thenCallRealMethod();
        when(freeipaDownscaleNodeCalculatorService.calculateDownscaleCandidates(stack, originalAvailabilityInfo, request.getTargetAvailabilityType(), null))
                .thenReturn(new ArrayList<>(List.of("im2")));

        DownscaleResponse response = underTest.downscale(ACCOUNT_ID, request);

        assertEquals(OPERATION_ID, response.getOperationId());
        assertEquals(TWO_NODE_BASED, response.getOriginalAvailabilityType());
        assertEquals(NON_HA, response.getTargetAvailabilityType());
        assertEquals(flowIdentifier, response.getFlowIdentifier());
        assertEquals(Set.of("im2"), response.getDownscaleCandidates());
    }

    @Test
    public void testDownscaleWithInstanceIdsIfValidationPassesAndOperationRunningThenSucceed() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> allInstances = createValidImSet();
        AvailabilityInfo originalAvailabilityInfo = new AvailabilityInfo(allInstances.size());
        Operation operation = createOperation(true);

        when(stack.getAccountId()).thenReturn(ACCOUNT_ID);
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(operationService.startOperation(ACCOUNT_ID, OperationType.DOWNSCALE, List.of(ENV_CRN), List.of())).thenReturn(operation);
        when(stack.getNotTerminatedInstanceMetaDataSet()).thenReturn(allInstances);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, POLLABLE_ID);
        when(flowManager.notify(anyString(), any())).thenReturn(flowIdentifier);
        Set<String> instanceIdsToDelete = Set.of("im2");
        DownscaleRequest request = createDownscaleRequest(instanceIdsToDelete);
        when(freeipaDownscaleNodeCalculatorService.calculateTargetAvailabilityType(request, allInstances.size())).thenCallRealMethod();
        when(freeipaDownscaleNodeCalculatorService.calculateDownscaleCandidates(stack, originalAvailabilityInfo, request.getTargetAvailabilityType(),
                instanceIdsToDelete)).thenReturn(new ArrayList<>(instanceIdsToDelete));

        DownscaleResponse response = underTest.downscale(ACCOUNT_ID, request);

        assertEquals(OPERATION_ID, response.getOperationId());
        assertEquals(TWO_NODE_BASED, response.getOriginalAvailabilityType());
        assertEquals(NON_HA, response.getTargetAvailabilityType());
        assertEquals(flowIdentifier, response.getFlowIdentifier());
        assertEquals(instanceIdsToDelete, response.getDownscaleCandidates());

        ArgumentCaptor<DownscaleEvent> downscaleEventArgumentCaptor = ArgumentCaptor.forClass(DownscaleEvent.class);
        verify(flowManager).notify(eq("DOWNSCALE_EVENT"), downscaleEventArgumentCaptor.capture());
        DownscaleEvent downscaleEvent = downscaleEventArgumentCaptor.getValue();
        assertThat(downscaleEvent.getInstanceIds()).asList().hasSize(1).hasSameElementsAs(Set.of("im2"));
    }

    @Test
    public void testDownscaleIfValidationPassesAndOperationFailedThenThrowException() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> allInstances = createValidImSet();
        Operation operation = createOperation(false);

        when(stack.getAccountId()).thenReturn(ACCOUNT_ID);
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(operationService.startOperation(ACCOUNT_ID, OperationType.DOWNSCALE, List.of(ENV_CRN), List.of())).thenReturn(operation);
        when(stack.getNotTerminatedInstanceMetaDataSet()).thenReturn(allInstances);
        DownscaleRequest request = createDownscaleRequest();

        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.downscale(ACCOUNT_ID, request));

        assertEquals(exception.getMessage(), "downscale operation couldn't be started with: operationError");
    }

    private DownscaleRequest createDownscaleRequest() {
        return createDownscaleRequest(Set.of());
    }

    private DownscaleRequest createDownscaleRequest(Set<String> instanceIds) {
        DownscaleRequest request = new DownscaleRequest();
        request.setEnvironmentCrn(ENV_CRN);
        if (instanceIds.isEmpty()) {
            request.setTargetAvailabilityType(NON_HA);
        } else {
            request.setInstanceIds(instanceIds);
        }
        return request;
    }

    private UpscaleRequest createUpscaleRequest() {
        UpscaleRequest request = new UpscaleRequest();
        request.setEnvironmentCrn(ENV_CRN);
        request.setTargetAvailabilityType(HA);
        return request;
    }

    private VerticalScaleRequest createVerticalScaleRequest() {
        VerticalScaleRequest request = new VerticalScaleRequest();
        request.setGroup("master");
        InstanceTemplateRequest instanceTemplateRequest = new InstanceTemplateRequest();
        instanceTemplateRequest.setInstanceType("ec2bug");
        request.setTemplate(instanceTemplateRequest);
        return request;
    }

    private Operation createOperation(boolean running) {
        Operation operation = new Operation();
        operation.setStatus(running ? OperationState.RUNNING : OperationState.FAILED);
        operation.setOperationId(OPERATION_ID);
        if (!running) {
            operation.setError(OPERATION_ERROR);
        }
        return operation;
    }

    @Test
    public void testVerticalScaleWithOneByOneWithoutRollingEntitlementThrowsBadRequest() {
        when(entitlementService.isFreeIpaRollingVerticalScaleEnabled(ACCOUNT_ID)).thenReturn(false);
        Stack stack = mock(Stack.class);
        when(stack.getStackStatus()).thenReturn(createStackStatus(Status.AVAILABLE));
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        VerticalScaleRequest request = createVerticalScaleRequest();
        request.setOrchestratorType(OrchestratorType.ONE_BY_ONE.name());

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.verticalScale(ACCOUNT_ID, ENV_CRN, request));

        assertEquals("Rolling vertical scale for FreeIPA is not enabled for this account.", exception.getMessage());
        verify(validationService, never()).validateStackForVerticalUpscale(any(), any());
        verify(flowManager, never()).notify(anyString(), any());
    }

    @Test
    public void testVerticalScaleWithOneByOneRoutesToRollingPath() {
        Stack stack = mock(Stack.class);
        when(stack.getId()).thenReturn(1L);
        when(stack.getResourceCrn()).thenReturn("crn");
        when(stack.getEnvironmentCrn()).thenReturn(ENV_CRN);
        when(stack.getStackStatus()).thenReturn(createStackStatus(Status.AVAILABLE));
        when(entitlementService.isFreeIpaRollingVerticalScaleEnabled(ACCOUNT_ID)).thenReturn(true);
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        doNothing().when(validationService).validateStackForRollingVerticalScale(any(), any());
        when(operationService.startOperation(ACCOUNT_ID, OperationType.VERTICAL_SCALE, List.of(ENV_CRN), List.of()))
                .thenReturn(createOperation(true));
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW_CHAIN, POLLABLE_ID);
        when(flowManager.notify(anyString(), any())).thenReturn(flowIdentifier);

        VerticalScaleRequest request = createVerticalScaleRequest();
        request.setOrchestratorType(OrchestratorType.ONE_BY_ONE.name());

        VerticalScaleResponse result = underTest.verticalScale(ACCOUNT_ID, ENV_CRN, request);

        assertEquals(flowIdentifier, result.getFlowIdentifier());
        assertEquals(OPERATION_ID, result.getOperationId());
        verify(flowManager).notify(eq(FlowChainTriggers.FREEIPA_ROLLING_VERTICAL_SCALE_CHAIN_TRIGGER_EVENT),
                any(FreeIpaRollingVerticalScaleChainTriggerEvent.class));
    }

    @Test
    public void testVerticalScaleWithAllAtOnceAndRollingEntitlementRoutesToRollingPath() {
        Stack stack = mockStackForRollingVerticalScale(Status.AVAILABLE);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW_CHAIN, POLLABLE_ID);
        when(flowManager.notify(anyString(), any())).thenReturn(flowIdentifier);
        VerticalScaleRequest request = createVerticalScaleRequest();
        request.setOrchestratorType(OrchestratorType.ALL_AT_ONCE.name());

        VerticalScaleResponse result = underTest.verticalScale(ACCOUNT_ID, ENV_CRN, request);

        assertEquals(flowIdentifier, result.getFlowIdentifier());
        assertEquals(OPERATION_ID, result.getOperationId());
        verify(flowManager).notify(eq(FlowChainTriggers.FREEIPA_ROLLING_VERTICAL_SCALE_CHAIN_TRIGGER_EVENT),
                any(FreeIpaRollingVerticalScaleChainTriggerEvent.class));
        verify(validationService).validateStackForRollingVerticalScale(stack, request);
        verify(validationService, never()).validateStackForVerticalUpscale(any(), any());
    }

    @Test
    public void testVerticalScaleWithAllAtOnceWithoutRollingEntitlementRoutesToExistingPath() {
        Stack stack = mock(Stack.class);
        when(stack.getId()).thenReturn(1L);
        when(stack.getStackStatus()).thenReturn(createStackStatus(Status.AVAILABLE));
        when(entitlementService.isFreeIpaRollingVerticalScaleEnabled(ACCOUNT_ID)).thenReturn(false);
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, POLLABLE_ID);
        when(flowManager.notify(anyString(), any())).thenReturn(flowIdentifier);
        VerticalScaleRequest request = createVerticalScaleRequest();
        request.setOrchestratorType(OrchestratorType.ALL_AT_ONCE.name());

        VerticalScaleResponse result = underTest.verticalScale(ACCOUNT_ID, ENV_CRN, request);

        assertEquals(flowIdentifier, result.getFlowIdentifier());
        verify(validationService).validateStackForVerticalUpscale(stack, request);
        verify(flowManager).notify(eq(STACK_VERTICALSCALE_EVENT.event()), any(FreeIpaVerticalScalingTriggerEvent.class));
        verify(validationService, never()).validateStackForRollingVerticalScale(any(), any());
    }

    @Test
    public void testVerticalScaleWithRollingEntitlementButStoppedFreeIpaRoutesToExistingPath() {
        Stack stack = mock(Stack.class);
        when(stack.getId()).thenReturn(1L);
        when(stack.getStackStatus()).thenReturn(createStackStatus(Status.STOPPED));
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, POLLABLE_ID);
        when(flowManager.notify(anyString(), any())).thenReturn(flowIdentifier);
        VerticalScaleRequest request = createVerticalScaleRequest();
        request.setOrchestratorType(OrchestratorType.ALL_AT_ONCE.name());

        VerticalScaleResponse result = underTest.verticalScale(ACCOUNT_ID, ENV_CRN, request);

        assertEquals(flowIdentifier, result.getFlowIdentifier());
        verify(flowManager).notify(eq(STACK_VERTICALSCALE_EVENT.event()), any(FreeIpaVerticalScalingTriggerEvent.class));
        verify(validationService, never()).validateStackForRollingVerticalScale(any(), any());
        verify(validationService).validateStackForVerticalUpscale(stack, request);
        verify(entitlementService, never()).isFreeIpaRollingVerticalScaleEnabled(any());
    }

    @Test
    public void testVerticalScaleWithOneByOneStoppedEntitledRoutesToExistingPath() {
        Stack stack = mock(Stack.class);
        when(stack.getId()).thenReturn(1L);
        when(stack.getStackStatus()).thenReturn(createStackStatus(Status.STOPPED));
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, POLLABLE_ID);
        when(flowManager.notify(anyString(), any())).thenReturn(flowIdentifier);
        VerticalScaleRequest request = createVerticalScaleRequest();
        request.setOrchestratorType(OrchestratorType.ONE_BY_ONE.name());

        VerticalScaleResponse result = underTest.verticalScale(ACCOUNT_ID, ENV_CRN, request);

        assertEquals(flowIdentifier, result.getFlowIdentifier());
        verify(validationService).validateStackForVerticalUpscale(stack, request);
        verify(flowManager).notify(eq(STACK_VERTICALSCALE_EVENT.event()), any(FreeIpaVerticalScalingTriggerEvent.class));
        verify(validationService, never()).validateStackForRollingVerticalScale(any(), any());
        verify(entitlementService, never()).isFreeIpaRollingVerticalScaleEnabled(any());
    }

    @Test
    public void testVerticalScaleOneByOneOnAwsCloudFormationThrowsBadRequest() {
        Stack stack = mock(Stack.class);
        when(stack.getStackStatus()).thenReturn(createStackStatus(Status.AVAILABLE));
        when(stack.getCloudPlatform()).thenReturn("AWS");
        when(stack.getPlatformvariant()).thenReturn("AWS");
        when(stack.getName()).thenReturn("freeipa-cf-stack");
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        VerticalScaleRequest request = createVerticalScaleRequest();
        request.setOrchestratorType(OrchestratorType.ONE_BY_ONE.name());

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.verticalScale(ACCOUNT_ID, ENV_CRN, request));

        assertThat(exception.getMessage()).contains("not supported for FreeIPA 'freeipa-cf-stack' on AWS CloudFormation");
        verify(validationService, never()).validateStackForRollingVerticalScale(any(), any());
        verify(flowManager, never()).notify(anyString(), any());
    }

    @Test
    public void testVerticalScaleOneByOneOnStoppedAwsCloudFormationRoutesToExistingFlow() {
        Stack stack = mock(Stack.class);
        when(stack.getId()).thenReturn(1L);
        when(stack.getStackStatus()).thenReturn(createStackStatus(Status.STOPPED));
        when(stack.getCloudPlatform()).thenReturn("AWS");
        when(stack.getPlatformvariant()).thenReturn("AWS");
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, POLLABLE_ID);
        when(flowManager.notify(anyString(), any())).thenReturn(flowIdentifier);
        VerticalScaleRequest request = createVerticalScaleRequest();
        request.setOrchestratorType(OrchestratorType.ONE_BY_ONE.name());

        VerticalScaleResponse result = underTest.verticalScale(ACCOUNT_ID, ENV_CRN, request);

        assertEquals(flowIdentifier, result.getFlowIdentifier());
        verify(validationService).validateStackForVerticalUpscale(stack, request);
        verify(flowManager).notify(eq(STACK_VERTICALSCALE_EVENT.event()), any(FreeIpaVerticalScalingTriggerEvent.class));
        verify(validationService, never()).validateStackForRollingVerticalScale(any(), any());
        verify(entitlementService, never()).isFreeIpaRollingVerticalScaleEnabled(any());
    }

    @Test
    public void testVerticalScaleEntitledAllAtOnceOnAwsCloudFormationRoutesToExistingFlow() {
        Stack stack = mock(Stack.class);
        when(stack.getId()).thenReturn(1L);
        when(stack.getStackStatus()).thenReturn(createStackStatus(Status.AVAILABLE));
        when(stack.getCloudPlatform()).thenReturn("AWS");
        when(stack.getPlatformvariant()).thenReturn("AWS");
        when(entitlementService.isFreeIpaRollingVerticalScaleEnabled(ACCOUNT_ID)).thenReturn(true);
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, POLLABLE_ID);
        when(flowManager.notify(anyString(), any())).thenReturn(flowIdentifier);
        VerticalScaleRequest request = createVerticalScaleRequest();
        request.setOrchestratorType(OrchestratorType.ALL_AT_ONCE.name());

        VerticalScaleResponse result = underTest.verticalScale(ACCOUNT_ID, ENV_CRN, request);

        assertEquals(flowIdentifier, result.getFlowIdentifier());
        verify(flowManager).notify(eq(STACK_VERTICALSCALE_EVENT.event()), any(FreeIpaVerticalScalingTriggerEvent.class));
        verify(validationService, never()).validateStackForRollingVerticalScale(any(), any());
        verify(validationService).validateStackForVerticalUpscale(stack, request);
    }

    @Test
    public void testVerticalScaleStoppedFreeIpaDoesNotCallEntitlementService() {
        Stack stack = mock(Stack.class);
        when(stack.getId()).thenReturn(1L);
        when(stack.getStackStatus()).thenReturn(createStackStatus(Status.STOPPED));
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, POLLABLE_ID);
        when(flowManager.notify(anyString(), any())).thenReturn(flowIdentifier);
        VerticalScaleRequest request = createVerticalScaleRequest();
        request.setOrchestratorType(OrchestratorType.ALL_AT_ONCE.name());

        VerticalScaleResponse result = underTest.verticalScale(ACCOUNT_ID, ENV_CRN, request);

        assertEquals(flowIdentifier, result.getFlowIdentifier());
        verify(flowManager).notify(eq(STACK_VERTICALSCALE_EVENT.event()), any(FreeIpaVerticalScalingTriggerEvent.class));
        verify(validationService, never()).validateStackForRollingVerticalScale(any(), any());
        verify(entitlementService, never()).isFreeIpaRollingVerticalScaleEnabled(any());
    }

    @Test
    public void testVerticalScaleStoppedFreeIpaSucceedsEvenWhenEntitlementServiceThrows() {
        Stack stack = mock(Stack.class);
        when(stack.getId()).thenReturn(1L);
        when(stack.getStackStatus()).thenReturn(createStackStatus(Status.STOPPED));
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        lenient().when(entitlementService.isFreeIpaRollingVerticalScaleEnabled(ACCOUNT_ID))
                .thenThrow(new RuntimeException("UMS unavailable"));
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, POLLABLE_ID);
        when(flowManager.notify(anyString(), any())).thenReturn(flowIdentifier);
        VerticalScaleRequest request = createVerticalScaleRequest();
        request.setOrchestratorType(OrchestratorType.ALL_AT_ONCE.name());

        VerticalScaleResponse result = underTest.verticalScale(ACCOUNT_ID, ENV_CRN, request);

        assertEquals(flowIdentifier, result.getFlowIdentifier());
        verify(flowManager).notify(eq(STACK_VERTICALSCALE_EVENT.event()), any(FreeIpaVerticalScalingTriggerEvent.class));
        verify(validationService, never()).validateStackForRollingVerticalScale(any(), any());
    }

    private Stack mockStackForRollingVerticalScale(Status status) {
        Stack stack = mock(Stack.class);
        when(stack.getId()).thenReturn(1L);
        when(stack.getResourceCrn()).thenReturn("crn");
        when(stack.getEnvironmentCrn()).thenReturn(ENV_CRN);
        when(stack.getStackStatus()).thenReturn(createStackStatus(status));
        when(entitlementService.isFreeIpaRollingVerticalScaleEnabled(ACCOUNT_ID)).thenReturn(true);
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(operationService.startOperation(ACCOUNT_ID, OperationType.VERTICAL_SCALE, List.of(ENV_CRN), List.of()))
                .thenReturn(createOperation(true));
        return stack;
    }

    private StackStatus createStackStatus(Status status) {
        DetailedStackStatus detailedStackStatus = Status.STOPPED == status ? DetailedStackStatus.STOPPED : DetailedStackStatus.AVAILABLE;
        return new StackStatus(null, status, "", detailedStackStatus);
    }

    private Set<InstanceMetaData> createValidImSet() {
        InstanceMetaData im1 = new InstanceMetaData();
        im1.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        im1.setInstanceId("pgw");
        InstanceMetaData im2 = new InstanceMetaData();
        im2.setInstanceMetadataType(InstanceMetadataType.GATEWAY);
        im2.setInstanceId("im2");
        return Set.of(im1, im2);
    }

}