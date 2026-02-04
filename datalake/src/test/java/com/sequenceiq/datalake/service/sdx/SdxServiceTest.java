package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.GCP;
import static com.sequenceiq.common.api.type.InstanceGroupType.CORE;
import static com.sequenceiq.common.api.type.InstanceGroupType.GATEWAY;
import static com.sequenceiq.datalake.service.sdx.SdxTestUtil.CLUSTER_NAME;
import static com.sequenceiq.datalake.service.sdx.SdxTestUtil.ENVIRONMENT_CRN;
import static com.sequenceiq.datalake.service.sdx.SdxTestUtil.SDX_CRN;
import static com.sequenceiq.datalake.service.sdx.SdxTestUtil.USER_CRN;
import static com.sequenceiq.datalake.service.sdx.SdxTestUtil.getSdxCluster;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.base.Strings;
import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.BaseStackDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerProductV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.AccountIdService;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.sdx.TargetPlatform;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.vm.CommonJavaVersionValidator;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.type.LoadBalancerSku;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.common.model.ImageCatalogPlatform;
import com.sequenceiq.common.model.OsType;
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
@DisplayName("SDX service tests")
class SdxServiceTest {
    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:default:datalake:e438a2db-d650-4132-ae62-242c5ba2f784";

    private static final Map<String, String> TAGS = Collections.singletonMap("mytag", "tagecske");

    private static final String OS = "centos7";

    private static final String ACCOUNT_ID = "accountId";

    @Mock
    private SdxExternalDatabaseConfigurer externalDatabaseConfigurer;

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private SdxDatabaseRepository sdxDatabaseRepository;

    @Mock
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private Clock clock;

    @Mock
    private CloudStorageManifester cloudStorageManifester;

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
    private CommonJavaVersionValidator commonJavaVersionValidator;

    @Mock
    private PlatformStringTransformer platformStringTransformer;

    @Mock
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @Mock
    private MultiAzDecorator multiAzDecorator;

    @Mock
    private StackService stackService;

    @Mock
    private RecipeService recipeService;

    @Mock
    private CcmService ccmService;

    @Mock
    private RangerRazService rangerRazService;

    @Mock
    private SdxInstanceService sdxInstanceService;

    @Mock
    private StorageValidationService storageValidationService;

    @Mock
    private StackRequestHandler stackRequestHandler;

    @Mock
    private SecurityConfigService securityConfigService;

    @Mock
    private ShapeValidator shapeValidator;

    @Mock
    private RangerRmsService rangerRmsService;

    @Mock
    private ImageCatalogPlatform imageCatalogPlatform;

    @Mock
    private PlatformConfig platformConfig;

    @Mock
    private AccountIdService accountIdService;

    @Mock
    private EncryptionProfileService encryptionProfileService;

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

    @BeforeEach
    void initMocks() {
        lenient().doNothing().when(platformAwareSdxConnector).validateIfOtherPlatformsHasSdx(any(), any());
        lenient().when(entitlementService.isEntitledToUseOS(any(), eq(OsType.CENTOS7))).thenReturn(true);
    }

    @Test
    void testOtherPlatformValidationFailure() {
        doThrow(BadRequestException.class).when(platformAwareSdxConnector).validateIfOtherPlatformsHasSdx(any(), any());
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCrn(ENVIRONMENT_CRN);
        environmentResponse.setEnvironmentStatus(EnvironmentStatus.AVAILABLE);
        when(environmentService.validateAndGetEnvironment(anyString())).thenReturn(environmentResponse);
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
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(eq(ACCOUNT_ID), eq(CLUSTER_NAME)))
                .thenReturn(Optional.of(sdxCluser));
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

        SdxCluster returnedSdxCluster = underTest.getByNameInAccount(USER_CRN, CLUSTER_NAME);

