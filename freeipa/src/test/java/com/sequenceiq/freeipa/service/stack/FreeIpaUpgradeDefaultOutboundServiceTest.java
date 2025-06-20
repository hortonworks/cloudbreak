package com.sequenceiq.freeipa.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.NetworkAttributes;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.common.api.type.OutboundType;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.converter.operation.OperationToOperationStatusConverter;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.resource.ResourceAttributeUtil;
import com.sequenceiq.freeipa.service.resource.ResourceService;

@ExtendWith(MockitoExtension.class)
class FreeIpaUpgradeDefaultOutboundServiceTest {

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:tenant:environment:12345";

    private static final String ACCOUNT_ID = "account-123";

    private static final String STACK_NAME = "test-stack";

    private static final Long STACK_ID = 1L;

    private static final String OPERATION_ID = "operation-123";

    @Mock
    private FreeIpaFlowManager flowManager;

    @Mock
    private StackService stackService;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private ResourceService resourceService;

    @Mock
    private ResourceAttributeUtil resourceAttributeUtil;

    @Mock
    private OperationService operationService;

    @Mock
    private OperationToOperationStatusConverter operationConverter;

    @InjectMocks
    private FreeIpaUpgradeDefaultOutboundService underTest;

    private Stack stack;

    private StackStatus stackStatus;

    private Resource networkResource;

    private NetworkAttributes networkAttributes;

    private Operation operation;

    private OperationStatus operationStatus;

    @BeforeEach
    void setUp() {
        stack = new Stack();
        stack.setId(STACK_ID);
        stack.setName(STACK_NAME);
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);

        stackStatus = new StackStatus();
        stackStatus.setStatus(Status.AVAILABLE);
        stack.setStackStatus(stackStatus);

        networkResource = new Resource();
        networkResource.setId(1L);
        networkResource.setResourceType(ResourceType.AZURE_NETWORK);

        networkAttributes = new NetworkAttributes();

        operation = new Operation();
        operation.setOperationId(OPERATION_ID);
        operation.setStatus(OperationState.RUNNING);

