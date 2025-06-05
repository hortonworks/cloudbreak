package com.sequenceiq.cloudbreak.core.flow2.externaldatabase;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;
import static com.sequenceiq.common.model.AzureDatabaseType.FLEXIBLE_SERVER;
import static com.sequenceiq.common.model.AzureDatabaseType.SINGLE_SERVER;
import static com.sequenceiq.redbeams.rotation.RedbeamsSecretType.REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.core.AttemptResults;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.conf.ExternalDatabaseConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.UpgradeRdsContext;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.datalake.SdxClientService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.externaldatabase.DatabaseOperation;
import com.sequenceiq.cloudbreak.service.externaldatabase.DatabaseServerParameterDecorator;
import com.sequenceiq.cloudbreak.service.externaldatabase.PollingConfig;
import com.sequenceiq.cloudbreak.service.externaldatabase.model.DatabaseServerParameter;
import com.sequenceiq.cloudbreak.service.externaldatabase.model.DatabaseStackConfig;
import com.sequenceiq.cloudbreak.service.externaldatabase.model.DatabaseStackConfigKey;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsClientService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.common.model.DatabaseType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.LocationResponse;
import com.sequenceiq.environment.api.v1.platformresource.EnvironmentPlatformResourceEndpoint;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformDatabaseCapabilitiesResponse;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.api.model.StateStatus;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.RotateDatabaseServerSecretV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslConfigV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslMode;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.UpgradeDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.UpgradeTargetMajorVersion;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerStatusV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.UpgradeDatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseResponse;

@ExtendWith(MockitoExtension.class)
class ExternalDatabaseServiceTest {

    private static final CloudPlatform CLOUD_PLATFORM = CloudPlatform.AZURE;

    private static final String ACCOUNT_ID = "accountId";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:" + ACCOUNT_ID + ":environment:envResourceId";

    private static final String RDBMS_CRN = "rdbmsCRN";

    private static final String CLUSTER_CRN = "clusterCRN";

    private static final String RDBMS_FLOW_ID = "flowId";

    private static final String INSTANCE_TYPE = "instance";

    private static final String VENDOR = "vendor";

    private static final long VOLUME_SIZE = 1234L;

    private static final String BLUEPRINT_TEXT = "blueprintText";

    private static final String STACK_VERSION_GOOD_MINIMAL = "7.2.2";

    private static final String STACK_VERSION_GOOD = "7.2.15";

    private static final String STACK_VERSION_BAD = "7.2.1";

    private static final String UPGRADE_ERROR_REASON = "upgrade error happened";

    private static final String ROTATE_ERROR_REASON = "rotate error happened";

    @Mock
    private RedbeamsClientService redbeamsClient;

    @Mock
    private ClusterService clusterService;

    @Mock
    private Map<DatabaseStackConfigKey, DatabaseStackConfig> dbConfigs;

    @Mock
    private Map<CloudPlatform, DatabaseServerParameterDecorator> parameterDecoratorMap;

    @Mock
    private DatabaseObtainerService databaseObtainerService;

    @Mock
    private DatabaseServerParameterDecorator dbServerParameterDecorator;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ExternalDatabasePollingConfig dbPollConfig;

    @Mock
    private ExternalDatabaseConfig externalDatabaseConfig;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Mock
    private SdxClientService sdxClientService;

    @Mock
    private EnvironmentPlatformResourceEndpoint environmentPlatformResourceEndpoint;

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @Mock
    private EnvironmentService environmentClientService;

    @Mock
    private CloudbreakEventService cloudbreakEventService;

    @InjectMocks
    private ExternalDatabaseService underTest;

    private DetailedEnvironmentResponse environmentResponse;

    private DatabaseServerV4StackRequest databaseServerV4StackRequest;

    private static Object[][] dbUpgradeMigrationScenarios() {
        return new Object[][] {
                // ExternalDB, Platform, currentVersion, targetVersion, originalDBType, migrationRequired, datalake
                {true, "AZURE", "10", "11", SINGLE_SERVER, false, true},
                {true, "AZURE", "10", "14", SINGLE_SERVER, true, true},
                {true, "AZURE", "10", "14", SINGLE_SERVER, true, false},
                {true, "AZURE", "14", "14", FLEXIBLE_SERVER, false, false},
                {true, "AZURE", "14", "11", null, false, false},
                {true, "AWS", "10", "11", null, false, false},
                {true, "AWS", "10", "14", null, false, false},
                {true, "AWS", "14", "14", null, false, false},
                {true, "AWS", "14", "11", null, false, false},
                {false, "AZURE", "10", "14", SINGLE_SERVER, false, true},
                {true, "AZURE", "11", "14", FLEXIBLE_SERVER, true, true},
        };
    }

    @BeforeEach
    void setUp() {
        mockShortPollConfig();
        environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCloudPlatform(CLOUD_PLATFORM.name());
        environmentResponse.setCrn(ENV_CRN);
        LocationResponse locationResponse = new LocationResponse();
        locationResponse.setName("test");
        environmentResponse.setLocation(locationResponse);
        lenient().when(dbConfigs.get(new DatabaseStackConfigKey(CLOUD_PLATFORM, null)))
                .thenReturn(new DatabaseStackConfig(INSTANCE_TYPE, VENDOR, VOLUME_SIZE));
        lenient().when(parameterDecoratorMap.get(CLOUD_PLATFORM)).thenReturn(dbServerParameterDecorator);
        databaseServerV4StackRequest = new DatabaseServerV4StackRequest();
    }

