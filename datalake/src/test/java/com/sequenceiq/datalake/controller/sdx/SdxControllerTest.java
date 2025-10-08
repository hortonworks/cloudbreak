package com.sequenceiq.datalake.controller.sdx;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_DATALAKE;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackAddVolumesRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.BackupOperationType;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.datalake.authorization.DataLakeFiltering;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.metric.SdxMetricService;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.DistroxService;
import com.sequenceiq.datalake.service.sdx.RangerRazService;
import com.sequenceiq.datalake.service.sdx.SELinuxService;
import com.sequenceiq.datalake.service.sdx.SaltService;
import com.sequenceiq.datalake.service.sdx.SdxImageCatalogService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.StackService;
import com.sequenceiq.datalake.service.sdx.StorageValidationService;
import com.sequenceiq.datalake.service.sdx.VerticalScaleService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.sdx.api.model.SdxBackupLocationValidationRequest;
import com.sequenceiq.sdx.api.model.SdxChangeImageCatalogRequest;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterResizeRequest;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAzureRequest;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;
import com.sequenceiq.sdx.api.model.SdxGenerateImageCatalogResponse;

@ExtendWith(MockitoExtension.class)
class SdxControllerTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:hortonworks:user:test@test.com";

    private static final String SDX_CLUSTER_NAME = "test-sdx-cluster";

    private static final String BACKUP_LOCATION = "abfs://backup@location/to/backup";

    @Mock
    private SdxStatusService sdxStatusService;

    @Spy
    private SdxClusterConverter sdxClusterConverter;

    @Mock
    private SdxService sdxService;

    @Mock
    private SdxMetricService metricService;

    @Mock
    private SdxImageCatalogService sdxImageCatalogService;

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private DistroxService distroxService;

    @Mock
    private DistroXV1Endpoint distroXV1Endpoint;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private StorageValidationService storageValidationService;

    @Mock
    private VerticalScaleService verticalScaleService;

    @Mock
    private DataLakeFiltering dataLakeFiltering;

    @Mock
    private SELinuxService seLinuxService;

    @Mock
    private StackService stackService;

    @Mock
    private RangerRazService rangerRazService;

    @Mock
    private SaltService saltService;

    @InjectMocks
    private SdxController sdxController;

    @Test
    void createTest() {
        SdxCluster sdxCluster = createSdxCluster();
        SdxClusterRequest createSdxClusterRequest = createSdxClusterRequest(sdxCluster);

        SdxClusterResponse sdxClusterResponse = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> sdxController.create(SDX_CLUSTER_NAME, createSdxClusterRequest));
        verifySdxClusterCreation(createSdxClusterRequest, sdxCluster, sdxClusterResponse);
    }

    @Test
    void createTestWithSingleServerWhenSingleServerRejectEnabled() {
        SdxCluster sdxCluster = createSdxCluster();
        SdxClusterRequest createSdxClusterRequest = createSdxClusterRequest(sdxCluster);
        addDatabaseAzureRequest(createSdxClusterRequest, AzureDatabaseType.SINGLE_SERVER, true);

        assertThatThrownBy(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN,
                        () -> sdxController.create(SDX_CLUSTER_NAME, createSdxClusterRequest)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Azure Database for PostgreSQL - Single Server is retired. New deployments cannot be created anymore. " +
                        "Check documentation for more information: " +
                        "https://learn.microsoft.com/en-us/azure/postgresql/migrate/whats-happening-to-postgresql-single-server");
    }

    @Test
    void createTestWithSingleServerWhenSingleServerRejectDisabled() {
        SdxCluster sdxCluster = createSdxCluster();
        SdxClusterRequest createSdxClusterRequest = createSdxClusterRequest(sdxCluster);
        addDatabaseAzureRequest(createSdxClusterRequest, AzureDatabaseType.SINGLE_SERVER, false);

        SdxClusterResponse sdxClusterResponse = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> sdxController.create(SDX_CLUSTER_NAME, createSdxClusterRequest));
        verifySdxClusterCreation(createSdxClusterRequest, sdxCluster, sdxClusterResponse);
    }

    @Test
    void createTestWithFlexibleServerWhenSingleServerRejectEnabled() {
        SdxCluster sdxCluster = createSdxCluster();
        SdxClusterRequest createSdxClusterRequest = createSdxClusterRequest(sdxCluster);
        addDatabaseAzureRequest(createSdxClusterRequest, AzureDatabaseType.FLEXIBLE_SERVER, true);

        SdxClusterResponse sdxClusterResponse = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> sdxController.create(SDX_CLUSTER_NAME, createSdxClusterRequest));
        verifySdxClusterCreation(createSdxClusterRequest, sdxCluster, sdxClusterResponse);
    }

    @Test
    void createTestWithFlexibleServerWhenSingleServerRejectDisabled() {
        SdxCluster sdxCluster = createSdxCluster();
        SdxClusterRequest createSdxClusterRequest = createSdxClusterRequest(sdxCluster);
        addDatabaseAzureRequest(createSdxClusterRequest, AzureDatabaseType.FLEXIBLE_SERVER, false);

        SdxClusterResponse sdxClusterResponse = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> sdxController.create(SDX_CLUSTER_NAME, createSdxClusterRequest));
        verifySdxClusterCreation(createSdxClusterRequest, sdxCluster, sdxClusterResponse);
    }

    private SdxCluster createSdxCluster() {
        SdxCluster sdxCluster = getValidSdxCluster();
        lenient().when(sdxService.createSdx(anyString(), anyString(), any(SdxClusterRequest.class), nullable(StackV4Request.class)))
                .thenReturn(Pair.of(sdxCluster, new FlowIdentifier(FlowType.FLOW, "FLOW_ID")));
        return sdxCluster;
    }

    private SdxClusterRequest createSdxClusterRequest(SdxCluster sdxCluster) {
        SdxClusterRequest createSdxClusterRequest = new SdxClusterRequest();
        createSdxClusterRequest.setClusterShape(SdxClusterShape.MEDIUM_DUTY_HA);
        createSdxClusterRequest.setEnvironment("test-env");
        Map<String, String> tags = new HashMap<>();
        tags.put("tag1", "value1");
        createSdxClusterRequest.addTags(tags);
        SdxStatusEntity sdxStatusEntity = new SdxStatusEntity();
        sdxStatusEntity.setStatus(DatalakeStatusEnum.REQUESTED);
        sdxStatusEntity.setStatusReason("statusreason");
        sdxStatusEntity.setCreated(1L);
        lenient().when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(sdxStatusEntity);
        ReflectionTestUtils.setField(sdxClusterConverter, "sdxStatusService", sdxStatusService);
        return createSdxClusterRequest;
    }

    private void verifySdxClusterCreation(SdxClusterRequest createSdxClusterRequest, SdxCluster sdxCluster, SdxClusterResponse sdxClusterResponse) {
        verify(sdxService).createSdx(eq(USER_CRN), eq(SDX_CLUSTER_NAME), eq(createSdxClusterRequest), nullable(StackV4Request.class));
        verify(sdxStatusService, times(1)).getActualStatusForSdx(sdxCluster);
        assertEquals(SDX_CLUSTER_NAME, sdxClusterResponse.getName());
        assertEquals("test-env", sdxClusterResponse.getEnvironmentName());
        assertEquals("crn:sdxcluster", sdxClusterResponse.getCrn());
        assertEquals(SdxClusterStatusResponse.REQUESTED, sdxClusterResponse.getStatus());
        assertEquals("statusreason", sdxClusterResponse.getStatusReason());
    }

    private void addDatabaseAzureRequest(SdxClusterRequest createSdxClusterRequest, AzureDatabaseType azureDatabaseType, boolean singleServerRejectEnabled) {
        SdxDatabaseAzureRequest sdxDatabaseAzureRequest = new SdxDatabaseAzureRequest();
        sdxDatabaseAzureRequest.setAzureDatabaseType(azureDatabaseType);
        SdxDatabaseRequest externalDatabase = new SdxDatabaseRequest();
        externalDatabase.setCreate(true);
        externalDatabase.setSdxDatabaseAzureRequest(sdxDatabaseAzureRequest);
        createSdxClusterRequest.setExternalDatabase(externalDatabase);

        when(entitlementService.isSingleServerRejectEnabled(any())).thenReturn(singleServerRejectEnabled);
    }

    @Test
    void createTestWithInvalidClusterShape() {
        SdxClusterRequest createSdxClusterRequest = new SdxClusterRequest();
        createSdxClusterRequest.setClusterShape(SdxClusterShape.CONTAINERIZED);
        createSdxClusterRequest.setEnvironment("test-env");
        assertThrows(BadRequestException.class, () -> sdxController.create(SDX_CLUSTER_NAME, createSdxClusterRequest));
    }

    @Test
    void getTest() {
        SdxCluster sdxCluster = getValidSdxCluster();
        when(sdxService.getByNameInAccount(anyString(), anyString())).thenReturn(sdxCluster);

        SdxStatusEntity sdxStatusEntity = new SdxStatusEntity();
        sdxStatusEntity.setStatus(DatalakeStatusEnum.REQUESTED);
        sdxStatusEntity.setStatusReason("statusreason");
        sdxStatusEntity.setCreated(1L);
        when(sdxStatusService.getActualStatusForSdx(sdxCluster)).thenReturn(sdxStatusEntity);
        ReflectionTestUtils.setField(sdxClusterConverter, "sdxStatusService", sdxStatusService);

        SdxClusterResponse sdxClusterResponse = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> sdxController.get(SDX_CLUSTER_NAME));
        assertEquals(SDX_CLUSTER_NAME, sdxClusterResponse.getName());
        assertEquals("test-env", sdxClusterResponse.getEnvironmentName());
        assertEquals("crn:sdxcluster", sdxClusterResponse.getCrn());
        assertEquals(SdxClusterStatusResponse.REQUESTED, sdxClusterResponse.getStatus());
        assertEquals("statusreason", sdxClusterResponse.getStatusReason());
    }

    @Test
    void changeImageCatalogTest() {
        SdxCluster sdxCluster = getValidSdxCluster();
        when(sdxService.getByNameInAccount(any(), anyString())).thenReturn(sdxCluster);

        SdxChangeImageCatalogRequest request = new SdxChangeImageCatalogRequest();
        request.setImageCatalog("image-catalog");

        sdxController.changeImageCatalog(sdxCluster.getName(), request);

        verify(sdxImageCatalogService).changeImageCatalog(sdxCluster, request.getImageCatalog());
    }

    @Test
    void generateImageCatalogTest() {
        CloudbreakImageCatalogV3 imageCatalog = mock(CloudbreakImageCatalogV3.class);
        when(sdxImageCatalogService.generateImageCatalog(SDX_CLUSTER_NAME)).thenReturn(imageCatalog);

        SdxGenerateImageCatalogResponse actual = sdxController.generateImageCatalog(SDX_CLUSTER_NAME);

        assertEquals(imageCatalog, actual.getImageCatalog());
    }

    @Test
    void enableRangerRazByCrnTest() {
        SdxCluster sdxCluster = getValidSdxCluster();
        when(sdxService.getByCrn(anyString(), anyString())).thenReturn(sdxCluster);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> sdxController.enableRangerRazByCrn(sdxCluster.getCrn()));

        verify(rangerRazService).updateRangerRazEnabled(sdxCluster);
    }

    @Test
    void enableRangerRazByNameTest() {
        SdxCluster sdxCluster = getValidSdxCluster();
        when(sdxService.getByNameInAccount(anyString(), anyString())).thenReturn(sdxCluster);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> sdxController.enableRangerRazByName(sdxCluster.getName()));

        verify(rangerRazService).updateRangerRazEnabled(sdxCluster);
    }

    @Test
    void rotateSaltPasswordByName() {
        SdxCluster sdxCluster = getValidSdxCluster();
        when(sdxService.getByCrn(USER_CRN, sdxCluster.getCrn())).thenReturn(sdxCluster);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> sdxController.rotateSaltPasswordByCrn(sdxCluster.getCrn()));

        verify(saltService).rotateSaltPassword(sdxCluster);
    }

    @Test
    void refreshDatahubs() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> sdxController.refreshDataHubs(SDX_CLUSTER_NAME, null));
        verify(sdxService, times(1)).refreshDataHub(SDX_CLUSTER_NAME, null);
    }

    @Test
    void validateBackupStorageWithDefaultLocation() {
        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        ValidationResult validationResult = resultBuilder.build();

        SdxCluster sdxCluster = getValidSdxCluster();
        sdxCluster.setClusterName(SDX_CLUSTER_NAME);
        SdxBackupLocationValidationRequest sdxBackupLocationValidationRequest = new SdxBackupLocationValidationRequest(SDX_CLUSTER_NAME,
                BackupOperationType.ANY);
        when(storageValidationService.validateBackupStorage(sdxCluster, BackupOperationType.ANY, null)).thenReturn(validationResult);
        when(sdxService.getByNameInAccount(USER_CRN, SDX_CLUSTER_NAME)).thenReturn(sdxCluster);

        ValidationResult result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> sdxController.validateBackupStorage(sdxBackupLocationValidationRequest));

        verify(sdxService, times(1)).getByNameInAccount(USER_CRN, SDX_CLUSTER_NAME);
        verify(storageValidationService, times(1)).validateBackupStorage(sdxCluster, BackupOperationType.ANY, null);
        assertEquals(ValidationResult.State.VALID, result.getState());
    }

    @Test
    void validateBackupStorageWithNonDefaultLocation() {
        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        ValidationResult validationResult = resultBuilder.build();

        SdxCluster sdxCluster = getValidSdxCluster();
        sdxCluster.setClusterName(SDX_CLUSTER_NAME);
        SdxBackupLocationValidationRequest sdxBackupLocationValidationRequest = new SdxBackupLocationValidationRequest(SDX_CLUSTER_NAME,
                BackupOperationType.ANY, BACKUP_LOCATION);
        when(storageValidationService.validateBackupStorage(sdxCluster, BackupOperationType.ANY, BACKUP_LOCATION)).thenReturn(validationResult);
        when(sdxService.getByNameInAccount(USER_CRN, SDX_CLUSTER_NAME)).thenReturn(sdxCluster);

        ValidationResult result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> sdxController.validateBackupStorage(sdxBackupLocationValidationRequest));

        verify(sdxService, times(1)).getByNameInAccount(USER_CRN, SDX_CLUSTER_NAME);
        verify(storageValidationService, times(1)).validateBackupStorage(sdxCluster, BackupOperationType.ANY, BACKUP_LOCATION);
        assertEquals(ValidationResult.State.VALID, result.getState());
    }

    @Test
    void testUpdateSaltByCrn() {
        SdxCluster sdxCluster = getValidSdxCluster();
        when(sdxService.getByCrn(USER_CRN, sdxCluster.getCrn())).thenReturn(sdxCluster);
        when(saltService.updateSalt(sdxCluster)).thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));

        FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> sdxController.updateSaltByCrn(sdxCluster.getCrn()));

        verify(sdxService, times(1)).getByCrn(USER_CRN, sdxCluster.getCrn());
        verify(saltService, times(1)).updateSalt(sdxCluster);
        assertEquals(FlowType.FLOW, flowIdentifier.getType());
        assertEquals("FLOW_ID", flowIdentifier.getPollableId());
    }

    @Test
    void testDiskUpdateByName() {
        DiskUpdateRequest diskUpdateRequest = new DiskUpdateRequest();
        diskUpdateRequest.setGroup("compute");
        diskUpdateRequest.setSize(100);
        diskUpdateRequest.setVolumeType("gp2");

        SdxCluster sdxCluster = getValidSdxCluster();
        when(sdxService.getByNameInAccount(USER_CRN, "TEST")).thenReturn(sdxCluster);
        when(verticalScaleService.updateDisksDatalake(sdxCluster, diskUpdateRequest, USER_CRN))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));

        FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> sdxController.diskUpdateByName("TEST", diskUpdateRequest));

        verify(sdxService, times(1)).getByNameInAccount(USER_CRN, "TEST");
        verify(verticalScaleService, times(1)).updateDisksDatalake(sdxCluster, diskUpdateRequest, USER_CRN);
        assertEquals(FlowType.FLOW, flowIdentifier.getType());
        assertEquals("FLOW_ID", flowIdentifier.getPollableId());
    }

    @Test
    void testDiskUpdateByCrn() {
        DiskUpdateRequest diskUpdateRequest = new DiskUpdateRequest();
        diskUpdateRequest.setGroup("compute");
        diskUpdateRequest.setSize(100);
        diskUpdateRequest.setVolumeType("gp2");

        SdxCluster sdxCluster = getValidSdxCluster();
        when(sdxService.getByCrn(USER_CRN, sdxCluster.getCrn())).thenReturn(sdxCluster);
        when(verticalScaleService.updateDisksDatalake(sdxCluster, diskUpdateRequest, USER_CRN))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));

        FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> sdxController.diskUpdateByCrn(sdxCluster.getCrn(), diskUpdateRequest));

        verify(sdxService, times(1)).getByCrn(USER_CRN, sdxCluster.getCrn());
        verify(verticalScaleService, times(1)).updateDisksDatalake(sdxCluster, diskUpdateRequest, USER_CRN);
        assertEquals(FlowType.FLOW, flowIdentifier.getType());
        assertEquals("FLOW_ID", flowIdentifier.getPollableId());
    }

    @Test
    void testAddVolumesByName() {
        StackAddVolumesRequest stackAddVolumesRequest = new StackAddVolumesRequest();
        stackAddVolumesRequest.setInstanceGroup("COMPUTE");
        stackAddVolumesRequest.setCloudVolumeUsageType("GENERAL");
        stackAddVolumesRequest.setSize(200L);
        stackAddVolumesRequest.setType("gp2");
        stackAddVolumesRequest.setNumberOfDisks(2L);

        SdxCluster sdxCluster = getValidSdxCluster();
        when(sdxService.getByNameInAccount(USER_CRN, "TEST")).thenReturn(sdxCluster);
        when(verticalScaleService.addVolumesDatalake(sdxCluster, stackAddVolumesRequest, USER_CRN))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));

        FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> sdxController.addVolumesByStackName("TEST",
                stackAddVolumesRequest));

        verify(sdxService, times(1)).getByNameInAccount(USER_CRN, "TEST");
        verify(verticalScaleService, times(1)).addVolumesDatalake(sdxCluster, stackAddVolumesRequest, USER_CRN);
        assertEquals(FlowType.FLOW, flowIdentifier.getType());
        assertEquals("FLOW_ID", flowIdentifier.getPollableId());
    }

    @Test
    void testAddVolumesByCrn() {
        StackAddVolumesRequest stackAddVolumesRequest = new StackAddVolumesRequest();
        stackAddVolumesRequest.setInstanceGroup("COMPUTE");
        stackAddVolumesRequest.setCloudVolumeUsageType("GENERAL");
        stackAddVolumesRequest.setSize(200L);
        stackAddVolumesRequest.setType("gp2");
        stackAddVolumesRequest.setNumberOfDisks(2L);

        SdxCluster sdxCluster = getValidSdxCluster();
        when(sdxService.getByCrn(USER_CRN, sdxCluster.getCrn())).thenReturn(sdxCluster);
        when(verticalScaleService.addVolumesDatalake(sdxCluster, stackAddVolumesRequest, USER_CRN))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));

        FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> sdxController.addVolumesByStackCrn(sdxCluster.getCrn(),
                stackAddVolumesRequest));

        verify(sdxService, times(1)).getByCrn(USER_CRN, sdxCluster.getCrn());
        verify(verticalScaleService, times(1)).addVolumesDatalake(sdxCluster, stackAddVolumesRequest, USER_CRN);
        assertEquals(FlowType.FLOW, flowIdentifier.getType());
        assertEquals("FLOW_ID", flowIdentifier.getPollableId());
    }

    @Test
    void resizeWithInvalidClusterShape() {
        SdxClusterResizeRequest request = new SdxClusterResizeRequest();
        request.setClusterShape(SdxClusterShape.CONTAINERIZED);
        request.setEnvironment("env-name");
        assertThrows(BadRequestException.class, () -> sdxController.resize(SDX_CLUSTER_NAME, request));
    }

    @Test
    void getInstanceGroupNamesBySdxDetailsWithInvalidClusterShape() {
        assertThrows(BadRequestException.class, () -> sdxController.getInstanceGroupNamesBySdxDetails(SdxClusterShape.CONTAINERIZED, "7.2.17", "AWS"));
    }

    @Test
    void getDefaultTemplateWithInvalidClusterShape() {
        assertThrows(BadRequestException.class, () -> sdxController.getDefaultTemplate(SdxClusterShape.CONTAINERIZED, "7.2.18", "aws", null));
    }

    @Test
    void getRecommendationWithInvalidClusterShape() {
        assertThrows(BadRequestException.class,
            () -> sdxController.getRecommendation("cred-crn", SdxClusterShape.CONTAINERIZED, "7.2.18", "aws", "us-west-1",
                "az1", null));
    }

    @Test
    void testUpdateRootVolumeByDatalakeName() {
        DiskUpdateRequest diskUpdateRequest = new DiskUpdateRequest();
        diskUpdateRequest.setGroup("compute");
        diskUpdateRequest.setSize(100);
        diskUpdateRequest.setVolumeType("gp2");
        diskUpdateRequest.setDiskType(DiskType.ROOT_DISK);

        SdxCluster sdxCluster = getValidSdxCluster();
        when(sdxService.getByNameInAccount(USER_CRN, "TEST")).thenReturn(sdxCluster);
        when(verticalScaleService.updateRootVolumeDatalake(sdxCluster, diskUpdateRequest, USER_CRN))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));

        FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> sdxController.updateRootVolumeByDatalakeName("TEST", diskUpdateRequest));

        verify(sdxService, times(1)).getByNameInAccount(USER_CRN, "TEST");
        verify(verticalScaleService, times(1)).updateRootVolumeDatalake(sdxCluster, diskUpdateRequest, USER_CRN);
        assertEquals(FlowType.FLOW, flowIdentifier.getType());
        assertEquals("FLOW_ID", flowIdentifier.getPollableId());
    }

    @Test
    void testUpdateRootVolumeByDatalakeCrn() {
        DiskUpdateRequest diskUpdateRequest = new DiskUpdateRequest();
        diskUpdateRequest.setGroup("compute");
        diskUpdateRequest.setSize(100);
        diskUpdateRequest.setVolumeType("gp2");
        diskUpdateRequest.setDiskType(DiskType.ROOT_DISK);

        SdxCluster sdxCluster = getValidSdxCluster();
        when(sdxService.getByCrn(USER_CRN, sdxCluster.getCrn())).thenReturn(sdxCluster);
        when(verticalScaleService.updateRootVolumeDatalake(sdxCluster, diskUpdateRequest, USER_CRN))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));

        FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> sdxController.updateRootVolumeByDatalakeCrn(sdxCluster.getCrn(), diskUpdateRequest));

        verify(sdxService, times(1)).getByCrn(USER_CRN, sdxCluster.getCrn());
        verify(verticalScaleService, times(1)).updateRootVolumeDatalake(sdxCluster, diskUpdateRequest, USER_CRN);
        assertEquals(FlowType.FLOW, flowIdentifier.getType());
        assertEquals("FLOW_ID", flowIdentifier.getPollableId());
    }

    private SdxCluster getValidSdxCluster() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName(SDX_CLUSTER_NAME);
        sdxCluster.setClusterShape(SdxClusterShape.MEDIUM_DUTY_HA);
        sdxCluster.setEnvName("test-env");
        sdxCluster.setCrn("crn:sdxcluster");
        sdxCluster.setSdxDatabase(new SdxDatabase());
        sdxCluster.setSeLinux(SeLinux.PERMISSIVE);
        return sdxCluster;
    }

    @Test
    void testGetSdxDetailWithResourcesByName() {
        SdxCluster sdxCluster = getValidSdxCluster();
        sdxCluster.setAccountId("accountId");
        ReflectionTestUtils.setField(sdxClusterConverter, "sdxStatusService", sdxStatusService);
        when(sdxService.getByNameInAccount(any(), eq("TEST"))).thenReturn(sdxCluster);
        StackV4Response stackV4Response = mock(StackV4Response.class);
        Set<String> entries = Set.of();
        when(stackService.getDetailWithResources("TEST", entries, "accountId")).thenReturn(stackV4Response);
        SdxClusterDetailResponse sdxClusterDetailResponse = sdxController.getSdxDetailWithResourcesByName("TEST", entries);
        assertEquals(stackV4Response, sdxClusterDetailResponse.getStackV4Response());
        assertEquals(sdxCluster.getClusterName(), sdxClusterDetailResponse.getName());
    }

    private static Object[][] getByEnvCrnSource() {
        return new Object[][]{
                {true, 2},
                {false, 1},
        };
    }

    @ParameterizedTest
    @MethodSource("getByEnvCrnSource")
    void testGetByEnvCrn(boolean includeDetached, int numberOfDatalakes) {
        ReflectionTestUtils.setField(sdxClusterConverter, "sdxStatusService", sdxStatusService);

        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("new-sdx-cluster");
        sdxCluster.setClusterShape(SdxClusterShape.MEDIUM_DUTY_HA);
        sdxCluster.setEnvName("test-env");
        sdxCluster.setCrn("crn:sdxcluster:new");
        sdxCluster.setSdxDatabase(new SdxDatabase());
        sdxCluster.setSeLinux(SeLinux.PERMISSIVE);

        SdxCluster detachedCluster = new SdxCluster();
        detachedCluster.setClusterName("old-sdx-cluster");
        detachedCluster.setClusterShape(SdxClusterShape.MEDIUM_DUTY_HA);
        detachedCluster.setEnvName("test-env");
        detachedCluster.setCrn("crn:sdxcluster:old");
        detachedCluster.setSdxDatabase(new SdxDatabase());
        detachedCluster.setSeLinux(SeLinux.PERMISSIVE);
        detachedCluster.setDetached(true);

        when(dataLakeFiltering.filterDataLakesByEnvCrn(DESCRIBE_DATALAKE, "envCrn", includeDetached))
                .thenReturn(List.of(sdxCluster, detachedCluster));

        List<SdxClusterResponse> sdxClusters = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> sdxController.getByEnvCrn("envCrn", includeDetached));

        assertEquals(numberOfDatalakes, sdxClusters.size());
        assertTrue(sdxClusters.stream().map(SdxClusterResponse::getName).anyMatch("new-sdx-cluster"::equals));
    }

    @Test
    void testEnableSeLinuxByName() {
        SdxCluster sdxCluster = getValidSdxCluster();
        when(sdxService.getByNameInAccount(USER_CRN, "TEST")).thenReturn(sdxCluster);
        when(seLinuxService.modifySeLinuxOnDatalake(sdxCluster, USER_CRN, SeLinux.ENFORCING))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));
        FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> sdxController.modifySeLinuxByName("TEST", SeLinux.ENFORCING));
        verify(sdxService).getByNameInAccount(USER_CRN, "TEST");
        verify(seLinuxService).modifySeLinuxOnDatalake(sdxCluster, USER_CRN, SeLinux.ENFORCING);
        assertEquals(FlowType.FLOW, flowIdentifier.getType());
        assertEquals("FLOW_ID", flowIdentifier.getPollableId());
    }

    @Test
    void testEnableSeLinuxByCrn() {
        SdxCluster sdxCluster = getValidSdxCluster();
        when(sdxService.getByCrn(USER_CRN, sdxCluster.getCrn())).thenReturn(sdxCluster);
        when(seLinuxService.modifySeLinuxOnDatalake(sdxCluster, USER_CRN, SeLinux.ENFORCING))
                .thenReturn(new FlowIdentifier(FlowType.FLOW, "FLOW_ID"));
        FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> sdxController.modifySeLinuxByCrn(sdxCluster.getCrn(),
                SeLinux.ENFORCING));
        verify(sdxService).getByCrn(USER_CRN, sdxCluster.getCrn());
        verify(seLinuxService).modifySeLinuxOnDatalake(sdxCluster, USER_CRN, SeLinux.ENFORCING);
        assertEquals(FlowType.FLOW, flowIdentifier.getType());
        assertEquals("FLOW_ID", flowIdentifier.getPollableId());
    }
}
