package com.sequenceiq.consumption.job.storage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.consumption.domain.Consumption;
import com.sequenceiq.consumption.service.ConsumptionService;

@ExtendWith(MockitoExtension.class)
public class StorageConsumptionJobInitializerTest {

    @Mock
    private StorageConsumptionJobService jobService;

    @Mock
    private ConsumptionService consumptionService;

    @Mock
    private StorageConsumptionConfig storageConsumptionConfig;

    @InjectMocks
    private StorageConsumptionJobInitializer underTest;

    @Captor
    private ArgumentCaptor<Long> jobIdCaptor;

    @Test
    public void testJobDisabled() {
        when(storageConsumptionConfig.isStorageConsumptionEnabled()).thenReturn(false);

        underTest.initJobs();

        verifyNoInteractions(consumptionService);
        verifyNoInteractions(jobService);
    }

    @Test
    public void testNoConsumptionToSchedule() {
        when(storageConsumptionConfig.isStorageConsumptionEnabled()).thenReturn(true);

        when(consumptionService.findAllConsumption()).thenReturn(List.of());
        when(consumptionService.groupConsumptionsByEnvCrnAndBucketName(any())).thenReturn(List.of());

        underTest.initJobs();

        verifyNoInteractions(jobService);
    }

    @Test
    public void testNonAggregatedScheduling() {
        when(storageConsumptionConfig.isStorageConsumptionEnabled()).thenReturn(true);

        Consumption consumption1 = consumption(1L);
        Consumption consumption2 = consumption(2L);

        when(consumptionService.findAllConsumption()).thenReturn(List.of(consumption1, consumption2));
        when(consumptionService.isAggregationRequired(consumption1)).thenReturn(false);
        when(consumptionService.isAggregationRequired(consumption2)).thenReturn(false);
        when(consumptionService.groupConsumptionsByEnvCrnAndBucketName(any())).thenReturn(List.of());

        underTest.initJobs();

        verify(jobService, times(2)).schedule(jobIdCaptor.capture());
        Assertions.assertEquals(List.of(consumption1.getId(), consumption2.getId()), jobIdCaptor.getAllValues());
    }

    @Test
    public void testAggregatedSchedulingWithMultiElementGroups() {
        when(storageConsumptionConfig.isStorageConsumptionEnabled()).thenReturn(true);

        Consumption consumption11 = consumption(11L);
        Consumption consumption12 = consumption(12L);
        Consumption consumption21 = consumption(21L);
        Consumption consumption22 = consumption(22L);
        List<Consumption> consumptions = List.of(consumption11, consumption12, consumption21, consumption22);

        when(consumptionService.findAllConsumption()).thenReturn(consumptions);
        consumptions.forEach(consumption -> when(consumptionService.isAggregationRequired(consumption)).thenReturn(true));
        List<List<Consumption>> grouppedConsumptions = List.of(
                List.of(consumption11, consumption12),
                List.of(consumption21, consumption22));
        when(consumptionService.groupConsumptionsByEnvCrnAndBucketName(any())).thenReturn(grouppedConsumptions);

        underTest.initJobs();

        verify(jobService, times(2)).schedule(jobIdCaptor.capture());
        Assertions.assertEquals(List.of(consumption11.getId(), consumption21.getId()), jobIdCaptor.getAllValues());
    }

    @Test
    public void testAggregatedSchedulingWithSingleElementGroups() {
        when(storageConsumptionConfig.isStorageConsumptionEnabled()).thenReturn(true);

        Consumption consumption11 = consumption(11L);
        Consumption consumption21 = consumption(21L);
        List<Consumption> consumptions = List.of(consumption11, consumption21);

        when(consumptionService.findAllConsumption()).thenReturn(consumptions);
        consumptions.forEach(consumption -> when(consumptionService.isAggregationRequired(consumption)).thenReturn(true));
        List<List<Consumption>> grouppedConsumptions = List.of(
                List.of(consumption11),
                List.of(consumption21));
        when(consumptionService.groupConsumptionsByEnvCrnAndBucketName(any())).thenReturn(grouppedConsumptions);

        underTest.initJobs();

        verify(jobService, times(2)).schedule(jobIdCaptor.capture());
        Assertions.assertEquals(List.of(consumption11.getId(), consumption21.getId()), jobIdCaptor.getAllValues());
    }

    @Test
    public void testAggregatedSchedulingWithSingleAndMultiElementGroups() {
        when(storageConsumptionConfig.isStorageConsumptionEnabled()).thenReturn(true);

        Consumption consumption11 = consumption(11L);
        Consumption consumption12 = consumption(12L);
        Consumption consumption21 = consumption(21L);
        List<Consumption> consumptions = List.of(consumption11, consumption12, consumption21);

        when(consumptionService.findAllConsumption()).thenReturn(consumptions);
        consumptions.forEach(consumption -> when(consumptionService.isAggregationRequired(consumption)).thenReturn(true));
        List<List<Consumption>> grouppedConsumptions = List.of(
                List.of(consumption11, consumption12),
                List.of(consumption21));
        when(consumptionService.groupConsumptionsByEnvCrnAndBucketName(any())).thenReturn(grouppedConsumptions);

        underTest.initJobs();

        verify(jobService, times(2)).schedule(jobIdCaptor.capture());
        Assertions.assertEquals(List.of(consumption11.getId(), consumption21.getId()), jobIdCaptor.getAllValues());
    }

    private Consumption consumption(Long id) {
        Consumption consumption = new Consumption();
        consumption.setId(id);
        return consumption;
    }

}
