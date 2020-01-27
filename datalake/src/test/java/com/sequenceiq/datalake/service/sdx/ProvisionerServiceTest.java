package com.sequenceiq.datalake.service.sdx;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams;
import com.sequenceiq.environment.api.v1.environment.model.response.CompactRegionResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentAuthenticationResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerStatusV4Response;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@ExtendWith(MockitoExtension.class)
@DisplayName("SDX provisioner service tests")
class ProvisionerServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:hortonworks:user:perdos@hortonworks.com";

    private static final long CLUSTER_ID = 2L;

    private static final boolean HAS_RUNNING_FLOW = true;

    private static final boolean NO_RUNNING_FLOW = false;

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
        SdxCluster sdxCluster = generateValidSdxCluster(CLUSTER_ID);
        StackV4Response stackV4Response = new StackV4Response();
        when(stackV4Endpoint.post(anyLong(), any(StackV4Request.class))).thenReturn(stackV4Response);
        when(sdxClusterRepository.findById(CLUSTER_ID)).thenReturn(Optional.of(sdxCluster));

        underTest.startStackProvisioning(CLUSTER_ID, getEnvironmentResponse(), getDatabaseServerResponse());

        verify(cloudbreakFlowService).getAndSaveLastCloudbreakFlowChainId(sdxCluster);
        verify(sdxClusterRepository, times(1)).save(any(SdxCluster.class));
    }

    @Test
    void startProvisioningSdxNotFound() {
        when(sdxClusterRepository.findById(CLUSTER_ID)).thenReturn(Optional.empty());

        Assertions.assertThrows(com.sequenceiq.cloudbreak.exception.NotFoundException.class,
                () -> underTest.startStackProvisioning(CLUSTER_ID, getEnvironmentResponse(), getDatabaseServerResponse()));

        verifyZeroInteractions(cloudbreakFlowService);
    }

    @Test
    void waitCloudbreakClusterCreationFailedByTimeout() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(CLUSTER_ID);
        sdxCluster.setClusterShape(SdxClusterShape.MEDIUM_DUTY_HA);
        sdxCluster.setEnvName("envir");
        sdxCluster.setInitiatorUserCrn(USER_CRN);
        sdxCluster.setAccountId("hortonworks");
        sdxCluster.setTags(Json.silent(new HashMap<>()));
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.REQUESTED);
        when(stackV4Endpoint.get(anyLong(), eq(sdxCluster.getClusterName()), anySet())).thenReturn(stackV4Response);

        when(sdxClusterRepository.findById(CLUSTER_ID)).thenReturn(Optional.of(sdxCluster));
        PollingConfig pollingConfig = new PollingConfig(10, TimeUnit.MILLISECONDS, 100, TimeUnit.MILLISECONDS);

        Assertions.assertThrows(PollerStoppedException.class, () -> underTest.waitCloudbreakClusterCreation(CLUSTER_ID, pollingConfig));

        verify(sdxStatusService, times(1))
                .setStatusForDatalakeAndNotify(DatalakeStatusEnum.STACK_CREATION_IN_PROGRESS,
                        ResourceEvent.SDX_CLUSTER_PROVISION_STARTED, "Datalake stack creation in progress", sdxCluster);
    }

    @Test
    void waitCloudbreakClusterCreationFailedByFailedStack() {
        SdxCluster sdxCluster = generateValidSdxCluster(CLUSTER_ID);
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.CREATE_FAILED);
        when(stackV4Endpoint.get(anyLong(), eq(sdxCluster.getClusterName()), anySet())).thenReturn(stackV4Response);
        when(sdxClusterRepository.findById(CLUSTER_ID)).thenReturn(Optional.of(sdxCluster));
        PollingConfig pollingConfig = new PollingConfig(10, TimeUnit.MILLISECONDS, 500, TimeUnit.MILLISECONDS);

        Assertions.assertThrows(UserBreakException.class, () -> underTest
                .waitCloudbreakClusterCreation(CLUSTER_ID, pollingConfig), "Stack creation failed");

        verify(sdxStatusService, times(1))
                .setStatusForDatalakeAndNotify(DatalakeStatusEnum.STACK_CREATION_IN_PROGRESS,
                        ResourceEvent.SDX_CLUSTER_PROVISION_STARTED, "Datalake stack creation in progress", sdxCluster);
    }

    @Test
    void waitCloudbreakClusterCreationSuccess() {
        SdxCluster sdxCluster = generateValidSdxCluster(CLUSTER_ID);
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.AVAILABLE);
        ClusterV4Response cluster = new ClusterV4Response();
        cluster.setStatus(Status.AVAILABLE);
        stackV4Response.setCluster(cluster);
        when(cloudbreakFlowService.isLastKnownFlowRunning(sdxCluster))
                .thenReturn(HAS_RUNNING_FLOW)
                .thenReturn(NO_RUNNING_FLOW);
        when(stackV4Endpoint.get(anyLong(), eq(sdxCluster.getClusterName()), anySet())).thenReturn(stackV4Response);
        when(sdxClusterRepository.findById(CLUSTER_ID)).thenReturn(Optional.of(sdxCluster));
        PollingConfig pollingConfig = new PollingConfig(10, TimeUnit.MILLISECONDS, 1000, TimeUnit.MILLISECONDS);

        underTest.waitCloudbreakClusterCreation(CLUSTER_ID, pollingConfig);

        verify(cloudbreakFlowService, times(2)).isLastKnownFlowRunning(sdxCluster);
        verify(sdxStatusService, times(1))
                .setStatusForDatalakeAndNotify(DatalakeStatusEnum.STACK_CREATION_IN_PROGRESS,
                        ResourceEvent.SDX_CLUSTER_PROVISION_STARTED, "Datalake stack creation in progress", sdxCluster);
        verify(sdxStatusService, times(1))
                .setStatusForDatalakeAndNotify(DatalakeStatusEnum.STACK_CREATION_FINISHED,
                        ResourceEvent.SDX_CLUSTER_PROVISION_FINISHED, "Stack created for Datalake", sdxCluster);
    }

    @Test
    void startStackDeletionStackNotFound() {
        SdxCluster sdxCluster = generateValidSdxCluster(CLUSTER_ID);
        when(sdxClusterRepository.findById(CLUSTER_ID)).thenReturn(Optional.of(sdxCluster));
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.CREATE_FAILED);
        doThrow(new NotFoundException()).when(stackV4Endpoint).delete(anyLong(), eq(sdxCluster.getClusterName()), eq(Boolean.FALSE));

        underTest.startStackDeletion(CLUSTER_ID, false);

        verify(stackV4Endpoint).delete(0L, null, false);
    }

    @Test
    void startForcedStackDeletionStackFound() {
        SdxCluster sdxCluster = generateValidSdxCluster(CLUSTER_ID);
        when(sdxClusterRepository.findById(CLUSTER_ID)).thenReturn(Optional.of(sdxCluster));
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.CREATE_FAILED);
        doNothing().when(stackV4Endpoint).delete(anyLong(), eq(sdxCluster.getClusterName()), eq(Boolean.TRUE));

        underTest.startStackDeletion(CLUSTER_ID, true);

        verify(stackV4Endpoint).delete(0L, null, true);
    }

    @Test
    void startStackDeletionButClientError() {
        SdxCluster sdxCluster = generateValidSdxCluster(CLUSTER_ID);
        when(sdxClusterRepository.findById(CLUSTER_ID)).thenReturn(Optional.of(sdxCluster));
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.CREATE_FAILED);
        doThrow(new InternalServerErrorException())
                .when(stackV4Endpoint).delete(anyLong(), eq(sdxCluster.getClusterName()), eq(Boolean.FALSE));

        Assertions.assertThrows(InternalServerErrorException.class, () -> underTest.startStackDeletion(CLUSTER_ID, false));

        verify(stackV4Endpoint).delete(0L, null, false);
    }

    @Test
    void waitCloudbreakClusterDeletionButTimeout() {
        SdxCluster sdxCluster = generateValidSdxCluster(CLUSTER_ID);
        when(sdxClusterRepository.findById(CLUSTER_ID)).thenReturn(Optional.of(sdxCluster));
        StackV4Response firstStackV4Response = new StackV4Response();
        firstStackV4Response.setStatus(Status.AVAILABLE);
        when(stackV4Endpoint.get(anyLong(), eq(sdxCluster.getClusterName()), anySet())).thenReturn(firstStackV4Response);
        PollingConfig pollingConfig = new PollingConfig(10, TimeUnit.MILLISECONDS, 200, TimeUnit.MILLISECONDS);

        Assertions.assertThrows(PollerStoppedException.class, () -> underTest.waitCloudbreakClusterDeletion(CLUSTER_ID, pollingConfig));
    }

    @Test
    void waitCloudbreakClusterDeletionButFailed() {
        SdxCluster sdxCluster = generateValidSdxCluster(CLUSTER_ID);
        when(sdxClusterRepository.findById(CLUSTER_ID)).thenReturn(Optional.of(sdxCluster));
        StackV4Response firstStackV4Response = new StackV4Response();
        firstStackV4Response.setStatus(Status.AVAILABLE);
        StackV4Response secondStackV4Response = new StackV4Response();
        secondStackV4Response.setStatus(Status.DELETE_FAILED);
        when(stackV4Endpoint.get(anyLong(), eq(sdxCluster.getClusterName()), anySet())).thenReturn(firstStackV4Response)
                .thenReturn(secondStackV4Response);
        PollingConfig pollingConfig = new PollingConfig(10, TimeUnit.MILLISECONDS, 200, TimeUnit.MILLISECONDS);

        Assertions.assertThrows(UserBreakException.class, () -> underTest.waitCloudbreakClusterDeletion(CLUSTER_ID, pollingConfig));
    }

    @Test
    void waitCloudbreakClusterDeletionSuccessful() {
        SdxCluster sdxCluster = generateValidSdxCluster(CLUSTER_ID);
        when(sdxClusterRepository.findById(CLUSTER_ID)).thenReturn(Optional.of(sdxCluster));
        when(stackV4Endpoint.get(anyLong(), eq(sdxCluster.getClusterName()), anySet())).thenThrow(new NotFoundException());
        PollingConfig pollingConfig = new PollingConfig(10, TimeUnit.MILLISECONDS, 200, TimeUnit.MILLISECONDS);

        underTest.waitCloudbreakClusterDeletion(CLUSTER_ID, pollingConfig);

        verify(sdxStatusService, times(1))
                .setStatusForDatalakeAndNotify(DatalakeStatusEnum.STACK_DELETED,
                        ResourceEvent.SDX_CLUSTER_DELETION_FINISHED, "Datalake stack deleted", sdxCluster);
    }

    private DatabaseServerStatusV4Response getDatabaseServerResponse() {
        return new DatabaseServerStatusV4Response();
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