        assertEquals(sdxCluser, returnedSdxCluster);
    }

    @Test
    void testGetSdxClusterByNameOrCrnWhenClusterNameProvidedShouldReturnSdxClusterWithTheSameNameAsTheRequest() {
        SdxCluster sdxCluser = new SdxCluster();
        sdxCluser.setEnvName("env");
        sdxCluser.setClusterName(CLUSTER_NAME);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(eq(ACCOUNT_ID), eq(CLUSTER_NAME)))
                .thenReturn(Optional.of(sdxCluser));
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

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
        Architecture architecture = underTest.validateAndGetArchitecture(sdxClusterRequest, imageV4Response, AWS, "1");
        assertEquals(Architecture.ARM64, architecture);
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
        when(sdxClusterRepository.findByAccountIdAndCrnAndDeletedIsNull(eq(ACCOUNT_ID), eq(ENVIRONMENT_CRN))).thenReturn(Optional.of(sdxCluser));
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

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
        when(sdxClusterRepository.findByAccountIdAndCrnAndDeletedIsNull(eq(ACCOUNT_ID), eq(ENVIRONMENT_CRN))).thenReturn(Optional.of(sdxCluser));
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

        SdxCluster returnedSdxCluster = underTest.getByNameOrCrn(USER_CRN, NameOrCrn.ofCrn(ENVIRONMENT_CRN));

        assertEquals(sdxCluser, returnedSdxCluster);
    }

    @Test
    void testGetSdxClusterByNameOrCrnWhenClusterCrnProvidedThrowsExceptionIfClusterDoesNotExists() {
        SdxCluster sdxCluser = new SdxCluster();
        sdxCluser.setEnvName("env");
        sdxCluser.setClusterName(CLUSTER_NAME);
        when(sdxClusterRepository.findByAccountIdAndCrnAndDeletedIsNull(eq(ACCOUNT_ID), eq(ENVIRONMENT_CRN))).thenReturn(Optional.empty());
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

        Assertions.assertThatCode(() -> underTest.getByNameOrCrn(USER_CRN, NameOrCrn.ofCrn(ENVIRONMENT_CRN)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("SDX cluster '" + ENVIRONMENT_CRN + "' not found.");
    }

    @Test
    void testGetSdxClusterByNameOrCrnWhenClusterNameProvidedThrowsExceptionIfClusterDoesNotExists() {
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.empty());
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

        Assertions.assertThatCode(() -> underTest.getByNameOrCrn(USER_CRN, NameOrCrn.ofName(CLUSTER_NAME)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("SDX cluster '" + CLUSTER_NAME + "' not found.");
    }

    @Test
    void testGetSdxClusterByAccountIdWhenNoDeployedClusterShouldThrowSdxNotFoundException() {
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.empty());
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

        NotFoundException notFoundException = assertThrows(NotFoundException.class, () -> underTest.getByNameInAccount(USER_CRN, "sdxcluster"));

        assertEquals("SDX cluster 'sdxcluster' not found.", notFoundException.getMessage());
    }

    @Test
    void testListSdxClustersWhenEnvironmentNameProvidedAndTwoSdxIsInTheDatabaseShouldListAllSdxClusterWhichIsTwo() {
        List<SdxCluster> sdxClusters = List.of(new SdxCluster(), new SdxCluster());
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(eq(ACCOUNT_ID), eq("envir"))).thenReturn(sdxClusters);
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

        List<SdxCluster> sdxList = underTest.listSdx(USER_CRN, "envir");

        assertEquals(2, sdxList.size());
    }

    @Test
    void testListSdxClustersWhenEnvironmentCrnProvidedAndTwoSdxIsInTheDatabaseShouldListAllSdxClusterWhichIsTwo() {
        List<SdxCluster> sdxClusters = List.of(new SdxCluster(), new SdxCluster());
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsFalse(eq(ACCOUNT_ID), eq(ENVIRONMENT_CRN))).thenReturn(sdxClusters);
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

        List<SdxCluster> sdxList = underTest.listSdxByEnvCrn(USER_CRN, ENVIRONMENT_CRN);

        assertEquals(2, sdxList.size());
    }

    @Test
    void testSyncSdxClusterWhenClusterNameSpecifiedShouldCallStackEndpointExactlyOnce() {
        SdxCluster sdxCluser = new SdxCluster();
        sdxCluser.setEnvName("env");
        sdxCluser.setClusterName(CLUSTER_NAME);
        when(sdxClusterRepository.findByAccountIdAndCrnAndDeletedIsNull(eq(ACCOUNT_ID), eq(DATALAKE_CRN)))
                .thenReturn(Optional.of(sdxCluser));
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

        underTest.syncByCrn(USER_CRN, DATALAKE_CRN);

        verify(stackService, times(1)).sync(eq(CLUSTER_NAME), anyString());
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
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

        Map<String, Optional<String>> result = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getEnvironmentCrnsByResourceCrns(List.of("crn1", "crn2", "crn3")));

        Map<String, Optional<String>> expected = new LinkedHashMap<>();
        expected.put("crn1", Optional.of("envcrn1"));
        expected.put("crn2", Optional.of("envcrn2"));
        expected.put("crn3", Optional.empty());
        assertEquals(expected, result);
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
    public void testAddRmsToSdxCluster() throws IOException, TransactionExecutionException {
        DetailedEnvironmentResponse environmentResponse = getDetailedEnvironmentResponse();
        doNothing().when(commonJavaVersionValidator).validateByVmConfiguration(any(), anyInt());
        when(environmentService.validateAndGetEnvironment(anyString())).thenReturn(environmentResponse);
        when(platformStringTransformer.getPlatformStringForImageCatalog(anyString(), anyBoolean())).thenReturn(imageCatalogPlatform);
        BaseStackDetailsV4Response baseStackDetailsV4Response = new BaseStackDetailsV4Response();
        baseStackDetailsV4Response.setVersion("7.2.18");
        ImageV4Response imageV4Response = new ImageV4Response();
        imageV4Response.setStackDetails(baseStackDetailsV4Response);
        when(imageCatalogService.getImageResponseFromImageRequest(any(), any())).thenReturn(imageV4Response);
        String enterpriseJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.18/aws/enterprise.json");
        StackV4Request stackV4Request = JsonUtil.readValue(enterpriseJson, StackV4Request.class);
        when(stackRequestHandler.getStackRequest(any(), any(), any(), any(), any(), any())).thenReturn(stackV4Request);
        SdxClusterRequest sdxClusterRequest = getSdxClusterRequest();

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, "name", sdxClusterRequest, null));

        verify(commonJavaVersionValidator, times(1)).validateByVmConfiguration(any(), eq(11));
        verify(imageCatalogService, times(1)).getImageResponseFromImageRequest(any(), any());
        verify(environmentService, times(2)).validateAndGetEnvironment(anyString());
        verify(stackRequestHandler, times(1)).getStackRequest(any(), any(), any(), any(), any(), any());
        verify(externalDatabaseConfigurer, times(1)).configure(any(), anyString(), any(), any(), any());
        verify(sdxReactorFlowManager, times(1)).triggerSdxCreation(any());
        verify(transactionService, times(1)).required(any(Supplier.class));
        verify(ownerAssignmentService, times(1)).assignResourceOwnerRoleIfEntitled(anyString(), any());
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

    @Test
    void testValidateRuntimeAndImageWhenDefaultEncryptionProfileIsUsed() {
        SdxClusterRequest clusterRequest = new SdxClusterRequest();
        clusterRequest.setRuntime("7.3.1");
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform("AWS");
        environment.setEncryptionProfileCrn("crn:cdp:environments:us-west-1:cloudera:encryptionProfile:cdp_default_fips_v1");

        assertDoesNotThrow(() -> underTest.validateRuntimeAndImage(clusterRequest, environment, null, null));
    }

    @Test
    void testFindDetachedSdxClusterByOriginalCrnNoDetached() {
        when(sdxClusterRepository.findByAccountIdAndOriginalCrnAndDeletedIsNull(any(), eq(DATALAKE_CRN))).thenReturn(Collections.emptyList());
        Optional<SdxCluster> result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.findDetachedSdxClusterByOriginalCrn(DATALAKE_CRN));
        assertFalse(result.isPresent());
    }

    @Test
    void testFindDetachedSdxClusterByOriginalCrnMoreThanOneDetached() {
        when(sdxClusterRepository.findByAccountIdAndOriginalCrnAndDeletedIsNull(any(), eq(DATALAKE_CRN)))
                .thenReturn(List.of(createSdxCluster("first"), createSdxCluster("second")));
        Optional<SdxCluster> result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.findDetachedSdxClusterByOriginalCrn(DATALAKE_CRN));
        assertFalse(result.isPresent());
    }

    @Test
    void testFindDetachedSdxClusterByOriginalCrnOneDetached() {
        when(sdxClusterRepository.findByAccountIdAndOriginalCrnAndDeletedIsNull(any(), eq(DATALAKE_CRN)))
                .thenReturn(List.of(createSdxCluster("onlyone")));
        Optional<SdxCluster> result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.findDetachedSdxClusterByOriginalCrn(DATALAKE_CRN));
        assertTrue(result.isPresent());
        assertEquals("onlyone", result.get().getClusterName());
    }

    @Test
    void testCreateSdxClusterWhenClusterWithSpecifiedNameAlreadyExistShouldThrowClusterAlreadyExistBadRequestException() {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest(null, MEDIUM_DUTY_HA);
        SdxCluster existing = new SdxCluster();
        existing.setEnvName("envir");
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(
                anyString(), anyString())).thenReturn(Collections.singletonList(existing));
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                        underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null)));
        assertEquals("SDX cluster exists for environment name: envir", badRequestException.getMessage());
    }

    @Test
    void testCreateSdxClusterWithoutCloudStorageShouldThrownBadRequestException() {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest("7.2.1", LIGHT_DUTY);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);
        mockEnvironmentCall(sdxClusterRequest, AWS, null);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                        underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null)));
        assertEquals("Cloud storage parameter is required.", badRequestException.getMessage());
    }

    @Test
    void testCreateSdxClusterWithoutCloudStorageShouldNotThrownBadRequestExceptionInCaseOfInternal() {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest("7.2.1", CUSTOM);
        StackV4Request stackV4Request = new StackV4Request();
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        stackV4Request.setCluster(clusterV4Request);

        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        when(stackRequestHandler.getStackRequest(any(), any(), any(), any(), any(), any())).thenReturn(stackV4Request);
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);
        mockEnvironmentCall(sdxClusterRequest, AWS, null);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, stackV4Request));
    }

    @Test
    void testCreateShouldThrowExceptionWhenTheRequestContainsBasicLoadBalancerSku() {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest("7.2.1", CUSTOM);
        SdxAzureRequest azureRequest = new SdxAzureRequest();
        azureRequest.setLoadBalancerSku(LoadBalancerSku.BASIC);
        StackV4Request stackV4Request = new StackV4Request();
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        stackV4Request.setCluster(clusterV4Request);
        sdxClusterRequest.setAzure(azureRequest);

        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        when(stackRequestHandler.getStackRequest(any(), any(), any(), any(), any(), any())).thenReturn(stackV4Request);
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);
        mockEnvironmentCall(sdxClusterRequest, AZURE, null);

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
        StackV4Request stackV4Request = new StackV4Request();
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        stackV4Request.setCluster(clusterV4Request);

        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        when(stackRequestHandler.getStackRequest(any(), any(), any(), any(), any(), any())).thenReturn(stackV4Request);
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);
        mockEnvironmentCall(sdxClusterRequest, AZURE, null);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, stackV4Request));
    }

    @Test
    void testNullJavaVersionShouldNotOverrideTheVersionInTheInternalStackRequest() throws IOException, TransactionExecutionException {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest("7.2.1", CUSTOM);
        StackV4Request stackV4Request = new StackV4Request();
        stackV4Request.setJavaVersion(8);
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        stackV4Request.setCluster(clusterV4Request);

        ArgumentCaptor<SdxCluster> sdxClusterArgumentCaptor = ArgumentCaptor.forClass(SdxCluster.class);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        when(sdxClusterRepository.save(sdxClusterArgumentCaptor.capture())).thenReturn(mock(SdxCluster.class));
        when(stackRequestHandler.getStackRequest(any(), any(), any(), any(), any(), any())).thenReturn(stackV4Request);
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);
        mockEnvironmentCall(sdxClusterRequest, AWS, null);
        mockTransactionServiceRequired();

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, stackV4Request));

        StackV4Request savedStackV4Request = JsonUtil.readValue(sdxClusterArgumentCaptor.getValue().getStackRequest(), StackV4Request.class);
        assertEquals(8, savedStackV4Request.getJavaVersion());
    }

    @Test
    void testCreateNOTInternalSdxClusterFromLightDutyTemplateShouldTriggerSdxCreationFlow() throws IOException, TransactionExecutionException {
        CrnTestUtil.mockCrnGenerator(regionAwareCrnGenerator);
        mockTransactionServiceRequired();
        String lightDutyJson = readLightDutyTestTemplate();
        StackV4Request stackV4Request = JsonUtil.readValue(lightDutyJson, StackV4Request.class);
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
        when(stackRequestHandler.getStackRequest(any(), any(), any(), any(), any(), any())).thenReturn(stackV4Request);
        mockEnvironmentCall(sdxClusterRequest, AZURE, null);
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

        doCallRealMethod().when(securityConfigService).prepareDefaultSecurityConfigs(any(), any(), any());

        Pair<SdxCluster, FlowIdentifier> result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null));

        SdxCluster createdSdxCluster = result.getLeft();
        assertEquals(id, createdSdxCluster.getId());
        ArgumentCaptor<SdxCluster> captor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository, times(1)).save(captor.capture());
        SdxCluster capturedSdx = captor.getValue();
        assertEquals("tagecske", capturedSdx.getTags().getString("mytag"));
        assertEquals(CLUSTER_NAME, capturedSdx.getClusterName());
        assertEquals(LIGHT_DUTY, capturedSdx.getClusterShape());
        assertEquals("envir", capturedSdx.getEnvName());
        assertEquals(ACCOUNT_ID, capturedSdx.getAccountId());
        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(DatalakeStatusEnum.REQUESTED, "Datalake requested", createdSdxCluster);

        assertEquals(1L, capturedSdx.getCreated());
        assertFalse(capturedSdx.isCreateDatabase());
        assertTrue(createdSdxCluster.getCrn().matches("crn:cdp:datalake:us-west-1:accountId:datalake:.*"));
        StackV4Request capturedStackV4Request = JsonUtil.readValue(capturedSdx.getStackRequest(), StackV4Request.class);

        assertEquals(2L, capturedStackV4Request.getInstanceGroups().size());

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
    void testCreateNotInternalSdxClusterFromLightDutyTemplateWhenLocationSpecifiedWithSlashShouldCreateAndSettedUpBaseLocationWithOUTSlash()
            throws IOException, TransactionExecutionException {
        mockTransactionServiceRequired();
        String lightDutyJson = readLightDutyTestTemplate();
        when(stackRequestHandler.getStackRequest(any(), any(), any(), any(), any(), any())).thenReturn(JsonUtil.readValue(lightDutyJson, StackV4Request.class));
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
        when(stackRequestHandler.getStackRequest(any(), any(), any(), any(), any(), any())).thenReturn(JsonUtil.readValue(lightDutyJson, StackV4Request.class));
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
        when(stackRequestHandler.getStackRequest(any(), any(), any(), any(), any(), any())).thenReturn(JsonUtil.readValue(lightDutyJson, StackV4Request.class));
        doNothing().when(commonJavaVersionValidator).validateByVmConfiguration(any(), eq(11));
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
        doCallRealMethod().when(stackRequestHandler).setStackRequestParams(any(), any(), anyBoolean(), anyBoolean(), eq(null));

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
        when(stackRequestHandler.getStackRequest(any(), any(), any(), any(), any(), any())).thenReturn(JsonUtil.readValue(lightDutyJson, StackV4Request.class));
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

    @ParameterizedTest(name = "{0}")
    @MethodSource("cloudPlatformMultiAzDataProvider")
    void testSdxCreateRazNotRequestedAndMultiAzRequested(String testCaseName, CloudPlatform cloudPlatform, boolean multiAz)
            throws IOException, TransactionExecutionException {
        mockTransactionServiceRequired();
        String lightDutyJson = readLightDutyTestTemplate();
        when(stackRequestHandler.getStackRequest(any(), any(), any(), any(), any(), any())).thenReturn(JsonUtil.readValue(lightDutyJson, StackV4Request.class));
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
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);
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

    @Test
    void test732Redhat8Validation() {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest("7.3.2", LIGHT_DUTY);
        sdxClusterRequest.setOs("redhat8");
        withCloudStorage(sdxClusterRequest);
        StackV4Request stackV4Request = new StackV4Request();
        ImageSettingsV4Request imageSettingsV4Request = new ImageSettingsV4Request();
        imageSettingsV4Request.setOs("redhat8");
        stackV4Request.setImage(imageSettingsV4Request);
        mockEnvironmentCall(sdxClusterRequest, AWS, null);

        assertThrows(BadRequestException.class, () ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, stackV4Request)),
                "Provision is not allowed for image with runtime version 7.3.2 and OS type redhat8.");
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
    void testCreateSdxClusterFailsInCaseOfForcedJavaVersionIsNotSupportedByTheVirtualMachineConfiguration() {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest(null, MEDIUM_DUTY_HA);
        sdxClusterRequest.setJavaVersion(11);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(
                anyString(), anyString())).thenReturn(Collections.emptyList());
        doThrow(new BadRequestException("java error")).when(commonJavaVersionValidator).validateByVmConfiguration(any(), eq(11));
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);
        mockEnvironmentCall(sdxClusterRequest, AWS, null);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                        underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null)));

        assertEquals("java error", badRequestException.getMessage());
    }

    @Test
    void testCreateSdxClusterFailsInCaseOfForcedJavaVersionIsNotSupportedByRuntime() {
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest(null, MEDIUM_DUTY_HA);
        sdxClusterRequest.setJavaVersion(11);
        when(cdpConfigService.getDefaultRuntime()).thenReturn("7.3.2");
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(
                anyString(), anyString())).thenReturn(Collections.emptyList());
        doThrow(new BadRequestException("java error")).when(commonJavaVersionValidator).validateByVmConfiguration(any(), eq(11));
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);
        when(imageCatalogService.getImageResponseFromImageRequest(any(), any())).thenReturn(new ImageV4Response());
        mockEnvironmentCall(sdxClusterRequest, AWS, null);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                        underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null)));

        assertEquals("java error", badRequestException.getMessage());

        verify(commonJavaVersionValidator).validateByVmConfiguration(eq("7.3.2"), eq(11));
    }

    @Test
    void testCreateInternalSdxClusterWithCustomInstanceGroupShouldFail() {
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);
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
    void testCreateMicroDuty() throws IOException, TransactionExecutionException {
        final String runtime = "7.2.12";
        mockTransactionServiceRequired();
        String microDutyJson = FileReaderUtils.readFileFromClasspath("/duties/" + runtime + "/aws/micro_duty.json");
        when(stackRequestHandler.getStackRequest(any(), any(), any(), any(), any(), any())).thenReturn(JsonUtil.readValue(microDutyJson, StackV4Request.class));
        when(sdxReactorFlowManager.triggerSdxCreation(any())).thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest(runtime, MICRO_DUTY);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        withCloudStorage(sdxClusterRequest);
        withRecipe(sdxClusterRequest);
        long id = 10L;
        when(sdxClusterRepository.save(any(SdxCluster.class))).thenAnswer(invocation -> {
            SdxCluster sdxWithId = invocation.getArgument(0, SdxCluster.class);
            sdxWithId.setId(id);
            return sdxWithId;
        });
        when(clock.getCurrentTimeMillis()).thenReturn(1L);
        when(entitlementService.isEntitledToUseOS(any(), eq(OsType.CENTOS7))).thenReturn(true);
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

        mockEnvironmentCall(sdxClusterRequest, AWS, null);

        Pair<SdxCluster, FlowIdentifier> result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null));

        SdxCluster createdSdxCluster = result.getLeft();
        assertEquals(id, createdSdxCluster.getId());
        ArgumentCaptor<SdxCluster> captor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository, times(1)).save(captor.capture());
        SdxCluster capturedSdx = captor.getValue();
        assertEquals(MICRO_DUTY, capturedSdx.getClusterShape());
    }

    @Test
    void testSdxCreateMediumDutySdx() throws IOException, TransactionExecutionException {
        final String runtime = "7.2.7";
        mockTransactionServiceRequired();
        String mediumDutyJson = FileReaderUtils.readFileFromClasspath("/duties/" + runtime + "/aws/medium_duty_ha.json");
        when(stackRequestHandler.getStackRequest(any(), any(), any(), any(), any(), any()))
                .thenReturn(JsonUtil.readValue(mediumDutyJson, StackV4Request.class));
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
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);
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
    void testCreateEnterpriseDatalake() throws IOException, TransactionExecutionException {
        final String runtime = "7.2.17";
        mockTransactionServiceRequired();
        String enterpriseJson = FileReaderUtils.readFileFromClasspath("/duties/" + runtime + "/aws/enterprise.json");
        when(stackRequestHandler.getStackRequest(any(), any(), any(), any(), any(), any()))
                .thenReturn(JsonUtil.readValue(enterpriseJson, StackV4Request.class));
        when(sdxReactorFlowManager.triggerSdxCreation(any())).thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));
        SdxClusterRequest sdxClusterRequest = createSdxClusterRequest(runtime, ENTERPRISE);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(new ArrayList<>());
        withCloudStorage(sdxClusterRequest);
        withRecipe(sdxClusterRequest);

        long id = 10L;
        when(sdxClusterRepository.save(any(SdxCluster.class))).thenAnswer(invocation -> {
            SdxCluster sdxWithId = invocation.getArgument(0, SdxCluster.class);
            sdxWithId.setId(id);
            return sdxWithId;
        });
        when(clock.getCurrentTimeMillis()).thenReturn(1L);
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

        mockEnvironmentCall(sdxClusterRequest, AWS, null);

        Pair<SdxCluster, FlowIdentifier> result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null));

        SdxCluster createdSdxCluster = result.getLeft();
        assertEquals(id, createdSdxCluster.getId());
        ArgumentCaptor<SdxCluster> captor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository, times(1)).save(captor.capture());
        SdxCluster capturedSdx = captor.getValue();
        assertEquals(ENTERPRISE, capturedSdx.getClusterShape());
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

    private DetailedEnvironmentResponse mockEnvironmentCall(SdxClusterRequest sdxClusterRequest, CloudPlatform cloudPlatform, Tunnel tunnel) {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setName(sdxClusterRequest.getEnvironment());
        detailedEnvironmentResponse.setCloudPlatform(cloudPlatform.name());
        detailedEnvironmentResponse.setEnvironmentStatus(EnvironmentStatus.AVAILABLE);
        detailedEnvironmentResponse.setCrn(getCrn());
        detailedEnvironmentResponse.setCreator(detailedEnvironmentResponse.getCrn());
        detailedEnvironmentResponse.setAccountId(UUID.randomUUID().toString());
        detailedEnvironmentResponse.setTunnel(tunnel);
        when(environmentService.validateAndGetEnvironment(anyString())).thenReturn(detailedEnvironmentResponse);
        return detailedEnvironmentResponse;
    }

    private String getCrn() {
        return CrnTestUtil.getEnvironmentCrnBuilder()
                .setResource(UUID.randomUUID().toString())
                .setAccountId(UUID.randomUUID().toString())
                .build().toString();
    }

    private SdxCluster createSdxCluster(String name) {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName(name);
        return sdxCluster;
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

    private void mockTransactionServiceRequired() throws TransactionExecutionException {
        when(transactionService.required(isA(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());
    }

    private InstanceGroupV4Request getGroup(StackV4Request stack, com.sequenceiq.common.api.type.InstanceGroupType type) {
        for (InstanceGroupV4Request instanceGroup : stack.getInstanceGroups()) {
            if (instanceGroup.getType().equals(type)) {
                return instanceGroup;
            }
        }
        return null;
    }

    private String readLightDutyTestTemplate() throws IOException {
        return FileReaderUtils.readFileFromClasspath("/duties/7.2.17/aws/light_duty.json");
    }

    private void withCloudStorage(SdxClusterRequest sdxClusterRequest) {
        SdxCloudStorageRequest cloudStorage = new SdxCloudStorageRequest();
        cloudStorage.setFileSystemType(FileSystemType.S3);
        cloudStorage.setBaseLocation("s3a://some/dir/");
        cloudStorage.setS3(new S3CloudStorageV1Parameters());
        sdxClusterRequest.setCloudStorage(cloudStorage);
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

    private void withRecipe(SdxClusterRequest sdxClusterRequest) {
        SdxRecipe recipe = new SdxRecipe();
        recipe.setHostGroup("master");
        recipe.setName("post-service-deployment");
        sdxClusterRequest.setRecipes(Set.of(recipe));
    }
}