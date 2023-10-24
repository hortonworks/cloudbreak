package com.sequenceiq.datalake.service.sdx.database;

import static com.sequenceiq.common.model.AzureHighAvailabiltyMode.SAME_ZONE;
import static com.sequenceiq.common.model.AzureHighAvailabiltyMode.ZONE_REDUNDANT;
import static com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType.HA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.common.model.AzureHighAvailabiltyMode;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.LocationResponse;
import com.sequenceiq.environment.api.v1.platformresource.EnvironmentPlatformResourceEndpoint;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformDatabaseCapabilitiesResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.azure.AzureDatabaseServerV4Parameters;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;

@ExtendWith(MockitoExtension.class)
public class AzureDatabaseServerParameterSetterTest {

    @Mock
    private DatabaseServerV4StackRequest request;

    @Mock
    private AzureDatabaseAttributesService azureDatabaseAttributesService;

    @Mock
    private EnvironmentPlatformResourceEndpoint environmentPlatformResourceEndpoint;

    @InjectMocks
    private AzureDatabaseServerParameterSetter underTest;

    @Captor
    private ArgumentCaptor<AzureDatabaseServerV4Parameters> captor;

    @BeforeEach
    public void setUp() {
        underTest.geoRedundantBackupHa = true;
        underTest.geoRedundantBackupNonHa = false;
        underTest.backupRetentionPeriodHa = 30;
        underTest.backupRetentionPeriodNonHa = 7;
    }

    @Test
    public void testHAServer() {
        underTest.setParameters(request, createSdxCluster(HA, null), null, "crn");

        verify(request).setAzure(captor.capture());
        AzureDatabaseServerV4Parameters azureDatabaseServerV4Parameters = captor.getValue();
        assertEquals(true, azureDatabaseServerV4Parameters.getGeoRedundantBackup());
        assertEquals(30, azureDatabaseServerV4Parameters.getBackupRetentionDays());
        assertEquals(AzureHighAvailabiltyMode.SAME_ZONE, azureDatabaseServerV4Parameters.getHighAvailabilityMode());
    }

    @Test
    public void testFlexibleWithSameZoneServerBecauseZoneRedundantNotSupportedThenShouldReturnSameZoneMustNotContainStandByZone() {
        DatabaseServerV4StackRequest databaseServerV4StackRequest = new DatabaseServerV4StackRequest();
        SdxCluster sdxCluster = createSdxCluster(HA, "11");
        sdxCluster.setEnableMultiAz(true);

        Map<String, List<String>> includedRegions = new HashMap<>();
        includedRegions.put(SAME_ZONE.name(), List.of("region1"));
        PlatformDatabaseCapabilitiesResponse platformDatabaseCapabilitiesResponse = new PlatformDatabaseCapabilitiesResponse(includedRegions);

        when(azureDatabaseAttributesService.getAzureDatabaseType(any())).thenReturn(AzureDatabaseType.FLEXIBLE_SERVER);
        when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(any(), any(), any(), any()))
                .thenReturn(platformDatabaseCapabilitiesResponse);

        underTest.setParameters(databaseServerV4StackRequest, sdxCluster, detailedEnvironmentResponse(), "crn");

