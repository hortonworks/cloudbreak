package com.sequenceiq.datalake.controller.sdx;

import static com.sequenceiq.common.model.ProviderSyncState.BASIC_SKU_MIGRATION_NEEDED;
import static com.sequenceiq.common.model.ProviderSyncState.VALID;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.util.Assert.notNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseResponse;

@ExtendWith(MockitoExtension.class)
class SdxClusterConverterTest {

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private SdxService sdxService;

    @InjectMocks
    private SdxClusterConverter sdxClusterConverter;

    @Test
    void sdxClusterStatusConverter() {
        for (SdxClusterStatusResponse s : SdxClusterStatusResponse.values()) {
            notNull(DatalakeStatusEnum.valueOf(s.name()), s.name());
        }
        for (DatalakeStatusEnum value : DatalakeStatusEnum.values()) {
            notNull(SdxClusterStatusResponse.valueOf(value.name()), value.name());
        }
        assertThrows("null Response conversion", NullPointerException.class, () -> SdxClusterStatusResponse.valueOf(null));
        assertThrows("null Status conversion", NullPointerException.class, () -> DatalakeStatusEnum.valueOf(null));
    }

    @Test
    public void testSdxClusterToResponse() {
        SdxCluster sdxCluster = generateSdxCluster();
        SdxStatusEntity sdxStatusEntity = new SdxStatusEntity();
        sdxStatusEntity.setStatus(DatalakeStatusEnum.RUNNING);
        sdxStatusEntity.setStatusReason("Cluster created successfully");

        when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(sdxStatusEntity);

        SdxClusterResponse response = sdxClusterConverter.sdxClusterToResponse(sdxCluster);

        assertEquals("testCrn", response.getCrn());
        assertEquals(SdxClusterStatusResponse.RUNNING, response.getStatus());
        assertEquals("Cluster created successfully", response.getStatusReason());
        assertEquals("testClusterName", response.getName());
        assertEquals(SdxClusterShape.LIGHT_DUTY, response.getClusterShape());
        assertEquals("testEnvironmentName", response.getEnvironmentName());
        assertEquals("testEnvironmentCrn", response.getEnvironmentCrn());
        assertEquals("testDatabaseCrn", response.getDatabaseServerCrn());
        assertEquals("testStackCrn", response.getStackCrn());
        assertEquals(123456789L, response.getCreated());
        assertEquals("s3://test-bucket", response.getCloudStorageBaseLocation());
        assertEquals(FileSystemType.S3, response.getCloudStorageFileSystemType());
        assertEquals("CDH6.3", response.getRuntime());
        assertTrue(response.getRangerRazEnabled());
        assertTrue(response.isEnableMultiAz());
        assertEquals("9.6.20", response.getDatabaseEngineVersion());
        assertNull(response.getDetachedClusterName());
        assertFalse(response.isDetached());

        Map<String, String> expectedTags = new HashMap<>();
        expectedTags.put("key1", "value1");
        expectedTags.put("key2", "value2");
        assertEquals(expectedTags, response.getTags());
        assertEquals(Set.of(VALID, BASIC_SKU_MIGRATION_NEEDED), response.getProviderSyncStates());

        SdxDatabaseResponse databaseResponse = response.getSdxDatabaseResponse();

        // Assert that the retrieved SdxDatabaseResponse matches the original one
        assertEquals(SdxDatabaseAvailabilityType.HA, databaseResponse.getAvailabilityType());
        assertEquals("9.6.20", databaseResponse.getDatabaseEngineVersion());
        assertEquals("testDatabaseCrn", databaseResponse.getDatabaseServerCrn());
    }

