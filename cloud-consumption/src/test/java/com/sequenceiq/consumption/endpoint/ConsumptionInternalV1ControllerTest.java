package com.sequenceiq.consumption.endpoint;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.consumption.api.v1.consumption.model.common.ConsumptionType;
import com.sequenceiq.consumption.api.v1.consumption.model.request.CloudResourceConsumptionRequest;
import com.sequenceiq.consumption.api.v1.consumption.model.request.StorageConsumptionRequest;
import com.sequenceiq.consumption.api.v1.consumption.model.response.ConsumptionExistenceResponse;
import com.sequenceiq.consumption.domain.Consumption;
import com.sequenceiq.consumption.dto.ConsumptionCreationDto;
import com.sequenceiq.consumption.endpoint.converter.ConsumptionApiConverter;
import com.sequenceiq.consumption.job.storage.StorageConsumptionJobService;
import com.sequenceiq.consumption.service.ConsumptionService;
import com.sequenceiq.consumption.service.ConsumptionStructuredEventCleanupService;

@ExtendWith(MockitoExtension.class)
public class ConsumptionInternalV1ControllerTest {

    private static final String MONITORED_CRN = "crn:cdp:datalake:us-west-1:hortonworks:datalake:guid";

    private static final String LOCATION = "s3a://location";

    private static final String CLOUD_RESOURCE_ID = "cloudResourceId";

    private static final String ACCOUNT_ID = "123";

    private static final Long CONSUMPTION_ID = 123L;

    private static final String RESOURCE_CRN = "crn:cdp:consumption:us-west-1:hortonworks:consumption:guid";

    private static final String INITIATOR_USER_CRN = "initiatorUserCrn";

    @Mock
    private ConsumptionService consumptionService;

    @Mock
    private ConsumptionApiConverter consumptionApiConverter;

    @Mock
    private StorageConsumptionJobService jobService;

    @Mock
    private ConsumptionStructuredEventCleanupService mockStructuredEventCleanupService;

    @InjectMocks
    private ConsumptionInternalV1Controller underTest;

    @Test
    public void testStorageConsumptionCollectionExists() {
        when(consumptionService.isConsumptionPresentForLocationAndMonitoredCrn(MONITORED_CRN, LOCATION)).thenReturn(true);

        ConsumptionExistenceResponse response = underTest.doesStorageConsumptionCollectionExist(ACCOUNT_ID, MONITORED_CRN, LOCATION, INITIATOR_USER_CRN);

        verify(consumptionService).isConsumptionPresentForLocationAndMonitoredCrn(MONITORED_CRN, LOCATION);
        assertTrue(response.isExists());
    }

    @Test
    public void testStorageConsumptionCollectionDoesntExist() {
        when(consumptionService.isConsumptionPresentForLocationAndMonitoredCrn(MONITORED_CRN, LOCATION)).thenReturn(false);

        ConsumptionExistenceResponse response = underTest.doesStorageConsumptionCollectionExist(ACCOUNT_ID, MONITORED_CRN, LOCATION, INITIATOR_USER_CRN);

        verify(consumptionService).isConsumptionPresentForLocationAndMonitoredCrn(MONITORED_CRN, LOCATION);
        assertFalse(response.isExists());
    }

    @Test
    void scheduleStorageConsumptionCollectionTestWhenSuccessAndConsumptionNotCreated() {
        StorageConsumptionRequest request = new StorageConsumptionRequest();
        ConsumptionCreationDto consumptionCreationDto = consumptionCreationDto(LOCATION);

        when(consumptionApiConverter.initCreationDtoForStorage(request)).thenReturn(consumptionCreationDto);
        when(consumptionService.create(consumptionCreationDto)).thenReturn(Optional.empty());

        underTest.scheduleStorageConsumptionCollection(ACCOUNT_ID, request, INITIATOR_USER_CRN);

        verify(jobService, never()).schedule(any(Consumption.class));
    }

    @Test
    void scheduleStorageConsumptionCollectionTestWhenSuccessAndConsumptionCreated() {
        StorageConsumptionRequest request = new StorageConsumptionRequest();
        ConsumptionCreationDto consumptionCreationDto = consumptionCreationDto(LOCATION);

        when(consumptionApiConverter.initCreationDtoForStorage(request)).thenReturn(consumptionCreationDto);
        Consumption consumption = consumption();
        when(consumptionService.create(consumptionCreationDto)).thenReturn(Optional.of(consumption));

        underTest.scheduleStorageConsumptionCollection(ACCOUNT_ID, request, INITIATOR_USER_CRN);

        verify(jobService).schedule(consumption);
    }

    @Test
    void unscheduleStorageConsumptionCollectionTestWhenConsumptionAbsent() {
        when(consumptionService.findStorageConsumptionByMonitoredResourceCrnAndLocation(MONITORED_CRN, LOCATION)).thenReturn(Optional.empty());

        underTest.unscheduleStorageConsumptionCollection(ACCOUNT_ID, MONITORED_CRN, LOCATION, INITIATOR_USER_CRN);

        verify(jobService, never()).unschedule(any(Consumption.class));
        verify(consumptionService, never()).delete(any(Consumption.class));
    }

