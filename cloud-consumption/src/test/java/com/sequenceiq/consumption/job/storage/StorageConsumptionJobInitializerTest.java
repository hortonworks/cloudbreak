package com.sequenceiq.consumption.job.storage;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.consumption.service.ConsumptionService;

@ExtendWith(MockitoExtension.class)
public class StorageConsumptionJobInitializerTest {

    @Mock
    private StorageConsumptionJobService jobService;

    @Mock
    private ConsumptionService consumptionService;

    @Mock
    private StorageConsumptionConfig storageConsumptionConfig;

    @Mock
    private JobResource consumption1;

    @Mock
    private JobResource consumption2;

    @InjectMocks
    private StorageConsumptionJobInitializer underTest;

    @Captor
    private ArgumentCaptor<StorageConsumptionJobAdapter> jobAdapterCaptor;

    @Test
    public void testJobDisabled() {
        when(storageConsumptionConfig.isStorageConsumptionEnabled()).thenReturn(false);

        underTest.initJobs();

        verifyNoInteractions(consumptionService);
        verifyNoInteractions(jobService);
    }

    @Test
    public void testNoJobsPresent() {
        when(storageConsumptionConfig.isStorageConsumptionEnabled()).thenReturn(true);
        when(consumptionService.findAllStorageConsumptionJobResource()).thenReturn(List.of());

        underTest.initJobs();

        verify(consumptionService, times(1)).findAllStorageConsumptionJobResource();
        verifyNoInteractions(jobService);
    }

    @Test
    public void testJobsScheduled() {
        when(storageConsumptionConfig.isStorageConsumptionEnabled()).thenReturn(true);
        when(consumptionService.findAllStorageConsumptionJobResource()).thenReturn(List.of(consumption1, consumption2));

        underTest.initJobs();

        verify(consumptionService, times(1)).findAllStorageConsumptionJobResource();
        verify(jobService, times(2)).schedule(jobAdapterCaptor.capture());
        Assertions.assertEquals(List.of(consumption1, consumption2),
                jobAdapterCaptor.getAllValues().stream().map(JobResourceAdapter::getJobResource).collect(Collectors.toList()));
    }

}