        AzureDatabaseServerV4Parameters azure = databaseServerV4StackRequest.getAzure();
        assertEquals(true, azure.getGeoRedundantBackup());
        assertEquals(30, azure.getBackupRetentionDays());
        assertEquals(AzureHighAvailabiltyMode.SAME_ZONE, azure.getHighAvailabilityMode());
        assertEquals(AzureDatabaseType.FLEXIBLE_SERVER, azure.getAzureDatabaseType());
        assertTrue(Set.of("3", "2", "1").contains(azure.getAvailabilityZone()));
        assertNull(azure.getStandbyAvailabilityZone());
    }

    @Test
    public void testFlexibleWithZoneRedundantServerBecauseZoneRedundantSupportedMustContainStandByZone() {
        DatabaseServerV4StackRequest databaseServerV4StackRequest = new DatabaseServerV4StackRequest();
        SdxCluster sdxCluster = createSdxCluster(HA, "11");
        sdxCluster.setEnableMultiAz(true);

        Map<String, List<String>> includedRegions = new HashMap<>();
        includedRegions.put(ZONE_REDUNDANT.name(), List.of("region1"));
        PlatformDatabaseCapabilitiesResponse platformDatabaseCapabilitiesResponse = new PlatformDatabaseCapabilitiesResponse(includedRegions);

        when(azureDatabaseAttributesService.getAzureDatabaseType(any())).thenReturn(AzureDatabaseType.FLEXIBLE_SERVER);
        when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(any(), any(), any(), any()))
                .thenReturn(platformDatabaseCapabilitiesResponse);

        underTest.setParameters(databaseServerV4StackRequest, sdxCluster, detailedEnvironmentResponse(), "crn");

        AzureDatabaseServerV4Parameters azure = databaseServerV4StackRequest.getAzure();
        assertEquals(true, azure.getGeoRedundantBackup());
        assertEquals(30, azure.getBackupRetentionDays());
        assertEquals(ZONE_REDUNDANT, azure.getHighAvailabilityMode());
        assertEquals(AzureDatabaseType.FLEXIBLE_SERVER, azure.getAzureDatabaseType());
        assertTrue(Set.of("3", "2", "1").contains(azure.getAvailabilityZone()));
        assertTrue(Set.of("3", "2", "1").contains(azure.getStandbyAvailabilityZone()));
    }

    @Test
    public void testNonHAServer() {
        underTest.setParameters(request, createSdxCluster(SdxDatabaseAvailabilityType.NON_HA, null), null, "crn");

        verify(request).setAzure(captor.capture());
        AzureDatabaseServerV4Parameters azureDatabaseServerV4Parameters = captor.getValue();
        assertEquals(false, azureDatabaseServerV4Parameters.getGeoRedundantBackup());
        assertEquals(7, azureDatabaseServerV4Parameters.getBackupRetentionDays());
        assertEquals(AzureHighAvailabiltyMode.DISABLED, azureDatabaseServerV4Parameters.getHighAvailabilityMode());
    }

    @Test
    public void testEngineVersion() {
        underTest.setParameters(request, createSdxCluster(SdxDatabaseAvailabilityType.NON_HA, "13"), null, "crn");

        verify(request).setAzure(captor.capture());
        AzureDatabaseServerV4Parameters azureDatabaseServerV4Parameters = captor.getValue();
        assertEquals(false, azureDatabaseServerV4Parameters.getGeoRedundantBackup());
        assertEquals(7, azureDatabaseServerV4Parameters.getBackupRetentionDays());
        assertEquals("13", azureDatabaseServerV4Parameters.getDbVersion());
        assertEquals(AzureHighAvailabiltyMode.DISABLED, azureDatabaseServerV4Parameters.getHighAvailabilityMode());
    }

    @ParameterizedTest
    @EnumSource(AzureDatabaseType.class)
    public void testAzureDatabaseType(AzureDatabaseType azureDatabaseType) {
        when(azureDatabaseAttributesService.getAzureDatabaseType(any(SdxDatabase.class))).thenReturn(azureDatabaseType);
        underTest.setParameters(request, createSdxCluster(SdxDatabaseAvailabilityType.NON_HA, "13"), null, "crn");
        verify(request).setAzure(captor.capture());
        assertEquals(azureDatabaseType, captor.getValue().getAzureDatabaseType());
    }

    @Test
    public void shouldThrowExceptionWhenAvailabilityTypeIsNotSupported() {
        IllegalArgumentException result =
                Assertions.assertThrows(IllegalArgumentException.class,
                        () -> underTest.setParameters(request, createSdxCluster(SdxDatabaseAvailabilityType.NONE, null), null, "crn"));

        assertEquals("NONE database availability type is not supported on Azure.", result.getMessage());
    }

    private SdxCluster createSdxCluster(SdxDatabaseAvailabilityType sdxDatabaseAvailabilityType, String databaseEngineVersion) {
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setDatabaseAvailabilityType(sdxDatabaseAvailabilityType);
        sdxDatabase.setDatabaseEngineVersion(databaseEngineVersion);
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setSdxDatabase(sdxDatabase);
        return sdxCluster;
    }

    private DetailedEnvironmentResponse detailedEnvironmentResponse() {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setCreator("creator");
        LocationResponse locationResponse = new LocationResponse();
        locationResponse.setName("region1");
        detailedEnvironmentResponse.setLocation(locationResponse);

        EnvironmentNetworkResponse environmentNetworkResponse = new EnvironmentNetworkResponse();
        EnvironmentNetworkAzureParams azureParams = new EnvironmentNetworkAzureParams();
        azureParams.setAvailabilityZones(Set.of("1", "2", "3"));
        environmentNetworkResponse.setAzure(azureParams);
        detailedEnvironmentResponse.setNetwork(environmentNetworkResponse);
        return detailedEnvironmentResponse;
    }
}
