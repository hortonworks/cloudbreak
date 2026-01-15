package com.sequenceiq.redbeams.converter.stack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.CrnUser;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.MappableBase;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParametersBase;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.tag.CostTagging;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.LocationResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SecurityAccessResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.TagResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslConfigV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslMode;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.NetworkV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.SecurityGroupV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.aws.AwsNetworkV4Parameters;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.azure.AzureDatabaseServerV4Parameters;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.domain.stack.SslConfig;
import com.sequenceiq.redbeams.service.AccountTagService;
import com.sequenceiq.redbeams.service.EnvironmentService;
import com.sequenceiq.redbeams.service.UserGeneratorService;
import com.sequenceiq.redbeams.service.UuidGeneratorService;
import com.sequenceiq.redbeams.service.crn.CrnService;
import com.sequenceiq.redbeams.service.sslcertificate.SslConfigService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AllocateDatabaseServerV4RequestToDBStackConverterTest {

    private static final String OWNER_CRN = "crn:cdp:iam:us-west-1:cloudera:user:external/bob@cloudera.com";

    private static final String ENVIRONMENT_CRN = "myenv";

    private static final String CLUSTER_CRN = "crn:cdp:datahub:us-west-1:cloudera:stack:id";

    private static final String ENVIRONMENT_NAME = "myenv-amazing-env";

    private static final Instant NOW = Instant.now();

    private static final Map<String, Object> ALLOCATE_REQUEST_PARAMETERS = Map.of("key", "value");

    private static final Map<String, Object> SUBNET_ID_REQUEST_PARAMETERS = Map.of("netkey", "netvalue");

    private static final String PASSWORD = "password";

    private static final String USERNAME = "username";

    private static final String DEFAULT_SECURITY_GROUP_ID = "defaultSecurityGroupId";

    private static final String UNKNOWN_CLOUD_PLATFORM = "UnknownCloudPlatform";

    private static final String USER_EMAIL = "userEmail";

    private static final CloudPlatform AWS_CLOUD_PLATFORM = CloudPlatform.AWS;

    private static final String REGION = "myRegion";

    private static final String DATABASE_VENDOR = "postgres";

    private static final String FIELD_DB_SERVICE_SUPPORTED_PLATFORMS = "dbServiceSupportedPlatforms";

    private static final Long NETWORK_ID = 12L;

    @Mock
    private EnvironmentService environmentService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ProviderParameterCalculator providerParameterCalculator;

    @Mock
    private Clock clock;

    @Mock
    private UserGeneratorService userGeneratorService;

    @Mock
    private UuidGeneratorService uuidGeneratorService;

    @Mock
    private CrnUserDetailsService crnUserDetailsService;

    @Mock
    private CostTagging costTagging;

    @Mock
    private CrnService crnService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private AccountTagService accountTagService;

    @Mock
    private SslConfigService sslConfigService;

    @Mock
    private DatabaseServer databaseServer;

    @Mock
    private DatabaseServerV4StackRequestToDatabaseServerConverter databaseServerV4StackRequestToDatabaseServerConverter;

    @InjectMocks
    private AllocateDatabaseServerV4RequestToDBStackConverter underTest;

    private AllocateDatabaseServerV4Request allocateRequest;

    private NetworkV4StackRequest networkRequest;

    private DatabaseServerV4StackRequest databaseServerRequest;

    private SecurityGroupV4StackRequest securityGroupRequest;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(underTest, FIELD_DB_SERVICE_SUPPORTED_PLATFORMS, Set.of("AWS", "AZURE"));

        allocateRequest = new AllocateDatabaseServerV4Request();

        networkRequest = new NetworkV4StackRequest();
        allocateRequest.setNetwork(networkRequest);

        databaseServerRequest = new DatabaseServerV4StackRequest();
        allocateRequest.setDatabaseServer(databaseServerRequest);

        securityGroupRequest = new SecurityGroupV4StackRequest();
        databaseServerRequest.setSecurityGroup(securityGroupRequest);

        when(crnUserDetailsService.getUmsUser(OWNER_CRN)).thenReturn(getCrnUser());
        when(uuidGeneratorService.randomUuid()).thenReturn("uuid");
        when(accountTagService.list()).thenReturn(new HashMap<>());
        when(uuidGeneratorService.uuidVariableParts(anyInt())).thenReturn("parts");
        when(entitlementService.internalTenant(anyString())).thenReturn(true);

        when(clock.getCurrentInstant()).thenReturn(NOW);
        when(crnService.createCrn(any(DBStack.class))).thenReturn(CrnTestUtil.getDatabaseServerCrnBuilder()
                .setAccountId("accountid")
                .setResource("1")
                .build());
    }

    @Test
    void conversionTestWhenAzureFlexibleAndCMKDefinedWithGeoredundant() throws IOException {
        setupAllocateRequest(true);
        allocateRequest.getDatabaseServer().setAzure(new AzureDatabaseServerV4Parameters());
        allocateRequest.getDatabaseServer().getAzure().setGeoRedundantBackup(true);
        allocateRequest.getDatabaseServer().getAzure().setAzureDatabaseType(AzureDatabaseType.FLEXIBLE_SERVER);

        AzureEnvironmentParameters azureEnvironmentParameters = new AzureEnvironmentParameters();
        AzureResourceEncryptionParameters azureResourceEncryptionParameters = new AzureResourceEncryptionParameters();
        azureResourceEncryptionParameters.setEncryptionKeyUrl("encryptionKeyUrl");
        azureEnvironmentParameters.setResourceEncryptionParameters(azureResourceEncryptionParameters);

        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withCloudPlatform(CloudPlatform.AZURE.name())
                .withCrn(ENVIRONMENT_CRN)
                .withLocation(LocationResponse.LocationResponseBuilder.aLocationResponse().withName(REGION).build())
                .withName(ENVIRONMENT_NAME)
                .withTag(new TagResponse())
                .withAzure(azureEnvironmentParameters).build();
        when(environmentService.getByCrn(ENVIRONMENT_CRN)).thenReturn(environment);

        SslConfig sslConfig = new SslConfig();
        sslConfig.setId(16L);
        when(sslConfigService.createSslConfig(eq(allocateRequest), any(DBStack.class))).thenReturn(sslConfig);
        when(databaseServerV4StackRequestToDatabaseServerConverter.buildDatabaseServer(any(DatabaseServerV4StackRequest.class),
                any(CloudPlatform.class),
                any(Crn.class),
                any())).thenReturn(databaseServer);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.convert(allocateRequest, OWNER_CRN));
        assertEquals(badRequestException.getMessage(),
                "Flexible server with GeoRedundant backup not supported with Azure CMK. See more info " +
                        "https://learn.microsoft.com/en-us/azure/postgresql/flexible-server/" +
                        "concepts-data-encryption#using-data-encryption-with-cmks-and-geo-redundant-business-continuity-features.");
    }

    @Test
    void conversionTestWhenOptionalElementsAreProvided() throws IOException {
        setupAllocateRequest(true);

        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withCloudPlatform(AWS_CLOUD_PLATFORM.name())
                .withCrn(ENVIRONMENT_CRN)
                .withLocation(LocationResponse.LocationResponseBuilder.aLocationResponse().withName(REGION).build())
                .withName(ENVIRONMENT_NAME)
                .withTag(new TagResponse())
                .build();
        when(environmentService.getByCrn(ENVIRONMENT_CRN)).thenReturn(environment);

        SslConfig sslConfig = new SslConfig();
        sslConfig.setId(16L);
        when(sslConfigService.createSslConfig(eq(allocateRequest), any(DBStack.class))).thenReturn(sslConfig);
        when(databaseServerV4StackRequestToDatabaseServerConverter.buildDatabaseServer(any(DatabaseServerV4StackRequest.class),
                any(CloudPlatform.class),
                any(Crn.class),
                any())).thenReturn(databaseServer);

        DBStack dbStack = underTest.convert(allocateRequest, OWNER_CRN);

        assertEquals(allocateRequest.getName(), dbStack.getName());
        assertEquals(allocateRequest.getEnvironmentCrn(), dbStack.getEnvironmentId());
        assertEquals(REGION, dbStack.getRegion());
        assertEquals(AWS_CLOUD_PLATFORM.name(), dbStack.getCloudPlatform());
        assertEquals(AWS_CLOUD_PLATFORM.name(), dbStack.getPlatformVariant());
        assertEquals(1, dbStack.getParameters().size());
        assertEquals("value", dbStack.getParameters().get("key"));
        assertEquals(Crn.safeFromString(OWNER_CRN), dbStack.getOwnerCrn());
        assertEquals(USER_EMAIL, dbStack.getUserName());
        assertEquals(Status.REQUESTED, dbStack.getStatus());
        assertEquals(DetailedDBStackStatus.PROVISION_REQUESTED, dbStack.getDbStackStatus().getDetailedDBStackStatus());
        assertEquals(NOW.toEpochMilli(), dbStack.getDbStackStatus().getCreated().longValue());
        assertEquals(databaseServer, dbStack.getDatabaseServer());
        assertNull(dbStack.getNetwork());
        assertEquals(dbStack.getTags().get(StackTags.class).getUserDefinedTags().get("DistroXKey1"), "DistroXValue1");

        verifySsl(dbStack, sslConfig.getId());

        verify(providerParameterCalculator).get(allocateRequest);
        verify(userGeneratorService, never()).generateUserName();
    }

    private CrnUser getCrnUser() {
        return new CrnUser("", "", "", USER_EMAIL, "", "");
    }

    @Test
    void conversionTestWhenOptionalElementsGenerated() throws IOException {
        setupAllocateRequest(false);

        List<CloudSubnet> cloudSubnets = List.of(
                new CloudSubnet.Builder()
                        .id("subnet-1")
                        .availabilityZone("az-a")
                        .build(),
                new CloudSubnet.Builder()
                        .id("subnet-2")
                        .availabilityZone("az-b")
                        .build()
        );
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withCloudPlatform(AWS_CLOUD_PLATFORM.name())
                .withName(ENVIRONMENT_NAME)
                .withCrn(ENVIRONMENT_CRN)
                .withTag(new TagResponse())
                .withLocation(LocationResponse.LocationResponseBuilder.aLocationResponse().withName(REGION).build())
                .withSecurityAccess(SecurityAccessResponse.builder().withDefaultSecurityGroupId(DEFAULT_SECURITY_GROUP_ID).build())
                .withNetwork(EnvironmentNetworkResponse.builder()
                        .withSubnetMetas(
                                Map.of(
                                        "subnet-1", cloudSubnets.get(0),
                                        "subnet-2", cloudSubnets.get(1)
                                )
                        )
                        .build())
                .build();
        when(environmentService.getByCrn(ENVIRONMENT_CRN)).thenReturn(environment);
        when(userGeneratorService.generateUserName()).thenReturn(USERNAME);
        SslConfig sslConfig = new SslConfig();
        sslConfig.setId(16L);
        when(sslConfigService.createSslConfig(eq(allocateRequest), any(DBStack.class))).thenReturn(sslConfig);
        when(databaseServerV4StackRequestToDatabaseServerConverter.buildDatabaseServer(any(DatabaseServerV4StackRequest.class),
                any(CloudPlatform.class),
                any(Crn.class),
                any(SecurityAccessResponse.class))).thenReturn(databaseServer);

        DBStack dbStack = underTest.convert(allocateRequest, OWNER_CRN);

        assertEquals(ENVIRONMENT_NAME + "-dbstck-parts", dbStack.getName());
        assertEquals(databaseServer, dbStack.getDatabaseServer());
        assertEquals(dbStack.getTags().get(StackTags.class).getUserDefinedTags().get("DistroXKey1"), "DistroXValue1");

        verifySsl(dbStack, sslConfig.getId());

        verify(providerParameterCalculator).get(allocateRequest);
        verify(providerParameterCalculator, never()).get(networkRequest);
    }

    @Test
    void conversionTestWhenRequestAndEnvironmentCloudPlatformsDiffer() {
        allocateRequest.setCloudPlatform(AWS_CLOUD_PLATFORM);
        allocateRequest.setTags(new HashMap<>());
        allocateRequest.setEnvironmentCrn(ENVIRONMENT_CRN);
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withCloudPlatform(UNKNOWN_CLOUD_PLATFORM)
                .build();
        when(environmentService.getByCrn(ENVIRONMENT_CRN)).thenReturn(environment);

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> underTest.convert(allocateRequest, OWNER_CRN));

        assertThat(badRequestException).hasMessage("Cloud platform of the request AWS and the environment " + UNKNOWN_CLOUD_PLATFORM + " do not match.");
    }

    @Test
    void conversionTestWhenUnsupportedCloudPlatform() {
        allocateRequest.setCloudPlatform(CloudPlatform.YARN);
        allocateRequest.setTags(new HashMap<>());
        allocateRequest.setEnvironmentCrn(ENVIRONMENT_CRN);
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withCloudPlatform(CloudPlatform.YARN.name())
                .build();
        when(environmentService.getByCrn(ENVIRONMENT_CRN)).thenReturn(environment);

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> underTest.convert(allocateRequest, OWNER_CRN));

        assertThat(badRequestException).hasMessage("Cloud platform YARN not supported yet.");
    }

    private void verifySsl(DBStack dbStack, Long id) {
        assertEquals(id, dbStack.getSslConfig());
    }

    private void setupAllocateRequest(boolean provideOptionalFields) {
        allocateRequest.setEnvironmentCrn(ENVIRONMENT_CRN);
        allocateRequest.setTags(Map.of("DistroXKey1", "DistroXValue1"));
        allocateRequest.setClusterCrn(CLUSTER_CRN);
        if (provideOptionalFields) {
            allocateRequest.setName("myallocation");
            AwsNetworkV4Parameters awsNetworkV4Parameters = new AwsNetworkV4Parameters();
            awsNetworkV4Parameters.setSubnetId("subnet-1,subnet-2");
            allocateRequest.getNetwork().setAws(awsNetworkV4Parameters);
            setupProviderCalculatorResponse(networkRequest, SUBNET_ID_REQUEST_PARAMETERS);
        } else {
            allocateRequest.setNetwork(null);
            allocateRequest.getDatabaseServer().setSecurityGroup(null);
        }
        allocateRequest.setSslConfig(createSslConfigV4Request(SslMode.ENABLED));

        databaseServerRequest.setInstanceType("db.m3.medium");
        databaseServerRequest.setDatabaseVendor(DATABASE_VENDOR);
        databaseServerRequest.setConnectionDriver("org.postgresql.Driver");
        databaseServerRequest.setStorageSize(50L);
        if (provideOptionalFields) {
            databaseServerRequest.setRootUserName("root");
            databaseServerRequest.setRootUserPassword("cloudera");
        }
        setupProviderCalculatorResponse(allocateRequest, ALLOCATE_REQUEST_PARAMETERS);
        setupProviderCalculatorResponse(databaseServerRequest, new HashMap<>(Map.of("dbkey", "dbvalue")));

        securityGroupRequest.setSecurityGroupIds(Set.of("sg-1234"));
    }

    private static SslConfigV4Request createSslConfigV4Request(SslMode sslMode) {
        SslConfigV4Request sslConfigV4Request = new SslConfigV4Request();
        sslConfigV4Request.setSslMode(sslMode);
        return sslConfigV4Request;
    }

    private void setupProviderCalculatorResponse(ProviderParametersBase request, Map<String, Object> response) {
        MappableBase providerCalculatorResponse = mock(MappableBase.class);
        when(providerCalculatorResponse.asMap()).thenReturn(response);
        when(providerParameterCalculator.get(request)).thenReturn(providerCalculatorResponse);
    }

}
