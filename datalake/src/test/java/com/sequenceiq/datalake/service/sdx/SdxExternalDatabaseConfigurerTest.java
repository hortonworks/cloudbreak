package com.sequenceiq.datalake.service.sdx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseRequest;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.service.database.DatabaseDefaultVersionProvider;
import com.sequenceiq.datalake.configuration.PlatformConfig;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.datalake.service.sdx.database.AzureDatabaseAttributesService;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

@ExtendWith(MockitoExtension.class)
public class SdxExternalDatabaseConfigurerTest {

    @Mock
    private PlatformConfig platformConfig;

    @Mock
    private DatabaseDefaultVersionProvider databaseDefaultVersionProvider;

    @Mock
    private AzureDatabaseAttributesService  azureDatabaseAttributesService;

    @InjectMocks
    private SdxExternalDatabaseConfigurer underTest;

    @BeforeEach
    void setUp() {
        lenient().when(databaseDefaultVersionProvider.calculateDbVersionBasedOnRuntimeAndOsIfMissing(any(), any(), any(), any(), anyBoolean())).thenReturn("11");
    }

    @Test
    public void whenPlatformIsAwsWithDefaultsShouldCreateDatabase() {
        CloudPlatform cloudPlatform = CloudPlatform.AWS;
        when(platformConfig.isExternalDatabaseSupportedFor(cloudPlatform)).thenReturn(true);
        when(platformConfig.isExternalDatabaseSupportedOrExperimental(cloudPlatform)).thenReturn(true);
        SdxCluster sdxCluster = new SdxCluster();

        SdxDatabase actualResult = underTest.configure(cloudPlatform, null, null, null, sdxCluster);

        assertTrue(actualResult.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.HA, actualResult.getDatabaseAvailabilityType());
        assertEquals("11", actualResult.getDatabaseEngineVersion());
    }

    @Test
    public void whenPlatformIsAwsWithSkipCreateShouldNotCreateDatabase() {
        CloudPlatform cloudPlatform = CloudPlatform.AWS;
        SdxDatabaseRequest dbRequest = new SdxDatabaseRequest();
        dbRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NONE);
        SdxCluster sdxCluster = new SdxCluster();

        SdxDatabase sdxDatabase = underTest.configure(cloudPlatform, null, null, dbRequest, sdxCluster);

