package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.common.api.type.InstanceGroupType.CORE;
import static com.sequenceiq.common.api.type.InstanceGroupType.GATEWAY;
import static com.sequenceiq.datalake.service.sdx.SdxService.CCMV2_JUMPGATE_REQUIRED_VERSION;
import static com.sequenceiq.datalake.service.sdx.SdxService.CCMV2_REQUIRED_VERSION;
import static com.sequenceiq.datalake.service.sdx.SdxService.MEDIUM_DUTY_REQUIRED_VERSION;
import static com.sequenceiq.datalake.service.sdx.SdxService.WORKSPACE_ID_DEFAULT;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.CUSTOM;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.LIGHT_DUTY;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.MEDIUM_DUTY_HA;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.MICRO_DUTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;
import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.ImageCatalogV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.BaseStackDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.RecipeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.RangerRazEnabledV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerProductV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.cloudbreak.client.CloudbreakServiceCrnEndpoints;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.datalake.configuration.CDPConfigService;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.EnvironmentClientService;
import com.sequenceiq.datalake.service.imagecatalog.ImageCatalogService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.datalake.service.validation.cloudstorage.CloudStorageLocationValidator;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.service.FlowCancelService;
import com.sequenceiq.sdx.api.model.SdxAwsRequest;
import com.sequenceiq.sdx.api.model.SdxAwsSpotParameters;
import com.sequenceiq.sdx.api.model.SdxCloudStorageRequest;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterRequestBase;
import com.sequenceiq.sdx.api.model.SdxClusterResizeRequest;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxCustomClusterRequest;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxInstanceGroupRequest;
import com.sequenceiq.sdx.api.model.SdxRecipe;

@ExtendWith(MockitoExtension.class)
@DisplayName("SDX service tests")
class SdxServiceTest {

    private static final Map<String, String> TAGS = Collections.singletonMap("mytag", "tagecske");

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:hortonworks:user:perdos@hortonworks.com";

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:default:environment:e438a2db-d650-4132-ae62-242c5ba2f784";

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:default:datalake:e438a2db-d650-4132-ae62-242c5ba2f784";

    private static final Long SDX_ID = 2L;

    private static final String SDX_CRN = "crn";

    private static final String CLUSTER_NAME = "test-sdx-cluster";

    @Mock
    private SdxExternalDatabaseConfigurer externalDatabaseConfigurer;

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Mock
    private EnvironmentClientService environmentClientService;

    @Mock
    private StackRequestManifester stackRequestManifester;

    @Mock
    private Clock clock;

    @Mock
    private CloudStorageManifester cloudStorageManifester;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private DistroXV1Endpoint distroXV1Endpoint;

    @Mock
    private SdxNotificationService notificationService;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private RecipeV4Endpoint recipeV4Endpoint;

    @Mock
    private ImageCatalogV4Endpoint imageCatalogV4Endpoint;

    @Mock
    private CloudbreakInternalCrnClient cloudbreakInternalCrnClient;

    @Mock
    private CloudbreakServiceCrnEndpoints cloudbreakServiceCrnEndpoints;

    @Mock
    private DistroxService distroxService;

    @Mock
    private CloudStorageLocationValidator cloudStorageLocationValidator;

    @Mock
    private CDPConfigService cdpConfigService;

    @Mock
    private FlowCancelService flowCancelService;

    @Mock
    private OwnerAssignmentService ownerAssignmentService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private Image image;

    @Mock
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @Mock
    private FlowLogService flowLogService;

    @InjectMocks
    private SdxService underTest;

