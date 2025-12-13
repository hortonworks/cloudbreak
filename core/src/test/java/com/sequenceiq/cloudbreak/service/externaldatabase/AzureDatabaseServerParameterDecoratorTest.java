package com.sequenceiq.cloudbreak.service.externaldatabase;

import static com.sequenceiq.common.model.AzureHighAvailabiltyMode.SAME_ZONE;
import static com.sequenceiq.common.model.AzureHighAvailabiltyMode.ZONE_REDUNDANT;
import static com.sequenceiq.common.model.DatabaseCapabilityType.AZURE_FLEXIBLE;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.service.externaldatabase.model.DatabaseServerParameter;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.common.model.AzureHighAvailabiltyMode;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams.EnvironmentNetworkAzureParamsBuilder;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.LocationResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.LocationResponse.LocationResponseBuilder;
import com.sequenceiq.environment.api.v1.platformresource.EnvironmentPlatformResourceEndpoint;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformDatabaseCapabilitiesResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.azure.AzureDatabaseServerV4Parameters;

@ExtendWith(MockitoExtension.class)
class AzureDatabaseServerParameterDecoratorTest {
    private static final String FIELD_RETENTION_PERIOD_HA = "retentionPeriodHa";

    private static final String FIELD_GEO_REDUNDANT_BACKUP_HA = "geoRedundantBackupHa";

    private static final String FIELD_RETENTION_PERIOD_NON_HA = "retentionPeriodNonHa";

    private static final String FIELD_GEO_REDUNDANT_BACKUP_NON_HA = "geoRedundantBackupNonHa";

    private static final int RETENTION_PERIOD_HA = 2;

    private static final int RETENTION_PERIOD_NON_HA = 1;

    private static final String ENGINE_VERSION = "11";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String LOCATION = "location";

    private static final String AZ_1 = "az1";

    private static final String AZ_2 = "az2";

    private static final String OTHER_REGION = "otherRegion";

    @Mock
    private EnvironmentPlatformResourceEndpoint environmentPlatformResourceEndpoint;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private AzureDatabaseServerParameterDecorator underTest;

    @Test
    void getCloudPlatformTest() {
        assertThat(underTest.getCloudPlatform()).isEqualTo(CloudPlatform.AZURE);
    }

