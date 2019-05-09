package com.sequenceiq.datalake.service.sdx;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.client.CloudbreakUserCrnClient;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxClusterStatus;
import com.sequenceiq.datalake.repository.SdxClusterRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SDX provisioner service tests")
class ProvisionerServiceTest {

    public static final String CRN = "crn:altus:iam:us-west-1:hortonworks:user:perdos@hortonworks.com";

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private CloudbreakUserCrnClient cloudbreakClient;

    @InjectMocks
    private ProvisionerService provisionerService;

    @BeforeEach
    public void initMocks() {
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

        CloudbreakUserCrnClient.CloudbreakEndpoint cbEndpointMock = mock(CloudbreakUserCrnClient.CloudbreakEndpoint.class);
        StackV4Endpoint stackEndpointMock = mock(StackV4Endpoint.class);
        StackV4Response stackV4Response = new StackV4Response();
        long stackIdFromCB = 100L;
        stackV4Response.setId(stackIdFromCB);
        when(stackEndpointMock.post(anyLong(), any(StackV4Request.class))).thenReturn(stackV4Response);
        when(cbEndpointMock.stackV4Endpoint()).thenReturn(stackEndpointMock);
        when(cloudbreakClient.withCrn(anyString())).thenReturn(cbEndpointMock);
        when(sdxClusterRepository.findById(id)).thenReturn(Optional.of(sdxCluster));
        provisionerService.startProvisioning(id);
        final ArgumentCaptor<SdxCluster> captor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository, times(1)).save(captor.capture());
        SdxCluster postedSdxCluster = captor.getValue();
        Assertions.assertEquals(SdxClusterStatus.REQUESTED_FROM_CLOUDBREAK, postedSdxCluster.getStatus());
        Assertions.assertEquals(stackIdFromCB, postedSdxCluster.getStackId());
    }

    @Test
    void startProvisioningSdxNotFound() {
        long id = 2L;

        when(sdxClusterRepository.findById(id)).thenReturn(Optional.empty());
        Assertions.assertThrows(BadRequestException.class, () -> provisionerService.startProvisioning(id), "Can not find SDX cluster by ID: 2");
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
}