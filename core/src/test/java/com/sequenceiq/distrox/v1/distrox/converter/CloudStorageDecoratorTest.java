package com.sequenceiq.distrox.v1.distrox.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.datalake.SdxClientService;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.common.api.cloudstorage.query.ConfigQueryEntry;
import com.sequenceiq.common.api.telemetry.response.LoggingResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.common.model.CloudStorageCdpService;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@ExtendWith(MockitoExtension.class)
class CloudStorageDecoratorTest {

    private static final String BLUEPRINT_NAME = "blueprintName";

    private static final String CLUSTER_NAME = "clusterName";

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private SdxClientService sdxClientService;

    @InjectMocks
    private CloudStorageDecorator underTest;

    @Test
    void testConvertWhenEnvironmentAndStorageRequestAreNull() {
        CloudStorageRequest result = underTest.decorate(BLUEPRINT_NAME, CLUSTER_NAME, null, null);

        assertNull(result);
    }

    @Test
    void testConvertWhenEnvironmentIsNullAndCloudStorageRequestIsNot() {
        CloudStorageRequest request = new CloudStorageRequest();

        CloudStorageRequest result = underTest.decorate(BLUEPRINT_NAME, CLUSTER_NAME, request, null);

        assertNotNull(result);
    }

    @Test
    void testConvertWhenCloudStorageLocationsIsNullAndEnvironmentHasTelemetryWithLogging() {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        TelemetryResponse telemetry = new TelemetryResponse();
        telemetry.setLogging(new LoggingResponse());
        environment.setTelemetry(telemetry);

        CloudStorageRequest result = underTest.decorate(BLUEPRINT_NAME, CLUSTER_NAME, null, environment);

        assertNotNull(result);
        assertTrue(result.getIdentities().stream().anyMatch(id -> CloudIdentityType.LOG.equals(id.getType())));
    }

    @Test
    void testConvertWhenIdentityIsSetToNull() {
        CloudStorageRequest request = new CloudStorageRequest();
        request.setIdentities(null);

        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        TelemetryResponse telemetry = new TelemetryResponse();
        telemetry.setLogging(new LoggingResponse());
        environment.setTelemetry(telemetry);

        CloudStorageRequest result = underTest.decorate(BLUEPRINT_NAME, CLUSTER_NAME, request, environment);

        assertNotNull(result);
        assertEquals(1, result.getIdentities().size());
    }

    @Test
    void testConvertWhenCloudStorageLocationsIsEmptyAndEnvironmentHasTelemetryWithLogging() {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        TelemetryResponse telemetry = new TelemetryResponse();
        telemetry.setLogging(new LoggingResponse());
        environment.setTelemetry(telemetry);

        CloudStorageRequest result = underTest.decorate(BLUEPRINT_NAME, CLUSTER_NAME, null, environment);

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

        CloudStorageRequest result = underTest.decorate(BLUEPRINT_NAME, CLUSTER_NAME, request, environment);

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

        CloudStorageRequest result = underTest.decorate(BLUEPRINT_NAME, CLUSTER_NAME, request, new DetailedEnvironmentResponse());

        assertNotNull(result);
        assertTrue(result.getLocations().stream().anyMatch(loc -> eStorageLocationType.equals(loc.getType()) && eStorageLocationValue.equals(loc.getValue())));
    }

    @Test
    void testConvertWhenRequestHasNotCloudStorageLocationsAndSdxHasShouldTheSdxOnesBeUsed() {
        CloudStorageCdpService storageLocationType = CloudStorageCdpService.RANGER_AUDIT;
        SdxClusterResponse sdxReponse = new SdxClusterResponse();
        String storageLocationValue = "MYBUCKET/CONTAINER2";
        sdxReponse.setCloudStorageBaseLocation(storageLocationValue);
        sdxReponse.setCloudStorageFileSystemType(FileSystemType.S3);
        when(sdxClientService.getByEnvironmentCrn(any())).thenReturn(List.of(sdxReponse));
        ConfigQueryEntry sdxConfigQueryEntry = new ConfigQueryEntry();
        sdxConfigQueryEntry.setType(storageLocationType);
        sdxConfigQueryEntry.setDefaultPath(storageLocationValue);
        when(blueprintService.queryFileSystemParameters(any(), any(), any(), any(), any(), eq(Boolean.TRUE), eq(Boolean.FALSE), eq(0L)))
                .thenReturn(Set.of(sdxConfigQueryEntry));

        CloudStorageRequest result = underTest.decorate(BLUEPRINT_NAME, CLUSTER_NAME, null, new DetailedEnvironmentResponse());

        assertNotNull(result);
        assertTrue(result.getLocations().stream().anyMatch(loc -> storageLocationType.equals(loc.getType()) && storageLocationValue.equals(loc.getValue())));

    }

    @Test
    void testConvertWhenRequestHasCloudStorageLocationsAndSdxHasDifferentStorageLocationsShouldTheRequestLocationsBeUsed() {
        CloudStorageCdpService storageLocationType = CloudStorageCdpService.RANGER_AUDIT;
        String eStorageLocationValue = "MYBUCKET/CONTAINER";
        StorageLocationBase storageLocationBase = new StorageLocationBase();
        storageLocationBase.setType(storageLocationType);
        storageLocationBase.setValue(eStorageLocationValue);
        List<StorageLocationBase> storageLocations = List.of(storageLocationBase);
        CloudStorageRequest request = new CloudStorageRequest();
        request.setLocations(storageLocations);
        SdxClusterResponse sdxReponse = new SdxClusterResponse();
        String storageLocationValue = "MYBUCKET/CONTAINER2";
        sdxReponse.setCloudStorageBaseLocation(storageLocationValue);
        sdxReponse.setCloudStorageFileSystemType(FileSystemType.S3);
        when(sdxClientService.getByEnvironmentCrn(any())).thenReturn(List.of(sdxReponse));

        CloudStorageRequest result = underTest.decorate(BLUEPRINT_NAME, CLUSTER_NAME, request, new DetailedEnvironmentResponse());

        assertNotNull(result);
        assertTrue(result.getLocations().stream().anyMatch(loc -> storageLocationType.equals(loc.getType()) && eStorageLocationValue.equals(loc.getValue())));

    }
}