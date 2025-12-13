package com.sequenceiq.datalake.service.sdx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAzureRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseRequest;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.service.database.DatabaseDefaultVersionProvider;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.datalake.configuration.PlatformConfig;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.datalake.service.sdx.database.AzureDatabaseAttributesService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseAzureRequest;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

@ExtendWith(MockitoExtension.class)
public class SdxExternalDatabaseConfigurerTest {
    private static final String ACTOR = "crn:cdp:iam:us-west-1:cloudera:user:__internal__actor__";

    @Mock
    private PlatformConfig platformConfig;

    @Mock
    private DatabaseDefaultVersionProvider databaseDefaultVersionProvider;

    @Mock
    private AzureDatabaseAttributesService azureDatabaseAttributesService;

    @InjectMocks
    private SdxExternalDatabaseConfigurer underTest;

    private DetailedEnvironmentResponse environmentResponse;

    @BeforeEach
    void setUp() {
        lenient().when(databaseDefaultVersionProvider.calculateDbVersionBasedOnRuntime(any(), any()))
                .thenReturn("11");
        environmentResponse = new DetailedEnvironmentResponse();
    }

    @Test
    public void whenPlatformIsAwsWithDefaultsShouldCreateDatabase() {
        CloudPlatform cloudPlatform = CloudPlatform.AWS;
        environmentResponse.setCloudPlatform(cloudPlatform.name());
        when(platformConfig.isExternalDatabaseSupportedFor(cloudPlatform)).thenReturn(true);
        when(platformConfig.isExternalDatabaseSupportedOrExperimental(cloudPlatform)).thenReturn(true);
        SdxCluster sdxCluster = new SdxCluster();

        SdxDatabase actualResult = ThreadBasedUserCrnProvider.doAs(ACTOR, () -> underTest.configure(environmentResponse, null, null, null, sdxCluster));

        assertTrue(actualResult.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.HA, actualResult.getDatabaseAvailabilityType());
        assertEquals("11", actualResult.getDatabaseEngineVersion());
    }

    @Test
    public void whenPlatformIsAwsWithSkipCreateShouldNotCreateDatabase() {
        CloudPlatform cloudPlatform = CloudPlatform.AWS;
        environmentResponse.setCloudPlatform(cloudPlatform.name());
        SdxDatabaseRequest dbRequest = new SdxDatabaseRequest();
        dbRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NONE);
        SdxCluster sdxCluster = new SdxCluster();

        SdxDatabase sdxDatabase = ThreadBasedUserCrnProvider.doAs(ACTOR, () -> underTest.configure(environmentResponse, null, null, dbRequest, sdxCluster));

