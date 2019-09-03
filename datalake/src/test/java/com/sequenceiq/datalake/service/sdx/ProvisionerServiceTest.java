package com.sequenceiq.datalake.service.sdx;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
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
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams;
import com.sequenceiq.environment.api.v1.environment.model.response.CompactRegionResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentAuthenticationResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.environment.client.EnvironmentServiceCrnEndpoints;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerStatusV4Response;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@ExtendWith(MockitoExtension.class)
@DisplayName("SDX provisioner service tests")
class ProvisionerServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:hortonworks:user:perdos@hortonworks.com";

    private static final String REQUEST_ID = "requestId";

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private Clock clock;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private EnvironmentServiceCrnEndpoints environmentServiceCrnEndpoints;

    @Mock
    private EnvironmentEndpoint environmentEndpoint;

    @Mock
    private StackRequestManifester stackRequestManifester;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private GatewayManifester gatewayManifester;

    @Mock
    private SdxNotificationService notificationService;

    @InjectMocks
    private ProvisionerService provisionerService;

    @BeforeEach
    void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void startProvisioning() {
        long id = 2L;
        SdxCluster sdxCluster = generateValidSdxCluster(id);
        StackV4Response stackV4Response = new StackV4Response();
        when(stackV4Endpoint.post(anyLong(), any(StackV4Request.class))).thenReturn(stackV4Response);
        when(sdxClusterRepository.findById(id)).thenReturn(Optional.of(sdxCluster));
        provisionerService.startStackProvisioning(id, getEnvironmentResponse(), getDatabaseServerResponse());
        verify(sdxClusterRepository, times(1)).save(any(SdxCluster.class));
        verify(notificationService).send(eq(ResourceEvent.SDX_CLUSTER_PROVISION_STARTED), any());
    }

    @Test
    void startProvisioningSdxNotFound() {
        long id = 2L;

        when(sdxClusterRepository.findById(id)).thenReturn(Optional.empty());
        Assertions.assertThrows(com.sequenceiq.cloudbreak.exception.NotFoundException.class,
                () -> provisionerService.startStackProvisioning(id, getEnvironmentResponse(), getDatabaseServerResponse()));
    }

    @Test
    void waitCloudbreakClusterCreationFailedByTimeout() {
        long id = 2L;
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(id);
        sdxCluster.setClusterShape(SdxClusterShape.MEDIUM_DUTY_HA);
        sdxCluster.setEnvName("envir");
        sdxCluster.setInitiatorUserCrn(USER_CRN);
        sdxCluster.setAccountId("hortonworks");
        sdxCluster.setTags(Json.silent(new HashMap<>()));

        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.REQUESTED);
        when(stackV4Endpoint.get(anyLong(), eq(sdxCluster.getClusterName()), anySet())).thenReturn(stackV4Response);

        when(sdxClusterRepository.findById(2L)).thenReturn(Optional.of(sdxCluster));
        PollingConfig pollingConfig = new PollingConfig(10, TimeUnit.MILLISECONDS, 100, TimeUnit.MILLISECONDS);
        Assertions.assertThrows(PollerStoppedException.class, () -> provisionerService.waitCloudbreakClusterCreation(id, pollingConfig, REQUEST_ID));
        verify(sdxStatusService, times(1))
                .setStatusForDatalake(DatalakeStatusEnum.STACK_CREATION_IN_PROGRESS, "Datalake stack creation in progress", sdxCluster);
    }

    @Test
    void waitCloudbreakClusterCreationFailedByFailedStack() {
        long id = 2L;
        SdxCluster sdxCluster = generateValidSdxCluster(id);

        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.CREATE_FAILED);
        when(stackV4Endpoint.get(anyLong(), eq(sdxCluster.getClusterName()), anySet())).thenReturn(stackV4Response);

        when(sdxClusterRepository.findById(2L)).thenReturn(Optional.of(sdxCluster));
        PollingConfig pollingConfig = new PollingConfig(10, TimeUnit.MILLISECONDS, 500, TimeUnit.MILLISECONDS);
        Assertions.assertThrows(UserBreakException.class, () -> provisionerService
                .waitCloudbreakClusterCreation(id, pollingConfig, REQUEST_ID), "Stack creation failed");
        verify(notificationService).send(eq(ResourceEvent.SDX_CLUSTER_CREATION_FAILED), any());
        verify(sdxStatusService, times(1))
                .setStatusForDatalake(DatalakeStatusEnum.STACK_CREATION_IN_PROGRESS, "Datalake stack creation in progress", sdxCluster);
    }

    @Test
    void waitCloudbreakClusterCreationSuccess() {
        long id = 2L;
        SdxCluster sdxCluster = generateValidSdxCluster(id);

        StackV4Response firstStackV4Response = new StackV4Response();
        firstStackV4Response.setStatus(Status.UPDATE_IN_PROGRESS);

        StackV4Response secondStackV4Response = new StackV4Response();
        secondStackV4Response.setStatus(Status.AVAILABLE);
        ClusterV4Response cluster = new ClusterV4Response();
        cluster.setStatus(Status.AVAILABLE);
        secondStackV4Response.setCluster(cluster);

        when(stackV4Endpoint.get(anyLong(), eq(sdxCluster.getClusterName()), anySet())).thenReturn(firstStackV4Response)
                .thenReturn(secondStackV4Response);

        when(sdxClusterRepository.findById(2L)).thenReturn(Optional.of(sdxCluster));
        PollingConfig pollingConfig = new PollingConfig(10, TimeUnit.MILLISECONDS, 1000, TimeUnit.MILLISECONDS);
        provisionerService.waitCloudbreakClusterCreation(id, pollingConfig, REQUEST_ID);
        final ArgumentCaptor<SdxCluster> captor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository, times(1)).save(captor.capture());
        SdxCluster savedSdxCluster = captor.getValue();
        verify(sdxStatusService, times(1))
                .setStatusForDatalake(DatalakeStatusEnum.STACK_CREATION_IN_PROGRESS, "Datalake stack creation in progress", sdxCluster);
        verify(sdxStatusService, times(1))
                .setStatusForDatalake(DatalakeStatusEnum.RUNNING, "Datalake is running", savedSdxCluster);
        verify(notificationService).send(eq(ResourceEvent.SDX_CLUSTER_PROVISION_FINISHED), any());
        verify(sdxStatusService, times(1))
                .setStatusForDatalake(DatalakeStatusEnum.STACK_CREATION_IN_PROGRESS, "Datalake stack creation in progress", sdxCluster);
    }

    @Test
    void startStackDeletionStackNotFound() {
        long id = 2L;
        SdxCluster sdxCluster = generateValidSdxCluster(id);
        when(sdxClusterRepository.findById(id)).thenReturn(Optional.of(sdxCluster));
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.CREATE_FAILED);

        doThrow(new NotFoundException()).when(stackV4Endpoint).delete(anyLong(), eq(sdxCluster.getClusterName()), eq(Boolean.FALSE));

        provisionerService.startStackDeletion(id);
        verify(stackV4Endpoint).delete(0L, null, false);
    }

    @Test
    void startStackDeletionStackFound() {
        long id = 2L;
        SdxCluster sdxCluster = generateValidSdxCluster(id);
        when(sdxClusterRepository.findById(id)).thenReturn(Optional.of(sdxCluster));
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.CREATE_FAILED);

        doNothing().when(stackV4Endpoint).delete(anyLong(), eq(sdxCluster.getClusterName()), eq(Boolean.FALSE));

        provisionerService.startStackDeletion(id);
        verify(stackV4Endpoint).delete(0L, null, false);
    }

    @Test
    void startStackDeletionButClientError() {
        long id = 2L;
        SdxCluster sdxCluster = generateValidSdxCluster(id);
        when(sdxClusterRepository.findById(id)).thenReturn(Optional.of(sdxCluster));
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.CREATE_FAILED);

        doThrow(new InternalServerErrorException())
                .when(stackV4Endpoint).delete(anyLong(), eq(sdxCluster.getClusterName()), eq(Boolean.FALSE));

        Assertions.assertThrows(InternalServerErrorException.class, () -> provisionerService.startStackDeletion(id));
        verify(stackV4Endpoint).delete(0L, null, false);
    }

    @Test
    void waitCloudbreakClusterDeletionButTimeout() {
        long id = 2L;
        SdxCluster sdxCluster = generateValidSdxCluster(id);
        when(sdxClusterRepository.findById(id)).thenReturn(Optional.of(sdxCluster));
        StackV4Response firstStackV4Response = new StackV4Response();
        firstStackV4Response.setStatus(Status.AVAILABLE);

        when(stackV4Endpoint.get(anyLong(), eq(sdxCluster.getClusterName()), anySet())).thenReturn(firstStackV4Response);

        PollingConfig pollingConfig = new PollingConfig(10, TimeUnit.MILLISECONDS, 200, TimeUnit.MILLISECONDS);
        Assertions.assertThrows(PollerStoppedException.class, () -> provisionerService.waitCloudbreakClusterDeletion(id, pollingConfig, REQUEST_ID));
    }

    @Test
    void waitCloudbreakClusterDeletionButFailed() {
        long id = 2L;
        SdxCluster sdxCluster = generateValidSdxCluster(id);
        when(sdxClusterRepository.findById(id)).thenReturn(Optional.of(sdxCluster));
        StackV4Response firstStackV4Response = new StackV4Response();
        firstStackV4Response.setStatus(Status.AVAILABLE);
        StackV4Response secondStackV4Response = new StackV4Response();
        secondStackV4Response.setStatus(Status.DELETE_FAILED);

        when(stackV4Endpoint.get(anyLong(), eq(sdxCluster.getClusterName()), anySet())).thenReturn(firstStackV4Response)
                .thenReturn(secondStackV4Response);

        PollingConfig pollingConfig = new PollingConfig(10, TimeUnit.MILLISECONDS, 200, TimeUnit.MILLISECONDS);
        Assertions.assertThrows(UserBreakException.class, () -> provisionerService.waitCloudbreakClusterDeletion(id, pollingConfig, REQUEST_ID));
        verify(notificationService).send(eq(ResourceEvent.SDX_CLUSTER_DELETION_FAILED), any());
    }

    @Test
    void waitCloudbreakClusterDeletionSuccessful() {
        long id = 2L;
        SdxCluster sdxCluster = generateValidSdxCluster(id);
        when(sdxClusterRepository.findById(id)).thenReturn(Optional.of(sdxCluster));
        when(stackV4Endpoint.get(anyLong(), eq(sdxCluster.getClusterName()), anySet())).thenThrow(new NotFoundException());
        PollingConfig pollingConfig = new PollingConfig(10, TimeUnit.MILLISECONDS, 200, TimeUnit.MILLISECONDS);

        provisionerService.waitCloudbreakClusterDeletion(id, pollingConfig, REQUEST_ID);

        final ArgumentCaptor<SdxCluster> captor = ArgumentCaptor.forClass(SdxCluster.class);
        verify(sdxClusterRepository, times(1)).save(captor.capture());
        SdxCluster postedSdxCluster = captor.getValue();

        verify(sdxStatusService, times(1))
                .setStatusForDatalake(DatalakeStatusEnum.STACK_DELETED, "Datalake deleted", postedSdxCluster);
        verify(notificationService).send(eq(ResourceEvent.SDX_CLUSTER_DELETION_FINISHED), any());
    }

    private DatabaseServerStatusV4Response getDatabaseServerResponse() {
        return new DatabaseServerStatusV4Response();
    }

    private DetailedEnvironmentResponse getEnvironmentResponse() {
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
