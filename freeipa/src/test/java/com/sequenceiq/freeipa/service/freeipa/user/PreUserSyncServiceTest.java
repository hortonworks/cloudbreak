package com.sequenceiq.freeipa.service.freeipa.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.converter.stack.StackToStackUserSyncViewConverter;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.projection.StackUserSyncView;
import com.sequenceiq.freeipa.service.operation.OperationService;

@ExtendWith(MockitoExtension.class)
class PreUserSyncServiceTest {

    private static final String OPERATION_ID = "opId";

    private static final String ACCOUNT_ID = "accId";

    private static final Long TIMEOUT = 6L;

    @Mock
    private TimeoutTaskScheduler timeoutTaskScheduler;

    @Mock
    private UmsVirtualGroupCreateService umsVirtualGroupCreateService;

    @Mock
    private ExecutorService usersyncExternalTaskExecutor;

    @Mock
    private OperationService operationService;

    @Mock
    private StackToStackUserSyncViewConverter stackUserSyncViewConverter;

    @InjectMocks
    private PreUserSyncService underTest;

    @Test
    public void testScheduling() {
        Stack stack = new Stack();
        stack.setEnvironmentCrn("ENV_CRN");
        Future<Void> task = mock(Future.class);
        doAnswer(inv -> {
            Runnable runnable = inv.getArgument(0, Runnable.class);
            runnable.run();
            return task;
        }).when(usersyncExternalTaskExecutor).submit(any(Runnable.class));
        ReflectionTestUtils.setField(underTest, "operationTimeout", TIMEOUT);
        StackUserSyncView userSyncView = new StackUserSyncView(2L, "rcrn", "rname", stack.getEnvironmentCrn(), ACCOUNT_ID, "MOCK", Status.AVAILABLE);
        when(stackUserSyncViewConverter.convert(stack))
                .thenReturn(userSyncView);

        underTest.asyncRunTask(OPERATION_ID, ACCOUNT_ID, stack);

        verify(umsVirtualGroupCreateService).createVirtualGroups(ACCOUNT_ID, List.of(userSyncView));
        verify(operationService).completeOperation(ACCOUNT_ID, OPERATION_ID, List.of(new SuccessDetails(stack.getEnvironmentCrn())), List.of());
        verify(timeoutTaskScheduler).scheduleTimeoutTask(OPERATION_ID, ACCOUNT_ID, task, TIMEOUT);
    }
}