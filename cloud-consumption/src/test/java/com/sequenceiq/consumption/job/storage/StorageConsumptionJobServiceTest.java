package com.sequenceiq.consumption.job.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.consumption.configuration.repository.ConsumptionRepository;
import com.sequenceiq.consumption.domain.Consumption;
import com.sequenceiq.consumption.service.ConsumptionService;

@ExtendWith(MockitoExtension.class)
public class StorageConsumptionJobServiceTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private Scheduler scheduler;

    @Mock
    private StorageConsumptionConfig storageConsumptionConfig;

    @Mock
    private ConsumptionService consumptionService;

    @Mock
    private ConsumptionRepository consumptionRepository;

    @InjectMocks
    private StorageConsumptionJobService underTest;

    @Captor
    private ArgumentCaptor<JobDetail> jobDetailCaptor;

    @Captor
    private ArgumentCaptor<JobKey> jobKeyCaptor;

    @BeforeEach
    public void setUp() {
        lenient().when(applicationContext.getBean(ConsumptionRepository.class)).thenReturn(consumptionRepository);
        lenient().when(storageConsumptionConfig.getIntervalInMinutes()).thenReturn(30);
    }

    @Test
    public void testScheduleByIdDisabled() {
        when(storageConsumptionConfig.isStorageConsumptionEnabled()).thenReturn(false);
        JobResource jobResource = mock(JobResource.class);
        when(consumptionRepository.getJobResource(1L)).thenReturn(Optional.of(jobResource));

        underTest.schedule(1L);

        verifyNoInteractions(scheduler);
    }

    @Test
    public void testScheduleByIdJobAlreadyRunning() throws SchedulerException {
        when(storageConsumptionConfig.isStorageConsumptionEnabled()).thenReturn(true);
        mockJobResource(1L);
        when(scheduler.getJobDetail(any())).thenReturn(mock(JobDetail.class));

        underTest.schedule(1L);

        verify(scheduler, never()).scheduleJob(any(), any());
    }

    @Test
    public void testScheduleByIdJobNotRunning() throws SchedulerException {
        when(storageConsumptionConfig.isStorageConsumptionEnabled()).thenReturn(true);
        mockJobResource(1L);
        when(scheduler.getJobDetail(any())).thenReturn(null);

        underTest.schedule(1L);

        verify(scheduler, times(1)).scheduleJob(any(), any());
    }

    @Test
    public void testScheduleWithAggregationNoJobRunningForGroup() throws SchedulerException {
        Consumption consumptionToSchedule = consumption(1L);
        when(consumptionService.isAggregationRequired(consumptionToSchedule)).thenReturn(true);
        when(consumptionService.findAllStorageConsumptionForEnvCrnAndBucketName(
                consumptionToSchedule.getEnvironmentCrn(), consumptionToSchedule.getStorageLocation()))
                .thenReturn(List.of(consumptionToSchedule));
        mockSchedulingById(1L);

        underTest.schedule(consumptionToSchedule);

        verify(scheduler, times(1)).scheduleJob(jobDetailCaptor.capture(), any());
        assertEquals("1", jobDetailCaptor.getValue().getKey().getName());
    }

    @Test
    public void testScheduleWithAggregationJobAlreadyRunningForGroup() throws SchedulerException {
        Consumption consumptionToSchedule = consumption(1L);
        Consumption consumptionOther = consumption(2L);
        when(consumptionService.isAggregationRequired(consumptionToSchedule)).thenReturn(true);
        when(consumptionService.findAllStorageConsumptionForEnvCrnAndBucketName(
                consumptionToSchedule.getEnvironmentCrn(), consumptionToSchedule.getStorageLocation()))
                .thenReturn(List.of(consumptionToSchedule, consumptionOther));
        doReturn(null).when(scheduler).getJobDetail(argThat((JobKey jobkey) -> "1".equals(jobkey.getName())));
        doReturn(mock(JobDetail.class)).when(scheduler).getJobDetail(argThat((JobKey jobkey) -> "2".equals(jobkey.getName())));

        underTest.schedule(consumptionToSchedule);

        verify(scheduler, never()).scheduleJob(any(), any());
    }

    @Test
    public void testUnscheduleWithAggregationNoJobRunningForConsumption() throws SchedulerException {
        Consumption consumptionToUnSchedule = consumption(1L);
        when(consumptionService.isAggregationRequired(consumptionToUnSchedule)).thenReturn(true);
        doReturn(null).when(scheduler).getJobDetail(argThat((JobKey jobkey) -> "1".equals(jobkey.getName())));

        underTest.unschedule(consumptionToUnSchedule);

        verify(scheduler, never()).deleteJob(any());
    }

    @Test
    public void testUnscheduleWithAggregationJobRunningForConsumptionNoReschedule() throws SchedulerException {
        Consumption consumptionToUnSchedule = consumption(1L);
        when(consumptionService.isAggregationRequired(consumptionToUnSchedule)).thenReturn(true);
        doReturn(mock(JobDetail.class)).when(scheduler).getJobDetail(argThat((JobKey jobkey) -> "1".equals(jobkey.getName())));
        when(consumptionService.findAllStorageConsumptionForEnvCrnAndBucketName(
                consumptionToUnSchedule.getEnvironmentCrn(), consumptionToUnSchedule.getStorageLocation()))
                .thenReturn(List.of(consumptionToUnSchedule));

        underTest.unschedule(consumptionToUnSchedule);

        verify(scheduler, times(1)).deleteJob(jobKeyCaptor.capture());
        assertEquals("1", jobKeyCaptor.getValue().getName());
        verify(scheduler, never()).scheduleJob(any(), any());
    }

    @Test
    public void testUnscheduleWithAggregationJobRunningForConsumptionWithReschedule() throws SchedulerException {
        Consumption consumptionToUnSchedule = consumption(1L);
        Consumption consumptionOther = consumption(2L);
        when(consumptionService.isAggregationRequired(consumptionToUnSchedule)).thenReturn(true);
        doReturn(mock(JobDetail.class)).when(scheduler).getJobDetail(argThat((JobKey jobkey) -> "1".equals(jobkey.getName())));
        when(consumptionService.findAllStorageConsumptionForEnvCrnAndBucketName(
                consumptionToUnSchedule.getEnvironmentCrn(), consumptionToUnSchedule.getStorageLocation()))
                .thenReturn(List.of(consumptionToUnSchedule, consumptionOther));
        mockSchedulingById(2L);

        underTest.unschedule(consumptionToUnSchedule);

        verify(scheduler, times(1)).deleteJob(jobKeyCaptor.capture());
        assertEquals("1", jobKeyCaptor.getValue().getName());
        verify(scheduler, times(1)).scheduleJob(jobDetailCaptor.capture(), any());
        assertEquals("2", jobDetailCaptor.getValue().getKey().getName());
    }

    private void mockJobResource(Long id) {
        JobResource jobResource = mock(JobResource.class);
        when(jobResource.getLocalId()).thenReturn(id.toString());
        when(jobResource.getRemoteResourceId()).thenReturn("crn");
        when(consumptionRepository.getJobResource(id)).thenReturn(Optional.of(jobResource));
    }

    private void mockSchedulingById(Long id) throws SchedulerException {
        when(storageConsumptionConfig.isStorageConsumptionEnabled()).thenReturn(true);
        mockJobResource(id);
    }

    private Consumption consumption(Long id) {
        Consumption consumption = new Consumption();
        consumption.setResourceCrn("crn");
        consumption.setEnvironmentCrn("env-crn");
        consumption.setStorageLocation("location");
        consumption.setId(id);
        return consumption;
    }

}