    @Test
    void unscheduleStorageConsumptionCollectionTestWhenConsumptionPresent() {
        Consumption consumption = consumption();
        when(consumptionService.findStorageConsumptionByMonitoredResourceCrnAndLocation(MONITORED_CRN, LOCATION)).thenReturn(Optional.of(consumption));

        underTest.unscheduleStorageConsumptionCollection(ACCOUNT_ID, MONITORED_CRN, LOCATION, INITIATOR_USER_CRN);

        verify(jobService).unschedule(consumption);
        verify(consumptionService).delete(consumption);
        verify(mockStructuredEventCleanupService).cleanUpStructuredEvents(consumption.getResourceCrn());
        verifyNoMoreInteractions(mockStructuredEventCleanupService);
    }

    @Test
    void doesCloudResourceConsumptionCollectionExistTestWhenExists() {
        when(consumptionService.isConsumptionPresentForLocationAndMonitoredCrn(MONITORED_CRN, CLOUD_RESOURCE_ID)).thenReturn(true);

        ConsumptionExistenceResponse response = underTest.doesCloudResourceConsumptionCollectionExist(ACCOUNT_ID, MONITORED_CRN, CLOUD_RESOURCE_ID,
                INITIATOR_USER_CRN);

        verify(consumptionService).isConsumptionPresentForLocationAndMonitoredCrn(MONITORED_CRN, CLOUD_RESOURCE_ID);
        assertTrue(response.isExists());
    }

    @Test
    void doesCloudResourceConsumptionCollectionExistTestWhenDoesNotExist() {
        when(consumptionService.isConsumptionPresentForLocationAndMonitoredCrn(MONITORED_CRN, CLOUD_RESOURCE_ID)).thenReturn(false);

        ConsumptionExistenceResponse response = underTest.doesCloudResourceConsumptionCollectionExist(ACCOUNT_ID, MONITORED_CRN, CLOUD_RESOURCE_ID,
                INITIATOR_USER_CRN);

        verify(consumptionService).isConsumptionPresentForLocationAndMonitoredCrn(MONITORED_CRN, CLOUD_RESOURCE_ID);
        assertFalse(response.isExists());
    }

    @Test
    void scheduleCloudResourceConsumptionCollectionTestWhenSuccessAndConsumptionNotCreated() {
        CloudResourceConsumptionRequest request = cloudResourceConsumptionRequest();
        ConsumptionCreationDto consumptionCreationDto = consumptionCreationDto(CLOUD_RESOURCE_ID);

        when(consumptionApiConverter.initCreationDtoForCloudResource(request, ConsumptionType.EBS)).thenReturn(consumptionCreationDto);
        when(consumptionService.create(consumptionCreationDto)).thenReturn(Optional.empty());

        underTest.scheduleCloudResourceConsumptionCollection(ACCOUNT_ID, request, INITIATOR_USER_CRN);

        verify(jobService, never()).schedule(any(Consumption.class));
    }

    @Test
    void scheduleCloudResourceConsumptionCollectionTestWhenSuccessAndConsumptionCreated() {
        CloudResourceConsumptionRequest request = cloudResourceConsumptionRequest();
        ConsumptionCreationDto consumptionCreationDto = consumptionCreationDto(CLOUD_RESOURCE_ID);

        when(consumptionApiConverter.initCreationDtoForCloudResource(request, ConsumptionType.EBS)).thenReturn(consumptionCreationDto);
        Consumption consumption = consumption();
        when(consumptionService.create(consumptionCreationDto)).thenReturn(Optional.of(consumption));

        underTest.scheduleCloudResourceConsumptionCollection(ACCOUNT_ID, request, INITIATOR_USER_CRN);

        verify(jobService).schedule(consumption);
    }

    @Test
    void unscheduleCloudResourceConsumptionCollectionWhenConsumptionAbsent() {
        when(consumptionService.findStorageConsumptionByMonitoredResourceCrnAndLocation(MONITORED_CRN, CLOUD_RESOURCE_ID)).thenReturn(Optional.empty());

        underTest.unscheduleCloudResourceConsumptionCollection(ACCOUNT_ID, MONITORED_CRN, CLOUD_RESOURCE_ID, INITIATOR_USER_CRN);

        verify(jobService, never()).unschedule(any(Consumption.class));
        verify(consumptionService, never()).delete(any(Consumption.class));
    }

    @Test
    void unscheduleCloudResourceConsumptionCollectionWhenConsumptionPresent() {
        Consumption consumption = consumption();
        when(consumptionService.findStorageConsumptionByMonitoredResourceCrnAndLocation(MONITORED_CRN, CLOUD_RESOURCE_ID)).thenReturn(Optional.of(consumption));

        underTest.unscheduleCloudResourceConsumptionCollection(ACCOUNT_ID, MONITORED_CRN, CLOUD_RESOURCE_ID, INITIATOR_USER_CRN);

        verify(jobService).unschedule(consumption);
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
        consumption.setResourceCrn(RESOURCE_CRN);
        return consumption;
    }

    private CloudResourceConsumptionRequest cloudResourceConsumptionRequest() {
        CloudResourceConsumptionRequest request = new CloudResourceConsumptionRequest();
        request.setConsumptionType(ConsumptionType.EBS);
        return request;
    }

}