    @Test
    void getDatabaseTypeTestWhenAttributeMissing() {
        assertThat(underTest.getDatabaseType(Map.of())).hasValue(AzureDatabaseType.SINGLE_SERVER);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(AzureDatabaseType.class)
    void getDatabaseTypeTestWhenAttributePresent(AzureDatabaseType azureDatabaseType) {
        Map<String, Object> attributes = Map.ofEntries(entry(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, azureDatabaseType.name()), entry("foo", "bar"));

        assertThat(underTest.getDatabaseType(attributes)).hasValue(azureDatabaseType);
    }

    @Test
    void getDatabaseTypeTestWhenAttributePresentWithInvalidValue() {
        Map<String, Object> attributes = Map.ofEntries(entry(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, "invalid"), entry("foo", "bar"));

        assertThat(underTest.getDatabaseType(attributes)).hasValue(AzureDatabaseType.SINGLE_SERVER);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = DatabaseAvailabilityType.class, mode = Mode.EXCLUDE, names = {"NON_HA", "HA"})
    void testSetParametersInvalidAvailabilityType(DatabaseAvailabilityType availabilityType) {
        DatabaseServerV4StackRequest databaseServerV4StackRequest = new DatabaseServerV4StackRequest();
        DatabaseServerParameter databaseServerParameter = DatabaseServerParameter.builder()
                .withAvailabilityType(availabilityType)
                .build();

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
                () -> underTest.setParameters(databaseServerV4StackRequest, databaseServerParameter, null, false));

        assertThat(illegalArgumentException).hasMessage(availabilityType + " database availability type is not supported on Azure.");

        verify(environmentPlatformResourceEndpoint, never()).getDatabaseCapabilities(any(), any(), any(), any(), any(), any());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(booleans = {false, true})
    void testSetParametersHaSingleServer(boolean multiAz) {
        ReflectionTestUtils.setField(underTest, FIELD_RETENTION_PERIOD_HA, RETENTION_PERIOD_HA);
        ReflectionTestUtils.setField(underTest, FIELD_GEO_REDUNDANT_BACKUP_HA, Boolean.TRUE);
        ReflectionTestUtils.setField(underTest, FIELD_RETENTION_PERIOD_NON_HA, RETENTION_PERIOD_NON_HA);
        ReflectionTestUtils.setField(underTest, FIELD_GEO_REDUNDANT_BACKUP_NON_HA, Boolean.FALSE);
        DatabaseServerV4StackRequest databaseServerV4StackRequest = new DatabaseServerV4StackRequest();
        DatabaseServerParameter databaseServerParameter = DatabaseServerParameter.builder()
                .withAvailabilityType(DatabaseAvailabilityType.HA)
                .withEngineVersion(ENGINE_VERSION)
                .withAttributes(Map.of(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, AzureDatabaseType.SINGLE_SERVER.name()))
                .build();

        underTest.setParameters(databaseServerV4StackRequest, databaseServerParameter, null, multiAz);

        AzureDatabaseServerV4Parameters azureDatabaseServerV4Parameters = databaseServerV4StackRequest.getAzure();
        assertEquals(RETENTION_PERIOD_HA, azureDatabaseServerV4Parameters.getBackupRetentionDays());
        assertTrue(azureDatabaseServerV4Parameters.getGeoRedundantBackup());
        assertEquals(ENGINE_VERSION, azureDatabaseServerV4Parameters.getDbVersion());
        assertEquals(AzureDatabaseType.SINGLE_SERVER, azureDatabaseServerV4Parameters.getAzureDatabaseType());
        assertEquals(AzureHighAvailabiltyMode.SAME_ZONE, azureDatabaseServerV4Parameters.getHighAvailabilityMode());
        assertThat(azureDatabaseServerV4Parameters.getAvailabilityZone()).isNull();
        assertThat(azureDatabaseServerV4Parameters.getStandbyAvailabilityZone()).isNull();

        verify(environmentPlatformResourceEndpoint, never()).getDatabaseCapabilities(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testSetParametersNonHaFlexibleServerNoMultiAz() {
        ReflectionTestUtils.setField(underTest, FIELD_RETENTION_PERIOD_HA, RETENTION_PERIOD_HA);
        ReflectionTestUtils.setField(underTest, FIELD_GEO_REDUNDANT_BACKUP_HA, Boolean.TRUE);
        ReflectionTestUtils.setField(underTest, FIELD_RETENTION_PERIOD_NON_HA, RETENTION_PERIOD_NON_HA);
        ReflectionTestUtils.setField(underTest, FIELD_GEO_REDUNDANT_BACKUP_NON_HA, Boolean.FALSE);
        DatabaseServerV4StackRequest databaseServerV4StackRequest = new DatabaseServerV4StackRequest();
        DatabaseServerParameter databaseServerParameter = DatabaseServerParameter.builder()
                .withAvailabilityType(DatabaseAvailabilityType.NON_HA)
                .withEngineVersion(ENGINE_VERSION)
                .withAttributes(Map.of(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, AzureDatabaseType.FLEXIBLE_SERVER.name()))
                .build();

        underTest.setParameters(databaseServerV4StackRequest, databaseServerParameter, null, false);

        AzureDatabaseServerV4Parameters azureDatabaseServerV4Parameters = databaseServerV4StackRequest.getAzure();
        assertEquals(RETENTION_PERIOD_NON_HA, azureDatabaseServerV4Parameters.getBackupRetentionDays());
        assertFalse(azureDatabaseServerV4Parameters.getGeoRedundantBackup());
        assertEquals(ENGINE_VERSION, azureDatabaseServerV4Parameters.getDbVersion());
        assertEquals(AzureDatabaseType.FLEXIBLE_SERVER, azureDatabaseServerV4Parameters.getAzureDatabaseType());
        assertEquals(AzureHighAvailabiltyMode.DISABLED, azureDatabaseServerV4Parameters.getHighAvailabilityMode());
        assertThat(azureDatabaseServerV4Parameters.getAvailabilityZone()).isNull();
        assertThat(azureDatabaseServerV4Parameters.getStandbyAvailabilityZone()).isNull();

        verify(environmentPlatformResourceEndpoint, never()).getDatabaseCapabilities(any(), any(), any(), any(), any(), any());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(booleans = {false, true})
    void testSetParametersNonHaNoAttributes(boolean multiAz) {
        ReflectionTestUtils.setField(underTest, FIELD_RETENTION_PERIOD_HA, RETENTION_PERIOD_HA);
        ReflectionTestUtils.setField(underTest, FIELD_GEO_REDUNDANT_BACKUP_HA, Boolean.TRUE);
        ReflectionTestUtils.setField(underTest, FIELD_RETENTION_PERIOD_NON_HA, RETENTION_PERIOD_NON_HA);
        ReflectionTestUtils.setField(underTest, FIELD_GEO_REDUNDANT_BACKUP_NON_HA, Boolean.FALSE);
        DatabaseServerV4StackRequest databaseServerV4StackRequest = new DatabaseServerV4StackRequest();
        DatabaseServerParameter databaseServerParameter = DatabaseServerParameter.builder()
                .withAvailabilityType(DatabaseAvailabilityType.NON_HA)
                .withEngineVersion(ENGINE_VERSION)
                .build();

        underTest.setParameters(databaseServerV4StackRequest, databaseServerParameter, null, multiAz);

        AzureDatabaseServerV4Parameters azureDatabaseServerV4Parameters = databaseServerV4StackRequest.getAzure();
        assertEquals(RETENTION_PERIOD_NON_HA, azureDatabaseServerV4Parameters.getBackupRetentionDays());
        assertFalse(azureDatabaseServerV4Parameters.getGeoRedundantBackup());
        assertEquals(ENGINE_VERSION, azureDatabaseServerV4Parameters.getDbVersion());
        assertEquals(AzureDatabaseType.SINGLE_SERVER, azureDatabaseServerV4Parameters.getAzureDatabaseType());
        assertEquals(AzureHighAvailabiltyMode.DISABLED, azureDatabaseServerV4Parameters.getHighAvailabilityMode());
        assertThat(azureDatabaseServerV4Parameters.getAvailabilityZone()).isNull();
        assertThat(azureDatabaseServerV4Parameters.getStandbyAvailabilityZone()).isNull();

        verify(environmentPlatformResourceEndpoint, never()).getDatabaseCapabilities(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testSetParametersNonHaFlexibleServerMultiAzEmptyCapabilities() {
        ReflectionTestUtils.setField(underTest, FIELD_RETENTION_PERIOD_HA, RETENTION_PERIOD_HA);
        ReflectionTestUtils.setField(underTest, FIELD_GEO_REDUNDANT_BACKUP_HA, Boolean.TRUE);
        ReflectionTestUtils.setField(underTest, FIELD_RETENTION_PERIOD_NON_HA, RETENTION_PERIOD_NON_HA);
        ReflectionTestUtils.setField(underTest, FIELD_GEO_REDUNDANT_BACKUP_NON_HA, Boolean.FALSE);
        DatabaseServerV4StackRequest databaseServerV4StackRequest = new DatabaseServerV4StackRequest();
        DatabaseServerParameter databaseServerParameter = DatabaseServerParameter.builder()
                .withAvailabilityType(DatabaseAvailabilityType.NON_HA)
                .withEngineVersion(ENGINE_VERSION)
                .withAttributes(Map.of(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, AzureDatabaseType.FLEXIBLE_SERVER.name()))
                .build();

        DetailedEnvironmentResponse env = createEnvironment();
        PlatformDatabaseCapabilitiesResponse databaseCapabilities = new PlatformDatabaseCapabilitiesResponse();
        when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(ENVIRONMENT_CRN, LOCATION, CloudPlatform.AZURE.name(), null, AZURE_FLEXIBLE,
                null))
                .thenReturn(databaseCapabilities);

        underTest.setParameters(databaseServerV4StackRequest, databaseServerParameter, env, true);

        AzureDatabaseServerV4Parameters azureDatabaseServerV4Parameters = databaseServerV4StackRequest.getAzure();
        assertEquals(RETENTION_PERIOD_NON_HA, azureDatabaseServerV4Parameters.getBackupRetentionDays());
        assertFalse(azureDatabaseServerV4Parameters.getGeoRedundantBackup());
        assertEquals(ENGINE_VERSION, azureDatabaseServerV4Parameters.getDbVersion());
        assertEquals(AzureDatabaseType.FLEXIBLE_SERVER, azureDatabaseServerV4Parameters.getAzureDatabaseType());
        assertEquals(AzureHighAvailabiltyMode.DISABLED, azureDatabaseServerV4Parameters.getHighAvailabilityMode());
        assertThat(azureDatabaseServerV4Parameters.getAvailabilityZone()).isEqualTo(AZ_1);
        assertThat(azureDatabaseServerV4Parameters.getStandbyAvailabilityZone()).isNull();
    }

    private DetailedEnvironmentResponse createEnvironment() {
        Set<String> availabilityZones = new LinkedHashSet<>();
        availabilityZones.add(AZ_1);
        availabilityZones.add(AZ_2);
        DetailedEnvironmentResponse env = DetailedEnvironmentResponse.builder()
                .withCrn(ENVIRONMENT_CRN)
                .withLocation(LocationResponseBuilder.aLocationResponse()
                        .withName(LOCATION)
                        .build())
                .withCloudPlatform(CloudPlatform.AZURE.name())
                .withNetwork(EnvironmentNetworkResponse.builder()
                        .withAzure(EnvironmentNetworkAzureParamsBuilder.anEnvironmentNetworkAzureParams()
                                .withAvailabilityZones(availabilityZones)
                                .build())
                        .build())
                .build();
        return env;
    }

    @Test
    void testSetParametersHaFlexibleServerMultiAzEmptyRegions() {
        PlatformDatabaseCapabilitiesResponse databaseCapabilities = new PlatformDatabaseCapabilitiesResponse();
        testSetParametersHaFlexibleServerMultiAzSameZoneInternal(databaseCapabilities);
    }

    private void testSetParametersHaFlexibleServerMultiAzSameZoneInternal(PlatformDatabaseCapabilitiesResponse databaseCapabilities) {
        ReflectionTestUtils.setField(underTest, FIELD_RETENTION_PERIOD_HA, RETENTION_PERIOD_HA);
        ReflectionTestUtils.setField(underTest, FIELD_GEO_REDUNDANT_BACKUP_HA, Boolean.TRUE);
        ReflectionTestUtils.setField(underTest, FIELD_RETENTION_PERIOD_NON_HA, RETENTION_PERIOD_NON_HA);
        ReflectionTestUtils.setField(underTest, FIELD_GEO_REDUNDANT_BACKUP_NON_HA, Boolean.FALSE);
        DatabaseServerV4StackRequest databaseServerV4StackRequest = new DatabaseServerV4StackRequest();
        DatabaseServerParameter databaseServerParameter = DatabaseServerParameter.builder()
                .withAvailabilityType(DatabaseAvailabilityType.HA)
                .withEngineVersion(ENGINE_VERSION)
                .withAttributes(Map.of(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, AzureDatabaseType.FLEXIBLE_SERVER.name()))
                .build();

        DetailedEnvironmentResponse env = createEnvironment();
        when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(ENVIRONMENT_CRN, LOCATION, CloudPlatform.AZURE.name(), null, AZURE_FLEXIBLE,
                null))
                .thenReturn(databaseCapabilities);

        underTest.setParameters(databaseServerV4StackRequest, databaseServerParameter, env, true);

        AzureDatabaseServerV4Parameters azureDatabaseServerV4Parameters = databaseServerV4StackRequest.getAzure();
        assertEquals(RETENTION_PERIOD_HA, azureDatabaseServerV4Parameters.getBackupRetentionDays());
        assertTrue(azureDatabaseServerV4Parameters.getGeoRedundantBackup());
        assertEquals(ENGINE_VERSION, azureDatabaseServerV4Parameters.getDbVersion());
        assertEquals(AzureDatabaseType.FLEXIBLE_SERVER, azureDatabaseServerV4Parameters.getAzureDatabaseType());
        assertEquals(AzureHighAvailabiltyMode.SAME_ZONE, azureDatabaseServerV4Parameters.getHighAvailabilityMode());
        assertThat(azureDatabaseServerV4Parameters.getAvailabilityZone()).isEqualTo(AZ_1);
        assertThat(azureDatabaseServerV4Parameters.getStandbyAvailabilityZone()).isNull();
    }

    @Test
    void testSetParametersHaFlexibleServerMultiAzNotSupportedRegion() {
        Map<String, List<String>> includedRegions = Map.ofEntries(entry(AzureHighAvailabiltyMode.ZONE_REDUNDANT.name(), List.of(OTHER_REGION)));
        PlatformDatabaseCapabilitiesResponse databaseCapabilities
                = new PlatformDatabaseCapabilitiesResponse(includedRegions, new HashMap<>());
        testSetParametersHaFlexibleServerMultiAzSameZoneInternal(databaseCapabilities);
    }

    @Test
    void testSetParametersHaFlexibleServerMultiAzZoneRedundant() {
        ReflectionTestUtils.setField(underTest, FIELD_RETENTION_PERIOD_HA, RETENTION_PERIOD_HA);
        ReflectionTestUtils.setField(underTest, FIELD_GEO_REDUNDANT_BACKUP_HA, Boolean.TRUE);
        ReflectionTestUtils.setField(underTest, FIELD_RETENTION_PERIOD_NON_HA, RETENTION_PERIOD_NON_HA);
        ReflectionTestUtils.setField(underTest, FIELD_GEO_REDUNDANT_BACKUP_NON_HA, Boolean.FALSE);
        DatabaseServerV4StackRequest databaseServerV4StackRequest = new DatabaseServerV4StackRequest();
        DatabaseServerParameter databaseServerParameter = DatabaseServerParameter.builder()
                .withAvailabilityType(DatabaseAvailabilityType.HA)
                .withEngineVersion(ENGINE_VERSION)
                .withAttributes(Map.of(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, AzureDatabaseType.FLEXIBLE_SERVER.name()))
                .build();

        DetailedEnvironmentResponse env = createEnvironment();
        Map<String, List<String>> includedRegions = Map.ofEntries(entry(AzureHighAvailabiltyMode.ZONE_REDUNDANT.name(), List.of(OTHER_REGION, LOCATION)));
        PlatformDatabaseCapabilitiesResponse databaseCapabilities = new PlatformDatabaseCapabilitiesResponse(includedRegions, new HashMap<>());
        when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(ENVIRONMENT_CRN, LOCATION, CloudPlatform.AZURE.name(), null, AZURE_FLEXIBLE,
                null))
                .thenReturn(databaseCapabilities);

        underTest.setParameters(databaseServerV4StackRequest, databaseServerParameter, env, true);

        AzureDatabaseServerV4Parameters azureDatabaseServerV4Parameters = databaseServerV4StackRequest.getAzure();
        assertEquals(RETENTION_PERIOD_HA, azureDatabaseServerV4Parameters.getBackupRetentionDays());
        assertTrue(azureDatabaseServerV4Parameters.getGeoRedundantBackup());
        assertEquals(ENGINE_VERSION, azureDatabaseServerV4Parameters.getDbVersion());
        assertEquals(AzureDatabaseType.FLEXIBLE_SERVER, azureDatabaseServerV4Parameters.getAzureDatabaseType());
        assertEquals(AzureHighAvailabiltyMode.ZONE_REDUNDANT, azureDatabaseServerV4Parameters.getHighAvailabilityMode());
        assertThat(azureDatabaseServerV4Parameters.getAvailabilityZone()).isEqualTo(AZ_1);
        assertThat(azureDatabaseServerV4Parameters.getStandbyAvailabilityZone()).isEqualTo(AZ_2);
    }

    @Test
    public void testAzureValidateWhenLocalAndMultiAzAndAzureParametersNotNullThenValidationShouldNotHappen() {
        DatabaseServerV4StackRequest databaseServerV4StackRequest = mock(DatabaseServerV4StackRequest.class);
        DatabaseServerParameter sdxCluster = mock(DatabaseServerParameter.class);
        DetailedEnvironmentResponse environmentResponse = mock(DetailedEnvironmentResponse.class);
        AzureDatabaseServerV4Parameters azureDatabaseServerV4Parameters = mock(AzureDatabaseServerV4Parameters.class);

        when(databaseServerV4StackRequest.getAzure()).thenReturn(azureDatabaseServerV4Parameters);
        when(environmentResponse.getAccountId()).thenReturn("accountId");
        when(entitlementService.localDevelopment(anyString())).thenReturn(true);

        underTest.validate(databaseServerV4StackRequest, sdxCluster, environmentResponse, true);
    }

    @Test
    public void testAzureValidateWhenNotLocalAndNotMultiAzAndAzureParametersNotNullThenValidationShouldNotHappen() {
        DatabaseServerV4StackRequest databaseServerV4StackRequest = mock(DatabaseServerV4StackRequest.class);
        DatabaseServerParameter databaseServerParameter = mock(DatabaseServerParameter.class);
        DetailedEnvironmentResponse environmentResponse = mock(DetailedEnvironmentResponse.class);
        AzureDatabaseServerV4Parameters azureDatabaseServerV4Parameters = mock(AzureDatabaseServerV4Parameters.class);

        when(databaseServerV4StackRequest.getAzure()).thenReturn(azureDatabaseServerV4Parameters);
        when(environmentResponse.getAccountId()).thenReturn("accountId");
        when(entitlementService.localDevelopment(anyString())).thenReturn(false);

        underTest.validate(databaseServerV4StackRequest, databaseServerParameter, environmentResponse, false);
    }

    @Test
    public void testValidateWhenValidationShouldHappenAndUsingMultiAzAndEmbeddedDatabaseShouldThrowBadRequestException() {
        DatabaseServerV4StackRequest databaseServerV4StackRequest = mock(DatabaseServerV4StackRequest.class);
        DatabaseServerParameter databaseServerParameter = mock(DatabaseServerParameter.class);
        DetailedEnvironmentResponse environmentResponse = mock(DetailedEnvironmentResponse.class);
        AzureDatabaseServerV4Parameters azureDatabaseServerV4Parameters = mock(AzureDatabaseServerV4Parameters.class);

        when(databaseServerV4StackRequest.getAzure()).thenReturn(azureDatabaseServerV4Parameters);
        when(databaseServerParameter.getAvailabilityType()).thenReturn(DatabaseAvailabilityType.NONE);
        when(environmentResponse.getAccountId()).thenReturn("accountId");
        when(entitlementService.localDevelopment(anyString())).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                underTest.validate(databaseServerV4StackRequest, databaseServerParameter, environmentResponse, true));

        assertEquals(exception.getMessage(),
                "Azure Data Hub which requested in multi availability zone option must use external database.");
    }

    @Test
    public void testValidateWhenValidationShouldHappenAndUsingNonHaShouldThrowBadRequestException() {
        DatabaseServerV4StackRequest databaseServerV4StackRequest = mock(DatabaseServerV4StackRequest.class);
        DatabaseServerParameter databaseServerParameter = mock(DatabaseServerParameter.class);
        DetailedEnvironmentResponse environmentResponse = mock(DetailedEnvironmentResponse.class);
        AzureDatabaseServerV4Parameters azureDatabaseServerV4Parameters = mock(AzureDatabaseServerV4Parameters.class);

        when(databaseServerV4StackRequest.getAzure()).thenReturn(azureDatabaseServerV4Parameters);
        when(databaseServerParameter.getAvailabilityType()).thenReturn(DatabaseAvailabilityType.NON_HA);
        when(environmentResponse.getAccountId()).thenReturn("accountId");
        when(entitlementService.localDevelopment(anyString())).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                underTest.validate(databaseServerV4StackRequest, databaseServerParameter, environmentResponse, true));

        assertEquals(exception.getMessage(),
                "Non HA Database is not supported for Azure multi availability zone Data Hubs.");
    }

    @Test
    public void testValidateWhenValidationShouldHappenAndUsingMultiAzAndNonFlexibleServerShouldThrowBadRequestException() {
        DatabaseServerV4StackRequest databaseServerV4StackRequest = mock(DatabaseServerV4StackRequest.class);
        DatabaseServerParameter databaseServerParameter = mock(DatabaseServerParameter.class);
        DetailedEnvironmentResponse environmentResponse = mock(DetailedEnvironmentResponse.class);
        AzureDatabaseServerV4Parameters azureDatabaseServerV4Parameters = mock(AzureDatabaseServerV4Parameters.class);

        when(azureDatabaseServerV4Parameters.getAzureDatabaseType()).thenReturn(AzureDatabaseType.SINGLE_SERVER);
        when(databaseServerV4StackRequest.getAzure()).thenReturn(azureDatabaseServerV4Parameters);
        when(databaseServerParameter.getAvailabilityType()).thenReturn(DatabaseAvailabilityType.HA);
        when(environmentResponse.getAccountId()).thenReturn("accountId");
        when(entitlementService.localDevelopment(anyString())).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                underTest.validate(databaseServerV4StackRequest, databaseServerParameter, environmentResponse, true));

        assertEquals(exception.getMessage(),
                "Azure Data Hub which requested in multi availability zone option must use Flexible server.");
    }

