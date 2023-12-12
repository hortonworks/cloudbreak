package com.sequenceiq.cloudbreak.quartz.metric.statusmetric;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.quartz.configuration.TransactionalScheduler;

@ExtendWith(MockitoExtension.class)
class StatusMetricCollectorJobServiceTest {

    @Mock
    private StatusMetricCollectorConfiguration statusMetricsCollectorConfiguration;

    @Mock
    private TransactionalScheduler scheduler;

    @InjectMocks
    private StatusMetricCollectorJobService underTest;

    @Test
    void testScheduleIfAlreadyScheduled() throws TransactionService.TransactionExecutionException, SchedulerException {
        when(scheduler.getJobDetail(any())).thenReturn(mock(JobDetail.class));
        underTest.schedule();
        verify(scheduler, times(1)).deleteJob(any());
        verify(scheduler, times(1)).scheduleJob(any(), any());
    }

    @Test
    void testScheduleIfNotScheduled() throws TransactionService.TransactionExecutionException, SchedulerException {
        when(scheduler.getJobDetail(any())).thenReturn(null);
        underTest.schedule();
        verify(scheduler, never()).deleteJob(any());
        verify(scheduler, times(1)).scheduleJob(any(), any());
    }

    @Test
    void testUnscheduleIfAlreadyScheduled() throws TransactionService.TransactionExecutionException, SchedulerException {
        when(scheduler.getJobDetail(any())).thenReturn(mock(JobDetail.class));
        underTest.unschedule();
        verify(scheduler, times(1)).deleteJob(any());
    }

    @Test
    void testUnscheduleIfNotScheduled() throws TransactionService.TransactionExecutionException, SchedulerException {
        when(scheduler.getJobDetail(any())).thenReturn(null);
        underTest.unschedule();
        verify(scheduler, never()).deleteJob(any());
    }
}