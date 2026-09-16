package com.sequenceiq.maintenance.dispatcher.scheduled;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.impl.triggers.SimpleTriggerImpl;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;

@ExtendWith(MockitoExtension.class)
class MaintenanceWindowDispatcherJobServiceTest {

    @Mock
    private TransactionalScheduler scheduler;

    @Mock
    private MaintenanceWindowDispatcherConfig dispatcherConfig;

    @Mock
    private Clock clock;

    @InjectMocks
    private MaintenanceWindowDispatcherJobService underTest;

    @BeforeEach
    void setUp() {
        lenient().when(dispatcherConfig.isEnabled()).thenReturn(true);
        lenient().when(dispatcherConfig.getIntervalInMinutes()).thenReturn(15);
        lenient().when(clock.getCurrentInstant()).thenReturn(Instant.parse("2026-01-05T12:00:00Z"));
    }

    @Test
    void scheduleSkippedWhenDispatcherDisabled() throws Exception {
        when(dispatcherConfig.isEnabled()).thenReturn(false);

        underTest.schedule();

        verify(scheduler, never()).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }

    @Test
    void scheduleRegistersJobWithConfiguredInterval() throws Exception {
        when(scheduler.getJobDetail(any(JobKey.class))).thenReturn(null);

        underTest.schedule();

        ArgumentCaptor<JobDetail> jobDetailCaptor = ArgumentCaptor.forClass(JobDetail.class);
        ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
        verify(scheduler, times(1)).scheduleJob(jobDetailCaptor.capture(), triggerCaptor.capture());

        JobDetail jobDetail = jobDetailCaptor.getValue();
        assertThat(jobDetail.getKey().getName()).isEqualTo("maintenance-window-dispatcher-job");
        assertThat(jobDetail.getKey().getGroup()).isEqualTo("maintenance-window-dispatcher-job-group");
        assertThat(jobDetail.getJobClass()).isEqualTo(MaintenanceWindowDispatcherJob.class);

        Trigger trigger = triggerCaptor.getValue();
        assertThat(trigger).isInstanceOf(SimpleTriggerImpl.class);
        SimpleTriggerImpl simpleTrigger = (SimpleTriggerImpl) trigger;
        assertThat(simpleTrigger.getRepeatInterval()).isEqualTo(15L * 60_000L);
    }

    @Test
    void scheduleUnschedulesExistingJobBeforeRescheduling() throws Exception {
        when(scheduler.getJobDetail(JobKey.jobKey("maintenance-window-dispatcher-job",
                "maintenance-window-dispatcher-job-group"))).thenReturn(org.mockito.Mockito.mock(JobDetail.class));

        underTest.schedule();

        verify(scheduler).deleteJob(JobKey.jobKey("maintenance-window-dispatcher-job",
                "maintenance-window-dispatcher-job-group"));
        verify(scheduler, times(1)).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }
}
