package com.sequenceiq.datalake.service.sdx;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.CloudbreakFlowService.FlowState;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams;
import com.sequenceiq.environment.api.v1.environment.model.response.CompactRegionResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentAuthenticationResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@ExtendWith(MockitoExtension.class)
@DisplayName("SDX provisioner service tests")
class ProvisionerServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:hortonworks:user:perdos@hortonworks.com";

    private static final AtomicLong CLUSTER_ID = new AtomicLong(10000L);

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private StackRequestManifester stackRequestManifester;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private CloudbreakFlowService cloudbreakFlowService;

    @Mock
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @InjectMocks
    private ProvisionerService underTest;

    @Test
    void startProvisioning() {
        long clusterId = CLUSTER_ID.incrementAndGet();
        SdxCluster sdxCluster = generateValidSdxCluster(clusterId);
        StackV4Response stackV4Response = new StackV4Response();
        when(stackV4Endpoint.get(anyLong(), nullable(String.class), nullable(Set.class))).thenThrow(new NotFoundException());
        when(stackV4Endpoint.post(anyLong(), any(StackV4Request.class))).thenReturn(stackV4Response);
        when(sdxClusterRepository.findById(clusterId)).thenReturn(Optional.of(sdxCluster));

        underTest.startStackProvisioning(clusterId, getEnvironmentResponse());

        verify(cloudbreakFlowService).saveLastCloudbreakFlowChainId(sdxCluster, stackV4Response.getFlowIdentifier());
        verify(sdxClusterRepository, times(1)).save(any(SdxCluster.class));
    }

    @Test
    void startProvisioningSdxNotFound() {
        long clusterId = CLUSTER_ID.incrementAndGet();
        when(sdxClusterRepository.findById(clusterId)).thenReturn(Optional.empty());

        Assertions.assertThrows(com.sequenceiq.cloudbreak.exception.NotFoundException.class,
                () -> underTest.startStackProvisioning(clusterId, getEnvironmentResponse()));

        verifyZeroInteractions(cloudbreakFlowService);
    }

    @Test
    void waitCloudbreakClusterCreationFailedByTimeout() {
        long clusterId = CLUSTER_ID.incrementAndGet();
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(clusterId);
        sdxCluster.setClusterShape(SdxClusterShape.MEDIUM_DUTY_HA);
        sdxCluster.setEnvName("envir");
        sdxCluster.setInitiatorUserCrn(USER_CRN);
        sdxCluster.setAccountId("hortonworks");
        sdxCluster.setTags(Json.silent(new HashMap<>()));
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.REQUESTED);
        when(stackV4Endpoint.get(anyLong(), eq(sdxCluster.getClusterName()), anySet())).thenReturn(stackV4Response);

        when(sdxClusterRepository.findById(clusterId)).thenReturn(Optional.of(sdxCluster));
        PollingConfig pollingConfig = new PollingConfig(10, TimeUnit.MILLISECONDS, 100, TimeUnit.MILLISECONDS);

        Assertions.assertThrows(PollerStoppedException.class, () -> underTest.waitCloudbreakClusterCreation(clusterId, pollingConfig));

        verify(sdxStatusService, times(1))
                .setStatusForDatalakeAndNotify(DatalakeStatusEnum.STACK_CREATION_IN_PROGRESS, "Datalake stack creation in progress", sdxCluster);
    }

    @Test
    void waitCloudbreakClusterCreationFailedByFailedStack() {
        long clusterId = CLUSTER_ID.incrementAndGet();
        SdxCluster sdxCluster = generateValidSdxCluster(clusterId);
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.CREATE_FAILED);
        when(stackV4Endpoint.get(anyLong(), eq(sdxCluster.getClusterName()), anySet())).thenReturn(stackV4Response);
        when(sdxClusterRepository.findById(clusterId)).thenReturn(Optional.of(sdxCluster));
        PollingConfig pollingConfig = new PollingConfig(10, TimeUnit.MILLISECONDS, 500, TimeUnit.MILLISECONDS);

        Assertions.assertThrows(UserBreakException.class, () -> underTest
                .waitCloudbreakClusterCreation(clusterId, pollingConfig), "Stack creation failed");

        verify(sdxStatusService, times(1))
                .setStatusForDatalakeAndNotify(DatalakeStatusEnum.STACK_CREATION_IN_PROGRESS, "Datalake stack creation in progress", sdxCluster);
    }

    @Test
    void waitCloudbreakClusterCreationSuccess() {
        long clusterId = CLUSTER_ID.incrementAndGet();
        SdxCluster sdxCluster = generateValidSdxCluster(clusterId);
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.AVAILABLE);
        ClusterV4Response cluster = new ClusterV4Response();
        cluster.setStatus(Status.AVAILABLE);
        stackV4Response.setCluster(cluster);
        when(cloudbreakFlowService.getLastKnownFlowState(sdxCluster))
                .thenReturn(FlowState.RUNNING)
                .thenReturn(FlowState.FINISHED);
        when(stackV4Endpoint.get(anyLong(), eq(sdxCluster.getClusterName()), anySet())).thenReturn(stackV4Response);
        when(sdxClusterRepository.findById(clusterId)).thenReturn(Optional.of(sdxCluster));
        PollingConfig pollingConfig = new PollingConfig(10, TimeUnit.MILLISECONDS, 1000, TimeUnit.MILLISECONDS);

        underTest.waitCloudbreakClusterCreation(clusterId, pollingConfig);

        verify(cloudbreakFlowService, times(2)).getLastKnownFlowState(sdxCluster);
        verify(sdxStatusService, times(1))
                .setStatusForDatalakeAndNotify(DatalakeStatusEnum.STACK_CREATION_IN_PROGRESS, "Datalake stack creation in progress", sdxCluster);
        verify(sdxStatusService, times(1))
                .setStatusForDatalakeAndNotify(DatalakeStatusEnum.STACK_CREATION_FINISHED, "Stack created for Datalake", sdxCluster);
    }

    @Test
    void startStackDeletionStackNotFound() {
        long clusterId = CLUSTER_ID.incrementAndGet();
        SdxCluster sdxCluster = generateValidSdxCluster(clusterId);
        when(sdxClusterRepository.findById(clusterId)).thenReturn(Optional.of(sdxCluster));
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.CREATE_FAILED);
        doThrow(new NotFoundException()).when(stackV4Endpoint).delete(anyLong(), eq(sdxCluster.getClusterName()), eq(Boolean.FALSE));

        underTest.startStackDeletion(clusterId, false);

        verify(stackV4Endpoint).delete(0L, null, false);
    }

    @Test
    void startForcedStackDeletionStackFound() {
        long clusterId = CLUSTER_ID.incrementAndGet();
        SdxCluster sdxCluster = generateValidSdxCluster(clusterId);
        when(sdxClusterRepository.findById(clusterId)).thenReturn(Optional.of(sdxCluster));
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.CREATE_FAILED);

        underTest.startStackDeletion(clusterId, true);

        verify(stackV4Endpoint).delete(0L, null, true);
    }

    @Test
    void startStackDeletionButClientError() {
        long clusterId = CLUSTER_ID.incrementAndGet();
        SdxCluster sdxCluster = generateValidSdxCluster(clusterId);
        when(sdxClusterRepository.findById(clusterId)).thenReturn(Optional.of(sdxCluster));
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.CREATE_FAILED);
        doThrow(new InternalServerErrorException())
                .when(stackV4Endpoint).delete(anyLong(), eq(sdxCluster.getClusterName()), eq(Boolean.FALSE));

        Assertions.assertThrows(InternalServerErrorException.class, () -> underTest.startStackDeletion(clusterId, false));

        verify(stackV4Endpoint).delete(0L, null, false);
    }

    @Test
    void waitCloudbreakClusterDeletionButTimeout() {
        long clusterId = CLUSTER_ID.incrementAndGet();
        SdxCluster sdxCluster = generateValidSdxCluster(clusterId);
        when(sdxClusterRepository.findById(clusterId)).thenReturn(Optional.of(sdxCluster));
        StackV4Response firstStackV4Response = new StackV4Response();
        firstStackV4Response.setStatus(Status.AVAILABLE);
        when(stackV4Endpoint.get(anyLong(), eq(sdxCluster.getClusterName()), anySet())).thenReturn(firstStackV4Response);
        PollingConfig pollingConfig = new PollingConfig(10, TimeUnit.MILLISECONDS, 200, TimeUnit.MILLISECONDS);

        Assertions.assertThrows(PollerStoppedException.class, () -> underTest.waitCloudbreakClusterDeletion(clusterId, pollingConfig));
    }

    @Test
    void waitCloudbreakClusterDeletionButFailed() {
        long clusterId = CLUSTER_ID.incrementAndGet();
        SdxCluster sdxCluster = generateValidSdxCluster(clusterId);
        when(sdxClusterRepository.findById(clusterId)).thenReturn(Optional.of(sdxCluster));
        StackV4Response firstStackV4Response = new StackV4Response();
        firstStackV4Response.setStatus(Status.AVAILABLE);
        StackV4Response secondStackV4Response = new StackV4Response();
        secondStackV4Response.setStatus(Status.DELETE_FAILED);
        when(stackV4Endpoint.get(anyLong(), eq(sdxCluster.getClusterName()), anySet())).thenReturn(firstStackV4Response)
                .thenReturn(secondStackV4Response);
        PollingConfig pollingConfig = new PollingConfig(10, TimeUnit.MILLISECONDS, 500, TimeUnit.MILLISECONDS);

        Assertions.assertThrows(UserBreakException.class, () -> underTest.waitCloudbreakClusterDeletion(clusterId, pollingConfig));
    }

    @Test
    void waitCloudbreakClusterDeletionSuccessful() {
        long clusterId = CLUSTER_ID.incrementAndGet();
        SdxCluster sdxCluster = generateValidSdxCluster(clusterId);
        when(sdxClusterRepository.findById(clusterId)).thenReturn(Optional.of(sdxCluster));
        when(stackV4Endpoint.get(anyLong(), eq(sdxCluster.getClusterName()), anySet())).thenThrow(new NotFoundException());
        PollingConfig pollingConfig = new PollingConfig(10, TimeUnit.MILLISECONDS, 200, TimeUnit.MILLISECONDS);

        underTest.waitCloudbreakClusterDeletion(clusterId, pollingConfig);

        verify(sdxStatusService, times(1))
                .setStatusForDatalakeAndNotify(DatalakeStatusEnum.STACK_DELETED, "Datalake stack deleted", sdxCluster);
    }

    @Test
    void waitCloudbreakClusterDeletionClusterRetryFailedTest() {
        long clusterId = CLUSTER_ID.incrementAndGet();
        SdxCluster sdxCluster = generateValidSdxCluster(clusterId);
        sdxCluster.setClusterName("sdxcluster1");
        when(sdxClusterRepository.findById(clusterId)).thenReturn(Optional.of(sdxCluster));

        StackV4Response firstStackV4Response = new StackV4Response();
        firstStackV4Response.setStatus(Status.DELETE_IN_PROGRESS);
        ClusterV4Response firstClusterResponse = new ClusterV4Response();
        firstClusterResponse.setStatus(Status.DELETE_IN_PROGRESS);
        firstClusterResponse.setStatusReason("delete failed");
        firstStackV4Response.setCluster(firstClusterResponse);

        StackV4Response secondStackV4Response = new StackV4Response();
        secondStackV4Response.setStatus(Status.DELETE_FAILED);
        ClusterV4Response secondClusterResponse = new ClusterV4Response();
        secondClusterResponse.setStatus(Status.DELETE_IN_PROGRESS);
        secondClusterResponse.setStatusReason("delete failed");
        secondStackV4Response.setCluster(secondClusterResponse);

        StackV4Response thirdStackV4Response = new StackV4Response();
        thirdStackV4Response.setStatus(Status.DELETE_FAILED);
        ClusterV4Response thirdClusterResponse = new ClusterV4Response();
        thirdClusterResponse.setStatus(Status.DELETE_FAILED);
        thirdClusterResponse.setStatusReason("delete failed");
        thirdStackV4Response.setCluster(thirdClusterResponse);

        when(stackV4Endpoint.get(anyLong(), eq(sdxCluster.getClusterName()), anySet()))
                .thenReturn(firstStackV4Response)
                .thenReturn(secondStackV4Response)
                .thenReturn(thirdStackV4Response);

        PollingConfig pollingConfig = new PollingConfig(10, TimeUnit.MILLISECONDS, 500, TimeUnit.MILLISECONDS);

        Assertions.assertThrows(UserBreakException.class, () -> underTest.waitCloudbreakClusterDeletion(clusterId, pollingConfig),
                "Data lake deletion failed 'sdxcluster1', delete failed");
        verify(stackV4Endpoint, times(5)).get(anyLong(), eq(sdxCluster.getClusterName()), anySet());
    }

    @Test
    void waitCloudbreakClusterDeletionStackRetryFailedTest() {
        long clusterId = CLUSTER_ID.incrementAndGet();
        SdxCluster sdxCluster = generateValidSdxCluster(clusterId);
        sdxCluster.setClusterName("sdxcluster1");
        when(sdxClusterRepository.findById(clusterId)).thenReturn(Optional.of(sdxCluster));

        StackV4Response firstStackV4Response = new StackV4Response();
        firstStackV4Response.setStatus(Status.DELETE_FAILED);
        ClusterV4Response firstClusterResponse = new ClusterV4Response();
        firstClusterResponse.setStatus(Status.DELETE_IN_PROGRESS);
        firstStackV4Response.setCluster(firstClusterResponse);

        StackV4Response secondStackV4Response = new StackV4Response();
        secondStackV4Response.setStatus(Status.DELETE_FAILED);
        ClusterV4Response secondClusterResponse = new ClusterV4Response();
        secondClusterResponse.setStatus(Status.DELETE_COMPLETED);
        secondStackV4Response.setCluster(secondClusterResponse);

        when(stackV4Endpoint.get(anyLong(), eq(sdxCluster.getClusterName()), anySet()))
                .thenReturn(firstStackV4Response)
                .thenReturn(secondStackV4Response)
                .thenThrow(new NotFoundException());

        PollingConfig pollingConfig = new PollingConfig(10, TimeUnit.MILLISECONDS, 1000, TimeUnit.MILLISECONDS);

        underTest.waitCloudbreakClusterDeletion(clusterId, pollingConfig);

        verify(sdxStatusService, times(1))
                .setStatusForDatalakeAndNotify(DatalakeStatusEnum.STACK_DELETED, "Datalake stack deleted", sdxCluster);
        verify(stackV4Endpoint, times(3)).get(anyLong(), eq(sdxCluster.getClusterName()), anySet());
    }

    private DetailedEnvironmentResponse getEnvironmentResponse() {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setName("env");
        detailedEnvironmentResponse.setEnvironmentStatus(EnvironmentStatus.AVAILABLE);
        CompactRegionResponse compactRegionResponse = new CompactRegionResponse();
        compactRegionResponse.setNames(Lists.newArrayList("eu-west-1"));
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
        Map<String, CloudSubnet> cloudSubnetMap = Map.of("subnet", cloudSubnet);
        network.setSubnetMetas(cloudSubnetMap);
        detailedEnvironmentResponse.setNetwork(network);
        EnvironmentAuthenticationResponse authentication = new EnvironmentAuthenticationResponse();
        authentication.setPublicKey("ssh-public-key");
        detailedEnvironmentResponse.setAuthentication(authentication);
        return detailedEnvironmentResponse;
    }

    private SdxCluster generateValidSdxCluster(long id) {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(id);
        sdxCluster.setClusterShape(SdxClusterShape.MEDIUM_DUTY_HA);
        sdxCluster.setEnvName("envir");
        sdxCluster.setInitiatorUserCrn(USER_CRN);
        sdxCluster.setAccountId("hortonworks");
        sdxCluster.setTags(Json.silent(new HashMap<>()));
        sdxCluster.setEnvCrn("");
        sdxCluster.setStackRequestToCloudbreak("{}");
        return sdxCluster;
    }
}
