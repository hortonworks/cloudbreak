package com.sequenceiq.redbeams.converter.stack;

import static com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag.CB_CREATION_TIMESTAMP;
import static com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag.CB_USER_NAME;
import static com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag.CB_VERSION;
import static com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag.OWNER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.MappableBase;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParametersBase;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.LocationResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.AwsNetworkV4Parameters;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.NetworkV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.SecurityGroupV4Request;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.service.EnvironmentService;
import com.sequenceiq.redbeams.service.UserGeneratorService;
import com.sequenceiq.redbeams.service.network.NetworkParameterFactoryService;
import com.sequenceiq.redbeams.service.network.SubnetChooserService;

public class AllocateDatabaseServerV4RequestToDBStackConverterTest {

    private static final String OWNER_CRN = "crn:altus:iam:us-west-1:cloudera:user:bob@cloudera.com";

    private static final String VERSION = "1.2.3.4";

    private static final Instant NOW = Instant.now();

    private static final Map<String, Object> ALLOCATE_REQUEST_PARAMETERS = Map.of("key", "value");

    private static final Map<String, Object> NETWORK_REQUEST_PARAMETERS = Map.of("netkey", "netvalue");

    private static final Map<String, Object> DATABASE_SERVER_REQUEST_PARAMETERS = Map.of("dbkey", "dbvalue");

    private static final String PASSWORD = "password";

    private static final String USERNAME = "username";

    @Mock
    private EnvironmentService environmentService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ProviderParameterCalculator providerParameterCalculator;

    @Mock
    private Clock clock;

    @Mock
    private SubnetChooserService subnetChooserService;

    @Mock
    private UserGeneratorService userGeneratorService;

    @Mock
    private NetworkParameterFactoryService networkParameterFactoryService;

    @InjectMocks
    private AllocateDatabaseServerV4RequestToDBStackConverter underTest;

    private AllocateDatabaseServerV4Request allocateRequest;

    private NetworkV4Request networkRequest;

    private DatabaseServerV4Request databaseServerRequest;

    private SecurityGroupV4Request securityGroupRequest;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        ReflectionTestUtils.setField(underTest, "version", VERSION);

        allocateRequest = new AllocateDatabaseServerV4Request();

        networkRequest = new NetworkV4Request();
        allocateRequest.setNetwork(networkRequest);

        databaseServerRequest = new DatabaseServerV4Request();
        allocateRequest.setDatabaseServer(databaseServerRequest);

        securityGroupRequest = new SecurityGroupV4Request();
        databaseServerRequest.setSecurityGroup(securityGroupRequest);

