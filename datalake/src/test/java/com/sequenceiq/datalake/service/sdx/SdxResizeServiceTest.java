package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.GCP;
import static com.sequenceiq.datalake.service.sdx.SdxResizeService.PREVIOUS_CLUSTER_SHAPE;
import static com.sequenceiq.datalake.service.sdx.SdxResizeService.PREVIOUS_DATABASE_CRN;
import static com.sequenceiq.datalake.service.sdx.SdxService.DATABASE_SSL_ENABLED;
import static com.sequenceiq.datalake.service.sdx.SdxTestUtil.CLUSTER_NAME;
import static com.sequenceiq.datalake.service.sdx.SdxTestUtil.ENVIRONMENT_CRN;
import static com.sequenceiq.datalake.service.sdx.SdxTestUtil.ENVIRONMENT_NAME;
import static com.sequenceiq.datalake.service.sdx.SdxTestUtil.SDX_ID;
import static com.sequenceiq.datalake.service.sdx.SdxTestUtil.USER_CRN;
import static com.sequenceiq.datalake.service.sdx.SdxTestUtil.getSdxCluster;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.ENTERPRISE;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.LIGHT_DUTY;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.MEDIUM_DUTY_HA;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.RecipeV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4Response;
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
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
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
import com.sequenceiq.cloudbreak.auth.crn.AccountIdService;
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
import com.sequenceiq.cloudbreak.datalakedr.DatalakeDrSkipOptions;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.datalake.configuration.CDPConfigService;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.converter.NetworkV4ResponseToNetworkV4RequestConverter;
import com.sequenceiq.datalake.service.sdx.dr.SdxBackupRestoreService;
import com.sequenceiq.datalake.service.validation.resize.SdxResizeValidator;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.sdx.api.model.SdxClusterResizeRequest;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseComputeStorageRequest;
import com.sequenceiq.sdx.api.model.SdxInstanceGroupDiskRequest;
import com.sequenceiq.sdx.api.model.SdxInstanceGroupRequest;

@ExtendWith(MockitoExtension.class)
class SdxResizeServiceTest {
    private static final String DATABASE_CRN = "crn:cdp:database:us-west-1:default:datalake:e438a2db-d650-4132-ae62-242c5ba2f784";

    private static final String OS_NAME = "rhel";

    private static final String CATALOG_NAME = "cdp_default";

    private static final String ACCOUNT_ID = "accountId";

    @Mock
    private Clock clock;

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private MultiAzDecorator multiAzDecorator;

    @Mock
    private SdxResizeValidator sdxResizeValidator;

    @Mock
    private StackRequestHandler stackRequestHandler;

    @Mock
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Mock
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Mock
    private CloudStorageManifester cloudStorageManifester;

    @Mock
    private ShapeValidator shapeValidator;

    @Mock
    private NetworkV4ResponseToNetworkV4RequestConverter networkV4ResponseToNetworkV4RequestConverter;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private CDPConfigService cdpConfigService;

    @Mock
    private StackService stackService;

    @Mock
    private SdxBackupRestoreService sdxBackupRestoreService;

    @Mock
    private SdxRecommendationService sdxRecommendationService;

    @Mock
    private SdxInstanceService sdxInstanceService;

    @Mock
    private RangerRazService rangerRazService;

    @Mock
    private RangerRmsService rangerRmsService;

    @Mock
    private SecurityConfigService securityConfigService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private AccountIdService accountIdService;

    @InjectMocks
    private SdxResizeService underTest;

    @BeforeEach
    void initMocks() {
        lenient().when(entitlementService.isEntitledToUseOS(any(), eq(OsType.CENTOS7))).thenReturn(true);
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
        when(stackRequestHandler.getStackRequest(any(), any(), any(), any(), any(), any()))
                .thenReturn(JsonUtil.readValue(mediumDutyJson, StackV4Request.class));
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.STOPPED);
        StackImageV4Response image = new StackImageV4Response();
        image.setOs(OS_NAME);
        image.setCatalogName(CATALOG_NAME);
        stackV4Response.setImage(image);
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        stackV4Response.setCluster(clusterV4Response);
        stackV4Response.setNetwork(getNetworkForCurrentDatalake());
        when(stackService.getDetail(anyString(), anySet(), anyString())).thenReturn(stackV4Response);
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

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
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

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
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

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
        when(stackRequestHandler.getStackRequest(any(), any(), any(), any(), any(), any()))
                .thenReturn(JsonUtil.readValue(mediumDutyJson, StackV4Request.class));
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

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
        when(stackService.getDetail(anyString(), anySet(), anyString())).thenReturn(stackV4Response);

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
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

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
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

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
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

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
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