    static Object[][] startParamProvider() {
        return new Object[][]{
                {EnvironmentStatus.ENV_STOPPED, "The environment is stopped. Please start the environment first!"},
                {EnvironmentStatus.STOP_FREEIPA_STARTED, "The environment is stopped. Please start the environment first!"},
                {EnvironmentStatus.START_FREEIPA_STARTED, "The environment is starting. Please wait until finished!"}
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

    static Object[][] razCloudPlatformDataProvider() {
        return new Object[][]{
                // testCaseName cloudPlatform
                {"CloudPlatform.AWS multiaz=true", CloudPlatform.AWS, true},
                {"CloudPlatform.AWS multiaz=false", CloudPlatform.AWS, false},
                {"CloudPlatform.AZURE multiaz=false", CloudPlatform.AZURE, false}
        };
    }

    static Object[][] razCloudPlatformAndRuntimeDataProvider() {
        return new Object[][]{
                // testCaseName cloudPlatform runtime
                {"CloudPlatform.AWS", CloudPlatform.AWS, "7.2.2"},
                {"CloudPlatform.AZURE", CloudPlatform.AZURE, "7.2.1"}
        };
    }

    static Object[][] razCloudPlatform710DataProvider() {
        return new Object[][]{
                // testCaseName cloudPlatform expectedErrorMsg
                {"CloudPlatform.AWS", CloudPlatform.AWS,
                        "Provisioning Ranger Raz on Amazon Web Services is only valid for CM version greater than or equal to 7.2.2 and not 7.1.0"},
                {"CloudPlatform.AZURE", CloudPlatform.AZURE,
                        "Provisioning Ranger Raz on Microsoft Azure is only valid for CM version greater than or equal to 7.2.1 and not 7.1.0"}
        };
    }

    static Object[][] razCloudPlatform720DataProvider() {
        return new Object[][]{
                // testCaseName cloudPlatform expectedErrorMsg
                {"CloudPlatform.AWS", CloudPlatform.AWS,
                        "Provisioning Ranger Raz on Amazon Web Services is only valid for CM version greater than or equal to 7.2.2 and not 7.2.0"},
                {"CloudPlatform.AZURE", CloudPlatform.AZURE,
                        "Provisioning Ranger Raz on Microsoft Azure is only valid for CM version greater than or equal to 7.2.1 and not 7.2.0"}
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

    public static Stream<Arguments> storageBaseLocationsWhiteSpaceValidation() {
        return Stream.of(
                Arguments.of(" abfs://myscontainer@mystorage", ValidationResult.State.VALID),
                Arguments.of("abfs://myscontainer @mystorage ", ValidationResult.State.ERROR),
                Arguments.of("a bfs://myscontainer@mystorage ", ValidationResult.State.ERROR),
                Arguments.of("s3a://mybucket/mylocation      ", ValidationResult.State.VALID),
                Arguments.of("abfs://myscontainer@mystorage ", ValidationResult.State.VALID));
    }

    @BeforeEach
    void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testGetSdxClusterWhenClusterNameProvidedShouldReturnSdxClusterWithTheSameNameAsTheRequest() {
        SdxCluster sdxCluser = new SdxCluster();
        sdxCluser.setEnvName("env");
        sdxCluser.setClusterName(CLUSTER_NAME);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(eq("hortonworks"), eq(CLUSTER_NAME)))
                .thenReturn(Optional.of(sdxCluser));
        SdxCluster returnedSdxCluster = underTest.getByNameInAccount(USER_CRN, CLUSTER_NAME);
        assertEquals(sdxCluser, returnedSdxCluster);
    }

    @Test
    void testGetSdxClusterByNameOrCrnWhenClusterNameProvidedShouldReturnSdxClusterWithTheSameNameAsTheRequest() {
        SdxCluster sdxCluser = new SdxCluster();
        sdxCluser.setEnvName("env");
        sdxCluser.setClusterName(CLUSTER_NAME);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(eq("hortonworks"), eq(CLUSTER_NAME)))
                .thenReturn(Optional.of(sdxCluser));
        SdxCluster returnedSdxCluster = underTest.getByNameOrCrn(USER_CRN, NameOrCrn.ofName(CLUSTER_NAME));
        assertEquals(sdxCluser, returnedSdxCluster);
    }

    @Test
    void testGetSdxClusterWhenClusterCrnProvidedShouldReturnSdxClusterWithTheSameCrnAsTheRequest() {
        SdxCluster sdxCluser = new SdxCluster();
        sdxCluser.setEnvName("env");
        sdxCluser.setClusterName(CLUSTER_NAME);
        when(sdxClusterRepository.findByAccountIdAndCrnAndDeletedIsNull(eq("hortonworks"), eq(ENVIRONMENT_CRN))).thenReturn(Optional.of(sdxCluser));
        SdxCluster returnedSdxCluster = underTest.getByCrn(USER_CRN, ENVIRONMENT_CRN);
        assertEquals(sdxCluser, returnedSdxCluster);
    }

    @Test
    void testGetSdxClusterOnlyWithCrnWhenClusterCrnProvidedShouldReturnSdxClusterWithTheSameCrnAsTheRequest() {
        SdxCluster sdxCluser = new SdxCluster();
        sdxCluser.setEnvName("env");
        sdxCluser.setCrn(CLUSTER_NAME);
        when(sdxClusterRepository.findByCrnAndDeletedIsNull(eq(ENVIRONMENT_CRN))).thenReturn(Optional.of(sdxCluser));
        SdxCluster returnedSdxCluster = underTest.getByCrn(ENVIRONMENT_CRN);
        assertEquals(sdxCluser, returnedSdxCluster);
    }

    @Test
    void testGetSdxClusterByNameOrCrnWhenClusterCrnProvidedShouldReturnSdxClusterWithTheSameCrnAsTheRequest() {
        SdxCluster sdxCluser = new SdxCluster();
        sdxCluser.setEnvName("env");
        sdxCluser.setClusterName(CLUSTER_NAME);
        when(sdxClusterRepository.findByAccountIdAndCrnAndDeletedIsNull(eq("hortonworks"), eq(ENVIRONMENT_CRN))).thenReturn(Optional.of(sdxCluser));
        SdxCluster returnedSdxCluster = underTest.getByNameOrCrn(USER_CRN, NameOrCrn.ofCrn(ENVIRONMENT_CRN));
        assertEquals(sdxCluser, returnedSdxCluster);
    }

    @Test
    void testGetSdxClusterByAccountIdWhenNoDeployedClusterShouldThrowSdxNotFoundException() {
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.empty());
        NotFoundException notFoundException = assertThrows(NotFoundException.class, () -> underTest.getByNameInAccount(USER_CRN, "sdxcluster"));
        assertEquals("SDX cluster 'sdxcluster' not found.", notFoundException.getMessage());
    }

    @Test
    void testUpdateRangerRazEnabledForSdxClusterWhenRangerRazIsPresent() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setEnvName("env");
        sdxCluster.setEnvCrn(ENVIRONMENT_CRN);
        sdxCluster.setClusterName(CLUSTER_NAME);
        sdxCluster.setCrn("test-crn");
        sdxCluster.setRuntime("7.2.11");

        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCreator(USER_CRN);
        environmentResponse.setCloudPlatform("AWS");

        RangerRazEnabledV4Response response = mock(RangerRazEnabledV4Response.class);
        when(stackV4Endpoint.rangerRazEnabledInternal(anyLong(), anyString(), anyString())).thenReturn(response);
        when(response.isRangerRazEnabled()).thenReturn(true);
        when(environmentClientService.getByCrn(anyString())).thenReturn(environmentResponse);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.updateRangerRazEnabled(sdxCluster));

