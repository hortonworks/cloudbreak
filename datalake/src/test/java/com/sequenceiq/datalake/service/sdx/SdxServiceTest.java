package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.SDX_CLUSTER_DELETION_STARTED;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.LIGHT_DUTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

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
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("SDX service tests")
class SdxServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:hortonworks:user:perdos@hortonworks.com";

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:default:environment:e438a2db-d650-4132-ae62-242c5ba2f784";

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:default:datalake:e438a2db-d650-4132-ae62-242c5ba2f784";

    private static final Long SDX_ID = 2L;

    private static final String CLUSTER_NAME = "test-sdx-cluster";

    private static final Set<String> SUPPORTED_PLATFORMS = Set.of("AWS");

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Mock
    private EnvironmentEndpoint environmentEndpoint;

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

    @InjectMocks
    private SdxService sdxService;

    @BeforeEach
    void initMocks() {
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(sdxService, "dbServiceSupportedPlatforms", SUPPORTED_PLATFORMS);
    }

    @Test
    void getWhenClusterNameProvided() {
        SdxCluster sdxCluser = new SdxCluster();
        sdxCluser.setEnvName("env");
        sdxCluser.setClusterName(CLUSTER_NAME);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(eq("hortonworks"), eq(CLUSTER_NAME))).thenReturn(Optional.of(sdxCluser));
        SdxCluster returnedSdxCluster = sdxService.getSdxByNameInAccount(USER_CRN, CLUSTER_NAME);
        Assertions.assertEquals(sdxCluser, returnedSdxCluster);
    }

    @Test
    void getWhenClusterCrnProvided() {
        SdxCluster sdxCluser = new SdxCluster();
        sdxCluser.setEnvName("env");
        sdxCluser.setClusterName(CLUSTER_NAME);
        when(sdxClusterRepository.findByAccountIdAndCrnAndDeletedIsNull(eq("hortonworks"), eq(ENVIRONMENT_CRN))).thenReturn(Optional.of(sdxCluser));
        SdxCluster returnedSdxCluster = sdxService.getByCrn(USER_CRN, ENVIRONMENT_CRN);
        Assertions.assertEquals(sdxCluser, returnedSdxCluster);
    }

    @Test
    void getByAccountIdAndEnvNameNotFound() {
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(anyString(), anyString())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> sdxService.getSdxByNameInAccount(USER_CRN, "env"), "Sdx cluster not found");
    }

    @Test
    void createSdxIfExists() {
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setClusterShape(SdxClusterShape.MEDIUM_DUTY_HA);
        sdxClusterRequest.setEnvironment("envir");
        Map<String, String> tags = new HashMap<>();
        tags.put("mytag", "tagecske");
        sdxClusterRequest.setTags(tags);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNull(anyString(), anyString())).thenReturn(Collections.singletonList(new SdxCluster()));
        Assertions.assertThrows(BadRequestException.class,
                () -> sdxService.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null), "SDX cluster exists for environment name");
    }

    @Test
    void createSdx() {
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setClusterShape(LIGHT_DUTY);
        Map<String, String> tags = new HashMap<>();
        tags.put("mytag", "tagecske");
        sdxClusterRequest.setTags(tags);
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
        SdxCluster createdSdxCluster = sdxService.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null);
        Assertions.assertEquals(id, createdSdxCluster.getId());
        final ArgumentCaptor<SdxCluster> captor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository, times(1)).save(captor.capture());
        SdxCluster capturedSdx = captor.getValue();
        Assertions.assertEquals("tagecske", capturedSdx.getTags().getValue("mytag"));
        Assertions.assertEquals(CLUSTER_NAME, capturedSdx.getClusterName());
        Assertions.assertEquals(LIGHT_DUTY, capturedSdx.getClusterShape());
        Assertions.assertEquals("envir", capturedSdx.getEnvName());
        Assertions.assertEquals("hortonworks", capturedSdx.getAccountId());
        Assertions.assertEquals(USER_CRN, capturedSdx.getInitiatorUserCrn());
        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(DatalakeStatusEnum.REQUESTED,
                ResourceEvent.SDX_CLUSTER_PROVISION_STARTED, "Datalake requested", createdSdxCluster);

        Assertions.assertEquals(1L, capturedSdx.getCreated());
        Assertions.assertFalse(capturedSdx.isCreateDatabase());
        Assertions.assertTrue(createdSdxCluster.getCrn().matches("crn:cdp:datalake:us-west-1:hortonworks:datalake:.*"));
        verify(sdxReactorFlowManager).triggerSdxCreation(id);
    }

    @Test
    void createSdxForAzureWithEnabledDB() {
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setClusterShape(LIGHT_DUTY);
        Map<String, String> tags = new HashMap<>();
        tags.put("mytag", "tagecske");
        sdxClusterRequest.setTags(tags);
        sdxClusterRequest.setEnvironment("envir");
        SdxDatabaseRequest externalDatabase = new SdxDatabaseRequest();
        externalDatabase.setCreate(true);
        sdxClusterRequest.setExternalDatabase(externalDatabase);
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNull(anyString(), anyString())).thenReturn(new ArrayList<>());
        long id = 10L;
        when(clock.getCurrentTimeMillis()).thenReturn(1L);
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.AZURE);
        BadRequestException gotException = assertThrows(BadRequestException.class,
                () -> sdxService.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null));
        assertEquals(String.format("Cannot create external database for sdx: test-sdx-cluster, for now only %s is/are supported", SUPPORTED_PLATFORMS),
                gotException.getMessage());
    }

    @Test
    void createSdxForAwsWithEmptyDB() {
        SdxClusterRequest sdxClusterRequest = new SdxClusterRequest();
        sdxClusterRequest.setClusterShape(LIGHT_DUTY);
        Map<String, String> tags = new HashMap<>();
        tags.put("mytag", "tagecske");
        sdxClusterRequest.setTags(tags);
        sdxClusterRequest.setEnvironment("envir");
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNull(anyString(), anyString())).thenReturn(new ArrayList<>());
        long id = 10L;
        when(sdxClusterRepository.save(any(SdxCluster.class))).thenAnswer(invocation -> {
            SdxCluster sdxWithId = invocation.getArgument(0, SdxCluster.class);
            sdxWithId.setId(id);
            return sdxWithId;
        });
        when(clock.getCurrentTimeMillis()).thenReturn(1L);
        mockEnvironmentCall(sdxClusterRequest, CloudPlatform.AWS);
        SdxCluster createdSdxCluster = sdxService.createSdx(USER_CRN, CLUSTER_NAME, sdxClusterRequest, null);
        Assertions.assertEquals(id, createdSdxCluster.getId());
        final ArgumentCaptor<SdxCluster> captor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository, times(1)).save(captor.capture());
        SdxCluster capturedSdx = captor.getValue();
        Assertions.assertEquals("tagecske", capturedSdx.getTags().getValue("mytag"));
        Assertions.assertEquals(CLUSTER_NAME, capturedSdx.getClusterName());
        Assertions.assertEquals(LIGHT_DUTY, capturedSdx.getClusterShape());
        Assertions.assertEquals("envir", capturedSdx.getEnvName());
        Assertions.assertEquals("hortonworks", capturedSdx.getAccountId());
        Assertions.assertEquals(USER_CRN, capturedSdx.getInitiatorUserCrn());
        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(DatalakeStatusEnum.REQUESTED,
                ResourceEvent.SDX_CLUSTER_PROVISION_STARTED, "Datalake requested", createdSdxCluster);
        Assertions.assertEquals(1L, capturedSdx.getCreated());
        Assertions.assertTrue(capturedSdx.isCreateDatabase());
        verify(sdxReactorFlowManager).triggerSdxCreation(id);
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
        when(environmentEndpoint.getByName(anyString())).thenReturn(detailedEnvironmentResponse);
    }

    @Test
    void listSdxWhenNameisProvided() {
        List<SdxCluster> sdxClusters = List.of(new SdxCluster(), new SdxCluster());
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNull(eq("hortonworks"), eq("envir"))).thenReturn(sdxClusters);
        List<SdxCluster> sdxList = sdxService.listSdx(USER_CRN, "envir");
        Assertions.assertEquals(2, sdxList.size());
    }

    @Test
    void testListSdxWhenCrnIsProvided() {
        List<SdxCluster> sdxClusters = List.of(new SdxCluster(), new SdxCluster());
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNull(eq("hortonworks"), eq(ENVIRONMENT_CRN))).thenReturn(sdxClusters);
        List<SdxCluster> sdxList = sdxService.listSdxByEnvCrn(USER_CRN, ENVIRONMENT_CRN);
        Assertions.assertEquals(2, sdxList.size());
    }

    @Test
    void listSdxWhenInvalidCrnProvided() {
        String crn = "crsdfadsfdsf sadasf3-df81ae585e10";
        assertThrows(BadRequestException.class, () -> sdxService.listSdx(crn, "envir"));
    }

    @Test
    void deleteSdxWhenNotFound() {
        Assertions.assertThrows(com.sequenceiq.cloudbreak.exception.NotFoundException.class,
                () -> sdxService.deleteSdx(USER_CRN, CLUSTER_NAME, false));
        verify(sdxClusterRepository, times(1))
                .findByAccountIdAndClusterNameAndDeletedIsNull(eq("hortonworks"), eq(CLUSTER_NAME));
    }

    @Test
    void validateAllAwsStackRequests() {
        CloudPlatform cp = CloudPlatform.AWS;
        Stream.of(SdxClusterShape.values())
                .filter(a -> !a.equals(SdxClusterShape.CUSTOM))
                .forEach(a -> assertStackV4Request(cp, a));
    }

    @Test
    void validateAllAzureStackRequests() {
        CloudPlatform cp = CloudPlatform.AZURE;
        Stream.of(SdxClusterShape.values())
                .filter(a -> !a.equals(SdxClusterShape.CUSTOM))
                .forEach(a -> assertStackV4Request(cp, a));
    }

    @Test
    void validateAllYarnStackRequests() {
        CloudPlatform cp = CloudPlatform.YARN;
        Stream.of(SdxClusterShape.values())
                .filter(a -> !a.equals(SdxClusterShape.CUSTOM))
                .forEach(a -> assertStackV4Request(cp, a));
    }

    private void assertStackV4Request(CloudPlatform type, SdxClusterShape shape) {
        StackV4Request lightDutyStackrequest = sdxService.getStackRequestFromFile(sdxService.generateClusterTemplatePath(type.toString(), shape));
        assertNotNull(lightDutyStackrequest.getCluster().getBlueprintName(), "Bp name should be defined in all templates");
    }

    @Test
    void deleteSdxWhenCorrectNameIsProvided() {
        SdxCluster sdxCluster = getSdxClusterForDeletionTest();
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        mockCBCallForDistroXClusters(Sets.newHashSet());
        sdxService.deleteSdx(USER_CRN, "sdx-cluster-name", true);
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
                () -> sdxService.deleteSdx(USER_CRN, "sdx-cluster-name", false),
                "The following Data Hub cluster(s) must be terminated before SDX deletion [existingDistroXCluster]");
    }

    @Test
    void testSyncByName() {
        sdxService.sync("name");

        verify(stackV4Endpoint).sync(0L, "name");
    }

    @Test
    void testSyncByCrn() {
        SdxCluster sdxCluser = new SdxCluster();
        sdxCluser.setEnvName("env");
        sdxCluser.setClusterName(CLUSTER_NAME);
        when(sdxClusterRepository.findByAccountIdAndCrnAndDeletedIsNull(eq("hortonworks"), eq(DATALAKE_CRN))).thenReturn(Optional.of(sdxCluser));

        sdxService.syncByCrn(USER_CRN, DATALAKE_CRN);

        verify(stackV4Endpoint).sync(0L, CLUSTER_NAME);
    }

    private void mockCBCallForDistroXClusters(Set<StackViewV4Response> stackViews) {
        when(distroxService.getAttachedDistroXClusters(anyString(), anyString())).thenReturn(stackViews);
    }

    private SdxCluster getSdxClusterForDeletionTest() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(SDX_ID);
        sdxCluster.setInitiatorUserCrn(USER_CRN);
        sdxCluster.setEnvCrn(ENVIRONMENT_CRN);
        sdxCluster.setEnvName("envir");
        sdxCluster.setClusterName("sdx-cluster-name");
        return sdxCluster;
    }

}
