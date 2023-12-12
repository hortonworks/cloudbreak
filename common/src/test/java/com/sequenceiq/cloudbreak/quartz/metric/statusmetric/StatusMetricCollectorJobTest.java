package com.sequenceiq.cloudbreak.quartz.metric.statusmetric;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@ExtendWith(MockitoExtension.class)
class StatusMetricCollectorJobTest {

    @Mock
    private StatusMetricCollector statusMetricCollector;

    @InjectMocks
    private StatusMetricCollectorJob underTest;

    @Test
    void testExecuteTraced() throws JobExecutionException {
        underTest.executeTracedJob(mock(JobExecutionContext.class));
        verify(statusMetricCollector, times(1)).collectStatusMetrics();
    }
}