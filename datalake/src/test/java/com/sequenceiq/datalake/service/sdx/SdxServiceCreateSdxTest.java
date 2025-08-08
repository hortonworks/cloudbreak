package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.GCP;
import static com.sequenceiq.common.api.type.InstanceGroupType.CORE;
import static com.sequenceiq.common.api.type.InstanceGroupType.GATEWAY;
import static com.sequenceiq.datalake.service.sdx.SdxVersionRuleEnforcer.CCMV2_JUMPGATE_REQUIRED_VERSION;
import static com.sequenceiq.datalake.service.sdx.SdxVersionRuleEnforcer.MEDIUM_DUTY_REQUIRED_VERSION;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.CUSTOM;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.ENTERPRISE;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.LIGHT_DUTY;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.MEDIUM_DUTY_HA;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.MICRO_DUTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.BaseStackDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.RecipeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.vm.VirtualMachineConfiguration;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.type.EnvironmentType;
import com.sequenceiq.common.api.type.LoadBalancerSku;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.datalake.configuration.CDPConfigService;
import com.sequenceiq.datalake.configuration.PlatformConfig;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.repository.SdxDatabaseRepository;
import com.sequenceiq.datalake.service.imagecatalog.ImageCatalogService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.sdx.api.model.SdxAwsRequest;
import com.sequenceiq.sdx.api.model.SdxAwsSpotParameters;
import com.sequenceiq.sdx.api.model.SdxAzureRequest;
import com.sequenceiq.sdx.api.model.SdxCloudStorageRequest;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;
import com.sequenceiq.sdx.api.model.SdxInstanceGroupRequest;
import com.sequenceiq.sdx.api.model.SdxRecipe;

@ExtendWith(MockitoExtension.class)
class SdxServiceCreateSdxTest {

    private static final Map<String, String> TAGS = Collections.singletonMap("mytag", "tagecske");

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:hortonworks:user:perdos@hortonworks.com";

    private static final String CLUSTER_NAME = "test-sdx-cluster";

    private static final String OS = "centos7";

    @Mock
    private SdxExternalDatabaseConfigurer externalDatabaseConfigurer;

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private SdxDatabaseRepository sdxDatabaseRepository;

    @Mock
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Mock
    private EnvironmentService environmentClientService;

    @Mock
    private Clock clock;

    @Mock
    private CloudStorageManifester cloudStorageManifester;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private RecipeV4Endpoint recipeV4Endpoint;

    @Mock
    private CDPConfigService cdpConfigService;

    @Mock
    private OwnerAssignmentService ownerAssignmentService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private PlatformConfig platformConfig;

    @Mock
    private VirtualMachineConfiguration virtualMachineConfiguration;

    @Mock
    private PlatformStringTransformer platformStringTransformer;

    @Mock
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @Mock
    private SdxVersionRuleEnforcer versionRuleEnforcer;

    @InjectMocks
    private SdxService underTest;

    static Object[][] cloudPlatformMultiAzDataProvider() {
        return new Object[][]{
                // testCaseName cloudPlatform multiAz
                {"CloudPlatform.AWS multiaz=true", AWS, true},
                {"CloudPlatform.AWS multiaz=false", AWS, false},
                {"CloudPlatform.AZURE multiaz=false", AZURE, false},
                {"CloudPlatform.GCP multiaz=false", GCP, false}
        };
    }

    static Object[][] razCloudPlatformAndRuntimeDataProvider() {
        return new Object[][]{
                // testCaseName cloudPlatform runtime
                {"CloudPlatform.AWS", AWS, "7.2.2"},
                {"CloudPlatform.AZURE", AZURE, "7.2.2"},
                {"CloudPlatform.GCP", GCP, "7.2.17"}
        };
    }

    static Object[][] razCloudPlatform710DataProvider() {
        return new Object[][]{
                // testCaseName cloudPlatform expectedErrorMsg
                {"CloudPlatform.AWS", AWS, "7.2.2",
                        "Provisioning Ranger Raz on Amazon Web Services is only valid for Cloudera Runtime version " +
                                "greater than or equal to 7.2.2 and not 7.1.0"},
                {"CloudPlatform.AZURE", AZURE, "7.2.2",
                        "Provisioning Ranger Raz on Microsoft Azure is only valid for Cloudera Runtime version " +
                                "greater than or equal to 7.2.2 and not 7.1.0"},
                {"CloudPlatform.GCP", GCP, "7.2.17",
                        "Provisioning Ranger Raz on GCP is only valid for Cloudera Runtime version " +
                                "greater than or equal to 7.2.17 and not 7.1.0"}
        };
    }

    static Object[][] razCloudPlatform720DataProvider() {
        return new Object[][]{
                // testCaseName cloudPlatform expectedErrorMsg
                {"CloudPlatform.AWS", AWS},
                {"CloudPlatform.AZURE", AZURE},
                {"CloudPlatform.GCP", GCP}
        };
    }

    static Object[][] ccmV2Scenarios() {
        return new Object[][]{
                // runtime  compatible
                {null, true},
                {"7.2.0", false},
                {"7.2.1", true},
                {"7.2.5", true},
                {"7.2.6", true},
                {"7.2.7", true},
        };
    }

    static Object[][] ccmV2JumpgateScenarios() {
        return new Object[][]{
                // runtime  compatible
                {null, true},
                {"7.2.0", false},
                {"7.2.1", false},
                {"7.2.5", false},
                {"7.2.6", true},
                {"7.2.7", true},
        };
    }

    static Object[][] deleteInProgressParamProvider() {
        return new Object[][]{
                {EnvironmentStatus.DELETE_INITIATED},
                {EnvironmentStatus.NETWORK_DELETE_IN_PROGRESS},
                {EnvironmentStatus.RDBMS_DELETE_IN_PROGRESS},
                {EnvironmentStatus.FREEIPA_DELETE_IN_PROGRESS},
                {EnvironmentStatus.CLUSTER_DEFINITION_CLEANUP_PROGRESS},
                {EnvironmentStatus.UMS_RESOURCE_DELETE_IN_PROGRESS},
                {EnvironmentStatus.IDBROKER_MAPPINGS_DELETE_IN_PROGRESS},
                {EnvironmentStatus.S3GUARD_TABLE_DELETE_IN_PROGRESS},
                {EnvironmentStatus.DATAHUB_CLUSTERS_DELETE_IN_PROGRESS},
                {EnvironmentStatus.DATALAKE_CLUSTERS_DELETE_IN_PROGRESS},
                {EnvironmentStatus.PUBLICKEY_DELETE_IN_PROGRESS}
        };
    }

    static Object[][] failedParamProvider() {
        return new Object[][]{
                {EnvironmentStatus.CREATE_FAILED},
                {EnvironmentStatus.DELETE_FAILED},
                {EnvironmentStatus.UPDATE_FAILED},
                {EnvironmentStatus.FREEIPA_DELETED_ON_PROVIDER_SIDE}
        };
    }

    @BeforeEach
    void initMocks() {
        lenient().when(platformConfig.getRazSupportedPlatforms())
                .thenReturn(List.of(AWS, AZURE, GCP));
        lenient().when(entitlementService.isRazForGcpEnabled(anyString()))
                .thenReturn(true);
        lenient().when(entitlementService.isEntitledToUseOS(any(), any())).thenReturn(true);
        lenient().doNothing().when(platformAwareSdxConnector).validateIfOtherPlatformsHasSdx(any(), any());
    }

