package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.GCP;
import static com.sequenceiq.datalake.service.sdx.SdxService.DATABASE_SSL_ENABLED;
import static com.sequenceiq.datalake.service.sdx.SdxService.PREVIOUS_CLUSTER_SHAPE;
import static com.sequenceiq.datalake.service.sdx.SdxService.PREVIOUS_DATABASE_CRN;
import static com.sequenceiq.datalake.service.sdx.SdxService.WORKSPACE_ID_DEFAULT;
import static com.sequenceiq.datalake.service.sdx.SdxVersionRuleEnforcer.MEDIUM_DUTY_REQUIRED_VERSION;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.ENTERPRISE;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.LIGHT_DUTY;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.MEDIUM_DUTY_HA;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.BaseStackDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.RecipeV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.azure.InstanceGroupAzureNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.gcp.InstanceGroupGcpNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.GcpNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.CloudbreakDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.PlacementSettingsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.RangerRazEnabledV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerProductV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.customdomain.CustomDomainSettingsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.StackImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.network.InstanceGroupNetworkV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.InstanceTemplateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.volume.VolumeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.network.NetworkV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.tags.TagsV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet.Builder;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.datalakedr.DatalakeDrSkipOptions;
import com.sequenceiq.cloudbreak.sdx.TargetPlatform;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.vm.VirtualMachineConfiguration;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.type.EnvironmentType;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.common.model.ImageCatalogPlatform;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.datalake.configuration.CDPConfigService;
import com.sequenceiq.datalake.configuration.PlatformConfig;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.repository.SdxDatabaseRepository;
import com.sequenceiq.datalake.service.imagecatalog.ImageCatalogService;
import com.sequenceiq.datalake.service.sdx.dr.SdxBackupRestoreService;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.datalake.service.validation.resize.SdxResizeValidator;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.sdx.api.model.SdxAwsRequest;
import com.sequenceiq.sdx.api.model.SdxCloudStorageRequest;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterResizeRequest;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseComputeStorageRequest;
import com.sequenceiq.sdx.api.model.SdxInstanceGroupDiskRequest;
import com.sequenceiq.sdx.api.model.SdxInstanceGroupRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("SDX service tests")
class SdxServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:hortonworks:user:perdos@hortonworks.com";

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:default:environment:e438a2db-d650-4132-ae62-242c5ba2f784";

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:default:datalake:e438a2db-d650-4132-ae62-242c5ba2f784";

    private static final String DATABASE_CRN = "crn:cdp:database:us-west-1:default:datalake:e438a2db-d650-4132-ae62-242c5ba2f784";

    private static final Long SDX_ID = 2L;

    private static final String SDX_CRN = "crn";

    private static final String CLUSTER_NAME = "test-sdx-cluster";

    private static final String OS_NAME = "rhel";

    private static final String CATALOG_NAME = "cdp_default";

    private static final String ENVIRONMENT_NAME = "environment-name";

    @Mock
    private SdxClusterRepository sdxClusterRepository;

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
    private DistroXV1Endpoint distroXV1Endpoint;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private DistroxService distroxService;

    @Mock
    private CDPConfigService cdpConfigService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Mock
    private CloudbreakFlowService cloudbreakFlowService;

    @Mock
    private PlatformConfig platformConfig;

    @Mock
    private ImageCatalogPlatform imageCatalogPlatform;

    @Mock
    private SdxBackupRestoreService sdxBackupRestoreService;

    @Mock
    private SdxDatabaseRepository sdxDatabaseRepository;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private VirtualMachineConfiguration virtualMachineConfiguration;

    @Mock
    private PlatformStringTransformer platformStringTransformer;

    @Mock
    private SdxExternalDatabaseConfigurer externalDatabaseConfigurer;

    @Mock
    private OwnerAssignmentService ownerAssignmentService;

    @Mock
    private SdxRecommendationService sdxRecommendationService;

    @Mock
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @Mock
    private SdxResizeValidator sdxResizeValidator;

    @Mock
    private SdxVersionRuleEnforcer sdxVersionRuleEnforcer;

    @Mock
    private MultiAzDecorator multiAzDecorator;

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
        lenient().when(platformConfig.getRazSupportedPlatforms()).thenReturn(List.of(AWS, AZURE, GCP));
        lenient().when(platformConfig.getMultiAzSupportedPlatforms()).thenReturn(Set.of(AWS, AZURE, GCP));
        lenient().doNothing().when(platformAwareSdxConnector).validateIfOtherPlatformsHasSdx(any(), any());
        lenient().when(entitlementService.isEntitledToUseOS(any(), any())).thenReturn(Boolean.TRUE);
    }

    @Test
    void testOtherPlatformValidationFailure() {
        doThrow(BadRequestException.class).when(platformAwareSdxConnector).validateIfOtherPlatformsHasSdx(any(), any());
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCrn(ENVIRONMENT_CRN);
        environmentResponse.setEnvironmentStatus(EnvironmentStatus.AVAILABLE);
        when(environmentClientService.getByName(anyString())).thenReturn(environmentResponse);
        SdxClusterRequest sdxClusterRequest = getSdxClusterRequest();
        sdxClusterRequest.setJavaVersion(null);
        assertThrows(BadRequestException.class, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null));
        verify(platformAwareSdxConnector).validateIfOtherPlatformsHasSdx(eq(ENVIRONMENT_CRN), eq(TargetPlatform.PAAS));
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
    void testGetSdxClustersByCrnWhenCrnProvidedShouldReturnSdxClusters() {
        SdxCluster sdxClusterOriginal = new SdxCluster();
        SdxCluster sdxCluster = new SdxCluster();
        List<SdxCluster> sdxClusterOriginals = new ArrayList<>();
        Optional<SdxCluster> sdxClusterOptional = Optional.of(sdxCluster);
        sdxClusterOriginals.add(sdxClusterOriginal);
        when(sdxClusterRepository.findByAccountIdAndOriginalCrn(any(), any())).thenReturn(sdxClusterOriginals);
        when(sdxClusterRepository.findByAccountIdAndCrnAndDeletedIsNull(any(), any())).thenReturn(sdxClusterOptional);
        List<SdxCluster> result = underTest.getSdxClustersByCrn(USER_CRN, SDX_CRN, true);
        assertEquals(2, result.size());
    }

    @Test
    void testValidateAndGetArchitectureWhenArmRequestedAndImageIsArm() {
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setArchitecture("arm64");
        ImageV4Response imageV4Response = new ImageV4Response();
        imageV4Response.setArchitecture("arm64");
        when(entitlementService.isDataLakeArmEnabled(any())).thenReturn(true);
        Architecture architecture = underTest.validateAndGetArchitecture(sdxClusterRequest, imageV4Response, AWS, "1");
        assertEquals(Architecture.ARM64, architecture);
    }

    @Test
    void testValidateAndGetArchitectureWhenArmRequestedButNotEntitled() {
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setArchitecture("arm64");
        ImageV4Response imageV4Response = new ImageV4Response();
        imageV4Response.setArchitecture("arm64");
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateAndGetArchitecture(sdxClusterRequest, imageV4Response, AWS, "1"));
        assertEquals("The current account is not entitled to use arm64 instances.", exception.getMessage());
    }

    @Test
    void testValidateAndGetArchitectureWhenArmRequestedButAzureBadRequest() {
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setArchitecture("arm64");
        ImageV4Response imageV4Response = new ImageV4Response();
        imageV4Response.setArchitecture("arm64");
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateAndGetArchitecture(sdxClusterRequest, imageV4Response, AZURE, "1"));
        assertEquals("Arm64 is only supported on AWS cloud provider.", exception.getMessage());
    }

    @Test
    void testValidateAndGetArchitectureWhenArmRequestedButIncorrectImage() {
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setArchitecture("arm64");
        ImageV4Response imageV4Response = new ImageV4Response();
        imageV4Response.setUuid("abcdef-12345");
        imageV4Response.setArchitecture("x86_64");
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateAndGetArchitecture(sdxClusterRequest, imageV4Response, AZURE, "1"));
        assertEquals("The selected cpu architecture arm64 doesn't match the cpu architecture x86_64 of the image 'abcdef-12345'.", exception.getMessage());
    }

    @Test
    void testGetSdxClustersByCrnNotFound() {
        SdxCluster sdxClusterOriginal = new SdxCluster();
        SdxCluster sdxCluster = new SdxCluster();
        List<SdxCluster> sdxClusterOriginals = new ArrayList<>();
        Optional<SdxCluster> sdxClusterOptional = Optional.empty();
        sdxClusterOriginals.add(sdxClusterOriginal);
        when(sdxClusterRepository.findByAccountIdAndCrnAndDeletedIsNull(any(), any())).thenReturn(sdxClusterOptional);
        assertThrows(NotFoundException.class, () -> underTest.getSdxClustersByCrn(USER_CRN, SDX_CRN, false));
    }

    @Test
    void testGetSdxClusterWhenClusterCrnProvidedShouldReturnSdxClusterWithTheSameCrnAsTheRequest() {
        SdxCluster sdxCluser = new SdxCluster();
        sdxCluser.setEnvName("env");
        sdxCluser.setClusterName(CLUSTER_NAME);
        sdxCluser.setSeLinux(SeLinux.PERMISSIVE);
        when(sdxClusterRepository.findByAccountIdAndCrnAndDeletedIsNull(eq("hortonworks"), eq(ENVIRONMENT_CRN))).thenReturn(Optional.of(sdxCluser));
        SdxCluster returnedSdxCluster = underTest.getByCrn(USER_CRN, ENVIRONMENT_CRN);
        assertEquals(sdxCluser, returnedSdxCluster);
        assertEquals(SeLinux.PERMISSIVE, returnedSdxCluster.getSeLinux());
    }

    @Test
    void testGetSdxClusterOnlyWithCrnWhenClusterCrnProvidedShouldReturnSdxClusterWithTheSameCrnAsTheRequest() {
        SdxCluster sdxCluser = new SdxCluster();
        sdxCluser.setEnvName("env");
        sdxCluser.setCrn(CLUSTER_NAME);
        sdxCluser.setSeLinux(SeLinux.ENFORCING);
        when(sdxClusterRepository.findByCrnAndDeletedIsNull(eq(ENVIRONMENT_CRN))).thenReturn(Optional.of(sdxCluser));
        SdxCluster returnedSdxCluster = underTest.getByCrn(ENVIRONMENT_CRN);
        assertEquals(sdxCluser, returnedSdxCluster);
        assertEquals(SeLinux.ENFORCING, returnedSdxCluster.getSeLinux());
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
    void testGetSdxClusterByNameOrCrnWhenClusterCrnProvidedThrowsExceptionIfClusterDoesNotExists() {
        SdxCluster sdxCluser = new SdxCluster();
        sdxCluser.setEnvName("env");
        sdxCluser.setClusterName(CLUSTER_NAME);
        when(sdxClusterRepository.findByAccountIdAndCrnAndDeletedIsNull(eq("hortonworks"), eq(ENVIRONMENT_CRN))).thenReturn(Optional.empty());
        Assertions.assertThatCode(() -> underTest.getByNameOrCrn(USER_CRN, NameOrCrn.ofCrn(ENVIRONMENT_CRN)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("SDX cluster '" + ENVIRONMENT_CRN + "' not found.");
    }

    @Test
    void testGetSdxClusterByNameOrCrnWhenClusterNameProvidedThrowsExceptionIfClusterDoesNotExists() {
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.empty());
        Assertions.assertThatCode(() -> underTest.getByNameOrCrn(USER_CRN, NameOrCrn.ofName(CLUSTER_NAME)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("SDX cluster '" + CLUSTER_NAME + "' not found.");
    }

    @Test
    void testGetSdxClusterByAccountIdWhenNoDeployedClusterShouldThrowSdxNotFoundException() {
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.empty());
        NotFoundException notFoundException = assertThrows(NotFoundException.class, () -> underTest.getByNameInAccount(USER_CRN, "sdxcluster"));
        assertEquals("SDX cluster 'sdxcluster' not found.", notFoundException.getMessage());
    }

    @Test
    void testUpdateRangerRazEnabledForSdxClusterWhenRangerRazIsPresent() throws TransactionExecutionException {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setEnvName("env");
        sdxCluster.setEnvCrn(ENVIRONMENT_CRN);
        sdxCluster.setClusterName(CLUSTER_NAME);
        sdxCluster.setCrn("test-crn");
        sdxCluster.setRuntime("7.2.11");

        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCloudPlatform("AWS");

        RangerRazEnabledV4Response response = mock(RangerRazEnabledV4Response.class);
        when(stackV4Endpoint.rangerRazEnabledInternal(anyLong(), anyString(), anyString())).thenReturn(response);
        when(response.isRangerRazEnabled()).thenReturn(true);
        when(environmentClientService.getByCrn(anyString())).thenReturn(environmentResponse);
        when(sdxVersionRuleEnforcer.isRazSupported(any(), any())).thenReturn(true);
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
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.updateRangerRazEnabled(sdxCluster));

        verify(sdxClusterRepository, times(0)).save(sdxCluster);
    }

    @ParameterizedTest
    @MethodSource("storageBaseLocationsWhiteSpaceValidation")
    void testValidateBaseLocationWhenWhiteSpaceIsPresent(String input, ValidationResult.State expected) {
        ValidationResult result = underTest.validateBaseLocation(input);
        assertEquals(expected, result.getState());
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
    void testSyncSdxClusterWhenClusterNameSpecifiedShouldCallStackEndpointExactlyOnce() {
        SdxCluster sdxCluser = new SdxCluster();
        sdxCluser.setEnvName("env");
        sdxCluser.setClusterName(CLUSTER_NAME);
        when(sdxClusterRepository.findByAccountIdAndCrnAndDeletedIsNull(eq("hortonworks"), eq(DATALAKE_CRN)))
                .thenReturn(Optional.of(sdxCluser));
        underTest.syncByCrn(USER_CRN, DATALAKE_CRN);

        verify(stackV4Endpoint, times(1)).sync(eq(0L), eq(CLUSTER_NAME), anyString());
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

    private void mockEnvironmentCall(SdxClusterResizeRequest sdxClusterResizeRequest, CloudPlatform cloudPlatform) {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setName(sdxClusterResizeRequest.getEnvironment());
        detailedEnvironmentResponse.setCloudPlatform(cloudPlatform.name());
        detailedEnvironmentResponse.setEnvironmentStatus(EnvironmentStatus.AVAILABLE);
        detailedEnvironmentResponse.setCrn(getCrn());
        detailedEnvironmentResponse.setCreator(detailedEnvironmentResponse.getCrn());
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
        sdxCluster.setEnvCrn(ENVIRONMENT_CRN);
        sdxCluster.setEnvName(ENVIRONMENT_NAME);
        sdxCluster.setClusterName(CLUSTER_NAME);
        sdxCluster.setSdxDatabase(new SdxDatabase());
        return sdxCluster;
    }

    @Test
    void testSdxResizeToEDL() throws IOException {
        SdxClusterResizeRequest resizeRequest = new SdxClusterResizeRequest();
        resizeRequest.setEnvironment(ENVIRONMENT_NAME);
        resizeRequest.setClusterShape(ENTERPRISE);
        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setId(SDX_ID);
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);
        sdxCluster.setRuntime("7.2.17");
        sdxCluster.setCloudStorageBaseLocation("s3a://some/dir/");
        sdxCluster.setSeLinux(SeLinux.PERMISSIVE);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString()))
                .thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(sdxBackupRestoreService.isDatalakeInBackupProgress(anyString(), anyString())).thenReturn(false);
        when(sdxBackupRestoreService.isDatalakeInRestoreProgress(anyString(), anyString())).thenReturn(false);

        mockEnvironmentCall(resizeRequest, AWS);
        when(sdxReactorFlowManager.triggerSdxResize(anyLong(), any(SdxCluster.class), any(DatalakeDrSkipOptions.class), eq(false)))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));

        String mediumDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.10/aws/medium_duty_ha.json");
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(mediumDutyJson, StackV4Request.class));
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.STOPPED);
        StackImageV4Response image = new StackImageV4Response();
        image.setOs(OS_NAME);
        image.setCatalogName(CATALOG_NAME);
        stackV4Response.setImage(image);
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        stackV4Response.setCluster(clusterV4Response);
        stackV4Response.setNetwork(getNetworkForCurrentDatalake());
        when(stackV4Endpoint.get(anyLong(), anyString(), anySet(), anyString())).thenReturn(stackV4Response);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.resizeSdx(USER_CRN, CLUSTER_NAME, resizeRequest));
        ArgumentCaptor<SdxCluster> sdxClusterArgumentCaptor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxReactorFlowManager, times(1)).triggerSdxResize(eq(SDX_ID), sdxClusterArgumentCaptor.capture(),
                any(DatalakeDrSkipOptions.class), eq(false));

        SdxCluster createdSdxCluster = sdxClusterArgumentCaptor.getValue();
        assertEquals(sdxCluster.getClusterName(), createdSdxCluster.getClusterName());
        assertEquals("7.2.17", createdSdxCluster.getRuntime());
        assertEquals("s3a://some/dir/", createdSdxCluster.getCloudStorageBaseLocation());
        assertEquals(ENVIRONMENT_NAME, createdSdxCluster.getEnvName());

        String stackRequestRawString = createdSdxCluster.getStackRequest();
        ObjectMapper mapper = new ObjectMapper();
        StackV4Request stackV4Request = mapper.readValue(stackRequestRawString, StackV4Request.class);
        assertEquals(CLUSTER_NAME + ENTERPRISE.getResizeSuffix(), stackV4Request.getCustomDomain().getHostname());
    }

    @Test
    void testSdxResizeByAccountIdAndNameWhenSdxDoesNotExist() {
        SdxClusterResizeRequest sdxClusterResizeRequest = new SdxClusterResizeRequest();
        sdxClusterResizeRequest.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setEnvironment(ENVIRONMENT_NAME);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.empty());
        NotFoundException notFoundException = assertThrows(NotFoundException.class, () -> underTest.resizeSdx(USER_CRN, "sdxcluster", sdxClusterResizeRequest));
        assertEquals("SDX cluster 'sdxcluster' not found.", notFoundException.getMessage());
    }

    @Test
    void testSdxResizeByAccountIdAndNameWhenSdxWithSameShape() {
        SdxClusterResizeRequest sdxClusterResizeRequest = new SdxClusterResizeRequest();
        sdxClusterResizeRequest.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setEnvironment(ENVIRONMENT_NAME);

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setClusterShape(MEDIUM_DUTY_HA);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> underTest.resizeSdx(USER_CRN, "sdxcluster",
                sdxClusterResizeRequest));
        assertEquals("SDX cluster is already of requested shape and not resizing to multi AZ from single AZ",
                badRequestException.getMessage());
    }

    @Test
    void testSdxResizeGcpClusterSuccess() throws IOException {
        final String runtime = "7.2.15";
        SdxClusterResizeRequest sdxClusterResizeRequest = new SdxClusterResizeRequest();
        sdxClusterResizeRequest.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setEnvironment(ENVIRONMENT_NAME);

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setId(SDX_ID);
        sdxCluster.setCloudStorageFileSystemType(FileSystemType.GCS);
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);
        sdxCluster.setRuntime(runtime);
        sdxCluster.setSeLinux(SeLinux.PERMISSIVE);
        sdxCluster.setCloudStorageBaseLocation("gcs://some/dir/");

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(sdxBackupRestoreService.isDatalakeInBackupProgress(anyString(), anyString())).thenReturn(false);
        when(sdxBackupRestoreService.isDatalakeInRestoreProgress(anyString(), anyString())).thenReturn(false);
        mockEnvironmentCall(sdxClusterResizeRequest, GCP);
        when(sdxReactorFlowManager.triggerSdxResize(anyLong(), any(SdxCluster.class), any(DatalakeDrSkipOptions.class), eq(false)))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));
        String mediumDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.15/gcp/medium_duty_ha.json");
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(mediumDutyJson, StackV4Request.class));
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.STOPPED);
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        stackV4Response.setCluster(clusterV4Response);
        NetworkV4Response networkResponse = new NetworkV4Response();
        GcpNetworkV4Parameters gcpNetworkV4Parameters = new GcpNetworkV4Parameters();
        gcpNetworkV4Parameters.setSubnetId("subnet-123");
        gcpNetworkV4Parameters.setNetworkId("net-123");
        networkResponse.setGcp(gcpNetworkV4Parameters);
        stackV4Response.setNetwork(networkResponse);
        when(stackV4Endpoint.get(anyLong(), anyString(), anySet(), anyString())).thenReturn(stackV4Response);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.resizeSdx(USER_CRN, sdxCluster.getClusterName(), sdxClusterResizeRequest));
        ArgumentCaptor<SdxCluster> sdxClusterArgumentCaptor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxReactorFlowManager, times(1)).triggerSdxResize(eq(SDX_ID), sdxClusterArgumentCaptor.capture(),
                any(DatalakeDrSkipOptions.class), eq(false));

        SdxCluster createdSdxCluster = sdxClusterArgumentCaptor.getValue();
        assertEquals(sdxCluster.getClusterName(), createdSdxCluster.getClusterName());
        assertEquals(runtime, createdSdxCluster.getRuntime());
        assertEquals("gcs://some/dir/", createdSdxCluster.getCloudStorageBaseLocation());
        assertEquals(ENVIRONMENT_NAME, createdSdxCluster.getEnvName());
        String stackRequestRawString = createdSdxCluster.getStackRequest();
        ObjectMapper mapper = new ObjectMapper();
        StackV4Request stackV4Request = mapper.readValue(stackRequestRawString, StackV4Request.class);
        assertEquals(CLUSTER_NAME + MEDIUM_DUTY_HA.getResizeSuffix(), stackV4Request.getCustomDomain().getHostname());
        assertEquals(stackV4Request.getNetwork().getGcp().getSubnetId(), "subnet-123");
        assertEquals(stackV4Request.getNetwork().getGcp().getNetworkId(), "net-123");
    }

    @Test
    void testSdxResizeByAccountIdAndNameWhenSdxWithExistingDetachedSdx() {
        SdxClusterResizeRequest sdxClusterResizeRequest = new SdxClusterResizeRequest();
        sdxClusterResizeRequest.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setEnvironment(ENVIRONMENT_NAME);

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);
        sdxCluster.setDetached(true);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> underTest.resizeSdx(USER_CRN, "sdxcluster",
                sdxClusterResizeRequest));
        assertEquals("SDX which is detached already exists for the environment. SDX name: " + sdxCluster.getClusterName(), badRequestException.getMessage());
    }

    @Test
    void testSdxResizeByAccountIdAndNameWhenDatalakeIsInProcessOfBackup() {
        SdxClusterResizeRequest sdxClusterResizeRequest = new SdxClusterResizeRequest();
        sdxClusterResizeRequest.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setEnvironment("environment");

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);
        sdxCluster.setDetached(true);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(sdxBackupRestoreService.isDatalakeInBackupProgress(anyString(), anyString())).thenReturn(true);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.resizeSdx(USER_CRN, "sdxcluster", sdxClusterResizeRequest)),
                "BadRequestException should thrown");
        assertEquals("SDX cluster is in the process of backup. Resize can not get started.", badRequestException.getMessage());
    }

    @Test
    void testSdxResizeByAccountIdAndNameWhenDatalakeIsInProcessOfRestore() {
        SdxClusterResizeRequest sdxClusterResizeRequest = new SdxClusterResizeRequest();
        sdxClusterResizeRequest.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setEnvironment(ENVIRONMENT_NAME);

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);
        sdxCluster.setDetached(true);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(sdxBackupRestoreService.isDatalakeInBackupProgress(anyString(), anyString())).thenReturn(false);
        when(sdxBackupRestoreService.isDatalakeInRestoreProgress(anyString(), anyString())).thenReturn(true);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.resizeSdx(USER_CRN, "sdxcluster", sdxClusterResizeRequest)),
                "BadRequestException should thrown");
        assertEquals("SDX cluster is in the process of restore. Resize can not get started.", badRequestException.getMessage());
    }

    @Test
    void testSdxResizeByAccountIdAndNameWhenDatalakeIsNotInProgressOfBackupOrRestore() throws Exception {
        final String runtime = "7.2.10";
        SdxClusterResizeRequest sdxClusterResizeRequest = new SdxClusterResizeRequest();
        sdxClusterResizeRequest.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setEnvironment(ENVIRONMENT_NAME);

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setId(1L);
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);
        sdxCluster.setRuntime(runtime);
        sdxCluster.setCloudStorageBaseLocation("s3a://some/dir/");
        sdxCluster.setSeLinux(SeLinux.PERMISSIVE);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(sdxBackupRestoreService.isDatalakeInBackupProgress(anyString(), anyString())).thenReturn(false);
        when(sdxBackupRestoreService.isDatalakeInRestoreProgress(anyString(), anyString())).thenReturn(false);

        mockEnvironmentCall(sdxClusterResizeRequest, CloudPlatform.AWS);
        when(sdxReactorFlowManager.triggerSdxResize(anyLong(), any(SdxCluster.class), any(DatalakeDrSkipOptions.class), eq(false)))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));

        String mediumDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.10/aws/medium_duty_ha.json");
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(mediumDutyJson, StackV4Request.class));
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.STOPPED);
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        stackV4Response.setCluster(clusterV4Response);
        stackV4Response.setNetwork(getNetworkForCurrentDatalake());
        when(stackV4Endpoint.get(anyLong(), anyString(), anySet(), anyString())).thenReturn(stackV4Response);

        assertDoesNotThrow(
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.resizeSdx(USER_CRN, "sdxcluster", sdxClusterResizeRequest)));
    }

    @ParameterizedTest
    @MethodSource("deleteInProgressParamProvider")
    void testSdxResizeButEnvInDeleteInProgressPhase(EnvironmentStatus environmentStatus) {

        SdxClusterResizeRequest sdxClusterResizeRequest = new SdxClusterResizeRequest();
        sdxClusterResizeRequest.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setEnvironment(ENVIRONMENT_NAME);

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(sdxBackupRestoreService.isDatalakeInBackupProgress(anyString(), anyString())).thenReturn(false);
        when(sdxBackupRestoreService.isDatalakeInRestoreProgress(anyString(), anyString())).thenReturn(false);
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setName(sdxClusterResizeRequest.getEnvironment());
        detailedEnvironmentResponse.setCloudPlatform(AWS.name());
        detailedEnvironmentResponse.setCrn(getCrn());
        detailedEnvironmentResponse.setEnvironmentStatus(environmentStatus);
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.resizeSdx(USER_CRN, "sdxcluster", sdxClusterResizeRequest)),
                "BadRequestException should thrown");
        assertEquals("The environment is in delete in progress phase. Please create a new environment first!", badRequestException.getMessage());
    }

    @Test
    void testSdxResizeForHybridEnvironment() {
        SdxClusterResizeRequest sdxClusterResizeRequest = new SdxClusterResizeRequest();
        sdxClusterResizeRequest.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setEnvironment(ENVIRONMENT_NAME);

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(sdxBackupRestoreService.isDatalakeInBackupProgress(anyString(), anyString())).thenReturn(false);
        when(sdxBackupRestoreService.isDatalakeInRestoreProgress(anyString(), anyString())).thenReturn(false);
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setName(sdxClusterResizeRequest.getEnvironment());
        detailedEnvironmentResponse.setCloudPlatform(AWS.name());
        detailedEnvironmentResponse.setCrn(getCrn());
        detailedEnvironmentResponse.setEnvironmentStatus(EnvironmentStatus.AVAILABLE);
        detailedEnvironmentResponse.setEnvironmentType(EnvironmentType.HYBRID_BASE.toString());
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.resizeSdx(USER_CRN, "sdxcluster", sdxClusterResizeRequest)),
                "BadRequestException should thrown");
        assertEquals("Creating or Resizing datalake is not supported for Hybrid Environment", badRequestException.getMessage());
    }

    @ParameterizedTest
    @MethodSource("failedParamProvider")
    void testSdxResizeButEnvInFailedPhase(EnvironmentStatus environmentStatus) {
        SdxClusterResizeRequest sdxClusterResizeRequest = new SdxClusterResizeRequest();
        sdxClusterResizeRequest.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setEnvironment(ENVIRONMENT_NAME);

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(sdxBackupRestoreService.isDatalakeInBackupProgress(anyString(), anyString())).thenReturn(false);
        when(sdxBackupRestoreService.isDatalakeInRestoreProgress(anyString(), anyString())).thenReturn(false);
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setName(sdxClusterResizeRequest.getEnvironment());
        detailedEnvironmentResponse.setCloudPlatform(AWS.name());
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
        sdxClusterResizeRequest.setEnvironment(ENVIRONMENT_NAME);

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(sdxBackupRestoreService.isDatalakeInBackupProgress(anyString(), anyString())).thenReturn(false);
        when(sdxBackupRestoreService.isDatalakeInRestoreProgress(anyString(), anyString())).thenReturn(false);
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setName(sdxClusterResizeRequest.getEnvironment());
        detailedEnvironmentResponse.setCloudPlatform(AWS.name());
        detailedEnvironmentResponse.setCrn(getCrn());
        detailedEnvironmentResponse.setEnvironmentStatus(environmentStatus);
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.resizeSdx(USER_CRN, "sdxcluster", sdxClusterResizeRequest)),
                "BadRequestException should thrown");
        assertEquals(exceptionMessage, badRequestException.getMessage());
    }

    @Test
    void testSdxResizeToMultiAZThrowsExceptionIfSubnetsAreNotInMultipleAZ() {
        SdxClusterResizeRequest sdxClusterResizeRequest = new SdxClusterResizeRequest();
        sdxClusterResizeRequest.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setEnvironment(ENVIRONMENT_NAME);
        sdxClusterResizeRequest.setEnableMultiAz(true);

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(sdxBackupRestoreService.isDatalakeInBackupProgress(anyString(), anyString())).thenReturn(false);
        when(sdxBackupRestoreService.isDatalakeInRestoreProgress(anyString(), anyString())).thenReturn(false);
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setName(sdxClusterResizeRequest.getEnvironment());
        detailedEnvironmentResponse.setCloudPlatform(AWS.name());
        detailedEnvironmentResponse.setCrn(getCrn());
        detailedEnvironmentResponse.setEnvironmentStatus(EnvironmentStatus.AVAILABLE);
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        network.setSubnetMetas(Map.of(
                "subnet1", new CloudSubnet(new Builder().availabilityZone("az1")),
                "subnet2", new CloudSubnet(new Builder().availabilityZone("az1"))
        ));
        detailedEnvironmentResponse.setNetwork(network);
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.resizeSdx(USER_CRN, "sdxcluster", sdxClusterResizeRequest)));
        assertEquals("Multi AZ cluster requires subnets in multiple availability zones but the cluster uses subnest only from az1 availability zone.",
                badRequestException.getMessage());
    }

    @Test
    void testSdxResizeMediumDutySdxEnabled710Runtime() {
        final String invalidRuntime = "7.1.0";
        SdxClusterResizeRequest sdxClusterResizeRequest = new SdxClusterResizeRequest();
        sdxClusterResizeRequest.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setEnvironment(ENVIRONMENT_NAME);

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);
        sdxCluster.setRuntime(invalidRuntime);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(sdxBackupRestoreService.isDatalakeInBackupProgress(anyString(), anyString())).thenReturn(false);
        when(sdxBackupRestoreService.isDatalakeInRestoreProgress(anyString(), anyString())).thenReturn(false);
        mockEnvironmentCall(sdxClusterResizeRequest, AWS);
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
        sdxClusterResizeRequest.setEnvironment(ENVIRONMENT_NAME);

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);
        sdxCluster.setRuntime(invalidRuntime);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(sdxBackupRestoreService.isDatalakeInBackupProgress(anyString(), anyString())).thenReturn(false);
        when(sdxBackupRestoreService.isDatalakeInRestoreProgress(anyString(), anyString())).thenReturn(false);
        mockEnvironmentCall(sdxClusterResizeRequest, AWS);
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.resizeSdx(USER_CRN, "sdxcluster", sdxClusterResizeRequest)));
        assertEquals("Provisioning a Medium Duty SDX shape is only valid for CM version greater than or equal to "
                + MEDIUM_DUTY_REQUIRED_VERSION + " and not " + invalidRuntime, badRequestException.getMessage());
    }

    @Test
    void testSdxResizeClusterWithoutCloudStorageShouldThrownBadRequestException() {
        SdxClusterResizeRequest sdxClusterResizeRequest = new SdxClusterResizeRequest();
        sdxClusterResizeRequest.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setEnvironment(ENVIRONMENT_NAME);

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(sdxBackupRestoreService.isDatalakeInBackupProgress(anyString(), anyString())).thenReturn(false);
        when(sdxBackupRestoreService.isDatalakeInRestoreProgress(anyString(), anyString())).thenReturn(false);
        mockEnvironmentCall(sdxClusterResizeRequest, AWS);
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                        underTest.resizeSdx(USER_CRN, "sdxcluster", sdxClusterResizeRequest)));
        assertEquals("Cloud storage parameter is required.", badRequestException.getMessage());
    }

    @Test
    void testSdxResizeAwsClusterSuccess() throws Exception {
        final String runtime = "7.2.10";
        SdxClusterResizeRequest sdxClusterResizeRequest = new SdxClusterResizeRequest();
        sdxClusterResizeRequest.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setEnvironment(ENVIRONMENT_NAME);
        sdxClusterResizeRequest.setEnableMultiAz(true);

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setId(SDX_ID);
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);
        sdxCluster.setRuntime(runtime);
        sdxCluster.setCloudStorageBaseLocation("s3a://some/dir/");
        sdxCluster.setSeLinux(SeLinux.PERMISSIVE);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(sdxBackupRestoreService.isDatalakeInBackupProgress(anyString(), anyString())).thenReturn(false);
        when(sdxBackupRestoreService.isDatalakeInRestoreProgress(anyString(), anyString())).thenReturn(false);
        mockEnvironmentCall(sdxClusterResizeRequest, AWS);
        when(sdxReactorFlowManager.triggerSdxResize(anyLong(), any(SdxCluster.class), any(DatalakeDrSkipOptions.class), eq(false)))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));

        String mediumDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.10/aws/medium_duty_ha.json");
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(mediumDutyJson, StackV4Request.class));
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.STOPPED);
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        stackV4Response.setCluster(clusterV4Response);
        stackV4Response.setNetwork(getNetworkForCurrentDatalake());
        when(stackV4Endpoint.get(anyLong(), anyString(), anySet(), anyString())).thenReturn(stackV4Response);
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setName(sdxClusterResizeRequest.getEnvironment());
        detailedEnvironmentResponse.setCloudPlatform(AWS.name());
        detailedEnvironmentResponse.setCrn(getCrn());
        detailedEnvironmentResponse.setEnvironmentStatus(EnvironmentStatus.AVAILABLE);
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        network.setSubnetMetas(Map.of(
                "subnet1", new CloudSubnet(new Builder().availabilityZone("az1")),
                "subnet2", new CloudSubnet(new Builder().availabilityZone("az2"))
        ));
        detailedEnvironmentResponse.setNetwork(network);
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.resizeSdx(USER_CRN, CLUSTER_NAME, sdxClusterResizeRequest));
        ArgumentCaptor<SdxCluster> sdxClusterArgumentCaptor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxReactorFlowManager, times(1)).triggerSdxResize(eq(SDX_ID), sdxClusterArgumentCaptor.capture(),
                any(DatalakeDrSkipOptions.class), eq(false));

        SdxCluster createdSdxCluster = sdxClusterArgumentCaptor.getValue();
        assertEquals(sdxCluster.getClusterName(), createdSdxCluster.getClusterName());
        assertEquals(runtime, createdSdxCluster.getRuntime());
        assertEquals("s3a://some/dir/", createdSdxCluster.getCloudStorageBaseLocation());
        assertEquals(ENVIRONMENT_NAME, createdSdxCluster.getEnvName());

        String stackRequestRawString = createdSdxCluster.getStackRequest();
        ObjectMapper mapper = new ObjectMapper();
        StackV4Request stackV4Request = mapper.readValue(stackRequestRawString, StackV4Request.class);
        assertEquals(CLUSTER_NAME + MEDIUM_DUTY_HA.getResizeSuffix(), stackV4Request.getCustomDomain().getHostname());
        assertEquals(stackV4Request.getNetwork().getAws().getSubnetId(), "subnet-123");
    }

    @Test
    void testSdxResizeAwsSameShapeMultiAZClusterSuccess() throws Exception {
        final String runtime = "7.2.10";
        SdxClusterResizeRequest sdxClusterResizeRequest = new SdxClusterResizeRequest();
        sdxClusterResizeRequest.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setEnvironment(ENVIRONMENT_NAME);
        sdxClusterResizeRequest.setEnableMultiAz(true);

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setId(SDX_ID);
        sdxCluster.setClusterShape(MEDIUM_DUTY_HA);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);
        sdxCluster.setRuntime(runtime);
        sdxCluster.setCloudStorageBaseLocation("s3a://some/dir/");
        sdxCluster.setEnableMultiAz(false);
        sdxCluster.setSeLinux(SeLinux.PERMISSIVE);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(sdxBackupRestoreService.isDatalakeInBackupProgress(anyString(), anyString())).thenReturn(false);
        when(sdxBackupRestoreService.isDatalakeInRestoreProgress(anyString(), anyString())).thenReturn(false);
        mockEnvironmentCall(sdxClusterResizeRequest, AWS);
        when(sdxReactorFlowManager.triggerSdxResize(anyLong(), any(SdxCluster.class), any(DatalakeDrSkipOptions.class), eq(false)))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));

        String mediumDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.10/aws/medium_duty_ha.json");
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(mediumDutyJson, StackV4Request.class));
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.STOPPED);
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        stackV4Response.setCluster(clusterV4Response);
        stackV4Response.setNetwork(getNetworkForCurrentDatalake());
        when(stackV4Endpoint.get(anyLong(), anyString(), anySet(), anyString())).thenReturn(stackV4Response);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.resizeSdx(USER_CRN, CLUSTER_NAME, sdxClusterResizeRequest));
        ArgumentCaptor<SdxCluster> sdxClusterArgumentCaptor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxReactorFlowManager, times(1)).triggerSdxResize(eq(SDX_ID), sdxClusterArgumentCaptor.capture(),
                any(DatalakeDrSkipOptions.class), eq(false));

        SdxCluster createdSdxCluster = sdxClusterArgumentCaptor.getValue();
        assertEquals(sdxCluster.getClusterName(), createdSdxCluster.getClusterName());
        assertEquals(runtime, createdSdxCluster.getRuntime());
        assertEquals("s3a://some/dir/", createdSdxCluster.getCloudStorageBaseLocation());
        assertEquals(ENVIRONMENT_NAME, createdSdxCluster.getEnvName());

        String stackRequestRawString = createdSdxCluster.getStackRequest();
        ObjectMapper mapper = new ObjectMapper();
        StackV4Request stackV4Request = mapper.readValue(stackRequestRawString, StackV4Request.class);
        assertEquals(CLUSTER_NAME + MEDIUM_DUTY_HA.getResizeSuffix() + "-az", stackV4Request.getCustomDomain().getHostname());
        assertEquals(stackV4Request.getNetwork().getAws().getSubnetId(), "subnet-123");
    }

    @Test
    void testSdxResizeAwsSameShapeSingleAZThrowsBadRequest() throws Exception {
        final String runtime = "7.2.10";
        SdxClusterResizeRequest sdxClusterResizeRequest = new SdxClusterResizeRequest();
        sdxClusterResizeRequest.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setEnvironment(ENVIRONMENT_NAME);
        sdxClusterResizeRequest.setEnableMultiAz(false);

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setId(SDX_ID);
        sdxCluster.setClusterShape(MEDIUM_DUTY_HA);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);
        sdxCluster.setRuntime(runtime);
        sdxCluster.setCloudStorageBaseLocation("s3a://some/dir/");
        sdxCluster.setEnableMultiAz(true);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));

        String mediumDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.10/aws/medium_duty_ha.json");
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.STOPPED);
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        stackV4Response.setCluster(clusterV4Response);
        stackV4Response.setNetwork(getNetworkForCurrentDatalake());

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                        underTest.resizeSdx(USER_CRN, "sdxcluster", sdxClusterResizeRequest)));
        assertEquals("SDX cluster is already of requested shape and not resizing to multi AZ from single AZ",
                badRequestException.getMessage());
    }

    @Test
    void testSdxResizeAwsSameShapeSameAZThrowsBadRequest() throws Exception {
        final String runtime = "7.2.10";
        SdxClusterResizeRequest sdxClusterResizeRequest = new SdxClusterResizeRequest();
        sdxClusterResizeRequest.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setEnvironment(ENVIRONMENT_NAME);
        sdxClusterResizeRequest.setEnableMultiAz(true);

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setId(SDX_ID);
        sdxCluster.setClusterShape(MEDIUM_DUTY_HA);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);
        sdxCluster.setRuntime(runtime);
        sdxCluster.setCloudStorageBaseLocation("s3a://some/dir/");
        sdxCluster.setEnableMultiAz(true);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));

        String mediumDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.10/aws/medium_duty_ha.json");
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.STOPPED);
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        stackV4Response.setCluster(clusterV4Response);
        stackV4Response.setNetwork(getNetworkForCurrentDatalake());

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                        underTest.resizeSdx(USER_CRN, "sdxcluster", sdxClusterResizeRequest)));
        assertEquals("SDX cluster is already of requested shape and not resizing to multi AZ from single AZ",
                badRequestException.getMessage());
    }

    @Test
    void testSdxResizeClusterWithNoEntitlement() {
        final String runtime = "7.2.10";
        SdxClusterResizeRequest sdxClusterResizeRequest = new SdxClusterResizeRequest();
        sdxClusterResizeRequest.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setEnvironment(ENVIRONMENT_NAME);

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setId(1L);
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);
        sdxCluster.setRuntime(runtime);
        sdxCluster.setCloudStorageBaseLocation("s3a://some/dir/");

        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.resizeSdx(USER_CRN, "sdxcluster", sdxClusterResizeRequest)));
        assertEquals("Resizing of the data lake is not supported", badRequestException.getMessage());
    }

    @Test
    void testSdxResizeDatabaseProperlySetUp() throws IOException {
        SdxClusterResizeRequest resizeRequest = new SdxClusterResizeRequest();
        resizeRequest.setEnvironment(ENVIRONMENT_NAME);
        resizeRequest.setClusterShape(ENTERPRISE);
        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setId(SDX_ID);
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.getSdxDatabase().setDatabaseCrn(DATABASE_CRN);
        sdxCluster.getSdxDatabase().setDatabaseAvailabilityType(SdxDatabaseAvailabilityType.NON_HA);
        sdxCluster.setRuntime("7.2.17");
        sdxCluster.setCloudStorageBaseLocation("s3a://some/dir/");
        sdxCluster.setSeLinux(SeLinux.PERMISSIVE);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString()))
                .thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(sdxBackupRestoreService.isDatalakeInBackupProgress(anyString(), anyString())).thenReturn(false);
        when(sdxBackupRestoreService.isDatalakeInRestoreProgress(anyString(), anyString())).thenReturn(false);
        mockEnvironmentCall(resizeRequest, AWS);
        when(sdxReactorFlowManager.triggerSdxResize(anyLong(), any(SdxCluster.class), any(DatalakeDrSkipOptions.class), eq(false)))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));
        String mediumDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.10/aws/medium_duty_ha.json");
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(mediumDutyJson, StackV4Request.class));
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.STOPPED);
        StackImageV4Response image = new StackImageV4Response();
        image.setOs(OS_NAME);
        image.setCatalogName(CATALOG_NAME);
        stackV4Response.setImage(image);
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        clusterV4Response.setDbSSLEnabled(false);
        stackV4Response.setCluster(clusterV4Response);
        stackV4Response.setNetwork(getNetworkForCurrentDatalake());
        when(stackV4Endpoint.get(anyLong(), anyString(), anySet(), anyString())).thenReturn(stackV4Response);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.resizeSdx(USER_CRN, CLUSTER_NAME, resizeRequest));
        ArgumentCaptor<SdxCluster> sdxClusterArgumentCaptor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxReactorFlowManager, times(1)).triggerSdxResize(eq(SDX_ID), sdxClusterArgumentCaptor.capture(),
                any(DatalakeDrSkipOptions.class), eq(false));

        SdxCluster createdSdxCluster = sdxClusterArgumentCaptor.getValue();
        Map<String, Object> databaseAttributes = createdSdxCluster.getSdxDatabase().getAttributes().getMap();
        assertEquals(SdxDatabaseAvailabilityType.NON_HA, createdSdxCluster.getSdxDatabase().getDatabaseAvailabilityType());
        assertEquals(DATABASE_CRN, databaseAttributes.get(PREVIOUS_DATABASE_CRN));
        assertEquals(LIGHT_DUTY.toString(), databaseAttributes.get(PREVIOUS_CLUSTER_SHAPE));
        assertEquals(false, databaseAttributes.get(DATABASE_SSL_ENABLED));

        String stackRequestRawString = createdSdxCluster.getStackRequest();
        ObjectMapper mapper = new ObjectMapper();
        StackV4Request stackV4Request = mapper.readValue(stackRequestRawString, StackV4Request.class);
        assertEquals(CLUSTER_NAME + ENTERPRISE.getResizeSuffix(), stackV4Request.getCustomDomain().getHostname());
    }

    @Test
    void testAttachRecipe() {
        SdxCluster sdxCluster = getSdxCluster();
        when(stackV4Endpoint.attachRecipeInternal(anyLong(), any(), anyString(), anyString())).thenReturn(null);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.attachRecipe(sdxCluster, null));
        verify(stackV4Endpoint).attachRecipeInternal(anyLong(), any(), eq(sdxCluster.getClusterName()), eq(USER_CRN));
    }

    @Test
    void testDetachRecipe() {
        SdxCluster sdxCluster = getSdxCluster();
        when(stackV4Endpoint.detachRecipeInternal(anyLong(), any(), anyString(), anyString())).thenReturn(null);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.detachRecipe(sdxCluster, null));
        verify(stackV4Endpoint).detachRecipeInternal(anyLong(), any(), eq(sdxCluster.getClusterName()), eq(USER_CRN));
    }

    @Test
    void testRefreshRecipe() {
        SdxCluster sdxCluster = getSdxCluster();
        when(stackV4Endpoint.refreshRecipesInternal(anyLong(), any(), anyString(), anyString())).thenReturn(null);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.refreshRecipes(sdxCluster, null));
        verify(stackV4Endpoint).refreshRecipesInternal(anyLong(), any(), eq(sdxCluster.getClusterName()), eq(USER_CRN));
    }

    @Test
    void rotateSaltPassword() {
        SdxCluster sdxCluster = getSdxCluster();
        FlowIdentifier sdxFlowIdentifier = mock(FlowIdentifier.class);
        when(sdxReactorFlowManager.triggerSaltPasswordRotationTracker(sdxCluster)).thenReturn(sdxFlowIdentifier);

        FlowIdentifier result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.rotateSaltPassword(sdxCluster));

        assertEquals(sdxFlowIdentifier, result);
        verify(sdxReactorFlowManager).triggerSaltPasswordRotationTracker(sdxCluster);
    }

    @Test
    void modifyProxyConfig() {
        SdxCluster sdxCluster = getSdxCluster();
        String previousProxyConfigCrn = "previous-proxy-crn";
        FlowIdentifier cbFlowIdentifier = mock(FlowIdentifier.class);
        when(stackV4Endpoint.modifyProxyConfigInternal(WORKSPACE_ID_DEFAULT, SDX_CRN, previousProxyConfigCrn, USER_CRN)).thenReturn(cbFlowIdentifier);
        FlowIdentifier sdxFlowIdentifier = mock(FlowIdentifier.class);
        when(sdxReactorFlowManager.triggerModifyProxyConfigTracker(sdxCluster)).thenReturn(sdxFlowIdentifier);

        FlowIdentifier result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.modifyProxyConfig(sdxCluster, previousProxyConfigCrn));

        assertEquals(sdxFlowIdentifier, result);
        verify(stackV4Endpoint).modifyProxyConfigInternal(WORKSPACE_ID_DEFAULT, SDX_CRN, previousProxyConfigCrn, USER_CRN);
        verify(cloudbreakFlowService).saveLastCloudbreakFlowChainId(sdxCluster, cbFlowIdentifier);
        verify(sdxReactorFlowManager).triggerModifyProxyConfigTracker(sdxCluster);
    }

    @Test
    void refreshDatahubsWithoutName() {
        SdxCluster sdxCluster = getSdxCluster();
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(anyString(), anyString()))
                .thenReturn(Optional.of(sdxCluster));
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.refreshDataHub(CLUSTER_NAME, null));
        verify(distroxService, times(1)).restartAttachedDistroxClustersServices(sdxCluster.getEnvCrn());
        verify(distroxService, times(1)).getAttachedDistroXClusters(sdxCluster.getEnvCrn());
    }

    @Test
    void refreshDatahubsWithName() {
        SdxCluster sdxCluster = getSdxCluster();
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(anyString(), anyString()))
                .thenReturn(Optional.of(sdxCluster));
        StackV4Response stackV4Response = mock(StackV4Response.class);
        when(stackV4Response.getCrn()).thenReturn(getCrn());
        when(distroXV1Endpoint.getByName(anyString(), eq(null))).thenReturn(stackV4Response);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.refreshDataHub(CLUSTER_NAME, "datahubName"));
        verify(distroXV1Endpoint, times(1)).getByName(anyString(), eq(null));
        verify(distroxService, times(1)).restartDistroxServicesByCrns(any());
    }

    @Test
    public void testUpdateDbEngineVersionUpdatesField() {
        when(sdxClusterRepository.findDatabaseIdByCrn(anyString())).thenReturn(Optional.of(1L));

        underTest.updateDatabaseEngineVersion(SDX_CRN, "10");
        verify(sdxDatabaseRepository, times(1)).updateDatabaseEngineVersion(1L, "10");
    }

    @Test
    public void testUpdateDbEngineVersionUpdatesFieldNoDB() {
        when(sdxClusterRepository.findDatabaseIdByCrn(anyString())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> underTest.updateDatabaseEngineVersion(SDX_CRN, "10"));
        verify(sdxDatabaseRepository, times(0)).updateDatabaseEngineVersion(anyLong(), anyString());
    }

    @Test
    public void testUpdateSalt() {
        SdxCluster sdxCluster = getSdxCluster();
        SdxStatusEntity sdxStatus = new SdxStatusEntity();
        sdxStatus.setStatus(DatalakeStatusEnum.RUNNING);
        when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(sdxStatus);
        when(sdxReactorFlowManager.triggerSaltUpdate(sdxCluster)).thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));

        FlowIdentifier flowIdentifier = underTest.updateSalt(sdxCluster);

        verify(sdxReactorFlowManager, times(1)).triggerSaltUpdate(sdxCluster);
        assertEquals(FlowType.FLOW, flowIdentifier.getType());
        assertEquals("FLOW_ID", flowIdentifier.getPollableId());
    }

    @ParameterizedTest
    @EnumSource(value = DatalakeStatusEnum.class, names = {"STOPPED", "STOP_IN_PROGRESS", "EXTERNAL_DATABASE_DELETION_IN_PROGRESS", "STACK_DELETED",
            "STACK_DELETION_IN_PROGRESS", "DELETE_REQUESTED", "DELETED", "DELETE_FAILED"}, mode = EnumSource.Mode.INCLUDE)
    public void testUpdateSaltThrowsBadRequestWhenDatalakeNotAvailable(DatalakeStatusEnum status) {
        SdxCluster sdxCluster = getSdxCluster();
        SdxStatusEntity sdxStatus = new SdxStatusEntity();
        sdxStatus.setStatus(status);
        when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(sdxStatus);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> underTest.updateSalt(sdxCluster));

        verifyNoInteractions(sdxReactorFlowManager);
        assertEquals(String.format("SaltStack update cannot be initiated as datalake 'test-sdx-cluster' is currently in '%s' state.", status), ex.getMessage());
    }

    @Test
    public void testAddRmsToSdxCluster() throws IOException, TransactionExecutionException {
        DetailedEnvironmentResponse environmentResponse = getDetailedEnvironmentResponse();
        when(entitlementService.isRmsEnabledOnDatalake(any())).thenReturn(true);
        when(virtualMachineConfiguration.isJavaVersionSupported(11)).thenReturn(true);
        when(environmentClientService.getByName(anyString())).thenReturn(environmentResponse);
        when(platformStringTransformer.getPlatformStringForImageCatalog(anyString(), anyBoolean())).thenReturn(imageCatalogPlatform);
        BaseStackDetailsV4Response baseStackDetailsV4Response = new BaseStackDetailsV4Response();
        baseStackDetailsV4Response.setVersion("7.2.18");
        ImageV4Response imageV4Response = new ImageV4Response();
        imageV4Response.setStackDetails(baseStackDetailsV4Response);
        when(imageCatalogService.getImageResponseFromImageRequest(any(), any())).thenReturn(imageV4Response);
        String enterpriseJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.18/aws/enterprise.json");
        StackV4Request stackV4Request = JsonUtil.readValue(enterpriseJson, StackV4Request.class);
        when(cdpConfigService.getConfigForKey(any())).thenReturn(stackV4Request);
        SdxClusterRequest sdxClusterRequest = getSdxClusterRequest();
        when(sdxVersionRuleEnforcer.isRazSupported(any(), any())).thenReturn(true);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, "name", sdxClusterRequest, null));

        verify(virtualMachineConfiguration, times(1)).isJavaVersionSupported(11);
        verify(imageCatalogService, times(1)).getImageResponseFromImageRequest(any(), any());
        verify(environmentClientService, times(2)).getByName(anyString());
        verify(cdpConfigService, times(1)).getConfigForKey(any());
        verify(externalDatabaseConfigurer, times(1)).configure(any(), anyString(), any(), any(), any());
        verify(sdxReactorFlowManager, times(1)).triggerSdxCreation(any());
        verify(transactionService, times(1)).required(any(Supplier.class));
        verify(ownerAssignmentService, times(1)).assignResourceOwnerRoleIfEntitled(anyString(), any());
    }

    @Test
    void testSdxResizeCustomInstances() throws IOException {
        SdxClusterResizeRequest resizeRequest = new SdxClusterResizeRequest();
        resizeRequest.setEnvironment(ENVIRONMENT_NAME);
        resizeRequest.setClusterShape(ENTERPRISE);
        SdxInstanceGroupRequest sdxInstanceGroupRequest = new SdxInstanceGroupRequest();
        sdxInstanceGroupRequest.setName("idbroker");
        sdxInstanceGroupRequest.setInstanceType("m5.xlarge");
        resizeRequest.setCustomInstanceGroups(List.of(sdxInstanceGroupRequest));
        SdxInstanceGroupDiskRequest sdxInstanceGroupDiskRequest = new SdxInstanceGroupDiskRequest();
        sdxInstanceGroupDiskRequest.setName("master");
        sdxInstanceGroupDiskRequest.setInstanceDiskSize(256);
        resizeRequest.setCustomInstanceGroupDiskSize(List.of(sdxInstanceGroupDiskRequest));

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setId(1L);
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);
        sdxCluster.setRuntime("7.2.17");
        sdxCluster.setCloudStorageBaseLocation("s3a://some/dir/");
        sdxCluster.setSeLinux(SeLinux.PERMISSIVE);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString()))
                .thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(sdxBackupRestoreService.isDatalakeInBackupProgress(anyString(), anyString())).thenReturn(false);
        when(sdxBackupRestoreService.isDatalakeInRestoreProgress(anyString(), anyString())).thenReturn(false);

        mockEnvironmentCall(resizeRequest, AWS);
        ArgumentCaptor<SdxCluster> captorResize = ArgumentCaptor.forClass(SdxCluster.class);
        when(sdxReactorFlowManager.triggerSdxResize(anyLong(), captorResize.capture(), any(DatalakeDrSkipOptions.class), eq(false)))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));

        String mediumDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.10/aws/medium_duty_ha.json");
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(mediumDutyJson, StackV4Request.class));
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.STOPPED);
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        stackV4Response.setCluster(clusterV4Response);
        stackV4Response.setNetwork(getNetworkForCurrentDatalake());
        when(stackV4Endpoint.get(anyLong(), anyString(), anySet(), anyString())).thenReturn(stackV4Response);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.resizeSdx(USER_CRN, sdxCluster.getClusterName(), resizeRequest));

        StackV4Request stackV4Request = JsonUtil.readValue(captorResize.getValue().getStackRequest(), StackV4Request.class);
        InstanceGroupV4Request idbrokerInstGroup = stackV4Request.getInstanceGroups().stream().filter(ig -> "idbroker".equals(ig.getName())).findAny().get();
        InstanceGroupV4Request masterInstGroup = stackV4Request.getInstanceGroups().stream().filter(ig -> "master".equals(ig.getName())).findAny().get();
        assertEquals("m5.xlarge", idbrokerInstGroup.getTemplate().getInstanceType());
        assertEquals(256, masterInstGroup.getTemplate().getAttachedVolumes().stream().findAny().get().getSize());
    }

    @Test
    void testSyncComponentVersionsFromCmThenTriggerSync() {
        NameOrCrn crn = NameOrCrn.ofCrn(SDX_CRN);
        SdxCluster sdxCluster = new SdxCluster();
        SdxStatusEntity sdxStatus = new SdxStatusEntity();
        sdxStatus.setStatus(DatalakeStatusEnum.RUNNING);
        Optional<SdxCluster> sdxClusterOptional = Optional.of(sdxCluster);
        when(sdxClusterRepository.findByAccountIdAndCrnAndDeletedIsNull(any(), any())).thenReturn(sdxClusterOptional);
        when(sdxStatusService.getActualStatusForSdx(any(SdxCluster.class))).thenReturn(sdxStatus);
        underTest.syncComponentVersionsFromCm(USER_CRN, crn);
        verify(sdxReactorFlowManager, times(1)).triggerDatalakeSyncComponentVersionsFromCmFlow(sdxCluster);
    }

    @Test
    void testSyncComponentVersionsFromCmWhenInStoppedState() {
        NameOrCrn crn = NameOrCrn.ofCrn(SDX_CRN);
        SdxCluster sdxCluster = new SdxCluster();
        SdxStatusEntity sdxStatus = new SdxStatusEntity();
        sdxStatus.setStatus(DatalakeStatusEnum.STOPPED);
        Optional<SdxCluster> sdxClusterOptional = Optional.of(sdxCluster);
        when(sdxClusterRepository.findByAccountIdAndCrnAndDeletedIsNull(any(), any())).thenReturn(sdxClusterOptional);
        when(sdxStatusService.getActualStatusForSdx(any(SdxCluster.class))).thenReturn(sdxStatus);
        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.syncComponentVersionsFromCm(USER_CRN, crn));
        assertEquals("Reading CM and parcel versions from CM cannot be initiated as the datalake is in STOPPED state", exception.getMessage());
    }

    @Test
    void testSdxResizeCustomDatabaseProperties() throws IOException {
        SdxClusterResizeRequest resizeRequest = new SdxClusterResizeRequest();
        resizeRequest.setEnvironment(ENVIRONMENT_NAME);
        resizeRequest.setClusterShape(ENTERPRISE);
        SdxDatabaseComputeStorageRequest sdxDatabaseComputeStorageRequest = new SdxDatabaseComputeStorageRequest();
        sdxDatabaseComputeStorageRequest.setInstanceType("customInstance");
        sdxDatabaseComputeStorageRequest.setStorageSize(128L);
        resizeRequest.setCustomSdxDatabaseComputeStorage(sdxDatabaseComputeStorageRequest);
        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setId(1L);
        sdxCluster.setSeLinux(SeLinux.PERMISSIVE);
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);
        sdxCluster.setRuntime("7.2.17");
        sdxCluster.setCloudStorageBaseLocation("s3a://some/dir/");

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString()))
                .thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(sdxBackupRestoreService.isDatalakeInBackupProgress(anyString(), anyString())).thenReturn(false);
        when(sdxBackupRestoreService.isDatalakeInRestoreProgress(anyString(), anyString())).thenReturn(false);

        mockEnvironmentCall(resizeRequest, AWS);
        ArgumentCaptor<SdxCluster> captorResize = ArgumentCaptor.forClass(SdxCluster.class);
        when(sdxReactorFlowManager.triggerSdxResize(anyLong(), captorResize.capture(), any(DatalakeDrSkipOptions.class), eq(false)))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));

        String mediumDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.10/aws/medium_duty_ha.json");
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(mediumDutyJson, StackV4Request.class));
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.STOPPED);
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        stackV4Response.setCluster(clusterV4Response);
        stackV4Response.setNetwork(getNetworkForCurrentDatalake());
        when(stackV4Endpoint.get(anyLong(), anyString(), anySet(), anyString())).thenReturn(stackV4Response);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.resizeSdx(USER_CRN, sdxCluster.getClusterName(), resizeRequest));

        SdxDatabase sdxDatabase = captorResize.getValue().getSdxDatabase();
        Map<String, Object> attributes = sdxDatabase.getAttributes().getMap();
        assertThat(attributes).containsEntry("instancetype", "customInstance");
        assertThat(attributes).containsEntry("storage", "128");
    }

    @Test
    void testShapeValidationOfSDXCreation() throws IOException {
        DetailedEnvironmentResponse detailedEnvironmentResponse = getDetailedEnvironmentResponse();
        when(environmentClientService.getByName(eq("env-name"))).thenReturn(detailedEnvironmentResponse);
        when(platformStringTransformer.getPlatformStringForImageCatalog(anyString(), anyBoolean())).thenReturn(imageCatalogPlatform);
        ImageV4Response imageV4Response = new ImageV4Response();
        when(imageCatalogService.getImageResponseFromImageRequest(any(), any())).thenReturn(imageV4Response);
        String enterpriseJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.18/aws/enterprise.json");
        StackV4Request stackV4Request = JsonUtil.readValue(enterpriseJson, StackV4Request.class);
        when(cdpConfigService.getConfigForKey(any())).thenReturn(stackV4Request);

        // By runtime in the request and not in the image
        SdxClusterRequest sdxClusterRequest = getSdxClusterRequest();
        sdxClusterRequest.setJavaVersion(21);
        sdxClusterRequest.setEnvironment("env-name");
        sdxClusterRequest.setEnableRangerRms(false);

        // MEDIUM_DUTY and 7.2.17 => non throws
        sdxClusterRequest.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterRequest.setRuntime("7.2.17");
        when(sdxVersionRuleEnforcer.isRazSupported(any(), any())).thenReturn(true);
        when(virtualMachineConfiguration.isJavaVersionSupported(21)).thenReturn(true);
        assertDoesNotThrow(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, "dl-name", sdxClusterRequest, null)));

        // MEDIUM_DUTY and 7.2.18 => throws
        sdxClusterRequest.setRuntime("7.2.19");
        assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, "dl-name", sdxClusterRequest, null)));

        // MEDIUM_DUTY and 7.3.0 => throws
        sdxClusterRequest.setRuntime("7.3.0");
        assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, "dl-name", sdxClusterRequest, null)));

        // MEDIUM_DUTY and 7.2.16 => non throws
        sdxClusterRequest.setRuntime("7.2.16");
        sdxClusterRequest.setClusterShape(MEDIUM_DUTY_HA);
        assertDoesNotThrow(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, "dl-name", sdxClusterRequest, null)));

        // LIGHT_DUTY and 7.2.18 => non throws
        sdxClusterRequest.setClusterShape(LIGHT_DUTY);
        assertDoesNotThrow(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, "dl-name", sdxClusterRequest, null)));

        // LIGHT_DUTY and 7.2.17 => non throws
        sdxClusterRequest.setRuntime("7.2.17");
        assertDoesNotThrow(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, "dl-name", sdxClusterRequest, null)));

        // ENTERPRISE and 7.2.18 => non throws
        sdxClusterRequest.setRuntime("7.2.18");
        sdxClusterRequest.setClusterShape(ENTERPRISE);
        assertDoesNotThrow(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, "dl-name", sdxClusterRequest, null)));

        // ENTERPRISE and 7.2.17 => non throws
        sdxClusterRequest.setRuntime("7.2.17");
        assertDoesNotThrow(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, "dl-name", sdxClusterRequest, null)));

        // ENTERPRISE and 7.2.16 => throws
        sdxClusterRequest.setRuntime("7.2.16");
        assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, "dl-name", sdxClusterRequest, null)));

        // By runtime in the image and not in the request
        sdxClusterRequest.setRuntime(null);

        // MEDIUM_DUTY and 7.2.17 => non throws
        imageV4Response.setVersion("7.2.17");
        sdxClusterRequest.setClusterShape(MEDIUM_DUTY_HA);
        assertDoesNotThrow(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, "dl-name", sdxClusterRequest, null)));

        // MEDIUM_DUTY and 7.2.18 => throws
        imageV4Response.setVersion("7.2.18");
        assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, "dl-name", sdxClusterRequest, null)));

        // MEDIUM_DUTY and 7.2.16 => non throws
        imageV4Response.setVersion("7.2.16");
        sdxClusterRequest.setClusterShape(MEDIUM_DUTY_HA);
        assertDoesNotThrow(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.createSdx(USER_CRN, "dl-name", sdxClusterRequest, null)));

        // LIGHT_DUTY and 7.2.18 => non throws
        imageV4Response.setVersion("7.2.18");
        sdxClusterRequest.setClusterShape(LIGHT_DUTY);
        assertDoesNotThrow(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.createSdx(USER_CRN, "dl-name", sdxClusterRequest, null)));

        // LIGHT_DUTY and 7.2.17 => non throws
        imageV4Response.setVersion("7.2.17");
        assertDoesNotThrow(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, "dl-name", sdxClusterRequest, null)));

        // ENTERPRISE and 7.2.18 => non throws
        imageV4Response.setVersion("7.2.18");
        sdxClusterRequest.setClusterShape(ENTERPRISE);
        assertDoesNotThrow(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, "dl-name", sdxClusterRequest, null)));

        // ENTERPRISE and 7.2.17 => non throws
        imageV4Response.setVersion("7.2.17");
        assertDoesNotThrow(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, "dl-name", sdxClusterRequest, null)));

        // ENTERPRISE and 7.2.16 => throws
        imageV4Response.setVersion("7.2.16");
        assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, "dl-name", sdxClusterRequest, null)));
    }

    @Test
    void testShapeValidationOfSDXResize() throws IOException {
        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setClusterName("dl-name");
        sdxCluster.setEnvName("env-name");
        sdxCluster.setSeLinux(SeLinux.PERMISSIVE);
        sdxCluster.setCloudStorageBaseLocation("s3a://bucket/path");
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(eq("hortonworks"), eq("dl-name")))
                .thenReturn(Optional.of(sdxCluster));
        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(eq("hortonworks"))).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(eq("hortonworks"), eq(ENVIRONMENT_CRN)))
                .thenReturn(Optional.empty());
        when(sdxBackupRestoreService.isDatalakeInBackupProgress(eq("dl-name"), eq(USER_CRN))).thenReturn(false);
        when(sdxBackupRestoreService.isDatalakeInRestoreProgress(eq("dl-name"), eq(USER_CRN))).thenReturn(false);
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.STOPPED);
        StackImageV4Response image = new StackImageV4Response();
        image.setOs(OS_NAME);
        image.setCatalogName(CATALOG_NAME);
        stackV4Response.setImage(image);
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        stackV4Response.setCluster(clusterV4Response);
        stackV4Response.setNetwork(getNetworkForCurrentDatalake());
        when(stackV4Endpoint.get(eq(0L), eq("dl-name"), any(), eq("hortonworks"))).thenReturn(stackV4Response);
        DetailedEnvironmentResponse detailedEnvironmentResponse = getDetailedEnvironmentResponse();
        when(environmentClientService.getByName(eq("env-name"))).thenReturn(detailedEnvironmentResponse);
        String enterpriseJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.18/aws/enterprise.json");
        StackV4Request stackV4Request = JsonUtil.readValue(enterpriseJson, StackV4Request.class);
        when(cdpConfigService.getConfigForKey(any())).thenReturn(stackV4Request);

        // By runtime in the request and not in the image
        SdxClusterResizeRequest sdxClusterResizeRequest = new SdxClusterResizeRequest();
        sdxClusterResizeRequest.setEnvironment("env-name");
        sdxClusterResizeRequest.setClusterShape(MEDIUM_DUTY_HA);

        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.setRuntime("7.2.16");
        // LIGHT_DUTY to MEDIUM_DUTY and 7.2.16 => non throws
        assertDoesNotThrow(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.resizeSdx(USER_CRN, "dl-name", sdxClusterResizeRequest)));

        // LIGHT_DUTY to MEDIUM_DUTY and 7.2.17 => non throws
        sdxCluster.setRuntime("7.2.16");
        assertDoesNotThrow(
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.resizeSdx(USER_CRN, "dl-name", sdxClusterResizeRequest)));

        // LIGHT_DUTY to MEDIUM_DUTY and 7.2.18 => throws
        sdxCluster.setRuntime("7.2.18");
        assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.resizeSdx(USER_CRN, "dl-name", sdxClusterResizeRequest)));

        // LIGHT_DUTY to MEDIUM_DUTY and 7.2.19 => throws
        sdxCluster.setRuntime("7.2.19");
        assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.resizeSdx(USER_CRN, "dl-name", sdxClusterResizeRequest)));

        // LIGHT_DUTY to MEDIUM_DUTY and 7.3.0 => throws
        sdxCluster.setRuntime("7.3.0");
        assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.resizeSdx(USER_CRN, "dl-name", sdxClusterResizeRequest)));

        // MEDIUM_DUTY to ENTERPRISE and 7.2.17 => non throws
        sdxCluster.setRuntime("7.2.17");
        sdxCluster.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setClusterShape(ENTERPRISE);
        assertDoesNotThrow(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.resizeSdx(USER_CRN, "dl-name", sdxClusterResizeRequest)));

        // MEDIUM_DUTY to ENTERPRISE and 7.3.0 => non throws
        sdxCluster.setRuntime("7.3.0");
        sdxCluster.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setClusterShape(ENTERPRISE);
        assertDoesNotThrow(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.resizeSdx(USER_CRN, "dl-name", sdxClusterResizeRequest)));

        // MEDIUM_DUTY to ENTERPRISE and 7.2.16 => throws
        sdxCluster.setRuntime("7.2.16");
        sdxCluster.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setClusterShape(ENTERPRISE);
        assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.resizeSdx(USER_CRN, "dl-name", sdxClusterResizeRequest)));
    }

    @Test
    void testSdxResizeWithPreviousDatalakeModified() throws IOException {
        SdxClusterResizeRequest resizeRequest = new SdxClusterResizeRequest();
        resizeRequest.setEnvironment(ENVIRONMENT_NAME);
        resizeRequest.setClusterShape(ENTERPRISE);
        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setId(1L);
        sdxCluster.setClusterShape(MEDIUM_DUTY_HA);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);
        sdxCluster.setRuntime("7.2.18");
        sdxCluster.setSeLinux(SeLinux.PERMISSIVE);
        sdxCluster.setCloudStorageBaseLocation("s3a://some/dir/");
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.STOPPED);
        InstanceGroupV4Response masterIg = new InstanceGroupV4Response();
        masterIg.setName("master");
        InstanceTemplateV4Response masterIgTemplate = new InstanceTemplateV4Response();
        masterIgTemplate.setInstanceType("m5.x4large");
        masterIg.setTemplate(masterIgTemplate);
        VolumeV4Response volumeV4Response = new VolumeV4Response();
        volumeV4Response.setSize(1024);
        masterIgTemplate.setAttachedVolumes(Set.of(volumeV4Response));
        stackV4Response.setInstanceGroups(List.of(masterIg));
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        stackV4Response.setCluster(clusterV4Response);
        stackV4Response.setNetwork(getNetworkForCurrentDatalake());
        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString()))
                .thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(sdxBackupRestoreService.isDatalakeInBackupProgress(anyString(), anyString())).thenReturn(false);
        when(sdxBackupRestoreService.isDatalakeInRestoreProgress(anyString(), anyString())).thenReturn(false);
        mockEnvironmentCall(resizeRequest, AWS);
        ArgumentCaptor<SdxCluster> captorResize = ArgumentCaptor.forClass(SdxCluster.class);
        when(sdxReactorFlowManager.triggerSdxResize(anyLong(), captorResize.capture(), any(DatalakeDrSkipOptions.class), eq(false)))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));
        String lightDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.10/aws/light_duty.json");
        CDPConfigKey cdpConfigKeyLightDuty = new CDPConfigKey(AWS, MEDIUM_DUTY_HA, "7.2.18");
        String enterpriseJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.18/aws/enterprise.json");
        CDPConfigKey cdpConfigKeyEnterprise = new CDPConfigKey(AWS, ENTERPRISE, "7.2.18");
        when(cdpConfigService.getConfigForKey(eq(cdpConfigKeyLightDuty))).thenReturn(JsonUtil.readValue(lightDutyJson, StackV4Request.class));
        when(cdpConfigService.getConfigForKey(eq(cdpConfigKeyEnterprise))).thenReturn(JsonUtil.readValue(enterpriseJson, StackV4Request.class));
        when(stackV4Endpoint.get(anyLong(), anyString(), anySet(), anyString())).thenReturn(stackV4Response);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.resizeSdx(USER_CRN, sdxCluster.getClusterName(), resizeRequest));

        StackV4Request stackV4Request = JsonUtil.readValue(captorResize.getValue().getStackRequest(), StackV4Request.class);
        InstanceGroupV4Request idbrokerInstGroup = stackV4Request.getInstanceGroups().stream().filter(ig -> "idbroker".equals(ig.getName())).findAny().get();
        InstanceGroupV4Request masterInstGroup = stackV4Request.getInstanceGroups().stream().filter(ig -> "master".equals(ig.getName())).findAny().get();
        assertEquals("t3.medium", idbrokerInstGroup.getTemplate().getInstanceType());
        assertEquals("m5.x4large", masterInstGroup.getTemplate().getInstanceType());
        assertEquals(1024, masterInstGroup.getTemplate().getAttachedVolumes().stream().findAny().get().getSize());
    }

    @Test
    void testSdxResizeWithLightDutyDatalakeModifiedShouldBeIgnored() throws IOException {
        SdxClusterResizeRequest resizeRequest = new SdxClusterResizeRequest();
        resizeRequest.setEnvironment(ENVIRONMENT_NAME);
        resizeRequest.setClusterShape(ENTERPRISE);
        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setId(1L);
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);
        sdxCluster.setRuntime("7.2.18");
        sdxCluster.setSeLinux(SeLinux.PERMISSIVE);
        sdxCluster.setCloudStorageBaseLocation("s3a://some/dir/");
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.STOPPED);
        InstanceGroupV4Response masterIg = new InstanceGroupV4Response();
        masterIg.setName("master");
        InstanceTemplateV4Response masterIgTemplate = new InstanceTemplateV4Response();
        masterIgTemplate.setInstanceType("m5.x4large");
        masterIg.setTemplate(masterIgTemplate);
        VolumeV4Response volumeV4Response = new VolumeV4Response();
        volumeV4Response.setSize(1024);
        masterIgTemplate.setAttachedVolumes(Set.of(volumeV4Response));
        stackV4Response.setInstanceGroups(List.of(masterIg));
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        stackV4Response.setCluster(clusterV4Response);
        stackV4Response.setNetwork(getNetworkForCurrentDatalake());
        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString()))
                .thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(sdxBackupRestoreService.isDatalakeInBackupProgress(anyString(), anyString())).thenReturn(false);
        when(sdxBackupRestoreService.isDatalakeInRestoreProgress(anyString(), anyString())).thenReturn(false);
        mockEnvironmentCall(resizeRequest, AWS);
        ArgumentCaptor<SdxCluster> captorResize = ArgumentCaptor.forClass(SdxCluster.class);
        when(sdxReactorFlowManager.triggerSdxResize(anyLong(), captorResize.capture(), any(DatalakeDrSkipOptions.class), eq(false)))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));
        String lightDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.10/aws/light_duty.json");
        CDPConfigKey cdpConfigKeyLightDuty = new CDPConfigKey(AWS, LIGHT_DUTY, "7.2.18");
        String enterpriseJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.18/aws/enterprise.json");
        CDPConfigKey cdpConfigKeyEnterprise = new CDPConfigKey(AWS, ENTERPRISE, "7.2.18");
        when(cdpConfigService.getConfigForKey(eq(cdpConfigKeyLightDuty))).thenReturn(JsonUtil.readValue(lightDutyJson, StackV4Request.class));
        when(cdpConfigService.getConfigForKey(eq(cdpConfigKeyEnterprise))).thenReturn(JsonUtil.readValue(enterpriseJson, StackV4Request.class));
        when(stackV4Endpoint.get(anyLong(), anyString(), anySet(), anyString())).thenReturn(stackV4Response);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.resizeSdx(USER_CRN, sdxCluster.getClusterName(), resizeRequest));

        StackV4Request stackV4Request = JsonUtil.readValue(captorResize.getValue().getStackRequest(), StackV4Request.class);
        InstanceGroupV4Request idbrokerInstGroup = stackV4Request.getInstanceGroups().stream().filter(ig -> "idbroker".equals(ig.getName())).findAny().get();
        InstanceGroupV4Request masterInstGroup = stackV4Request.getInstanceGroups().stream().filter(ig -> "master".equals(ig.getName())).findAny().get();
        assertEquals("t3.medium", idbrokerInstGroup.getTemplate().getInstanceType());
        assertEquals("m5.xlarge", masterInstGroup.getTemplate().getInstanceType());
        assertEquals(128, masterInstGroup.getTemplate().getAttachedVolumes().stream().findAny().get().getSize());
    }

    @Test
    void testSdxResizeCustomInstancesInvalidInstanceType() throws IOException {
        SdxClusterResizeRequest resizeRequest = new SdxClusterResizeRequest();
        resizeRequest.setEnvironment("environment");
        resizeRequest.setClusterShape(ENTERPRISE);
        SdxInstanceGroupRequest sdxInstanceGroupRequest = new SdxInstanceGroupRequest();
        sdxInstanceGroupRequest.setName("master");
        sdxInstanceGroupRequest.setInstanceType("r5.large");
        resizeRequest.setCustomInstanceGroups(List.of(sdxInstanceGroupRequest));

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setId(1L);
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);
        sdxCluster.setRuntime("7.2.17");
        sdxCluster.setCloudStorageBaseLocation("s3a://some/dir/");
        sdxCluster.setSeLinux(SeLinux.PERMISSIVE);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString()))
                .thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(sdxBackupRestoreService.isDatalakeInBackupProgress(anyString(), anyString())).thenReturn(false);
        when(sdxBackupRestoreService.isDatalakeInRestoreProgress(anyString(), anyString())).thenReturn(false);
        mockEnvironmentCall(resizeRequest, AWS);
        String mediumDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.10/aws/medium_duty_ha.json");
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(mediumDutyJson, StackV4Request.class));
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setCluster(new ClusterV4Response());
        stackV4Response.getCluster().setDbSSLEnabled(false);
        stackV4Response.setStatus(Status.STOPPED);
        stackV4Response.setNetwork(getNetworkForCurrentDatalake());
        when(stackV4Endpoint.get(anyLong(), anyString(), anySet(), anyString())).thenReturn(stackV4Response);
        doThrow(new BadRequestException("Invalid custom instance type for instance group: master - r5.large"))
                .when(sdxRecommendationService).validateVmTypeOverride(any(), any());

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                        underTest.resizeSdx(USER_CRN, sdxCluster.getClusterName(), resizeRequest)));

        assertEquals("Invalid custom instance type for instance group: master - r5.large", exception.getMessage());
    }

    @Test
    void resizeMoveRecipesToNewCluster() throws IOException {
        String accountId = Crn.safeFromString(USER_CRN).getAccountId();
        SdxClusterResizeRequest resizeRequest = new SdxClusterResizeRequest();
        resizeRequest.setEnvironment(ENVIRONMENT_NAME);
        resizeRequest.setClusterShape(ENTERPRISE);
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setCreateDatabase(true);
        sdxDatabase.setDatabaseAvailabilityType(SdxDatabaseAvailabilityType.NONE);
        sdxDatabase.setDatabaseEngineVersion("11");
        sdxDatabase.setId(1L);
        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setRuntime("7.2.17");
        sdxCluster.setRangerRazEnabled(true);
        sdxCluster.setRangerRmsEnabled(false);
        sdxCluster.setSdxDatabase(sdxDatabase);
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.setSeLinux(SeLinux.PERMISSIVE);
        sdxCluster.setCloudStorageBaseLocation("s3a://cloudStorageBase/location");
        DetailedEnvironmentResponse detailedEnvironmentResponse = getDetailedEnvironmentResponse();
        detailedEnvironmentResponse.setAccountId(accountId);
        RecipeV4Response recipeV4Response1 = new RecipeV4Response();
        recipeV4Response1.setName("recipe1");
        recipeV4Response1.setType(RecipeV4Type.POST_SERVICE_DEPLOYMENT);
        RecipeV4Response recipeV4Response2 = new RecipeV4Response();
        recipeV4Response2.setName("recipe2");
        RecipeV4Response recipeV4Response3 = new RecipeV4Response();
        recipeV4Response3.setName("recipe3");
        InstanceGroupV4Response masterIg = new InstanceGroupV4Response();
        masterIg.setName("master");
        masterIg.setRecipes(List.of(recipeV4Response1, recipeV4Response2));
        InstanceGroupV4Response coreIg = new InstanceGroupV4Response();
        coreIg.setName("core");
        coreIg.setRecipes(List.of(recipeV4Response1, recipeV4Response2, recipeV4Response3));
        StackV4Response stackV4Response = getStackV4ResponseForEnterpriseDatalake(Map.of("tag1", "value1"),
                List.of(masterIg, coreIg), getClusterV4Response());

        String enterpriseDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.17/aws/enterprise.json");

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(eq(accountId)))
                .thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(eq(accountId), eq(CLUSTER_NAME)))
                .thenReturn(Optional.of(sdxCluster));
        when(environmentClientService.getByName(eq(ENVIRONMENT_NAME)))
                .thenReturn(detailedEnvironmentResponse);
        when(stackV4Endpoint.get(eq(0L), eq(CLUSTER_NAME), anySet(), eq(accountId)))
                .thenReturn(stackV4Response);
        when(cdpConfigService.getConfigForKey(any()))
                .thenReturn(JsonUtil.readValue(enterpriseDutyJson, StackV4Request.class));
        when(sdxVersionRuleEnforcer.isRazSupported(any(), any())).thenReturn(true);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.resizeSdx(USER_CRN, CLUSTER_NAME, resizeRequest));

        ArgumentCaptor<SdxCluster> sdxClusterArgumentCaptor = ArgumentCaptor.forClass(SdxCluster.class);

        verify(sdxReactorFlowManager, times(1)).triggerSdxResize(eq(SDX_ID), sdxClusterArgumentCaptor.capture(),
                any(DatalakeDrSkipOptions.class), eq(false));

        SdxCluster createdSdxCluster = sdxClusterArgumentCaptor.getValue();

        // Validate new SdxCluster
        assertEquals(ENTERPRISE, createdSdxCluster.getClusterShape());
        assertEquals(SeLinux.PERMISSIVE, createdSdxCluster.getSeLinux());
        assertEquals("7.2.17", createdSdxCluster.getRuntime());
        assertEquals(sdxCluster.getCloudStorageBaseLocation(), createdSdxCluster.getCloudStorageBaseLocation());
        assertEquals(sdxCluster.getCloudStorageFileSystemType(), createdSdxCluster.getCloudStorageFileSystemType());
        assertEquals(sdxCluster.getTags(), createdSdxCluster.getTags());

        // Validate new StackRequest
        String stackRequestRawString = createdSdxCluster.getStackRequest();
        ObjectMapper mapper = new ObjectMapper();
        StackV4Request stackV4Request = mapper.readValue(stackRequestRawString, StackV4Request.class);

        assertEquals(11, stackV4Request.getJavaVersion());
        assertEquals(CLUSTER_NAME + ENTERPRISE.getResizeSuffix(), stackV4Request.getCustomDomain().getHostname());
        assertEquals("RHEL8", stackV4Request.getImage().getOs());
        assertEquals("cb-default", stackV4Request.getImage().getCatalog());
        assertEquals("random-uuid-id", stackV4Request.getImage().getId());
        ClusterV4Request cluster = stackV4Request.getCluster();
        assertTrue(cluster.isRangerRazEnabled());
        assertFalse(cluster.isRangerRmsEnabled());
        // validate recipes
        assertAll(() -> {
            assertEquals(11, stackV4Request.getInstanceGroups().size());
            Map<String, List<RecipeV4Response>> hostGroupRecipesMap = stackV4Response.getInstanceGroups()
                    .stream()
                    .collect(toMap(InstanceGroupV4Response::getName, InstanceGroupV4Response::getRecipes));
            assertEquals(3, hostGroupRecipesMap.get("core").size());
            assertEquals(2, hostGroupRecipesMap.get("master").size());
            List<String> coreRecipes = hostGroupRecipesMap.get("core").stream().map(RecipeV4Base::getName).toList();
            List<String> masterRecipes = hostGroupRecipesMap.get("master").stream().map(RecipeV4Base::getName).toList();
            assertTrue(coreRecipes.contains("recipe1"));
            assertTrue(coreRecipes.contains("recipe2"));
            assertTrue(coreRecipes.contains("recipe3"));
            assertTrue(masterRecipes.contains("recipe1"));
            assertTrue(masterRecipes.contains("recipe2"));
            assertFalse(masterRecipes.contains("recipe3"));
        });
    }

    @Test
    void resizeMoveRecipesToNewClusterWhenSeLinuxEnforcing() throws IOException {
        String accountId = Crn.safeFromString(USER_CRN).getAccountId();
        SdxClusterResizeRequest resizeRequest = new SdxClusterResizeRequest();
        resizeRequest.setEnvironment(ENVIRONMENT_NAME);
        resizeRequest.setClusterShape(ENTERPRISE);
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setCreateDatabase(true);
        sdxDatabase.setDatabaseAvailabilityType(SdxDatabaseAvailabilityType.NONE);
        sdxDatabase.setDatabaseEngineVersion("11");
        sdxDatabase.setId(1L);
        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setSeLinux(SeLinux.ENFORCING);
        sdxCluster.setRuntime("7.2.17");
        sdxCluster.setRangerRazEnabled(true);
        sdxCluster.setRangerRmsEnabled(false);
        sdxCluster.setSdxDatabase(sdxDatabase);
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.setCloudStorageBaseLocation("s3a://cloudStorageBase/location");
        DetailedEnvironmentResponse detailedEnvironmentResponse = getDetailedEnvironmentResponse();
        detailedEnvironmentResponse.setAccountId(accountId);
        RecipeV4Response recipeV4Response1 = new RecipeV4Response();
        recipeV4Response1.setName("recipe1");
        recipeV4Response1.setType(RecipeV4Type.POST_SERVICE_DEPLOYMENT);
        RecipeV4Response recipeV4Response2 = new RecipeV4Response();
        recipeV4Response2.setName("recipe2");
        RecipeV4Response recipeV4Response3 = new RecipeV4Response();
        recipeV4Response3.setName("recipe3");
        InstanceGroupV4Response masterIg = new InstanceGroupV4Response();
        masterIg.setName("master");
        masterIg.setRecipes(List.of(recipeV4Response1, recipeV4Response2));
        InstanceGroupV4Response coreIg = new InstanceGroupV4Response();
        coreIg.setName("core");
        coreIg.setRecipes(List.of(recipeV4Response1, recipeV4Response2, recipeV4Response3));
        StackV4Response stackV4Response = getStackV4ResponseForEnterpriseDatalake(Map.of("tag1", "value1"),
                List.of(masterIg, coreIg), getClusterV4Response());

        String enterpriseDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.17/aws/enterprise.json");

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(eq(accountId)))
                .thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(eq(accountId), eq(CLUSTER_NAME)))
                .thenReturn(Optional.of(sdxCluster));
        when(environmentClientService.getByName(eq(ENVIRONMENT_NAME)))
                .thenReturn(detailedEnvironmentResponse);
        when(stackV4Endpoint.get(eq(0L), eq(CLUSTER_NAME), anySet(), eq(accountId)))
                .thenReturn(stackV4Response);
        when(cdpConfigService.getConfigForKey(any()))
                .thenReturn(JsonUtil.readValue(enterpriseDutyJson, StackV4Request.class));
        when(sdxVersionRuleEnforcer.isRazSupported(any(), any())).thenReturn(true);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.resizeSdx(USER_CRN, CLUSTER_NAME, resizeRequest));

        ArgumentCaptor<SdxCluster> sdxClusterArgumentCaptor = ArgumentCaptor.forClass(SdxCluster.class);

        verify(sdxReactorFlowManager, times(1)).triggerSdxResize(eq(SDX_ID), sdxClusterArgumentCaptor.capture(),
                any(DatalakeDrSkipOptions.class), eq(false));

        SdxCluster createdSdxCluster = sdxClusterArgumentCaptor.getValue();

        // Validate new SdxCluster
        assertEquals(ENTERPRISE, createdSdxCluster.getClusterShape());
        assertEquals(SeLinux.ENFORCING, createdSdxCluster.getSeLinux());
        assertEquals("7.2.17", createdSdxCluster.getRuntime());
        assertEquals(sdxCluster.getCloudStorageBaseLocation(), createdSdxCluster.getCloudStorageBaseLocation());
        assertEquals(sdxCluster.getCloudStorageFileSystemType(), createdSdxCluster.getCloudStorageFileSystemType());
        assertEquals(sdxCluster.getTags(), createdSdxCluster.getTags());

        // Validate new StackRequest
        String stackRequestRawString = createdSdxCluster.getStackRequest();
        ObjectMapper mapper = new ObjectMapper();
        StackV4Request stackV4Request = mapper.readValue(stackRequestRawString, StackV4Request.class);

        assertEquals(11, stackV4Request.getJavaVersion());
        assertEquals(CLUSTER_NAME + ENTERPRISE.getResizeSuffix(), stackV4Request.getCustomDomain().getHostname());
        assertEquals("RHEL8", stackV4Request.getImage().getOs());
        assertEquals("cb-default", stackV4Request.getImage().getCatalog());
        assertEquals("random-uuid-id", stackV4Request.getImage().getId());
        ClusterV4Request cluster = stackV4Request.getCluster();
        assertTrue(cluster.isRangerRazEnabled());
        assertFalse(cluster.isRangerRmsEnabled());
        // validate recipes
        assertAll(() -> {
            assertEquals(11, stackV4Request.getInstanceGroups().size());
            Map<String, List<RecipeV4Response>> hostGroupRecipesMap = stackV4Response.getInstanceGroups()
                    .stream()
                    .collect(toMap(InstanceGroupV4Response::getName, InstanceGroupV4Response::getRecipes));
            assertEquals(3, hostGroupRecipesMap.get("core").size());
            assertEquals(2, hostGroupRecipesMap.get("master").size());
            List<String> coreRecipes = hostGroupRecipesMap.get("core").stream().map(RecipeV4Base::getName).toList();
            List<String> masterRecipes = hostGroupRecipesMap.get("master").stream().map(RecipeV4Base::getName).toList();
            assertTrue(coreRecipes.contains("recipe1"));
            assertTrue(coreRecipes.contains("recipe2"));
            assertTrue(coreRecipes.contains("recipe3"));
            assertTrue(masterRecipes.contains("recipe1"));
            assertTrue(masterRecipes.contains("recipe2"));
            assertFalse(masterRecipes.contains("recipe3"));
        });
    }

    @Test
    void ensureNoShapeHasSameResizeSuffix() {
        Set<String> resizeSuffixes = Arrays.stream(SdxClusterShape.values())
                .map(SdxClusterShape::getResizeSuffix)
                .collect(Collectors.toSet());
        assertEquals(SdxClusterShape.values().length, resizeSuffixes.size());
    }

    @Test
    void ensureNoShapeHasEmptyResizeSuffix() {
        Arrays.stream(SdxClusterShape.values()).forEach(shape -> assertFalse(Strings.isNullOrEmpty(shape.getResizeSuffix())));
    }

    private DetailedEnvironmentResponse getDetailedEnvironmentResponse() {
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCreator(USER_CRN);
        environmentResponse.setCloudPlatform("AWS");
        environmentResponse.setEnvironmentStatus(EnvironmentStatus.AVAILABLE);
        return environmentResponse;
    }

    private SdxClusterRequest getSdxClusterRequest() {
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setRuntime("7.2.18");
        sdxClusterRequest.setClusterShape(ENTERPRISE);
        sdxClusterRequest.setAws(new SdxAwsRequest());
        sdxClusterRequest.setEnvironment(ENVIRONMENT_CRN);
        sdxClusterRequest.setEnableRangerRms(true);
        sdxClusterRequest.setEnableRangerRaz(true);
        sdxClusterRequest.setJavaVersion(11);
        sdxClusterRequest.setOs("os");
        SdxCloudStorageRequest sdxCloudStorageRequest = new SdxCloudStorageRequest();
        sdxCloudStorageRequest.setBaseLocation("s3a://");
        sdxCloudStorageRequest.setFileSystemType(FileSystemType.S3);
        sdxCloudStorageRequest.setS3(new S3CloudStorageV1Parameters());
        sdxClusterRequest.setCloudStorage(sdxCloudStorageRequest);
        return sdxClusterRequest;
    }

    private StackV4Response getStackV4ResponseForEnterpriseDatalake(Map<String, String> userDefinedTags, List<InstanceGroupV4Response> instanceGroups,
            ClusterV4Response clusterV4Response) {
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setId(1L);
        stackV4Response.setCrn(getCrn());
        stackV4Response.setEnvironmentName(ENVIRONMENT_NAME);
        stackV4Response.setEnvironmentCrn(ENVIRONMENT_CRN);
        stackV4Response.setCredentialCrn(getCrn());
        stackV4Response.setCredentialName("");
        stackV4Response.setGovCloud(false);
        stackV4Response.setStatus(Status.STOPPED);
        stackV4Response.setCluster(clusterV4Response);
        stackV4Response.setNetwork(getNetworkV4ResponseForAWS());
        stackV4Response.setInstanceGroups(instanceGroups);
        stackV4Response.setCreated(Instant.now().getEpochSecond());
        stackV4Response.setGatewayPort(9443);
        stackV4Response.setImage(getStackImageV4Response());
        CloudbreakDetailsV4Response cloudbreakDetailsV4Response = new CloudbreakDetailsV4Response();
        cloudbreakDetailsV4Response.setVersion("2.85.0-106b");
        stackV4Response.setCloudbreakDetails(cloudbreakDetailsV4Response);
        stackV4Response.setNodeCount(10);
        stackV4Response.setTags(getTagsV4Response(userDefinedTags));
        stackV4Response.setTelemetry(new TelemetryResponse());
        stackV4Response.setCustomDomains(getCustomDomainSettingsV4Response());
        stackV4Response.setPlacement(getPlacementSettingsV4Response());
        stackV4Response.setCloudPlatform(AWS);
        stackV4Response.setJavaVersion(11);
        stackV4Response.setEnableMultiAz(true);
        stackV4Response.setEnableLoadBalancer(false);
        stackV4Response.setExternalDatabase(getDatabaseResponse());
        return stackV4Response;
    }

    private CustomDomainSettingsV4Response getCustomDomainSettingsV4Response() {
        CustomDomainSettingsV4Response customDomainSettingsV4Response = new CustomDomainSettingsV4Response();
        customDomainSettingsV4Response.setClusterNameAsSubdomain(false);
        customDomainSettingsV4Response.setDomainName("xcu2-8y8x.wl.cloudera.site");
        customDomainSettingsV4Response.setHostname(CLUSTER_NAME);
        customDomainSettingsV4Response.setHostgroupNameAsHostname(true);
        return customDomainSettingsV4Response;
    }

    private NetworkV4Response getNetworkV4ResponseForAWS() {
        AwsNetworkV4Parameters awsNetworkV4Parameters = new AwsNetworkV4Parameters();
        awsNetworkV4Parameters.setSubnetId("subnet-0d0ae15bc71549212");
        awsNetworkV4Parameters.setVpcId("vpc-0507f88d49efb8118");
        awsNetworkV4Parameters.setEndpointGatewaySubnetId("subnet-0d0ae15bc71549212");
        NetworkV4Response networkV4Response = new NetworkV4Response();
        networkV4Response.setId(1L);
        networkV4Response.setAws(awsNetworkV4Parameters);
        networkV4Response.setSubnetCIDR("10.117.224.0/20");
        return networkV4Response;
    }

    private PlacementSettingsV4Response getPlacementSettingsV4Response() {
        PlacementSettingsV4Response placementSettingsV4Response = new PlacementSettingsV4Response();
        placementSettingsV4Response.setRegion("us-west-1");
        placementSettingsV4Response.setAvailabilityZone("az-1");
        return placementSettingsV4Response;
    }

    private TagsV4Response getTagsV4Response(Map<String, String> userDefinedTags) {
        TagsV4Response tagsV4Response = new TagsV4Response();
        tagsV4Response.setUserDefined(userDefinedTags);
        return tagsV4Response;
    }

    private DatabaseResponse getDatabaseResponse() {
        DatabaseResponse databaseResponse = new DatabaseResponse();
        databaseResponse.setAvailabilityType(DatabaseAvailabilityType.NONE);
        databaseResponse.setDatalakeDatabaseAvailabilityType(DatabaseAvailabilityType.NONE);
        databaseResponse.setDatabaseEngineVersion("11");
        return databaseResponse;
    }

    private StackImageV4Response getStackImageV4Response() {
        StackImageV4Response stackImageV4Response = new StackImageV4Response();
        stackImageV4Response.setCatalogName("cb-default");
        stackImageV4Response.setOs("RHEL8");
        stackImageV4Response.setName("default-name");
        stackImageV4Response.setId("random-uuid-id");
        stackImageV4Response.setCatalogUrl("url");
        return stackImageV4Response;
    }

    private ClusterV4Response getClusterV4Response() {
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        clusterV4Response.setId(1L);
        clusterV4Response.setRangerRazEnabled(true);
        clusterV4Response.setRangerRmsEnabled(false);
        clusterV4Response.setStatus(Status.AVAILABLE);
        clusterV4Response.setName(CLUSTER_NAME);
        return clusterV4Response;
    }

    private NetworkV4Response getNetworkForCurrentDatalake() {
        NetworkV4Response networkResponse = new NetworkV4Response();
        AwsNetworkV4Parameters awsNetworkV4Parameters = new AwsNetworkV4Parameters();
        awsNetworkV4Parameters.setVpcId("vpc-123");
        awsNetworkV4Parameters.setSubnetId("subnet-123");
        awsNetworkV4Parameters.setEndpointGatewaySubnetId("subnet-123");
        networkResponse.setAws(awsNetworkV4Parameters);
        return networkResponse;
    }

    @Test
    void testSdxGetDetailsWithResources() {
        StackV4Response stackResponse = mock(StackV4Response.class);
        when(stackV4Endpoint.getWithResources(anyLong(), anyString(), any(), anyString())).thenReturn(stackResponse);
        Set<String> entries = Set.of();

        StackV4Response result = underTest.getDetailWithResources("test", entries, "accountId");
        assertEquals(stackResponse, result);
        verify(stackV4Endpoint).getWithResources(0L, "test", entries, "accountId");
    }

    @Test
    void testSdxGetDetailsWithResourcesNull() {
        when(stackV4Endpoint.getWithResources(anyLong(), anyString(), any(), anyString())).thenThrow(new jakarta.ws.rs.NotFoundException("test"));
        Set<String> entries = Set.of();

        StackV4Response result = underTest.getDetailWithResources("test", entries, "accountId");
        assertNull(result);
        verify(stackV4Endpoint).getWithResources(0L, "test", entries, "accountId");
    }

    @Test
    void testSdxResizeUsingPreviousDatalakeNetwork() throws IOException {
        SdxClusterResizeRequest resizeRequest = new SdxClusterResizeRequest();
        resizeRequest.setEnvironment(ENVIRONMENT_NAME);
        resizeRequest.setClusterShape(ENTERPRISE);
        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setId(SDX_ID);
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.setEnableMultiAz(true);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);
        sdxCluster.setRuntime("7.2.18");
        sdxCluster.setCloudStorageBaseLocation("s3a://some/dir/");
        sdxCluster.setSeLinux(SeLinux.PERMISSIVE);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString()))
                .thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(sdxBackupRestoreService.isDatalakeInBackupProgress(anyString(), anyString())).thenReturn(false);
        when(sdxBackupRestoreService.isDatalakeInRestoreProgress(anyString(), anyString())).thenReturn(false);

        mockEnvironmentCall(resizeRequest, AWS);
        when(sdxReactorFlowManager.triggerSdxResize(anyLong(), any(SdxCluster.class), any(DatalakeDrSkipOptions.class), eq(false)))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));

        String mediumDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.18/aws/enterprise.json");
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(mediumDutyJson, StackV4Request.class));

        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.STOPPED);
        stackV4Response.setCloudPlatform(AWS);
        StackImageV4Response image = new StackImageV4Response();
        image.setOs(OS_NAME);
        image.setCatalogName(CATALOG_NAME);
        stackV4Response.setImage(image);
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        stackV4Response.setCluster(clusterV4Response);
        InstanceGroupV4Response coreInstaceGroup = new InstanceGroupV4Response();
        coreInstaceGroup.setName("core");
        InstanceMetaDataV4Response coreMetadata = new InstanceMetaDataV4Response();
        coreMetadata.setSubnetId("subnet1");
        coreMetadata.setAvailabilityZone("az1");
        coreInstaceGroup.setMetadata(Set.of(coreMetadata));
        InstanceGroupV4Response masterInstaceGroup = new InstanceGroupV4Response();
        masterInstaceGroup.setName("master");
        InstanceMetaDataV4Response masterMetadata = new InstanceMetaDataV4Response();
        masterMetadata.setSubnetId("subnet2");
        masterMetadata.setAvailabilityZone("az2");
        masterInstaceGroup.setMetadata(Set.of(masterMetadata));
        InstanceGroupV4Response gatewayInstaceGroup = new InstanceGroupV4Response();
        gatewayInstaceGroup.setName("gateway");
        InstanceMetaDataV4Response gatewayMetadata = new InstanceMetaDataV4Response();
        gatewayMetadata.setSubnetId("subnet3");
        gatewayMetadata.setAvailabilityZone("az3");
        gatewayInstaceGroup.setMetadata(Set.of(gatewayMetadata));
        stackV4Response.setInstanceGroups(List.of(coreInstaceGroup, masterInstaceGroup, gatewayInstaceGroup));
        stackV4Response.setNetwork(getNetworkForCurrentDatalake());

        when(stackV4Endpoint.get(anyLong(), anyString(), anySet(), anyString())).thenReturn(stackV4Response);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.resizeSdx(USER_CRN, CLUSTER_NAME, resizeRequest));

        Map<String, Set<String>> subnetsByAz = Map.of("az1", Set.of("subnet1"), "az2", Set.of("subnet2"), "az3", Set.of("subnet3"));
        verify(multiAzDecorator, times(1)).decorateStackRequestWithPreviousNetwork(any(), any(), eq(subnetsByAz));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"AZURE", "GCP"})
    void testSdxResizeUsingPreviousDatalakeNetwork(String cloudPlatform) throws IOException {
        SdxClusterResizeRequest resizeRequest = new SdxClusterResizeRequest();
        resizeRequest.setEnvironment(ENVIRONMENT_NAME);
        resizeRequest.setClusterShape(ENTERPRISE);
        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setId(SDX_ID);
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.setEnableMultiAz(true);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);
        sdxCluster.setRuntime("7.2.18");
        sdxCluster.setCloudStorageBaseLocation("s3a://some/dir/");
        sdxCluster.setSeLinux(SeLinux.PERMISSIVE);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString()))
                .thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(sdxBackupRestoreService.isDatalakeInBackupProgress(anyString(), anyString())).thenReturn(false);
        when(sdxBackupRestoreService.isDatalakeInRestoreProgress(anyString(), anyString())).thenReturn(false);

        mockEnvironmentCall(resizeRequest, AWS);
        when(sdxReactorFlowManager.triggerSdxResize(anyLong(), any(SdxCluster.class), any(DatalakeDrSkipOptions.class), eq(false)))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));

        String mediumDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.18/aws/enterprise.json");
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(mediumDutyJson, StackV4Request.class));

        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.STOPPED);
        stackV4Response.setCloudPlatform(CloudPlatform.valueOf(cloudPlatform));
        StackImageV4Response image = new StackImageV4Response();
        image.setOs(OS_NAME);
        image.setCatalogName(CATALOG_NAME);
        stackV4Response.setImage(image);
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        stackV4Response.setCluster(clusterV4Response);

        stackV4Response.setInstanceGroups(getInstanceGroups(CloudPlatform.valueOf(cloudPlatform)));
        stackV4Response.setNetwork(getNetworkForCurrentDatalake());

        when(stackV4Endpoint.get(anyLong(), anyString(), anySet(), anyString())).thenReturn(stackV4Response);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.resizeSdx(USER_CRN, CLUSTER_NAME, resizeRequest));

        Map<String, Set<String>> subnetsByAz = Map.of("1", Set.of("subnet1", "subnet2"), "2", Set.of("subnet1", "subnet2"),
                "3", Set.of("subnet1", "subnet2"));

        verify(multiAzDecorator, times(1)).decorateStackRequestWithPreviousNetwork(any(), any(), eq(subnetsByAz));
    }

    @Test
    void testSdxResizeShouldNotUsePreviousDatalakeNetworkForMultiAzIfThereIsOnlyOneAz() throws IOException {
        SdxClusterResizeRequest resizeRequest = new SdxClusterResizeRequest();
        resizeRequest.setEnvironment(ENVIRONMENT_NAME);
        resizeRequest.setClusterShape(ENTERPRISE);
        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setId(SDX_ID);
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.setEnableMultiAz(true);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);
        sdxCluster.setRuntime("7.2.18");
        sdxCluster.setCloudStorageBaseLocation("s3a://some/dir/");
        sdxCluster.setSeLinux(SeLinux.PERMISSIVE);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString()))
                .thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(sdxBackupRestoreService.isDatalakeInBackupProgress(anyString(), anyString())).thenReturn(false);
        when(sdxBackupRestoreService.isDatalakeInRestoreProgress(anyString(), anyString())).thenReturn(false);

        mockEnvironmentCall(resizeRequest, AWS);
        when(sdxReactorFlowManager.triggerSdxResize(anyLong(), any(SdxCluster.class), any(DatalakeDrSkipOptions.class), eq(false)))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));

        String mediumDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.18/aws/enterprise.json");
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(mediumDutyJson, StackV4Request.class));

        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.STOPPED);
        stackV4Response.setCloudPlatform(AWS);
        StackImageV4Response image = new StackImageV4Response();
        image.setOs(OS_NAME);
        image.setCatalogName(CATALOG_NAME);
        stackV4Response.setImage(image);
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        stackV4Response.setCluster(clusterV4Response);
        InstanceGroupV4Response coreInstaceGroup = new InstanceGroupV4Response();
        coreInstaceGroup.setName("core");
        InstanceMetaDataV4Response coreMetadata = new InstanceMetaDataV4Response();
        coreMetadata.setSubnetId("subnet1");
        coreMetadata.setAvailabilityZone("az1");
        coreInstaceGroup.setMetadata(Set.of(coreMetadata));
        InstanceGroupV4Response masterInstaceGroup = new InstanceGroupV4Response();
        masterInstaceGroup.setName("master");
        InstanceMetaDataV4Response masterMetadata = new InstanceMetaDataV4Response();
        masterMetadata.setSubnetId("subnet2");
        masterMetadata.setAvailabilityZone("az1");
        masterInstaceGroup.setMetadata(Set.of(masterMetadata));
        InstanceGroupV4Response gatewayInstaceGroup = new InstanceGroupV4Response();
        gatewayInstaceGroup.setName("gateway");
        InstanceMetaDataV4Response gatewayMetadata = new InstanceMetaDataV4Response();
        gatewayMetadata.setSubnetId("subnet3");
        gatewayMetadata.setAvailabilityZone("az1");
        gatewayInstaceGroup.setMetadata(Set.of(gatewayMetadata));
        stackV4Response.setInstanceGroups(List.of(coreInstaceGroup, masterInstaceGroup, gatewayInstaceGroup));
        stackV4Response.setNetwork(getNetworkForCurrentDatalake());

        when(stackV4Endpoint.get(anyLong(), anyString(), anySet(), anyString())).thenReturn(stackV4Response);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.resizeSdx(USER_CRN, CLUSTER_NAME, resizeRequest));

        Map<String, Set<String>> subnetsByAz = Map.of("az1", Set.of("subnet1", "subnet2", "subnet3"));
        verify(multiAzDecorator, never()).decorateStackRequestWithPreviousNetwork(any(), any(), eq(subnetsByAz));
    }

    private List<InstanceGroupV4Response> getInstanceGroups(CloudPlatform cloudPlatform) {
        List<InstanceGroupV4Response> instanceGroups = new ArrayList<>();
        if (cloudPlatform.equals(AZURE)) {
            InstanceGroupV4Response masterInstanceGroup = new InstanceGroupV4Response();
            InstanceGroupNetworkV4Response masterNetwork = new InstanceGroupNetworkV4Response();
            InstanceGroupAzureNetworkV4Parameters azureMaster = new InstanceGroupAzureNetworkV4Parameters();
            masterInstanceGroup.setNetwork(masterNetwork);
            masterNetwork.setAzure(azureMaster);
            azureMaster.setAvailabilityZones(Set.of("1", "2", "3"));
            azureMaster.setSubnetIds(List.of("subnet1"));

            InstanceGroupV4Response gatewayInstanceGroup = new InstanceGroupV4Response();
            InstanceGroupNetworkV4Response gatewayNetwork = new InstanceGroupNetworkV4Response();
            InstanceGroupAzureNetworkV4Parameters gatewayAzure = new InstanceGroupAzureNetworkV4Parameters();
            gatewayInstanceGroup.setNetwork(gatewayNetwork);
            gatewayNetwork.setAzure(gatewayAzure);
            gatewayAzure.setAvailabilityZones(Set.of("1", "2", "3"));
            gatewayAzure.setSubnetIds(List.of("subnet2"));

            InstanceGroupV4Response coreInstanceGroup = new InstanceGroupV4Response();
            InstanceGroupNetworkV4Response coreNetwork = new InstanceGroupNetworkV4Response();
            InstanceGroupAzureNetworkV4Parameters coreAzure = new InstanceGroupAzureNetworkV4Parameters();
            coreInstanceGroup.setNetwork(coreNetwork);
            coreNetwork.setAzure(coreAzure);
            coreAzure.setAvailabilityZones(Set.of("1", "2", "3"));
            coreAzure.setSubnetIds(List.of("subnet1"));

            instanceGroups.addAll(List.of(masterInstanceGroup, gatewayInstanceGroup, coreInstanceGroup));
        } else if (cloudPlatform.equals(GCP)) {
            InstanceGroupV4Response masterInstanceGroup = new InstanceGroupV4Response();
            InstanceGroupNetworkV4Response masterNetwork = new InstanceGroupNetworkV4Response();
            InstanceGroupGcpNetworkV4Parameters gcpMaster = new InstanceGroupGcpNetworkV4Parameters();
            masterInstanceGroup.setNetwork(masterNetwork);
            masterNetwork.setGcp(gcpMaster);
            gcpMaster.setAvailabilityZones(Set.of("1", "2", "3"));
            gcpMaster.setSubnetIds(List.of("subnet1"));

            InstanceGroupV4Response gatewayInstanceGroup = new InstanceGroupV4Response();
            InstanceGroupNetworkV4Response gatewayNetwork = new InstanceGroupNetworkV4Response();
            InstanceGroupGcpNetworkV4Parameters gatewayGcp = new InstanceGroupGcpNetworkV4Parameters();
            gatewayInstanceGroup.setNetwork(gatewayNetwork);
            gatewayNetwork.setGcp(gatewayGcp);
            gatewayGcp.setAvailabilityZones(Set.of("1", "2", "3"));
            gatewayGcp.setSubnetIds(List.of("subnet2"));

            InstanceGroupV4Response coreInstanceGroup = new InstanceGroupV4Response();
            InstanceGroupNetworkV4Response coreNetwork = new InstanceGroupNetworkV4Response();
            InstanceGroupGcpNetworkV4Parameters coreGcp = new InstanceGroupGcpNetworkV4Parameters();
            coreInstanceGroup.setNetwork(coreNetwork);
            coreNetwork.setGcp(coreGcp);
            coreGcp.setAvailabilityZones(Set.of("1", "2", "3"));
            coreGcp.setSubnetIds(List.of("subnet1"));

            instanceGroups.addAll(List.of(masterInstanceGroup, gatewayInstanceGroup, coreInstanceGroup));
        }
        return instanceGroups;
    }
}