package com.sequenceiq.datalake.service.sdx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
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

    @Test
    public void whenPlatformIsAwsWithDefaultsShouldCreateDatabase() {
        CloudPlatform cloudPlatform = CloudPlatform.AWS;
        when(platformConfig.isExternalDatabaseSupportedFor(cloudPlatform)).thenReturn(true);
        when(platformConfig.isExternalDatabaseSupportedOrExperimental(cloudPlatform)).thenReturn(true);
        when(databaseDefaultVersionProvider.calculateDbVersionBasedOnRuntimeIfMissing(null, null)).thenReturn("11");
        SdxCluster sdxCluster = new SdxCluster();

        underTest.configure(cloudPlatform, null, null, sdxCluster);

        assertTrue(sdxCluster.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.HA, sdxCluster.getDatabaseAvailabilityType());
        assertEquals("11", sdxCluster.getDatabaseEngineVersion());
    }

    @Test
    public void whenPlatformIsAwsWithSkipCreateShouldNotCreateDatabase() {
        CloudPlatform cloudPlatform = CloudPlatform.AWS;
        SdxDatabaseRequest dbRequest = new SdxDatabaseRequest();
        dbRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NONE);
        SdxCluster sdxCluster = new SdxCluster();
        when(databaseDefaultVersionProvider.calculateDbVersionBasedOnRuntimeIfMissing(null, null)).thenReturn("11");

        underTest.configure(cloudPlatform, null, dbRequest, sdxCluster);

        assertFalse(sdxCluster.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.NONE, sdxCluster.getDatabaseAvailabilityType());
        assertEquals("11", sdxCluster.getDatabaseEngineVersion());
    }

    @Test
    public void whenPlatformIsAwsAndCreateShouldCreateDatabase() {
        CloudPlatform cloudPlatform = CloudPlatform.AWS;
        when(platformConfig.isExternalDatabaseSupportedOrExperimental(cloudPlatform)).thenReturn(true);
        SdxDatabaseRequest dbRequest = new SdxDatabaseRequest();
        dbRequest.setAvailabilityType(SdxDatabaseAvailabilityType.HA);
        SdxCluster sdxCluster = new SdxCluster();
        when(databaseDefaultVersionProvider.calculateDbVersionBasedOnRuntimeIfMissing(null, null)).thenReturn("11");

        underTest.configure(cloudPlatform, null, dbRequest, sdxCluster);

        assertTrue(sdxCluster.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.HA, sdxCluster.getDatabaseAvailabilityType());
        assertEquals("11", sdxCluster.getDatabaseEngineVersion());
    }

    @Test
    public void whenPlatformIsAzureWithoutRuntimeVerionSet() {
        CloudPlatform cloudPlatform = CloudPlatform.AZURE;
        when(platformConfig.isExternalDatabaseSupportedFor(cloudPlatform)).thenReturn(true);
        when(platformConfig.isExternalDatabaseSupportedOrExperimental(CloudPlatform.AZURE)).thenReturn(true);
        SdxDatabaseRequest dbRequest = new SdxDatabaseRequest();
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("clusterName");
        when(databaseDefaultVersionProvider.calculateDbVersionBasedOnRuntimeIfMissing(null, null)).thenReturn("11");

        underTest.configure(cloudPlatform, null, dbRequest, sdxCluster);

        assertTrue(sdxCluster.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.HA, sdxCluster.getDatabaseAvailabilityType());
        assertEquals("11", sdxCluster.getDatabaseEngineVersion());
    }

    @Test
    public void whenPlatformIsAzureWithoutRuntimeVerionSetAndNoDbRequested() {
        CloudPlatform cloudPlatform = CloudPlatform.AZURE;
        SdxDatabaseRequest dbRequest = new SdxDatabaseRequest();
        dbRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NONE);
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("clusterName");
        when(databaseDefaultVersionProvider.calculateDbVersionBasedOnRuntimeIfMissing(null, null)).thenReturn("11");

        underTest.configure(cloudPlatform, null, dbRequest, sdxCluster);

        assertFalse(sdxCluster.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.NONE, sdxCluster.getDatabaseAvailabilityType());
        assertEquals("11", sdxCluster.getDatabaseEngineVersion());
    }

    @Test
    public void whenPlatformIsAzureWithNotSupportedRuntime() {
        CloudPlatform cloudPlatform = CloudPlatform.AZURE;
        when(platformConfig.isExternalDatabaseSupportedFor(cloudPlatform)).thenReturn(true);
        SdxDatabaseRequest dbRequest = new SdxDatabaseRequest();
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("clusterName");
        sdxCluster.setRuntime("7.0.2");
        when(databaseDefaultVersionProvider.calculateDbVersionBasedOnRuntimeIfMissing(sdxCluster.getRuntime(), null)).thenReturn("11");


        underTest.configure(cloudPlatform, null, dbRequest, sdxCluster);

        assertFalse(sdxCluster.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.NONE, sdxCluster.getDatabaseAvailabilityType());
        assertEquals("11", sdxCluster.getDatabaseEngineVersion());
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
        when(databaseDefaultVersionProvider.calculateDbVersionBasedOnRuntimeIfMissing(sdxCluster.getRuntime(), null)).thenReturn("11");

        underTest.configure(cloudPlatform, null, dbRequest, sdxCluster);

        assertTrue(sdxCluster.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.HA, sdxCluster.getDatabaseAvailabilityType());
        assertEquals("11", sdxCluster.getDatabaseEngineVersion());
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
        when(databaseDefaultVersionProvider.calculateDbVersionBasedOnRuntimeIfMissing(sdxCluster.getRuntime(), null)).thenReturn("11");

        underTest.configure(cloudPlatform, null, dbRequest, sdxCluster);

        assertTrue(sdxCluster.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.HA, sdxCluster.getDatabaseAvailabilityType());
        assertEquals("11", sdxCluster.getDatabaseEngineVersion());
    }

    @Test
    public void whenPlatformIsYarnShouldNotAllowDatabase() {
        CloudPlatform cloudPlatform = CloudPlatform.YARN;
        when(platformConfig.isExternalDatabaseSupportedOrExperimental(cloudPlatform)).thenReturn(false);
        SdxDatabaseRequest dbRequest = new SdxDatabaseRequest();
        dbRequest.setAvailabilityType(SdxDatabaseAvailabilityType.HA);
        SdxCluster sdxCluster = new SdxCluster();
        when(databaseDefaultVersionProvider.calculateDbVersionBasedOnRuntimeIfMissing(null, null)).thenReturn("11");

        Assertions.assertThrows(BadRequestException.class, () -> underTest.configure(cloudPlatform, null, dbRequest, sdxCluster));

        assertTrue(sdxCluster.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.HA, sdxCluster.getDatabaseAvailabilityType());
        assertEquals("11", sdxCluster.getDatabaseEngineVersion());
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
        when(databaseDefaultVersionProvider.calculateDbVersionBasedOnRuntimeIfMissing(null, databaseEngineVersion)).thenReturn(databaseEngineVersion);

        underTest.configure(cloudPlatform, databaseRequest, null, sdxCluster);

        assertTrue(sdxCluster.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.NON_HA, sdxCluster.getDatabaseAvailabilityType());
        assertEquals(databaseEngineVersion, sdxCluster.getDatabaseEngineVersion());
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
        when(databaseDefaultVersionProvider.calculateDbVersionBasedOnRuntimeIfMissing(null, databaseEngineVersion)).thenReturn(databaseEngineVersion);

        underTest.configure(cloudPlatform, databaseRequest, null, sdxCluster);

        assertTrue(sdxCluster.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.HA, sdxCluster.getDatabaseAvailabilityType());
        assertEquals(databaseEngineVersion, sdxCluster.getDatabaseEngineVersion());
    }

    @Test
    public void whenPlatformIsAwsWithInternalRequestAndRuntimeVersionIsNotPresentAndAvailabilityTypeIsHa() {
        CloudPlatform cloudPlatform = CloudPlatform.AWS;
        DatabaseRequest databaseRequest = new DatabaseRequest();
        databaseRequest.setAvailabilityType(DatabaseAvailabilityType.HA);
        SdxCluster sdxCluster = new SdxCluster();
        when(platformConfig.isExternalDatabaseSupportedOrExperimental(cloudPlatform)).thenReturn(true);
        when(databaseDefaultVersionProvider.calculateDbVersionBasedOnRuntimeIfMissing(null, null)).thenReturn(null);

        underTest.configure(cloudPlatform, databaseRequest, null, sdxCluster);

        assertTrue(sdxCluster.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.HA, sdxCluster.getDatabaseAvailabilityType());
        assertNull(sdxCluster.getDatabaseEngineVersion());
    }

    @Test
    public void whenPlatformIsAwsWithInternalRequestAndRuntimeVersionIsPresentAndAvailabilityTypeIsNull() {
        CloudPlatform cloudPlatform = CloudPlatform.AWS;
        DatabaseRequest databaseRequest = new DatabaseRequest();
        String databaseEngineVersion = "11";
        databaseRequest.setDatabaseEngineVersion(databaseEngineVersion);
        SdxCluster sdxCluster = new SdxCluster();
        when(databaseDefaultVersionProvider.calculateDbVersionBasedOnRuntimeIfMissing(null, databaseEngineVersion)).thenReturn(databaseEngineVersion);

        underTest.configure(cloudPlatform, databaseRequest, null, sdxCluster);

        assertFalse(sdxCluster.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.NONE, sdxCluster.getDatabaseAvailabilityType());
        assertEquals(databaseEngineVersion, sdxCluster.getDatabaseEngineVersion());
    }
}