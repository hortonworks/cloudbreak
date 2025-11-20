package com.sequenceiq.cloudbreak.job.diskusage;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.DATALAKE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.WORKLOAD;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType.NONE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType.NON_HA;
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;

@ExtendWith(MockitoExtension.class)
class DiskUsageSyncJobServiceTest {

    private static final String LOCAL_ID = "1";

    @Mock
    private TransactionalScheduler scheduler;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private DiskUsageSyncConfig diskUsageSyncConfig;

    @Mock
    private StackDtoService stackService;

    @Mock
    private Clock clock;

    @Mock
    private JobKey jobKey;

    @InjectMocks
    private DiskUsageSyncJobService underTest;

    @Test
    void testSchedule() throws TransactionService.TransactionExecutionException {
        JobResource jobResource = mock(JobResource.class);
        when(jobResource.getLocalId()).thenReturn(LOCAL_ID);
        when(jobResource.getRemoteResourceId()).thenReturn("1");
        when(diskUsageSyncConfig.getIntervalInMinutes()).thenReturn(10);
        when(diskUsageSyncConfig.isDiskUsageSyncEnabled()).thenReturn(Boolean.TRUE);
        when(clock.getCurrentInstant()).thenReturn(Instant.now());
        configureEmbeddedDhStack();
        underTest.schedule(new DiskUsageSyncJobAdapter(jobResource));
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
        when(stack.getId()).thenReturn(1L);
        configureEmbeddedDhStack();
        when(diskUsageSyncConfig.getIntervalInMinutes()).thenReturn(10);
        when(diskUsageSyncConfig.isDiskUsageSyncEnabled()).thenReturn(Boolean.TRUE);
        when(clock.getCurrentInstant()).thenReturn(Instant.now());
        underTest.schedule(stack);
        verify(scheduler, times(1)).scheduleJob(any(), any());
    }

    @Test
    void testScheduleWithIdDl() throws TransactionService.TransactionExecutionException {
        configureJobResource();
        StackView stack = mock(StackView.class);
        when(stack.getId()).thenReturn(1L);
        configureStack(DATALAKE, null);
        when(diskUsageSyncConfig.isDiskUsageSyncEnabled()).thenReturn(Boolean.TRUE);
        underTest.schedule(stack);
        verify(scheduler, never()).scheduleJob(any(), any());
    }

    @Test
    void testScheduleWithIdExternalDbDh() throws TransactionService.TransactionExecutionException {
        configureJobResource();
        StackView stack = mock(StackView.class);
        when(stack.getId()).thenReturn(1L);
        configureStack(WORKLOAD, NON_HA);
        when(diskUsageSyncConfig.isDiskUsageSyncEnabled()).thenReturn(Boolean.TRUE);
        underTest.schedule(stack);
        verify(scheduler, never()).scheduleJob(any(), any());
    }

    @Test
    void testScheduleWithIdWhenDiskUsageSyncIsDisabled() throws TransactionService.TransactionExecutionException {
        StackView stackView = mock(StackView.class);
        when(diskUsageSyncConfig.isDiskUsageSyncEnabled()).thenReturn(Boolean.FALSE);

        underTest.schedule(stackView);
        verify(scheduler, never()).scheduleJob(any(), any());
    }

    @Test
    void testScheduleWithResource() throws TransactionService.TransactionExecutionException {
        JobResource jobResource = mock(JobResource.class);
        when(jobResource.getLocalId()).thenReturn(LOCAL_ID);
        when(diskUsageSyncConfig.getIntervalInMinutes()).thenReturn(10);
        when(diskUsageSyncConfig.isDiskUsageSyncEnabled()).thenReturn(Boolean.TRUE);
        when(clock.getCurrentInstant()).thenReturn(Instant.now());
        configureEmbeddedDhStack();
        DiskUsageSyncJobAdapter resource = new DiskUsageSyncJobAdapter(jobResource);
        underTest.schedule(resource);
        verify(scheduler, times(1)).scheduleJob(any(), any());
    }

    @Test
    void testScheduleWithResourceWhenDiskUsageSyncIsDisabled() throws TransactionService.TransactionExecutionException {
        when(diskUsageSyncConfig.isDiskUsageSyncEnabled()).thenReturn(Boolean.FALSE);
        JobResource jobResource = mock(JobResource.class);
        DiskUsageSyncJobAdapter resource = new DiskUsageSyncJobAdapter(jobResource);
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
        assertEquals("diskusage-sync-jobs", jobGroup);
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
        lenient().when(jobResource.getProvider()).thenReturn(Optional.of("AZURE"));
    }

    private void configureEmbeddedDhStack() {
        configureStack(WORKLOAD, NONE);
    }

    private void configureStack(StackType stackType, DatabaseAvailabilityType databaseAvailabilityType) {
        StackDto stack = mock(StackDto.class);
        when(stack.getType()).thenReturn(stackType);
        if (databaseAvailabilityType != null) {
            Database database = mock(Database.class);
            when(database.getExternalDatabaseAvailabilityType()).thenReturn(databaseAvailabilityType);
            when(stack.getDatabase()).thenReturn(database);
        }
        when(stackService.getById(1L)).thenReturn(stack);

    }

}