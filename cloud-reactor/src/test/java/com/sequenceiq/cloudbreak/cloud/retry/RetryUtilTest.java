package com.sequenceiq.cloudbreak.cloud.retry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.retry.CheckTask;
import com.sequenceiq.cloudbreak.retry.ErrorTask;
import com.sequenceiq.cloudbreak.retry.ExceptionCheckTask;
import com.sequenceiq.cloudbreak.retry.RetryTask;
import com.sequenceiq.cloudbreak.retry.RetryUtil;

class RetryUtilTest {

    private final RetryTask task = mock(RetryTask.class);

    private final ErrorTask error = mock(ErrorTask.class);

    private final CheckTask check = mock(CheckTask.class);

    private final ExceptionCheckTask exceptionCheck = mock(ExceptionCheckTask.class);

    @Test
    void testRunWithoutException() {
        runRetryTask();
        verify(task, times(1)).run();
        verify(error, times(0)).run(new Exception());
    }

    @Test
    void testRunWithoutExceptionCheckOk() {
        when(check.check()).thenReturn(true);
        runRetryTaskWithCheck();
        verify(task, times(1)).run();
        verify(check, times(1)).check();
        verify(error, times(0)).run(new Exception());
    }

    @Test
    void testRunWithoutExceptionCheckNok() {
        when(check.check()).thenReturn(false);
        runRetryTaskWithCheck();
        verify(task, times(3)).run();
        verify(check, times(3)).check();
        verify(error, times(1)).run(any());
    }

    @Test
    void testRunWithRecoverableException() {
        when(exceptionCheck.check(any())).thenReturn(true);
        doThrow(new IllegalArgumentException()).when(task).run();
        runRetryTaskWithExceptionCheck();
        verify(task, times(3)).run();
        verify(exceptionCheck, times(3)).check(any());
        verify(error, times(1)).run(any());
    }

    @Test
    void testRunWithNotRecoverableException() {
        when(exceptionCheck.check(any())).thenReturn(false);
        doThrow(new NullPointerException()).when(task).run();
        runRetryTaskWithExceptionCheck();
        verify(task, times(1)).run();
        verify(exceptionCheck, times(1)).check(any());
        verify(error, times(1)).run(any());
    }

    private void runRetryTask() {
        RetryUtil.withRetries(3)
                .retry(task)
                .ifNotRecoverable(error)
                .run();
    }

    private void runRetryTaskWithCheck() {
        RetryUtil.withRetries(3)
                .retry(task)
                .retryIfFalse(check)
                .ifNotRecoverable(error)
                .run();
    }

    private void runRetryTaskWithExceptionCheck() {
        RetryUtil.withRetries(3)
                .retry(task)
                .checkIfRecoverable(exceptionCheck)
                .ifNotRecoverable(error)
                .run();
    }
}