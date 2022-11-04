package com.sequenceiq.freeipa.service.freeipa.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.service.operation.OperationService;

@ExtendWith(MockitoExtension.class)
class TimeoutTaskSchedulerTest {

    private static final String OPERATION_ID = "opId";

    private static final String ACCOUNT_ID = "accId";

    private static final Long TIMEOUT = 6L;

    @Mock
    private ScheduledExecutorService timeoutTaskExecutor;

    @Mock
    private OperationService operationService;

    @InjectMocks
    private TimeoutTaskScheduler underTest;

    @Test
    public void taskCancelled() {
                Future<Void> task = mock(Future.class);
        when(task.isCancelled()).thenReturn(false);
        when(task.isDone()).thenReturn(false);
        doAnswer(inv -> {
            Runnable runnable = inv.getArgument(0, Runnable.class);
            runnable.run();
            return null;
        }).when(timeoutTaskExecutor).schedule(any(Runnable.class), eq(TIMEOUT), eq(TimeUnit.MILLISECONDS));

        underTest.scheduleTimeoutTask(OPERATION_ID, ACCOUNT_ID, task, TIMEOUT);

        verify(operationService).timeout(OPERATION_ID, ACCOUNT_ID);
        verify(task).cancel(true);
    }

    @Test
    public void taskAlreadyCancelled() {
        Future<Void> task = mock(Future.class);
        when(task.isCancelled()).thenReturn(true);
        doAnswer(inv -> {
            Runnable runnable = inv.getArgument(0, Runnable.class);
            runnable.run();
            return null;
        }).when(timeoutTaskExecutor).schedule(any(Runnable.class), eq(TIMEOUT), eq(TimeUnit.MILLISECONDS));

        underTest.scheduleTimeoutTask(OPERATION_ID, ACCOUNT_ID, task, TIMEOUT);

        verifyNoInteractions(operationService);
        verifyNoMoreInteractions(task);
    }

    @Test
    public void taskIsDone() {
        Future<Void> task = mock(Future.class);
        when(task.isCancelled()).thenReturn(false);
        when(task.isDone()).thenReturn(true);
        doAnswer(inv -> {
            Runnable runnable = inv.getArgument(0, Runnable.class);
            runnable.run();
            return null;
        }).when(timeoutTaskExecutor).schedule(any(Runnable.class), eq(TIMEOUT), eq(TimeUnit.MILLISECONDS));

        underTest.scheduleTimeoutTask(OPERATION_ID, ACCOUNT_ID, task, TIMEOUT);

        verifyNoInteractions(operationService);
        verifyNoMoreInteractions(task);
    }
}