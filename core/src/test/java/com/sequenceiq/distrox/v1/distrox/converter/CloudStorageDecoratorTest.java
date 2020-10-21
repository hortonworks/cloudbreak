package com.sequenceiq.distrox.v1.distrox.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cmtemplate.cloudstorage.CmCloudStorageConfigProvider;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.datalake.SdxClientService;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigQueryObject;
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

    @Mock
    private CmCloudStorageConfigProvider cmCloudStorageConfigProvider;

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

    @Test
    void testUpdateCloudStorageLocationsWhenRequestContainsTemplatedLocationThenItShouldBeReplaced() {
        CloudStorageCdpService storageLocationType = CloudStorageCdpService.DEFAULT_FS;
        String eStorageLocationValue = "s3a://some-dir/some-other-dir/{{{clusterName}}}";
        StorageLocationBase storageLocationBase = new StorageLocationBase();
        storageLocationBase.setType(storageLocationType);
        storageLocationBase.setValue(eStorageLocationValue);
        List<StorageLocationBase> storageLocations = new ArrayList<>(1);
        storageLocations.add(storageLocationBase);
        CloudStorageRequest request = new CloudStorageRequest();
        request.setLocations(storageLocations);
        SdxClusterResponse sdxResponse = new SdxClusterResponse();
        String storageLocationValue = "s3a://some-dir/some-other-dir/" + CLUSTER_NAME;
        sdxResponse.setCloudStorageBaseLocation(storageLocationValue);
        sdxResponse.setCloudStorageFileSystemType(FileSystemType.S3);

        ConfigQueryEntry cqe = new ConfigQueryEntry();
        cqe.setType(CloudStorageCdpService.DEFAULT_FS);
        cqe.setDefaultPath(storageLocationValue);

        Set<ConfigQueryEntry> cqes = new LinkedHashSet<>(1);
        cqes.add(cqe);

        when(blueprintService.queryFileSystemParameters(BLUEPRINT_NAME, CLUSTER_NAME, storageLocationValue, FileSystemType.S3.name(), "", true, false, 0L))
                .thenReturn(cqes);

        Pair<Blueprint, String> mockBt = mock(Pair.class);
        when(blueprintService.getBlueprintAndText(BLUEPRINT_NAME, 0L)).thenReturn(mockBt);

        FileSystemConfigQueryObject mockfscqo = mock(FileSystemConfigQueryObject.class);
        when(blueprintService.createFileSystemConfigQueryObject(mockBt, CLUSTER_NAME, sdxResponse.getCloudStorageBaseLocation(),
                sdxResponse.getCloudStorageFileSystemType().name(), "", true, false)).thenReturn(mockfscqo);

        when(cmCloudStorageConfigProvider.queryParameters(any(), eq(mockfscqo))).thenReturn(cqes);

        CloudStorageRequest result = underTest.updateCloudStorageLocations(BLUEPRINT_NAME, CLUSTER_NAME, request, List.of(sdxResponse));

        assertNotNull(result);
        assertEquals(0, result.getLocations().stream().filter(slb -> slb.getValue().contains("{{{") && slb.getValue().contains("}}}")).count());
    }

    @Test
    void testUpdateCloudStorageLocationsWhenRequestContainsOneTemplatedLocationAndOneWithoutTemplatePlaceholderThenThatShouldBeReplaced() {
        String templatedStorageLocationBaseValue = "s3a://some-dir/some-other-dir/";

        CloudStorageCdpService templatedStorageLocationType = CloudStorageCdpService.DEFAULT_FS;
        String templatedStorageLocationValue = templatedStorageLocationBaseValue + "{{{clusterName}}}";
        StorageLocationBase templatedStorageLocationBase = new StorageLocationBase();
        templatedStorageLocationBase.setType(templatedStorageLocationType);
        templatedStorageLocationBase.setValue(templatedStorageLocationValue);

        CloudStorageCdpService storageLocationType = CloudStorageCdpService.FLINK_JOBMANAGER_ARCHIVE;
        String eStorageLocationValue = "s3a://some-awesome-dir/some-other-awesome-dir/" + CLUSTER_NAME;
        StorageLocationBase storageLocationBase = new StorageLocationBase();
        storageLocationBase.setType(storageLocationType);
        storageLocationBase.setValue(eStorageLocationValue);

        List<StorageLocationBase> storageLocations = new ArrayList<>(2);
        storageLocations.add(storageLocationBase);
        storageLocations.add(templatedStorageLocationBase);

        ConfigQueryEntry cqe = new ConfigQueryEntry();
        cqe.setType(CloudStorageCdpService.DEFAULT_FS);
        cqe.setDefaultPath(templatedStorageLocationBaseValue + CLUSTER_NAME);

        CloudStorageRequest request = new CloudStorageRequest();
        request.setLocations(storageLocations);
        SdxClusterResponse sdxReponse = new SdxClusterResponse();
        String storageLocationValue = eStorageLocationValue;
        sdxReponse.setCloudStorageBaseLocation(storageLocationValue);
        sdxReponse.setCloudStorageFileSystemType(FileSystemType.S3);

        ConfigQueryEntry cqeFlink = new ConfigQueryEntry();
        cqeFlink.setType(CloudStorageCdpService.FLINK_JOBMANAGER_ARCHIVE);
        cqeFlink.setDefaultPath(storageLocationValue);

        Set<ConfigQueryEntry> cqes = new LinkedHashSet<>(1);
        cqes.add(cqe);
        cqes.add(cqeFlink);

        when(blueprintService.queryFileSystemParameters(BLUEPRINT_NAME, CLUSTER_NAME, storageLocationValue, FileSystemType.S3.name(), "", true, false, 0L))
                .thenReturn(cqes);

        Pair<Blueprint, String> mockBt = mock(Pair.class);
        when(blueprintService.getBlueprintAndText(BLUEPRINT_NAME, 0L)).thenReturn(mockBt);

        FileSystemConfigQueryObject mockfscqo = mock(FileSystemConfigQueryObject.class);
        when(blueprintService.createFileSystemConfigQueryObject(mockBt, CLUSTER_NAME, sdxReponse.getCloudStorageBaseLocation(),
                sdxReponse.getCloudStorageFileSystemType().name(), "", true, false)).thenReturn(mockfscqo);

        when(cmCloudStorageConfigProvider.queryParameters(any(), eq(mockfscqo))).thenReturn(cqes);

        CloudStorageRequest result = underTest.updateCloudStorageLocations(BLUEPRINT_NAME, CLUSTER_NAME, request, List.of(sdxReponse));

        assertNotNull(result);
        assertEquals(0, result.getLocations().stream().filter(slb -> slb.getValue().contains("{{{") && slb.getValue().contains("}}}")).count());
    }

}