        operationStatus = new OperationStatus(OPERATION_ID, OperationType.UPGRADE_DEFAULT_OUTBOUND, OperationState.RUNNING,
                List.of(), List.of(), null, System.currentTimeMillis(), null);
    }

    @Test
    void upgradeDefaultOutboundShouldStartUpgradeWhenOutboundTypeIsUpgradeable() {
        networkAttributes.setOutboundType(OutboundType.DEFAULT);
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(resourceService.findByStackIdAndType(STACK_ID, ResourceType.AZURE_NETWORK)).thenReturn(List.of(networkResource));
        when(resourceAttributeUtil.getTypedAttributes(networkResource, NetworkAttributes.class)).thenReturn(Optional.of(networkAttributes));
        when(operationService.startOperation(eq(ACCOUNT_ID), eq(OperationType.UPGRADE_DEFAULT_OUTBOUND), eq(List.of(ENVIRONMENT_CRN)), eq(List.of())))
                .thenReturn(operation);
        when(operationConverter.convert(operation)).thenReturn(operationStatus);

        OperationStatus result = underTest.upgradeDefaultOutbound(ENVIRONMENT_CRN, ACCOUNT_ID);

        assertNotNull(result);
        assertEquals(OPERATION_ID, result.getOperationId());
        assertEquals(OperationType.UPGRADE_DEFAULT_OUTBOUND, result.getOperationType());
        verify(stackUpdater).updateStackStatus(stack, DetailedStackStatus.UPGRADE_DEFAULT_OUTBOUND_REQUESTED,
                "Starting of DEFAULT OUTBOUND upgrade requested");
    }

    @Test
    void upgradeDefaultOutboundShouldReturnCompletedStatusWhenOutboundTypeIsNotUpgradeable() {
        networkAttributes.setOutboundType(OutboundType.LOAD_BALANCER);
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(resourceService.findByStackIdAndType(STACK_ID, ResourceType.AZURE_NETWORK)).thenReturn(List.of(networkResource));
        when(resourceAttributeUtil.getTypedAttributes(networkResource, NetworkAttributes.class)).thenReturn(Optional.of(networkAttributes));

        OperationStatus result = underTest.upgradeDefaultOutbound(ENVIRONMENT_CRN, ACCOUNT_ID);

        assertNotNull(result);
        assertEquals(OperationType.UPGRADE_DEFAULT_OUTBOUND, result.getOperationType());
        assertEquals(OperationState.COMPLETED, result.getStatus());
        verify(operationService, never()).startOperation(anyString(), any(), any(), any());
        verify(stackUpdater, never()).updateStackStatus(any(Stack.class), any(), anyString());
    }

    @Test
    void upgradeDefaultOutboundShouldReturnCompletedStatusWhenOutboundTypeIsNotDefined() {
        networkAttributes.setOutboundType(OutboundType.NOT_DEFINED);
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(resourceService.findByStackIdAndType(STACK_ID, ResourceType.AZURE_NETWORK)).thenReturn(List.of(networkResource));
        when(resourceAttributeUtil.getTypedAttributes(networkResource, NetworkAttributes.class)).thenReturn(Optional.of(networkAttributes));
        when(operationService.startOperation(eq(ACCOUNT_ID), eq(OperationType.UPGRADE_DEFAULT_OUTBOUND), eq(List.of(ENVIRONMENT_CRN)), eq(List.of())))
                .thenReturn(operation);
        when(operationConverter.convert(operation)).thenReturn(operationStatus);

        OperationStatus result = underTest.upgradeDefaultOutbound(ENVIRONMENT_CRN, ACCOUNT_ID);

        assertNotNull(result);
        assertEquals(OPERATION_ID, result.getOperationId());
        verify(operationService).startOperation(eq(ACCOUNT_ID), eq(OperationType.UPGRADE_DEFAULT_OUTBOUND), eq(List.of(ENVIRONMENT_CRN)), eq(List.of()));
    }

    @Test
    void upgradeDefaultOutboundShouldReturnOperationStatusWhenOperationIsNotRunning() {
        networkAttributes.setOutboundType(OutboundType.DEFAULT);
        operation.setStatus(OperationState.FAILED);
        operationStatus.setStatus(OperationState.FAILED);

        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(resourceService.findByStackIdAndType(STACK_ID, ResourceType.AZURE_NETWORK)).thenReturn(List.of(networkResource));
        when(resourceAttributeUtil.getTypedAttributes(networkResource, NetworkAttributes.class)).thenReturn(Optional.of(networkAttributes));
        when(operationService.startOperation(eq(ACCOUNT_ID), eq(OperationType.UPGRADE_DEFAULT_OUTBOUND), eq(List.of(ENVIRONMENT_CRN)), eq(List.of())))
                .thenReturn(operation);
        when(operationConverter.convert(operation)).thenReturn(operationStatus);

        OperationStatus result = underTest.upgradeDefaultOutbound(ENVIRONMENT_CRN, ACCOUNT_ID);

        assertNotNull(result);
        assertEquals(OperationState.FAILED, result.getStatus());
        verify(stackUpdater, never()).updateStackStatus(any(Stack.class), any(), anyString());
    }

    @Test
    void upgradeDefaultOutboundShouldFailOperationWhenExceptionOccursDuringFlowTrigger() {
        networkAttributes.setOutboundType(OutboundType.DEFAULT);
        Operation failedOperation = new Operation();
        failedOperation.setOperationId(OPERATION_ID);
        failedOperation.setStatus(OperationState.FAILED);

        OperationStatus failedOperationStatus = new OperationStatus(OPERATION_ID, OperationType.UPGRADE_DEFAULT_OUTBOUND, OperationState.FAILED,
                List.of(), List.of(), null, System.currentTimeMillis(), null);

        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(resourceService.findByStackIdAndType(STACK_ID, ResourceType.AZURE_NETWORK)).thenReturn(List.of(networkResource));
        when(resourceAttributeUtil.getTypedAttributes(networkResource, NetworkAttributes.class)).thenReturn(Optional.of(networkAttributes));
        when(operationService.startOperation(eq(ACCOUNT_ID), eq(OperationType.UPGRADE_DEFAULT_OUTBOUND), eq(List.of(ENVIRONMENT_CRN)), eq(List.of())))
                .thenReturn(operation);
        doThrow(new RuntimeException("Flow trigger failed")).when(stackUpdater)
                .updateStackStatus(stack, DetailedStackStatus.UPGRADE_DEFAULT_OUTBOUND_REQUESTED, "Starting of DEFAULT OUTBOUND upgrade requested");
        when(operationService.failOperation(eq(ACCOUNT_ID), eq(OPERATION_ID), anyString())).thenReturn(failedOperation);
        when(operationConverter.convert(failedOperation)).thenReturn(failedOperationStatus);

        OperationStatus result = underTest.upgradeDefaultOutbound(ENVIRONMENT_CRN, ACCOUNT_ID);

        assertNotNull(result);
        assertEquals(OperationState.FAILED, result.getStatus());
        verify(operationService).failOperation(eq(ACCOUNT_ID), eq(OPERATION_ID),
                eq("Couldn't start Cluster Connectivity Manager upgrade flow: Flow trigger failed"));
    }

    @Test
    void getCurrentDefaultOutboundShouldReturnOutboundTypeFromNetworkAttributes() {
        networkAttributes.setOutboundType(OutboundType.LOAD_BALANCER);
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(resourceService.findByStackIdAndType(STACK_ID, ResourceType.AZURE_NETWORK)).thenReturn(List.of(networkResource));
        when(resourceAttributeUtil.getTypedAttributes(networkResource, NetworkAttributes.class)).thenReturn(Optional.of(networkAttributes));

        OutboundType result = underTest.getCurrentDefaultOutbound(ENVIRONMENT_CRN, ACCOUNT_ID);

        assertEquals(OutboundType.LOAD_BALANCER, result);
    }

    @Test
    void getCurrentDefaultOutboundShouldReturnNotDefinedWhenNetworkAttributesNotFound() {
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(resourceService.findByStackIdAndType(STACK_ID, ResourceType.AZURE_NETWORK)).thenReturn(List.of(networkResource));
        when(resourceAttributeUtil.getTypedAttributes(networkResource, NetworkAttributes.class)).thenReturn(Optional.empty());

        OutboundType result = underTest.getCurrentDefaultOutbound(ENVIRONMENT_CRN, ACCOUNT_ID);

        assertEquals(OutboundType.NOT_DEFINED, result);
    }

    @Test
    void getCurrentDefaultOutboundShouldThrowExceptionWhenStackIsNotAvailable() {
        stackStatus.setStatus(Status.CREATE_IN_PROGRESS);
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.getCurrentDefaultOutbound(ENVIRONMENT_CRN, ACCOUNT_ID));

        assertEquals("FreeIPA stack 'test-stack' must be AVAILABLE to start Default Outbound upgrade.", exception.getMessage());
    }

    @Test
    void getCurrentDefaultOutboundShouldThrowExceptionWhenNoNetworkResourceFound() {
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(resourceService.findByStackIdAndType(STACK_ID, ResourceType.AZURE_NETWORK)).thenReturn(List.of());

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.getCurrentDefaultOutbound(ENVIRONMENT_CRN, ACCOUNT_ID));

        assertEquals("FreeIPA stack 'test-stack' has 0 network resources, cannot determine OutboundType.", exception.getMessage());
    }

    @Test
    void getCurrentDefaultOutboundShouldThrowExceptionWhenMultipleNetworkResourcesFound() {
        Resource secondNetworkResource = new Resource();
        secondNetworkResource.setId(2L);
        secondNetworkResource.setResourceType(ResourceType.AZURE_NETWORK);

        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(resourceService.findByStackIdAndType(STACK_ID, ResourceType.AZURE_NETWORK))
                .thenReturn(List.of(networkResource, secondNetworkResource));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.getCurrentDefaultOutbound(ENVIRONMENT_CRN, ACCOUNT_ID));

        assertEquals("FreeIPA stack 'test-stack' has 2 network resources, cannot determine OutboundType.", exception.getMessage());
    }

    @Test
    void upgradeDefaultOutboundShouldHandleAllUpgradeableOutboundTypes() {
        OutboundType[] upgradeableTypes = {OutboundType.DEFAULT, OutboundType.NOT_DEFINED};

        for (OutboundType outboundType : upgradeableTypes) {
            networkAttributes.setOutboundType(outboundType);
            when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);
            when(resourceService.findByStackIdAndType(STACK_ID, ResourceType.AZURE_NETWORK)).thenReturn(List.of(networkResource));
            when(resourceAttributeUtil.getTypedAttributes(networkResource, NetworkAttributes.class)).thenReturn(Optional.of(networkAttributes));
            when(operationService.startOperation(eq(ACCOUNT_ID), eq(OperationType.UPGRADE_DEFAULT_OUTBOUND), eq(List.of(ENVIRONMENT_CRN)), eq(List.of())))
                    .thenReturn(operation);
            when(operationConverter.convert(operation)).thenReturn(operationStatus);

            OperationStatus result = underTest.upgradeDefaultOutbound(ENVIRONMENT_CRN, ACCOUNT_ID);

            assertNotNull(result);
            assertEquals(OperationType.UPGRADE_DEFAULT_OUTBOUND, result.getOperationType());
        }
    }

    @Test
    void upgradeDefaultOutboundShouldHandleAllNonUpgradeableOutboundTypes() {
        OutboundType[] nonUpgradeableTypes = {
                OutboundType.LOAD_BALANCER,
                OutboundType.PUBLIC_IP,
                OutboundType.USER_ASSIGNED_NATGATEWAY,
                OutboundType.USER_DEFINED_ROUTING
        };

        for (OutboundType outboundType : nonUpgradeableTypes) {
            networkAttributes.setOutboundType(outboundType);
            when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);
            when(resourceService.findByStackIdAndType(STACK_ID, ResourceType.AZURE_NETWORK)).thenReturn(List.of(networkResource));
            when(resourceAttributeUtil.getTypedAttributes(networkResource, NetworkAttributes.class)).thenReturn(Optional.of(networkAttributes));

            OperationStatus result = underTest.upgradeDefaultOutbound(ENVIRONMENT_CRN, ACCOUNT_ID);

            assertNotNull(result);
            assertEquals(OperationType.UPGRADE_DEFAULT_OUTBOUND, result.getOperationType());
            assertEquals(OperationState.COMPLETED, result.getStatus());
        }
    }
}