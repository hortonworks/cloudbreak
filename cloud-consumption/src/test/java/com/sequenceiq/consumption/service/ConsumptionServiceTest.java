package com.sequenceiq.consumption.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import javax.validation.ValidationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.consumption.AwsS3ConsumptionCalculator;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudConsumption;
import com.sequenceiq.cloudbreak.common.mappable.StorageType;
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

    private static final String ENVIRONMENT_CRN_OTHER = "env-crn-other";

    private static final String ACCOUNT_ID = "acc-id";

    private static final String CRN = "consumption-crn";

    private static final ResourceType RESOURCE_TYPE = ResourceType.DATALAKE;

    private static final ConsumptionType CONSUMPTION_TYPE = ConsumptionType.STORAGE;

    private static final String STORAGE_LOCATION = "location";

    private static final String STORAGE_LOCATION_OTHER = "location-other";

    private static final String BUCKET = "bucket";

    private static final String BUCKET_OTHER = "bucket-other";

    @Mock
    private ConsumptionRepository consumptionRepository;

    @Mock
    private ConsumptionDtoConverter consumptionDtoConverter;

    @Mock
    private AwsS3ConsumptionCalculator awsS3ConsumptionCalculator;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @InjectMocks
    private ConsumptionService underTest;

    @Test
    void testFindStorageConsumptionByMonitoredResourceCrnAndLocationNotFound() {
        when(consumptionRepository.findStorageConsumptionByMonitoredResourceCrnAndObjectId(MONITORED_RESOURCE_CRN, STORAGE_LOCATION))
                .thenReturn(Optional.empty());

        Optional<Consumption> result =
                underTest.findStorageConsumptionByMonitoredResourceCrnAndLocation(MONITORED_RESOURCE_CRN, STORAGE_LOCATION);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void testFindStorageConsumptionByMonitoredResourceCrnAndLocationFound() {
        Consumption consumption = mock(Consumption.class);
        when(consumptionRepository.findStorageConsumptionByMonitoredResourceCrnAndObjectId(MONITORED_RESOURCE_CRN, STORAGE_LOCATION))
                .thenReturn(Optional.of(consumption));

        Optional<Consumption> result = underTest
                .findStorageConsumptionByMonitoredResourceCrnAndLocation(MONITORED_RESOURCE_CRN, STORAGE_LOCATION);

        assertThat(result).isNotNull();
        assertThat(result).isPresent();
        assertThat(result).hasValue(consumption);
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
        consumption.setConsumptionType(ConsumptionType.STORAGE);
        consumption.setAccountId(ACCOUNT_ID);
        when(consumptionDtoConverter.creationDtoToConsumption(eq(consumptionCreationDto))).thenReturn(consumption);
        when(consumptionRepository.save(consumption)).thenReturn(consumption);
        when(consumptionRepository.findStorageConsumptionByMonitoredResourceCrnAndObjectId(MONITORED_RESOURCE_CRN, STORAGE_LOCATION))
                .thenReturn(Optional.of(consumption));
        when(consumptionRepository.doesStorageConsumptionExistWithLocationForMonitoredCrn(
                MONITORED_RESOURCE_CRN,
                STORAGE_LOCATION,
                ConsumptionType.STORAGE)).thenReturn(false);

        Optional<Consumption> result = underTest.create(consumptionCreationDto);

        assertThat(result).isNotNull();
        assertThat(result).isPresent();
        assertEquals(consumption, result.get());
        verify(consumptionRepository, Mockito.times(1))
                .doesStorageConsumptionExistWithLocationForMonitoredCrn(MONITORED_RESOURCE_CRN, STORAGE_LOCATION, ConsumptionType.STORAGE);
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
        consumption.setConsumptionType(ConsumptionType.STORAGE);
        consumption.setMonitoredResourceType(ResourceType.ENVIRONMENT);
        when(consumptionRepository.doesStorageConsumptionExistWithLocationForMonitoredCrn(
                MONITORED_RESOURCE_CRN,
                STORAGE_LOCATION,
                ConsumptionType.STORAGE)).thenReturn(true);
        when(consumptionRepository.findStorageConsumptionByMonitoredResourceCrnAndObjectId(MONITORED_RESOURCE_CRN, STORAGE_LOCATION))
                .thenReturn(Optional.of(consumption));

        Optional<Consumption> result = underTest.create(consumptionCreationDto);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(consumptionRepository, Mockito.times(1))
                .doesStorageConsumptionExistWithLocationForMonitoredCrn(
                        MONITORED_RESOURCE_CRN,
                        STORAGE_LOCATION,
                        ConsumptionType.STORAGE);
        verify(consumptionRepository, Mockito.times(0)).save(consumption);
        verify(consumptionDtoConverter, Mockito.times(0)).creationDtoToConsumption(eq(consumptionCreationDto));
    }

    @Test
    public void testFindAllConsumption() {
        when(consumptionRepository.findAllConsumption()).thenReturn(List.of());

        underTest.findAllConsumption();

        verify(consumptionRepository, times(1)).findAllConsumption();
    }

    @Test
    public void testNoAggregationRequiredIfNotStorageType() {
        Consumption consumption = new Consumption();
        consumption.setConsumptionType(ConsumptionType.UNKNOWN);

        boolean result = underTest.isAggregationRequired(consumption);

        assertFalse(result);
    }

    @Test
    public void testNoAggregationRequiredIfLocationValidationFails() {
        Consumption consumption = consumption("not_s3_location");
        doThrow(new ValidationException("error")).when(awsS3ConsumptionCalculator)
                .validate(any(CloudConsumption.class));
        CloudConnector connector = mock(CloudConnector.class);
        when(cloudPlatformConnectors.getDefault(any())).thenReturn(connector);
        when(connector.consumptionCalculator(any())).thenReturn(Optional.of(awsS3ConsumptionCalculator));

        boolean result = underTest.isAggregationRequired(consumption);

        assertFalse(result);
    }

    @Test
    public void testAggregationRequired() {
        Consumption consumption = consumption("s3_location");
        CloudConnector connector = mock(CloudConnector.class);
        when(cloudPlatformConnectors.getDefault(any())).thenReturn(connector);
        when(connector.consumptionCalculator(any())).thenReturn(Optional.of(awsS3ConsumptionCalculator));

        boolean result = underTest.isAggregationRequired(consumption);

        assertTrue(result);
    }

    @Test
    public void testFindAllStorageConsumptionForEnvCrnAndBucketNameNoResult() {
        when(consumptionRepository.findAllStorageConsumptionByEnvironmentCrn(ENVIRONMENT_CRN, ConsumptionType.STORAGE))
                .thenReturn(List.of());
        when(awsS3ConsumptionCalculator.getObjectId(STORAGE_LOCATION)).thenAnswer(invocation -> BUCKET);
        CloudConnector connector = mock(CloudConnector.class);
        when(cloudPlatformConnectors.getDefault(any())).thenReturn(connector);
        when(connector.consumptionCalculator(any())).thenReturn(Optional.of(awsS3ConsumptionCalculator));

        List<Consumption> result = underTest.findAllStorageConsumptionForEnvCrnAndBucketName(
                ENVIRONMENT_CRN,
                STORAGE_LOCATION,
                StorageType.S3,
                ConsumptionType.STORAGE);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testFindAllStorageConsumptionForEnvCrnAndBucketNameValidationError() {
        when(consumptionRepository.findAllStorageConsumptionByEnvironmentCrn(ENVIRONMENT_CRN, ConsumptionType.STORAGE))
                .thenReturn(List.of(consumption(ENVIRONMENT_CRN, STORAGE_LOCATION)));
        when(awsS3ConsumptionCalculator.getObjectId(STORAGE_LOCATION)).thenThrow(new ValidationException("error"));
        CloudConnector connector = mock(CloudConnector.class);
        when(cloudPlatformConnectors.getDefault(any())).thenReturn(connector);
        when(connector.consumptionCalculator(any())).thenReturn(Optional.of(awsS3ConsumptionCalculator));

        List<Consumption> result = underTest.findAllStorageConsumptionForEnvCrnAndBucketName(
                ENVIRONMENT_CRN,
                STORAGE_LOCATION,
                StorageType.S3,
                ConsumptionType.STORAGE);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testFindAllStorageConsumptionForEnvCrnAndBucketName() {
        Consumption consumption1 = consumption(ENVIRONMENT_CRN, STORAGE_LOCATION);
        Consumption consumption2 = consumption(ENVIRONMENT_CRN, STORAGE_LOCATION);
        Consumption consumption3 = consumption(ENVIRONMENT_CRN, STORAGE_LOCATION_OTHER);

        when(consumptionRepository.findAllStorageConsumptionByEnvironmentCrn(ENVIRONMENT_CRN, ConsumptionType.STORAGE))
                .thenReturn(List.of(consumption1, consumption2, consumption3));
        when(awsS3ConsumptionCalculator.getObjectId(STORAGE_LOCATION)).thenAnswer(invocation -> BUCKET);
        when(awsS3ConsumptionCalculator.getObjectId(STORAGE_LOCATION_OTHER)).thenAnswer(invocation -> BUCKET_OTHER);
        CloudConnector connector = mock(CloudConnector.class);
        when(cloudPlatformConnectors.getDefault(any())).thenReturn(connector);
        when(connector.consumptionCalculator(any())).thenReturn(Optional.of(awsS3ConsumptionCalculator));

        List<Consumption> result = underTest.findAllStorageConsumptionForEnvCrnAndBucketName(
                ENVIRONMENT_CRN,
                STORAGE_LOCATION,
                StorageType.S3,
                ConsumptionType.STORAGE);

        assertEquals(List.of(consumption1, consumption2), result);
    }

    @Test
    public void testGroupConsumptionsByEnvCrnAndBucketNameNoConsumptions() {
        List<List<Consumption>> result = underTest.groupConsumptionsByEnvCrnAndBucketName(List.of());

        assertTrue(result.isEmpty());
    }

    @Test
    public void testGroupConsumptionsByEnvCrnAndBucketName() {
        Consumption consumption11 = consumption(ENVIRONMENT_CRN, STORAGE_LOCATION);
        Consumption consumption12 = consumption(ENVIRONMENT_CRN, STORAGE_LOCATION);
        Consumption consumption21 = consumption(ENVIRONMENT_CRN, STORAGE_LOCATION_OTHER);
        Consumption consumption31 = consumption(ENVIRONMENT_CRN_OTHER, STORAGE_LOCATION);
        Consumption consumption41 = consumption(ENVIRONMENT_CRN_OTHER, STORAGE_LOCATION_OTHER);
        Consumption consumption42 = consumption(ENVIRONMENT_CRN_OTHER, STORAGE_LOCATION_OTHER);

        when(awsS3ConsumptionCalculator.getObjectId(STORAGE_LOCATION)).thenAnswer(invocation -> BUCKET);
        when(awsS3ConsumptionCalculator.getObjectId(STORAGE_LOCATION_OTHER)).thenAnswer(invocation -> BUCKET_OTHER);
        CloudConnector connector = mock(CloudConnector.class);
        when(cloudPlatformConnectors.getDefault(any())).thenReturn(connector);
        when(connector.consumptionCalculator(any())).thenReturn(Optional.of(awsS3ConsumptionCalculator));

        List<List<Consumption>> result = underTest.groupConsumptionsByEnvCrnAndBucketName(
                List.of(consumption11, consumption12, consumption21, consumption31, consumption41, consumption42));

        List<List<Consumption>> expected = List.of(
                List.of(consumption11, consumption12),
                List.of(consumption21),
                List.of(consumption31),
                List.of(consumption41, consumption42));
        assertEquals(expected, result);
    }

    @Test
    public void testGroupConsumptionsByEnvCrnAndBucketNameInvalidLocationFilteredOut() {
        Consumption consumption11 = consumption(ENVIRONMENT_CRN, STORAGE_LOCATION);
        Consumption consumption12 = consumption(ENVIRONMENT_CRN, STORAGE_LOCATION);
        Consumption consumption21 = consumption(ENVIRONMENT_CRN, STORAGE_LOCATION_OTHER);
        Consumption consumptionInvalid1 = consumption(ENVIRONMENT_CRN, "invalid");
        Consumption consumption31 = consumption(ENVIRONMENT_CRN_OTHER, STORAGE_LOCATION);
        Consumption consumption41 = consumption(ENVIRONMENT_CRN_OTHER, STORAGE_LOCATION_OTHER);
        Consumption consumption42 = consumption(ENVIRONMENT_CRN_OTHER, STORAGE_LOCATION_OTHER);
        Consumption consumptionInvalid2 = consumption(ENVIRONMENT_CRN_OTHER, "invalid");

        when(awsS3ConsumptionCalculator.getObjectId(STORAGE_LOCATION)).thenAnswer(invocation -> BUCKET);
        when(awsS3ConsumptionCalculator.getObjectId(STORAGE_LOCATION_OTHER)).thenAnswer(invocation -> BUCKET_OTHER);
        when(awsS3ConsumptionCalculator.getObjectId("invalid")).thenAnswer(invocation -> {
            throw new ValidationException("error");
        });
        CloudConnector connector = mock(CloudConnector.class);
        when(cloudPlatformConnectors.getDefault(any())).thenReturn(connector);
        when(connector.consumptionCalculator(any())).thenReturn(Optional.of(awsS3ConsumptionCalculator));

        List<List<Consumption>> result = underTest.groupConsumptionsByEnvCrnAndBucketName(
                List.of(consumption11, consumption12, consumption21, consumptionInvalid1, consumption31, consumption41, consumption42, consumptionInvalid2));

        List<List<Consumption>> expected = List.of(
                List.of(consumption11, consumption12),
                List.of(consumption21),
                List.of(consumption31),
                List.of(consumption41, consumption42));
        assertEquals(expected, result);
    }

    private Consumption consumption(String location) {
        return consumption(null, location);
    }

    private Consumption consumption(String envCrn, String location) {
        Consumption consumption = new Consumption();
        consumption.setStorageLocation(location);
        consumption.setEnvironmentCrn(envCrn);
        consumption.setConsumptionType(ConsumptionType.STORAGE);
        return consumption;
    }
}