    @Test
    void provisionDatabaseBadRequest() {
        Stack stack = new Stack();
        stack.setDatabase(createDatabase(DatabaseAvailabilityType.HA));
        stack.setCluster(new Cluster());
        when(redbeamsClient.create(any())).thenThrow(BadRequestException.class);
        when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(anyString(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(new PlatformDatabaseCapabilitiesResponse(new HashMap<>(), Map.of("test", "instanceType")));

        assertThatThrownBy(() -> underTest.provisionDatabase(stack, environmentResponse))
                .isInstanceOf(BadRequestException.class);
    }

    @ParameterizedTest
    @EnumSource(DatabaseAvailabilityType.class)
    void provisionDatabase(DatabaseAvailabilityType availability) throws JsonProcessingException {
        Assumptions.assumeTrue(!availability.isEmbedded());

        DatabaseServerStatusV4Response createResponse = new DatabaseServerStatusV4Response();
        createResponse.setResourceCrn(RDBMS_CRN);
        Cluster cluster = spy(new Cluster());
        Stack stack = new Stack();
        stack.setResourceCrn(CLUSTER_CRN);
        stack.setDatabase(createDatabase(availability));
        stack.setCluster(cluster);
        stack.setMultiAz(true);
        when(redbeamsClient.getByClusterCrn(nullable(String.class), nullable(String.class))).thenReturn(null);
        when(redbeamsClient.create(any())).thenReturn(createResponse);
        when(databaseObtainerService.obtainAttemptResult(eq(cluster), eq(DatabaseOperation.CREATION), eq(RDBMS_CRN), eq(true)))
                .thenReturn(AttemptResults.finishWith(new DatabaseServerV4Response()));
        when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(anyString(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(new PlatformDatabaseCapabilitiesResponse(new HashMap<>(), Map.of("test", "instanceType")));
        underTest.provisionDatabase(stack, environmentResponse);

        ArgumentCaptor<DatabaseServerParameter> serverParameterCaptor = ArgumentCaptor.forClass(DatabaseServerParameter.class);
        verify(redbeamsClient).getByClusterCrn(ENV_CRN, CLUSTER_CRN);
        verify(redbeamsClient).create(any(AllocateDatabaseServerV4Request.class));
        verify(cluster).setDatabaseServerCrn(RDBMS_CRN);
        verify(dbServerParameterDecorator).setParameters(any(), serverParameterCaptor.capture(), eq(environmentResponse), eq(true));
        verify(clusterService).save(cluster);
        DatabaseServerParameter paramValue = serverParameterCaptor.getValue();
        assertThat(paramValue.getAvailabilityType()).isEqualTo(availability);
    }

    @Test
    void provisionArmDatabase() throws JsonProcessingException {
        DatabaseServerStatusV4Response createResponse = new DatabaseServerStatusV4Response();
        createResponse.setResourceCrn(RDBMS_CRN);
        Cluster cluster = spy(new Cluster());
        Stack stack = new Stack();
        stack.setResourceCrn(CLUSTER_CRN);
        stack.setDatabase(createDatabase(DatabaseAvailabilityType.HA));
        stack.setCluster(cluster);
        stack.setMultiAz(true);
        stack.setArchitecture(Architecture.ARM64);
        when(redbeamsClient.getByClusterCrn(nullable(String.class), nullable(String.class))).thenReturn(null);
        when(redbeamsClient.create(any())).thenReturn(createResponse);
        when(databaseObtainerService.obtainAttemptResult(eq(cluster), eq(DatabaseOperation.CREATION), eq(RDBMS_CRN), eq(true)))
                .thenReturn(AttemptResults.finishWith(new DatabaseServerV4Response()));
        when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(anyString(), anyString(), anyString(), any(), any(), eq("arm64")))
                .thenReturn(new PlatformDatabaseCapabilitiesResponse(new HashMap<>(), Map.of("test", "armInstanceType")));
        underTest.provisionDatabase(stack, environmentResponse);

        ArgumentCaptor<DatabaseServerParameter> serverParameterCaptor = ArgumentCaptor.forClass(DatabaseServerParameter.class);
        verify(redbeamsClient).getByClusterCrn(ENV_CRN, CLUSTER_CRN);
        verify(redbeamsClient).create(any(AllocateDatabaseServerV4Request.class));
        verify(cluster).setDatabaseServerCrn(RDBMS_CRN);
        verify(dbServerParameterDecorator).setParameters(any(), serverParameterCaptor.capture(), eq(environmentResponse), eq(true));
        verify(clusterService).save(cluster);
        DatabaseServerParameter paramValue = serverParameterCaptor.getValue();
        assertThat(paramValue.getAvailabilityType()).isEqualTo(DatabaseAvailabilityType.HA);
    }

    @Test
    void provisionArmDatabaseFallbackToX86() throws JsonProcessingException {
        DatabaseServerStatusV4Response createResponse = new DatabaseServerStatusV4Response();
        createResponse.setResourceCrn(RDBMS_CRN);
        Cluster cluster = spy(new Cluster());
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setResourceCrn(CLUSTER_CRN);
        stack.setDatabase(createDatabase(DatabaseAvailabilityType.HA));
        stack.setCluster(cluster);
        stack.setMultiAz(true);
        stack.setArchitecture(Architecture.ARM64);
        when(redbeamsClient.getByClusterCrn(nullable(String.class), nullable(String.class))).thenReturn(null);
        when(redbeamsClient.create(any())).thenReturn(createResponse);
        when(databaseObtainerService.obtainAttemptResult(eq(cluster), eq(DatabaseOperation.CREATION), eq(RDBMS_CRN), eq(true)))
                .thenReturn(AttemptResults.finishWith(new DatabaseServerV4Response()));
        when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(anyString(), anyString(), anyString(), any(), any(), eq("arm64")))
                .thenReturn(new PlatformDatabaseCapabilitiesResponse(new HashMap<>(), new HashMap<>()));
        when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(anyString(), anyString(), anyString(), any(), any(), eq("x86_64")))
                .thenReturn(new PlatformDatabaseCapabilitiesResponse(new HashMap<>(), Map.of("test", "armInstanceType")));
        underTest.provisionDatabase(stack, environmentResponse);

        ArgumentCaptor<DatabaseServerParameter> serverParameterCaptor = ArgumentCaptor.forClass(DatabaseServerParameter.class);
        verify(redbeamsClient).getByClusterCrn(ENV_CRN, CLUSTER_CRN);
        verify(redbeamsClient).create(any(AllocateDatabaseServerV4Request.class));
        verify(cluster).setDatabaseServerCrn(RDBMS_CRN);
        verify(dbServerParameterDecorator).setParameters(any(), serverParameterCaptor.capture(), eq(environmentResponse), eq(true));
        verify(clusterService).save(cluster);
        verify(cloudbreakEventService).fireCloudbreakEvent(1L, Status.UPDATE_IN_PROGRESS.name(),
                ResourceEvent.DATABASE_ARM_NOT_AVAILABLE, List.of("test"));

        DatabaseServerParameter paramValue = serverParameterCaptor.getValue();
        assertThat(paramValue.getAvailabilityType()).isEqualTo(DatabaseAvailabilityType.HA);
    }

    @ParameterizedTest
    @EnumSource(DatabaseAvailabilityType.class)
    void provisionDatabaseWithDBEntity(DatabaseAvailabilityType availability) throws JsonProcessingException {
        Assumptions.assumeTrue(!availability.isEmbedded());

        DatabaseServerStatusV4Response createResponse = new DatabaseServerStatusV4Response();
        createResponse.setResourceCrn(RDBMS_CRN);
        Cluster cluster = spy(new Cluster());
        Stack stack = new Stack();
        stack.setResourceCrn(CLUSTER_CRN);
        Database database = new Database();
        database.setExternalDatabaseAvailabilityType(availability);
        stack.setDatabase(database);
        stack.setCluster(cluster);
        when(redbeamsClient.getByClusterCrn(nullable(String.class), nullable(String.class))).thenReturn(null);
        when(redbeamsClient.create(any())).thenReturn(createResponse);
        when(databaseObtainerService.obtainAttemptResult(eq(cluster), eq(DatabaseOperation.CREATION), eq(RDBMS_CRN), eq(true)))
                .thenReturn(AttemptResults.finishWith(new DatabaseServerV4Response()));
        when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(anyString(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(new PlatformDatabaseCapabilitiesResponse(new HashMap<>(), Map.of("test", "instanceType")));
        underTest.provisionDatabase(stack, environmentResponse);

        ArgumentCaptor<DatabaseServerParameter> serverParameterCaptor = ArgumentCaptor.forClass(DatabaseServerParameter.class);
        verify(redbeamsClient).getByClusterCrn(ENV_CRN, CLUSTER_CRN);
        verify(redbeamsClient).create(any(AllocateDatabaseServerV4Request.class));
        verify(cluster).setDatabaseServerCrn(RDBMS_CRN);
        verify(dbServerParameterDecorator).setParameters(any(), serverParameterCaptor.capture(), eq(environmentResponse), eq(false));
        verify(clusterService).save(cluster);
        DatabaseServerParameter paramValue = serverParameterCaptor.getValue();
        assertThat(paramValue.getAvailabilityType()).isEqualTo(availability);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void terminateDatabase(boolean forced) throws JsonProcessingException {
        Cluster cluster = new Cluster();
        cluster.setDatabaseServerCrn(RDBMS_CRN);
        DatabaseServerV4Response deleteResponse = new DatabaseServerV4Response();
        deleteResponse.setCrn(RDBMS_CRN);
        when(databaseObtainerService.obtainAttemptResult(eq(cluster), eq(DatabaseOperation.DELETION), eq(RDBMS_CRN), eq(false)))
                .thenReturn(AttemptResults.finishWith(new DatabaseServerV4Response()));
        when(redbeamsClient.deleteByCrn(any(), anyBoolean())).thenReturn(deleteResponse);

        underTest.terminateDatabase(cluster, DatabaseAvailabilityType.HA, environmentResponse, forced);

        ArgumentCaptor<Boolean> forceCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(redbeamsClient).deleteByCrn(eq(RDBMS_CRN), forceCaptor.capture());
        assertThat(forceCaptor.getValue()).isEqualTo(forced);
    }

    @Test
    void provisionDatabaseWhenAlreadyExists() throws JsonProcessingException {
        Cluster cluster = spy(new Cluster());
        Stack stack = new Stack();
        stack.setResourceCrn(CLUSTER_CRN);
        stack.setCluster(cluster);
        stack.setDatabase(createDatabase(DatabaseAvailabilityType.NON_HA));
        DatabaseServerV4Response dbServerResponse = new DatabaseServerV4Response();
        dbServerResponse.setCrn(RDBMS_CRN);
        when(redbeamsClient.getByClusterCrn(nullable(String.class), nullable(String.class))).thenReturn(dbServerResponse);
        when(databaseObtainerService.obtainAttemptResult(eq(cluster), eq(DatabaseOperation.CREATION), eq(RDBMS_CRN), eq(true)))
                .thenReturn(AttemptResults.finishWith(new DatabaseServerV4Response()));
        underTest.provisionDatabase(stack, environmentResponse);

        verify(redbeamsClient).getByClusterCrn(ENV_CRN, CLUSTER_CRN);
        verify(redbeamsClient, never()).create(any(AllocateDatabaseServerV4Request.class));
        verify(dbServerParameterDecorator, never()).setParameters(any(), any(), any(), anyBoolean());
        verify(cluster).setDatabaseServerCrn(RDBMS_CRN);
        verify(clusterService).save(cluster);
    }

    @Test
    void provisionDatabaseTestSslWhenUnsupportedCloudPlatform() throws JsonProcessingException {
        when(externalDatabaseConfig.isExternalDatabaseSslEnforcementSupportedFor(CLOUD_PLATFORM)).thenReturn(false);
        when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(anyString(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(new PlatformDatabaseCapabilitiesResponse(new HashMap<>(), Map.of("test", "instance")));

        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(BLUEPRINT_TEXT);
        when(cmTemplateProcessorFactory.get(BLUEPRINT_TEXT)).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getStackVersion()).thenReturn(STACK_VERSION_GOOD);

        provisionDatabaseTestSslInternal(blueprint, false);
    }

    private void provisionDatabaseTestSslInternal(Blueprint blueprint, boolean sslEnabledExpected) throws JsonProcessingException {
        DatabaseServerStatusV4Response createResponse = new DatabaseServerStatusV4Response();
        createResponse.setResourceCrn(RDBMS_CRN);
        Cluster cluster = new Cluster();
        Stack stack = new Stack();
        stack.setResourceCrn(CLUSTER_CRN);
        stack.setDatabase(createDatabase(DatabaseAvailabilityType.HA));
        Map<String, String> userDefinedTags = Map.ofEntries(entry("key1", "value1"), entry("key2", "value2"));
        StackTags stackTags = new StackTags(userDefinedTags, Map.of(), Map.of());
        stack.setTags(new Json(stackTags));
        cluster.setBlueprint(blueprint);
        cluster.setEnvironmentCrn(ENV_CRN);
        cluster.setDbSslEnabled(sslEnabledExpected);
        stack.setCluster(cluster);

        when(redbeamsClient.getByClusterCrn(ENV_CRN, CLUSTER_CRN)).thenReturn(null);
        when(redbeamsClient.create(any(AllocateDatabaseServerV4Request.class))).thenReturn(createResponse);
        when(databaseObtainerService.obtainAttemptResult(cluster, DatabaseOperation.CREATION, RDBMS_CRN, true))
                .thenReturn(AttemptResults.finishWith(new DatabaseServerV4Response()));

        underTest.provisionDatabase(stack, environmentResponse);

        assertThat(cluster.getDatabaseServerCrn()).isEqualTo(RDBMS_CRN);
        verify(clusterService).save(cluster);

        ArgumentCaptor<DatabaseServerV4StackRequest> databaseServerCaptor = ArgumentCaptor.forClass(DatabaseServerV4StackRequest.class);
        ArgumentCaptor<DatabaseServerParameter> serverParameterCaptor = ArgumentCaptor.forClass(DatabaseServerParameter.class);
        verify(dbServerParameterDecorator).setParameters(databaseServerCaptor.capture(), serverParameterCaptor.capture(), eq(environmentResponse), eq(false));

        DatabaseServerParameter serverParameter = serverParameterCaptor.getValue();
        assertThat(serverParameter).isNotNull();
        assertThat(serverParameter.getAvailabilityType()).isEqualTo(DatabaseAvailabilityType.HA);

        ArgumentCaptor<AllocateDatabaseServerV4Request> allocateRequestCaptor = ArgumentCaptor.forClass(AllocateDatabaseServerV4Request.class);
        verify(redbeamsClient).create(allocateRequestCaptor.capture());
        AllocateDatabaseServerV4Request allocateRequest = allocateRequestCaptor.getValue();
        assertThat(allocateRequest).isNotNull();
        assertThat(allocateRequest.getEnvironmentCrn()).isEqualTo(ENV_CRN);
        assertThat(allocateRequest.getClusterCrn()).isEqualTo(CLUSTER_CRN);

        DatabaseServerV4StackRequest databaseServer = databaseServerCaptor.getValue();
        assertThat(allocateRequest.getDatabaseServer()).isSameAs(databaseServer);
        assertThat(databaseServer).isNotNull();
        assertThat(databaseServer.getInstanceType()).isEqualTo(INSTANCE_TYPE);
        assertThat(databaseServer.getDatabaseVendor()).isEqualTo(VENDOR);
        assertThat(databaseServer.getStorageSize()).isEqualTo(VOLUME_SIZE);

        Map<String, String> tags = allocateRequest.getTags();
        assertThat(tags).isNotNull();
        assertThat(tags).isEqualTo(userDefinedTags);

        SslConfigV4Request sslConfig = allocateRequest.getSslConfig();
        if (sslEnabledExpected) {
            assertThat(sslConfig).isNotNull();
            assertThat(sslConfig.getSslMode()).isEqualTo(SslMode.ENABLED);
        } else {
            assertThat(sslConfig).isNull();
        }
    }

    @Test
    void provisionDatabaseTestSslWhenNoBlueprint() throws JsonProcessingException {
        when(externalDatabaseConfig.isExternalDatabaseSslEnforcementSupportedFor(CLOUD_PLATFORM)).thenReturn(true);
        when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(anyString(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(new PlatformDatabaseCapabilitiesResponse(new HashMap<>(), Map.of("test", "instance")));

        provisionDatabaseTestSslInternal(null, false);

        verify(cmTemplateProcessorFactory, never()).get(anyString());
        verify(cmTemplateProcessor, never()).getStackVersion();
    }

    @Test
    void provisionDatabaseTestSslWhenNoBlueprintText() throws JsonProcessingException {
        when(externalDatabaseConfig.isExternalDatabaseSslEnforcementSupportedFor(CLOUD_PLATFORM)).thenReturn(true);
        when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(anyString(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(new PlatformDatabaseCapabilitiesResponse(new HashMap<>(), Map.of("test", "instance")));

        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(null);

        provisionDatabaseTestSslInternal(blueprint, false);

        verify(cmTemplateProcessorFactory, never()).get(anyString());
        verify(cmTemplateProcessor, never()).getStackVersion();
    }

    @ParameterizedTest(name = "runtime={0}")
    @ValueSource(strings = {"", " ", STACK_VERSION_BAD})
    @NullSource
    void provisionDatabaseTestSslWhenBadRuntime(String runtime) throws JsonProcessingException {
        when(externalDatabaseConfig.isExternalDatabaseSslEnforcementSupportedFor(CLOUD_PLATFORM)).thenReturn(true);
        when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(anyString(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(new PlatformDatabaseCapabilitiesResponse(new HashMap<>(), Map.of("test", "instance")));
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(BLUEPRINT_TEXT);
        when(cmTemplateProcessorFactory.get(BLUEPRINT_TEXT)).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getStackVersion()).thenReturn(runtime);

        provisionDatabaseTestSslInternal(blueprint, false);
    }

    @ParameterizedTest(name = "runtime={0}")
    @ValueSource(strings = {STACK_VERSION_GOOD_MINIMAL, STACK_VERSION_GOOD})
    void provisionDatabaseTestSslWhenSslEnabled(String runtime) throws JsonProcessingException {
        when(externalDatabaseConfig.isExternalDatabaseSslEnforcementSupportedFor(CLOUD_PLATFORM)).thenReturn(true);
        when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(anyString(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(new PlatformDatabaseCapabilitiesResponse(new HashMap<>(), Map.of("test", "instance")));

        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(BLUEPRINT_TEXT);
        when(cmTemplateProcessorFactory.get(BLUEPRINT_TEXT)).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getStackVersion()).thenReturn(runtime);

        provisionDatabaseTestSslInternal(blueprint, true);
    }

    @Test
    void terminateDatabaseWhenCrnNull() {
        Cluster cluster = new Cluster();
        cluster.setDatabaseServerCrn(null);

        underTest.terminateDatabase(cluster, DatabaseAvailabilityType.HA, environmentResponse, true);

        verify(redbeamsClient, never()).deleteByCrn(anyString(), anyBoolean());
    }

    @Test
    void startDatabase() throws JsonProcessingException {
        Cluster cluster = new Cluster();
        cluster.setDatabaseServerCrn(RDBMS_CRN);
        DatabaseServerV4Response deleteResponse = new DatabaseServerV4Response();
        deleteResponse.setCrn(RDBMS_CRN);
        when(databaseObtainerService.obtainAttemptResult(eq(cluster), eq(DatabaseOperation.START), eq(RDBMS_CRN), eq(false)))
                .thenReturn(AttemptResults.finishWith(deleteResponse));

        underTest.startDatabase(cluster, DatabaseAvailabilityType.HA, environmentResponse);

        verify(redbeamsClient).startByCrn(RDBMS_CRN);
    }

    @Test
    void startDatabaseWhenCrnNull() {
        Cluster cluster = new Cluster();
        cluster.setDatabaseServerCrn(null);

        underTest.startDatabase(cluster, DatabaseAvailabilityType.HA, environmentResponse);

        verify(redbeamsClient, never()).startByCrn(anyString());
    }

    @Test
    void stopDatabase() throws JsonProcessingException {
        Cluster cluster = spy(new Cluster());
        cluster.setDatabaseServerCrn(RDBMS_CRN);

        when(databaseObtainerService.obtainAttemptResult(eq(cluster), eq(DatabaseOperation.STOP), eq(RDBMS_CRN), eq(false)))
                .thenReturn(AttemptResults.finishWith(new DatabaseServerV4Response()));

        underTest.stopDatabase(cluster, DatabaseAvailabilityType.HA, environmentResponse);

        verify(redbeamsClient).stopByCrn(RDBMS_CRN);
    }

    @Test
    void stopDatabaseWhenCrnNull() {
        Cluster cluster = spy(new Cluster());
        cluster.setDatabaseServerCrn(null);

        underTest.stopDatabase(cluster, DatabaseAvailabilityType.HA, environmentResponse);

        verify(redbeamsClient, never()).stopByCrn(anyString());
    }

    @Test
    void upgradeDatabase() {
        Cluster cluster = spy(new Cluster());
        cluster.setDatabaseServerCrn(RDBMS_CRN);

        UpgradeTargetMajorVersion targetMajorVersion = UpgradeTargetMajorVersion.VERSION_11;

        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, RDBMS_FLOW_ID);
        UpgradeDatabaseServerV4Response response = new UpgradeDatabaseServerV4Response();
        response.setFlowIdentifier(new FlowIdentifier(FlowType.FLOW, RDBMS_FLOW_ID));
        when(redbeamsClient.upgradeByCrn(eq(RDBMS_CRN), any())).thenReturn(response);

        FlowIdentifier actualResult = underTest.upgradeDatabase(cluster, targetMajorVersion, databaseServerV4StackRequest);

        ArgumentCaptor<UpgradeDatabaseServerV4Request> argumentCaptor = ArgumentCaptor.forClass(UpgradeDatabaseServerV4Request.class);
        verify(redbeamsClient).upgradeByCrn(eq(RDBMS_CRN), argumentCaptor.capture());
        assertEquals(targetMajorVersion, argumentCaptor.getValue().getUpgradeTargetMajorVersion());
        assertEquals(flowIdentifier, actualResult);
    }

    @Test
    void upgradeDatabaseNotFound() {
        Cluster cluster = spy(new Cluster());
        cluster.setDatabaseServerCrn(RDBMS_CRN);

        UpgradeTargetMajorVersion targetMajorVersion = UpgradeTargetMajorVersion.VERSION_11;

        when(redbeamsClient.upgradeByCrn(eq(RDBMS_CRN), any())).thenThrow(new NotFoundException("Not found"));

        underTest.upgradeDatabase(cluster, targetMajorVersion, databaseServerV4StackRequest);

        ArgumentCaptor<UpgradeDatabaseServerV4Request> argumentCaptor = ArgumentCaptor.forClass(UpgradeDatabaseServerV4Request.class);
        verify(redbeamsClient).upgradeByCrn(eq(RDBMS_CRN), argumentCaptor.capture());
        assertEquals(targetMajorVersion, argumentCaptor.getValue().getUpgradeTargetMajorVersion());
    }

    @Test
    void upgradeDatabaseNoFlowId() {
        Cluster cluster = spy(new Cluster());
        cluster.setDatabaseServerCrn(RDBMS_CRN);

        UpgradeTargetMajorVersion targetMajorVersion = UpgradeTargetMajorVersion.VERSION_11;

        UpgradeDatabaseServerV4Response response = new UpgradeDatabaseServerV4Response();
        response.setFlowIdentifier(null);
        when(redbeamsClient.upgradeByCrn(eq(RDBMS_CRN), any())).thenReturn(response);

        FlowIdentifier flowIdentifier = underTest.upgradeDatabase(cluster, targetMajorVersion, databaseServerV4StackRequest);

        ArgumentCaptor<UpgradeDatabaseServerV4Request> argumentCaptor = ArgumentCaptor.forClass(UpgradeDatabaseServerV4Request.class);
        verify(redbeamsClient).upgradeByCrn(eq(RDBMS_CRN), argumentCaptor.capture());
        assertEquals(targetMajorVersion, argumentCaptor.getValue().getUpgradeTargetMajorVersion());
        assertNull(flowIdentifier);
    }

    @Test
    void testWaitForDatabaseFlowToBeFinished() {
        Cluster cluster = new Cluster();
        cluster.setDatabaseServerCrn(RDBMS_CRN);
        when(redbeamsClient.hasFlowRunningByFlowId(RDBMS_FLOW_ID)).thenReturn(
                createFlowCheckResponse(Boolean.TRUE, Boolean.FALSE),
                createFlowCheckResponse(Boolean.FALSE, Boolean.FALSE));

        underTest.waitForDatabaseFlowToBeFinished(cluster, new FlowIdentifier(FlowType.FLOW, RDBMS_FLOW_ID));

        verify(redbeamsClient, times(2)).hasFlowRunningByFlowId(RDBMS_FLOW_ID);
        verify(redbeamsClient).getByCrn(any());
    }

    @Test
    void testWaitForDatabaseFlowToBeFinishedWhenFlowFailed() {
        Cluster cluster = new Cluster();
        cluster.setDatabaseServerCrn(RDBMS_CRN);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, RDBMS_FLOW_ID);
        DatabaseServerV4Response dbServerResponse = new DatabaseServerV4Response();
        dbServerResponse.setCrn(RDBMS_CRN);
        dbServerResponse.setStatusReason(UPGRADE_ERROR_REASON);

        when(redbeamsClient.getByCrn(RDBMS_CRN)).thenReturn(dbServerResponse);
        when(redbeamsClient.hasFlowRunningByFlowId(RDBMS_FLOW_ID)).thenReturn(createFlowCheckResponse(Boolean.FALSE, Boolean.TRUE));

        CloudbreakServiceException cloudbreakServiceException = assertThrows(CloudbreakServiceException.class,
                () -> underTest.waitForDatabaseFlowToBeFinished(cluster, flowIdentifier));

        String expected = String.format("Database flow failed in Redbeams with error: 'upgrade error happened'. "
                        + "Database crn: %s, flow: FlowIdentifier{type=FLOW, pollableId='%s'}",
                RDBMS_CRN, RDBMS_FLOW_ID);
        assertEquals(expected, cloudbreakServiceException.getMessage());
        verify(redbeamsClient, times(1)).hasFlowRunningByFlowId(RDBMS_FLOW_ID);
        verify(redbeamsClient, times(1)).getByCrn(any());
    }

    @Test
    void testWaitForDatabaseFlowToBeFinishedWhenFlowFailedAndNoDbFound() {
        Cluster cluster = new Cluster();
        cluster.setDatabaseServerCrn(RDBMS_CRN);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, RDBMS_FLOW_ID);

        when(redbeamsClient.getByCrn(RDBMS_CRN)).thenThrow(new NotFoundException("notfound"));
        when(redbeamsClient.hasFlowRunningByFlowId(RDBMS_FLOW_ID)).thenReturn(
                createFlowCheckResponse(Boolean.TRUE, Boolean.FALSE),
                createFlowCheckResponse(Boolean.FALSE, Boolean.TRUE));

        CloudbreakServiceException cloudbreakServiceException = assertThrows(CloudbreakServiceException.class,
                () -> underTest.waitForDatabaseFlowToBeFinished(cluster, flowIdentifier));

        String expected = String.format("Database flow failed in Redbeams with error: 'notfound'. "
                        + "Database crn: %s, flow: FlowIdentifier{type=FLOW, pollableId='%s'}",
                RDBMS_CRN, RDBMS_FLOW_ID);
        assertEquals(expected, cloudbreakServiceException.getMessage());
        verify(redbeamsClient, times(2)).hasFlowRunningByFlowId(RDBMS_FLOW_ID);
        verify(redbeamsClient, times(1)).getByCrn(RDBMS_CRN);
    }

    @Test
    void testWaitForDatabaseFlowToBeFinishedWhenNoFlowId() {
        Cluster cluster = spy(new Cluster());
        cluster.setDatabaseServerCrn(RDBMS_CRN);

        underTest.waitForDatabaseFlowToBeFinished(cluster, null);

        verify(redbeamsClient, never()).hasFlowRunningByFlowId(any());
        verify(redbeamsClient, never()).getByCrn(any());
    }

    @ParameterizedTest
    @MethodSource("dbUpgradeMigrationScenarios")
    void testIsMigrationNeededDuringUpgrade(boolean externalDb, String platform, String current, String target, DatabaseType databaseType,
            boolean migrationRequired) {
        StackView stack = mock(StackView.class);
        ClusterView clusterView = mock(ClusterView.class);
        when(clusterView.hasExternalDatabase()).thenReturn(externalDb);
        Database database = new Database();
        database.setExternalDatabaseEngineVersion(current);
        TargetMajorVersion majorVersion = mock(TargetMajorVersion.class);
        UpgradeRdsContext upgradeRdsContext = new UpgradeRdsContext(null, stack, clusterView, database, majorVersion);
        when(stack.getCloudPlatform()).thenReturn(platform);
        when(majorVersion.getMajorVersion()).thenReturn(target);
        when(parameterDecoratorMap.get(CloudPlatform.valueOf(platform))).thenReturn(dbServerParameterDecorator);
        doReturn(Optional.ofNullable(databaseType)).when(dbServerParameterDecorator).getDatabaseType(any());
        boolean actualMigrationNeeded = underTest.isMigrationNeededDuringUpgrade(upgradeRdsContext);
        assertEquals(actualMigrationNeeded, migrationRequired);
    }

    @ParameterizedTest
    @MethodSource("dbUpgradeMigrationScenarios")
    void testMigrateDatabaseSettingsAzureBothConditionsMetThenMigration(boolean externalDb, String platform, String current, String target,
            DatabaseType databaseType, boolean migrationRequired, boolean datalake) {
        StackDto stack = mock(StackDto.class);
        TargetMajorVersion majorVersion = mock(TargetMajorVersion.class);

        when(stack.getCloudPlatform()).thenReturn(platform);
        when(majorVersion.getMajorVersion()).thenReturn(target);
        when(stack.getExternalDatabaseEngineVersion()).thenReturn(current);
        ClusterView clusterView = mock(ClusterView.class);
        when(clusterView.hasExternalDatabase()).thenReturn(externalDb);
        when(stack.getCluster()).thenReturn(clusterView);
        when(stack.getDatabase()).thenReturn(new Database());
        when(parameterDecoratorMap.get(CloudPlatform.valueOf(platform))).thenReturn(dbServerParameterDecorator);
        doReturn(Optional.ofNullable(databaseType)).when(dbServerParameterDecorator).getDatabaseType(any());
        lenient().when(dbConfigs.get(new DatabaseStackConfigKey(CloudPlatform.valueOf(platform), databaseType)))
                .thenReturn(new DatabaseStackConfig(INSTANCE_TYPE, VENDOR, VOLUME_SIZE));
        when(stack.getEnvironmentCrn()).thenReturn(ENV_CRN);
        when(environmentClientService.getByCrn(ENV_CRN)).thenReturn(environmentResponse);
        if (migrationRequired) {
            lenient().when(stack.getExternalDatabaseCreationType()).thenReturn(DatabaseAvailabilityType.HA);
            Stack realStack = new Stack();
            realStack.setMultiAz(true);
            realStack.setType(datalake ? StackType.DATALAKE : StackType.WORKLOAD);
            when(stack.getStack()).thenReturn(realStack);
            SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
            SdxDatabaseResponse databaseResponse = new SdxDatabaseResponse();
            databaseResponse.setAvailabilityType(SdxDatabaseAvailabilityType.HA);
            sdxClusterResponse.setSdxDatabaseResponse(databaseResponse);
            lenient().when(sdxClientService.getByCrnInternal(any())).thenReturn(sdxClusterResponse);
            when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(anyString(), anyString(), anyString(), any(), any(), any()))
                    .thenReturn(new PlatformDatabaseCapabilitiesResponse(new HashMap<>(), Map.of("test", "instanceType")));
        }

        DatabaseServerV4StackRequest result = underTest.migrateDatabaseSettingsIfNeeded(stack, majorVersion);

        if (migrationRequired) {
            DatabaseServerParameter serverParameter = DatabaseServerParameter.builder()
                    .withEngineVersion(target)
                    .withAttributes(Map.of(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, FLEXIBLE_SERVER.name()))
                    .withAvailabilityType(DatabaseAvailabilityType.HA)
                    .build();
            assertNotNull(result);
            verify(dbServerParameterDecorator).setParameters(eq(result), eq(serverParameter), eq(environmentResponse), eq(true));
        } else {
            assertNull(result);
            verify(dbServerParameterDecorator, never()).setParameters(any(), any(), any(), anyBoolean());
        }
    }

    @Test
    void rotateDatabaseSecretsShouldFailIfRedbeamsFlowChainIsNotTriggered() {
        when(redbeamsClient.rotateSecret(any())).thenReturn(new FlowIdentifier(FlowType.NOT_TRIGGERED, null));
        CloudbreakServiceException cloudbreakServiceException = assertThrows(CloudbreakServiceException.class,
                () -> underTest.rotateDatabaseSecret(RDBMS_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD, ROTATE, null));
        String expected = String.format("Database flow failed in Redbeams with error: 'Flow null not triggered'. "
                        + "Database crn: %s, flow: FlowIdentifier{type=%s, pollableId='%s'}",
                RDBMS_CRN, FlowType.NOT_TRIGGERED, null);
        assertEquals(expected, cloudbreakServiceException.getMessage());
    }

    @Test
    void rotateDatabaseSecretsShouldFailIfReturnedFlowInformationIsNull() {
        when(redbeamsClient.rotateSecret(any())).thenReturn(null);
        CloudbreakServiceException cloudbreakServiceException = assertThrows(CloudbreakServiceException.class,
                () -> underTest.rotateDatabaseSecret(RDBMS_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD, ROTATE, null));
        String expected = String.format("Database flow failed in Redbeams with error: 'unknown'. Database crn: %s, flow: null", RDBMS_CRN);
        assertEquals(expected, cloudbreakServiceException.getMessage());
    }

    @Test
    void rotateDatabaseSecretsShouldFailIfRedbeamsFlowChainFailed() {
        when(redbeamsClient.rotateSecret(any())).thenReturn(new FlowIdentifier(FlowType.FLOW_CHAIN, RDBMS_FLOW_ID));
        when(redbeamsClient.hasFlowChainRunningByFlowChainId(RDBMS_FLOW_ID)).thenReturn(createFlowCheckResponse(Boolean.FALSE, Boolean.TRUE));
        DatabaseServerV4Response dbServerResponse = new DatabaseServerV4Response();
        dbServerResponse.setCrn(RDBMS_CRN);
        dbServerResponse.setStatusReason(ROTATE_ERROR_REASON);
        when(redbeamsClient.getByCrn(RDBMS_CRN)).thenReturn(dbServerResponse);

        CloudbreakServiceException cloudbreakServiceException = assertThrows(CloudbreakServiceException.class,
                () -> underTest.rotateDatabaseSecret(RDBMS_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD, ROTATE, null));

        String expected = String.format("Database flow failed in Redbeams with error: '%s'. Database crn: %s, "
                        + "flow: FlowIdentifier{type=%s, pollableId='%s'}",
                ROTATE_ERROR_REASON, RDBMS_CRN, FlowType.FLOW_CHAIN, RDBMS_FLOW_ID);
        assertEquals(expected, cloudbreakServiceException.getMessage());
    }

    @Test
    void rotateDatabaseSecretsShouldSucceed() {
        when(redbeamsClient.rotateSecret(any())).thenReturn(new FlowIdentifier(FlowType.FLOW_CHAIN, RDBMS_FLOW_ID));
        when(redbeamsClient.hasFlowChainRunningByFlowChainId(RDBMS_FLOW_ID)).thenReturn(createFlowCheckResponse(Boolean.FALSE, Boolean.FALSE));
        underTest.rotateDatabaseSecret(RDBMS_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD, ROTATE, null);
        ArgumentCaptor<RotateDatabaseServerSecretV4Request> requestCaptor = ArgumentCaptor.forClass(RotateDatabaseServerSecretV4Request.class);
        verify(redbeamsClient, times(1)).rotateSecret(requestCaptor.capture());
        RotateDatabaseServerSecretV4Request request = requestCaptor.getValue();
        assertEquals(RDBMS_CRN, request.getCrn());
        assertEquals(REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD.name(), request.getSecret());
        assertEquals(ROTATE, request.getExecutionType());
    }

    @Test
    void preValidateShouldFailIfDatabaseCrnIsNull() {
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class, () -> underTest.preValidateDatabaseSecretRotation(null));
        assertEquals("No database server crn found, rotation is not possible.", secretRotationException.getMessage());
        verify(redbeamsClient, never()).getLastFlowId(eq(RDBMS_CRN));
    }

    @Test
    void preValidateShouldFailIfLastFlowIsRunningInRedbeams() {
        FlowLogResponse lastFlow = new FlowLogResponse();
        lastFlow.setStateStatus(StateStatus.PENDING);
        lastFlow.setCurrentState("currentState");
        when(redbeamsClient.getLastFlowId(eq(RDBMS_CRN))).thenReturn(lastFlow);
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.preValidateDatabaseSecretRotation(RDBMS_CRN));
        assertEquals("Polling in Redbeams is not possible since last known state of flow for the database is currentState",
                secretRotationException.getMessage());
        verify(redbeamsClient, times(1)).getLastFlowId(eq(RDBMS_CRN));
    }

    @Test
    void preValidateShouldSucceedIfLastFlowIsMissingInRedbeams() {
        when(redbeamsClient.getLastFlowId(eq(RDBMS_CRN))).thenReturn(null);
        underTest.preValidateDatabaseSecretRotation(RDBMS_CRN);
        verify(redbeamsClient, times(1)).getLastFlowId(eq(RDBMS_CRN));
    }

    @Test
    void preValidateShouldSucceedIfLastFlowIsNotRunningInRedbeams() {
        FlowLogResponse lastFlow = new FlowLogResponse();
        lastFlow.setStateStatus(StateStatus.SUCCESSFUL);
        when(redbeamsClient.getLastFlowId(eq(RDBMS_CRN))).thenReturn(lastFlow);
        underTest.preValidateDatabaseSecretRotation(RDBMS_CRN);
        verify(redbeamsClient, times(1)).getLastFlowId(eq(RDBMS_CRN));
    }

    @Test
    void upgradeDatabaseWhenCrnNull() {
        Cluster cluster = spy(new Cluster());
        cluster.setDatabaseServerCrn(null);
        UpgradeTargetMajorVersion targetMajorVersion = UpgradeTargetMajorVersion.VERSION_11;

        underTest.upgradeDatabase(cluster, targetMajorVersion, databaseServerV4StackRequest);

        verify(redbeamsClient, never()).upgradeByCrn(anyString(), any());
    }

    @ParameterizedTest
    @EnumSource(SdxDatabaseAvailabilityType.class)
    void testEnumConversionPossible(SdxDatabaseAvailabilityType input) {
        DatabaseAvailabilityType.valueOf(input.name());
    }

    private FlowCheckResponse createFlowCheckResponse(Boolean hasActiveFlow, Boolean failed) {
        FlowCheckResponse flowResp = new FlowCheckResponse();
        flowResp.setFlowId(RDBMS_FLOW_ID);
        flowResp.setHasActiveFlow(hasActiveFlow);
        flowResp.setLatestFlowFinalizedAndFailed(failed);
        return flowResp;
    }

    private void mockShortPollConfig() {
        PollingConfig config = PollingConfig.builder()
                .withSleepTime(10)
                .withSleepTimeUnit(TimeUnit.MILLISECONDS)
                .withTimeout(10)
                .withTimeoutTimeUnit(TimeUnit.MILLISECONDS)
                .withStopPollingIfExceptionOccured(false)
                .build();
        lenient().when(dbPollConfig.getConfig()).thenReturn(config);
    }

    private Database createDatabase(DatabaseAvailabilityType databaseAvailabilityType) {
        Database database = new Database();
        database.setExternalDatabaseAvailabilityType(databaseAvailabilityType);
        return database;
    }
}