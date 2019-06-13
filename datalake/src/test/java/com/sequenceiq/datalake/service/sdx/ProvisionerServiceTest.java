package com.sequenceiq.datalake.service.sdx;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;

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

import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.client.CloudbreakUserCrnClient;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxClusterStatus;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams;
import com.sequenceiq.environment.api.v1.environment.model.response.CompactRegionResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.environment.client.EnvironmentServiceClient;
import com.sequenceiq.environment.client.EnvironmentServiceEndpoints;

@ExtendWith(MockitoExtension.class)
@DisplayName("SDX provisioner service tests")
class ProvisionerServiceTest {

    private static final String CRN = "crn:altus:iam:us-west-1:hortonworks:user:perdos@hortonworks.com";

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private CloudbreakUserCrnClient cloudbreakClient;

    @Mock
    private Clock clock;

    @Mock
    private EnvironmentServiceClient environmentServiceClient;

    @Mock
    private EnvironmentServiceEndpoints environmentServiceEndpoints;

    @Mock
    private EnvironmentEndpoint environmentEndpoint;

    @InjectMocks
    private ProvisionerService provisionerService;

    @BeforeEach
    void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void startProvisioning() {
        long id = 2L;
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(id);
        sdxCluster.setClusterShape("big");
        sdxCluster.setStatus(SdxClusterStatus.REQUESTED);
        sdxCluster.setEnvName("envir");
        sdxCluster.setInitiatorUserCrn(CRN);
        sdxCluster.setAccessCidr("0.0.0.0/0");
        sdxCluster.setAccountId("hortonworks");
        sdxCluster.setTags(Json.silent(new HashMap<>()));
        sdxCluster.setEnvCrn("");

        CloudbreakUserCrnClient.CloudbreakEndpoint cbEndpointMock = mock(CloudbreakUserCrnClient.CloudbreakEndpoint.class);
        StackV4Endpoint stackEndpointMock = mock(StackV4Endpoint.class);
        StackV4Response stackV4Response = new StackV4Response();
        long stackIdFromCB = 100L;
        stackV4Response.setId(stackIdFromCB);
        when(stackEndpointMock.post(anyLong(), any(StackV4Request.class))).thenReturn(stackV4Response);
        when(cbEndpointMock.stackV4Endpoint()).thenReturn(stackEndpointMock);
        when(cloudbreakClient.withCrn(anyString())).thenReturn(cbEndpointMock);
        when(sdxClusterRepository.findById(id)).thenReturn(Optional.of(sdxCluster));
        mockEnvironmentCall();
        provisionerService.startStackProvisioning(id);
        final ArgumentCaptor<SdxCluster> captor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository, times(2)).save(captor.capture());
        SdxCluster postedSdxCluster = captor.getValue();
        Assertions.assertEquals(SdxClusterStatus.REQUESTED_FROM_CLOUDBREAK, postedSdxCluster.getStatus());
        Assertions.assertEquals(stackIdFromCB, postedSdxCluster.getStackId());
    }

    @Test
    void startProvisioningSdxNotFound() {
        long id = 2L;

        when(sdxClusterRepository.findById(id)).thenReturn(Optional.empty());
        Assertions.assertThrows(BadRequestException.class, () -> provisionerService.startStackProvisioning(id), "Can not find SDX cluster by ID: 2");
    }

    @Test
    void waitCloudbreakClusterCreationFailedByTimeout() {
        long id = 2L;
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(id);
        sdxCluster.setClusterShape("big");
        sdxCluster.setStatus(SdxClusterStatus.REQUESTED);
        sdxCluster.setEnvName("envir");
        sdxCluster.setInitiatorUserCrn(CRN);
        sdxCluster.setAccessCidr("0.0.0.0/0");
        sdxCluster.setAccountId("hortonworks");
        sdxCluster.setTags(Json.silent(new HashMap<>()));

        CloudbreakUserCrnClient.CloudbreakEndpoint cbEndpointMock = mock(CloudbreakUserCrnClient.CloudbreakEndpoint.class);
        StackV4Endpoint stackEndpointMock = mock(StackV4Endpoint.class);
        StackV4Response stackV4Response = new StackV4Response();
        long stackIdFromCB = 100L;
        stackV4Response.setId(stackIdFromCB);
        stackV4Response.setStatus(Status.REQUESTED);
        when(stackEndpointMock.get(anyLong(), eq(sdxCluster.getClusterName()), anySet())).thenReturn(stackV4Response);
        when(cbEndpointMock.stackV4Endpoint()).thenReturn(stackEndpointMock);
        when(cloudbreakClient.withCrn(anyString())).thenReturn(cbEndpointMock);

        when(sdxClusterRepository.findById(2L)).thenReturn(Optional.of(sdxCluster));
        PollingConfig pollingConfig = new PollingConfig(10, TimeUnit.MILLISECONDS, 100, TimeUnit.MILLISECONDS);
        Assertions.assertThrows(PollerStoppedException.class, () -> provisionerService.waitCloudbreakClusterCreation(id, pollingConfig));
    }

    @Test
    void waitCloudbreakClusterCreationFailedByFailedStack() {
        long id = 2L;
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(id);
        sdxCluster.setClusterShape("big");
        sdxCluster.setStatus(SdxClusterStatus.REQUESTED);
        sdxCluster.setEnvName("envir");
        sdxCluster.setInitiatorUserCrn(CRN);
        sdxCluster.setAccessCidr("0.0.0.0/0");
        sdxCluster.setAccountId("hortonworks");
        sdxCluster.setTags(Json.silent(new HashMap<>()));

        CloudbreakUserCrnClient.CloudbreakEndpoint cbEndpointMock = mock(CloudbreakUserCrnClient.CloudbreakEndpoint.class);
        StackV4Endpoint stackEndpointMock = mock(StackV4Endpoint.class);
        StackV4Response stackV4Response = new StackV4Response();
        long stackIdFromCB = 100L;
        stackV4Response.setId(stackIdFromCB);
        stackV4Response.setStatus(Status.CREATE_FAILED);
        when(stackEndpointMock.get(anyLong(), eq(sdxCluster.getClusterName()), anySet())).thenReturn(stackV4Response);
        when(cbEndpointMock.stackV4Endpoint()).thenReturn(stackEndpointMock);
        when(cloudbreakClient.withCrn(anyString())).thenReturn(cbEndpointMock);

        when(sdxClusterRepository.findById(2L)).thenReturn(Optional.of(sdxCluster));
        PollingConfig pollingConfig = new PollingConfig(10, TimeUnit.MILLISECONDS, 500, TimeUnit.MILLISECONDS);
        Assertions.assertThrows(UserBreakException.class, () -> provisionerService.waitCloudbreakClusterCreation(id, pollingConfig), "Stack creation failed");
    }

    @Test
    void waitCloudbreakClusterCreationSuccess() {
        long id = 2L;
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(id);
        sdxCluster.setClusterShape("big");
        sdxCluster.setStatus(SdxClusterStatus.REQUESTED);
        sdxCluster.setEnvName("envir");
        sdxCluster.setInitiatorUserCrn(CRN);
        sdxCluster.setAccessCidr("0.0.0.0/0");
        sdxCluster.setAccountId("hortonworks");
        sdxCluster.setTags(Json.silent(new HashMap<>()));

        CloudbreakUserCrnClient.CloudbreakEndpoint cbEndpointMock = mock(CloudbreakUserCrnClient.CloudbreakEndpoint.class);
        StackV4Endpoint stackEndpointMock = mock(StackV4Endpoint.class);

        long stackIdFromCB = 100L;

        StackV4Response firstStackV4Response = new StackV4Response();
        firstStackV4Response.setId(stackIdFromCB);
        firstStackV4Response.setStatus(Status.UPDATE_IN_PROGRESS);

        StackV4Response secondStackV4Response = new StackV4Response();
        secondStackV4Response.setId(stackIdFromCB);
        secondStackV4Response.setStatus(Status.AVAILABLE);
        ClusterV4Response cluster = new ClusterV4Response();
        cluster.setStatus(Status.AVAILABLE);
        secondStackV4Response.setCluster(cluster);

        when(stackEndpointMock.get(anyLong(), eq(sdxCluster.getClusterName()), anySet())).thenReturn(firstStackV4Response).thenReturn(secondStackV4Response);
        when(cbEndpointMock.stackV4Endpoint()).thenReturn(stackEndpointMock);
        when(cloudbreakClient.withCrn(anyString())).thenReturn(cbEndpointMock);

        when(sdxClusterRepository.findById(2L)).thenReturn(Optional.of(sdxCluster));
        PollingConfig pollingConfig = new PollingConfig(10, TimeUnit.MILLISECONDS, 1000, TimeUnit.MILLISECONDS);
        provisionerService.waitCloudbreakClusterCreation(id, pollingConfig);
        final ArgumentCaptor<SdxCluster> captor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository, times(1)).save(captor.capture());
        SdxCluster savedSdxCluster = captor.getValue();
        Assertions.assertEquals(SdxClusterStatus.RUNNING, savedSdxCluster.getStatus());
    }

    @Test
    void startStackDeletionStackNotFound() {
        long id = 2L;
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(id);
        sdxCluster.setClusterShape("big");
        sdxCluster.setStatus(SdxClusterStatus.REQUESTED);
        sdxCluster.setEnvName("envir");
        sdxCluster.setInitiatorUserCrn(CRN);
        sdxCluster.setAccessCidr("0.0.0.0/0");
        sdxCluster.setAccountId("hortonworks");
        sdxCluster.setTags(Json.silent(new HashMap<>()));
        sdxCluster.setClusterName("envir-sdx-cluster");
        when(sdxClusterRepository.findById(id)).thenReturn(Optional.of(sdxCluster));

        CloudbreakUserCrnClient.CloudbreakEndpoint cbEndpointMock = mock(CloudbreakUserCrnClient.CloudbreakEndpoint.class);
        StackV4Endpoint stackEndpointMock = mock(StackV4Endpoint.class);
        StackV4Response stackV4Response = new StackV4Response();
        long stackIdFromCB = 100L;
        stackV4Response.setId(stackIdFromCB);
        stackV4Response.setStatus(Status.CREATE_FAILED);

        doThrow(new NotFoundException()).when(stackEndpointMock).delete(anyLong(), eq(sdxCluster.getClusterName()), eq(Boolean.FALSE), eq(Boolean.FALSE));

        when(cbEndpointMock.stackV4Endpoint()).thenReturn(stackEndpointMock);
        when(cloudbreakClient.withCrn(anyString())).thenReturn(cbEndpointMock);

        provisionerService.startStackDeletion(id);

        verify(cloudbreakClient).withCrn(anyString());
    }

    @Test
    void startStackDeletionStackFound() {
        long id = 2L;
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(id);
        sdxCluster.setClusterShape("big");
        sdxCluster.setStatus(SdxClusterStatus.REQUESTED);
        sdxCluster.setEnvName("envir");
        sdxCluster.setInitiatorUserCrn(CRN);
        sdxCluster.setAccessCidr("0.0.0.0/0");
        sdxCluster.setAccountId("hortonworks");
        sdxCluster.setTags(Json.silent(new HashMap<>()));
        sdxCluster.setClusterName("envir-sdx-cluster");
        when(sdxClusterRepository.findById(id)).thenReturn(Optional.of(sdxCluster));

        CloudbreakUserCrnClient.CloudbreakEndpoint cbEndpointMock = mock(CloudbreakUserCrnClient.CloudbreakEndpoint.class);
        StackV4Endpoint stackEndpointMock = mock(StackV4Endpoint.class);
        StackV4Response stackV4Response = new StackV4Response();
        long stackIdFromCB = 100L;
        stackV4Response.setId(stackIdFromCB);
        stackV4Response.setStatus(Status.CREATE_FAILED);

        doNothing().when(stackEndpointMock).delete(anyLong(), eq(sdxCluster.getClusterName()), eq(Boolean.FALSE), eq(Boolean.FALSE));

        when(cbEndpointMock.stackV4Endpoint()).thenReturn(stackEndpointMock);
        when(cloudbreakClient.withCrn(anyString())).thenReturn(cbEndpointMock);

        provisionerService.startStackDeletion(id);

        verify(cloudbreakClient).withCrn(anyString());
    }

    @Test
    void startStackDeletionButClientError() {
        long id = 2L;
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(id);
        sdxCluster.setClusterShape("big");
        sdxCluster.setStatus(SdxClusterStatus.REQUESTED);
        sdxCluster.setEnvName("envir");
        sdxCluster.setInitiatorUserCrn(CRN);
        sdxCluster.setAccessCidr("0.0.0.0/0");
        sdxCluster.setAccountId("hortonworks");
        sdxCluster.setTags(Json.silent(new HashMap<>()));
        sdxCluster.setClusterName("envir-sdx-cluster");
        when(sdxClusterRepository.findById(id)).thenReturn(Optional.of(sdxCluster));

        CloudbreakUserCrnClient.CloudbreakEndpoint cbEndpointMock = mock(CloudbreakUserCrnClient.CloudbreakEndpoint.class);
        StackV4Endpoint stackEndpointMock = mock(StackV4Endpoint.class);
        StackV4Response stackV4Response = new StackV4Response();
        long stackIdFromCB = 100L;
        stackV4Response.setId(stackIdFromCB);
        stackV4Response.setStatus(Status.CREATE_FAILED);

        doThrow(new InternalServerErrorException())
                .when(stackEndpointMock).delete(anyLong(), eq(sdxCluster.getClusterName()), eq(Boolean.FALSE), eq(Boolean.FALSE));

        when(cbEndpointMock.stackV4Endpoint()).thenReturn(stackEndpointMock);
        when(cloudbreakClient.withCrn(anyString())).thenReturn(cbEndpointMock);

        Assertions.assertThrows(InternalServerErrorException.class, () -> provisionerService.startStackDeletion(id));

        verify(cloudbreakClient).withCrn(anyString());
    }

    @Test
    void waitCloudbreakClusterDeletionButTimeout() {
        long id = 2L;
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(id);
        sdxCluster.setClusterShape("big");
        sdxCluster.setStatus(SdxClusterStatus.REQUESTED);
        sdxCluster.setEnvName("envir");
        sdxCluster.setInitiatorUserCrn(CRN);
        sdxCluster.setAccessCidr("0.0.0.0/0");
        sdxCluster.setAccountId("hortonworks");
        sdxCluster.setTags(Json.silent(new HashMap<>()));
        sdxCluster.setClusterName("envir-sdx-cluster");
        when(sdxClusterRepository.findById(id)).thenReturn(Optional.of(sdxCluster));

        long stackIdFromCB = 100L;

        CloudbreakUserCrnClient.CloudbreakEndpoint cbEndpointMock = mock(CloudbreakUserCrnClient.CloudbreakEndpoint.class);
        StackV4Endpoint stackEndpointMock = mock(StackV4Endpoint.class);

        StackV4Response firstStackV4Response = new StackV4Response();
        firstStackV4Response.setId(stackIdFromCB);
        firstStackV4Response.setStatus(Status.AVAILABLE);

        when(stackEndpointMock.get(anyLong(), eq(sdxCluster.getClusterName()), anySet())).thenReturn(firstStackV4Response);
        when(cbEndpointMock.stackV4Endpoint()).thenReturn(stackEndpointMock);
        when(cloudbreakClient.withCrn(anyString())).thenReturn(cbEndpointMock);

        PollingConfig pollingConfig = new PollingConfig(10, TimeUnit.MILLISECONDS, 200, TimeUnit.MILLISECONDS);
        Assertions.assertThrows(PollerStoppedException.class, () -> provisionerService.waitCloudbreakClusterDeletion(id, pollingConfig));
    }

    @Test
    void waitCloudbreakClusterDeletionButFailed() {
        long id = 2L;
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(id);
        sdxCluster.setClusterShape("big");
        sdxCluster.setStatus(SdxClusterStatus.REQUESTED);
        sdxCluster.setEnvName("envir");
        sdxCluster.setInitiatorUserCrn(CRN);
        sdxCluster.setAccessCidr("0.0.0.0/0");
        sdxCluster.setAccountId("hortonworks");
        sdxCluster.setTags(Json.silent(new HashMap<>()));
        sdxCluster.setClusterName("envir-sdx-cluster");
        when(sdxClusterRepository.findById(id)).thenReturn(Optional.of(sdxCluster));

        long stackIdFromCB = 100L;

        CloudbreakUserCrnClient.CloudbreakEndpoint cbEndpointMock = mock(CloudbreakUserCrnClient.CloudbreakEndpoint.class);
        StackV4Endpoint stackEndpointMock = mock(StackV4Endpoint.class);

        StackV4Response firstStackV4Response = new StackV4Response();
        firstStackV4Response.setId(stackIdFromCB);
        firstStackV4Response.setStatus(Status.AVAILABLE);

        StackV4Response secondStackV4Response = new StackV4Response();
        secondStackV4Response.setId(stackIdFromCB);
        secondStackV4Response.setStatus(Status.DELETE_FAILED);

        when(stackEndpointMock.get(anyLong(), eq(sdxCluster.getClusterName()), anySet())).thenReturn(firstStackV4Response).thenReturn(secondStackV4Response);
        when(cbEndpointMock.stackV4Endpoint()).thenReturn(stackEndpointMock);
        when(cloudbreakClient.withCrn(anyString())).thenReturn(cbEndpointMock);

        PollingConfig pollingConfig = new PollingConfig(10, TimeUnit.MILLISECONDS, 200, TimeUnit.MILLISECONDS);
        Assertions.assertThrows(UserBreakException.class, () -> provisionerService.waitCloudbreakClusterDeletion(id, pollingConfig));
    }

    @Test
    void waitCloudbreakClusterDeletionSuccessful() {
        long id = 2L;
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(id);
        sdxCluster.setClusterShape("big");
        sdxCluster.setStatus(SdxClusterStatus.REQUESTED);
        sdxCluster.setEnvName("envir");
        sdxCluster.setInitiatorUserCrn(CRN);
        sdxCluster.setAccessCidr("0.0.0.0/0");
        sdxCluster.setAccountId("hortonworks");
        sdxCluster.setTags(Json.silent(new HashMap<>()));
        sdxCluster.setClusterName("envir-sdx-cluster");
        when(sdxClusterRepository.findById(id)).thenReturn(Optional.of(sdxCluster));

        CloudbreakUserCrnClient.CloudbreakEndpoint cbEndpointMock = mock(CloudbreakUserCrnClient.CloudbreakEndpoint.class);
        StackV4Endpoint stackEndpointMock = mock(StackV4Endpoint.class);

        when(stackEndpointMock.get(anyLong(), eq(sdxCluster.getClusterName()), anySet())).thenThrow(new NotFoundException());
        when(cbEndpointMock.stackV4Endpoint()).thenReturn(stackEndpointMock);
        when(cloudbreakClient.withCrn(anyString())).thenReturn(cbEndpointMock);

        PollingConfig pollingConfig = new PollingConfig(10, TimeUnit.MILLISECONDS, 200, TimeUnit.MILLISECONDS);
        provisionerService.waitCloudbreakClusterDeletion(id, pollingConfig);

        final ArgumentCaptor<SdxCluster> captor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository, times(1)).save(captor.capture());
        SdxCluster postedSdxCluster = captor.getValue();

        Assertions.assertEquals(SdxClusterStatus.DELETED, postedSdxCluster.getStatus());
    }

    private void mockEnvironmentCall() {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setName("env");
        detailedEnvironmentResponse.setEnvironmentStatus(EnvironmentStatus.AVAILABLE);
        CompactRegionResponse compactRegionResponse = new CompactRegionResponse();
        compactRegionResponse.setNames(Sets.newHashSet("eu-west-1"));
        compactRegionResponse.setDisplayNames(Map.of("eu-west-1", "ireland"));
        detailedEnvironmentResponse.setRegions(compactRegionResponse);
        detailedEnvironmentResponse.setCrn(Crn.builder()
                .setService(Crn.Service.ENVIRONMENTS)
                .setResourceType(Crn.ResourceType.ENVIRONMENT)
                .setResource(UUID.randomUUID().toString())
                .setAccountId(UUID.randomUUID().toString())
                .build().toString());
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        network.setCrn(Crn.builder()
                .setService(Crn.Service.ENVIRONMENTS)
                .setResourceType(Crn.ResourceType.NETWORK)
                .setResource(UUID.randomUUID().toString())
                .setAccountId(UUID.randomUUID().toString())
                .build().toString());
        EnvironmentNetworkAwsParams environmentNetworkAwsParams = new EnvironmentNetworkAwsParams();
        environmentNetworkAwsParams.setVpcId("vpc");
        network.setAws(environmentNetworkAwsParams);
        network.setSubnetIds(Sets.newHashSet("subnet"));
        CloudSubnet cloudSubnet = new CloudSubnet();
        cloudSubnet.setId("subnet");
        cloudSubnet.setName("subnet");
        cloudSubnet.setAvailabilityZone("eu-west-1a");
        Map<String, CloudSubnet> cloudSubnetMap = Map.of("subnet",  cloudSubnet);
        network.setSubnetMetas(cloudSubnetMap);
        detailedEnvironmentResponse.setNetwork(network);
        when(environmentServiceClient.withCrn(anyString())).thenReturn(environmentServiceEndpoints);
        when(environmentServiceEndpoints.environmentV1Endpoint()).thenReturn(environmentEndpoint);
        when(environmentEndpoint.getByCrn(anyString())).thenReturn(detailedEnvironmentResponse);
    }
}