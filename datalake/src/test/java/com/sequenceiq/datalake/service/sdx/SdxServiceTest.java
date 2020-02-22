package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.SDX_CLUSTER_DELETION_STARTED;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.LIGHT_DUTY;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.EnvironmentClientService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.datalake.service.validation.cloudstorage.CloudStorageLocationValidator;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.sdx.api.model.SdxCloudStorageRequest;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@ExtendWith(MockitoExtension.class)
@DisplayName("SDX service tests")
class SdxServiceTest extends SdxServiceTestBase {

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:default:datalake:e438a2db-d650-4132-ae62-242c5ba2f784";

    private static final Long SDX_ID = 2L;

    private static final String CLUSTER_NAME = "test-sdx-cluster";

    private static final Set<String> SUPPORTED_PLATFORMS = Set.of("AWS");

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
    private DistroxService distroxService;

    @Mock
    private Map<CDPConfigKey, StackV4Request> cdpStackRequests;

    @InjectMocks
    private SdxService underTest;

    @Mock
    private CloudStorageLocationValidator cloudStorageLocationValidator;

    @BeforeEach
    void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void getWhenClusterNameProvided() {
        SdxCluster sdxCluser = new SdxCluster();
        sdxCluser.setEnvName("env");
        sdxCluser.setClusterName(CLUSTER_NAME);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(eq("hortonworks"), eq(CLUSTER_NAME))).thenReturn(Optional.of(sdxCluser));
        SdxCluster returnedSdxCluster = underTest.getSdxByNameInAccount(super.getTestUserCrn(), CLUSTER_NAME);
        Assertions.assertEquals(sdxCluser, returnedSdxCluster);
    }

    @Test
    void getWhenClusterCrnProvided() {
        SdxCluster sdxCluser = new SdxCluster();
        sdxCluser.setEnvName("env");
        sdxCluser.setClusterName(CLUSTER_NAME);
        when(sdxClusterRepository.findByAccountIdAndCrnAndDeletedIsNull(eq("hortonworks"), eq(super.getTestEnvironmentCrn())))
                .thenReturn(Optional.of(sdxCluser));
        SdxCluster returnedSdxCluster = underTest.getByCrn(super.getTestUserCrn(), super.getTestEnvironmentCrn());
        Assertions.assertEquals(sdxCluser, returnedSdxCluster);
    }

    @Test
    void getByAccountIdAndEnvNameNotFound() {
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(anyString(), anyString())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> underTest.getSdxByNameInAccount(super.getTestUserCrn(), "env"), "Sdx cluster not found");
    }

    @Test
    void createSdxIfExists() {
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setClusterShape(SdxClusterShape.MEDIUM_DUTY_HA);
        sdxClusterRequest.setEnvironment("envir");
        Map<String, String> tags = new HashMap<>();
        tags.put("mytag", "tagecske");
        sdxClusterRequest.addTags(tags);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNull(anyString(), anyString())).thenReturn(Collections.singletonList(new SdxCluster()));
        assertThrows(BadRequestException.class,
                () -> underTest.createSdx(super.getTestUserCrn(), CLUSTER_NAME, sdxClusterRequest, null),
                "SDX cluster exists for environment name");
    }

    @Test
    void createSdx() throws IOException {
        String lightDutyJson = FileReaderUtils.readFileFromClasspath("/runtime/7.1.0/aws/light_duty.json");
        when(cdpStackRequests.get(any())).thenReturn(JsonUtil.readValue(lightDutyJson, StackV4Request.class));
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setClusterShape(LIGHT_DUTY);
        Map<String, String> tags = new HashMap<>();
        tags.put("mytag", "tagecske");
        sdxClusterRequest.addTags(tags);
        sdxClusterRequest.setEnvironment("envir");
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNull(anyString(), anyString())).thenReturn(new ArrayList<>());
        long id = 10L;
        when(sdxClusterRepository.save(any(SdxCluster.class))).thenAnswer(invocation -> {
            SdxCluster sdxWithId = invocation.getArgument(0, SdxCluster.class);
            sdxWithId.setId(id);
            return sdxWithId;
        });
        when(clock.getCurrentTimeMillis()).thenReturn(1L);
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.AZURE);
        SdxCluster createdSdxCluster = underTest.createSdx(super.getTestUserCrn(), CLUSTER_NAME, sdxClusterRequest, null);
        Assertions.assertEquals(id, createdSdxCluster.getId());
        final ArgumentCaptor<SdxCluster> captor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository, times(1)).save(captor.capture());
        SdxCluster capturedSdx = captor.getValue();
        Assertions.assertEquals("tagecske", capturedSdx.getTags().getValue("mytag"));
        Assertions.assertEquals(CLUSTER_NAME, capturedSdx.getClusterName());
        Assertions.assertEquals(LIGHT_DUTY, capturedSdx.getClusterShape());
        Assertions.assertEquals("envir", capturedSdx.getEnvName());
        Assertions.assertEquals("hortonworks", capturedSdx.getAccountId());
        Assertions.assertEquals(super.getTestUserCrn(), capturedSdx.getInitiatorUserCrn());
        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(DatalakeStatusEnum.REQUESTED,
                ResourceEvent.SDX_CLUSTER_PROVISION_STARTED, "Datalake requested", createdSdxCluster);

        Assertions.assertEquals(1L, capturedSdx.getCreated());
        Assertions.assertFalse(capturedSdx.isCreateDatabase());
        Assertions.assertTrue(createdSdxCluster.getCrn().matches("crn:cdp:datalake:us-west-1:hortonworks:datalake:.*"));
        verify(sdxReactorFlowManager).triggerSdxCreation(id);
    }

    @Test
    void createSdxWhenBaseLocationEndsWithSlash() throws IOException {
        String lightDutyJson = FileReaderUtils.readFileFromClasspath("/runtime/7.1.0/aws/light_duty.json");
        when(cdpStackRequests.get(any())).thenReturn(JsonUtil.readValue(lightDutyJson, StackV4Request.class));
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setClusterShape(LIGHT_DUTY);
        sdxClusterRequest.setEnvironment("envir");
        SdxCloudStorageRequest cloudStorage = new SdxCloudStorageRequest();
        cloudStorage.setFileSystemType(FileSystemType.S3);
        cloudStorage.setBaseLocation("s3a://some/dir/");
        cloudStorage.setS3(new S3CloudStorageV1Parameters());
        sdxClusterRequest.setCloudStorage(cloudStorage);
        long id = 10L;
        when(sdxClusterRepository.save(any(SdxCluster.class))).thenAnswer(invocation -> {
            SdxCluster sdxWithId = invocation.getArgument(0, SdxCluster.class);
            sdxWithId.setId(id);
            return sdxWithId;
        });
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.AWS);
        SdxCluster createdSdxCluster = underTest.createSdx(super.getTestUserCrn(), CLUSTER_NAME, sdxClusterRequest, null);
        Assertions.assertEquals("s3a://some/dir", createdSdxCluster.getCloudStorageBaseLocation());
    }

    @Test
    void createSdxWhenBaseLocation() throws IOException {
        String lightDutyJson = FileReaderUtils.readFileFromClasspath("/runtime/7.1.0/aws/light_duty.json");
        when(cdpStackRequests.get(any())).thenReturn(JsonUtil.readValue(lightDutyJson, StackV4Request.class));
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setClusterShape(LIGHT_DUTY);
        sdxClusterRequest.setEnvironment("envir");
        SdxCloudStorageRequest cloudStorage = new SdxCloudStorageRequest();
        cloudStorage.setFileSystemType(FileSystemType.S3);
        cloudStorage.setBaseLocation("s3a://some/dir");
        cloudStorage.setS3(new S3CloudStorageV1Parameters());
        sdxClusterRequest.setCloudStorage(cloudStorage);
        long id = 10L;
        when(sdxClusterRepository.save(any(SdxCluster.class))).thenAnswer(invocation -> {
            SdxCluster sdxWithId = invocation.getArgument(0, SdxCluster.class);
            sdxWithId.setId(id);
            return sdxWithId;
        });
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.AWS);
        SdxCluster createdSdxCluster = underTest.createSdx(super.getTestUserCrn(), CLUSTER_NAME, sdxClusterRequest, null);
        Assertions.assertEquals("s3a://some/dir", createdSdxCluster.getCloudStorageBaseLocation());
    }

    private void mockEnvironmentCall(SdxClusterRequest sdxClusterRequest, CloudPlatform cloudPlatform) {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setName(sdxClusterRequest.getEnvironment());
        detailedEnvironmentResponse.setCloudPlatform(cloudPlatform.name());
        detailedEnvironmentResponse.setCrn(Crn.builder()
                .setService(Crn.Service.ENVIRONMENTS)
                .setResourceType(Crn.ResourceType.ENVIRONMENT)
                .setResource(UUID.randomUUID().toString())
                .setAccountId(UUID.randomUUID().toString())
                .build().toString());
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);
    }

    @Test
    void listSdxWhenNameisProvided() {
        List<SdxCluster> sdxClusters = List.of(new SdxCluster(), new SdxCluster());
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNull(eq("hortonworks"), eq("envir"))).thenReturn(sdxClusters);
        List<SdxCluster> sdxList = underTest.listSdx(super.getTestUserCrn(), "envir");
        Assertions.assertEquals(2, sdxList.size());
    }

    @Test
    void testListSdxWhenCrnIsProvided() {
        List<SdxCluster> sdxClusters = List.of(new SdxCluster(), new SdxCluster());
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNull(eq("hortonworks"), eq(super.getTestEnvironmentCrn()))).thenReturn(sdxClusters);
        List<SdxCluster> sdxList = underTest.listSdxByEnvCrn(super.getTestUserCrn(), super.getTestEnvironmentCrn());
        Assertions.assertEquals(2, sdxList.size());
    }

    @Test
    void listSdxWhenInvalidCrnProvided() {
        String crn = "crsdfadsfdsf sadasf3-df81ae585e10";
        assertThrows(BadRequestException.class, () -> underTest.listSdx(crn, "envir"));
    }

    @Test
    void deleteSdxWhenNotFound() {
        assertThrows(com.sequenceiq.cloudbreak.exception.NotFoundException.class,
                () -> underTest.deleteSdx(super.getTestUserCrn(), CLUSTER_NAME, false));
        verify(sdxClusterRepository, times(1))
                .findByAccountIdAndClusterNameAndDeletedIsNull(eq("hortonworks"), eq(CLUSTER_NAME));
    }

    @Test
    void deleteSdxWhenCorrectNameIsProvided() {
        SdxCluster sdxCluster = getSdxClusterForDeletionTest();
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        mockCBCallForDistroXClusters(Sets.newHashSet());
        underTest.deleteSdx(super.getTestUserCrn(), "sdx-cluster-name", true);
        verify(sdxReactorFlowManager, times(1)).triggerSdxDeletion(SDX_ID, true);
        final ArgumentCaptor<SdxCluster> captor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository, times(1)).save(captor.capture());
        verify(sdxStatusService, times(1))
                .setStatusForDatalakeAndNotify(DatalakeStatusEnum.DELETE_REQUESTED,
                        SDX_CLUSTER_DELETION_STARTED, "Datalake deletion requested", sdxCluster);
    }

    @Test
    void testDistroXClustersValidationBeforeDelete() {
        SdxCluster sdxCluster = getSdxClusterForDeletionTest();
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));

        StackViewV4Response stackViewV4Response = new StackViewV4Response();
        stackViewV4Response.setName("existingDistroXCluster");
        mockCBCallForDistroXClusters(Sets.newHashSet(stackViewV4Response));

        assertThrows(BadRequestException.class,
                () -> underTest.deleteSdx(super.getTestUserCrn(), "sdx-cluster-name", false),
                "The following Data Hub cluster(s) must be terminated before SDX deletion [existingDistroXCluster]");
    }

    @Test
    void testSyncByName() {
        underTest.sync("name");

        verify(stackV4Endpoint).sync(0L, "name");
    }

    @Test
    void testSyncByCrn() {
        SdxCluster sdxCluser = new SdxCluster();
        sdxCluser.setEnvName("env");
        sdxCluser.setClusterName(CLUSTER_NAME);
        when(sdxClusterRepository.findByAccountIdAndCrnAndDeletedIsNull(eq("hortonworks"), eq(DATALAKE_CRN))).thenReturn(Optional.of(sdxCluser));

        underTest.syncByCrn(super.getTestUserCrn(), DATALAKE_CRN);

        verify(stackV4Endpoint).sync(0L, CLUSTER_NAME);
    }

    private void mockCBCallForDistroXClusters(Set<StackViewV4Response> stackViews) {
        when(distroxService.getAttachedDistroXClusters(anyString())).thenReturn(stackViews);
    }

    private SdxCluster getSdxClusterForDeletionTest() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(SDX_ID);
        sdxCluster.setInitiatorUserCrn(super.getTestUserCrn());
        sdxCluster.setEnvCrn(super.getTestEnvironmentCrn());
        sdxCluster.setEnvName("envir");
        sdxCluster.setClusterName("sdx-cluster-name");
        return sdxCluster;
    }

}
