package com.sequenceiq.periscope.monitor.executor;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.periscope.monitor.evaluator.EvaluatorExecutor;

public class ExecutorServiceWithRegistryTest {

    public static final long CLUSTER_ID = 1L;

    @Mock
    private EvaluatorExecutorRegistry evaluatorExecutorRegistry;

    @Mock
    private ExecutorService executorService;

    @InjectMocks
    private ExecutorServiceWithRegistry executorServiceWithRegistry;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testSubmitIfAbsent() {
        when(evaluatorExecutorRegistry.putIfAbsent(any(), eq(CLUSTER_ID))).thenReturn(true);

        executorServiceWithRegistry.submitIfAbsent(getEvaluatorExecutor(), CLUSTER_ID);

        verify(executorService).submit((EvaluatorExecutor) any());
        verify(evaluatorExecutorRegistry, never()).remove(any(), anyLong());
    }

    @Test
    public void testSubmitIfAbsentWhenAlreadyPresent() {
        when(evaluatorExecutorRegistry.putIfAbsent(any(), eq(CLUSTER_ID))).thenReturn(false);

        executorServiceWithRegistry.submitIfAbsent(getEvaluatorExecutor(), CLUSTER_ID);

        verify(executorService, never()).submit((EvaluatorExecutor) any());
        verify(evaluatorExecutorRegistry, never()).remove(any(), anyLong());
    }

    @Test
    public void testSubmitIfAbsentWhenExecutorServiceThrows() {
        when(evaluatorExecutorRegistry.putIfAbsent(any(), eq(CLUSTER_ID))).thenReturn(true);
        when(executorService.submit((Runnable) any())).thenThrow(new RejectedExecutionException());

        try {
            executorServiceWithRegistry.submitIfAbsent(getEvaluatorExecutor(), CLUSTER_ID);
            fail("Expected rejectedExecutionException");
        } catch (RejectedExecutionException e) {

        }

        verify(executorService).submit((EvaluatorExecutor) any());
        verify(evaluatorExecutorRegistry).remove(any(), eq(CLUSTER_ID));
    }

    private EvaluatorExecutor getEvaluatorExecutor() {
        EvaluatorExecutor evaluatorExecutor = mock(EvaluatorExecutor.class);
        when(evaluatorExecutor.getName()).thenReturn("");
        return evaluatorExecutor;
    }

}
