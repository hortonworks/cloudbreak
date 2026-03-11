package com.sequenceiq.cloudbreak.rotation.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
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
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.rotation.config.PeriodicRotationProperties;

@ExtendWith(MockitoExtension.class)
class PeriodicRotationJobServiceTest {

    @Mock
    private TransactionalScheduler scheduler;

    @Mock
    private PeriodicRotationProperties periodicRotationProperties;

    @Mock
    private Clock clock;

    @InjectMocks
    private PeriodicRotationJobService underTest;

    @BeforeEach
    void setup() {
        lenient().when(periodicRotationProperties.getScheduleIntervalMinutes()).thenReturn(60);
        lenient().when(clock.getCurrentInstant()).thenReturn(Instant.now());
    }

    @Test
    void schedulePerAdapterWithStartAtAndInterval() throws Exception {
        JobResource jobResource = mock(JobResource.class);
        when(jobResource.getLocalId()).thenReturn("periodic-secret-rotation-job:");
        when(jobResource.getRemoteResourceId()).thenReturn("crn:1");
        PeriodicRotationJobAdapter adapter = new PeriodicRotationJobAdapter(jobResource);
        when(scheduler.getJobDetail(any(JobKey.class))).thenReturn(null);

        underTest.schedule(adapter);

        ArgumentCaptor<JobDetail> jdCaptor = ArgumentCaptor.forClass(JobDetail.class);
        ArgumentCaptor<Trigger> trigCaptor = ArgumentCaptor.forClass(Trigger.class);
        verify(scheduler, times(1)).scheduleJob(jdCaptor.capture(), trigCaptor.capture());

        JobDetail jobDetail = jdCaptor.getValue();
        assertThat(jobDetail.getKey().getGroup()).isEqualTo("periodic-secret-rotation-job-group");
        assertThat(jobDetail.getKey().getName()).isEqualTo("periodic-secret-rotation-job:");
        assertThat(jobDetail.getJobDataMap().getString("remoteResourceCrn")).isEqualTo("crn:1");

        Trigger trigger = trigCaptor.getValue();
        assertThat(trigger.getStartTime()).isNotNull();
        SimpleTriggerImpl impl = (SimpleTriggerImpl) trigger;
        assertThat(impl.getRepeatInterval()).isEqualTo(60L * 60L * 1000L);
    }

    @Test
    void unschedulesExistingJobBeforeRescheduling() throws Exception {
        JobResource jobResource = mock(JobResource.class);
        when(jobResource.getLocalId()).thenReturn("crn:existing");
        when(jobResource.getRemoteResourceId()).thenReturn("crn:existing");
        PeriodicRotationJobAdapter adapter = new PeriodicRotationJobAdapter(jobResource);
        when(scheduler.getJobDetail(any(JobKey.class))).thenReturn(org.mockito.Mockito.mock(JobDetail.class));

        underTest.schedule(adapter);

        verify(scheduler, times(1)).deleteJob(any(JobKey.class));
        verify(scheduler, times(1)).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }
}

