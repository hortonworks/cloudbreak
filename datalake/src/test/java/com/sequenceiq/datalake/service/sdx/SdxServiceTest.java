package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.GCP;
import static com.sequenceiq.datalake.service.sdx.SdxService.MEDIUM_DUTY_REQUIRED_VERSION;
import static com.sequenceiq.datalake.service.sdx.SdxService.WORKSPACE_ID_DEFAULT;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.ENTERPRISE;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.LIGHT_DUTY;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.MEDIUM_DUTY_HA;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.BaseStackDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.RotateSaltPasswordRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.RangerRazEnabledV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerProductV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.StackImageV4Response;
import com.sequenceiq.cloudbreak.api.model.RotateSaltPasswordReason;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.datalakedr.DatalakeDrSkipOptions;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.vm.VirtualMachineConfiguration;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.common.model.ImageCatalogPlatform;
import com.sequenceiq.datalake.configuration.CDPConfigService;
import com.sequenceiq.datalake.configuration.PlatformConfig;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.repository.SdxDatabaseRepository;
import com.sequenceiq.datalake.service.EnvironmentClientService;
import com.sequenceiq.datalake.service.imagecatalog.ImageCatalogService;
import com.sequenceiq.datalake.service.sdx.dr.SdxBackupRestoreService;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.sdx.api.model.SdxAwsRequest;
import com.sequenceiq.sdx.api.model.SdxCloudStorageRequest;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterResizeRequest;
import com.sequenceiq.sdx.api.model.SdxInstanceGroupDiskRequest;
import com.sequenceiq.sdx.api.model.SdxInstanceGroupRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("SDX service tests")
class SdxServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:hortonworks:user:perdos@hortonworks.com";

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:default:environment:e438a2db-d650-4132-ae62-242c5ba2f784";

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:default:datalake:e438a2db-d650-4132-ae62-242c5ba2f784";

    private static final Long SDX_ID = 2L;

    private static final String SDX_CRN = "crn";

    private static final String CLUSTER_NAME = "test-sdx-cluster";

    private static final String OS_NAME = "rhel";

    private static final String CATALOG_NAME = "cdp_default";

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Mock
    private EnvironmentClientService environmentClientService;

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
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

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
        lenient().when(platformConfig.getRazSupportedPlatforms())
                .thenReturn(List.of(AWS, AZURE, GCP));
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
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
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
        sdxCluster.setEnvName("envir");
        sdxCluster.setClusterName("sdx-cluster-name");
        sdxCluster.setSdxDatabase(new SdxDatabase());
        return sdxCluster;
    }

    @Test
    void testSdxResizeToEDL() throws IOException {
        SdxClusterResizeRequest resizeRequest = new SdxClusterResizeRequest();
        resizeRequest.setEnvironment("environment");
        resizeRequest.setClusterShape(ENTERPRISE);
        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setId(1L);
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
        when(sdxReactorFlowManager.triggerSdxResize(anyLong(), any(SdxCluster.class), any(DatalakeDrSkipOptions.class)))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));

        String mediumDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.10/aws/medium_duty_ha.json");
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(mediumDutyJson, StackV4Request.class));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.STOPPED);
        StackImageV4Response image = new StackImageV4Response();
        image.setOs(OS_NAME);
        image.setCatalogName(CATALOG_NAME);
        stackV4Response.setImage(image);
        when(stackV4Endpoint.get(anyLong(), anyString(), anySet(), anyString())).thenReturn(stackV4Response);

        Pair<SdxCluster, FlowIdentifier> result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.resizeSdx(USER_CRN, sdxCluster.getClusterName(), resizeRequest));
        SdxCluster createdSdxCluster = result.getLeft();
        assertEquals(sdxCluster.getClusterName(), createdSdxCluster.getClusterName());
        assertEquals("7.2.17", createdSdxCluster.getRuntime());
        assertEquals("s3a://some/dir/", createdSdxCluster.getCloudStorageBaseLocation());
        assertEquals("envir", createdSdxCluster.getEnvName());
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
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> underTest.resizeSdx(USER_CRN, "sdxcluster",
                sdxClusterResizeRequest));
        assertEquals("SDX cluster already is of requested shape", badRequestException.getMessage());
    }

    @Test
    void testSdxResizeGcpClusterSuccess() throws IOException {
        final String runtime = "7.2.15";
        SdxClusterResizeRequest sdxClusterResizeRequest = new SdxClusterResizeRequest();
        sdxClusterResizeRequest.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setEnvironment("environment");

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setId(1L);
        sdxCluster.setCloudStorageFileSystemType(FileSystemType.GCS);
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);
        sdxCluster.setRuntime(runtime);
        sdxCluster.setCloudStorageBaseLocation("gcs://some/dir/");

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(sdxBackupRestoreService.isDatalakeInBackupProgress(anyString(), anyString())).thenReturn(false);
        when(sdxBackupRestoreService.isDatalakeInRestoreProgress(anyString(), anyString())).thenReturn(false);

        mockEnvironmentCall(sdxClusterResizeRequest, GCP);
        when(sdxReactorFlowManager.triggerSdxResize(anyLong(), any(SdxCluster.class), any(DatalakeDrSkipOptions.class)))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));

        String mediumDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.15/gcp/medium_duty_ha.json");
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
        assertEquals("gcs://some/dir/", createdSdxCluster.getCloudStorageBaseLocation());
        assertEquals("envir", createdSdxCluster.getEnvName());

    }

    @Test
    void testSdxResizeByAccountIdAndNameWhenSdxWithExistingDetachedSdx() {
        SdxClusterResizeRequest sdxClusterResizeRequest = new SdxClusterResizeRequest();
        sdxClusterResizeRequest.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setEnvironment("environment");

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
        sdxClusterResizeRequest.setEnvironment("environment");

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
        sdxClusterResizeRequest.setEnvironment("environment");

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setId(1L);
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);
        sdxCluster.setRuntime(runtime);
        sdxCluster.setCloudStorageBaseLocation("s3a://some/dir/");

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(sdxBackupRestoreService.isDatalakeInBackupProgress(anyString(), anyString())).thenReturn(false);
        when(sdxBackupRestoreService.isDatalakeInRestoreProgress(anyString(), anyString())).thenReturn(false);

        mockEnvironmentCall(sdxClusterResizeRequest, CloudPlatform.AWS);
        when(sdxReactorFlowManager.triggerSdxResize(anyLong(), any(SdxCluster.class), any(DatalakeDrSkipOptions.class)))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));

        String mediumDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.10/aws/medium_duty_ha.json");
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(mediumDutyJson, StackV4Request.class));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.STOPPED);
        when(stackV4Endpoint.get(anyLong(), anyString(), anySet(), anyString())).thenReturn(stackV4Response);

        assertDoesNotThrow(
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.resizeSdx(USER_CRN, "sdxcluster", sdxClusterResizeRequest)));
    }

    @ParameterizedTest
    @MethodSource("deleteInProgressParamProvider")
    void testSdxResizeButEnvInDeleteInProgressPhase(EnvironmentStatus environmentStatus) {

        SdxClusterResizeRequest sdxClusterResizeRequest = new SdxClusterResizeRequest();
        sdxClusterResizeRequest.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setEnvironment("environment");

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(sdxBackupRestoreService.isDatalakeInBackupProgress(anyString(), anyString())).thenReturn(false);
        when(sdxBackupRestoreService.isDatalakeInRestoreProgress(anyString(), anyString())).thenReturn(false);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
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

    @ParameterizedTest
    @MethodSource("failedParamProvider")
    void testSdxResizeButEnvInFailedPhase(EnvironmentStatus environmentStatus) {
        SdxClusterResizeRequest sdxClusterResizeRequest = new SdxClusterResizeRequest();
        sdxClusterResizeRequest.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setEnvironment("environment");

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(sdxBackupRestoreService.isDatalakeInBackupProgress(anyString(), anyString())).thenReturn(false);
        when(sdxBackupRestoreService.isDatalakeInRestoreProgress(anyString(), anyString())).thenReturn(false);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
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
        sdxClusterResizeRequest.setEnvironment("environment");

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(sdxBackupRestoreService.isDatalakeInBackupProgress(anyString(), anyString())).thenReturn(false);
        when(sdxBackupRestoreService.isDatalakeInRestoreProgress(anyString(), anyString())).thenReturn(false);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
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
    void testSdxResizeMediumDutySdxEnabled710Runtime() {
        final String invalidRuntime = "7.1.0";
        SdxClusterResizeRequest sdxClusterResizeRequest = new SdxClusterResizeRequest();
        sdxClusterResizeRequest.setClusterShape(MEDIUM_DUTY_HA);
        sdxClusterResizeRequest.setEnvironment("environment");

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);
        sdxCluster.setRuntime(invalidRuntime);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(sdxBackupRestoreService.isDatalakeInBackupProgress(anyString(), anyString())).thenReturn(false);
        when(sdxBackupRestoreService.isDatalakeInRestoreProgress(anyString(), anyString())).thenReturn(false);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
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
        sdxClusterResizeRequest.setEnvironment("environment");

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);
        sdxCluster.setRuntime(invalidRuntime);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(sdxBackupRestoreService.isDatalakeInBackupProgress(anyString(), anyString())).thenReturn(false);
        when(sdxBackupRestoreService.isDatalakeInRestoreProgress(anyString(), anyString())).thenReturn(false);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
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
        sdxClusterResizeRequest.setEnvironment("environment");

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(sdxBackupRestoreService.isDatalakeInBackupProgress(anyString(), anyString())).thenReturn(false);
        when(sdxBackupRestoreService.isDatalakeInRestoreProgress(anyString(), anyString())).thenReturn(false);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
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
        sdxClusterResizeRequest.setEnvironment("environment");

        SdxCluster sdxCluster = getSdxCluster();
        sdxCluster.setId(1L);
        sdxCluster.setClusterShape(LIGHT_DUTY);
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);
        sdxCluster.setRuntime(runtime);
        sdxCluster.setCloudStorageBaseLocation("s3a://some/dir/");

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(sdxBackupRestoreService.isDatalakeInBackupProgress(anyString(), anyString())).thenReturn(false);
        when(sdxBackupRestoreService.isDatalakeInRestoreProgress(anyString(), anyString())).thenReturn(false);

        mockEnvironmentCall(sdxClusterResizeRequest, AWS);
        when(sdxReactorFlowManager.triggerSdxResize(anyLong(), any(SdxCluster.class), any(DatalakeDrSkipOptions.class)))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));

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
        sdxCluster.getSdxDatabase().setDatabaseCrn(null);
        sdxCluster.setRuntime(runtime);
        sdxCluster.setCloudStorageBaseLocation("s3a://some/dir/");

        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.resizeSdx(USER_CRN, "sdxcluster", sdxClusterResizeRequest)));
        assertEquals("Resizing of the data lake is not supported", badRequestException.getMessage());
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
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        FlowIdentifier cbFlowIdentifier = mock(FlowIdentifier.class);
        RotateSaltPasswordRequest request = new RotateSaltPasswordRequest(RotateSaltPasswordReason.MANUAL);
        when(stackV4Endpoint.rotateSaltPasswordInternal(WORKSPACE_ID_DEFAULT, SDX_CRN, request, USER_CRN)).thenReturn(cbFlowIdentifier);
        FlowIdentifier sdxFlowIdentifier = mock(FlowIdentifier.class);
        when(sdxReactorFlowManager.triggerSaltPasswordRotationTracker(sdxCluster)).thenReturn(sdxFlowIdentifier);

        FlowIdentifier result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.rotateSaltPassword(sdxCluster, RotateSaltPasswordReason.MANUAL));

        assertEquals(sdxFlowIdentifier, result);
        verify(stackV4Endpoint).rotateSaltPasswordInternal(WORKSPACE_ID_DEFAULT, SDX_CRN, request, USER_CRN);
        verify(regionAwareInternalCrnGenerator).getInternalCrnForServiceAsString();
        verify(cloudbreakFlowService).saveLastCloudbreakFlowChainId(sdxCluster, cbFlowIdentifier);
        verify(sdxReactorFlowManager).triggerSaltPasswordRotationTracker(sdxCluster);
    }

    @Test
    void modifyProxyConfig() {
        SdxCluster sdxCluster = getSdxCluster();
        String previousProxyConfigCrn = "previous-proxy-crn";
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        FlowIdentifier cbFlowIdentifier = mock(FlowIdentifier.class);
        when(stackV4Endpoint.modifyProxyConfigInternal(WORKSPACE_ID_DEFAULT, SDX_CRN, previousProxyConfigCrn, USER_CRN)).thenReturn(cbFlowIdentifier);
        FlowIdentifier sdxFlowIdentifier = mock(FlowIdentifier.class);
        when(sdxReactorFlowManager.triggerModifyProxyConfigTracker(sdxCluster)).thenReturn(sdxFlowIdentifier);

        FlowIdentifier result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.modifyProxyConfig(sdxCluster, previousProxyConfigCrn));

        assertEquals(sdxFlowIdentifier, result);
        verify(stackV4Endpoint).modifyProxyConfigInternal(WORKSPACE_ID_DEFAULT, SDX_CRN, previousProxyConfigCrn, USER_CRN);
        verify(regionAwareInternalCrnGenerator).getInternalCrnForServiceAsString();
        verify(cloudbreakFlowService).saveLastCloudbreakFlowChainId(sdxCluster, cbFlowIdentifier);
        verify(sdxReactorFlowManager).triggerModifyProxyConfigTracker(sdxCluster);
    }

    @Test
    void refreshDatahubsWithoutName() {
        SdxCluster sdxCluster = getSdxCluster();
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(anyString(), anyString()))
                .thenReturn(Optional.of(sdxCluster));
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.refreshDataHub(CLUSTER_NAME, null));
        verify(distroxService, times(1)).restartAttachedDistroxClusters(sdxCluster.getEnvCrn());
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
        verify(distroxService, times(1)).restartDistroxByCrns(any());
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
        assertEquals(String.format("SaltStack update cannot be initiated as datalake 'sdx-cluster-name' is currently in '%s' state.", status), ex.getMessage());
    }

    @Test
    public void testAddRmsToSdxCluster() throws IOException, TransactionExecutionException {
        DetailedEnvironmentResponse environmentResponse = getDetailedEnvironmentResponse();
        when(entitlementService.isRmsEnabledOnDatalake(any())).thenReturn(true);
        when(virtualMachineConfiguration.getSupportedJavaVersions()).thenReturn(Set.of(11, 13, 17, 18));
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
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createSdx(USER_CRN, "name", sdxClusterRequest, null));

        verify(virtualMachineConfiguration, times(1)).getSupportedJavaVersions();
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
        resizeRequest.setEnvironment("environment");
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

        when(entitlementService.isDatalakeLightToMediumMigrationEnabled(anyString())).thenReturn(true);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(anyString(), anyString()))
                .thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(anyString(), anyString())).thenReturn(Optional.empty());
        when(sdxBackupRestoreService.isDatalakeInBackupProgress(anyString(), anyString())).thenReturn(false);
        when(sdxBackupRestoreService.isDatalakeInRestoreProgress(anyString(), anyString())).thenReturn(false);

        mockEnvironmentCall(resizeRequest, AWS);
        ArgumentCaptor<SdxCluster> captorResize = ArgumentCaptor.forClass(SdxCluster.class);
        when(sdxReactorFlowManager.triggerSdxResize(anyLong(), captorResize.capture(), any(DatalakeDrSkipOptions.class)))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));

        String mediumDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.10/aws/medium_duty_ha.json");
        when(cdpConfigService.getConfigForKey(any())).thenReturn(JsonUtil.readValue(mediumDutyJson, StackV4Request.class));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.STOPPED);
        when(stackV4Endpoint.get(anyLong(), anyString(), anySet(), anyString())).thenReturn(stackV4Response);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.resizeSdx(USER_CRN, sdxCluster.getClusterName(), resizeRequest));

        StackV4Request stackV4Request = JsonUtil.readValue(captorResize.getValue().getStackRequest(), StackV4Request.class);
        InstanceGroupV4Request idbrokerInstGroup = stackV4Request.getInstanceGroups().stream().filter(ig -> "idbroker".equals(ig.getName())).findAny().get();
        InstanceGroupV4Request masterInstGroup = stackV4Request.getInstanceGroups().stream().filter(ig -> "master".equals(ig.getName())).findAny().get();
        assertEquals("m5.xlarge", idbrokerInstGroup.getTemplate().getInstanceType());
        assertEquals(256, masterInstGroup.getTemplate().getAttachedVolumes().stream().findAny().get().getSize());
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
}
