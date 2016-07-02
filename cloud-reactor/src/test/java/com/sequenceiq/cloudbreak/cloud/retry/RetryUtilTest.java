package com.sequenceiq.cloudbreak.cloud.retry;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

public class RetryUtilTest {

    private RetryTask task = mock(RetryTask.class);
    private ErrorTask error = mock(ErrorTask.class);
    private CheckTask check = mock(CheckTask.class);
    private ExceptionCheckTask exceptionCheck = mock(ExceptionCheckTask.class);

    @Test
    public void testRunWithoutException() throws Exception {
        runRetryTask();
        verify(task, times(1)).run();
        verify(error, times(0)).run(new Exception());
    }

    @Test
    public void testRunWithoutExceptionCheckOk() throws Exception {
        when(check.check()).thenReturn(true);
        runRetryTaskWithCheck();
        verify(task, times(1)).run();
        verify(check, times(1)).check();
        verify(error, times(0)).run(new Exception());
    }

    @Test
    public void testRunWithoutExceptionCheckNok() throws Exception {
        when(check.check()).thenReturn(false);
        runRetryTaskWithCheck();
        verify(task, times(3)).run();
        verify(check, times(3)).check();
        verify(error, times(1)).run(any());
    }

    @Test
    public void testRunWithRecoverableException() throws Exception {
        when(exceptionCheck.check(any())).thenReturn(true);
        doThrow(new IllegalArgumentException()).when(task).run();
        runRetryTaskWithExceptionCheck();
        verify(task, times(3)).run();
        verify(exceptionCheck, times(3)).check(any());
        verify(error, times(1)).run(any());
    }

    @Test
    public void testRunWithNotRecoverableException() throws Exception {
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