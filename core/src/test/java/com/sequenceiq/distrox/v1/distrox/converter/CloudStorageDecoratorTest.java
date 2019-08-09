package com.sequenceiq.distrox.v1.distrox.converter;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.common.api.telemetry.response.LoggingResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.common.model.CloudStorageCdpService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

class CloudStorageDecoratorTest {

    private CloudStorageDecorator underTest;

    @BeforeEach
    void setUp() {
        underTest = new CloudStorageDecorator();
    }

    @Test
    void testConvertWhenEnvironmentAndStorageRequestAreNull() {
        CloudStorageRequest result = underTest.decorate(null, null);

        assertNull(result);
    }

    @Test
    void testConvertWhenEnvironmentIsNullAndCloudStorageRequestIsNot() {
        CloudStorageRequest request = new CloudStorageRequest();

        CloudStorageRequest result = underTest.decorate(request, null);

        assertNotNull(result);
    }

    @Test
    void testConvertWhenCloudStorageLocationsIsNullAndEnvironmentHasTelemetryWithLogging() {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        TelemetryResponse telemetry = new TelemetryResponse();
        telemetry.setLogging(new LoggingResponse());
        environment.setTelemetry(telemetry);

        CloudStorageRequest result = underTest.decorate(null, environment);

        assertNotNull(result);
        assertTrue(result.getIdentities().stream().anyMatch(id -> CloudIdentityType.LOG.equals(id.getType())));
    }

    @Test
    void testConvertWhenCloudStorageLocationsIsEmptyAndEnvironmentHasTelemetryWithLogging() {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        TelemetryResponse telemetry = new TelemetryResponse();
        telemetry.setLogging(new LoggingResponse());
        environment.setTelemetry(telemetry);

        CloudStorageRequest result = underTest.decorate(null, environment);

        assertNotNull(result);
        assertTrue(result.getIdentities().stream().anyMatch(id -> CloudIdentityType.LOG.equals(id.getType())));
        assertTrue(result.getLocations().isEmpty());
    }

    @Test
    void testConvertWhenEnvironmentHaveTelemetryAndStorageLocationsIsNotNullOrEmpty() {
        TelemetryResponse telemetry = new TelemetryResponse();
        telemetry.setLogging(new LoggingResponse());
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setTelemetry(telemetry);

        CloudStorageCdpService eStorageLocationType = CloudStorageCdpService.RANGER_AUDIT;
        String eStorageLocationValue = "MYBUCKET/CONTAINER";
        StorageLocationBase storageLocationBase = new StorageLocationBase();
        storageLocationBase.setType(eStorageLocationType);
        storageLocationBase.setValue(eStorageLocationValue);
        List<StorageLocationBase> storageLocations = List.of(storageLocationBase);
        CloudStorageRequest request = new CloudStorageRequest();
        request.setLocations(storageLocations);

        CloudStorageRequest result = underTest.decorate(request, environment);

        assertNotNull(result);
        assertTrue(result.getIdentities().stream().anyMatch(id -> CloudIdentityType.LOG.equals(id.getType())));
        assertTrue(result.getLocations().stream().anyMatch(loc -> eStorageLocationType.equals(loc.getType()) && eStorageLocationValue.equals(loc.getValue())));
    }

    @Test
    void testConvertWhenEnvironmentDoesNotHaveTelemetryButCloudStorageLocationsIsNotEmpty() {
        CloudStorageCdpService eStorageLocationType = CloudStorageCdpService.RANGER_AUDIT;
        String eStorageLocationValue = "MYBUCKET/CONTAINER";
        StorageLocationBase storageLocationBase = new StorageLocationBase();
        storageLocationBase.setType(eStorageLocationType);
        storageLocationBase.setValue(eStorageLocationValue);
        List<StorageLocationBase> storageLocations = List.of(storageLocationBase);
        CloudStorageRequest request = new CloudStorageRequest();
        request.setLocations(storageLocations);

        CloudStorageRequest result = underTest.decorate(request, new DetailedEnvironmentResponse());

        assertNotNull(result);
        assertTrue(result.getLocations().stream().anyMatch(loc -> eStorageLocationType.equals(loc.getType()) && eStorageLocationValue.equals(loc.getValue())));
    }
}