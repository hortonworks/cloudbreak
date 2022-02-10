package com.sequenceiq.freeipa.service.stack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
class FreeIpaUpgradeCcmServiceTest {

    private static final String STACK_NAME = "stackName";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String ACCOUNT_ID = "accountId";

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
        // TODO verify that flow has been triggered successfully; see CB-14571
    }

    // TODO add test for AvailableAndOperationRunningAndFlowStartFailure; see CB-14571

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = OperationState.class, names = {"RUNNING"}, mode = EnumSource.Mode.EXCLUDE)
    void upgradeCcmTestWhenAvailableAndOperationNotRunning(OperationState operationState) {
        Stack stack = createStack(Status.AVAILABLE);
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);
        Operation operation = createOperation(operationState);
        when(operationService.startOperation(ACCOUNT_ID, OperationType.UPGRADE_CCM, List.of(ENVIRONMENT_CRN), List.of())).thenReturn(operation);
        when(operationConverter.convert(operation)).thenReturn(operationStatus);

        OperationStatus result = underTest.upgradeCcm(ENVIRONMENT_CRN, ACCOUNT_ID);

        assertThat(result).isSameAs(operationStatus);
        // TODO verify that flow has not been triggered; see CB-14571
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Status.class, names = {"AVAILABLE"}, mode = EnumSource.Mode.EXCLUDE)
    void upgradeCcmTestWhenNotAvailable(Status status) {
        Stack stack = createStack(status);
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> underTest.upgradeCcm(ENVIRONMENT_CRN, ACCOUNT_ID));
        assertThat(badRequestException).hasMessage("FreeIPA stack 'stackName' must be AVAILABLE to start Cluster Connectivity Manager upgrade.");
    }

    private Stack createStack(Status status) {
        Stack stack = new Stack();
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        stack.setName(STACK_NAME);
        StackStatus stackStatus = new StackStatus();
        stack.setStackStatus(stackStatus);
        stackStatus.setStatus(status);
        return stack;
    }

    private Operation createOperation(OperationState operationState) {
        Operation operation = new Operation();
        operation.setStatus(operationState);
        return operation;
    }

}