        when(clock.getCurrentInstant()).thenReturn(NOW);
    }

    @Test
    public void testConversionWhenOptionalElementsAreProvided() throws IOException {
        setupAllocateRequest(true);

        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.Builder.builder()
                .withCloudPlatform(CloudPlatform.AWS.name())
                .withLocation(LocationResponse.LocationResponseBuilder.aLocationResponse().withName("myRegion").build())
                .build();
        when(environmentService.getByCrn("myenv")).thenReturn(environment);

        DBStack dbStack = underTest.convert(allocateRequest, OWNER_CRN);

        assertEquals(allocateRequest.getName(), dbStack.getName());
        assertEquals(allocateRequest.getEnvironmentCrn(), dbStack.getEnvironmentId());
//        assertEquals(allocateRequest.getRegion(), dbStack.getRegion());
        assertEquals(CloudPlatform.AWS.name(), dbStack.getCloudPlatform());
        assertEquals(CloudPlatform.AWS.name(), dbStack.getPlatformVariant());
        assertEquals(1, dbStack.getParameters().size());
        assertEquals("value", dbStack.getParameters().get("key"));
        assertEquals(Crn.safeFromString(OWNER_CRN), dbStack.getOwnerCrn());

        Json tags = dbStack.getTags();
        StackTags stackTags = tags.get(StackTags.class);
        Map<String, String> defaultTags = stackTags.getDefaultTags();
        assertEquals("bob@cloudera.com", defaultTags.get(CB_USER_NAME.key()));
        assertEquals("bob@cloudera.com", defaultTags.get(OWNER.key()));
        assertEquals(String.valueOf(NOW.getEpochSecond()), defaultTags.get(CB_CREATION_TIMESTAMP.key()));
        assertEquals(VERSION, defaultTags.get(CB_VERSION.key()));

        assertEquals(Status.REQUESTED, dbStack.getStatus());
        assertEquals(DetailedDBStackStatus.PROVISION_REQUESTED, dbStack.getDBStackStatus().getDetailedDBStackStatus());
        assertEquals(NOW.toEpochMilli(), dbStack.getDBStackStatus().getCreated().longValue());

        assertNotNull(dbStack.getNetwork().getName());
        assertEquals(1, dbStack.getNetwork().getAttributes().getMap().size());
        assertEquals("netvalue", dbStack.getNetwork().getAttributes().getMap().get("netkey"));

        assertNotNull(dbStack.getDatabaseServer().getName());
        assertEquals(databaseServerRequest.getInstanceType(), dbStack.getDatabaseServer().getInstanceType());
        assertEquals(DatabaseVendor.fromValue(databaseServerRequest.getDatabaseVendor()), dbStack.getDatabaseServer().getDatabaseVendor());
        assertEquals(databaseServerRequest.getStorageSize(), dbStack.getDatabaseServer().getStorageSize());
        assertEquals(databaseServerRequest.getRootUserName(), dbStack.getDatabaseServer().getRootUserName());
        assertEquals(databaseServerRequest.getRootUserPassword(), dbStack.getDatabaseServer().getRootPassword());
        assertEquals(1, dbStack.getDatabaseServer().getAttributes().getMap().size());
        assertEquals("dbvalue", dbStack.getDatabaseServer().getAttributes().getMap().get("dbkey"));

        assertEquals(securityGroupRequest.getSecurityGroupIds(), dbStack.getDatabaseServer().getSecurityGroup().getSecurityGroupIds());
        verify(providerParameterCalculator).get(allocateRequest);
        verify(providerParameterCalculator).get(networkRequest);
        verify(subnetChooserService, never()).chooseSubnetsFromDifferentAzs(anyList());
        verify(networkParameterFactoryService, never()).createNetworkParameters(anyList(), anyString());
        verify(userGeneratorService, never()).generatePassword();
        verify(userGeneratorService, never()).generateUserName();
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
                .withLocation(LocationResponse.LocationResponseBuilder.aLocationResponse().withName("myRegion").build())
                .withNetwork(EnvironmentNetworkResponse.EnvironmentNetworkResponseBuilder.anEnvironmentNetworkResponse()
                        .withSubnetMetas(
                                Map.of(
                                        "subnet-1", cloudSubnets.get(0),
                                        "subnet-2", cloudSubnets.get(1)
                                )
                        )
                        .build())
                .build();
        when(environmentService.getByCrn("myenv")).thenReturn(environment);
        when(subnetChooserService.chooseSubnetsFromDifferentAzs(any())).thenReturn(cloudSubnets);
        AwsNetworkV4Parameters awsNetworkV4Parameters = mock(AwsNetworkV4Parameters.class);
        when(awsNetworkV4Parameters.asMap()).thenReturn(NETWORK_REQUEST_PARAMETERS);
        when(networkParameterFactoryService.createNetworkParameters(any(), anyString())).thenReturn(awsNetworkV4Parameters);
        when(userGeneratorService.generatePassword()).thenReturn(PASSWORD);
        when(userGeneratorService.generateUserName()).thenReturn(USERNAME);

        DBStack dbStack = underTest.convert(allocateRequest, OWNER_CRN);

        assertEquals(PASSWORD, dbStack.getDatabaseServer().getRootPassword());
        assertEquals(USERNAME, dbStack.getDatabaseServer().getRootUserName());
        assertNotNull(dbStack.getNetwork().getName());
        assertEquals(1, dbStack.getNetwork().getAttributes().getMap().size());
        assertEquals("netvalue", dbStack.getNetwork().getAttributes().getMap().get("netkey"));
        verify(providerParameterCalculator).get(allocateRequest);
        verify(providerParameterCalculator, never()).get(networkRequest);
        verify(subnetChooserService).chooseSubnetsFromDifferentAzs(anyList());
        verify(networkParameterFactoryService).createNetworkParameters(anyList(), anyString());
        verify(userGeneratorService).generatePassword();
        verify(userGeneratorService).generateUserName();
    }

    private void setupAllocateRequest(boolean provideOptionalFields) {

        allocateRequest.setName("myallocation");
        allocateRequest.setEnvironmentCrn("myenv");
//        allocateRequest.setRegion("us-east-1");
        // allocateRequest.setCloudPlatform(CloudPlatform.AWS);
        if (provideOptionalFields) {
            AwsNetworkV4Parameters awsNetworkV4Parameters = new AwsNetworkV4Parameters();
            awsNetworkV4Parameters.setSubnetId("subnet-1,subnet-2");
            allocateRequest.getNetwork().setAws(awsNetworkV4Parameters);
            setupProviderCalculatorResponse(networkRequest, NETWORK_REQUEST_PARAMETERS);
        } else {
            allocateRequest.setNetwork(null);
        }

        databaseServerRequest.setInstanceType("db.m3.medium");
        databaseServerRequest.setDatabaseVendor("postgres");
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
