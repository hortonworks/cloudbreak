package com.sequenceiq.consumption.endpoint;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.consumption.api.v1.consumption.model.request.StorageConsumptionRequest;
import com.sequenceiq.consumption.api.v1.consumption.model.response.ConsumptionExistenceResponse;
import com.sequenceiq.consumption.domain.Consumption;
import com.sequenceiq.consumption.dto.ConsumptionCreationDto;
import com.sequenceiq.consumption.endpoint.converter.ConsumptionApiConverter;
import com.sequenceiq.consumption.job.storage.StorageConsumptionJobService;
import com.sequenceiq.consumption.service.ConsumptionService;

@ExtendWith(MockitoExtension.class)
public class ConsumptionInternalV1ControllerTest {

    private static final String MONITORED_CRN = "crn:cdp:datalake:us-west-1:hortonworks:datalake:guid";

    private static final String LOCATION = "s3a://location";

    private static final String ACCOUNT_ID = "123";

    private static final Long CONSUMPTION_ID = 123L;

    @Mock
    private ConsumptionService consumptionService;

    @Mock
    private ConsumptionApiConverter consumptionApiConverter;

    @Mock
    private StorageConsumptionJobService jobService;

    @InjectMocks
    private ConsumptionInternalV1Controller underTest;

    @Test
    public void testStorageConsumptionCollectionExists() {
        when(consumptionService.isConsumptionPresentForLocationAndMonitoredCrn(MONITORED_CRN, LOCATION)).thenReturn(true);

        ConsumptionExistenceResponse response = underTest.doesStorageConsumptionCollectionExist(ACCOUNT_ID, MONITORED_CRN, LOCATION);

        verify(consumptionService).isConsumptionPresentForLocationAndMonitoredCrn(MONITORED_CRN, LOCATION);
        assertTrue(response.isExists());
    }

    @Test
    public void testStorageConsumptionCollectionDoesntExist() {
        when(consumptionService.isConsumptionPresentForLocationAndMonitoredCrn(MONITORED_CRN, LOCATION)).thenReturn(false);

        ConsumptionExistenceResponse response = underTest.doesStorageConsumptionCollectionExist(ACCOUNT_ID, MONITORED_CRN, LOCATION);

        verify(consumptionService).isConsumptionPresentForLocationAndMonitoredCrn(MONITORED_CRN, LOCATION);
        assertFalse(response.isExists());
    }

    @Test
    void scheduleStorageConsumptionCollectionTestWhenSuccessAndConsumptionNotCreated() {
        StorageConsumptionRequest request = new StorageConsumptionRequest();
        ConsumptionCreationDto consumptionCreationDto = consumptionCreationDto(LOCATION);

        when(consumptionApiConverter.initCreationDtoForStorage(request)).thenReturn(consumptionCreationDto);
        when(consumptionService.create(consumptionCreationDto)).thenReturn(Optional.empty());

        underTest.scheduleStorageConsumptionCollection(ACCOUNT_ID, request);

        verify(jobService, never()).schedule(anyLong());
    }

    @Test
    void scheduleStorageConsumptionCollectionTestWhenSuccessAndConsumptionCreated() {
        StorageConsumptionRequest request = new StorageConsumptionRequest();
        ConsumptionCreationDto consumptionCreationDto = consumptionCreationDto(LOCATION);

        when(consumptionApiConverter.initCreationDtoForStorage(request)).thenReturn(consumptionCreationDto);
        when(consumptionService.create(consumptionCreationDto)).thenReturn(Optional.of(consumption()));

        underTest.scheduleStorageConsumptionCollection(ACCOUNT_ID, request);

        verify(jobService).schedule(CONSUMPTION_ID);
    }

    @Test
    void unscheduleStorageConsumptionCollectionTestWhenConsumptionAbsent() {
        when(consumptionService.findStorageConsumptionByMonitoredResourceCrnAndLocation(MONITORED_CRN, LOCATION)).thenReturn(Optional.empty());

        underTest.unscheduleStorageConsumptionCollection(ACCOUNT_ID, MONITORED_CRN, LOCATION);

        verify(jobService, never()).unschedule(anyString());
        verify(consumptionService, never()).delete(any(Consumption.class));
    }

    @Test
    void unscheduleStorageConsumptionCollectionTestWhenConsumptionPresent() {
        Consumption consumption = consumption();
        when(consumptionService.findStorageConsumptionByMonitoredResourceCrnAndLocation(MONITORED_CRN, LOCATION)).thenReturn(Optional.of(consumption));

        underTest.unscheduleStorageConsumptionCollection(ACCOUNT_ID, MONITORED_CRN, LOCATION);

        verify(jobService).unschedule("123");
        verify(consumptionService).delete(consumption);
    }

    private ConsumptionCreationDto consumptionCreationDto(String location) {
        return ConsumptionCreationDto.builder()
                .withStorageLocation(location)
                .withMonitoredResourceCrn(MONITORED_CRN)
                .build();
    }

    private Consumption consumption() {
        Consumption consumption = new Consumption();
        consumption.setId(CONSUMPTION_ID);
        return consumption;
    }

}
