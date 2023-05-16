package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.freeipa.flow.chain.FlowChainTriggers.UPGRADE_CCM_CHAIN_TRIGGER_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.converter.operation.OperationToOperationStatusConverter;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmFlowChainTriggerEvent;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;

@ExtendWith(MockitoExtension.class)
class FreeIpaUpgradeCcmServiceTest {

    private static final String STACK_NAME = "stackName";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String ACCOUNT_ID = "accountId";

    private static final String OPERATION_ID = "opid";

    private static final long STACK_ID = 12L;

    @Mock
    private FreeIpaFlowManager flowManager;

    @Mock
    private StackService stackService;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private OperationService operationService;

    @Mock
    private OperationToOperationStatusConverter operationConverter;

    @InjectMocks
    private FreeIpaUpgradeCcmService underTest;

    private OperationStatus operationStatus;

    @BeforeEach
    void setUp() {
        operationStatus = new OperationStatus();
    }

    @Test
    void upgradeCcmTestWhenAvailableAndOperationRunning() {
        Stack stack = createStack(Status.AVAILABLE);
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);
        Operation operation = createOperation(OperationState.RUNNING);
        when(operationService.startOperation(ACCOUNT_ID, OperationType.UPGRADE_CCM, List.of(ENVIRONMENT_CRN), List.of())).thenReturn(operation);
        when(operationConverter.convert(operation)).thenReturn(operationStatus);

        OperationStatus result = underTest.upgradeCcm(ENVIRONMENT_CRN, ACCOUNT_ID);

        assertThat(result).isSameAs(operationStatus);
        ArgumentCaptor<UpgradeCcmFlowChainTriggerEvent> eventCaptor = ArgumentCaptor.forClass(UpgradeCcmFlowChainTriggerEvent.class);
        verify(flowManager).notify(eq(UPGRADE_CCM_CHAIN_TRIGGER_EVENT), eventCaptor.capture());
        UpgradeCcmFlowChainTriggerEvent event = eventCaptor.getValue();
        assertThat(event.getOldTunnel()).isEqualTo(Tunnel.CCM);
        assertThat(event.getOperationId()).isEqualTo(OPERATION_ID);
        assertThat(event.getResourceId()).isEqualTo(STACK_ID);
    }

    @EnumSource(value = Tunnel.class, names = { "DIRECT", "CLUSTER_PROXY"}, mode = EnumSource.Mode.INCLUDE)
    @ParameterizedTest
    void upgradeCcmTestWhenAvailableButIncorrectTunnelType(Tunnel tunnel) {
        Stack stack = createStack(Status.AVAILABLE);
        stack.setTunnel(tunnel);
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);

        assertThatThrownBy(() -> underTest.upgradeCcm(ENVIRONMENT_CRN, ACCOUNT_ID)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void upgradeCcmTestWhenAlreadyUpgraded() {
        Stack stack = createStack(Status.AVAILABLE);
        stack.setTunnel(Tunnel.CCMV2_JUMPGATE);
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);

        OperationStatus operationStatus = underTest.upgradeCcm(ENVIRONMENT_CRN, ACCOUNT_ID);
        assertThat(operationStatus.getStatus()).isEqualTo(OperationState.COMPLETED);
    }

    @Test
    void upgradeCcmTestWhenAvailableAndOperationRunningButFailedToStart() {
        Stack stack = createStack(Status.AVAILABLE);
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);
        Operation operation = createOperation(OperationState.RUNNING);
        when(operationService.startOperation(ACCOUNT_ID, OperationType.UPGRADE_CCM, List.of(ENVIRONMENT_CRN), List.of())).thenReturn(operation);
        Operation failedOperation = createOperation(OperationState.FAILED);
        when(operationService.failOperation(eq(ACCOUNT_ID), eq(OPERATION_ID), any())).thenReturn(failedOperation);
        when(operationConverter.convert(any())).then(invocation -> {
            Operation opArg = invocation.getArgument(0);
            if (!Objects.equals(opArg.getStatus(), OperationState.FAILED)) {
                return operationStatus;
            }
            operationStatus.setStatus(OperationState.FAILED);
            return operationStatus;
        });
        when(flowManager.notify(nullable(String.class), any())).thenThrow(new IllegalStateException("bad state"));
        OperationStatus result = underTest.upgradeCcm(ENVIRONMENT_CRN, ACCOUNT_ID);

        assertThat(result.getStatus()).isEqualTo(OperationState.FAILED);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = OperationState.class, names = { "RUNNING" }, mode = EnumSource.Mode.EXCLUDE)
    void upgradeCcmTestWhenAvailableAndOperationNotRunning(OperationState operationState) {
        Stack stack = createStack(Status.AVAILABLE);
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);
        Operation operation = createOperation(operationState);
        when(operationService.startOperation(ACCOUNT_ID, OperationType.UPGRADE_CCM, List.of(ENVIRONMENT_CRN), List.of())).thenReturn(operation);
        when(operationConverter.convert(operation)).thenReturn(operationStatus);

        OperationStatus result = underTest.upgradeCcm(ENVIRONMENT_CRN, ACCOUNT_ID);

        assertThat(result).isSameAs(operationStatus);
        verifyNoInteractions(flowManager);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Status.class, names = { "AVAILABLE", "UPGRADE_CCM_FAILED" }, mode = EnumSource.Mode.EXCLUDE)
    void upgradeCcmTestWhenNotAvailable(Status status) {
        Stack stack = createStack(status);
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> underTest.upgradeCcm(ENVIRONMENT_CRN, ACCOUNT_ID));
        assertThat(badRequestException).hasMessage("FreeIPA stack 'stackName' must be AVAILABLE to start Cluster Connectivity Manager upgrade.");
    }

    private Stack createStack(Status status) {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        stack.setName(STACK_NAME);
        stack.setTunnel(Tunnel.CCM);
        StackStatus stackStatus = new StackStatus();
        stack.setStackStatus(stackStatus);
        stackStatus.setStatus(status);
        return stack;
    }

    private Operation createOperation(OperationState operationState) {
        Operation operation = new Operation();
        operation.setOperationId(OPERATION_ID);
        operation.setStatus(operationState);
        return operation;
    }

}
