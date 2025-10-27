package com.sequenceiq.cloudbreak.job.provider;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.view.StackView;

@ExtendWith(MockitoExtension.class)
class ProviderSyncJobServiceTest {

    private static final String LOCAL_ID = "1";

    @Mock
    private TransactionalScheduler scheduler;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private ProviderSyncConfig providerSyncConfig;

    @Mock
    private Clock clock;

    @Mock
    private JobKey jobKey;

    @InjectMocks
    private ProviderSyncJobService underTest;

    @BeforeEach
    void setUp() {
        lenient().when(providerSyncConfig.getEnabledProviders()).thenReturn(Set.of("AZURE", "AWS"));
    }

    @Test
    void testSchedule() throws TransactionService.TransactionExecutionException {
        JobResource jobResource = mock(JobResource.class);
        when(jobResource.getLocalId()).thenReturn(LOCAL_ID);
        when(jobResource.getRemoteResourceId()).thenReturn("1");
        when(jobResource.getProvider()).thenReturn(Optional.of("AZURE"));
        when(providerSyncConfig.getIntervalInMinutes()).thenReturn(10);
        when(providerSyncConfig.isProviderSyncEnabled()).thenReturn(Boolean.TRUE);
        when(clock.getCurrentInstant()).thenReturn(Instant.now());
        underTest.schedule(new ProviderSyncJobAdapter(jobResource));
        verify(scheduler, times(1)).scheduleJob(any(), any());
    }

    @Test
    void testUnscheduleIfScheduled() throws SchedulerException, TransactionService.TransactionExecutionException {
        JobDetail jobDetail = mock(JobDetail.class);
        when(scheduler.getJobDetail(eq(jobKey))).thenReturn(jobDetail);
        underTest.deregister(jobKey);
        verify(scheduler, times(1)).deleteJob(eq(jobKey));
    }

    @Test
    void testUnscheduleIfNotScheduled() throws SchedulerException, TransactionService.TransactionExecutionException {
        when(scheduler.getJobDetail(eq(jobKey))).thenReturn(null);
        underTest.deregister(eq(jobKey));
        verify(scheduler, never()).deleteJob(eq(jobKey));
    }

    @Test
    void testScheduleWithId() throws TransactionService.TransactionExecutionException {
        configureJobResource();
        StackView stack = mock(StackView.class);
        when(stack.getCloudPlatform()).thenReturn("AZURE");
        when(stack.getId()).thenReturn(1L);
        when(providerSyncConfig.getIntervalInMinutes()).thenReturn(10);
        when(providerSyncConfig.isProviderSyncEnabled()).thenReturn(Boolean.TRUE);
        when(clock.getCurrentInstant()).thenReturn(Instant.now());
        underTest.schedule(stack);
        verify(scheduler, times(1)).scheduleJob(any(), any());
    }

    @Test
    void testScheduleWithIdWhenProviderSyncIsDisabled() throws TransactionService.TransactionExecutionException {
        StackView stackView = mock(StackView.class);
        when(providerSyncConfig.isProviderSyncEnabled()).thenReturn(Boolean.FALSE);

        underTest.schedule(stackView);
        verify(scheduler, never()).scheduleJob(any(), any());
    }

    @Test
    void testScheduleWithResource() throws TransactionService.TransactionExecutionException {
        JobResource jobResource = mock(JobResource.class);
        when(jobResource.getLocalId()).thenReturn(LOCAL_ID);
        when(jobResource.getProvider()).thenReturn(Optional.of("AZURE"));
        when(providerSyncConfig.getIntervalInMinutes()).thenReturn(10);
        when(providerSyncConfig.isProviderSyncEnabled()).thenReturn(Boolean.TRUE);
        when(clock.getCurrentInstant()).thenReturn(Instant.now());
        ProviderSyncJobAdapter resource = new ProviderSyncJobAdapter(jobResource);
        underTest.schedule(resource);
        verify(scheduler, times(1)).scheduleJob(any(), any());
    }

    @Test
    void testScheduleWithResourceWhenProviderSyncIsDisabled() throws TransactionService.TransactionExecutionException {
        when(providerSyncConfig.isProviderSyncEnabled()).thenReturn(Boolean.FALSE);
        JobResource jobResource = mock(JobResource.class);
        ProviderSyncJobAdapter resource = new ProviderSyncJobAdapter(jobResource);
        underTest.schedule(resource);
        verify(scheduler, never()).scheduleJob(any(), any());
    }

    @Test
    void testDeregister() throws SchedulerException, TransactionService.TransactionExecutionException {
        JobDetail jobDetail = mock(JobDetail.class);
        when(scheduler.getJobDetail(eq(jobKey))).thenReturn(jobDetail);
        underTest.deregister(jobKey);
        verify(scheduler, times(1)).deleteJob(eq(jobKey));
    }

    @Test
    void testDeregisterWhenJobNotScheduled() throws SchedulerException, TransactionService.TransactionExecutionException {
        when(scheduler.getJobDetail(eq(jobKey))).thenReturn(null);
        underTest.deregister(eq(jobKey));
        verify(scheduler, never()).deleteJob(eq(jobKey));
    }

    @Test
    void testGetJobGroup() {
        String jobGroup = underTest.getJobGroup();
        assertEquals("provider-sync-jobs", jobGroup);
    }

    @Test
    void testGetScheduler() {
        TransactionalScheduler transactionalScheduler = underTest.getScheduler();
        assertNotNull(transactionalScheduler);
    }

    private void configureJobResource() {
        JobResource jobResource = mock(JobResource.class);
        StackRepository jobResourceRepository = mock(StackRepository.class);
        when(applicationContext.getBean(StackRepository.class)).thenReturn(jobResourceRepository);
        when(jobResourceRepository.getJobResource(1L)).thenReturn(Optional.of(jobResource));
        when(jobResource.getLocalId()).thenReturn(LOCAL_ID);
        when(jobResource.getProvider()).thenReturn(Optional.of("AZURE"));
    }
}