    @Test
    void testCreateSdxClusterWhenClusterWithSpecifiedNameAlreadyExistShouldThrowClusterAlreadyExistBadRequestException() {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest(null, MEDIUM_DUTY_HA);
        SdxCluster existing = new SdxCluster();
        existing.setEnvName("envir");
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(
                anyString(), anyString())).thenReturn(Collections.singletonList(existing));
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                        underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null)));
        assertEquals("SDX cluster exists for environment name: envir", badRequestException.getMessage());
    }

    @Test
    void testCreateSdxClusterWithoutCloudStorageShouldThrownBadRequestException() {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest("7.2.1", LIGHT_DUTY);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        mockEnvironmentCall(sdxClusterRequest, AWS, null);
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                        underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null)));
        assertEquals("Cloud storage parameter is required.", badRequestException.getMessage());
    }

    @Test
    void testCreateSdxClusterWithoutCloudStorageShouldNotThrownBadRequestExceptionInCaseOfInternal() {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest("7.2.1", CUSTOM);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        mockEnvironmentCall(sdxClusterRequest, AWS, null);
        StackV4Request stackV4Request = new StackV4Request();
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        stackV4Request.setCluster(clusterV4Request);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, stackV4Request));
    }

    @Test
    void testCreateShouldThrowExceptionWhenTheRequestContainsBasicLoadBalancerSku() {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest("7.2.1", CUSTOM);
        SdxAzureRequest azureRequest = new SdxAzureRequest();
        azureRequest.setLoadBalancerSku(LoadBalancerSku.BASIC);
        sdxClusterRequest.setAzure(azureRequest);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        mockEnvironmentCall(sdxClusterRequest, AZURE, null);
        StackV4Request stackV4Request = new StackV4Request();
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        stackV4Request.setCluster(clusterV4Request);

        assertThatThrownBy(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, stackV4Request)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("The Basic SKU type is no longer supported for Load Balancers. Please use the Standard SKU to provision a Load Balancer. "
                        + "Check documentation for more information: https://azure.microsoft.com/en-gb/updates?id="
                        + "azure-basic-load-balancer-will-be-retired-on-30-september-2025-upgrade-to-standard-load-balancer");
    }

    @Test
    void testShouldNotThrowExceptionWhenTheAzureLoadBalancerSkuIsNull() {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest("7.2.1", CUSTOM);
        sdxClusterRequest.setAzure(new SdxAzureRequest());
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        mockEnvironmentCall(sdxClusterRequest, AZURE, null);
        StackV4Request stackV4Request = new StackV4Request();
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        stackV4Request.setCluster(clusterV4Request);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, stackV4Request));
    }

    @Test
    void testNullJavaVersionShouldNotOverrideTheVersionInTheInternalStackRequest() throws IOException, TransactionExecutionException {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest("7.2.1", CUSTOM);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        mockTransactionServiceRequired();
        mockEnvironmentCall(sdxClusterRequest, AWS, null);
        StackV4Request stackV4Request = new StackV4Request();
        stackV4Request.setJavaVersion(8);
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        stackV4Request.setCluster(clusterV4Request);

        ArgumentCaptor<SdxCluster> sdxClusterArgumentCaptor = ArgumentCaptor.forClass(SdxCluster.class);
        when(sdxClusterRepository.save(sdxClusterArgumentCaptor.capture())).thenReturn(mock(SdxCluster.class));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, stackV4Request));

        StackV4Request savedStackV4Request = JsonUtil.readValue(sdxClusterArgumentCaptor.getValue().getStackRequest(), StackV4Request.class);
        assertEquals(8, savedStackV4Request.getJavaVersion());
    }

    private void mockTransactionServiceRequired() throws TransactionExecutionException {
        when(transactionService.required(isA(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());
    }

    @Test
    void testCreateNOTInternalSdxClusterFromLightDutyTemplateShouldTriggerSdxCreationFlow() throws IOException, TransactionExecutionException {
        CrnTestUtil.mockCrnGenerator(regionAwareCrnGenerator);
        mockTransactionServiceRequired();
        String lightDutyJson = readLightDutyTestTemplate();
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(lightDutyJson, StackV4Request.class));
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest(null, LIGHT_DUTY);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        withCloudStorage(sdxClusterRequest);
        long id = 10L;
        when(sdxClusterRepository.save(any(SdxCluster.class))).thenAnswer(invocation -> {
            SdxCluster sdxWithId = invocation.getArgument(0, SdxCluster.class);
            sdxWithId.setId(id);
            return sdxWithId;
        });
        when(externalDatabaseConfigurer.configure(any(), any(), any(), any(), any())).thenReturn(new SdxDatabase());
        when(clock.getCurrentTimeMillis()).thenReturn(1L);
        mockEnvironmentCall(sdxClusterRequest, AZURE, null);
        Pair<SdxCluster, FlowIdentifier> result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null));
        SdxCluster createdSdxCluster = result.getLeft();
        assertEquals(id, createdSdxCluster.getId());
        ArgumentCaptor<SdxCluster> captor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository, times(1)).save(captor.capture());
        SdxCluster capturedSdx = captor.getValue();
        assertEquals("tagecske", capturedSdx.getTags().getValue("mytag"));
        assertEquals(CLUSTER_NAME, capturedSdx.getClusterName());
        assertEquals(LIGHT_DUTY, capturedSdx.getClusterShape());
        assertEquals("envir", capturedSdx.getEnvName());
        assertEquals("hortonworks", capturedSdx.getAccountId());
        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(DatalakeStatusEnum.REQUESTED, "Datalake requested", createdSdxCluster);

        assertEquals(1L, capturedSdx.getCreated());
        assertFalse(capturedSdx.isCreateDatabase());
        assertTrue(createdSdxCluster.getCrn().matches("crn:cdp:datalake:us-west-1:hortonworks:datalake:.*"));
        StackV4Request stackV4Request = JsonUtil.readValue(capturedSdx.getStackRequest(), StackV4Request.class);

        assertEquals(2L, stackV4Request.getInstanceGroups().size());

        InstanceGroupV4Request core = getGroup(stackV4Request, CORE);
        assertEquals(1L, core.getSecurityGroup().getSecurityRules().size());
        assertEquals("0.0.0.0/0", core.getSecurityGroup().getSecurityRules().get(0).getSubnet());
        assertEquals("22", core.getSecurityGroup().getSecurityRules().get(0).getPorts().get(0));

        InstanceGroupV4Request gateway = getGroup(stackV4Request, GATEWAY);
        assertEquals(2L, gateway.getSecurityGroup().getSecurityRules().size());
        assertEquals("0.0.0.0/0", gateway.getSecurityGroup().getSecurityRules().get(0).getSubnet());
        assertEquals("443", gateway.getSecurityGroup().getSecurityRules().get(0).getPorts().get(0));
        assertEquals("0.0.0.0/0", gateway.getSecurityGroup().getSecurityRules().get(1).getSubnet());
        assertEquals("22", gateway.getSecurityGroup().getSecurityRules().get(1).getPorts().get(0));

        verify(sdxReactorFlowManager).triggerSdxCreation(createdSdxCluster);
    }

    @Test
    void testCreateNOTInternalSdxClusterFromLightDutyTemplateWhenLocationSpecifiedWithSlashShouldCreateAndSettedUpBaseLocationWithOUTSlash()
            throws IOException, TransactionExecutionException {
        mockTransactionServiceRequired();
        String lightDutyJson = readLightDutyTestTemplate();
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(lightDutyJson, StackV4Request.class));
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest(null, LIGHT_DUTY);
        withCloudStorage(sdxClusterRequest);
        long id = 10L;
        when(sdxClusterRepository.save(any(SdxCluster.class))).thenAnswer(invocation -> {
            SdxCluster sdxWithId = invocation.getArgument(0, SdxCluster.class);
            sdxWithId.setId(id);
            return sdxWithId;
        });
        mockEnvironmentCall(sdxClusterRequest, AWS, null);
        Pair<SdxCluster, FlowIdentifier> result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null));
        SdxCluster createdSdxCluster = result.getLeft();
        assertEquals("s3a://some/dir", createdSdxCluster.getCloudStorageBaseLocation());
    }

    @Test
    void testCreateSdxClusterWithSpotStackRequestContainsRequiredAttributes() throws IOException, TransactionExecutionException {
        mockTransactionServiceRequired();
        String lightDutyJson = readLightDutyTestTemplate();
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(lightDutyJson, StackV4Request.class));
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest(null, LIGHT_DUTY);
        setSpot(sdxClusterRequest);
        withCloudStorage(sdxClusterRequest);
        long id = 10L;
        when(sdxClusterRepository.save(any(SdxCluster.class))).thenAnswer(invocation -> {
            SdxCluster sdxWithId = invocation.getArgument(0, SdxCluster.class);
            sdxWithId.setId(id);
            return sdxWithId;
        });
        mockEnvironmentCall(sdxClusterRequest, AWS, null);
        Pair<SdxCluster, FlowIdentifier> result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null));
        SdxCluster createdSdxCluster = result.getLeft();
        // AWS 7.1.0 light duty contains exactly 2 instance groups
        assertThat(createdSdxCluster.getStackRequest()).containsSubsequence(
                "{\"aws\":{\"spot\":{\"percentage\":100,\"maxPrice\":0.9}}",
                "{\"aws\":{\"spot\":{\"percentage\":100,\"maxPrice\":0.9}}");
    }

    @Test
    void testCreateSdxClusterWithJavaVersionStackRequestContainsRequiredAttributes() throws IOException, TransactionExecutionException {
        mockTransactionServiceRequired();
        String lightDutyJson = readLightDutyTestTemplate();
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(lightDutyJson, StackV4Request.class));
        when(virtualMachineConfiguration.isJavaVersionSupported(11)).thenReturn(true);
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest(null, LIGHT_DUTY);
        sdxClusterRequest.setJavaVersion(11);
        withCloudStorage(sdxClusterRequest);
        long id = 10L;
        when(sdxClusterRepository.save(any(SdxCluster.class))).thenAnswer(invocation -> {
            SdxCluster sdxWithId = invocation.getArgument(0, SdxCluster.class);
            sdxWithId.setId(id);
            return sdxWithId;
        });
        mockEnvironmentCall(sdxClusterRequest, AWS, null);
        Pair<SdxCluster, FlowIdentifier> result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null));
        SdxCluster createdSdxCluster = result.getLeft();

        assertThat(createdSdxCluster.getStackRequest()).containsSubsequence("\"javaVersion\":11");
    }

    @Test
    void testCreateNOTInternalSdxClusterFromLightDutyTemplateWhenBaseLocationSpecifiedShouldCreateStackRequestWithSettedUpBaseLocation()
            throws IOException, TransactionExecutionException {
        mockTransactionServiceRequired();
        String lightDutyJson = readLightDutyTestTemplate();
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(lightDutyJson, StackV4Request.class));
        //doNothing().when(cloudStorageLocationValidator.validate("s3a://some/dir", ));
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest(null, LIGHT_DUTY);
        withCloudStorage(sdxClusterRequest);
        long id = 10L;
        when(sdxClusterRepository.save(any(SdxCluster.class))).thenAnswer(invocation -> {
            SdxCluster sdxWithId = invocation.getArgument(0, SdxCluster.class);
            sdxWithId.setId(id);
            return sdxWithId;
        });
        mockEnvironmentCall(sdxClusterRequest, AWS, null);
        Pair<SdxCluster, FlowIdentifier> result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null));
        SdxCluster createdSdxCluster = result.getLeft();
        assertEquals("s3a://some/dir", createdSdxCluster.getCloudStorageBaseLocation());
    }

    @ParameterizedTest
    @EnumSource(value = EnvironmentStatus.class, names = {"STOP_DATAHUB_STARTED", "STOP_DATALAKE_STARTED", "STOP_FREEIPA_STARTED", "ENV_STOPPED"})
    void testCreateButEnvInStoppedStatus(EnvironmentStatus environmentStatus) {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest(null, MEDIUM_DUTY_HA);

        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setName(sdxClusterRequest.getEnvironment());
        detailedEnvironmentResponse.setCloudPlatform(AWS.name());
        detailedEnvironmentResponse.setCrn(getCrn());
        detailedEnvironmentResponse.setEnvironmentStatus(environmentStatus);
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null)),
                "BadRequestException should thrown");
        assertEquals("The environment is stopped. Please start the environment first!", badRequestException.getMessage());
    }

    @ParameterizedTest
    @EnumSource(value = EnvironmentStatus.class, names = {"START_DATAHUB_STARTED", "START_DATALAKE_STARTED", "START_SYNCHRONIZE_USERS_STARTED",
            "START_FREEIPA_STARTED"})
    void testCreateButEnvInStartInProgressPhase(EnvironmentStatus environmentStatus) {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest(null, MEDIUM_DUTY_HA);

        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setName(sdxClusterRequest.getEnvironment());
        detailedEnvironmentResponse.setCloudPlatform(AWS.name());
        detailedEnvironmentResponse.setCrn(getCrn());
        detailedEnvironmentResponse.setEnvironmentStatus(environmentStatus);
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null)),
                "BadRequestException should thrown");
        assertEquals("The environment is starting. Please wait until finished!", badRequestException.getMessage());
    }

    @ParameterizedTest
    @MethodSource("deleteInProgressParamProvider")
    void testCreateButEnvInDeleteInProgressPhase(EnvironmentStatus environmentStatus) {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest(null, MEDIUM_DUTY_HA);
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setName(sdxClusterRequest.getEnvironment());
        detailedEnvironmentResponse.setCloudPlatform(AWS.name());
        detailedEnvironmentResponse.setCrn(getCrn());
        detailedEnvironmentResponse.setEnvironmentStatus(environmentStatus);
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null)),
                "BadRequestException should thrown");
        assertEquals("The environment is in delete in progress phase. Please create a new environment first!", badRequestException.getMessage());
    }

    @Test
    void testCreateForHybridEnvironment() {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest(null, MEDIUM_DUTY_HA);
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setName(sdxClusterRequest.getEnvironment());
        detailedEnvironmentResponse.setCloudPlatform(AWS.name());
        detailedEnvironmentResponse.setCrn(getCrn());
        detailedEnvironmentResponse.setEnvironmentStatus(EnvironmentStatus.AVAILABLE);
        detailedEnvironmentResponse.setEnvironmentType(EnvironmentType.HYBRID_BASE.toString());
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null)),
                "BadRequestException should thrown");
        assertEquals("Creating or Resizing datalake is not supported for Hybrid Environment", badRequestException.getMessage());
    }

    @ParameterizedTest
    @MethodSource("failedParamProvider")
    void testCreateButEnvInFailedPhase(EnvironmentStatus environmentStatus) {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest(null, MEDIUM_DUTY_HA);
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setName(sdxClusterRequest.getEnvironment());
        detailedEnvironmentResponse.setCloudPlatform(AWS.name());
        detailedEnvironmentResponse.setCrn(getCrn());
        detailedEnvironmentResponse.setEnvironmentStatus(environmentStatus);
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null)),
                "BadRequestException should thrown");
        assertEquals("The environment is in failed phase. Please fix the environment or create a new one first!", badRequestException.getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cloudPlatformMultiAzDataProvider")
    void testSdxCreateRazNotRequestedAndMultiAzRequested(String testCaseName, CloudPlatform cloudPlatform, boolean multiAz)
            throws IOException, TransactionExecutionException {
        mockTransactionServiceRequired();
        String lightDutyJson = readLightDutyTestTemplate();
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(lightDutyJson, StackV4Request.class));
        when(sdxReactorFlowManager.triggerSdxCreation(any())).thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest("7.2.17", MEDIUM_DUTY_HA);
        withCloudStorage(sdxClusterRequest);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        long id = 10L;
        when(sdxClusterRepository.save(any(SdxCluster.class))).thenAnswer(invocation -> {
            SdxCluster sdxWithId = invocation.getArgument(0, SdxCluster.class);
            sdxWithId.setId(id);
            return sdxWithId;
        });
        when(clock.getCurrentTimeMillis()).thenReturn(1L);
        mockEnvironmentCall(sdxClusterRequest, cloudPlatform, null);
        sdxClusterRequest.setEnableRangerRaz(false);
        sdxClusterRequest.setEnableMultiAz(multiAz);
        if (multiAz) {
            when(platformConfig.getMultiAzSupportedPlatforms()).thenReturn(EnumSet.of(AWS, AZURE, GCP));
        }

        Pair<SdxCluster, FlowIdentifier> result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null));

        SdxCluster createdSdxCluster = result.getLeft();
        assertEquals(id, createdSdxCluster.getId());
        ArgumentCaptor<SdxCluster> captor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository, times(1)).save(captor.capture());
        SdxCluster capturedSdx = captor.getValue();
        assertFalse(capturedSdx.isRangerRazEnabled());
        assertEquals(multiAz, capturedSdx.isEnableMultiAz());
        if (!multiAz) {
            verify(platformConfig, never()).getMultiAzSupportedPlatforms();
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = CloudPlatform.class, mode = Mode.EXCLUDE, names = {"AWS", "AZURE", "GCP"})
    void testSdxCreateRazNotRequestedAndMultiAzRequestedAndBadCloudPlatform(CloudPlatform cloudPlatform) {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest("7.2.17", MEDIUM_DUTY_HA);
        withCloudStorage(sdxClusterRequest);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        mockEnvironmentCall(sdxClusterRequest, cloudPlatform, null);
        sdxClusterRequest.setEnableRangerRaz(false);
        sdxClusterRequest.setEnableMultiAz(true);
        when(platformConfig.getMultiAzSupportedPlatforms()).thenReturn(EnumSet.of(AWS, AZURE, GCP));

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null)));

        assertThat(badRequestException).hasMessage("Provisioning a multi AZ cluster is only enabled for the following cloud platforms: AWS,AZURE,GCP.");
        verify(sdxClusterRepository, never()).save(any(SdxCluster.class));
    }

    @Test
    void testSdxCreateRazNotRequestedAndMultiAzRequestedAndAzureAndNotEntitled() {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest("7.2.17", MEDIUM_DUTY_HA);
        withCloudStorage(sdxClusterRequest);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        DetailedEnvironmentResponse detailedEnvironmentResponse = mockEnvironmentCall(sdxClusterRequest, AZURE, null);
        when(entitlementService.isAzureMultiAzEnabled(detailedEnvironmentResponse.getAccountId())).thenReturn(false);
        sdxClusterRequest.setEnableRangerRaz(false);
        sdxClusterRequest.setEnableMultiAz(true);
        when(platformConfig.getMultiAzSupportedPlatforms()).thenReturn(EnumSet.of(AWS, AZURE, GCP));

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null)));

        assertThat(badRequestException).hasMessage("Provisioning a multi AZ cluster on Azure requires entitlement CDP_CB_AZURE_MULTIAZ.");
        verify(sdxClusterRepository, never()).save(any(SdxCluster.class));
    }

    @Test
    void testSdxCreateRazNotRequestedAndMultiAzRequestedAndGcpAndNotEntitled() {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest("7.2.17", MEDIUM_DUTY_HA);
        withCloudStorage(sdxClusterRequest);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        DetailedEnvironmentResponse detailedEnvironmentResponse = mockEnvironmentCall(sdxClusterRequest, GCP, null);
        when(entitlementService.isGcpMultiAzEnabled(detailedEnvironmentResponse.getAccountId())).thenReturn(false);
        sdxClusterRequest.setEnableRangerRaz(false);
        sdxClusterRequest.setEnableMultiAz(true);
        when(platformConfig.getMultiAzSupportedPlatforms()).thenReturn(EnumSet.of(AWS, AZURE, GCP));

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null)));

        assertThat(badRequestException).hasMessage("Provisioning a multi AZ cluster on GCP requires entitlement CDP_CB_GCP_MULTIAZ.");
        verify(sdxClusterRepository, never()).save(any(SdxCluster.class));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = SdxClusterShape.class, mode = Mode.EXCLUDE, names = {"MEDIUM_DUTY_HA", "ENTERPRISE"})
    void testSdxCreateRazNotRequestedAndMultiAzRequestedAndAwsAndBadClusterShape(SdxClusterShape clusterShape) {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest("7.2.17", clusterShape);
        withCloudStorage(sdxClusterRequest);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        DetailedEnvironmentResponse detailedEnvironmentResponse = mockEnvironmentCall(sdxClusterRequest, AWS, null);
        if (clusterShape == MICRO_DUTY) {
            when(entitlementService.microDutySdxEnabled(Crn.safeFromString(detailedEnvironmentResponse.getCreator()).getAccountId())).thenReturn(true);
        }
        sdxClusterRequest.setEnableRangerRaz(false);
        sdxClusterRequest.setEnableMultiAz(true);
        when(platformConfig.getMultiAzSupportedPlatforms()).thenReturn(EnumSet.of(AWS, AZURE, GCP));

        StackV4Request internalStackV4Request;
        if (CUSTOM.equals(clusterShape)) {
            internalStackV4Request = new StackV4Request();
            internalStackV4Request.setCluster(new ClusterV4Request());
        } else {
            internalStackV4Request = null;
        }
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, internalStackV4Request)));

        assertThat(badRequestException).hasMessage(String.format("Provisioning a multi AZ cluster on AWS is not supported for cluster shape %s.",
                clusterShape.name()));
        verify(sdxClusterRepository, never()).save(any(SdxCluster.class));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = SdxClusterShape.class, mode = Mode.EXCLUDE, names = {"MEDIUM_DUTY_HA", "ENTERPRISE"})
    void testSdxCreateRazNotRequestedAndMultiAzRequestedAndAzureAndBadClusterShape(SdxClusterShape clusterShape) {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest("7.2.17", clusterShape);
        withCloudStorage(sdxClusterRequest);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        DetailedEnvironmentResponse detailedEnvironmentResponse = mockEnvironmentCall(sdxClusterRequest, AZURE, null);
        when(entitlementService.isAzureMultiAzEnabled(detailedEnvironmentResponse.getAccountId())).thenReturn(true);
        if (clusterShape == MICRO_DUTY) {
            when(entitlementService.microDutySdxEnabled(Crn.safeFromString(detailedEnvironmentResponse.getCreator()).getAccountId())).thenReturn(true);
        }
        sdxClusterRequest.setEnableRangerRaz(false);
        sdxClusterRequest.setEnableMultiAz(true);
        when(platformConfig.getMultiAzSupportedPlatforms()).thenReturn(EnumSet.of(AWS, AZURE, GCP));

        StackV4Request internalStackV4Request;
        if (CUSTOM.equals(clusterShape)) {
            internalStackV4Request = new StackV4Request();
            internalStackV4Request.setCluster(new ClusterV4Request());
        } else {
            internalStackV4Request = null;
        }
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, internalStackV4Request)));

        assertThat(badRequestException).hasMessage(String.format("Provisioning a multi AZ cluster on AZURE is not supported for cluster shape %s.",
                clusterShape.name()));
        verify(sdxClusterRepository, never()).save(any(SdxCluster.class));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = SdxClusterShape.class, mode = Mode.EXCLUDE, names = {"MEDIUM_DUTY_HA", "ENTERPRISE"})
    void testSdxCreateRazNotRequestedAndMultiAzRequestedAndGcpAndBadClusterShape(SdxClusterShape clusterShape) {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest("7.2.17", clusterShape);
        withCloudStorage(sdxClusterRequest);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        DetailedEnvironmentResponse detailedEnvironmentResponse = mockEnvironmentCall(sdxClusterRequest, GCP, null);
        when(entitlementService.isGcpMultiAzEnabled(detailedEnvironmentResponse.getAccountId())).thenReturn(true);
        if (clusterShape == MICRO_DUTY) {
            when(entitlementService.microDutySdxEnabled(Crn.safeFromString(detailedEnvironmentResponse.getCreator()).getAccountId())).thenReturn(true);
        }
        sdxClusterRequest.setEnableRangerRaz(false);
        sdxClusterRequest.setEnableMultiAz(true);
        when(platformConfig.getMultiAzSupportedPlatforms()).thenReturn(EnumSet.of(AWS, AZURE, GCP));

        StackV4Request internalStackV4Request;
        if (CUSTOM.equals(clusterShape)) {
            internalStackV4Request = new StackV4Request();
            internalStackV4Request.setCluster(new ClusterV4Request());
        } else {
            internalStackV4Request = null;
        }
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, internalStackV4Request)));

        assertThat(badRequestException).hasMessage(String.format("Provisioning a multi AZ cluster on GCP is not supported for cluster shape %s.",
                clusterShape.name()));
        verify(sdxClusterRepository, never()).save(any(SdxCluster.class));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = SdxClusterShape.class, names = {"MEDIUM_DUTY_HA", "ENTERPRISE"})
    void testSdxCreateRazNotRequestedAndMultiAzRequestedAndAzureAndSuccess(SdxClusterShape clusterShape) throws IOException, TransactionExecutionException {
        mockTransactionServiceRequired();
        String lightDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.17/azure/light_duty.json");
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(lightDutyJson, StackV4Request.class));
        when(sdxReactorFlowManager.triggerSdxCreation(any())).thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest("7.2.17", clusterShape);
        withCloudStorage(sdxClusterRequest);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        long id = 10L;
        when(sdxClusterRepository.save(any(SdxCluster.class))).thenAnswer(invocation -> {
            SdxCluster sdxWithId = invocation.getArgument(0, SdxCluster.class);
            sdxWithId.setId(id);
            return sdxWithId;
        });
        when(clock.getCurrentTimeMillis()).thenReturn(1L);
        DetailedEnvironmentResponse detailedEnvironmentResponse = mockEnvironmentCall(sdxClusterRequest, AZURE, null);
        when(entitlementService.isAzureMultiAzEnabled(detailedEnvironmentResponse.getAccountId())).thenReturn(true);
        sdxClusterRequest.setEnableRangerRaz(false);
        sdxClusterRequest.setEnableMultiAz(true);
        when(platformConfig.getMultiAzSupportedPlatforms()).thenReturn(EnumSet.of(AWS, AZURE, GCP));

        Pair<SdxCluster, FlowIdentifier> result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null));

        SdxCluster createdSdxCluster = result.getLeft();
        assertEquals(id, createdSdxCluster.getId());
        ArgumentCaptor<SdxCluster> captor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository, times(1)).save(captor.capture());
        SdxCluster capturedSdx = captor.getValue();
        assertFalse(capturedSdx.isRangerRazEnabled());
        assertThat(capturedSdx.isEnableMultiAz()).isTrue();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("razCloudPlatformAndRuntimeDataProvider")
    void testSdxCreateRazEnabled(String testCaseName, CloudPlatform cloudPlatform, String runtime) throws IOException, TransactionExecutionException {
        mockTransactionServiceRequired();
        String lightDutyJson = readLightDutyTestTemplate();
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(lightDutyJson, StackV4Request.class));
        when(sdxReactorFlowManager.triggerSdxCreation(any())).thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest(runtime, LIGHT_DUTY);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        withCloudStorage(sdxClusterRequest);
        long id = 10L;
        when(sdxClusterRepository.save(any(SdxCluster.class))).thenAnswer(invocation -> {
            SdxCluster sdxWithId = invocation.getArgument(0, SdxCluster.class);
            sdxWithId.setId(id);
            return sdxWithId;
        });
        when(clock.getCurrentTimeMillis()).thenReturn(1L);
        when(versionRuleEnforcer.isRazSupported(any(), any())).thenReturn(true);
        mockEnvironmentCall(sdxClusterRequest, cloudPlatform, null);
        sdxClusterRequest.setEnableRangerRaz(true);
        Pair<SdxCluster, FlowIdentifier> result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null));
        SdxCluster createdSdxCluster = result.getLeft();
        assertEquals(id, createdSdxCluster.getId());
        ArgumentCaptor<SdxCluster> captor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository, times(1)).save(captor.capture());
        SdxCluster capturedSdx = captor.getValue();
        assertTrue(capturedSdx.isRangerRazEnabled());
    }

    @Test
    void testSdxCreateRazEnabledWithRazNotEnabledCloud() {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest("7.2.2", LIGHT_DUTY);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.YARN, null);
        sdxClusterRequest.setEnableRangerRaz(true);
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null)));
        assertEquals("Provisioning Ranger Raz is only valid for Amazon Web Services, Microsoft Azure, GCP", badRequestException.getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("razCloudPlatform710DataProvider")
    void testSdxCreateRazEnabled710Runtime(String testCaseName, CloudPlatform cloudPlatform, String expectedVersion, String expectedErrorMsg) {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest("7.1.0", LIGHT_DUTY);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        mockEnvironmentCall(sdxClusterRequest, cloudPlatform, null);
        sdxClusterRequest.setEnableRangerRaz(true);
        when(versionRuleEnforcer.getSupportedRazVersionForPlatform(cloudPlatform)).thenReturn(expectedVersion);
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null)));
        assertEquals(expectedErrorMsg, badRequestException.getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("razCloudPlatform720DataProvider")
    void testSdxCreateRazEnabled720Runtime(String testCaseName, CloudPlatform cloudPlatform) throws IOException {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest("7.2.7", LIGHT_DUTY);
        withCloudStorage(sdxClusterRequest);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        when(versionRuleEnforcer.isRazSupported(any(), any())).thenReturn(true);
        mockEnvironmentCall(sdxClusterRequest, cloudPlatform, null);
        sdxClusterRequest.setEnableRangerRaz(true);
        String lightDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.7/aws/light_duty.json");
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(lightDutyJson, StackV4Request.class));

        assertDoesNotThrow(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null)));
    }

    @Test
    void testSdxCreateRazEnabledForGcpEntitilementNotEnabled() {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest("7.2.17", LIGHT_DUTY);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        when(entitlementService.isRazForGcpEnabled(anyString())).thenReturn(false);
        when(versionRuleEnforcer.isRazSupported(any(), any())).thenReturn(true);
        mockEnvironmentCall(sdxClusterRequest, GCP, null);

        sdxClusterRequest.setEnableRangerRaz(true);
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null)));
        assertEquals("Provisioning Ranger Raz on GCP is not enabled for this account", badRequestException.getMessage());
    }

    @Test
    void testSdxCreateWithDifferingOsValues() {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest("7.2.17", LIGHT_DUTY);
        sdxClusterRequest.setOs("os1");
        withCloudStorage(sdxClusterRequest);
        StackV4Request stackV4Request = new StackV4Request();
        ImageSettingsV4Request imageSettingsV4Request = new ImageSettingsV4Request();
        imageSettingsV4Request.setOs("os2");
        stackV4Request.setImage(imageSettingsV4Request);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, stackV4Request)));
        assertEquals("Differing os was set in request, only the image settings os should be set.", badRequestException.getMessage());
    }

    @Test
    void testSdxCreateMediumDutySdx() throws IOException, TransactionExecutionException {
        final String runtime = "7.2.7";
        mockTransactionServiceRequired();
        String lightDutyJson = FileReaderUtils.readFileFromClasspath("/duties/" + runtime + "/aws/medium_duty_ha.json");
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(lightDutyJson, StackV4Request.class));
        when(sdxReactorFlowManager.triggerSdxCreation(any())).thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest(runtime, MEDIUM_DUTY_HA);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        withCloudStorage(sdxClusterRequest);
        long id = 10L;
        when(sdxClusterRepository.save(any(SdxCluster.class))).thenAnswer(invocation -> {
            SdxCluster sdxWithId = invocation.getArgument(0, SdxCluster.class);
            sdxWithId.setId(id);
            return sdxWithId;
        });
        when(clock.getCurrentTimeMillis()).thenReturn(1L);
        mockEnvironmentCall(sdxClusterRequest, AWS, null);
        Pair<SdxCluster, FlowIdentifier> result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null));
        SdxCluster createdSdxCluster = result.getLeft();
        assertEquals(id, createdSdxCluster.getId());
        ArgumentCaptor<SdxCluster> captor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository, times(1)).save(captor.capture());
        SdxCluster capturedSdx = captor.getValue();
        assertEquals(MEDIUM_DUTY_HA, capturedSdx.getClusterShape());
    }

    @Test
    void testSdxCreateMediumDutySdx710Runtime() {
        final String invalidRuntime = "7.1.0";
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest(invalidRuntime, MEDIUM_DUTY_HA);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        mockEnvironmentCall(sdxClusterRequest, AWS, null);
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null)));
        assertEquals("Provisioning a Medium Duty SDX shape is only valid for CM version greater than or equal to "
                + MEDIUM_DUTY_REQUIRED_VERSION + " and not " + invalidRuntime, badRequestException.getMessage());
    }

    @Test
    void testSdxCreateMediumDutySdx720Runtime() {
        final String invalidRuntime = "7.2.0";
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest(invalidRuntime, MEDIUM_DUTY_HA);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        mockEnvironmentCall(sdxClusterRequest, AWS, null);
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null)));
        assertEquals("Provisioning a Medium Duty SDX shape is only valid for CM version greater than or equal to "
                + MEDIUM_DUTY_REQUIRED_VERSION + " and not " + invalidRuntime, badRequestException.getMessage());
    }

    @Test
    void testCreateSdxClusterWithCustomRequestContainsImageInfo() throws Exception {
        ImageV4Response imageResponse = getImageResponse();
        mockTransactionServiceRequired();
        String lightDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.7/aws/light_duty.json");
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(lightDutyJson, StackV4Request.class));
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest(LIGHT_DUTY, "cdp-default", "imageId_1");
        setSpot(sdxClusterRequest);
        withCloudStorage(sdxClusterRequest);
        sdxClusterRequest.setVariant("AWS");
        when(imageCatalogService.getImageResponseFromImageRequest(eq(sdxClusterRequest.getImage()), any())).thenReturn(imageResponse);
        when(externalDatabaseConfigurer.configure(any(), eq(OS), any(), any(), any())).thenReturn(new SdxDatabase());
        long id = 10L;
        when(sdxClusterRepository.save(any(SdxCluster.class))).thenAnswer(invocation -> {
            SdxCluster sdxWithId = invocation.getArgument(0, SdxCluster.class);
            sdxWithId.setId(id);
            return sdxWithId;
        });
        mockEnvironmentCall(sdxClusterRequest, AWS, null);
        Pair<SdxCluster, FlowIdentifier> result = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null));

        SdxCluster createdSdxCluster = result.getLeft();
        StackV4Request stackV4Request = JsonUtil.readValue(createdSdxCluster.getStackRequest(), StackV4Request.class);
        assertNotNull(stackV4Request.getImage());
        assertEquals("AWS", stackV4Request.getVariant());
        assertEquals("cdp-default", stackV4Request.getImage().getCatalog());
        assertEquals("imageId_1", stackV4Request.getImage().getId());
        verify(externalDatabaseConfigurer).configure(any(), eq(OS), any(), any(), any());
    }

    @Test
    void testCreateInternalSdxClusterWithCustomInstanceGroupShouldFail() {
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        StackV4Request stackV4Request = new StackV4Request();
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        stackV4Request.setCluster(clusterV4Request);
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest("7.2.12", CUSTOM);
        withCustomInstanceGroups(sdxClusterRequest);
        mockEnvironmentCall(sdxClusterRequest, AWS, null);
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, stackV4Request));
        assertEquals("Custom instance group is not accepted on SDX Internal API.", badRequestException.getMessage());
    }

    @Test
    void testCreateSdxClusterWithCustomInstanceGroup() throws Exception {
        final String runtime = "7.2.12";
        mockTransactionServiceRequired();
        String microDutyJson = FileReaderUtils.readFileFromClasspath("/duties/" + runtime + "/aws/micro_duty.json");
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(microDutyJson, StackV4Request.class));
        when(sdxReactorFlowManager.triggerSdxCreation(any())).thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest(runtime, MICRO_DUTY);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        withCloudStorage(sdxClusterRequest);
        withRecipe(sdxClusterRequest);
        withCustomInstanceGroups(sdxClusterRequest);
        RecipeViewV4Responses recipeViewV4Responses = new RecipeViewV4Responses();
        RecipeViewV4Response recipeViewV4Response = new RecipeViewV4Response();
        recipeViewV4Response.setName("post-service-deployment");
        recipeViewV4Responses.setResponses(List.of(recipeViewV4Response));
        when(recipeV4Endpoint.listInternal(anyLong(), anyString())).thenReturn(recipeViewV4Responses);
        long id = 10L;
        when(sdxClusterRepository.save(any(SdxCluster.class))).thenAnswer(invocation -> {
            SdxCluster sdxWithId = invocation.getArgument(0, SdxCluster.class);
            sdxWithId.setId(id);
            return sdxWithId;
        });
        when(clock.getCurrentTimeMillis()).thenReturn(1L);
        mockEnvironmentCall(sdxClusterRequest, AWS, null);
        when(entitlementService.microDutySdxEnabled(anyString())).thenReturn(true);

        Pair<SdxCluster, FlowIdentifier> result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null));

        SdxCluster createdSdxCluster = result.getLeft();
        assertEquals(id, createdSdxCluster.getId());
        ArgumentCaptor<SdxCluster> captor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository, times(1)).save(captor.capture());
        verify(recipeV4Endpoint, times(1)).listInternal(anyLong(), anyString());
        SdxCluster capturedSdx = captor.getValue();
        assertEquals(MICRO_DUTY, capturedSdx.getClusterShape());
        StackV4Request stackRequest = JsonUtil.readValue(capturedSdx.getStackRequest(), StackV4Request.class);
        Optional<InstanceGroupV4Request> masterGroup = stackRequest.getInstanceGroups().stream()
                .filter(instanceGroup -> "master".equals(instanceGroup.getName()))
                .findAny();
        assertTrue(masterGroup.isPresent());
        assertEquals("verylarge", masterGroup.get().getTemplate().getInstanceType());
        Optional<InstanceGroupV4Request> idbrokerGroup = stackRequest.getInstanceGroups().stream()
                .filter(instanceGroup -> "idbroker".equals(instanceGroup.getName()))
                .findAny();
        assertTrue(idbrokerGroup.isPresent());
        assertEquals("notverylarge", idbrokerGroup.get().getTemplate().getInstanceType());
    }

    @ParameterizedTest(name = "Runtime {0} is compatible with CCMv2 = {1}")
    @MethodSource("ccmV2Scenarios")
    void testCcmV2VersionChecker(String runtime, boolean compatible) throws IOException {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest(runtime, LIGHT_DUTY);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        String lightDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.10/aws/light_duty.json");
        lenient().when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(lightDutyJson, StackV4Request.class));
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.MOCK, Tunnel.CCMV2);
        if (!compatible) {
            assertThatThrownBy(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage(String.format("Runtime version %s does not support Cluster Connectivity Manager. "
                            + "Please try creating a datalake with runtime version at least %s.", runtime, SdxVersionRuleEnforcer.CCMV2_REQUIRED_VERSION));
        } else {
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null));
        }
    }

    @ParameterizedTest(name = "Runtime {0} is compatible with CCMv2JumpGate = {1}")
    @MethodSource("ccmV2JumpgateScenarios")
    void testCcmV2JumpgateVersionChecker(String runtime, boolean compatible) throws IOException {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest(runtime, LIGHT_DUTY);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        String lightDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.10/aws/light_duty.json");
        lenient().when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(lightDutyJson, StackV4Request.class));
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.MOCK, Tunnel.CCMV2_JUMPGATE);
        if (!compatible) {
            assertThatThrownBy(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage(String.format("Runtime version %s does not support Cluster Connectivity Manager. "
                            + "Please try creating a datalake with runtime version at least %s.", runtime, CCMV2_JUMPGATE_REQUIRED_VERSION));
        } else {
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null));
        }
    }

    @Test
    void testCreateMicroDuty() throws IOException, TransactionExecutionException {
        final String runtime = "7.2.12";
        mockTransactionServiceRequired();
        String microDutyJson = FileReaderUtils.readFileFromClasspath("/duties/" + runtime + "/aws/micro_duty.json");
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(microDutyJson, StackV4Request.class));
        when(sdxReactorFlowManager.triggerSdxCreation(any())).thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest(runtime, MICRO_DUTY);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        withCloudStorage(sdxClusterRequest);
        withRecipe(sdxClusterRequest);
        RecipeViewV4Responses recipeViewV4Responses = new RecipeViewV4Responses();
        RecipeViewV4Response recipeViewV4Response = new RecipeViewV4Response();
        recipeViewV4Response.setName("post-service-deployment");
        recipeViewV4Responses.setResponses(List.of(recipeViewV4Response));
        when(recipeV4Endpoint.listInternal(anyLong(), anyString())).thenReturn(recipeViewV4Responses);
        long id = 10L;
        when(sdxClusterRepository.save(any(SdxCluster.class))).thenAnswer(invocation -> {
            SdxCluster sdxWithId = invocation.getArgument(0, SdxCluster.class);
            sdxWithId.setId(id);
            return sdxWithId;
        });
        when(clock.getCurrentTimeMillis()).thenReturn(1L);
        mockEnvironmentCall(sdxClusterRequest, AWS, null);
        when(entitlementService.microDutySdxEnabled(anyString())).thenReturn(true);
        Pair<SdxCluster, FlowIdentifier> result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null));
        SdxCluster createdSdxCluster = result.getLeft();
        assertEquals(id, createdSdxCluster.getId());
        ArgumentCaptor<SdxCluster> captor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository, times(1)).save(captor.capture());
        verify(recipeV4Endpoint, times(1)).listInternal(anyLong(), anyString());
        SdxCluster capturedSdx = captor.getValue();
        assertEquals(MICRO_DUTY, capturedSdx.getClusterShape());
    }

    @Test
    void testCreateMicroDutyWrongVersion() {
        final String runtime = "7.2.11";
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest(runtime, MICRO_DUTY);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        mockEnvironmentCall(sdxClusterRequest, AZURE, null);
        when(entitlementService.microDutySdxEnabled(anyString())).thenReturn(true);
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null)));
        assertEquals("Provisioning a Micro Duty SDX shape is only valid for CM version greater than or equal to 7.2.12 and not 7.2.11",
                badRequestException.getMessage());
    }

    @Test
    void testCreateMicroDutyNoEntitlement() {
        final String runtime = "7.2.12";
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest(runtime, MICRO_DUTY);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        mockEnvironmentCall(sdxClusterRequest, AZURE, null);
        when(entitlementService.microDutySdxEnabled(anyString())).thenReturn(false);
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null)));
        assertEquals(String.format("Provisioning a micro duty data lake cluster is not enabled for %s. ", AZURE.name()) +
                "Contact Cloudera support to enable CDP_MICRO_DUTY_SDX entitlement for the account.", badRequestException.getMessage());
    }

    @Test
    void testCreateEnterpriseDatalake() throws IOException, TransactionExecutionException {
        final String runtime = "7.2.17";
        mockTransactionServiceRequired();
        String enterpriseJson = FileReaderUtils.readFileFromClasspath("/duties/" + runtime + "/aws/enterprise.json");
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(enterpriseJson, StackV4Request.class));
        when(sdxReactorFlowManager.triggerSdxCreation(any())).thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest(runtime, ENTERPRISE);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        withCloudStorage(sdxClusterRequest);
        withRecipe(sdxClusterRequest);
        RecipeViewV4Responses recipeViewV4Responses = new RecipeViewV4Responses();
        RecipeViewV4Response recipeViewV4Response = new RecipeViewV4Response();
        recipeViewV4Response.setName("post-service-deployment");
        recipeViewV4Responses.setResponses(List.of(recipeViewV4Response));
        when(recipeV4Endpoint.listInternal(anyLong(), anyString())).thenReturn(recipeViewV4Responses);
        long id = 10L;
        when(sdxClusterRepository.save(any(SdxCluster.class))).thenAnswer(invocation -> {
            SdxCluster sdxWithId = invocation.getArgument(0, SdxCluster.class);
            sdxWithId.setId(id);
            return sdxWithId;
        });
        when(clock.getCurrentTimeMillis()).thenReturn(1L);
        mockEnvironmentCall(sdxClusterRequest, AWS, null);
        Pair<SdxCluster, FlowIdentifier> result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null));
        SdxCluster createdSdxCluster = result.getLeft();
        assertEquals(id, createdSdxCluster.getId());
        ArgumentCaptor<SdxCluster> captor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository, times(1)).save(captor.capture());
        verify(recipeV4Endpoint, times(1)).listInternal(anyLong(), anyString());
        SdxCluster capturedSdx = captor.getValue();
        assertEquals(ENTERPRISE, capturedSdx.getClusterShape());
    }

    @Test
    void testCreateEnterpriseDatalakeWrongVersion() {
        final String runtime = "7.2.11";
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest(runtime, ENTERPRISE);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        mockEnvironmentCall(sdxClusterRequest, AZURE, null);
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null)));
        assertEquals("Provisioning an Enterprise SDX shape is only valid for CM version greater than or equal to 7.2.17 and not 7.2.11",
                badRequestException.getMessage());
    }

    @Test
    void testCreateSdxClusterFialsInCaseOfForcedJavaVersionIsNotSupportedByTheVirtualMachineConfiguration() {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest(null, MEDIUM_DUTY_HA);
        sdxClusterRequest.setJavaVersion(11);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(
                anyString(), anyString())).thenReturn(Collections.emptyList());
        when(virtualMachineConfiguration.isJavaVersionSupported(11)).thenReturn(false);
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                        underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null)));
        assertEquals("Java version 11 is not supported.", badRequestException.getMessage());
    }

    private void setSpot(SdxClusterRequest sdxClusterRequest) {
        SdxAwsRequest aws = new SdxAwsRequest();
        SdxAwsSpotParameters spot = new SdxAwsSpotParameters();
        spot.setPercentage(100);
        spot.setMaxPrice(0.9);
        aws.setSpot(spot);
        sdxClusterRequest.setAws(aws);
    }

    private ImageV4Response getImageResponse() {
        Map<String, Map<String, String>> imageSetsByProvider = new HashMap<>();
        imageSetsByProvider.put("aws", null);
        BaseStackDetailsV4Response stackDetails = new BaseStackDetailsV4Response();
        stackDetails.setVersion("7.2.7");

        ImageV4Response imageV4Response = new ImageV4Response();
        imageV4Response.setOs(OS);
        imageV4Response.setImageSetsByProvider(imageSetsByProvider);
        imageV4Response.setStackDetails(stackDetails);
        return imageV4Response;
    }

    private DetailedEnvironmentResponse mockEnvironmentCall(SdxClusterRequest sdxClusterRequest, CloudPlatform cloudPlatform, Tunnel tunnel) {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setName(sdxClusterRequest.getEnvironment());
        detailedEnvironmentResponse.setCloudPlatform(cloudPlatform.name());
        detailedEnvironmentResponse.setEnvironmentStatus(EnvironmentStatus.AVAILABLE);
        detailedEnvironmentResponse.setCrn(getCrn());
        detailedEnvironmentResponse.setCreator(detailedEnvironmentResponse.getCrn());
        detailedEnvironmentResponse.setAccountId(UUID.randomUUID().toString());
        detailedEnvironmentResponse.setTunnel(tunnel);
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);
        return detailedEnvironmentResponse;
    }

    private String getCrn() {
        return CrnTestUtil.getEnvironmentCrnBuilder()
                .setResource(UUID.randomUUID().toString())
                .setAccountId(UUID.randomUUID().toString())
                .build().toString();
    }

    private InstanceGroupV4Request getGroup(StackV4Request stack, com.sequenceiq.common.api.type.InstanceGroupType type) {
        for (InstanceGroupV4Request instanceGroup : stack.getInstanceGroups()) {
            if (instanceGroup.getType().equals(type)) {
                return instanceGroup;
            }
        }
        return null;
    }

    private SdxClusterRequest createSdxClusterRequest(String runtime, SdxClusterShape shape) {
        SdxClusterRequest sdxClusterRequest = getSdxClusterRequest(shape);
        sdxClusterRequest.setRuntime(runtime);
        return sdxClusterRequest;
    }

    private SdxClusterRequest createSdxClusterRequest(SdxClusterShape shape, String catalog, String imageId) {
        SdxClusterRequest sdxClusterRequest = getSdxClusterRequest(shape);

        ImageSettingsV4Request imageSettingsV4Request = new ImageSettingsV4Request();
        imageSettingsV4Request.setCatalog(catalog);
        imageSettingsV4Request.setId(imageId);

        sdxClusterRequest.setImage(imageSettingsV4Request);
        return sdxClusterRequest;
    }

    private SdxClusterRequest getSdxClusterRequest(SdxClusterShape shape) {
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setClusterShape(shape);
        sdxClusterRequest.addTags(TAGS);
        sdxClusterRequest.setEnvironment("envir");
        sdxClusterRequest.setExternalDatabase(new SdxDatabaseRequest());
        return sdxClusterRequest;
    }

    private void withRecipe(SdxClusterRequest sdxClusterRequest) {
        SdxRecipe recipe = new SdxRecipe();
        recipe.setHostGroup("master");
        recipe.setName("post-service-deployment");
        sdxClusterRequest.setRecipes(Set.of(recipe));
    }

    private void withCloudStorage(SdxClusterRequest sdxClusterRequest) {
        SdxCloudStorageRequest cloudStorage = new SdxCloudStorageRequest();
        cloudStorage.setFileSystemType(FileSystemType.S3);
        cloudStorage.setBaseLocation("s3a://some/dir/");
        cloudStorage.setS3(new S3CloudStorageV1Parameters());
        sdxClusterRequest.setCloudStorage(cloudStorage);
    }

    private void withCustomInstanceGroups(SdxClusterRequest sdxClusterRequest) {
        sdxClusterRequest.setCustomInstanceGroups(List.of(withInstanceGroup("master", "verylarge"),
                withInstanceGroup("idbroker", "notverylarge")));
    }

    private SdxInstanceGroupRequest withInstanceGroup(String name, String instanceType) {
        SdxInstanceGroupRequest masterInstanceGroup = new SdxInstanceGroupRequest();
        masterInstanceGroup.setName(name);
        masterInstanceGroup.setInstanceType(instanceType);
        return masterInstanceGroup;
    }

    private String readLightDutyTestTemplate() throws IOException {
        return FileReaderUtils.readFileFromClasspath("/duties/7.2.17/aws/light_duty.json");
    }
}
