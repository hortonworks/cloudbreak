package com.sequenceiq.cloudbreak.job.instancechecker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.temporal.TemporalAmount;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.JobDetailImpl;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.conf.InstanceCheckerConfig;
import com.sequenceiq.cloudbreak.job.instancechecker.scheduler.InstanceCheckerTransactionalScheduler;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.repository.StackRepository;

@ExtendWith(MockitoExtension.class)
class InstanceCheckerJobServiceTest {

    private static final String LOCAL_ID = "1";

    private static final String RESOURCE_CRN = "resource-crn";

    @Mock
    private InstanceCheckerConfig instanceCheckerConfig;

    @Mock
    private InstanceCheckerTransactionalScheduler scheduler;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private Clock clock;

    @InjectMocks
    private InstanceCheckerJobService underTest;

    @Captor
    private ArgumentCaptor<JobDetail> jobDetailCaptor;

    @Captor
    private ArgumentCaptor<Trigger> triggerCaptor;

    @Test
    void testScheduleWhenEnabled() throws TransactionService.TransactionExecutionException {
        JobResource jobResource = mock(JobResource.class);
        Date date = mock(Date.class);
        when(instanceCheckerConfig.isEnabled()).thenReturn(Boolean.TRUE);
        when(jobResource.getLocalId()).thenReturn(LOCAL_ID);
        when(jobResource.getRemoteResourceId()).thenReturn(RESOURCE_CRN);
        when(jobResource.getProvider()).thenReturn(Optional.of("AWS"));
        when(instanceCheckerConfig.getInstanceCheckerDelayInSeconds()).thenReturn(10);
        when(instanceCheckerConfig.getInstanceCheckerIntervalInHours()).thenReturn(10);
        when(clock.getDateForDelayedStart(any(TemporalAmount.class))).thenReturn(date);

        underTest.schedule(new InstanceCheckerJobAdapter(jobResource));

        verify(scheduler).scheduleJob(jobDetailCaptor.capture(), triggerCaptor.capture());
        verifyJobDetailAndTrigger(date);
    }

    @Test
    void testScheduleWhenDisabled() throws TransactionService.TransactionExecutionException {
        JobResource jobResource = mock(JobResource.class);
        Date date = mock(Date.class);
        when(instanceCheckerConfig.isEnabled()).thenReturn(Boolean.FALSE);
        when(jobResource.getLocalId()).thenReturn(LOCAL_ID);
        when(instanceCheckerConfig.getInstanceCheckerIntervalInHours()).thenReturn(10);
        when(instanceCheckerConfig.getInstanceCheckerDelayInSeconds()).thenReturn(10);
        when(clock.getDateForDelayedStart(any(TemporalAmount.class))).thenReturn(date);

        underTest.schedule(new InstanceCheckerJobAdapter(jobResource));

        verify(scheduler, never()).scheduleJob(any(), any());
    }

    @Test
    void testScheduleWhenAlreadyScheduled() throws TransactionService.TransactionExecutionException, SchedulerException {
        JobResource jobResource = mock(JobResource.class);
        Date date = mock(Date.class);
        JobKey jobKey = JobKey.jobKey(LOCAL_ID, "instance-checker-jobs");
        when(instanceCheckerConfig.isEnabled()).thenReturn(Boolean.TRUE);
        when(jobResource.getLocalId()).thenReturn(LOCAL_ID);
        when(jobResource.getRemoteResourceId()).thenReturn(RESOURCE_CRN);
        when(jobResource.getProvider()).thenReturn(Optional.of("AWS"));
        when(instanceCheckerConfig.getInstanceCheckerDelayInSeconds()).thenReturn(10);
        when(instanceCheckerConfig.getInstanceCheckerIntervalInHours()).thenReturn(10);
        when(clock.getDateForDelayedStart(any(TemporalAmount.class))).thenReturn(date);
        when(scheduler.getJobDetail(jobKey)).thenReturn(new JobDetailImpl());

        underTest.schedule(new InstanceCheckerJobAdapter(jobResource));

        verify(scheduler).deleteJob(jobKey);
        verify(scheduler).scheduleJob(jobDetailCaptor.capture(), triggerCaptor.capture());
        verifyJobDetailAndTrigger(date);
    }

