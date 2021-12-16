package com.sequenceiq.redbeams.converter.stack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
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
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.LocationResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SecurityAccessResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.TagResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslConfigV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslMode;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateType;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.NetworkV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.SecurityGroupV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.aws.AwsNetworkV4Parameters;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.configuration.DatabaseServerSslCertificateConfig;
import com.sequenceiq.redbeams.configuration.SslCertificateEntry;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.SslConfig;
import com.sequenceiq.redbeams.service.AccountTagService;
import com.sequenceiq.redbeams.service.EnvironmentService;
import com.sequenceiq.redbeams.service.PasswordGeneratorService;
import com.sequenceiq.redbeams.service.UserGeneratorService;
import com.sequenceiq.redbeams.service.UuidGeneratorService;
import com.sequenceiq.redbeams.service.crn.CrnService;
import com.sequenceiq.redbeams.service.network.NetworkParameterAdder;
import com.sequenceiq.redbeams.service.network.SubnetChooserService;
import com.sequenceiq.redbeams.service.network.SubnetListerService;

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

    private static final CloudPlatform AZURE_CLOUD_PLATFORM = CloudPlatform.AZURE;

    private static final String CLOUD_PROVIDER_IDENTIFIER_V2 = "cert-id-2";

    private static final String CLOUD_PROVIDER_IDENTIFIER_V3 = "cert-id-3";

    private static final String CERT_PEM_V2 = "super-cert-2";

    private static final String CERT_PEM_V3 = "super-cert-3";

    private static final String REGION = "myRegion";

    private static final String DATABASE_VENDOR = "postgres";

    private static final String REDBEAMS_DB_MAJOR_VERSION = "10";

    private static final String FIELD_DB_SERVICE_SUPPORTED_PLATFORMS = "dbServiceSupportedPlatforms";

    private static final String FIELD_REDBEAMS_DB_MAJOR_VERSION = "redbeamsDbMajorVersion";

    private static final String FIELD_SSL_ENABLED = "sslEnabled";

    private static final int MAX_VERSION = 3;

    private static final int VERSION_1 = 1;

    private static final int VERSION_2 = 2;

    private static final int VERSION_3 = 3;

    private static final int NO_CERTS = 0;

    private static final int SINGLE_CERT = 1;

    private static final int TWO_CERTS = 2;

    private static final int THREE_CERTS = 3;

    @Mock
    private EnvironmentService environmentService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ProviderParameterCalculator providerParameterCalculator;

    @Mock
    private Clock clock;

    @Mock
    private SubnetListerService subnetListerService;

    @Mock
    private SubnetChooserService subnetChooserService;

    @Mock
    private UserGeneratorService userGeneratorService;

    @Mock
    private PasswordGeneratorService passwordGeneratorService;

    @Mock
    private NetworkParameterAdder networkParameterAdder;

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
    private DatabaseServerSslCertificateConfig databaseServerSslCertificateConfig;

    @Mock
    private X509Certificate x509Certificate;

    @InjectMocks
    private AllocateDatabaseServerV4RequestToDBStackConverter underTest;

    private AllocateDatabaseServerV4Request allocateRequest;

    private NetworkV4StackRequest networkRequest;

    private DatabaseServerV4StackRequest databaseServerRequest;

    private SecurityGroupV4StackRequest securityGroupRequest;

    private SslCertificateEntry sslCertificateEntryV2;

    private SslCertificateEntry sslCertificateEntryV3;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(underTest, FIELD_DB_SERVICE_SUPPORTED_PLATFORMS, Set.of("AWS", "AZURE"));
        ReflectionTestUtils.setField(underTest, FIELD_REDBEAMS_DB_MAJOR_VERSION, REDBEAMS_DB_MAJOR_VERSION);
        ReflectionTestUtils.setField(underTest, FIELD_SSL_ENABLED, true);

        allocateRequest = new AllocateDatabaseServerV4Request();

        networkRequest = new NetworkV4StackRequest();
        allocateRequest.setNetwork(networkRequest);

        databaseServerRequest = new DatabaseServerV4StackRequest();
        allocateRequest.setDatabaseServer(databaseServerRequest);

        securityGroupRequest = new SecurityGroupV4StackRequest();
        databaseServerRequest.setSecurityGroup(securityGroupRequest);

        when(crnUserDetailsService.loadUserByUsername(OWNER_CRN)).thenReturn(getCrnUser());
        when(uuidGeneratorService.randomUuid()).thenReturn("uuid");
        when(accountTagService.list()).thenReturn(new HashMap<>());
        when(uuidGeneratorService.uuidVariableParts(anyInt())).thenReturn("parts");
        when(entitlementService.internalTenant(anyString())).thenReturn(true);

        sslCertificateEntryV2 = new SslCertificateEntry(VERSION_2, CLOUD_PROVIDER_IDENTIFIER_V2, CERT_PEM_V2, x509Certificate);
        sslCertificateEntryV3 = new SslCertificateEntry(VERSION_3, CLOUD_PROVIDER_IDENTIFIER_V3, CERT_PEM_V3, x509Certificate);
        when(databaseServerSslCertificateConfig.getMaxVersionByCloudPlatformAndRegion(anyString(), eq(REGION))).thenReturn(MAX_VERSION);

        when(clock.getCurrentInstant()).thenReturn(NOW);
        when(crnService.createCrn(any(DBStack.class))).thenReturn(CrnTestUtil.getDatabaseServerCrnBuilder()
                .setAccountId("accountid")
                .setResource("1")
                .build());
    }

    @Test
    void conversionTestWhenOptionalElementsAreProvided() throws IOException {
        setupAllocateRequest(true);

        when(databaseServerSslCertificateConfig.getNumberOfCertsByCloudPlatformAndRegion(AWS_CLOUD_PLATFORM.name(), REGION)).thenReturn(SINGLE_CERT);
        when(databaseServerSslCertificateConfig.getCertByCloudPlatformAndRegionAndVersion(AWS_CLOUD_PLATFORM.name(), REGION, VERSION_3))
                .thenReturn(sslCertificateEntryV3);

        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withCloudPlatform(AWS_CLOUD_PLATFORM.name())
                .withCrn(ENVIRONMENT_CRN)
                .withLocation(LocationResponse.LocationResponseBuilder.aLocationResponse().withName(REGION).build())
                .withName(ENVIRONMENT_NAME)
                .withTag(new TagResponse())
                .build();
        when(environmentService.getByCrn(ENVIRONMENT_CRN)).thenReturn(environment);
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

        assertEquals("n-uuid", dbStack.getNetwork().getName());
        assertEquals(1, dbStack.getNetwork().getAttributes().getMap().size());
        assertEquals("netvalue", dbStack.getNetwork().getAttributes().getMap().get("netkey"));

        assertEquals("dbsvr-uuid", dbStack.getDatabaseServer().getName());
        assertEquals(databaseServerRequest.getInstanceType(), dbStack.getDatabaseServer().getInstanceType());
        assertEquals(DatabaseVendor.fromValue(databaseServerRequest.getDatabaseVendor()), dbStack.getDatabaseServer().getDatabaseVendor());
        assertEquals("org.postgresql.Driver", dbStack.getDatabaseServer().getConnectionDriver());
        assertEquals(databaseServerRequest.getStorageSize(), dbStack.getDatabaseServer().getStorageSize());
        assertEquals(databaseServerRequest.getRootUserName(), dbStack.getDatabaseServer().getRootUserName());
        assertEquals(databaseServerRequest.getRootUserPassword(), dbStack.getDatabaseServer().getRootPassword());
        assertEquals(2, dbStack.getDatabaseServer().getAttributes().getMap().size());
        assertEquals("dbvalue", dbStack.getDatabaseServer().getAttributes().getMap().get("dbkey"));
        assertEquals(REDBEAMS_DB_MAJOR_VERSION, dbStack.getDatabaseServer().getAttributes().getMap().get("engineVersion"));
        assertEquals(securityGroupRequest.getSecurityGroupIds(), dbStack.getDatabaseServer().getSecurityGroup().getSecurityGroupIds());
        assertEquals(dbStack.getTags().get(StackTags.class).getUserDefinedTags().get("DistroXKey1"), "DistroXValue1");

        verifySsl(dbStack, Set.of(CERT_PEM_V3), CLOUD_PROVIDER_IDENTIFIER_V3);
        verify(databaseServerSslCertificateConfig, never()).getCertsByCloudPlatformAndRegionAndVersions(anyString(), anyString(), any());

        verify(providerParameterCalculator).get(allocateRequest);
        verify(providerParameterCalculator).get(networkRequest);
        verify(subnetListerService, never()).listSubnets(any(), any());
        verify(subnetChooserService, never()).chooseSubnets(anyList(), any(), any());
        verify(networkParameterAdder, never()).addSubnetIds(any(), any(), any(), any());
        verify(userGeneratorService, never()).generateUserName();
        verify(passwordGeneratorService, never()).generatePassword(any());
    }

    private CrnUser getCrnUser() {
        return new CrnUser("", "", "", USER_EMAIL, "", "");
    }

    @Test
    void conversionTestWhenOptionalElementsGenerated() throws IOException {
        setupAllocateRequest(false);

        when(databaseServerSslCertificateConfig.getNumberOfCertsByCloudPlatformAndRegion(AWS_CLOUD_PLATFORM.name(), REGION)).thenReturn(SINGLE_CERT);
        when(databaseServerSslCertificateConfig.getCertByCloudPlatformAndRegionAndVersion(AWS_CLOUD_PLATFORM.name(), REGION, VERSION_3))
                .thenReturn(sslCertificateEntryV3);

        List<CloudSubnet> cloudSubnets = List.of(
                new CloudSubnet("subnet-1", "", "az-a", ""),
                new CloudSubnet("subnet-2", "", "az-b", "")
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
        when(subnetListerService.listSubnets(any(), any())).thenReturn(cloudSubnets);
        when(subnetChooserService.chooseSubnets(any(), any(), any())).thenReturn(cloudSubnets);
        when(networkParameterAdder.addSubnetIds(any(), any(), any(), any())).thenReturn(SUBNET_ID_REQUEST_PARAMETERS);
        when(userGeneratorService.generateUserName()).thenReturn(USERNAME);
        when(passwordGeneratorService.generatePassword(any())).thenReturn(PASSWORD);

        DBStack dbStack = underTest.convert(allocateRequest, OWNER_CRN);

        assertEquals(ENVIRONMENT_NAME + "-dbstck-parts", dbStack.getName());
        assertEquals(PASSWORD, dbStack.getDatabaseServer().getRootPassword());
        assertEquals(USERNAME, dbStack.getDatabaseServer().getRootUserName());
        assertEquals("n-uuid", dbStack.getNetwork().getName());
        assertEquals(1, dbStack.getNetwork().getAttributes().getMap().size());
        assertEquals("netvalue", dbStack.getNetwork().getAttributes().getMap().get("netkey"));
        assertThat(dbStack.getDatabaseServer().getSecurityGroup().getSecurityGroupIds()).hasSize(1);
        assertEquals(dbStack.getDatabaseServer().getSecurityGroup().getSecurityGroupIds().iterator().next(), DEFAULT_SECURITY_GROUP_ID);
        assertEquals(dbStack.getTags().get(StackTags.class).getUserDefinedTags().get("DistroXKey1"), "DistroXValue1");

        verifySsl(dbStack, Set.of(CERT_PEM_V3), CLOUD_PROVIDER_IDENTIFIER_V3);
        verify(databaseServerSslCertificateConfig, never()).getCertsByCloudPlatformAndRegionAndVersions(anyString(), anyString(), any());

        verify(providerParameterCalculator).get(allocateRequest);
        verify(providerParameterCalculator, never()).get(networkRequest);
        verify(subnetListerService).listSubnets(any(), any());
        verify(subnetChooserService).chooseSubnets(anyList(), any(), any());
        verify(networkParameterAdder).addSubnetIds(any(), any(), any(), any());
        verify(userGeneratorService).generateUserName();
        verify(passwordGeneratorService).generatePassword(any());
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

    static Object[][] conversionTestWhenSslDisabledDataProvider() {
        return new Object[][]{
                // testCaseName fieldSslEnabled sslConfigV4Request
                {"false, null", false, null},
                {"true, null", true, null},
                {"false, request with sslMode=null", false, new SslConfigV4Request()},
                {"true, request with sslMode=null", true, new SslConfigV4Request()},
                {"false, request with sslMode=DISABLED", false, createSslConfigV4Request(SslMode.DISABLED)},
                {"true, request with sslMode=DISABLED", true, createSslConfigV4Request(SslMode.DISABLED)},
                {"false, request with sslMode=ENABLED", false, createSslConfigV4Request(SslMode.ENABLED)},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("conversionTestWhenSslDisabledDataProvider")
    void conversionTestWhenSslDisabled(String testCaseName, boolean fieldSslEnabled, SslConfigV4Request sslConfigV4Request) {
        setupMinimalValid(sslConfigV4Request, AWS_CLOUD_PLATFORM);
        ReflectionTestUtils.setField(underTest, FIELD_SSL_ENABLED, fieldSslEnabled);

        DBStack dbStack = underTest.convert(allocateRequest, OWNER_CRN);

        SslConfig sslConfig = dbStack.getSslConfig();
        assertThat(sslConfig).isNotNull();
        Set<String> sslCertificates = sslConfig.getSslCertificates();
        assertThat(sslCertificates).isNotNull();
        assertThat(sslCertificates).isEmpty();
        assertThat(sslConfig.getSslCertificateType()).isEqualTo(SslCertificateType.NONE);
    }

    private void setupMinimalValid(SslConfigV4Request sslConfigV4Request, CloudPlatform cloudPlatform) {
        allocateRequest.setEnvironmentCrn(ENVIRONMENT_CRN);
        allocateRequest.setTags(new HashMap<>());
        allocateRequest.setClusterCrn(CLUSTER_CRN);
        allocateRequest.setSslConfig(sslConfigV4Request);

        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withCloudPlatform(cloudPlatform.name())
                .withCrn(ENVIRONMENT_CRN)
                .withLocation(LocationResponse.LocationResponseBuilder.aLocationResponse().withName(REGION).build())
                .withName(ENVIRONMENT_NAME)
                .withTag(new TagResponse())
                .build();
        when(environmentService.getByCrn(ENVIRONMENT_CRN)).thenReturn(environment);

        databaseServerRequest.setDatabaseVendor(DATABASE_VENDOR);
    }

    @Test
    void conversionTestWhenSslEnabledAndAwsAndNoCerts() {
        setupMinimalValid(createSslConfigV4Request(SslMode.ENABLED), AWS_CLOUD_PLATFORM);

        when(databaseServerSslCertificateConfig.getNumberOfCertsByCloudPlatformAndRegion(AWS_CLOUD_PLATFORM.name(), REGION)).thenReturn(NO_CERTS);

        DBStack dbStack = underTest.convert(allocateRequest, OWNER_CRN);

        verifySsl(dbStack, Set.of(), null);

        verify(databaseServerSslCertificateConfig, never()).getCertByCloudPlatformAndRegionAndVersion(anyString(), anyString(), anyInt());
        verify(databaseServerSslCertificateConfig, never()).getCertsByCloudPlatformAndRegionAndVersions(anyString(), anyString(), any());
    }

    @Test
    void conversionTestWhenSslEnabledAndAwsAndSingleCert() {
        setupMinimalValid(createSslConfigV4Request(SslMode.ENABLED), AWS_CLOUD_PLATFORM);

        conversionTestWhenSslEnabledAndSingleCertReturnedInternal(AWS_CLOUD_PLATFORM.name(), SINGLE_CERT);
    }

    private void conversionTestWhenSslEnabledAndSingleCertReturnedInternal(String cloudPlatform, int numOfCerts) {
        when(databaseServerSslCertificateConfig.getNumberOfCertsByCloudPlatformAndRegion(cloudPlatform, REGION)).thenReturn(numOfCerts);
        when(databaseServerSslCertificateConfig.getCertByCloudPlatformAndRegionAndVersion(cloudPlatform, REGION, VERSION_3)).thenReturn(sslCertificateEntryV3);

        DBStack dbStack = underTest.convert(allocateRequest, OWNER_CRN);

        verifySsl(dbStack, Set.of(CERT_PEM_V3), CLOUD_PROVIDER_IDENTIFIER_V3);

        verify(databaseServerSslCertificateConfig, never()).getCertsByCloudPlatformAndRegionAndVersions(anyString(), anyString(), any());
    }

    @Test
    void conversionTestWhenSslEnabledAndAwsAndSingleCertErrorNullCert() {
        setupMinimalValid(createSslConfigV4Request(SslMode.ENABLED), AWS_CLOUD_PLATFORM);

        when(databaseServerSslCertificateConfig.getNumberOfCertsByCloudPlatformAndRegion(AWS_CLOUD_PLATFORM.name(), REGION)).thenReturn(SINGLE_CERT);
        when(databaseServerSslCertificateConfig.getCertByCloudPlatformAndRegionAndVersion(AWS_CLOUD_PLATFORM.name(), REGION, VERSION_3)).thenReturn(null);

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> underTest.convert(allocateRequest, OWNER_CRN));
        assertThat(illegalStateException).hasMessage("Could not find SSL certificate version 3 for cloud platform \"AWS\"");

        verify(databaseServerSslCertificateConfig, never()).getCertsByCloudPlatformAndRegionAndVersions(anyString(), anyString(), any());
    }

    @Test
    void conversionTestWhenSslEnabledAndAwsAndSingleCertErrorVersionMismatch() {
        setupMinimalValid(createSslConfigV4Request(SslMode.ENABLED), AWS_CLOUD_PLATFORM);

        when(databaseServerSslCertificateConfig.getNumberOfCertsByCloudPlatformAndRegion(AWS_CLOUD_PLATFORM.name(), REGION)).thenReturn(SINGLE_CERT);
        when(databaseServerSslCertificateConfig.getCertByCloudPlatformAndRegionAndVersion(AWS_CLOUD_PLATFORM.name(), REGION, VERSION_3))
                .thenReturn(sslCertificateEntryV2);

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> underTest.convert(allocateRequest, OWNER_CRN));
        assertThat(illegalStateException).hasMessage("SSL certificate version mismatch for cloud platform \"AWS\": expected=3, actual=2");

        verify(databaseServerSslCertificateConfig, never()).getCertsByCloudPlatformAndRegionAndVersions(anyString(), anyString(), any());
    }

    @Test
    void conversionTestWhenSslEnabledAndAwsAndSingleCertErrorBlankCloudProviderIdentifier() {
        setupMinimalValid(createSslConfigV4Request(SslMode.ENABLED), AWS_CLOUD_PLATFORM);

        when(databaseServerSslCertificateConfig.getNumberOfCertsByCloudPlatformAndRegion(AWS_CLOUD_PLATFORM.name(), REGION)).thenReturn(SINGLE_CERT);
        SslCertificateEntry sslCertificateEntryV3Broken = new SslCertificateEntry(VERSION_3, "", CERT_PEM_V3, x509Certificate);
        when(databaseServerSslCertificateConfig.getCertByCloudPlatformAndRegionAndVersion(AWS_CLOUD_PLATFORM.name(), REGION, VERSION_3))
                .thenReturn(sslCertificateEntryV3Broken);

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> underTest.convert(allocateRequest, OWNER_CRN));
        assertThat(illegalStateException).hasMessage("Blank CloudProviderIdentifier in SSL certificate version 3 for cloud platform \"AWS\"");

        verify(databaseServerSslCertificateConfig, never()).getCertsByCloudPlatformAndRegionAndVersions(anyString(), anyString(), any());
    }

    @Test
    void conversionTestWhenSslEnabledAndAwsAndSingleCertErrorBlankPem() {
        setupMinimalValid(createSslConfigV4Request(SslMode.ENABLED), AWS_CLOUD_PLATFORM);

        when(databaseServerSslCertificateConfig.getNumberOfCertsByCloudPlatformAndRegion(AWS_CLOUD_PLATFORM.name(), REGION)).thenReturn(SINGLE_CERT);
        SslCertificateEntry sslCertificateEntryV3Broken = new SslCertificateEntry(VERSION_3, CLOUD_PROVIDER_IDENTIFIER_V3, "", x509Certificate);
        when(databaseServerSslCertificateConfig.getCertByCloudPlatformAndRegionAndVersion(AWS_CLOUD_PLATFORM.name(), REGION, VERSION_3))
                .thenReturn(sslCertificateEntryV3Broken);

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> underTest.convert(allocateRequest, OWNER_CRN));
        assertThat(illegalStateException).hasMessage("Blank PEM in SSL certificate version 3 for cloud platform \"AWS\"");

        verify(databaseServerSslCertificateConfig, never()).getCertsByCloudPlatformAndRegionAndVersions(anyString(), anyString(), any());
    }

    @Test
    void conversionTestWhenSslEnabledAndAzureAndSingleCert() {
        setupMinimalValid(createSslConfigV4Request(SslMode.ENABLED), AZURE_CLOUD_PLATFORM);

        conversionTestWhenSslEnabledAndSingleCertReturnedInternal(AZURE_CLOUD_PLATFORM.name(), SINGLE_CERT);
    }

    @Test
    void conversionTestWhenSslEnabledAndAwsAndTwoCerts() {
        setupMinimalValid(createSslConfigV4Request(SslMode.ENABLED), AWS_CLOUD_PLATFORM);

        conversionTestWhenSslEnabledAndSingleCertReturnedInternal(AWS_CLOUD_PLATFORM.name(), TWO_CERTS);
    }

    @Test
    void conversionTestWhenSslEnabledAndAwsAndThreeCerts() {
        setupMinimalValid(createSslConfigV4Request(SslMode.ENABLED), AWS_CLOUD_PLATFORM);

        conversionTestWhenSslEnabledAndSingleCertReturnedInternal(AWS_CLOUD_PLATFORM.name(), THREE_CERTS);
    }

    @Test
    void conversionTestWhenSslEnabledAndAzureAndTwoCerts() {
        setupMinimalValid(createSslConfigV4Request(SslMode.ENABLED), AZURE_CLOUD_PLATFORM);

        conversionTestWhenSslEnabledAndTwoCertsReturnedInternal(AZURE_CLOUD_PLATFORM.name(), TWO_CERTS);
    }

    private void conversionTestWhenSslEnabledAndTwoCertsReturnedInternal(String cloudPlatform, int numOfCerts) {
        when(databaseServerSslCertificateConfig.getNumberOfCertsByCloudPlatformAndRegion(cloudPlatform, REGION)).thenReturn(numOfCerts);
        when(databaseServerSslCertificateConfig.getCertsByCloudPlatformAndRegionAndVersions(cloudPlatform, REGION, VERSION_2, VERSION_3))
                .thenReturn(Set.of(sslCertificateEntryV2, sslCertificateEntryV3));

        DBStack dbStack = underTest.convert(allocateRequest, OWNER_CRN);

        verifySsl(dbStack, Set.of(CERT_PEM_V2, CERT_PEM_V3), CLOUD_PROVIDER_IDENTIFIER_V3);

        verify(databaseServerSslCertificateConfig, never()).getCertByCloudPlatformAndRegionAndVersion(anyString(), anyString(), anyInt());
    }

    @Test
    void conversionTestWhenSslEnabledAndAzureAndTwoCertsErrorNullCert() {
        setupMinimalValid(createSslConfigV4Request(SslMode.ENABLED), AZURE_CLOUD_PLATFORM);

        when(databaseServerSslCertificateConfig.getNumberOfCertsByCloudPlatformAndRegion(AZURE_CLOUD_PLATFORM.name(), REGION)).thenReturn(TWO_CERTS);

        Set<SslCertificateEntry> certs = new HashSet<>();
        certs.add(sslCertificateEntryV3);
        certs.add(null);
        when(databaseServerSslCertificateConfig.getCertsByCloudPlatformAndRegionAndVersions(AZURE_CLOUD_PLATFORM.name(), REGION, VERSION_2, VERSION_3))
                .thenReturn(certs);

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> underTest.convert(allocateRequest, OWNER_CRN));
        assertThat(illegalStateException)
                .hasMessage("Could not find SSL certificate(s) when requesting versions [2, 3] for cloud platform \"AZURE\": expected 2 certificates, got 1");

        verify(databaseServerSslCertificateConfig, never()).getCertByCloudPlatformAndRegionAndVersion(anyString(), anyString(), anyInt());
    }

    @Test
    void conversionTestWhenSslEnabledAndAzureAndTwoCertsErrorFewerCerts() {
        setupMinimalValid(createSslConfigV4Request(SslMode.ENABLED), AZURE_CLOUD_PLATFORM);

        when(databaseServerSslCertificateConfig.getNumberOfCertsByCloudPlatformAndRegion(AZURE_CLOUD_PLATFORM.name(), REGION)).thenReturn(TWO_CERTS);
        when(databaseServerSslCertificateConfig.getCertsByCloudPlatformAndRegionAndVersions(AZURE_CLOUD_PLATFORM.name(), REGION, VERSION_2, VERSION_3))
                .thenReturn(Set.of(sslCertificateEntryV3));

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> underTest.convert(allocateRequest, OWNER_CRN));
        assertThat(illegalStateException)
                .hasMessage("Could not find SSL certificate(s) when requesting versions [2, 3] for cloud platform \"AZURE\": expected 2 certificates, got 1");

        verify(databaseServerSslCertificateConfig, never()).getCertByCloudPlatformAndRegionAndVersion(anyString(), anyString(), anyInt());
    }

    @Test
    void conversionTestWhenSslEnabledAndAzureAndTwoCertsErrorDuplicatedCertPem() {
        setupMinimalValid(createSslConfigV4Request(SslMode.ENABLED), AZURE_CLOUD_PLATFORM);

        SslCertificateEntry sslCertificateEntryV2DuplicateOfV3 = new SslCertificateEntry(VERSION_2, CLOUD_PROVIDER_IDENTIFIER_V3, CERT_PEM_V3, x509Certificate);

        when(databaseServerSslCertificateConfig.getNumberOfCertsByCloudPlatformAndRegion(AZURE_CLOUD_PLATFORM.name(), REGION)).thenReturn(TWO_CERTS);
        when(databaseServerSslCertificateConfig.getCertsByCloudPlatformAndRegionAndVersions(AZURE_CLOUD_PLATFORM.name(), REGION, VERSION_2, VERSION_3))
                .thenReturn(Set.of(sslCertificateEntryV2DuplicateOfV3, sslCertificateEntryV3));

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> underTest.convert(allocateRequest, OWNER_CRN));
        assertThat(illegalStateException)
                .hasMessage("Received duplicated SSL certificate PEM when requesting versions [2, 3] for cloud platform \"AZURE\"");

        verify(databaseServerSslCertificateConfig, never()).getCertByCloudPlatformAndRegionAndVersion(anyString(), anyString(), anyInt());
    }

    @Test
    void conversionTestWhenSslEnabledAndAzureAndTwoCertsErrorVersionMismatch() {
        setupMinimalValid(createSslConfigV4Request(SslMode.ENABLED), AZURE_CLOUD_PLATFORM);

        SslCertificateEntry sslCertificateEntryV2Broken = new SslCertificateEntry(VERSION_1, CLOUD_PROVIDER_IDENTIFIER_V2, CERT_PEM_V2, x509Certificate);

        when(databaseServerSslCertificateConfig.getNumberOfCertsByCloudPlatformAndRegion(AZURE_CLOUD_PLATFORM.name(), REGION)).thenReturn(TWO_CERTS);
        when(databaseServerSslCertificateConfig.getCertsByCloudPlatformAndRegionAndVersions(AZURE_CLOUD_PLATFORM.name(), REGION, VERSION_2, VERSION_3))
                .thenReturn(Set.of(sslCertificateEntryV2Broken, sslCertificateEntryV3));

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> underTest.convert(allocateRequest, OWNER_CRN));
        assertThat(illegalStateException)
                .hasMessage("Could not find SSL certificate version 2 for cloud platform \"AZURE\"");

        verify(databaseServerSslCertificateConfig, never()).getCertByCloudPlatformAndRegionAndVersion(anyString(), anyString(), anyInt());
    }

    @Test
    void conversionTestWhenSslEnabledAndAzureAndThreeCerts() {
        setupMinimalValid(createSslConfigV4Request(SslMode.ENABLED), AZURE_CLOUD_PLATFORM);

        conversionTestWhenSslEnabledAndTwoCertsReturnedInternal(AZURE_CLOUD_PLATFORM.name(), THREE_CERTS);
    }

    private void verifySsl(DBStack dbStack, Set<String> sslCertificatesExpected, String cloudProviderIdentifierExpected) {
        SslConfig sslConfig = dbStack.getSslConfig();
        assertThat(sslConfig).isNotNull();
        Set<String> sslCertificates = sslConfig.getSslCertificates();
        assertThat(sslCertificates).isNotNull();
        assertThat(sslCertificates).isEqualTo(sslCertificatesExpected);
        assertThat(sslConfig.getSslCertificateType()).isEqualTo(SslCertificateType.CLOUD_PROVIDER_OWNED);
        assertThat(sslConfig.getSslCertificateActiveVersion()).isEqualTo(MAX_VERSION);
        assertThat(sslConfig.getSslCertificateActiveCloudProviderIdentifier()).isEqualTo(cloudProviderIdentifierExpected);
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