    @Test
    public void testValidateWhenValidationShouldHappenAndUsingMultiAzAndNonZoneRedundantServerShouldThrowBadRequestException() {
        DatabaseServerV4StackRequest databaseServerV4StackRequest = mock(DatabaseServerV4StackRequest.class);
        DatabaseServerParameter databaseServerParameter = mock(DatabaseServerParameter.class);
        LocationResponse locationResponse = mock(LocationResponse.class);
        DetailedEnvironmentResponse environmentResponse = mock(DetailedEnvironmentResponse.class);
        AzureDatabaseServerV4Parameters azureDatabaseServerV4Parameters = mock(AzureDatabaseServerV4Parameters.class);

        when(azureDatabaseServerV4Parameters.getAzureDatabaseType()).thenReturn(AzureDatabaseType.FLEXIBLE_SERVER);
        when(locationResponse.getName()).thenReturn("eu-west-1");
        when(environmentResponse.getLocation()).thenReturn(locationResponse);
        when(azureDatabaseServerV4Parameters.getHighAvailabilityMode()).thenReturn(SAME_ZONE);
        when(databaseServerV4StackRequest.getAzure()).thenReturn(azureDatabaseServerV4Parameters);
        when(databaseServerParameter.getAvailabilityType()).thenReturn(DatabaseAvailabilityType.HA);
        when(environmentResponse.getAccountId()).thenReturn("accountId");
        when(entitlementService.localDevelopment(anyString())).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                underTest.validate(databaseServerV4StackRequest, databaseServerParameter, environmentResponse, true));