        assertTrue(sdxCluster.isRangerRazEnabled());
        verify(sdxClusterRepository, times(1)).save(sdxCluster);
    }

    @Test
    void testUpdateRangerRazThrowsExceptionForSdxClusterWhenRangerRazIsNotPresent() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setEnvName("env");
        sdxCluster.setClusterName(CLUSTER_NAME);
        sdxCluster.setCrn("test-crn");
        sdxCluster.setRuntime("7.2.11");

        RangerRazEnabledV4Response response = mock(RangerRazEnabledV4Response.class);
        when(stackV4Endpoint.rangerRazEnabledInternal(anyLong(), anyString(), anyString())).thenReturn(response);
        when(response.isRangerRazEnabled()).thenReturn(false);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.updateRangerRazEnabled(sdxCluster)));
        assertEquals(String.format("Ranger raz is not installed on the datalake: %s!", CLUSTER_NAME), exception.getMessage());
        verify(sdxClusterRepository, times(0)).save(sdxCluster);
    }

    @Test
    void testUpdateRangerRazIsIgnoredIfRangerRazIsInstalledAndFlagAlreadySet() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setEnvName("env");
        sdxCluster.setClusterName(CLUSTER_NAME);
        sdxCluster.setRangerRazEnabled(true);
        sdxCluster.setCrn("test-crn");
        sdxCluster.setRuntime("7.2.11");

        RangerRazEnabledV4Response response = mock(RangerRazEnabledV4Response.class);
        when(stackV4Endpoint.rangerRazEnabledInternal(anyLong(), anyString(), anyString())).thenReturn(response);
        when(response.isRangerRazEnabled()).thenReturn(true);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.updateRangerRazEnabled(sdxCluster));

        verify(sdxClusterRepository, times(0)).save(sdxCluster);
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
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.AWS, null);
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                        underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null)));
        assertEquals("Cloud storage parameter is required.", badRequestException.getMessage());
    }

    @Test
    void testCreateSdxClusterWithoutCloudStorageShouldNotThrownBadRequestExceptionInCaseOfInternal() {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest("7.2.1", CUSTOM);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.AWS, null);
        StackV4Request stackV4Request = new StackV4Request();
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        stackV4Request.setCluster(clusterV4Request);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, stackV4Request));
    }

    @Test
    void testCreateNOTInternalSdxClusterFromLightDutyTemplateShouldTriggerSdxCreationFlow() throws IOException, TransactionExecutionException {
        CrnTestUtil.mockCrnGenerator(regionAwareCrnGenerator);
        when(transactionService.required(isA(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());
        String lightDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.1.0/aws/light_duty.json");
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
        when(clock.getCurrentTimeMillis()).thenReturn(1L);
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.AZURE, null);
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
        assertEquals(USER_CRN, capturedSdx.getInitiatorUserCrn());
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
        when(transactionService.required(isA(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());
        String lightDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.1.0/aws/light_duty.json");
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(lightDutyJson, StackV4Request.class));
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest(null, LIGHT_DUTY);
        withCloudStorage(sdxClusterRequest);
        long id = 10L;
        when(sdxClusterRepository.save(any(SdxCluster.class))).thenAnswer(invocation -> {
            SdxCluster sdxWithId = invocation.getArgument(0, SdxCluster.class);
            sdxWithId.setId(id);
            return sdxWithId;
        });
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.AWS, null);
        Pair<SdxCluster, FlowIdentifier> result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null));
        SdxCluster createdSdxCluster = result.getLeft();
        assertEquals("s3a://some/dir", createdSdxCluster.getCloudStorageBaseLocation());
    }

    @Test
    void testCreateSdxClusterWithSpotStackRequestContainsRequiredAttributes() throws IOException, TransactionExecutionException {
        when(transactionService.required(isA(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());
        String lightDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.1.0/aws/light_duty.json");
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
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.AWS, null);
        Pair<SdxCluster, FlowIdentifier> result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null));
        SdxCluster createdSdxCluster = result.getLeft();
        // AWS 7.1.0 light duty contains exactly 2 instance groups
        assertThat(createdSdxCluster.getStackRequest()).containsSubsequence(
                "{\"aws\":{\"spot\":{\"percentage\":100,\"maxPrice\":0.9}}",
                "{\"aws\":{\"spot\":{\"percentage\":100,\"maxPrice\":0.9}}");
    }

    @Test
    void testCreateNOTInternalSdxClusterFromLightDutyTemplateWhenBaseLocationSpecifiedShouldCreateStackRequestWithSettedUpBaseLocation()
            throws IOException, TransactionExecutionException {
        when(transactionService.required(isA(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());
        String lightDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.1.0/aws/light_duty.json");
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
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.AWS, null);
        Pair<SdxCluster, FlowIdentifier> result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null));
        SdxCluster createdSdxCluster = result.getLeft();
        assertEquals("s3a://some/dir", createdSdxCluster.getCloudStorageBaseLocation());
    }

    @ParameterizedTest
    @MethodSource("storageBaseLocationsWhiteSpaceValidation")
    void testValidateBaseLocationWhenWhiteSpaceIsPresent(String input, ValidationResult.State expected) {
        ValidationResult result = underTest.validateBaseLocation(input);
        assertEquals(expected, result.getState());
    }

    @MethodSource("startParamProvider")
    void testCreateButEnvInStoppedStatus(EnvironmentStatus environmentStatus, String exceptionMessage) {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest(null, MEDIUM_DUTY_HA);

        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setName(sdxClusterRequest.getEnvironment());
        detailedEnvironmentResponse.setCloudPlatform(CloudPlatform.AWS.name());
        detailedEnvironmentResponse.setCrn(getCrn());
        detailedEnvironmentResponse.setEnvironmentStatus(environmentStatus);
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null)),
                "BadRequestException should thrown");
        assertEquals(exceptionMessage, badRequestException.getMessage());
    }

    @ParameterizedTest
    @MethodSource("deleteInProgressParamProvider")
    void testCreateButEnvInDeleteInProgressPhase(EnvironmentStatus environmentStatus) {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest(null, MEDIUM_DUTY_HA);
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setName(sdxClusterRequest.getEnvironment());
        detailedEnvironmentResponse.setCloudPlatform(CloudPlatform.AWS.name());
        detailedEnvironmentResponse.setCrn(getCrn());
        detailedEnvironmentResponse.setEnvironmentStatus(environmentStatus);
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null)),
                "BadRequestException should thrown");
        assertEquals("The environment is in delete in progress phase. Please create a new environment first!", badRequestException.getMessage());
    }

    @ParameterizedTest
    @MethodSource("failedParamProvider")
    void testCreateButEnvInFailedPhase(EnvironmentStatus environmentStatus) {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest(null, MEDIUM_DUTY_HA);
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setName(sdxClusterRequest.getEnvironment());
        detailedEnvironmentResponse.setCloudPlatform(CloudPlatform.AWS.name());
        detailedEnvironmentResponse.setCrn(getCrn());
        detailedEnvironmentResponse.setEnvironmentStatus(environmentStatus);
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null)),
                "BadRequestException should thrown");
        assertEquals("The environment is in failed phase. Please fix the environment or create a new one first!", badRequestException.getMessage());
    }

    @Test
    void testListSdxClustersWhenEnvironmentNameProvidedAndTwoSdxIsInTheDatabaseShouldListAllSdxClusterWhichIsTwo() {
        List<SdxCluster> sdxClusters = List.of(new SdxCluster(), new SdxCluster());
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(eq("hortonworks"), eq("envir"))).thenReturn(sdxClusters);
        List<SdxCluster> sdxList = underTest.listSdx(USER_CRN, "envir");
        assertEquals(2, sdxList.size());
    }

    @Test
    void testListSdxClustersWhenEnvironmentCrnProvidedAndTwoSdxIsInTheDatabaseShouldListAllSdxClusterWhichIsTwo() {
        List<SdxCluster> sdxClusters = List.of(new SdxCluster(), new SdxCluster());
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsFalse(eq("hortonworks"), eq(ENVIRONMENT_CRN))).thenReturn(sdxClusters);
        List<SdxCluster> sdxList = underTest.listSdxByEnvCrn(USER_CRN, ENVIRONMENT_CRN);
        assertEquals(2, sdxList.size());
    }

    @Test
    void testListSdxClustersWhenInvalidEnvironmentNameProvidedShouldThrowBadRequest() {
        String crn = "crsdfadsfdsf sadasf3-df81ae585e10";
        assertThrows(BadRequestException.class, () -> underTest.listSdx(crn, "envir"));
    }

    @Test
    void testDeleteSdxWhenNameIsProvidedAndClusterDoesNotExistShouldThrowNotFoundException() {
        assertThrows(com.sequenceiq.cloudbreak.common.exception.NotFoundException.class,
                () -> underTest.deleteSdx(USER_CRN, CLUSTER_NAME, false));
        verify(sdxClusterRepository, times(1))
                .findByAccountIdAndClusterNameAndDeletedIsNull(eq("hortonworks"), eq(CLUSTER_NAME));
    }

    @Test
    void testDeleteSdxWhenNameIsProvidedShouldInitiateSdxDeletionFlow() {
        SdxCluster sdxCluster = getSdxCluster();
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxReactorFlowManager.triggerSdxDeletion(any(SdxCluster.class), anyBoolean())).thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));
        mockCBCallForDistroXClusters(Sets.newHashSet());
        underTest.deleteSdx(USER_CRN, "sdx-cluster-name", true);
        verify(sdxReactorFlowManager, times(1)).triggerSdxDeletion(sdxCluster, true);
        ArgumentCaptor<SdxCluster> captor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository, times(1)).save(captor.capture());
        verify(sdxStatusService, times(1))
                .setStatusForDatalakeAndNotify(DatalakeStatusEnum.DELETE_REQUESTED, "Datalake deletion requested", sdxCluster);
    }

    @Test
    void testDeleteSdxWhenSdxHasAttachedDataHubsShouldThrowBadRequestBecauseSdxCanNotDeletedIfAttachedClustersAreAvailable() {
        SdxCluster sdxCluster = getSdxCluster();
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));

        StackViewV4Response stackViewV4Response = new StackViewV4Response();
        stackViewV4Response.setName("existingDistroXCluster");
        stackViewV4Response.setStatus(Status.AVAILABLE);
        mockCBCallForDistroXClusters(Sets.newHashSet(stackViewV4Response));

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.deleteSdx(USER_CRN, "sdx-cluster-name", false));
        assertEquals("The following Data Hub cluster(s) must be stopped before attempting SDX deletion [existingDistroXCluster].",
                badRequestException.getMessage());
    }

    @Test
    void testDeleteSdxWhenSdxHasStoppedDataHubsShouldThrowBadRequest() {
        SdxCluster sdxCluster = getSdxCluster();
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));

        StackViewV4Response stackViewV4Response = new StackViewV4Response();
        stackViewV4Response.setName("existingDistroXCluster");
        stackViewV4Response.setStatus(Status.STOPPED);
        mockCBCallForDistroXClusters(Sets.newHashSet(stackViewV4Response));

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.deleteSdx(USER_CRN, "sdx-cluster-name", false));
        assertEquals("The following stopped Data Hubs clusters(s) must be terminated before SDX deleting [existingDistroXCluster]." +
                " Use --force to skip this check.", badRequestException.getMessage());
    }

    @Test
    void testDeleteSdxWhenSdxHasStoppedDataHubsShouldSucceedWhenForced() {
        SdxCluster sdxCluster = getSdxCluster();
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxReactorFlowManager.triggerSdxDeletion(any(SdxCluster.class), anyBoolean())).thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));

        StackViewV4Response stackViewV4Response = new StackViewV4Response();
        stackViewV4Response.setName("existingDistroXCluster");
        stackViewV4Response.setStatus(Status.STOPPED);
        mockCBCallForDistroXClusters(Sets.newHashSet(stackViewV4Response));

        underTest.deleteSdx(USER_CRN, "sdx-cluster-name", true);
        verify(sdxReactorFlowManager, times(1)).triggerSdxDeletion(sdxCluster, true);
        ArgumentCaptor<SdxCluster> captor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository, times(1)).save(captor.capture());
        verify(sdxStatusService, times(1))
                .setStatusForDatalakeAndNotify(DatalakeStatusEnum.DELETE_REQUESTED, "Datalake deletion requested", sdxCluster);
    }

    @Test
    void testDeleteSdxWhenSdxHasAttachedDataHubsAndExceptionHappensWhenGettingDatahubsAndForceDeleteShouldInitiateSdxDeletionFlow() {
        SdxCluster sdxCluster = getSdxCluster();
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxReactorFlowManager.triggerSdxDeletion(any(SdxCluster.class), anyBoolean())).thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));
        doThrow(new NotFoundException("nope")).when(distroxService).getAttachedDistroXClusters(anyString());

        underTest.deleteSdx(USER_CRN, "sdx-cluster-name", true);
        verify(sdxReactorFlowManager, times(1)).triggerSdxDeletion(sdxCluster, true);
        ArgumentCaptor<SdxCluster> captor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository, times(1)).save(captor.capture());
        verify(sdxStatusService, times(1))
                .setStatusForDatalakeAndNotify(DatalakeStatusEnum.DELETE_REQUESTED, "Datalake deletion requested", sdxCluster);
    }

    @Test
    void testDeleteSdxWhenSdxHasExternalDatabaseButCrnIsMissingShouldThrowNotFoundException() {
        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setDatabaseAvailabilityType(SdxDatabaseAvailabilityType.HA);
        sdxCluster.setDatabaseCrn(null);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.deleteSdx(USER_CRN, "sdx-cluster-name", false));
        assertEquals(String.format("Can not find external database for Data Lake, but it was requested: %s. Please use force delete.",
                sdxCluster.getClusterName()), badRequestException.getMessage());
    }

    @Test
    void testSyncSdxClusterWhenClusterNameSpecifiedShouldCallStackEndpointExactlyOnce() {
        SdxCluster sdxCluser = new SdxCluster();
        sdxCluser.setEnvName("env");
        sdxCluser.setClusterName(CLUSTER_NAME);
        when(sdxClusterRepository.findByAccountIdAndCrnAndDeletedIsNull(eq("hortonworks"), eq(DATALAKE_CRN)))
                .thenReturn(Optional.of(sdxCluser));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        underTest.syncByCrn(USER_CRN, DATALAKE_CRN);

        verify(stackV4Endpoint, times(1)).sync(eq(0L), eq(CLUSTER_NAME), anyString());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("razCloudPlatformDataProvider")
    void testSdxCreateRazNotRequestedAndMultiAzRequested(String testCaseName, CloudPlatform cloudPlatform, boolean multiAz)
            throws IOException, TransactionExecutionException {
        when(transactionService.required(isA(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());
        String lightDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.1.0/aws/light_duty.json");
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(lightDutyJson, StackV4Request.class));
        when(sdxReactorFlowManager.triggerSdxCreation(any())).thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest("7.1.0", LIGHT_DUTY);
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
        Pair<SdxCluster, FlowIdentifier> result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null));
        SdxCluster createdSdxCluster = result.getLeft();
        assertEquals(id, createdSdxCluster.getId());
        ArgumentCaptor<SdxCluster> captor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository, times(1)).save(captor.capture());
        SdxCluster capturedSdx = captor.getValue();
        assertFalse(capturedSdx.isRangerRazEnabled());
        assertEquals(multiAz, capturedSdx.isEnableMultiAz());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("razCloudPlatformAndRuntimeDataProvider")
    void testSdxCreateRazEnabled(String testCaseName, CloudPlatform cloudPlatform, String runtime) throws IOException, TransactionExecutionException {
        when(transactionService.required(isA(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());
        String lightDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.1.0/aws/light_duty.json");
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
    void testSdxCreateRazEnabledNotAwsOrAzure() {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest("7.2.2", LIGHT_DUTY);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.YARN, null);
        sdxClusterRequest.setEnableRangerRaz(true);
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null)));
        assertEquals("Provisioning Ranger Raz is only valid for Amazon Web Services and Microsoft Azure.", badRequestException.getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("razCloudPlatform710DataProvider")
    void testSdxCreateRazEnabled710Runtime(String testCaseName, CloudPlatform cloudPlatform, String expectedErrorMsg) {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest("7.1.0", LIGHT_DUTY);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        mockEnvironmentCall(sdxClusterRequest, cloudPlatform, null);
        sdxClusterRequest.setEnableRangerRaz(true);
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null)));
        assertEquals(expectedErrorMsg, badRequestException.getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("razCloudPlatform720DataProvider")
    void testSdxCreateRazEnabled720Runtime(String testCaseName, CloudPlatform cloudPlatform, String expectedErrorMsg) {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest("7.2.0", LIGHT_DUTY);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        mockEnvironmentCall(sdxClusterRequest, cloudPlatform, null);
        sdxClusterRequest.setEnableRangerRaz(true);
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null)));
        assertEquals(expectedErrorMsg, badRequestException.getMessage());
    }

    @Test
    void testSdxCreateMediumDutySdx() throws IOException, TransactionExecutionException {
        final String runtime = "7.2.7";
        when(transactionService.required(isA(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());
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
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.AWS, null);
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
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.AWS, null);
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
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.AWS, null);
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null)));
        assertEquals("Provisioning a Medium Duty SDX shape is only valid for CM version greater than or equal to "
                + MEDIUM_DUTY_REQUIRED_VERSION + " and not " + invalidRuntime, badRequestException.getMessage());
    }

    @Test
    public void testUpdateRuntimeVersionFromStackResponse() {
        SdxCluster sdxCluster = getSdxCluster();
        StackV4Response stackV4Response = new StackV4Response();
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        ClouderaManagerV4Response cm = new ClouderaManagerV4Response();
        ClouderaManagerProductV4Response cdpResponse = new ClouderaManagerProductV4Response();
        cdpResponse.setName("CDH");
        cdpResponse.setVersion("7.2.1-1.32.123-123");
        cm.setProducts(Collections.singletonList(cdpResponse));
        clusterV4Response.setCm(cm);
        stackV4Response.setCluster(clusterV4Response);

        underTest.updateRuntimeVersionFromStackResponse(sdxCluster, stackV4Response);

        ArgumentCaptor<SdxCluster> sdxClusterArgumentCaptor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository, times(1)).save(sdxClusterArgumentCaptor.capture());
        assertEquals("7.2.1", sdxClusterArgumentCaptor.getValue().getRuntime());

        cdpResponse.setVersion("7.1.0");

        underTest.updateRuntimeVersionFromStackResponse(sdxCluster, stackV4Response);
        verify(sdxClusterRepository, times(2)).save(sdxClusterArgumentCaptor.capture());
        assertEquals("7.1.0", sdxClusterArgumentCaptor.getValue().getRuntime());

        cdpResponse.setVersion("7.0.2-valami");

        underTest.updateRuntimeVersionFromStackResponse(sdxCluster, stackV4Response);
        verify(sdxClusterRepository, times(3)).save(sdxClusterArgumentCaptor.capture());
        assertEquals("7.0.2", sdxClusterArgumentCaptor.getValue().getRuntime());
    }

    @Test
    public void testGetWithEnvironmentCrnsByResourceCrns() {
        SdxCluster cluster1 = new SdxCluster();
        cluster1.setCrn("crn1");
        cluster1.setEnvCrn("envcrn1");
        SdxCluster cluster2 = new SdxCluster();
        cluster2.setCrn("crn2");
        cluster2.setEnvCrn("envcrn2");
        SdxCluster clusterWithoutEnv = new SdxCluster();
        clusterWithoutEnv.setCrn("crn3");
        when(sdxClusterRepository.findAllByAccountIdAndCrnAndDeletedIsNullAndDetachedIsFalse(anyString(), anySet()))
                .thenReturn(List.of(cluster1, cluster2, clusterWithoutEnv));

        Map<String, Optional<String>> result = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getEnvironmentCrnsByResourceCrns(List.of("crn1", "crn2", "crn3")));

        Map<String, Optional<String>> expected = new LinkedHashMap<>();
        expected.put("crn1", Optional.of("envcrn1"));
        expected.put("crn2", Optional.of("envcrn2"));
        expected.put("crn3", Optional.empty());
        assertEquals(expected, result);
    }

    @Test
    void testCreateSdxClusterWithCustomRequestContainsImageInfo() throws Exception {
        ImageV4Response imageResponse = getImageResponse();
        when(transactionService.required(isA(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());
        String lightDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.7/aws/light_duty.json");
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(lightDutyJson, StackV4Request.class));
        SdxCustomClusterRequest sdxClusterRequest = createSdxCustomClusterRequest(LIGHT_DUTY, "cdp-default", "imageId_1");
        setSpot(sdxClusterRequest);
        withCloudStorage(sdxClusterRequest);
        when(imageCatalogService.getImageResponseFromImageRequest(eq(sdxClusterRequest.getImageSettingsV4Request()), any())).thenReturn(imageResponse);

        long id = 10L;
        when(sdxClusterRepository.save(any(SdxCluster.class))).thenAnswer(invocation -> {
            SdxCluster sdxWithId = invocation.getArgument(0, SdxCluster.class);
            sdxWithId.setId(id);
            return sdxWithId;
        });
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.AWS, null);
        Pair<SdxCluster, FlowIdentifier> result = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest));

        SdxCluster createdSdxCluster = result.getLeft();
        StackV4Request stackV4Request = JsonUtil.readValue(createdSdxCluster.getStackRequest(), StackV4Request.class);
        assertNotNull(stackV4Request.getImage());
        assertEquals("cdp-default", stackV4Request.getImage().getCatalog());
        assertEquals("imageId_1", stackV4Request.getImage().getId());
    }

    @Test
    void testCreateInternalSdxClusterWithCustomInstanceGroupShouldFail() {
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        StackV4Request stackV4Request = new StackV4Request();
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        stackV4Request.setCluster(clusterV4Request);
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest("7.2.12", CUSTOM);
        withCustomInstanceGroups(sdxClusterRequest);
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.AWS, null);
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, stackV4Request));
        assertEquals("Custom instance group is not accepted on SDX Internal API.", badRequestException.getMessage());
    }

    @Test
    void testCreateSdxClusterWithCustomInstanceGroup() throws Exception {
        final String runtime = "7.2.12";
        when(transactionService.required(isA(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());
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
        recipeViewV4Response.setName("post-install");
        recipeViewV4Responses.setResponses(List.of(recipeViewV4Response));
        when(recipeV4Endpoint.listInternal(anyLong(), anyString())).thenReturn(recipeViewV4Responses);
        long id = 10L;
        when(sdxClusterRepository.save(any(SdxCluster.class))).thenAnswer(invocation -> {
            SdxCluster sdxWithId = invocation.getArgument(0, SdxCluster.class);
            sdxWithId.setId(id);
            return sdxWithId;
        });
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(clock.getCurrentTimeMillis()).thenReturn(1L);
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.AWS, null);
        when(entitlementService.microDutySdxEnabled(anyString())).thenReturn(true);
        when(entitlementService.isDatalakeSelectInstanceTypeEnabled(anyString())).thenReturn(true);

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

    private void setSpot(SdxClusterRequestBase sdxClusterRequest) {
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
        imageV4Response.setImageSetsByProvider(imageSetsByProvider);
        imageV4Response.setStackDetails(stackDetails);
        return imageV4Response;
    }

    private void mockCBCallForDistroXClusters(Set<StackViewV4Response> stackViews) {
        when(distroxService.getAttachedDistroXClusters(anyString())).thenReturn(stackViews);
    }

    private void mockEnvironmentCall(SdxClusterResizeRequest sdxClusterResizeRequest, CloudPlatform cloudPlatform) {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setName(sdxClusterResizeRequest.getEnvironment());
        detailedEnvironmentResponse.setCloudPlatform(cloudPlatform.name());
        detailedEnvironmentResponse.setEnvironmentStatus(EnvironmentStatus.AVAILABLE);
        detailedEnvironmentResponse.setCrn(getCrn());
        detailedEnvironmentResponse.setCreator(detailedEnvironmentResponse.getCrn());
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);
    }

    private void mockEnvironmentCall(SdxClusterRequestBase sdxClusterRequest, CloudPlatform cloudPlatform, Tunnel tunnel) {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setName(sdxClusterRequest.getEnvironment());
        detailedEnvironmentResponse.setCloudPlatform(cloudPlatform.name());
        detailedEnvironmentResponse.setEnvironmentStatus(EnvironmentStatus.AVAILABLE);
        detailedEnvironmentResponse.setCrn(getCrn());
        detailedEnvironmentResponse.setCreator(detailedEnvironmentResponse.getCrn());
        detailedEnvironmentResponse.setTunnel(tunnel);
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);
    }

    private String getCrn() {
        return CrnTestUtil.getEnvironmentCrnBuilder()
                .setResource(UUID.randomUUID().toString())
                .setAccountId(UUID.randomUUID().toString())
                .build().toString();
    }

    private SdxCluster getSdxCluster() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(SDX_ID);
        sdxCluster.setCrn(SDX_CRN);
        sdxCluster.setInitiatorUserCrn(USER_CRN);
        sdxCluster.setEnvCrn(ENVIRONMENT_CRN);
        sdxCluster.setEnvName("envir");
        sdxCluster.setClusterName("sdx-cluster-name");
        return sdxCluster;
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
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setRuntime(runtime);
        sdxClusterRequest.setClusterShape(shape);
        sdxClusterRequest.addTags(TAGS);
        sdxClusterRequest.setEnvironment("envir");

        return sdxClusterRequest;
    }

    private void withRecipe(SdxClusterRequest sdxClusterRequest) {
        SdxRecipe recipe = new SdxRecipe();
        recipe.setHostGroup("master");
        recipe.setName("post-install");
        sdxClusterRequest.setRecipes(Set.of(recipe));
    }

    private void withCloudStorage(SdxClusterRequestBase sdxClusterRequest) {
        SdxCloudStorageRequest cloudStorage = new SdxCloudStorageRequest();
        cloudStorage.setFileSystemType(FileSystemType.S3);
        cloudStorage.setBaseLocation("s3a://some/dir/");
        cloudStorage.setS3(new S3CloudStorageV1Parameters());
        sdxClusterRequest.setCloudStorage(cloudStorage);
    }

    private void withCustomInstanceGroups(SdxClusterRequestBase sdxClusterRequest) {
        sdxClusterRequest.setCustomInstanceGroups(List.of(withInstanceGroup("master", "verylarge"),
                withInstanceGroup("idbroker", "notverylarge")));
    }

    private SdxInstanceGroupRequest withInstanceGroup(String name, String instanceType) {
        SdxInstanceGroupRequest masterInstanceGroup = new SdxInstanceGroupRequest();
        masterInstanceGroup.setName(name);
        masterInstanceGroup.setInstanceType(instanceType);
        return masterInstanceGroup;
    }

    private SdxCustomClusterRequest createSdxCustomClusterRequest(SdxClusterShape shape, String catalog, String imageId) {
        ImageSettingsV4Request imageSettingsV4Request = new ImageSettingsV4Request();
        imageSettingsV4Request.setCatalog(catalog);
        imageSettingsV4Request.setId(imageId);

        SdxCustomClusterRequest sdxClusterRequest = new SdxCustomClusterRequest();
        sdxClusterRequest.setClusterShape(shape);
        sdxClusterRequest.addTags(TAGS);
        sdxClusterRequest.setEnvironment("envir");
        sdxClusterRequest.setImageSettingsV4Request(imageSettingsV4Request);

        return sdxClusterRequest;
    }

    @Test
    void testSdxResizeByAccountIdAndNameWhenSdxDoesNotExist() {
        SdxClusterResizeRequest sdxClusterResizeRequest = new SdxClusterResizeRequest();
        sdxClusterResizeRequest.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setEnvironment("environment");
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.empty());
        NotFoundException notFoundException = assertThrows(NotFoundException.class, () -> underTest.resizeSdx(USER_CRN, "sdxcluster", sdxClusterResizeRequest));
        assertEquals("SDX cluster 'sdxcluster' not found.", notFoundException.getMessage());
    }

    @Test
    void testSdxResizeByAccountIdAndNameWhenSdxWithSameShape() {
        SdxClusterResizeRequest sdxClusterResizeRequest = new SdxClusterResizeRequest();
        sdxClusterResizeRequest.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setEnvironment("environment");

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setClusterShape(MEDIUM_DUTY_HA);
        sdxCluster.setDatabaseCrn(null);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> underTest.resizeSdx(USER_CRN, "sdxcluster",
                sdxClusterResizeRequest));
        assertEquals("SDX cluster already is of requested shape", badRequestException.getMessage());
    }

    @Test
    void testSdxResizeByAccountIdAndNameWhenSdxOnGcp() {
        SdxClusterResizeRequest sdxClusterResizeRequest = new SdxClusterResizeRequest();
        sdxClusterResizeRequest.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setEnvironment("environment");

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.setCloudStorageFileSystemType(FileSystemType.GCS);
        sdxCluster.setDatabaseCrn(null);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> underTest.resizeSdx(USER_CRN, "sdxcluster",
                sdxClusterResizeRequest));
        assertTrue(badRequestException.getMessage().startsWith("Unsupported cloud provider GCP"));
    }

    @Test
    void testSdxResizeByAccountIdAndNameWhenSdxWithExistingDetachedSdx() {
        SdxClusterResizeRequest sdxClusterResizeRequest = new SdxClusterResizeRequest();
        sdxClusterResizeRequest.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setEnvironment("environment");

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.setDatabaseCrn(null);
        sdxCluster.setDetached(true);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> underTest.resizeSdx(USER_CRN, "sdxcluster",
                sdxClusterResizeRequest));
        assertEquals("SDX which is detached already exists for the environment. SDX name: " + sdxCluster.getClusterName(), badRequestException.getMessage());
    }

    @ParameterizedTest
    @MethodSource("deleteInProgressParamProvider")
    void testSdxResizeButEnvInDeleteInProgressPhase(EnvironmentStatus environmentStatus) {

        SdxClusterResizeRequest sdxClusterResizeRequest = new SdxClusterResizeRequest();
        sdxClusterResizeRequest.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setEnvironment("environment");

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.setDatabaseCrn(null);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setName(sdxClusterResizeRequest.getEnvironment());
        detailedEnvironmentResponse.setCloudPlatform(CloudPlatform.AWS.name());
        detailedEnvironmentResponse.setCrn(getCrn());
        detailedEnvironmentResponse.setEnvironmentStatus(environmentStatus);
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.resizeSdx(USER_CRN, "sdxcluster", sdxClusterResizeRequest)),
                "BadRequestException should thrown");
        assertEquals("The environment is in delete in progress phase. Please create a new environment first!", badRequestException.getMessage());
    }

    @ParameterizedTest
    @MethodSource("failedParamProvider")
    void testSdxResizeButEnvInFailedPhase(EnvironmentStatus environmentStatus) {
        SdxClusterResizeRequest sdxClusterResizeRequest = new SdxClusterResizeRequest();
        sdxClusterResizeRequest.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setEnvironment("environment");

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.setDatabaseCrn(null);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setName(sdxClusterResizeRequest.getEnvironment());
        detailedEnvironmentResponse.setCloudPlatform(CloudPlatform.AWS.name());
        detailedEnvironmentResponse.setCrn(getCrn());
        detailedEnvironmentResponse.setEnvironmentStatus(environmentStatus);
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.resizeSdx(USER_CRN, "sdxcluster", sdxClusterResizeRequest)),
                "BadRequestException should thrown");
        assertEquals("The environment is in failed phase. Please fix the environment or create a new one first!", badRequestException.getMessage());
    }

    @ParameterizedTest
    @MethodSource("startParamProvider")
    void testSdxResizeButEnvInStoppedStatus(EnvironmentStatus environmentStatus, String exceptionMessage) {
        SdxClusterResizeRequest sdxClusterResizeRequest = new SdxClusterResizeRequest();
        sdxClusterResizeRequest.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setEnvironment("environment");

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.setDatabaseCrn(null);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setName(sdxClusterResizeRequest.getEnvironment());
        detailedEnvironmentResponse.setCloudPlatform(CloudPlatform.AWS.name());
        detailedEnvironmentResponse.setCrn(getCrn());
        detailedEnvironmentResponse.setEnvironmentStatus(environmentStatus);
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.resizeSdx(USER_CRN, "sdxcluster", sdxClusterResizeRequest)),
                "BadRequestException should thrown");
        assertEquals(exceptionMessage, badRequestException.getMessage());
    }

    @Test
    void testSdxResizeMediumDutySdxEnabled710Runtime() {
        final String invalidRuntime = "7.1.0";
        SdxClusterResizeRequest sdxClusterResizeRequest = new SdxClusterResizeRequest();
        sdxClusterResizeRequest.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setEnvironment("environment");

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.setDatabaseCrn(null);
        sdxCluster.setRuntime(invalidRuntime);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        mockEnvironmentCall(sdxClusterResizeRequest, CloudPlatform.AWS);
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.resizeSdx(USER_CRN, "sdxcluster", sdxClusterResizeRequest)));
        assertEquals("Provisioning a Medium Duty SDX shape is only valid for CM version greater than or equal to "
                + MEDIUM_DUTY_REQUIRED_VERSION + " and not " + invalidRuntime, badRequestException.getMessage());
    }

    @Test
    void testSdxResizeMediumDutySdxEnabled720Runtime() {
        final String invalidRuntime = "7.2.0";
        SdxClusterResizeRequest sdxClusterResizeRequest = new SdxClusterResizeRequest();
        sdxClusterResizeRequest.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setEnvironment("environment");

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.setDatabaseCrn(null);
        sdxCluster.setRuntime(invalidRuntime);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        mockEnvironmentCall(sdxClusterResizeRequest, CloudPlatform.AWS);
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.resizeSdx(USER_CRN, "sdxcluster", sdxClusterResizeRequest)));
        assertEquals("Provisioning a Medium Duty SDX shape is only valid for CM version greater than or equal to "
                + MEDIUM_DUTY_REQUIRED_VERSION + " and not " + invalidRuntime, badRequestException.getMessage());
    }

    @Test
    void testSdxResizeClusterWithoutCloudStorageShouldThrownBadRequestException() {
        SdxClusterResizeRequest sdxClusterResizeRequest = new SdxClusterResizeRequest();
        sdxClusterResizeRequest.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setEnvironment("environment");

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.setDatabaseCrn(null);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        mockEnvironmentCall(sdxClusterResizeRequest, CloudPlatform.AWS);
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                        underTest.resizeSdx(USER_CRN, "sdxcluster", sdxClusterResizeRequest)));
        assertEquals("Cloud storage parameter is required.", badRequestException.getMessage());
    }

    @Test
    void testSdxResizeClusterSuccess() throws Exception {
        final String runtime = "7.2.10";
        SdxClusterResizeRequest sdxClusterResizeRequest = new SdxClusterResizeRequest();
        sdxClusterResizeRequest.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setEnvironment("environment");

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setId(1L);
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.setDatabaseCrn(null);
        sdxCluster.setRuntime(runtime);
        sdxCluster.setCloudStorageBaseLocation("s3a://some/dir/");

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());

        mockEnvironmentCall(sdxClusterResizeRequest, CloudPlatform.AWS);
        when(sdxReactorFlowManager.triggerSdxResize(anyLong(), any(SdxCluster.class))).thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));

        String mediumDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.10/aws/medium_duty_ha.json");
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(mediumDutyJson, StackV4Request.class));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.STOPPED);
        when(stackV4Endpoint.get(anyLong(), anyString(), anySet(), anyString())).thenReturn(stackV4Response);

        Pair<SdxCluster, FlowIdentifier> result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.resizeSdx(USER_CRN, sdxCluster.getClusterName(), sdxClusterResizeRequest));
        SdxCluster createdSdxCluster = result.getLeft();
        assertEquals(sdxCluster.getClusterName(), createdSdxCluster.getClusterName());
        assertEquals(runtime, createdSdxCluster.getRuntime());
        assertEquals("s3a://some/dir/", createdSdxCluster.getCloudStorageBaseLocation());
        assertEquals("envir", createdSdxCluster.getEnvName());
    }

    @Test
    void testSdxResizeClusterWithNoEntitlement() {
        final String runtime = "7.2.10";
        SdxClusterResizeRequest sdxClusterResizeRequest = new SdxClusterResizeRequest();
        sdxClusterResizeRequest.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setEnvironment("environment");

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setId(1L);
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.setDatabaseCrn(null);
        sdxCluster.setRuntime(runtime);
        sdxCluster.setCloudStorageBaseLocation("s3a://some/dir/");

        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.resizeSdx(USER_CRN, "sdxcluster", sdxClusterResizeRequest)));
        assertEquals("Resizing of the data lake is not supported", badRequestException.getMessage());
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
                            + "Please try creating a datalake with runtime version at least %s.", runtime, CCMV2_REQUIRED_VERSION));
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
        when(transactionService.required(isA(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());
        String microDutyJson = FileReaderUtils.readFileFromClasspath("/duties/" + runtime + "/aws/micro_duty.json");
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(microDutyJson, StackV4Request.class));
        when(sdxReactorFlowManager.triggerSdxCreation(any())).thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest(runtime, MICRO_DUTY);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        withCloudStorage(sdxClusterRequest);
        withRecipe(sdxClusterRequest);
        RecipeViewV4Responses recipeViewV4Responses = new RecipeViewV4Responses();
        RecipeViewV4Response recipeViewV4Response = new RecipeViewV4Response();
        recipeViewV4Response.setName("post-install");
        recipeViewV4Responses.setResponses(List.of(recipeViewV4Response));
        when(recipeV4Endpoint.listInternal(anyLong(), anyString())).thenReturn(recipeViewV4Responses);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        long id = 10L;
        when(sdxClusterRepository.save(any(SdxCluster.class))).thenAnswer(invocation -> {
            SdxCluster sdxWithId = invocation.getArgument(0, SdxCluster.class);
            sdxWithId.setId(id);
            return sdxWithId;
        });
        when(clock.getCurrentTimeMillis()).thenReturn(1L);
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.AWS, null);
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
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.AZURE, null);
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
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.AZURE, null);
        when(entitlementService.microDutySdxEnabled(anyString())).thenReturn(false);
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null)));
        assertEquals(String.format("Provisioning a micro duty data lake cluster is not enabled for %s. ", CloudPlatform.AZURE.name()) +
                "Contact Cloudera support to enable CDP_MICRO_DUTY_SDX entitlement for the account.", badRequestException.getMessage());
    }

    @Test
    void testAttachRecipe() {
        SdxCluster sdxCluster = setupRecipeTest();
        when(stackV4Endpoint.attachRecipeInternal(anyLong(), any(), anyString(), anyString())).thenReturn(null);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.attachRecipe(sdxCluster, null));
        verify(stackV4Endpoint).attachRecipeInternal(anyLong(), any(), eq(sdxCluster.getClusterName()), eq(USER_CRN));
    }

    @Test
    void testDetachRecipe() {
        SdxCluster sdxCluster = setupRecipeTest();
        when(stackV4Endpoint.detachRecipeInternal(anyLong(), any(), anyString(), anyString())).thenReturn(null);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.detachRecipe(sdxCluster, null));
        verify(stackV4Endpoint).detachRecipeInternal(anyLong(), any(), eq(sdxCluster.getClusterName()), eq(USER_CRN));
    }

    @Test
    void testRefreshRecipe() {
        SdxCluster sdxCluster = setupRecipeTest();
        when(stackV4Endpoint.refreshRecipesInternal(anyLong(), any(), anyString(), anyString())).thenReturn(null);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.refreshRecipes(sdxCluster, null));
        verify(stackV4Endpoint).refreshRecipesInternal(anyLong(), any(), eq(sdxCluster.getClusterName()), eq(USER_CRN));
    }

    private SdxCluster setupRecipeTest() {
        SdxCluster sdxCluster = getSdxCluster();
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:iam:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        return sdxCluster;
    }

    @Test
    void rotateSaltPassword() {
        SdxCluster sdxCluster = getSdxCluster();
        when(flowLogService.isOtherFlowRunning(sdxCluster.getId())).thenReturn(false);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.rotateSaltPassword(sdxCluster));

        verify(stackV4Endpoint).rotateSaltPasswordInternal(WORKSPACE_ID_DEFAULT, SDX_CRN, USER_CRN);
        verify(regionAwareInternalCrnGenerator).getInternalCrnForServiceAsString();
    }

    @Test
    void rotateSaltPasswordWithRunningFlow() {
        SdxCluster sdxCluster = getSdxCluster();
        when(flowLogService.isOtherFlowRunning(sdxCluster.getId())).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.rotateSaltPassword(sdxCluster)));

        assertEquals(String.format("Operation is running for cluster '%s'. Please try again later.", sdxCluster.getName()), exception.getMessage());

        verify(stackV4Endpoint, never()).rotateSaltPasswordInternal(WORKSPACE_ID_DEFAULT, sdxCluster.getName(), USER_CRN);
        verify(regionAwareInternalCrnGenerator, never()).getInternalCrnForServiceAsString();
    }

}
