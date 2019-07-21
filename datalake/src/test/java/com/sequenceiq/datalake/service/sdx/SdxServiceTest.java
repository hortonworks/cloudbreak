package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.sdx.api.model.SdxClusterShape.LIGHT_DUTY;
import static org.junit.Assert.assertNotNull;
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

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxClusterStatus;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.client.EnvironmentServiceCrnClient;
import com.sequenceiq.environment.client.EnvironmentServiceCrnEndpoints;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@ExtendWith(MockitoExtension.class)
@DisplayName("SDX service tests")
public class SdxServiceTest {

    public static final String USER_CRN = "crn:altus:iam:us-west-1:hortonworks:user:perdos@hortonworks.com";

    public static final String ENVIRONMENT_CRN = "crn:altus:environments:us-west-1:default:environment:e438a2db-d650-4132-ae62-242c5ba2f784";

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Mock
    private EnvironmentServiceCrnClient environmentServiceCrnClient;

    @Mock
    private EnvironmentServiceCrnEndpoints environmentServiceCrnEndpoints;

    @Mock
    private EnvironmentEndpoint environmentEndpoint;

    @Mock
    private StackRequestManifester stackRequestManifester;

    @InjectMocks
    private SdxService sdxService;

    @BeforeEach
    void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void getWhenClusterNameProvided() {
        String clusterName = "test-sdx-cluster";
        SdxCluster sdxCluser = new SdxCluster();
        sdxCluser.setStatus(SdxClusterStatus.REQUESTED);
        sdxCluser.setEnvName("env");
        sdxCluser.setClusterName(clusterName);
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(eq("hortonworks"), eq(clusterName))).thenReturn(Optional.of(sdxCluser));
        SdxCluster returnedSdxCluster = sdxService.getByAccountIdAndSdxName(USER_CRN, clusterName);
        Assertions.assertEquals(sdxCluser, returnedSdxCluster);
    }

    @Test
    void getWhenClusterCrnProvided() {
        String clusterName = "test-sdx-cluster";
        SdxCluster sdxCluser = new SdxCluster();
        sdxCluser.setStatus(SdxClusterStatus.REQUESTED);
        sdxCluser.setEnvName("env");
        sdxCluser.setClusterName(clusterName);
        when(sdxClusterRepository.findByAccountIdAndCrnAndDeletedIsNull(eq("hortonworks"), eq(ENVIRONMENT_CRN))).thenReturn(Optional.of(sdxCluser));
        SdxCluster returnedSdxCluster = sdxService.getByCrn(USER_CRN, ENVIRONMENT_CRN);
        Assertions.assertEquals(sdxCluser, returnedSdxCluster);
    }

    @Test
    void getByAccountIdAndEnvNameNotFound() {
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(anyString(), anyString())).thenReturn(Optional.empty());
        Assertions.assertThrows(NotFoundException.class, () -> sdxService.getByAccountIdAndSdxName(USER_CRN, "env"), "Sdx cluster not found");
    }

    @Test
    void updateSdxStatus() {
        long id = 1L;
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(id);
        when(sdxClusterRepository.findById(id)).thenReturn(Optional.of(sdxCluster));
        sdxService.updateSdxStatus(id, SdxClusterStatus.PROVISIONING_FAILED);
        verify(sdxClusterRepository, times(1)).save(sdxCluster);
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
                () -> sdxService.createSdx(USER_CRN, "test-sdx-cluster", sdxClusterRequest, null), "SDX cluster exists for environment name");
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
        mockEnvironmentCall(sdxClusterRequest);
        String sdxName = "test-sdx-cluster";
        SdxCluster createdSdxCluster = sdxService.createSdx(USER_CRN, sdxName, sdxClusterRequest, null);
        Assertions.assertEquals(id, createdSdxCluster.getId());
        final ArgumentCaptor<SdxCluster> captor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository, times(1)).save(captor.capture());
        SdxCluster capturedSdx = captor.getValue();
        Assertions.assertEquals("tagecske", capturedSdx.getTags().getValue("mytag"));
        Assertions.assertEquals(sdxName, capturedSdx.getClusterName());
        Assertions.assertEquals(LIGHT_DUTY, capturedSdx.getClusterShape());
        Assertions.assertEquals("envir", capturedSdx.getEnvName());
        Assertions.assertEquals("hortonworks", capturedSdx.getAccountId());
        Assertions.assertEquals(USER_CRN, capturedSdx.getInitiatorUserCrn());
        Assertions.assertEquals(SdxClusterStatus.REQUESTED, capturedSdx.getStatus());
        verify(sdxReactorFlowManager).triggerSdxCreation(id);
    }

    private void mockEnvironmentCall(SdxClusterRequest sdxClusterRequest) {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setName(sdxClusterRequest.getEnvironment());
        detailedEnvironmentResponse.setCloudPlatform(CloudPlatform.AWS.name());
        detailedEnvironmentResponse.setCrn(Crn.builder()
                .setService(Crn.Service.ENVIRONMENTS)
                .setResourceType(Crn.ResourceType.ENVIRONMENT)
                .setResource(UUID.randomUUID().toString())
                .setAccountId(UUID.randomUUID().toString())
                .build().toString());
        when(environmentServiceCrnClient.withCrn(anyString())).thenReturn(environmentServiceCrnEndpoints);
        when(environmentServiceCrnEndpoints.environmentV1Endpoint()).thenReturn(environmentEndpoint);
        when(environmentEndpoint.getByName(anyString())).thenReturn(detailedEnvironmentResponse);
    }

    @Test
    void listSdxWhenNameisProvided() {
        ArrayList<SdxCluster> sdxClusters = new ArrayList<>();
        sdxClusters.add(new SdxCluster());
        sdxClusters.add(new SdxCluster());
        when(sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNull(eq("hortonworks"), eq("envir"))).thenReturn(sdxClusters);
        List<SdxCluster> sdxList = sdxService.listSdx(USER_CRN, "envir");
        Assertions.assertEquals(2, sdxList.size());
    }

    @Test
    void testListSdxWhenCrnIsProvided() {
        ArrayList<SdxCluster> sdxClusters = new ArrayList<>();
        sdxClusters.add(new SdxCluster());
        sdxClusters.add(new SdxCluster());
        when(sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNull(eq("hortonworks"), eq(ENVIRONMENT_CRN))).thenReturn(sdxClusters);
        List<SdxCluster> sdxList = sdxService.listSdxByEnvCrn(USER_CRN, ENVIRONMENT_CRN);
        Assertions.assertEquals(2, sdxList.size());
    }

    @Test
    void listSdxWhenInvalidCrnProvided() {
        String crn = "crsdfadsfdsf sadasf3-df81ae585e10";
        Assertions.assertThrows(BadRequestException.class, () -> sdxService.listSdx(crn, "envir"));
    }

    @Test
    void deleteSdxWhenNotFound() {
        Assertions.assertThrows(com.sequenceiq.cloudbreak.exception.NotFoundException.class,
                () -> sdxService.deleteSdx(USER_CRN, "test-sdx-cluster"));
        verify(sdxClusterRepository, times(1))
                .findByAccountIdAndClusterNameAndDeletedIsNull(eq("hortonworks"), eq("test-sdx-cluster"));
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
        CloudPlatform cp = CloudPlatform.YARN;
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
        assertNotNull("Bp name should be defined in all templates", lightDutyStackrequest.getCluster().getBlueprintName());
    }

    @Test
    void deleteSdxWhenCorrectNameIsProvided() {
        SdxCluster sdxCluster = new SdxCluster();
        long sdxId = 2L;
        sdxCluster.setId(sdxId);
        sdxCluster.setClusterName("sdx-cluster-name");
        when(sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(anyString(), anyString())).thenReturn(Optional.of(sdxCluster));
        sdxService.deleteSdx(USER_CRN, "envir");
        verify(sdxReactorFlowManager, times(1)).triggerSdxDeletion(sdxId);
        final ArgumentCaptor<SdxCluster> captor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository, times(1)).save(captor.capture());
        SdxClusterStatus sdxClusterStatus = captor.getValue().getStatus();
        Assertions.assertEquals(SdxClusterStatus.DELETE_REQUESTED, sdxClusterStatus);
    }
}
