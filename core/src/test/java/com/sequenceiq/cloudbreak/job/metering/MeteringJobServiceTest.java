package com.sequenceiq.cloudbreak.job.metering;

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
import org.quartz.SchedulerException;
import org.quartz.impl.JobDetailImpl;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.metering.config.MeteringConfig;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;

@ExtendWith(MockitoExtension.class)
class MeteringJobServiceTest {

    private static final String LOCAL_ID = "LOCAL_ID";

    @Mock
    private MeteringConfig meteringConfig;

    @Mock
    private MeteringTransactionalScheduler scheduler;

    @Mock
    private ApplicationContext applicationContext;

    @InjectMocks
    private MeteringJobService underTest;

    @Test
    void testScheduleWhenEnabled() throws TransactionService.TransactionExecutionException {
        when(meteringConfig.isEnabled()).thenReturn(Boolean.TRUE);
        JobResource jobResource = mock(JobResource.class);
        when(jobResource.getLocalId()).thenReturn(LOCAL_ID);
        when(meteringConfig.getIntervalInSeconds()).thenReturn(10);
        underTest.schedule(new MeteringJobAdapter(jobResource));
        verify(scheduler, times(1)).scheduleJob(any(), any());
    }

    @Test
    void testScheduleWhenDisabled() throws TransactionService.TransactionExecutionException {
        when(meteringConfig.isEnabled()).thenReturn(Boolean.FALSE);
        JobResource jobResource = mock(JobResource.class);
        when(jobResource.getLocalId()).thenReturn(LOCAL_ID);
        when(meteringConfig.getIntervalInSeconds()).thenReturn(10);
        underTest.schedule(new MeteringJobAdapter(jobResource));
        verify(scheduler, never()).scheduleJob(any(), any());
    }

    @Test
    void testUnscheduleIfScheduled() throws SchedulerException, TransactionService.TransactionExecutionException {
        when(scheduler.getJobDetail(any())).thenReturn(new JobDetailImpl());
        underTest.unschedule(LOCAL_ID);
        verify(scheduler, times(1)).deleteJob(any());
    }

    @Test
    void testUnscheduleIfNotScheduled() throws SchedulerException, TransactionService.TransactionExecutionException {
        when(scheduler.getJobDetail(any())).thenReturn(null);
        underTest.unschedule(LOCAL_ID);
        verify(scheduler, never()).deleteJob(any());
    }
}