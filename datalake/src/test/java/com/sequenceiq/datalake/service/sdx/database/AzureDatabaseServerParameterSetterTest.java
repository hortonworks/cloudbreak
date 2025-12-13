package com.sequenceiq.datalake.service.sdx.database;

import static com.sequenceiq.common.model.AzureHighAvailabiltyMode.SAME_ZONE;
import static com.sequenceiq.common.model.AzureHighAvailabiltyMode.ZONE_REDUNDANT;
import static com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType.HA;
import static com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType.NON_HA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
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
    private EntitlementService entitlementService;

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
        PlatformDatabaseCapabilitiesResponse platformDatabaseCapabilitiesResponse =
                new PlatformDatabaseCapabilitiesResponse(includedRegions, new HashMap<>());

        when(azureDatabaseAttributesService.getAzureDatabaseType(any())).thenReturn(AzureDatabaseType.FLEXIBLE_SERVER);
        when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(any(), any(), any(), any(), any(), any()))
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
        PlatformDatabaseCapabilitiesResponse platformDatabaseCapabilitiesResponse =
                new PlatformDatabaseCapabilitiesResponse(includedRegions, new HashMap<>());

        when(azureDatabaseAttributesService.getAzureDatabaseType(any())).thenReturn(AzureDatabaseType.FLEXIBLE_SERVER);
        when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(any(), any(), any(), any(), any(), any()))
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
                assertThrows(IllegalArgumentException.class,
                        () -> underTest.setParameters(request, createSdxCluster(SdxDatabaseAvailabilityType.NONE, null), null, "crn"));

        assertEquals("NONE database availability type is not supported on Azure.", result.getMessage());
    }

    @Test
    public void testAzureValidateWhenLocalAndMultiAzAndAzureParametersNotNullThenValidationShouldNotHappen() {
        DatabaseServerV4StackRequest databaseServerV4StackRequest = mock(DatabaseServerV4StackRequest.class);
        SdxCluster sdxCluster = mock(SdxCluster.class);
        DetailedEnvironmentResponse environmentResponse = mock(DetailedEnvironmentResponse.class);
        AzureDatabaseServerV4Parameters azureDatabaseServerV4Parameters = mock(AzureDatabaseServerV4Parameters.class);

        when(databaseServerV4StackRequest.getAzure()).thenReturn(azureDatabaseServerV4Parameters);
        when(sdxCluster.isEnableMultiAz()).thenReturn(true);
        when(environmentResponse.getAccountId()).thenReturn("accountId");
        when(entitlementService.localDevelopment(anyString())).thenReturn(true);

        underTest.validate(databaseServerV4StackRequest, sdxCluster, environmentResponse, "initiatorUserCrn");
    }

    @Test
    public void testAzureValidateWhenNotLocalAndNotMultiAzAndAzureParametersNotNullThenValidationShouldNotHappen() {
        DatabaseServerV4StackRequest databaseServerV4StackRequest = mock(DatabaseServerV4StackRequest.class);
        SdxCluster sdxCluster = mock(SdxCluster.class);
        DetailedEnvironmentResponse environmentResponse = mock(DetailedEnvironmentResponse.class);
        AzureDatabaseServerV4Parameters azureDatabaseServerV4Parameters = mock(AzureDatabaseServerV4Parameters.class);

        when(databaseServerV4StackRequest.getAzure()).thenReturn(azureDatabaseServerV4Parameters);
        when(sdxCluster.isEnableMultiAz()).thenReturn(false);
        when(environmentResponse.getAccountId()).thenReturn("accountId");
        when(entitlementService.localDevelopment(anyString())).thenReturn(false);

        underTest.validate(databaseServerV4StackRequest, sdxCluster, environmentResponse, "initiatorUserCrn");
    }

    @Test
    public void testValidateWhenValidationShouldHappenAndUsingMultiAzAndEmbeddedDatabaseShouldThrowBadRequestException() {
        DatabaseServerV4StackRequest databaseServerV4StackRequest = mock(DatabaseServerV4StackRequest.class);
        SdxCluster sdxCluster = mock(SdxCluster.class);
        DetailedEnvironmentResponse environmentResponse = mock(DetailedEnvironmentResponse.class);
        AzureDatabaseServerV4Parameters azureDatabaseServerV4Parameters = mock(AzureDatabaseServerV4Parameters.class);
        SdxDatabase sdxDatabase = mock(SdxDatabase.class);

        when(databaseServerV4StackRequest.getAzure()).thenReturn(azureDatabaseServerV4Parameters);
        when(sdxCluster.isEnableMultiAz()).thenReturn(true);
        when(sdxDatabase.hasExternalDatabase()).thenReturn(false);
        when(sdxCluster.getSdxDatabase()).thenReturn(sdxDatabase);
        when(environmentResponse.getAccountId()).thenReturn("accountId");
        when(entitlementService.localDevelopment(anyString())).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                underTest.validate(databaseServerV4StackRequest, sdxCluster, environmentResponse, "initiatorUserCrn"));

        assertEquals(exception.getMessage(), "Azure Data Lake requested in multi availability zone setup must use external database.");
    }

    @Test
    public void testValidateWhenValidationShouldHappenAndUsingNonHaShouldThrowBadRequestException() {
        DatabaseServerV4StackRequest databaseServerV4StackRequest = mock(DatabaseServerV4StackRequest.class);
        SdxCluster sdxCluster = mock(SdxCluster.class);
        DetailedEnvironmentResponse environmentResponse = mock(DetailedEnvironmentResponse.class);
        AzureDatabaseServerV4Parameters azureDatabaseServerV4Parameters = mock(AzureDatabaseServerV4Parameters.class);
        SdxDatabase sdxDatabase = mock(SdxDatabase.class);

        when(databaseServerV4StackRequest.getAzure()).thenReturn(azureDatabaseServerV4Parameters);
        when(sdxCluster.isEnableMultiAz()).thenReturn(true);
        when(sdxDatabase.hasExternalDatabase()).thenReturn(true);
        when(sdxDatabase.getDatabaseAvailabilityType()).thenReturn(NON_HA);
        when(sdxCluster.getSdxDatabase()).thenReturn(sdxDatabase);
        when(environmentResponse.getAccountId()).thenReturn("accountId");
        when(entitlementService.localDevelopment(anyString())).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                underTest.validate(databaseServerV4StackRequest, sdxCluster, environmentResponse, "initiatorUserCrn"));

        assertEquals(exception.getMessage(), "Non HA Database is not supported for Azure multi availability zone Data Hubs.");
    }

    @Test
    public void testValidateWhenValidationShouldHappenAndUsingMultiAzAndNonFlexibleServerShouldThrowBadRequestException() {
        DatabaseServerV4StackRequest databaseServerV4StackRequest = mock(DatabaseServerV4StackRequest.class);
        SdxCluster sdxCluster = mock(SdxCluster.class);
        DetailedEnvironmentResponse environmentResponse = mock(DetailedEnvironmentResponse.class);
        AzureDatabaseServerV4Parameters azureDatabaseServerV4Parameters = mock(AzureDatabaseServerV4Parameters.class);
        SdxDatabase sdxDatabase = mock(SdxDatabase.class);

        when(azureDatabaseServerV4Parameters.getAzureDatabaseType()).thenReturn(AzureDatabaseType.SINGLE_SERVER);
        when(databaseServerV4StackRequest.getAzure()).thenReturn(azureDatabaseServerV4Parameters);
        when(sdxCluster.isEnableMultiAz()).thenReturn(true);
        when(sdxDatabase.hasExternalDatabase()).thenReturn(true);
        when(sdxCluster.getSdxDatabase()).thenReturn(sdxDatabase);
        when(environmentResponse.getAccountId()).thenReturn("accountId");
        when(entitlementService.localDevelopment(anyString())).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                underTest.validate(databaseServerV4StackRequest, sdxCluster, environmentResponse, "initiatorUserCrn"));

        assertEquals(exception.getMessage(), "Azure Data Lake requested in multi availability zone setup must use Flexible server.");
    }

    @Test
    public void testValidateWhenValidationShouldHappenAndUsingMultiAzAndNonZoneRedundantServerShouldThrowBadRequestException() {
        DatabaseServerV4StackRequest databaseServerV4StackRequest = mock(DatabaseServerV4StackRequest.class);
        SdxCluster sdxCluster = mock(SdxCluster.class);
        LocationResponse locationResponse = mock(LocationResponse.class);
        DetailedEnvironmentResponse environmentResponse = mock(DetailedEnvironmentResponse.class);
        AzureDatabaseServerV4Parameters azureDatabaseServerV4Parameters = mock(AzureDatabaseServerV4Parameters.class);
        SdxDatabase sdxDatabase = mock(SdxDatabase.class);

        when(azureDatabaseServerV4Parameters.getAzureDatabaseType()).thenReturn(AzureDatabaseType.FLEXIBLE_SERVER);
        when(locationResponse.getName()).thenReturn("eu-west-1");
        when(environmentResponse.getLocation()).thenReturn(locationResponse);
        when(azureDatabaseServerV4Parameters.getHighAvailabilityMode()).thenReturn(SAME_ZONE);
        when(databaseServerV4StackRequest.getAzure()).thenReturn(azureDatabaseServerV4Parameters);
        when(sdxDatabase.hasExternalDatabase()).thenReturn(true);
        when(sdxCluster.isEnableMultiAz()).thenReturn(true);
        when(sdxCluster.getSdxDatabase()).thenReturn(sdxDatabase);
        when(environmentResponse.getAccountId()).thenReturn("accountId");
        when(entitlementService.localDevelopment(anyString())).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                underTest.validate(databaseServerV4StackRequest, sdxCluster, environmentResponse, "initiatorUserCrn"));

        assertEquals(exception.getMessage(), "Azure Data Lake requested in multi availability zone setup must use Zone redundant " +
                "Flexible server and the eu-west-1 region currently does not support that. " +
                "You can see the limitations on the following url https://learn.microsoft.com/en-us/azure/postgresql/flexible-server/overview. " +
                "Please contact Microsoft support that you need Zone Redundant Flexible Server option in the given region.");
    }

    @Test
    public void testValidateWhenValidationShouldHappenAndUsingMultiAzAndEverythingLooksOk() {
        DatabaseServerV4StackRequest databaseServerV4StackRequest = mock(DatabaseServerV4StackRequest.class);
        SdxCluster sdxCluster = mock(SdxCluster.class);
        DetailedEnvironmentResponse environmentResponse = mock(DetailedEnvironmentResponse.class);
        AzureDatabaseServerV4Parameters azureDatabaseServerV4Parameters = mock(AzureDatabaseServerV4Parameters.class);
        SdxDatabase sdxDatabase = mock(SdxDatabase.class);

        when(azureDatabaseServerV4Parameters.getAzureDatabaseType()).thenReturn(AzureDatabaseType.FLEXIBLE_SERVER);
        when(azureDatabaseServerV4Parameters.getHighAvailabilityMode()).thenReturn(ZONE_REDUNDANT);
        when(databaseServerV4StackRequest.getAzure()).thenReturn(azureDatabaseServerV4Parameters);
        when(sdxDatabase.hasExternalDatabase()).thenReturn(true);
        when(sdxCluster.isEnableMultiAz()).thenReturn(true);
        when(sdxCluster.getSdxDatabase()).thenReturn(sdxDatabase);
        when(environmentResponse.getAccountId()).thenReturn("accountId");
        when(entitlementService.localDevelopment(anyString())).thenReturn(false);

        underTest.validate(databaseServerV4StackRequest, sdxCluster, environmentResponse, "initiatorUserCrn");
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
