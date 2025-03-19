package com.sequenceiq.freeipa.service.freeipa.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class AbstractUserSyncTaskRunnerTest {

    private static final String ACCOUNT_ID = "accid";

    private static final String ENVIRONMENT_CRN = "envcrn";

    @Mock
    private StackService stackService;

    @Mock
    private OperationService operationService;

    @InjectMocks
    private AbstractUserSyncTaskRunner underTest = spy(create());

    @Test
    public void testOperationRunning() {
        Operation operation = new Operation();
        operation.setOperationId(UUID.randomUUID().toString());
        operation.setStatus(OperationState.RUNNING);
        when(operationService.startOperation(ACCOUNT_ID, OperationType.USER_SYNC, List.of(ENVIRONMENT_CRN), List.of())).thenReturn(operation);
        Stack stack = new Stack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);
        doAnswer(inv -> {
            inv.getArgument(2, Runnable.class).run();
            return null;
        }).when(operationService).tryWithOperationCleanup(eq(operation.getOperationId()), eq(ACCOUNT_ID), any(Runnable.class));

        underTest.runUserSyncTasks(ENVIRONMENT_CRN, ACCOUNT_ID);

        verify(underTest).asyncRunTask(operation.getOperationId(), ACCOUNT_ID, stack);
    }

    @Test
    public void testOperationRejected() {
        Operation operation = new Operation();
        operation.setOperationId(UUID.randomUUID().toString());
        operation.setStatus(OperationState.REJECTED);
        when(operationService.startOperation(ACCOUNT_ID, OperationType.USER_SYNC, List.of(ENVIRONMENT_CRN), List.of())).thenReturn(operation);

        underTest.runUserSyncTasks(ENVIRONMENT_CRN, ACCOUNT_ID);

        verify(underTest, never()).asyncRunTask(any(), any(), any());
    }

    private AbstractUserSyncTaskRunner create() {
        return new AbstractUserSyncTaskRunner() {
            @Override
            protected void asyncRunTask(String operationId, String accountId, Stack stack) {
            }
        };
    }

}