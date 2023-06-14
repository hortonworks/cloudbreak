package com.sequenceiq.cloudbreak.core.flow2.externaldatabase;

import static com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType.ROTATE;
import static com.sequenceiq.redbeams.rotation.RedbeamsSecretType.REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.core.AttemptResults;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.conf.ExternalDatabaseConfig;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.repository.cluster.ClusterRepository;
import com.sequenceiq.cloudbreak.service.externaldatabase.DatabaseOperation;
import com.sequenceiq.cloudbreak.service.externaldatabase.DatabaseServerParameterDecorator;
import com.sequenceiq.cloudbreak.service.externaldatabase.model.DatabaseServerParameter;
import com.sequenceiq.cloudbreak.service.externaldatabase.model.DatabaseStackConfig;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsClientService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
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

    private static final String DATABASE_NOT_FOUND = "Database not found";

    @Mock
    private RedbeamsClientService redbeamsClient;

    @Mock
    private ClusterRepository clusterRepository;

    private final Map<CloudPlatform, DatabaseStackConfig> dbConfigs = new HashMap<>();

    @Mock
    private Map<CloudPlatform, DatabaseServerParameterDecorator> parameterDecoratorMap;

    @Mock
    private DatabaseObtainerService databaseObtainerService;

    @Mock
    private DatabaseServerParameterDecorator dbServerParameterDecorator;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ExternalDatabaseConfig externalDatabaseConfig;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    private ExternalDatabaseService underTest;

    private DetailedEnvironmentResponse environmentResponse;

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @BeforeEach
    void setUp() {
        underTest = new ExternalDatabaseService(redbeamsClient, clusterRepository, dbConfigs, parameterDecoratorMap, databaseObtainerService,
                entitlementService, externalDatabaseConfig, cmTemplateProcessorFactory);
        environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCloudPlatform(CLOUD_PLATFORM.name());
        environmentResponse.setCrn(ENV_CRN);
        dbConfigs.put(CLOUD_PLATFORM, new DatabaseStackConfig(INSTANCE_TYPE, VENDOR, VOLUME_SIZE));
        lenient().when(parameterDecoratorMap.get(CLOUD_PLATFORM)).thenReturn(dbServerParameterDecorator);
    }

    @Test
    void provisionDatabaseBadRequest() {
        Stack stack = new Stack();
        stack.setExternalDatabaseCreationType(DatabaseAvailabilityType.HA);
        stack.setCluster(new Cluster());
        when(redbeamsClient.create(any())).thenThrow(BadRequestException.class);

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
        stack.setExternalDatabaseCreationType(availability);
        stack.setCluster(cluster);
        when(redbeamsClient.getByClusterCrn(nullable(String.class), nullable(String.class))).thenReturn(null);
        when(redbeamsClient.create(any())).thenReturn(createResponse);
        when(databaseObtainerService.obtainAttemptResult(eq(cluster), eq(DatabaseOperation.CREATION), eq(RDBMS_CRN), eq(true)))
                .thenReturn(AttemptResults.finishWith(new DatabaseServerV4Response()));
        underTest.provisionDatabase(stack, environmentResponse);

        ArgumentCaptor<DatabaseServerParameter> serverParameterCaptor = ArgumentCaptor.forClass(DatabaseServerParameter.class);
        verify(redbeamsClient).getByClusterCrn(ENV_CRN, CLUSTER_CRN);
        verify(redbeamsClient).create(any(AllocateDatabaseServerV4Request.class));
        verify(cluster).setDatabaseServerCrn(RDBMS_CRN);
        verify(dbServerParameterDecorator).setParameters(any(), serverParameterCaptor.capture());
        verify(clusterRepository).save(cluster);
        DatabaseServerParameter paramValue = serverParameterCaptor.getValue();
        assertThat(paramValue.isHighlyAvailable()).isEqualTo(availability == DatabaseAvailabilityType.HA);
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
        underTest.provisionDatabase(stack, environmentResponse);

        ArgumentCaptor<DatabaseServerParameter> serverParameterCaptor = ArgumentCaptor.forClass(DatabaseServerParameter.class);
        verify(redbeamsClient).getByClusterCrn(ENV_CRN, CLUSTER_CRN);
        verify(redbeamsClient).create(any(AllocateDatabaseServerV4Request.class));
        verify(cluster).setDatabaseServerCrn(RDBMS_CRN);
        verify(dbServerParameterDecorator).setParameters(any(), serverParameterCaptor.capture());
        verify(clusterRepository).save(cluster);
        DatabaseServerParameter paramValue = serverParameterCaptor.getValue();
        assertThat(paramValue.isHighlyAvailable()).isEqualTo(availability == DatabaseAvailabilityType.HA);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
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
        stack.setExternalDatabaseCreationType(DatabaseAvailabilityType.NON_HA);
        DatabaseServerV4Response dbServerResponse = new DatabaseServerV4Response();
        dbServerResponse.setCrn(RDBMS_CRN);
        when(redbeamsClient.getByClusterCrn(nullable(String.class), nullable(String.class))).thenReturn(dbServerResponse);
        when(databaseObtainerService.obtainAttemptResult(eq(cluster), eq(DatabaseOperation.CREATION), eq(RDBMS_CRN), eq(true)))
                .thenReturn(AttemptResults.finishWith(new DatabaseServerV4Response()));
        underTest.provisionDatabase(stack, environmentResponse);

        verify(redbeamsClient).getByClusterCrn(ENV_CRN, CLUSTER_CRN);
        verify(redbeamsClient, never()).create(any(AllocateDatabaseServerV4Request.class));
        verify(dbServerParameterDecorator, never()).setParameters(any(), any());
        verify(cluster).setDatabaseServerCrn(RDBMS_CRN);
        verify(clusterRepository).save(cluster);
    }

    @Test
    void provisionDatabaseTestSslWhenUnsupportedCloudPlatform() throws JsonProcessingException {
        when(externalDatabaseConfig.isExternalDatabaseSslEnforcementSupportedFor(CLOUD_PLATFORM)).thenReturn(false);

        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(BLUEPRINT_TEXT);
        when(cmTemplateProcessorFactory.get(BLUEPRINT_TEXT)).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getStackVersion()).thenReturn(STACK_VERSION_GOOD);

        provisionDatabaseTestSslInternal(blueprint, false);

        verify(entitlementService, never()).databaseWireEncryptionDatahubEnabled(anyString());
    }

    private void provisionDatabaseTestSslInternal(Blueprint blueprint, boolean sslEnabledExpected) throws JsonProcessingException {
        DatabaseServerStatusV4Response createResponse = new DatabaseServerStatusV4Response();
        createResponse.setResourceCrn(RDBMS_CRN);
        Cluster cluster = new Cluster();
        Stack stack = new Stack();
        stack.setResourceCrn(CLUSTER_CRN);
        stack.setExternalDatabaseCreationType(DatabaseAvailabilityType.HA);
        Map<String, String> userDefinedTags = Map.ofEntries(entry("key1", "value1"), entry("key2", "value2"));
        StackTags stackTags = new StackTags(userDefinedTags, Map.of(), Map.of());
        stack.setTags(new Json(stackTags));
        cluster.setBlueprint(blueprint);
        cluster.setEnvironmentCrn(ENV_CRN);
        stack.setCluster(cluster);

        when(redbeamsClient.getByClusterCrn(ENV_CRN, CLUSTER_CRN)).thenReturn(null);
        when(redbeamsClient.create(any(AllocateDatabaseServerV4Request.class))).thenReturn(createResponse);
        when(databaseObtainerService.obtainAttemptResult(cluster, DatabaseOperation.CREATION, RDBMS_CRN, true))
                .thenReturn(AttemptResults.finishWith(new DatabaseServerV4Response()));

        underTest.provisionDatabase(stack, environmentResponse);

        assertThat(cluster.getDatabaseServerCrn()).isEqualTo(RDBMS_CRN);
        verify(clusterRepository).save(cluster);

        ArgumentCaptor<DatabaseServerV4StackRequest> databaseServerCaptor = ArgumentCaptor.forClass(DatabaseServerV4StackRequest.class);
        ArgumentCaptor<DatabaseServerParameter> serverParameterCaptor = ArgumentCaptor.forClass(DatabaseServerParameter.class);
        verify(dbServerParameterDecorator).setParameters(databaseServerCaptor.capture(), serverParameterCaptor.capture());

        DatabaseServerParameter serverParameter = serverParameterCaptor.getValue();
        assertThat(serverParameter).isNotNull();
        assertThat(serverParameter.isHighlyAvailable()).isTrue();

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

        provisionDatabaseTestSslInternal(null, false);

        verify(cmTemplateProcessorFactory, never()).get(anyString());
        verify(cmTemplateProcessor, never()).getStackVersion();
        verify(entitlementService, never()).databaseWireEncryptionDatahubEnabled(anyString());
    }

    @Test
    void provisionDatabaseTestSslWhenNoBlueprintText() throws JsonProcessingException {
        when(externalDatabaseConfig.isExternalDatabaseSslEnforcementSupportedFor(CLOUD_PLATFORM)).thenReturn(true);

        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(null);

        provisionDatabaseTestSslInternal(blueprint, false);

        verify(cmTemplateProcessorFactory, never()).get(anyString());
        verify(cmTemplateProcessor, never()).getStackVersion();
        verify(entitlementService, never()).databaseWireEncryptionDatahubEnabled(anyString());
    }

    @ParameterizedTest(name = "runtime={0}")
    @ValueSource(strings = {"", " ", STACK_VERSION_BAD})
    @NullSource
    void provisionDatabaseTestSslWhenBadRuntime(String runtime) throws JsonProcessingException {
        when(externalDatabaseConfig.isExternalDatabaseSslEnforcementSupportedFor(CLOUD_PLATFORM)).thenReturn(true);

        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(BLUEPRINT_TEXT);
        when(cmTemplateProcessorFactory.get(BLUEPRINT_TEXT)).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getStackVersion()).thenReturn(runtime);

        provisionDatabaseTestSslInternal(blueprint, false);

        verify(entitlementService, never()).databaseWireEncryptionDatahubEnabled(anyString());
    }

    @Test
    void provisionDatabaseTestSslWhenNotEntitled() throws JsonProcessingException {
        when(externalDatabaseConfig.isExternalDatabaseSslEnforcementSupportedFor(CLOUD_PLATFORM)).thenReturn(true);

        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(BLUEPRINT_TEXT);
        when(cmTemplateProcessorFactory.get(BLUEPRINT_TEXT)).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getStackVersion()).thenReturn(STACK_VERSION_GOOD);
        when(entitlementService.databaseWireEncryptionDatahubEnabled(ACCOUNT_ID)).thenReturn(false);

        provisionDatabaseTestSslInternal(blueprint, false);
    }

    @ParameterizedTest(name = "runtime={0}")
    @ValueSource(strings = {STACK_VERSION_GOOD_MINIMAL, STACK_VERSION_GOOD})
    void provisionDatabaseTestSslWhenSslEnabled(String runtime) throws JsonProcessingException {
        when(externalDatabaseConfig.isExternalDatabaseSslEnforcementSupportedFor(CLOUD_PLATFORM)).thenReturn(true);

        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(BLUEPRINT_TEXT);
        when(cmTemplateProcessorFactory.get(BLUEPRINT_TEXT)).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getStackVersion()).thenReturn(runtime);
        when(entitlementService.databaseWireEncryptionDatahubEnabled(ACCOUNT_ID)).thenReturn(true);

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

        UpgradeDatabaseServerV4Response response = new UpgradeDatabaseServerV4Response();
        response.setFlowIdentifier(new FlowIdentifier(FlowType.FLOW, RDBMS_FLOW_ID));
        when(redbeamsClient.upgradeByCrn(eq(RDBMS_CRN), any())).thenReturn(response);

        when(redbeamsClient.hasFlowRunningByFlowId(RDBMS_FLOW_ID)).thenReturn(
                createFlowCheckResponse(Boolean.TRUE, Boolean.FALSE),
                createFlowCheckResponse(Boolean.FALSE, Boolean.FALSE));

        underTest.upgradeDatabase(cluster, targetMajorVersion);

        ArgumentCaptor<UpgradeDatabaseServerV4Request> argumentCaptor = ArgumentCaptor.forClass(UpgradeDatabaseServerV4Request.class);
        verify(redbeamsClient).upgradeByCrn(eq(RDBMS_CRN), argumentCaptor.capture());
        assertEquals(targetMajorVersion, argumentCaptor.getValue().getUpgradeTargetMajorVersion());
        verify(redbeamsClient, times(2)).hasFlowRunningByFlowId(RDBMS_FLOW_ID);
    }

    @Test
    void upgradeDatabaseNotFound() {
        Cluster cluster = spy(new Cluster());
        cluster.setDatabaseServerCrn(RDBMS_CRN);

        UpgradeTargetMajorVersion targetMajorVersion = UpgradeTargetMajorVersion.VERSION_11;

        when(redbeamsClient.upgradeByCrn(eq(RDBMS_CRN), any())).thenThrow(new NotFoundException("Not found"));

        underTest.upgradeDatabase(cluster, targetMajorVersion);

        ArgumentCaptor<UpgradeDatabaseServerV4Request> argumentCaptor = ArgumentCaptor.forClass(UpgradeDatabaseServerV4Request.class);
        verify(redbeamsClient).upgradeByCrn(eq(RDBMS_CRN), argumentCaptor.capture());
        assertEquals(targetMajorVersion, argumentCaptor.getValue().getUpgradeTargetMajorVersion());
        verify(redbeamsClient, never()).hasFlowRunningByFlowId(any());
    }

    @Test
    void upgradeDatabaseFlowFailed() {
        Cluster cluster = spy(new Cluster());
        cluster.setDatabaseServerCrn(RDBMS_CRN);

        UpgradeTargetMajorVersion targetMajorVersion = UpgradeTargetMajorVersion.VERSION_11;

        UpgradeDatabaseServerV4Response response = new UpgradeDatabaseServerV4Response();
        response.setFlowIdentifier(new FlowIdentifier(FlowType.FLOW, RDBMS_FLOW_ID));
        DatabaseServerV4Response dbServerResponse = new DatabaseServerV4Response();
        dbServerResponse.setCrn(RDBMS_CRN);
        dbServerResponse.setStatusReason(UPGRADE_ERROR_REASON);
        when(redbeamsClient.getByCrn(RDBMS_CRN)).thenReturn(dbServerResponse);
        when(redbeamsClient.upgradeByCrn(eq(RDBMS_CRN), any())).thenReturn(response);

        when(redbeamsClient.hasFlowRunningByFlowId(RDBMS_FLOW_ID)).thenReturn(createFlowCheckResponse(Boolean.FALSE, Boolean.TRUE));

        CloudbreakServiceException cloudbreakServiceException = assertThrows(CloudbreakServiceException.class,
                () -> underTest.upgradeDatabase(cluster, targetMajorVersion));
        String expected = String.format("Database flow failed in Redbeams. Database crn: %s, flow: FlowIdentifier{type=FLOW, pollableId='%s'}",
                RDBMS_CRN, RDBMS_FLOW_ID);
        assertEquals(expected, cloudbreakServiceException.getMessage());

        ArgumentCaptor<UpgradeDatabaseServerV4Request> argumentCaptor = ArgumentCaptor.forClass(UpgradeDatabaseServerV4Request.class);
        verify(redbeamsClient).upgradeByCrn(eq(RDBMS_CRN), argumentCaptor.capture());
        assertEquals(targetMajorVersion, argumentCaptor.getValue().getUpgradeTargetMajorVersion());
        verify(redbeamsClient, times(1)).hasFlowRunningByFlowId(RDBMS_FLOW_ID);
    }

    @Test
    void upgradeDatabaseNoFlowId() {
        Cluster cluster = spy(new Cluster());
        cluster.setDatabaseServerCrn(RDBMS_CRN);

        UpgradeTargetMajorVersion targetMajorVersion = UpgradeTargetMajorVersion.VERSION_11;

        UpgradeDatabaseServerV4Response response = new UpgradeDatabaseServerV4Response();
        response.setFlowIdentifier(null);
        when(redbeamsClient.upgradeByCrn(eq(RDBMS_CRN), any())).thenReturn(response);

        underTest.upgradeDatabase(cluster, targetMajorVersion);

        ArgumentCaptor<UpgradeDatabaseServerV4Request> argumentCaptor = ArgumentCaptor.forClass(UpgradeDatabaseServerV4Request.class);
        verify(redbeamsClient).upgradeByCrn(eq(RDBMS_CRN), argumentCaptor.capture());
        verify(redbeamsClient, never()).hasFlowRunningByFlowId(any());
        assertEquals(targetMajorVersion, argumentCaptor.getValue().getUpgradeTargetMajorVersion());
    }

    @Test
    void rotateDatabaseSecretsShouldFailIfRedbeamsFlowChainIsNotTriggered() {
        when(redbeamsClient.rotateSecret(any())).thenReturn(new FlowIdentifier(FlowType.NOT_TRIGGERED, null));
        CloudbreakServiceException cloudbreakServiceException = assertThrows(CloudbreakServiceException.class,
                () -> underTest.rotateDatabaseSecret(RDBMS_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD, ROTATE));
        String expected = String.format("Database flow failed in Redbeams. Database crn: %s, flow: FlowIdentifier{type=%s, pollableId='%s'}",
                RDBMS_CRN, FlowType.NOT_TRIGGERED, null);
        assertEquals(expected, cloudbreakServiceException.getMessage());
    }

    @Test
    void rotateDatabaseSecretsShouldFailIfReturnedFlowInformationIsNull() {
        when(redbeamsClient.rotateSecret(any())).thenReturn(null);
        CloudbreakServiceException cloudbreakServiceException = assertThrows(CloudbreakServiceException.class,
                () -> underTest.rotateDatabaseSecret(RDBMS_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD, ROTATE));
        String expected = String.format("Database flow failed in Redbeams. Database crn: %s, flow: null", RDBMS_CRN);
        assertEquals(expected, cloudbreakServiceException.getMessage());
    }

    @Test
    void rotateDatabaseSecretsShouldFailIfRedbeamsFlowChainFailed() {
        when(redbeamsClient.rotateSecret(any())).thenReturn(new FlowIdentifier(FlowType.FLOW_CHAIN, RDBMS_FLOW_ID));
        when(redbeamsClient.hasFlowChainRunningByFlowChainId(RDBMS_FLOW_ID)).thenReturn(createFlowCheckResponse(Boolean.FALSE, Boolean.TRUE));
        CloudbreakServiceException cloudbreakServiceException = assertThrows(CloudbreakServiceException.class,
                () -> underTest.rotateDatabaseSecret(RDBMS_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD, ROTATE));
        String expected = String.format("Database flow failed in Redbeams. Database crn: %s, flow: FlowIdentifier{type=%s, pollableId='%s'}",
                RDBMS_CRN, FlowType.FLOW_CHAIN, RDBMS_FLOW_ID);
        assertEquals(expected, cloudbreakServiceException.getMessage());
    }

    @Test
    void rotateDatabaseSecretsShouldSucceed() {
        when(redbeamsClient.rotateSecret(any())).thenReturn(new FlowIdentifier(FlowType.FLOW_CHAIN, RDBMS_FLOW_ID));
        when(redbeamsClient.hasFlowChainRunningByFlowChainId(RDBMS_FLOW_ID)).thenReturn(createFlowCheckResponse(Boolean.FALSE, Boolean.FALSE));
        underTest.rotateDatabaseSecret(RDBMS_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD, ROTATE);
        ArgumentCaptor<RotateDatabaseServerSecretV4Request> requestCaptor = ArgumentCaptor.forClass(RotateDatabaseServerSecretV4Request.class);
        verify(redbeamsClient, times(1)).rotateSecret(requestCaptor.capture());
        RotateDatabaseServerSecretV4Request request = requestCaptor.getValue();
        assertEquals(RDBMS_CRN, request.getCrn());
        assertEquals(REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD.name(), request.getSecret());
        assertEquals(ROTATE, request.getExecutionType());
    }

    private static FlowCheckResponse createFlowCheckResponse(Boolean hasActiveFlow, Boolean failed) {
        FlowCheckResponse flowResp = new FlowCheckResponse();
        flowResp.setFlowId(RDBMS_FLOW_ID);
        flowResp.setHasActiveFlow(hasActiveFlow);
        flowResp.setLatestFlowFinalizedAndFailed(failed);
        return flowResp;
    }

    @Test
    void upgradeDatabaseWhenCrnNull() {
        Cluster cluster = spy(new Cluster());
        cluster.setDatabaseServerCrn(null);
        UpgradeTargetMajorVersion targetMajorVersion = UpgradeTargetMajorVersion.VERSION_11;

        underTest.upgradeDatabase(cluster, targetMajorVersion);

        verify(redbeamsClient, never()).upgradeByCrn(anyString(), any());
    }
}
