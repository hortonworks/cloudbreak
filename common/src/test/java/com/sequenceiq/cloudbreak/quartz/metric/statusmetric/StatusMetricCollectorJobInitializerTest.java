package com.sequenceiq.cloudbreak.quartz.metric.statusmetric;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatusMetricCollectorJobInitializerTest {

    @Mock
    private StatusMetricCollectorConfiguration statusMetricCollectorConfiguration;

    @Mock
    private StatusMetricCollectorJobService statusMetricCollectorJobService;

    @InjectMocks
    private StatusMetricCollectorJobInitializer underTest;

    @Test
    void testInitJobsWhenStatusMetricCollectorIsDisabled() {
        when(statusMetricCollectorConfiguration.isEnabled()).thenReturn(Boolean.FALSE);
        underTest.initJobs();
        verify(statusMetricCollectorJobService, never()).schedule();
    }

    @Test
    void testInitJobsWhenStatusMetricCollectorIsEnabled() {
        when(statusMetricCollectorConfiguration.isEnabled()).thenReturn(Boolean.TRUE);
        underTest.initJobs();
        verify(statusMetricCollectorJobService, times(1)).schedule();
    }
}