        mockEnvironmentCall(sdxClusterResizeRequest, AWS);
        when(sdxReactorFlowManager.triggerSdxResize(anyLong(), any(SdxCluster.class), any(DatalakeDrSkipOptions.class), eq(false)))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));

        String mediumDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.10/aws/medium_duty_ha.json");
        when(stackRequestHandler.getStackRequest(any(), any(), any(), any(), any(), any()))
                .thenReturn(JsonUtil.readValue(mediumDutyJson, StackV4Request.class));
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.STOPPED);
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        stackV4Response.setCluster(clusterV4Response);
        stackV4Response.setNetwork(getNetworkForCurrentDatalake());
        when(stackService.getDetail(anyString(), anySet(), anyString())).thenReturn(stackV4Response);

        assertDoesNotThrow(
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.resizeSdx(USER_CRN, "sdxcluster", sdxClusterResizeRequest)));
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
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);
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
        when(stackRequestHandler.getStackRequest(any(), any(), any(), any(), any(), any()))
                .thenReturn(JsonUtil.readValue(mediumDutyJson, StackV4Request.class));
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.STOPPED);
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        stackV4Response.setCluster(clusterV4Response);
        stackV4Response.setNetwork(getNetworkForCurrentDatalake());
        when(stackService.getDetail(anyString(), anySet(), anyString())).thenReturn(stackV4Response);
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
        when(environmentService.validateAndGetEnvironment(anyString())).thenReturn(detailedEnvironmentResponse);
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

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
        when(stackRequestHandler.getStackRequest(any(), any(), any(), any(), any(), any()))
                .thenReturn(JsonUtil.readValue(mediumDutyJson, StackV4Request.class));
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.STOPPED);
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        stackV4Response.setCluster(clusterV4Response);
        stackV4Response.setNetwork(getNetworkForCurrentDatalake());
        when(stackService.getDetail(anyString(), anySet(), anyString())).thenReturn(stackV4Response);
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

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
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

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
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

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
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

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
        when(stackRequestHandler.getStackRequest(any(), any(), any(), any(), any(), any()))
                .thenReturn(JsonUtil.readValue(mediumDutyJson, StackV4Request.class));
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
        when(stackService.getDetail(anyString(), anySet(), anyString())).thenReturn(stackV4Response);
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

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
        when(stackRequestHandler.getStackRequest(any(), any(), any(), any(), any(), any()))
                .thenReturn(JsonUtil.readValue(mediumDutyJson, StackV4Request.class));
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.STOPPED);
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        stackV4Response.setCluster(clusterV4Response);
        stackV4Response.setNetwork(getNetworkForCurrentDatalake());
        when(stackService.getDetail(anyString(), anySet(), anyString())).thenReturn(stackV4Response);
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);
        doCallRealMethod().when(sdxInstanceService).overrideDefaultInstanceType(any(), any(), any(), any(), any());
        doCallRealMethod().when(sdxInstanceService).overrideDefaultInstanceStorage(any(), any(), any(), any());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.resizeSdx(USER_CRN, sdxCluster.getClusterName(), resizeRequest));

        StackV4Request stackV4Request = JsonUtil.readValue(captorResize.getValue().getStackRequest(), StackV4Request.class);
        InstanceGroupV4Request idbrokerInstGroup = stackV4Request.getInstanceGroups().stream().filter(ig -> "idbroker".equals(ig.getName())).findAny().get();
        InstanceGroupV4Request masterInstGroup = stackV4Request.getInstanceGroups().stream().filter(ig -> "master".equals(ig.getName())).findAny().get();
        assertEquals("m5.xlarge", idbrokerInstGroup.getTemplate().getInstanceType());
        assertEquals(256, masterInstGroup.getTemplate().getAttachedVolumes().stream().findAny().get().getSize());
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
        when(stackRequestHandler.getStackRequest(any(), any(), any(), any(), any(), any()))
                .thenReturn(JsonUtil.readValue(mediumDutyJson, StackV4Request.class));
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.STOPPED);
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        stackV4Response.setCluster(clusterV4Response);
        stackV4Response.setNetwork(getNetworkForCurrentDatalake());
        when(stackService.getDetail(anyString(), anySet(), anyString())).thenReturn(stackV4Response);
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.resizeSdx(USER_CRN, sdxCluster.getClusterName(), resizeRequest));

        SdxDatabase sdxDatabase = captorResize.getValue().getSdxDatabase();
        Map<String, Object> attributes = sdxDatabase.getAttributes().getMap();
        assertThat(attributes).containsEntry("instancetype", "customInstance");
        assertThat(attributes).containsEntry("storage", "128");
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
        when(stackRequestHandler.getStackRequest(eq(ENTERPRISE), any(), any(), any(), any(), any()))
                .thenReturn(JsonUtil.readValue(enterpriseJson, StackV4Request.class));
        when(stackService.getDetail(anyString(), anySet(), anyString())).thenReturn(stackV4Response);
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);
        doCallRealMethod().when(sdxInstanceService).overrideDefaultInstanceType(any(), any(), any(), any(), any());
        doCallRealMethod().when(sdxInstanceService).overrideDefaultInstanceStorage(any(), any(), any(), any());

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
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(eq(ACCOUNT_ID), anyString()))
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
        when(stackRequestHandler.getStackRequest(eq(ENTERPRISE), any(), any(), any(), any(), any()))
                .thenReturn(JsonUtil.readValue(enterpriseJson, StackV4Request.class));
        when(stackService.getDetail(anyString(), anySet(), anyString())).thenReturn(stackV4Response);
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

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
        when(stackRequestHandler.getStackRequest(any(), any(), any(), any(), any(), any()))
                .thenReturn(JsonUtil.readValue(mediumDutyJson, StackV4Request.class));
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setCluster(new ClusterV4Response());
        stackV4Response.getCluster().setDbSSLEnabled(false);
        stackV4Response.setStatus(Status.STOPPED);
        stackV4Response.setNetwork(getNetworkForCurrentDatalake());
        when(stackService.getDetail(anyString(), anySet(), anyString())).thenReturn(stackV4Response);
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

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
        when(environmentService.validateAndGetEnvironment(eq(ENVIRONMENT_NAME)))
                .thenReturn(detailedEnvironmentResponse);
        when(stackService.getDetail(eq(CLUSTER_NAME), anySet(), eq(accountId)))
                .thenReturn(stackV4Response);
        when(stackRequestHandler.getStackRequest(any(), any(), any(), any(), any(), any()))
                .thenReturn(JsonUtil.readValue(enterpriseDutyJson, StackV4Request.class));
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(accountId);

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

        assertEquals(CLUSTER_NAME + ENTERPRISE.getResizeSuffix(), stackV4Request.getCustomDomain().getHostname());
        assertEquals("RHEL8", stackV4Request.getImage().getOs());
        assertEquals("cb-default", stackV4Request.getImage().getCatalog());
        assertEquals("random-uuid-id", stackV4Request.getImage().getId());

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
        when(environmentService.validateAndGetEnvironment(eq(ENVIRONMENT_NAME)))
                .thenReturn(detailedEnvironmentResponse);
        when(stackService.getDetail(eq(CLUSTER_NAME), anySet(), eq(accountId)))
                .thenReturn(stackV4Response);
        when(stackRequestHandler.getStackRequest(any(), any(), any(), any(), any(), any()))
                .thenReturn(JsonUtil.readValue(enterpriseDutyJson, StackV4Request.class));
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(accountId);

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

        assertEquals(CLUSTER_NAME + ENTERPRISE.getResizeSuffix(), stackV4Request.getCustomDomain().getHostname());
        assertEquals("RHEL8", stackV4Request.getImage().getOs());
        assertEquals("cb-default", stackV4Request.getImage().getCatalog());
        assertEquals("random-uuid-id", stackV4Request.getImage().getId());
        ClusterV4Request cluster = stackV4Request.getCluster();

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

    // TODO shape
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
    // end shape

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
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(eq(ACCOUNT_ID), anyString()))
                .thenReturn(Optional.of(sdxCluster));
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(eq(ACCOUNT_ID), anyString())).thenReturn(Optional.empty());
        when(sdxBackupRestoreService.isDatalakeInBackupProgress(anyString(), anyString())).thenReturn(false);
        when(sdxBackupRestoreService.isDatalakeInRestoreProgress(anyString(), anyString())).thenReturn(false);

        mockEnvironmentCall(resizeRequest, AWS);
        when(sdxReactorFlowManager.triggerSdxResize(anyLong(), any(SdxCluster.class), any(DatalakeDrSkipOptions.class), eq(false)))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));

        String mediumDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.18/aws/enterprise.json");
        when(stackRequestHandler.getStackRequest(any(), any(), any(), any(), any(), any()))
                .thenReturn(JsonUtil.readValue(mediumDutyJson, StackV4Request.class));
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

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

        when(stackService.getDetail(anyString(), anySet(), anyString())).thenReturn(stackV4Response);
        doCallRealMethod().when(multiAzDecorator).decorateRequestWithMultiAz(any(), any(), any(), any(), eq(true));

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
        when(stackRequestHandler.getStackRequest(eq(ENTERPRISE), any(), any(), any(), any(), any()))
                .thenReturn(JsonUtil.readValue(mediumDutyJson, StackV4Request.class));
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

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

        when(stackService.getDetail(anyString(), anySet(), anyString())).thenReturn(stackV4Response);
        doCallRealMethod().when(multiAzDecorator).decorateRequestWithMultiAz(any(), any(), any(), any(), eq(true));

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
        when(accountIdService.getAccountIdFromUserCrn(any())).thenReturn(ACCOUNT_ID);

        mockEnvironmentCall(resizeRequest, AWS);
        when(sdxReactorFlowManager.triggerSdxResize(anyLong(), any(SdxCluster.class), any(DatalakeDrSkipOptions.class), eq(false)))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));

        String mediumDutyJson = FileReaderUtils.readFileFromClasspath("/duties/7.2.18/aws/enterprise.json");
        when(stackRequestHandler.getStackRequest(eq(ENTERPRISE), any(), any(), any(), any(), any()))
                .thenReturn(JsonUtil.readValue(mediumDutyJson, StackV4Request.class));

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

        when(stackService.getDetail(anyString(), anySet(), anyString())).thenReturn(stackV4Response);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.resizeSdx(USER_CRN, CLUSTER_NAME, resizeRequest));

        Map<String, Set<String>> subnetsByAz = Map.of("az1", Set.of("subnet1", "subnet2", "subnet3"));
        verify(multiAzDecorator, never()).decorateStackRequestWithPreviousNetwork(any(), any(), eq(subnetsByAz));
    }

    private void mockEnvironmentCall(SdxClusterResizeRequest sdxClusterResizeRequest, CloudPlatform cloudPlatform) {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setName(sdxClusterResizeRequest.getEnvironment());
        detailedEnvironmentResponse.setCloudPlatform(cloudPlatform.name());
        detailedEnvironmentResponse.setEnvironmentStatus(EnvironmentStatus.AVAILABLE);
        detailedEnvironmentResponse.setCrn(getCrn());
        detailedEnvironmentResponse.setCreator(detailedEnvironmentResponse.getCrn());
        when(environmentService.validateAndGetEnvironment(anyString())).thenReturn(detailedEnvironmentResponse);
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

    private String getCrn() {
        return CrnTestUtil.getEnvironmentCrnBuilder()
                .setResource(UUID.randomUUID().toString())
                .setAccountId(UUID.randomUUID().toString())
                .build().toString();
    }

    private DetailedEnvironmentResponse getDetailedEnvironmentResponse() {
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCreator(USER_CRN);
        environmentResponse.setCloudPlatform("AWS");
        environmentResponse.setEnvironmentStatus(EnvironmentStatus.AVAILABLE);
        return environmentResponse;
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
}