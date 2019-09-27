package com.sequenceiq.redbeams.converter.stack;

import static com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag.CB_CREATION_TIMESTAMP;
import static com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag.CB_USER_NAME;
import static com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag.CB_VERSION;
import static com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag.OWNER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.security.CrnUser;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.MappableBase;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParametersBase;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.LocationResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SecurityAccessResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.NetworkV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.SecurityGroupV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.aws.AwsNetworkV4Parameters;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.service.EnvironmentService;
import com.sequenceiq.redbeams.service.PasswordGeneratorService;
import com.sequenceiq.redbeams.service.UserGeneratorService;
import com.sequenceiq.redbeams.service.UuidGeneratorService;
import com.sequenceiq.redbeams.service.network.NetworkParameterAdder;
import com.sequenceiq.redbeams.service.network.SubnetChooserService;
import com.sequenceiq.redbeams.service.network.SubnetListerService;

public class AllocateDatabaseServerV4RequestToDBStackConverterTest {

    private static final String OWNER_CRN = "crn:cdp:iam:us-west-1:cloudera:user:external/bob@cloudera.com";

    private static final String ENVIRONMENT_CRN = "myenv";

    private static final String VERSION = "1.2.3.4";

    private static final Instant NOW = Instant.now();

    private static final Map<String, Object> ALLOCATE_REQUEST_PARAMETERS = Map.of("key", "value");

    private static final Map<String, Object> SUBNET_ID_REQUEST_PARAMETERS = Map.of("netkey", "netvalue");

    private static final Map<String, Object> DATABASE_SERVER_REQUEST_PARAMETERS = Map.of("dbkey", "dbvalue");

    private static final String PASSWORD = "password";

    private static final String USERNAME = "username";

    private static final String DEFAULT_SECURITY_GROUP_ID = "defaultSecurityGroupId";

    private static final String UNKNOWN_CLOUD_PLATFORM = "UnknownCloudPlatform";

    private static final String USER_EMAIL = "userEmail";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

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

    @InjectMocks
    private AllocateDatabaseServerV4RequestToDBStackConverter underTest;

    private AllocateDatabaseServerV4Request allocateRequest;

    private NetworkV4StackRequest networkRequest;

    private DatabaseServerV4StackRequest databaseServerRequest;

    private SecurityGroupV4StackRequest securityGroupRequest;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        ReflectionTestUtils.setField(underTest, "version", VERSION);
        ReflectionTestUtils.setField(underTest, "dbServiceSupportedPlatforms", Set.of("AWS", "AZURE"));

        allocateRequest = new AllocateDatabaseServerV4Request();

        networkRequest = new NetworkV4StackRequest();
        allocateRequest.setNetwork(networkRequest);

        databaseServerRequest = new DatabaseServerV4StackRequest();
        allocateRequest.setDatabaseServer(databaseServerRequest);

        securityGroupRequest = new SecurityGroupV4StackRequest();
        databaseServerRequest.setSecurityGroup(securityGroupRequest);

        when(uuidGeneratorService.randomUuid()).thenReturn("uuid");
        when(uuidGeneratorService.uuidVariableParts(anyInt())).thenReturn("parts");