        assertEquals(exception.getMessage(),
                "Azure Data Hub which requested with multi availability zone option must use Zone redundant " +
                        "Flexible server and the eu-west-1 region currently does not support that. " +
                        "You can see the limitations on the following url https://learn.microsoft.com/en-us/azure/postgresql/flexible-server/overview. " +
                        "Please contact Microsoft support that you need Zone redundant option in the given region.");
    }

    @Test
    public void testValidateWhenValidationShouldHappenAndUsingMultiAzAndEverythingLooksOk() {
        DatabaseServerV4StackRequest databaseServerV4StackRequest = mock(DatabaseServerV4StackRequest.class);
        DatabaseServerParameter databaseServerParameter = mock(DatabaseServerParameter.class);
        DetailedEnvironmentResponse environmentResponse = mock(DetailedEnvironmentResponse.class);
        AzureDatabaseServerV4Parameters azureDatabaseServerV4Parameters = mock(AzureDatabaseServerV4Parameters.class);

        when(azureDatabaseServerV4Parameters.getAzureDatabaseType()).thenReturn(AzureDatabaseType.FLEXIBLE_SERVER);
        when(azureDatabaseServerV4Parameters.getHighAvailabilityMode()).thenReturn(ZONE_REDUNDANT);
        when(databaseServerV4StackRequest.getAzure()).thenReturn(azureDatabaseServerV4Parameters);
        when(databaseServerParameter.getAvailabilityType()).thenReturn(DatabaseAvailabilityType.HA);
        when(environmentResponse.getAccountId()).thenReturn("accountId");
        when(entitlementService.localDevelopment(anyString())).thenReturn(false);

        underTest.validate(databaseServerV4StackRequest, databaseServerParameter, environmentResponse, true);
    }

    @Test
    void testUpdateVersionRelatedDatabaseParams() {
        Database database = new Database();
        database.setAttributes(new Json("{\"AZURE_DATABASE_TYPE\": \"SINGLE_SERVER\"}"));
        Optional<Database> actualDb = underTest.updateVersionRelatedDatabaseParams(database, MajorVersion.VERSION_14.getMajorVersion());
        assertTrue(actualDb.isPresent());
        assertEquals(AzureDatabaseType.FLEXIBLE_SERVER, underTest.getDatabaseType(database.getAttributesMap()).get());
    }

    @Test
    void testUpdateVersionRelatedDatabaseParamsNoUpdateNeeded() {
        Database database = new Database();
        database.setAttributes(new Json("{\"AZURE_DATABASE_TYPE\": \"FLEXIBLE_SERVER\"}"));
        Optional<Database> actualDb = underTest.updateVersionRelatedDatabaseParams(database, MajorVersion.VERSION_14.getMajorVersion());
        assertFalse(actualDb.isPresent());
    }

    @Test
    void testUpdateVersionRelatedDatabaseParamsNoUpdateNeededVersion() {
        Database database = new Database();
        database.setAttributes(new Json("{\"AZURE_DATABASE_TYPE\": \"SINGLE_SERVER\"}"));
        Optional<Database> actualDb = underTest.updateVersionRelatedDatabaseParams(database, MajorVersion.VERSION_11.getMajorVersion());
        assertFalse(actualDb.isPresent());
    }

    @Test
    void testUpdateVersionRelatedDatabaseParamsNoUpdateNeededVersionFlexi11() {
        Database database = new Database();
        database.setAttributes(new Json("{\"AZURE_DATABASE_TYPE\": \"FLEXIBLE_SERVER\"}"));
        Optional<Database> actualDb = underTest.updateVersionRelatedDatabaseParams(database, MajorVersion.VERSION_11.getMajorVersion());
        assertFalse(actualDb.isPresent());
    }
}
