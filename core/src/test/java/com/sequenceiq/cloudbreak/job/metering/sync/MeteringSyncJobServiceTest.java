package com.sequenceiq.cloudbreak.job.metering.sync;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.SchedulerException;
import org.quartz.impl.JobDetailImpl;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.job.metering.scheduler.MeteringSyncTransactionalScheduler;
import com.sequenceiq.cloudbreak.metering.config.MeteringConfig;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.repository.StackRepository;

@ExtendWith(MockitoExtension.class)
class MeteringSyncJobServiceTest {

    private static final String LOCAL_ID = "1";

    @Mock
    private MeteringConfig meteringConfig;

    @Mock
    private MeteringSyncTransactionalScheduler scheduler;

    @Mock
    private ApplicationContext applicationContext;

    @InjectMocks
    private MeteringSyncJobService underTest;

    @Test
    void testScheduleWhenEnabled() throws TransactionService.TransactionExecutionException {
        when(meteringConfig.isEnabled()).thenReturn(Boolean.TRUE);
        JobResource jobResource = mock(JobResource.class);
        when(jobResource.getLocalId()).thenReturn(LOCAL_ID);
        when(meteringConfig.getSyncIntervalInSeconds()).thenReturn(10);
        underTest.schedule(new MeteringSyncJobAdapter(jobResource));
        verify(scheduler, times(1)).scheduleJob(any(), any());
    }

    @Test
    void testScheduleWhenDisabled() throws TransactionService.TransactionExecutionException {
        when(meteringConfig.isEnabled()).thenReturn(Boolean.FALSE);
        JobResource jobResource = mock(JobResource.class);
        when(jobResource.getLocalId()).thenReturn(LOCAL_ID);
        when(meteringConfig.getSyncIntervalInSeconds()).thenReturn(10);
        underTest.schedule(new MeteringSyncJobAdapter(jobResource));
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

    @Test
    void testScheduleIfNotScheduledWhenAlreadyScheduled() throws TransactionService.TransactionExecutionException, SchedulerException {
        lenient().when(meteringConfig.isEnabled()).thenReturn(Boolean.TRUE);
        StackRepository jobResourceRepository = mock(StackRepository.class);
        when(scheduler.getJobDetail(any())).thenReturn(new JobDetailImpl());
        underTest.scheduleIfNotScheduled(Long.valueOf(LOCAL_ID));
        verify(scheduler, never()).scheduleJob(any(), any());
    }

    @Test
    void testScheduleIfNotScheduledWhenNotScheduled() throws TransactionService.TransactionExecutionException, SchedulerException {
        when(meteringConfig.isEnabled()).thenReturn(Boolean.TRUE);
        StackRepository jobResourceRepository = mock(StackRepository.class);
        when(applicationContext.getBean(StackRepository.class)).thenReturn(jobResourceRepository);
        JobResource jobResource = mock(JobResource.class);
        when(jobResource.getLocalId()).thenReturn(LOCAL_ID);
        when(jobResourceRepository.getJobResource(eq(Long.valueOf(LOCAL_ID)))).thenReturn(Optional.of(jobResource));
        when(scheduler.getJobDetail(any())).thenReturn(null);
        when(meteringConfig.getSyncIntervalInSeconds()).thenReturn(10);
        underTest.scheduleIfNotScheduled(Long.valueOf(LOCAL_ID));
        verify(scheduler, times(1)).scheduleJob(any(), any());
    }
}