        when(clock.getCurrentInstant()).thenReturn(NOW);
    }

    @Test
    public void testConversionWhenOptionalElementsAreProvided() throws IOException {
        setupAllocateRequest(true);

        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.Builder.builder()
                .withCloudPlatform(CloudPlatform.AWS.name())
                .withLocation(LocationResponse.LocationResponseBuilder.aLocationResponse().withName("myRegion").build())
                .build();
        when(environmentService.getByCrn(ENVIRONMENT_CRN)).thenReturn(environment);
        when(crnUserDetailsService.loadUserByUsername(OWNER_CRN)).thenReturn(getCrnUser());

        DBStack dbStack = underTest.convert(allocateRequest, OWNER_CRN);

        assertEquals(allocateRequest.getName(), dbStack.getName());
        assertEquals(allocateRequest.getEnvironmentCrn(), dbStack.getEnvironmentId());
//        assertEquals(allocateRequest.getRegion(), dbStack.getRegion());
        assertEquals(CloudPlatform.AWS.name(), dbStack.getCloudPlatform());
        assertEquals(CloudPlatform.AWS.name(), dbStack.getPlatformVariant());
        assertEquals(1, dbStack.getParameters().size());
        assertEquals("value", dbStack.getParameters().get("key"));
        assertEquals(Crn.safeFromString(OWNER_CRN), dbStack.getOwnerCrn());
        assertEquals(USER_EMAIL, dbStack.getUserName());

        Json tags = dbStack.getTags();
        StackTags stackTags = tags.get(StackTags.class);
        Map<String, String> defaultTags = stackTags.getDefaultTags();
        assertEquals(USER_EMAIL, defaultTags.get(CB_USER_NAME.key()));
        assertEquals(USER_EMAIL, defaultTags.get(OWNER.key()));
        assertEquals(String.valueOf(NOW.getEpochSecond()), defaultTags.get(CB_CREATION_TIMESTAMP.key()));
        assertEquals(VERSION, defaultTags.get(CB_VERSION.key()));

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
        assertEquals(1, dbStack.getDatabaseServer().getAttributes().getMap().size());
        assertEquals("dbvalue", dbStack.getDatabaseServer().getAttributes().getMap().get("dbkey"));

        assertEquals(securityGroupRequest.getSecurityGroupIds(), dbStack.getDatabaseServer().getSecurityGroup().getSecurityGroupIds());
        verify(providerParameterCalculator).get(allocateRequest);
        verify(providerParameterCalculator).get(networkRequest);
        verify(subnetListerService, never()).listSubnets(any(), any());
        verify(subnetChooserService, never()).chooseSubnets(anyList(), any());
        verify(networkParameterAdder, never()).addSubnetIds(any(), any(), any());
        verify(userGeneratorService, never()).generateUserName();
        verify(passwordGeneratorService, never()).generatePassword(any());
    }

    private CrnUser getCrnUser() {
        return new CrnUser("", "", "", USER_EMAIL, "", "");
    }

    @Test
    public void testConversionWhenOptionalElementsGenerated() throws IOException {
        setupAllocateRequest(false);
        List<CloudSubnet> cloudSubnets = List.of(
                new CloudSubnet("subnet-1", "", "az-a", ""),
                new CloudSubnet("subnet-2", "", "az-b", "")
        );
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.Builder.builder()
                .withCloudPlatform(CloudPlatform.AWS.name())
                .withName("envName")
                .withLocation(LocationResponse.LocationResponseBuilder.aLocationResponse().withName("myRegion").build())
                .withSecurityAccess(SecurityAccessResponse.builder().withDefaultSecurityGroupId(DEFAULT_SECURITY_GROUP_ID).build())
                .withNetwork(EnvironmentNetworkResponse.EnvironmentNetworkResponseBuilder.anEnvironmentNetworkResponse()
                        .withSubnetMetas(
                                Map.of(
                                        "subnet-1", cloudSubnets.get(0),
                                        "subnet-2", cloudSubnets.get(1)
                                )
                        )
                        .build())
                .build();
        when(crnUserDetailsService.loadUserByUsername(OWNER_CRN)).thenReturn(getCrnUser());
        when(environmentService.getByCrn(ENVIRONMENT_CRN)).thenReturn(environment);
        when(subnetListerService.listSubnets(any(), any())).thenReturn(cloudSubnets);
        when(subnetChooserService.chooseSubnets(any(), any())).thenReturn(cloudSubnets);
        when(networkParameterAdder.addSubnetIds(any(), any(), any())).thenReturn(SUBNET_ID_REQUEST_PARAMETERS);
        when(userGeneratorService.generateUserName()).thenReturn(USERNAME);
        when(passwordGeneratorService.generatePassword(any())).thenReturn(PASSWORD);

        DBStack dbStack = underTest.convert(allocateRequest, OWNER_CRN);

        assertEquals("dbstck-envName-parts", dbStack.getName());
        assertEquals(PASSWORD, dbStack.getDatabaseServer().getRootPassword());
        assertEquals(USERNAME, dbStack.getDatabaseServer().getRootUserName());
        assertEquals("n-uuid", dbStack.getNetwork().getName());
        assertEquals(1, dbStack.getNetwork().getAttributes().getMap().size());
        assertEquals("netvalue", dbStack.getNetwork().getAttributes().getMap().get("netkey"));
        assertThat(dbStack.getDatabaseServer().getSecurityGroup().getSecurityGroupIds(), hasSize(1));
        assertEquals(dbStack.getDatabaseServer().getSecurityGroup().getSecurityGroupIds().iterator().next(), DEFAULT_SECURITY_GROUP_ID);
        verify(providerParameterCalculator).get(allocateRequest);
        verify(providerParameterCalculator, never()).get(networkRequest);
        verify(subnetListerService).listSubnets(any(), any());
        verify(subnetChooserService).chooseSubnets(anyList(), any());
        verify(networkParameterAdder).addSubnetIds(any(), any(), any());
        verify(userGeneratorService).generateUserName();
        verify(passwordGeneratorService).generatePassword(any());
    }

    @Test
    public void testConversionWhenRequestAndEnvironmentCloudplatformsDiffer() throws IOException {
        when(crnUserDetailsService.loadUserByUsername(OWNER_CRN)).thenReturn(getCrnUser());
        allocateRequest.setCloudPlatform(CloudPlatform.AWS);
        allocateRequest.setEnvironmentCrn(ENVIRONMENT_CRN);
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.Builder.builder()
                .withCloudPlatform(UNKNOWN_CLOUD_PLATFORM)
                .build();
        when(environmentService.getByCrn(ENVIRONMENT_CRN)).thenReturn(environment);
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Cloud platform of the request AWS and the environment " + UNKNOWN_CLOUD_PLATFORM + " do not match.");

        underTest.convert(allocateRequest, OWNER_CRN);
    }

    @Test
    public void testConversionWhenUnsupportedCloudplatform() throws IOException {
        when(crnUserDetailsService.loadUserByUsername(OWNER_CRN)).thenReturn(getCrnUser());
        allocateRequest.setCloudPlatform(CloudPlatform.YARN);
        allocateRequest.setEnvironmentCrn(ENVIRONMENT_CRN);
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.Builder.builder()
                .withCloudPlatform(CloudPlatform.YARN.name())
                .build();
        when(environmentService.getByCrn(ENVIRONMENT_CRN)).thenReturn(environment);
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Cloud platform YARN not supported yet.");

        underTest.convert(allocateRequest, OWNER_CRN);
    }

    private void setupAllocateRequest(boolean provideOptionalFields) {

        allocateRequest.setEnvironmentCrn(ENVIRONMENT_CRN);
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

        databaseServerRequest.setInstanceType("db.m3.medium");
        databaseServerRequest.setDatabaseVendor("postgres");
        databaseServerRequest.setConnectionDriver("org.postgresql.Driver");
        databaseServerRequest.setStorageSize(50L);
        if (provideOptionalFields) {
            databaseServerRequest.setRootUserName("root");
            databaseServerRequest.setRootUserPassword("cloudera");
        }
        setupProviderCalculatorResponse(allocateRequest, ALLOCATE_REQUEST_PARAMETERS);
        setupProviderCalculatorResponse(databaseServerRequest, DATABASE_SERVER_REQUEST_PARAMETERS);

        securityGroupRequest.setSecurityGroupIds(Set.of("sg-1234"));
    }

    private void setupProviderCalculatorResponse(ProviderParametersBase request, Map<String, Object> response) {
        MappableBase providerCalculatorResponse = mock(MappableBase.class);
        when(providerCalculatorResponse.asMap()).thenReturn(response);
        when(providerParameterCalculator.get(request)).thenReturn(providerCalculatorResponse);
    }

}


