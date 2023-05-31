package com.sequenceiq.periscope.utils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.logger.concurrent.MDCCleanerScheduledExecutor;
import com.sequenceiq.periscope.domain.MetricType;
import com.sequenceiq.periscope.service.PeriscopeMetricService;

public class MetricUtilsTest {

    @Mock
    private PeriscopeMetricService metricService;

    @InjectMocks
    private MetricUtils underTest;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testSubmitThreadPoolExecutorParameters() {
        ThreadPoolExecutor threadPoolExecutor = mock(MDCCleanerScheduledExecutor.class);
        when(threadPoolExecutor.getCorePoolSize()).thenReturn(5);
        BlockingQueue blockingQueue = mock(BlockingQueue.class);
        when(blockingQueue.size()).thenReturn(6);
        when(threadPoolExecutor.getQueue()).thenReturn(blockingQueue);
        when(threadPoolExecutor.getCompletedTaskCount()).thenReturn(7L);
        when(threadPoolExecutor.getActiveCount()).thenReturn(8);

        underTest.submitThreadPoolExecutorParameters(threadPoolExecutor);

        verify(metricService).gauge(MetricType.THREADPOOL_ACTIVE_THREADS, 8);
        verify(metricService).gauge(MetricType.THREADPOOL_TASKS_COMPLETED, 7L);
        verify(metricService).gauge(MetricType.THREADPOOL_QUEUE_SIZE, 6);
        verify(metricService).gauge(MetricType.THREADPOOL_THREADS_TOTAL, 5);
    }

}