    @Test
    void testUnscheduleIfScheduled() throws SchedulerException, TransactionService.TransactionExecutionException {
        JobKey jobKey = JobKey.jobKey(LOCAL_ID, "instance-checker-jobs");
        when(scheduler.getJobDetail(jobKey)).thenReturn(new JobDetailImpl());

        underTest.unschedule(LOCAL_ID);

        verify(scheduler).deleteJob(jobKey);
    }

    @Test
    void testUnscheduleIfNotScheduled() throws SchedulerException, TransactionService.TransactionExecutionException {
        JobKey jobKey = JobKey.jobKey(LOCAL_ID, "instance-checker-jobs");
        when(scheduler.getJobDetail(jobKey)).thenReturn(null);

        underTest.unschedule(LOCAL_ID);

        verify(scheduler, never()).deleteJob(any());
    }

    @Test
    void testScheduleIfNotScheduledWhenAlreadyScheduled() throws TransactionService.TransactionExecutionException, SchedulerException {
        JobKey jobKey = JobKey.jobKey(LOCAL_ID, "instance-checker-jobs");
        when(scheduler.getJobDetail(jobKey)).thenReturn(new JobDetailImpl());

        underTest.scheduleIfNotScheduled(Long.valueOf(LOCAL_ID));

        verify(scheduler, never()).scheduleJob(any(), any());
    }

    @Test
    void testScheduleIfNotScheduledWhenNotScheduled() throws TransactionService.TransactionExecutionException, SchedulerException {
        JobResource jobResource = mock(JobResource.class);
        StackRepository jobResourceRepository = mock(StackRepository.class);
        Date date = mock(Date.class);
        JobKey jobKey = JobKey.jobKey(LOCAL_ID, "instance-checker-jobs");
        when(scheduler.getJobDetail(jobKey)).thenReturn(null);
        when(instanceCheckerConfig.isEnabled()).thenReturn(Boolean.TRUE);
        when(applicationContext.getBean(StackRepository.class)).thenReturn(jobResourceRepository);
        when(jobResource.getLocalId()).thenReturn(LOCAL_ID);
        when(jobResource.getRemoteResourceId()).thenReturn(RESOURCE_CRN);
        when(jobResource.getProvider()).thenReturn(Optional.of("AWS"));
        when(jobResourceRepository.getJobResource(Long.valueOf(LOCAL_ID))).thenReturn(Optional.of(jobResource));
        when(instanceCheckerConfig.getInstanceCheckerIntervalInHours()).thenReturn(10);
        when(instanceCheckerConfig.getInstanceCheckerDelayInSeconds()).thenReturn(10);
        when(clock.getDateForDelayedStart(any(TemporalAmount.class))).thenReturn(date);

        underTest.scheduleIfNotScheduled(Long.valueOf(LOCAL_ID));

        verify(scheduler).scheduleJob(jobDetailCaptor.capture(), triggerCaptor.capture());
        verifyJobDetailAndTrigger(date);
    }

    private void verifyJobDetailAndTrigger(Date date) {
        JobDetail jobDetail = jobDetailCaptor.getValue();
        assertEquals(InstanceCheckerJob.class, jobDetail.getJobClass());
        JobKey jobKey = jobDetail.getKey();
        assertEquals(LOCAL_ID, jobKey.getName());
        assertEquals("instance-checker-jobs", jobKey.getGroup());
        assertEquals("Instance checker job", jobDetail.getDescription());
        JobDataMap jobDataMap = jobDetail.getJobDataMap();
        assertThat(jobDataMap).containsAllEntriesOf(Map.of("localId", LOCAL_ID, "remoteResourceCrn", RESOURCE_CRN, "provider", "AWS"));
        assertTrue(jobDetail.isDurable());

        Trigger trigger = triggerCaptor.getValue();
        assertEquals(jobKey, trigger.getJobKey());
        assertEquals(jobDataMap, trigger.getJobDataMap());
        TriggerKey triggerKey = trigger.getKey();
        assertEquals(LOCAL_ID, triggerKey.getName());
        assertEquals("instance-checker-triggers", triggerKey.getGroup());
        assertEquals(date, trigger.getStartTime());
    }
}