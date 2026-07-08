package com.sequenceiq.redbeams.sync.provider;

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
import com.sequenceiq.redbeams.repository.DBStackRepository;

@ExtendWith(MockitoExtension.class)
class RdsProviderSyncJobServiceTest {

    private static final String LOCAL_ID = "1";

    @Mock
    private TransactionalScheduler scheduler;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private RdsProviderSyncConfig config;

    @Mock
    private Clock clock;

    @Mock
    private JobKey jobKey;

    @InjectMocks
    private RdsProviderSyncJobService underTest;

    @BeforeEach
    void setUp() {
        lenient().when(config.getEnabledProviders()).thenReturn(Set.of("AZURE", "AWS"));
    }

    @Test
    void testScheduleWithResource() throws TransactionService.TransactionExecutionException {
        JobResource jobResource = mock(JobResource.class);
        when(jobResource.getLocalId()).thenReturn(LOCAL_ID);
        when(jobResource.getProvider()).thenReturn(Optional.of("AZURE"));
        when(config.getIntervalInMinutes()).thenReturn(1440);
        when(config.isEnabled()).thenReturn(Boolean.TRUE);
        when(clock.getCurrentInstant()).thenReturn(Instant.now());

        underTest.schedule(new RdsProviderSyncJobAdapter(jobResource));

        verify(scheduler, times(1)).scheduleJob(any(), any());
    }

    @Test
    void testScheduleWithResourceWhenDisabled() throws TransactionService.TransactionExecutionException {
        when(config.isEnabled()).thenReturn(Boolean.FALSE);
        JobResource jobResource = mock(JobResource.class);

        underTest.schedule(new RdsProviderSyncJobAdapter(jobResource));

        verify(scheduler, never()).scheduleJob(any(), any());
    }

    @Test
    void testScheduleWithResourceWhenProviderNotEnabled() throws TransactionService.TransactionExecutionException {
        when(config.isEnabled()).thenReturn(Boolean.TRUE);
        JobResource jobResource = mock(JobResource.class);
        when(jobResource.getProvider()).thenReturn(Optional.of("GCP"));

        underTest.schedule(new RdsProviderSyncJobAdapter(jobResource));

        verify(scheduler, never()).scheduleJob(any(), any());
    }

    @Test
    void testScheduleWithId() throws TransactionService.TransactionExecutionException {
        JobResource jobResource = mock(JobResource.class);
        DBStackRepository repository = mock(DBStackRepository.class);
        when(applicationContext.getBean(DBStackRepository.class)).thenReturn(repository);
        when(repository.getJobResource(1L)).thenReturn(Optional.of(jobResource));
        when(jobResource.getLocalId()).thenReturn(LOCAL_ID);
        when(jobResource.getProvider()).thenReturn(Optional.of("AWS"));
        when(config.getIntervalInMinutes()).thenReturn(1440);
        when(config.isEnabled()).thenReturn(Boolean.TRUE);
        when(clock.getCurrentInstant()).thenReturn(Instant.now());

        underTest.schedule(1L);

        verify(scheduler, times(1)).scheduleJob(any(), any());
    }

    @Test
    void testUnschedule() throws SchedulerException, TransactionService.TransactionExecutionException {
        JobDetail jobDetail = mock(JobDetail.class);
        when(scheduler.getJobDetail(any())).thenReturn(jobDetail);

        underTest.unschedule(1L);

        verify(scheduler, times(1)).deleteJob(any());
    }

    @Test
    void testDeregisterIfScheduled() throws SchedulerException, TransactionService.TransactionExecutionException {
        JobDetail jobDetail = mock(JobDetail.class);
        when(scheduler.getJobDetail(eq(jobKey))).thenReturn(jobDetail);

        underTest.deregister(jobKey);

        verify(scheduler, times(1)).deleteJob(eq(jobKey));
    }

    @Test
    void testDeregisterIfNotScheduled() throws SchedulerException, TransactionService.TransactionExecutionException {
        when(scheduler.getJobDetail(eq(jobKey))).thenReturn(null);

        underTest.deregister(jobKey);

        verify(scheduler, never()).deleteJob(any());
    }

    @Test
    void testGetJobGroup() {
        assertEquals("rds-provider-sync-jobs", underTest.getJobGroup());
    }

    @Test
    void testGetScheduler() {
        assertNotNull(underTest.getScheduler());
    }
}