    @Test
    public void testSdxClusterToResponseSetDetachedClusterName() {
        SdxCluster sdxCluster = generateSdxCluster();
        SdxStatusEntity sdxStatusEntity = new SdxStatusEntity();
        sdxStatusEntity.setStatus(DatalakeStatusEnum.RUNNING);
        sdxStatusEntity.setStatusReason("Cluster created successfully");
        SdxCluster originalSdxCluster = new SdxCluster();
        originalSdxCluster.setClusterName("originalClusterName");

        when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(sdxStatusEntity);
        when(sdxService.findDetachedSdxClusterByOriginalCrn(sdxCluster.getCrn())).thenReturn(Optional.of(originalSdxCluster));

        SdxClusterResponse response = sdxClusterConverter.sdxClusterToResponse(sdxCluster);

        assertEquals("testCrn", response.getCrn());
        assertEquals(SdxClusterStatusResponse.RUNNING, response.getStatus());
        assertEquals("Cluster created successfully", response.getStatusReason());
        assertEquals("testClusterName", response.getName());
        assertEquals(SdxClusterShape.LIGHT_DUTY, response.getClusterShape());
        assertEquals("testEnvironmentName", response.getEnvironmentName());
        assertEquals("testEnvironmentCrn", response.getEnvironmentCrn());
        assertEquals("testDatabaseCrn", response.getDatabaseServerCrn());
        assertEquals("testStackCrn", response.getStackCrn());
        assertEquals(123456789L, response.getCreated());
        assertEquals("s3://test-bucket", response.getCloudStorageBaseLocation());
        assertEquals(FileSystemType.S3, response.getCloudStorageFileSystemType());
        assertEquals("CDH6.3", response.getRuntime());
        assertTrue(response.getRangerRazEnabled());
        assertTrue(response.isEnableMultiAz());
        assertEquals("9.6.20", response.getDatabaseEngineVersion());
        assertEquals("originalClusterName", response.getDetachedClusterName());
        assertFalse(response.isDetached());

        Map<String, String> expectedTags = new HashMap<>();
        expectedTags.put("key1", "value1");
        expectedTags.put("key2", "value2");
        assertEquals(expectedTags, response.getTags());
        assertEquals(Set.of(VALID, BASIC_SKU_MIGRATION_NEEDED), response.getProviderSyncStates());

        SdxDatabaseResponse databaseResponse = response.getSdxDatabaseResponse();

        // Assert that the retrieved SdxDatabaseResponse matches the original one
        assertEquals(SdxDatabaseAvailabilityType.HA, databaseResponse.getAvailabilityType());
        assertEquals("9.6.20", databaseResponse.getDatabaseEngineVersion());
        assertEquals("testDatabaseCrn", databaseResponse.getDatabaseServerCrn());
    }

    private SdxCluster generateSdxCluster() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setSdxDatabase(new SdxDatabase());
        sdxCluster.setClusterName("testClusterName");
        sdxCluster.setCrn("testCrn");
        sdxCluster.setClusterShape(SdxClusterShape.LIGHT_DUTY);
        sdxCluster.setEnvName("testEnvironmentName");
        sdxCluster.setEnvCrn("testEnvironmentCrn");
        sdxCluster.getSdxDatabase().setDatabaseCrn("testDatabaseCrn");
        sdxCluster.setStackCrn("testStackCrn");
        sdxCluster.setCreated(123456789L);
        sdxCluster.setCloudStorageBaseLocation("s3://test-bucket");
        sdxCluster.setCloudStorageFileSystemType(FileSystemType.S3);
        sdxCluster.setRuntime("CDH6.3");
        sdxCluster.setRangerRazEnabled(true);
        sdxCluster.setSeLinux(SeLinux.PERMISSIVE);
        sdxCluster.setEnableMultiAz(true);
        sdxCluster.getSdxDatabase().setDatabaseEngineVersion("9.6.20");
        sdxCluster.getSdxDatabase().setDatabaseAvailabilityType(SdxDatabaseAvailabilityType.HA);
        sdxCluster.setProviderSyncStates(Set.of(VALID, BASIC_SKU_MIGRATION_NEEDED));

        Map<String, String> tags = new HashMap<>();
        tags.put("key1", "value1");
        tags.put("key2", "value2");
        sdxCluster.setTags(new Json(tags));
        return sdxCluster;
    }
}