package com.sequenceiq.consumption.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.consumption.api.v1.consumption.model.common.ConsumptionType;
import com.sequenceiq.consumption.api.v1.consumption.model.common.ResourceType;
import com.sequenceiq.consumption.configuration.repository.ConsumptionRepository;
import com.sequenceiq.consumption.domain.Consumption;
import com.sequenceiq.consumption.dto.ConsumptionCreationDto;
import com.sequenceiq.consumption.dto.converter.ConsumptionDtoConverter;

@ExtendWith(MockitoExtension.class)
public class ConsumptionServiceTest {

    private static final String MONITORED_RESOURCE_CRN = "monitored-crn";

    private static final String NAME = "name_STORAGE";

    private static final String ENVIRONMENT_CRN = "env-crn";

    private static final String ACCOUNT_ID = "acc-id";

    private static final String CRN = "consumption-crn";

    private static final ResourceType RESOURCE_TYPE = ResourceType.DATALAKE;

    private static final ConsumptionType CONSUMPTION_TYPE = ConsumptionType.STORAGE;

    private static final String STORAGE_LOCATION = "location";

    @Mock
    private ConsumptionRepository consumptionRepository;

    @Mock
    private ConsumptionDtoConverter consumptionDtoConverter;

    @InjectMocks
    private ConsumptionService underTest;

    private Consumption consumption;

    @BeforeEach
    void setUp() {
        consumption = new Consumption();
    }

    @Test
    void testFindStorageConsumptionByMonitoredResourceCrnAndLocationNotFound() {
        when(consumptionRepository
                .findStorageConsumptionByMonitoredResourceCrnAndLocation(eq(MONITORED_RESOURCE_CRN), eq(STORAGE_LOCATION)))
                .thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> underTest
                .findStorageConsumptionByMonitoredResourceCrnAndLocation(MONITORED_RESOURCE_CRN, STORAGE_LOCATION));
    }

    @Test
    void testFindStorageConsumptionByMonitoredResourceCrnAndLocationFound() {
        when(consumptionRepository
                .findStorageConsumptionByMonitoredResourceCrnAndLocation(eq(MONITORED_RESOURCE_CRN), eq(STORAGE_LOCATION)))
                .thenReturn(Optional.of(consumption));
        assertEquals(consumption, underTest
                .findStorageConsumptionByMonitoredResourceCrnAndLocation(MONITORED_RESOURCE_CRN, STORAGE_LOCATION));
    }

    @Test
    void testCreate() {
        ConsumptionCreationDto consumptionCreationDto = ConsumptionCreationDto.builder()
                .withName(NAME)
                .withConsumptionType(CONSUMPTION_TYPE)
                .withEnvironmentCrn(ENVIRONMENT_CRN)
                .withAccountId(ACCOUNT_ID)
                .withResourceCrn(CRN)
                .withMonitoredResourceType(RESOURCE_TYPE)
                .withMonitoredResourceCrn(MONITORED_RESOURCE_CRN)
                .withStorageLocation(STORAGE_LOCATION)
                .build();
        Consumption consumption = new Consumption();
        consumption.setName(NAME);
        consumption.setId(1L);
        consumption.setAccountId(ACCOUNT_ID);
        when(consumptionDtoConverter.creationDtoToConsumption(eq(consumptionCreationDto))).thenReturn(consumption);
        when(consumptionRepository.save(consumption)).thenReturn(consumption);
        when(consumptionRepository.doesStorageConsumptionExistWithLocationForMonitoredCrn(MONITORED_RESOURCE_CRN, STORAGE_LOCATION)).thenReturn(false);

        Consumption result = underTest.create(consumptionCreationDto);

        assertEquals(consumption, result);
        verify(consumptionRepository, Mockito.times(1))
                .doesStorageConsumptionExistWithLocationForMonitoredCrn(MONITORED_RESOURCE_CRN, STORAGE_LOCATION);
        verify(consumptionRepository).save(consumption);
        verify(consumptionDtoConverter).creationDtoToConsumption(eq(consumptionCreationDto));
    }

    @Test
    void testCreateWhenConsumptionAlreadyExists() {
        ConsumptionCreationDto consumptionCreationDto = ConsumptionCreationDto.builder()
                .withName(NAME)
                .withConsumptionType(CONSUMPTION_TYPE)
                .withEnvironmentCrn(ENVIRONMENT_CRN)
                .withAccountId(ACCOUNT_ID)
                .withResourceCrn(CRN)
                .withMonitoredResourceType(RESOURCE_TYPE)
                .withMonitoredResourceCrn(MONITORED_RESOURCE_CRN)
                .withStorageLocation(STORAGE_LOCATION)
                .build();
        Consumption consumption = new Consumption();
        consumption.setName(NAME);
        consumption.setId(1L);
        consumption.setAccountId(ACCOUNT_ID);
        when(consumptionRepository.doesStorageConsumptionExistWithLocationForMonitoredCrn(MONITORED_RESOURCE_CRN, STORAGE_LOCATION)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> underTest.create(consumptionCreationDto));

        verify(consumptionRepository, Mockito.times(1))
                .doesStorageConsumptionExistWithLocationForMonitoredCrn(MONITORED_RESOURCE_CRN, STORAGE_LOCATION);
        verify(consumptionRepository, Mockito.times(0)).save(consumption);
        verify(consumptionDtoConverter, Mockito.times(0)).creationDtoToConsumption(eq(consumptionCreationDto));
    }
}