        assertFalse(sdxDatabase.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.NONE, sdxDatabase.getDatabaseAvailabilityType());
        assertEquals("11", sdxDatabase.getDatabaseEngineVersion());
    }

    @Test
    public void whenPlatformIsAwsAndCreateShouldCreateDatabase() {
        CloudPlatform cloudPlatform = CloudPlatform.AWS;
        environmentResponse.setCloudPlatform(cloudPlatform.name());
        when(platformConfig.isExternalDatabaseSupportedOrExperimental(cloudPlatform)).thenReturn(true);
        SdxDatabaseRequest dbRequest = new SdxDatabaseRequest();
        dbRequest.setAvailabilityType(SdxDatabaseAvailabilityType.HA);
        SdxCluster sdxCluster = new SdxCluster();

        SdxDatabase sdxDatabase = ThreadBasedUserCrnProvider.doAs(ACTOR, () -> underTest.configure(environmentResponse, null, null, dbRequest, sdxCluster));

        assertTrue(sdxDatabase.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.HA, sdxDatabase.getDatabaseAvailabilityType());
        assertEquals("11", sdxDatabase.getDatabaseEngineVersion());
    }

    @Test
    public void whenPlatformIsAzureWithoutRuntimeVerionSet() {
        CloudPlatform cloudPlatform = CloudPlatform.AZURE;
        environmentResponse.setCloudPlatform(cloudPlatform.name());
        when(platformConfig.isExternalDatabaseSupportedFor(cloudPlatform)).thenReturn(true);
        when(platformConfig.isExternalDatabaseSupportedOrExperimental(CloudPlatform.AZURE)).thenReturn(true);
        SdxDatabaseRequest dbRequest = new SdxDatabaseRequest();
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("clusterName");
        when(azureDatabaseAttributesService.determineAzureDatabaseType(null, dbRequest)).thenReturn(AzureDatabaseType.SINGLE_SERVER);

        SdxDatabase sdxDatabase = ThreadBasedUserCrnProvider.doAs(ACTOR, () -> underTest.configure(environmentResponse, null, null, dbRequest, sdxCluster));

        assertTrue(sdxDatabase.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.HA, sdxDatabase.getDatabaseAvailabilityType());
        assertEquals("11", sdxDatabase.getDatabaseEngineVersion());
        verify(azureDatabaseAttributesService).configureAzureDatabase(eq(AzureDatabaseType.SINGLE_SERVER), isNull(), eq(dbRequest), any(SdxDatabase.class));
    }

    @Test
    public void whenPlatformIsAzureWithoutRuntimeVerionSetEmbeddedRequested() {
        CloudPlatform cloudPlatform = CloudPlatform.AZURE;
        environmentResponse.setCloudPlatform(cloudPlatform.name());
        SdxDatabaseRequest dbRequest = new SdxDatabaseRequest();
        dbRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NONE);
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("clusterName");

        SdxDatabase sdxDatabase = ThreadBasedUserCrnProvider.doAs(ACTOR, () -> underTest.configure(environmentResponse, null, null, dbRequest, sdxCluster));

        assertFalse(sdxDatabase.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.NONE, sdxDatabase.getDatabaseAvailabilityType());
        assertEquals("11", sdxDatabase.getDatabaseEngineVersion());
        verifyNoInteractions(azureDatabaseAttributesService);
    }

    @Test
    public void whenPlatformIsAzureWithoutRuntimeVerionSetAndNoDbRequested() {
        CloudPlatform cloudPlatform = CloudPlatform.AZURE;
        environmentResponse.setCloudPlatform(cloudPlatform.name());
        SdxDatabaseRequest dbRequest = new SdxDatabaseRequest();
        dbRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NONE);
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("clusterName");

        SdxDatabase sdxDatabase = ThreadBasedUserCrnProvider.doAs(ACTOR, () -> underTest.configure(environmentResponse, null, null, dbRequest, sdxCluster));

        assertFalse(sdxDatabase.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.NONE, sdxDatabase.getDatabaseAvailabilityType());
        assertEquals("11", sdxDatabase.getDatabaseEngineVersion());
    }

    @Test
    public void whenPlatformIsAzureWithNotSupportedRuntime() {
        CloudPlatform cloudPlatform = CloudPlatform.AZURE;
        environmentResponse.setCloudPlatform(cloudPlatform.name());
        when(platformConfig.isExternalDatabaseSupportedFor(cloudPlatform)).thenReturn(true);
        SdxDatabaseRequest dbRequest = new SdxDatabaseRequest();
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("clusterName");
        sdxCluster.setRuntime("7.0.2");

        SdxDatabase sdxDatabase = ThreadBasedUserCrnProvider.doAs(ACTOR, () -> underTest.configure(environmentResponse, null, null, dbRequest, sdxCluster));

        assertFalse(sdxDatabase.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.NONE, sdxDatabase.getDatabaseAvailabilityType());
        assertEquals("11", sdxDatabase.getDatabaseEngineVersion());
    }

    @Test
    public void whenPlatformIsAzureWithMinSupportedVersion() {
        CloudPlatform cloudPlatform = CloudPlatform.AZURE;
        environmentResponse.setCloudPlatform(cloudPlatform.name());
        when(platformConfig.isExternalDatabaseSupportedFor(cloudPlatform)).thenReturn(true);
        when(platformConfig.isExternalDatabaseSupportedOrExperimental(CloudPlatform.AZURE)).thenReturn(true);
        SdxDatabaseRequest dbRequest = new SdxDatabaseRequest();
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("clusterName");
        sdxCluster.setRuntime("7.1.0");

        SdxDatabase sdxDatabase = ThreadBasedUserCrnProvider.doAs(ACTOR, () -> underTest.configure(environmentResponse, null, null, dbRequest, sdxCluster));

        assertTrue(sdxDatabase.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.HA, sdxDatabase.getDatabaseAvailabilityType());
        assertEquals("11", sdxDatabase.getDatabaseEngineVersion());
    }

    @Test
    public void whenPlatformIsAzureWithNewerSupportedVersion() {
        CloudPlatform cloudPlatform = CloudPlatform.AZURE;
        environmentResponse.setCloudPlatform(cloudPlatform.name());
        when(platformConfig.isExternalDatabaseSupportedFor(cloudPlatform)).thenReturn(true);
        when(platformConfig.isExternalDatabaseSupportedOrExperimental(CloudPlatform.AZURE)).thenReturn(true);
        SdxDatabaseRequest dbRequest = new SdxDatabaseRequest();
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("clusterName");
        sdxCluster.setRuntime("7.2.0");

        SdxDatabase sdxDatabase = ThreadBasedUserCrnProvider.doAs(ACTOR, () -> underTest.configure(environmentResponse, null, null, dbRequest, sdxCluster));

        assertTrue(sdxDatabase.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.HA, sdxDatabase.getDatabaseAvailabilityType());
        assertEquals("11", sdxDatabase.getDatabaseEngineVersion());
    }

    @Test
    public void testFlexibleEnabledButSingleRequestedInternal() {
        CloudPlatform cloudPlatform = CloudPlatform.AZURE;
        environmentResponse.setCloudPlatform(cloudPlatform.name());
        when(platformConfig.isExternalDatabaseSupportedFor(cloudPlatform)).thenReturn(true);
        when(platformConfig.isExternalDatabaseSupportedOrExperimental(CloudPlatform.AZURE)).thenReturn(true);
        SdxDatabaseRequest dbRequest = new SdxDatabaseRequest();
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("clusterName");
        sdxCluster.setRuntime("7.2.0");
        DatabaseRequest internalDatabaseRequest = new DatabaseRequest();
        DatabaseAzureRequest databaseAzureRequest = new DatabaseAzureRequest();
        databaseAzureRequest.setAzureDatabaseType(AzureDatabaseType.SINGLE_SERVER);
        when(azureDatabaseAttributesService.determineAzureDatabaseType(internalDatabaseRequest, null)).thenReturn(AzureDatabaseType.SINGLE_SERVER);
        internalDatabaseRequest.setDatabaseAzureRequest(databaseAzureRequest);

        SdxDatabase sdxDatabase = ThreadBasedUserCrnProvider.doAs(ACTOR,
                () -> underTest.configure(environmentResponse, "sles", internalDatabaseRequest, null, sdxCluster));

        assertTrue(sdxDatabase.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.HA, sdxDatabase.getDatabaseAvailabilityType());
        assertEquals("11", sdxDatabase.getDatabaseEngineVersion());
        verify(databaseDefaultVersionProvider).calculateDbVersionBasedOnRuntime("7.2.0", null);
    }

    @Test
    public void testFlexibleEnabledFlexibleRequestedInternal() {
        CloudPlatform cloudPlatform = CloudPlatform.AZURE;
        environmentResponse.setCloudPlatform(cloudPlatform.name());
        when(platformConfig.isExternalDatabaseSupportedFor(cloudPlatform)).thenReturn(true);
        when(platformConfig.isExternalDatabaseSupportedOrExperimental(CloudPlatform.AZURE)).thenReturn(true);
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("clusterName");
        sdxCluster.setRuntime("7.2.0");
        DatabaseRequest internalDatabaseRequest = new DatabaseRequest();
        DatabaseAzureRequest databaseAzureRequest = new DatabaseAzureRequest();
        databaseAzureRequest.setAzureDatabaseType(AzureDatabaseType.FLEXIBLE_SERVER);
        internalDatabaseRequest.setDatabaseAzureRequest(databaseAzureRequest);
        when(azureDatabaseAttributesService.determineAzureDatabaseType(internalDatabaseRequest, null)).thenReturn(AzureDatabaseType.FLEXIBLE_SERVER);

        SdxDatabase sdxDatabase = ThreadBasedUserCrnProvider.doAs(ACTOR,
                () -> underTest.configure(environmentResponse, "sles", internalDatabaseRequest, null, sdxCluster));

        assertTrue(sdxDatabase.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.HA, sdxDatabase.getDatabaseAvailabilityType());
        assertEquals("11", sdxDatabase.getDatabaseEngineVersion());
        verify(databaseDefaultVersionProvider).calculateDbVersionBasedOnRuntime("7.2.0", null);
    }

    @Test
    public void testFlexibleEnabledButSingleRequestedExternal() {
        CloudPlatform cloudPlatform = CloudPlatform.AZURE;
        environmentResponse.setCloudPlatform(cloudPlatform.name());
        when(platformConfig.isExternalDatabaseSupportedFor(cloudPlatform)).thenReturn(true);
        when(platformConfig.isExternalDatabaseSupportedOrExperimental(CloudPlatform.AZURE)).thenReturn(true);
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("clusterName");
        sdxCluster.setRuntime("7.2.0");
        SdxDatabaseRequest databaseRequest = new SdxDatabaseRequest();
        SdxDatabaseAzureRequest sdxDatabaseAzureRequest = new SdxDatabaseAzureRequest();
        sdxDatabaseAzureRequest.setAzureDatabaseType(AzureDatabaseType.SINGLE_SERVER);
        databaseRequest.setSdxDatabaseAzureRequest(sdxDatabaseAzureRequest);
        when(azureDatabaseAttributesService.determineAzureDatabaseType(null, databaseRequest)).thenReturn(AzureDatabaseType.SINGLE_SERVER);

        SdxDatabase sdxDatabase = ThreadBasedUserCrnProvider.doAs(ACTOR,
                () -> underTest.configure(environmentResponse, "sles", null, databaseRequest, sdxCluster));

        assertTrue(sdxDatabase.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.HA, sdxDatabase.getDatabaseAvailabilityType());
        assertEquals("11", sdxDatabase.getDatabaseEngineVersion());
        verify(databaseDefaultVersionProvider).calculateDbVersionBasedOnRuntime("7.2.0", null);
    }

    @Test
    public void testFlexibleEnabledFlexibleRequestedExternal() {
        CloudPlatform cloudPlatform = CloudPlatform.AZURE;
        environmentResponse.setCloudPlatform(cloudPlatform.name());

        when(platformConfig.isExternalDatabaseSupportedFor(cloudPlatform)).thenReturn(true);
        when(platformConfig.isExternalDatabaseSupportedOrExperimental(CloudPlatform.AZURE)).thenReturn(true);
        SdxDatabaseRequest dbRequest = new SdxDatabaseRequest();
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("clusterName");
        sdxCluster.setRuntime("7.2.0");
        SdxDatabaseRequest databaseRequest = new SdxDatabaseRequest();
        SdxDatabaseAzureRequest sdxDatabaseAzureRequest = new SdxDatabaseAzureRequest();
        sdxDatabaseAzureRequest.setAzureDatabaseType(AzureDatabaseType.FLEXIBLE_SERVER);
        databaseRequest.setSdxDatabaseAzureRequest(sdxDatabaseAzureRequest);
        when(azureDatabaseAttributesService.determineAzureDatabaseType(null, databaseRequest)).thenReturn(AzureDatabaseType.FLEXIBLE_SERVER);

        SdxDatabase sdxDatabase = ThreadBasedUserCrnProvider.doAs(ACTOR,
                () -> underTest.configure(environmentResponse, "sles", null, databaseRequest, sdxCluster));

        assertTrue(sdxDatabase.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.HA, sdxDatabase.getDatabaseAvailabilityType());
        assertEquals("11", sdxDatabase.getDatabaseEngineVersion());
        verify(databaseDefaultVersionProvider).calculateDbVersionBasedOnRuntime("7.2.0", null);
    }

    @Test
    public void testFlexibleEnabledNoDbRequest() {
        CloudPlatform cloudPlatform = CloudPlatform.AZURE;
        environmentResponse.setCloudPlatform(cloudPlatform.name());
        when(platformConfig.isExternalDatabaseSupportedFor(cloudPlatform)).thenReturn(true);
        when(platformConfig.isExternalDatabaseSupportedOrExperimental(CloudPlatform.AZURE)).thenReturn(true);
        SdxDatabaseRequest dbRequest = new SdxDatabaseRequest();
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("clusterName");
        sdxCluster.setRuntime("7.2.0");

        SdxDatabase sdxDatabase = ThreadBasedUserCrnProvider.doAs(ACTOR, () -> underTest.configure(environmentResponse, "sles", null, null, sdxCluster));

        assertTrue(sdxDatabase.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.HA, sdxDatabase.getDatabaseAvailabilityType());
        assertEquals("11", sdxDatabase.getDatabaseEngineVersion());
        verify(databaseDefaultVersionProvider).calculateDbVersionBasedOnRuntime("7.2.0", null);
    }

    @Test
    public void whenPlatformIsYarnShouldNotAllowDatabase() {
        CloudPlatform cloudPlatform = CloudPlatform.YARN;
        environmentResponse.setCloudPlatform(cloudPlatform.name());
        when(platformConfig.isExternalDatabaseSupportedOrExperimental(cloudPlatform)).thenReturn(false);
        SdxDatabaseRequest dbRequest = new SdxDatabaseRequest();
        dbRequest.setAvailabilityType(SdxDatabaseAvailabilityType.HA);
        SdxCluster sdxCluster = new SdxCluster();

        assertThrows(BadRequestException.class, () -> ThreadBasedUserCrnProvider.doAs(ACTOR,
                () -> underTest.configure(environmentResponse, null, null, dbRequest, sdxCluster)));
    }

    @Test
    public void whenPlatformIsAwsWithInternalRequestAndRuntimeVersionIsPresentAndAvailabilityTypeIsNonHa() {
        CloudPlatform cloudPlatform = CloudPlatform.AWS;
        environmentResponse.setCloudPlatform(cloudPlatform.name());
        DatabaseRequest databaseRequest = new DatabaseRequest();
        String databaseEngineVersion = "11";
        databaseRequest.setDatabaseEngineVersion(databaseEngineVersion);
        databaseRequest.setAvailabilityType(DatabaseAvailabilityType.NON_HA);
        SdxCluster sdxCluster = new SdxCluster();
        when(platformConfig.isExternalDatabaseSupportedOrExperimental(cloudPlatform)).thenReturn(true);

        SdxDatabase sdxDatabase = ThreadBasedUserCrnProvider.doAs(ACTOR,
                () -> underTest.configure(environmentResponse, null, databaseRequest, null, sdxCluster));

        assertTrue(sdxDatabase.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.NON_HA, sdxDatabase.getDatabaseAvailabilityType());
        assertEquals(databaseEngineVersion, sdxDatabase.getDatabaseEngineVersion());
    }

    @Test
    public void whenPlatformIsAwsWithInternalRequestAndRuntimeVersionIsPresentAndAvailabilityTypeIsHa() {
        CloudPlatform cloudPlatform = CloudPlatform.AWS;
        environmentResponse.setCloudPlatform(cloudPlatform.name());
        DatabaseRequest databaseRequest = new DatabaseRequest();
        String databaseEngineVersion = "11";
        databaseRequest.setDatabaseEngineVersion(databaseEngineVersion);
        databaseRequest.setAvailabilityType(DatabaseAvailabilityType.HA);
        SdxCluster sdxCluster = new SdxCluster();
        when(platformConfig.isExternalDatabaseSupportedOrExperimental(cloudPlatform)).thenReturn(true);

        SdxDatabase sdxDatabase = ThreadBasedUserCrnProvider.doAs(ACTOR,
                () -> underTest.configure(environmentResponse, null, databaseRequest, null, sdxCluster));

        assertTrue(sdxDatabase.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.HA, sdxDatabase.getDatabaseAvailabilityType());
        assertEquals(databaseEngineVersion, sdxDatabase.getDatabaseEngineVersion());
    }

    @Test
    public void whenPlatformIsAwsWithInternalRequestAndRuntimeVersionIsNotPresentAndAvailabilityTypeIsHa() {
        CloudPlatform cloudPlatform = CloudPlatform.AWS;
        environmentResponse.setCloudPlatform(cloudPlatform.name());
        DatabaseRequest databaseRequest = new DatabaseRequest();
        databaseRequest.setAvailabilityType(DatabaseAvailabilityType.HA);
        SdxCluster sdxCluster = new SdxCluster();
        when(platformConfig.isExternalDatabaseSupportedOrExperimental(cloudPlatform)).thenReturn(true);
        when(databaseDefaultVersionProvider.calculateDbVersionBasedOnRuntime(any(), any()))
                .thenReturn(null);

        SdxDatabase sdxDatabase = ThreadBasedUserCrnProvider.doAs(ACTOR,
                () -> underTest.configure(environmentResponse, null, databaseRequest, null, sdxCluster));

        assertTrue(sdxDatabase.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.HA, sdxDatabase.getDatabaseAvailabilityType());
        assertNull(sdxDatabase.getDatabaseEngineVersion());
    }

    @Test
    public void whenPlatformIsAwsWithInternalRequestAndRuntimeVersionIsPresentAndAvailabilityTypeIsNull() {
        CloudPlatform cloudPlatform = CloudPlatform.AWS;
        environmentResponse.setCloudPlatform(cloudPlatform.name());
        DatabaseRequest databaseRequest = new DatabaseRequest();
        String databaseEngineVersion = "11";
        databaseRequest.setDatabaseEngineVersion(databaseEngineVersion);
        SdxCluster sdxCluster = new SdxCluster();

        SdxDatabase sdxDatabase = ThreadBasedUserCrnProvider.doAs(ACTOR,
                () -> underTest.configure(environmentResponse, null, databaseRequest, null, sdxCluster));

        assertFalse(sdxDatabase.isCreateDatabase());
        assertEquals(SdxDatabaseAvailabilityType.NONE, sdxDatabase.getDatabaseAvailabilityType());
        assertEquals(databaseEngineVersion, sdxDatabase.getDatabaseEngineVersion());
    }
}