        assertFalse(sdxDatabase.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.NONE, sdxDatabase.getDatabaseAvailabilityType());
        assertEquals("11", sdxDatabase.getDatabaseEngineVersion());
    }

    @Test
    public void whenPlatformIsAwsAndCreateShouldCreateDatabase() {
        CloudPlatform cloudPlatform = CloudPlatform.AWS;
        when(platformConfig.isExternalDatabaseSupportedOrExperimental(cloudPlatform)).thenReturn(true);
        SdxDatabaseRequest dbRequest = new SdxDatabaseRequest();
        dbRequest.setAvailabilityType(SdxDatabaseAvailabilityType.HA);
        SdxCluster sdxCluster = new SdxCluster();

        SdxDatabase sdxDatabase = underTest.configure(cloudPlatform, null, null, dbRequest, sdxCluster);

        assertTrue(sdxDatabase.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.HA, sdxDatabase.getDatabaseAvailabilityType());
        assertEquals("11", sdxDatabase.getDatabaseEngineVersion());
    }

    @Test
    public void whenPlatformIsAzureWithoutRuntimeVerionSet() {
        CloudPlatform cloudPlatform = CloudPlatform.AZURE;
        when(platformConfig.isExternalDatabaseSupportedFor(cloudPlatform)).thenReturn(true);
        when(platformConfig.isExternalDatabaseSupportedOrExperimental(CloudPlatform.AZURE)).thenReturn(true);
        SdxDatabaseRequest dbRequest = new SdxDatabaseRequest();
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("clusterName");

        SdxDatabase sdxDatabase = underTest.configure(cloudPlatform, null, null, dbRequest, sdxCluster);

        assertTrue(sdxDatabase.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.HA, sdxDatabase.getDatabaseAvailabilityType());
        assertEquals("11", sdxDatabase.getDatabaseEngineVersion());
    }

    @Test
    public void whenPlatformIsAzureWithoutRuntimeVerionSetAndNoDbRequested() {
        CloudPlatform cloudPlatform = CloudPlatform.AZURE;
        SdxDatabaseRequest dbRequest = new SdxDatabaseRequest();
        dbRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NONE);
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("clusterName");

        SdxDatabase sdxDatabase = underTest.configure(cloudPlatform, null, null, dbRequest, sdxCluster);

        assertFalse(sdxDatabase.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.NONE, sdxDatabase.getDatabaseAvailabilityType());
        assertEquals("11", sdxDatabase.getDatabaseEngineVersion());
    }

    @Test
    public void whenPlatformIsAzureWithNotSupportedRuntime() {
        CloudPlatform cloudPlatform = CloudPlatform.AZURE;
        when(platformConfig.isExternalDatabaseSupportedFor(cloudPlatform)).thenReturn(true);
        SdxDatabaseRequest dbRequest = new SdxDatabaseRequest();
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("clusterName");
        sdxCluster.setRuntime("7.0.2");

        SdxDatabase sdxDatabase = underTest.configure(cloudPlatform, null, null, dbRequest, sdxCluster);

        assertFalse(sdxDatabase.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.NONE, sdxDatabase.getDatabaseAvailabilityType());
        assertEquals("11", sdxDatabase.getDatabaseEngineVersion());
    }

    @Test
    public void whenPlatformIsAzureWithMinSupportedVersion() {
        CloudPlatform cloudPlatform = CloudPlatform.AZURE;
        when(platformConfig.isExternalDatabaseSupportedFor(cloudPlatform)).thenReturn(true);
        when(platformConfig.isExternalDatabaseSupportedOrExperimental(CloudPlatform.AZURE)).thenReturn(true);
        SdxDatabaseRequest dbRequest = new SdxDatabaseRequest();
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("clusterName");
        sdxCluster.setRuntime("7.1.0");

        SdxDatabase sdxDatabase = underTest.configure(cloudPlatform, null, null, dbRequest, sdxCluster);

        assertTrue(sdxDatabase.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.HA, sdxDatabase.getDatabaseAvailabilityType());
        assertEquals("11", sdxDatabase.getDatabaseEngineVersion());
    }

    @Test
    public void whenPlatformIsAzureWithNewerSupportedVersion() {
        CloudPlatform cloudPlatform = CloudPlatform.AZURE;
        when(platformConfig.isExternalDatabaseSupportedFor(cloudPlatform)).thenReturn(true);
        when(platformConfig.isExternalDatabaseSupportedOrExperimental(CloudPlatform.AZURE)).thenReturn(true);
        SdxDatabaseRequest dbRequest = new SdxDatabaseRequest();
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("clusterName");
        sdxCluster.setRuntime("7.2.0");

        SdxDatabase sdxDatabase = underTest.configure(cloudPlatform, null, null, dbRequest, sdxCluster);

        assertTrue(sdxDatabase.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.HA, sdxDatabase.getDatabaseAvailabilityType());
        assertEquals("11", sdxDatabase.getDatabaseEngineVersion());
    }

    @Test
    public void whenPlatformIsYarnShouldNotAllowDatabase() {
        CloudPlatform cloudPlatform = CloudPlatform.YARN;
        when(platformConfig.isExternalDatabaseSupportedOrExperimental(cloudPlatform)).thenReturn(false);
        SdxDatabaseRequest dbRequest = new SdxDatabaseRequest();
        dbRequest.setAvailabilityType(SdxDatabaseAvailabilityType.HA);
        SdxCluster sdxCluster = new SdxCluster();

        Assertions.assertThrows(BadRequestException.class, () -> underTest.configure(cloudPlatform, null, null, dbRequest, sdxCluster));
    }

    @Test
    public void whenPlatformIsAwsWithInternalRequestAndRuntimeVersionIsPresentAndAvailabilityTypeIsNonHa() {
        CloudPlatform cloudPlatform = CloudPlatform.AWS;
        DatabaseRequest databaseRequest = new DatabaseRequest();
        String databaseEngineVersion = "11";
        databaseRequest.setDatabaseEngineVersion(databaseEngineVersion);
        databaseRequest.setAvailabilityType(DatabaseAvailabilityType.NON_HA);
        SdxCluster sdxCluster = new SdxCluster();
        when(platformConfig.isExternalDatabaseSupportedOrExperimental(cloudPlatform)).thenReturn(true);

        SdxDatabase sdxDatabase = underTest.configure(cloudPlatform, null, databaseRequest, null, sdxCluster);

        assertTrue(sdxDatabase.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.NON_HA, sdxDatabase.getDatabaseAvailabilityType());
        assertEquals(databaseEngineVersion, sdxDatabase.getDatabaseEngineVersion());
    }

    @Test
    public void whenPlatformIsAwsWithInternalRequestAndRuntimeVersionIsPresentAndAvailabilityTypeIsHa() {
        CloudPlatform cloudPlatform = CloudPlatform.AWS;
        DatabaseRequest databaseRequest = new DatabaseRequest();
        String databaseEngineVersion = "11";
        databaseRequest.setDatabaseEngineVersion(databaseEngineVersion);
        databaseRequest.setAvailabilityType(DatabaseAvailabilityType.HA);
        SdxCluster sdxCluster = new SdxCluster();
        when(platformConfig.isExternalDatabaseSupportedOrExperimental(cloudPlatform)).thenReturn(true);

        SdxDatabase sdxDatabase = underTest.configure(cloudPlatform, null, databaseRequest, null, sdxCluster);

        assertTrue(sdxDatabase.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.HA, sdxDatabase.getDatabaseAvailabilityType());
        assertEquals(databaseEngineVersion, sdxDatabase.getDatabaseEngineVersion());
    }

    @Test
    public void whenPlatformIsAwsWithInternalRequestAndRuntimeVersionIsNotPresentAndAvailabilityTypeIsHa() {
        CloudPlatform cloudPlatform = CloudPlatform.AWS;
        DatabaseRequest databaseRequest = new DatabaseRequest();
        databaseRequest.setAvailabilityType(DatabaseAvailabilityType.HA);
        SdxCluster sdxCluster = new SdxCluster();
        when(platformConfig.isExternalDatabaseSupportedOrExperimental(cloudPlatform)).thenReturn(true);
        when(databaseDefaultVersionProvider.calculateDbVersionBasedOnRuntimeAndOsIfMissing(any(), any(), any(), any(), anyBoolean())).thenReturn(null);

        SdxDatabase sdxDatabase = underTest.configure(cloudPlatform, null, databaseRequest, null, sdxCluster);

        assertTrue(sdxDatabase.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.HA, sdxDatabase.getDatabaseAvailabilityType());
        assertNull(sdxDatabase.getDatabaseEngineVersion());
    }

    @Test
    public void whenPlatformIsAwsWithInternalRequestAndRuntimeVersionIsPresentAndAvailabilityTypeIsNull() {
        CloudPlatform cloudPlatform = CloudPlatform.AWS;
        DatabaseRequest databaseRequest = new DatabaseRequest();
        String databaseEngineVersion = "11";
        databaseRequest.setDatabaseEngineVersion(databaseEngineVersion);
        SdxCluster sdxCluster = new SdxCluster();

        SdxDatabase sdxDatabase = underTest.configure(cloudPlatform, null, databaseRequest, null, sdxCluster);

        assertFalse(sdxDatabase.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.NONE, sdxDatabase.getDatabaseAvailabilityType());
        assertEquals(databaseEngineVersion, sdxDatabase.getDatabaseEngineVersion());
    }
}