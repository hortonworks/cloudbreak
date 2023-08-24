package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.MODIFY_PROXY_CONFIG_REQUESTED;
import static com.sequenceiq.freeipa.flow.chain.FlowChainTriggers.MODIFY_PROXY_CHAIN_TRIGGER_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.converter.operation.OperationToOperationStatusConverter;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;

@ExtendWith(MockitoExtension.class)
class FreeipaModifyProxyConfigServiceTest {

    private static final String ENV_CRN = "env-crn";

    private static final String ACCOUNT_ID = "account-id";

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
    private FreeipaModifyProxyConfigService underTest;

    @Mock
    private Stack stack;

    @Mock
    private StackStatus stackStatus;

    @Mock
    private Operation operation;

    @Mock
    private OperationStatus operationStatus;

    @BeforeEach
    void setUp() {
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        lenient().when(stack.isAvailable()).thenReturn(true);
        lenient().when(stack.getAccountId()).thenReturn(ACCOUNT_ID);
        lenient().when(stack.getEnvironmentCrn()).thenReturn(ENV_CRN);
        lenient().when(stack.getStackStatus()).thenReturn(stackStatus);
        lenient().when(stackStatus.getStatus()).thenReturn(Status.AVAILABLE);

        lenient().when(operationService.startOperation(ACCOUNT_ID, OperationType.MODIFY_PROXY_CONFIG,
                List.of(ENV_CRN), List.of())).thenReturn(operation);
        lenient().when(operation.getStatus()).thenReturn(OperationState.RUNNING);
        lenient().when(operationConverter.convert(operation)).thenReturn(operationStatus);
    }

    @Test
    void validateNonAvailableEnv() {
        when(stackStatus.getStatus()).thenReturn(Status.CREATE_FAILED);

        Assertions.assertThatThrownBy(() -> underTest.modifyProxyConfig(ENV_CRN, null, ACCOUNT_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Proxy config modification is not supported in current FreeIpa status: CREATE_FAILED");
    }

    @Test
    void failToStartOperation() {
        when(operation.getStatus()).thenReturn(OperationState.FAILED);

        OperationStatus result = underTest.modifyProxyConfig(ENV_CRN, null, ACCOUNT_ID);

        assertThat(result).isEqualTo(operationStatus);
        verifyStartOperation();
        verifyNoInteractions(stackUpdater);
        verifyNoInteractions(flowManager);
    }

    @Test
    void failToStartFlow() {
        RuntimeException cause = new RuntimeException("cause");
        when(flowManager.notify(eq(MODIFY_PROXY_CHAIN_TRIGGER_EVENT), any())).thenThrow(cause);
        Operation failedOperation = mock(Operation.class);
        when(operationService.failOperation(eq(ACCOUNT_ID), any(), any())).thenReturn(failedOperation);
        OperationStatus failedOperationStatus = mock(OperationStatus.class);
        when(operationConverter.convert(failedOperation)).thenReturn(failedOperationStatus);

        OperationStatus result = underTest.modifyProxyConfig(ENV_CRN, null, ACCOUNT_ID);

        assertThat(result).isEqualTo(failedOperationStatus);
    }

    @Test
    void success() {
        OperationStatus result = underTest.modifyProxyConfig(ENV_CRN, null, ACCOUNT_ID);

        assertThat(result).isEqualTo(operationStatus);
        verify(stackUpdater).updateStackStatus(stack, MODIFY_PROXY_CONFIG_REQUESTED, "Starting proxy config modification");
        verify(flowManager).notify(eq(MODIFY_PROXY_CHAIN_TRIGGER_EVENT), any());
    }

    private void verifyStartOperation() {
        verify(operationService).startOperation(stack.getAccountId(), OperationType.MODIFY_PROXY_CONFIG,
                List.of(stack.getEnvironmentCrn()), List.of());
    }

}
