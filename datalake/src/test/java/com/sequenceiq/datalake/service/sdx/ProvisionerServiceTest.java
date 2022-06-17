package com.sequenceiq.datalake.service.sdx;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;

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
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
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
    private SdxService sdxService;

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

    @Mock
    private CloudbreakPoller cloudbreakPoller;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @InjectMocks
    private ProvisionerService underTest;

    @Test
    void startProvisioning() {
        long clusterId = CLUSTER_ID.incrementAndGet();
        SdxCluster sdxCluster = generateValidSdxCluster(clusterId);
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setId(1L);
        when(stackV4Endpoint.getByCrn(anyLong(), nullable(String.class), nullable(Set.class))).thenThrow(new NotFoundException());
        when(stackV4Endpoint.postInternal(anyLong(), any(StackV4Request.class), nullable(String.class))).thenReturn(stackV4Response);
        when(sdxService.getById(clusterId)).thenReturn(sdxCluster);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datahub:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.startStackProvisioning(clusterId, getEnvironmentResponse()));

        verify(cloudbreakFlowService).saveLastCloudbreakFlowChainId(sdxCluster, stackV4Response.getFlowIdentifier());
        verify(sdxClusterRepository, times(1)).save(any(SdxCluster.class));
    }

    @Test
    void startProvisioningSdxNotFound() {
        long clusterId = CLUSTER_ID.incrementAndGet();
        when(sdxService.getById(clusterId)).thenThrow(new com.sequenceiq.cloudbreak.common.exception.NotFoundException("not found"));

        Assertions.assertThrows(com.sequenceiq.cloudbreak.common.exception.NotFoundException.class,
                () -> underTest.startStackProvisioning(clusterId, getEnvironmentResponse()));

        verifyNoInteractions(cloudbreakFlowService);
    }

    @Test
    void waitCloudbreakClusterCreationFailedByTimeout() {
        long clusterId = CLUSTER_ID.incrementAndGet();
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(clusterId);
        sdxCluster.setClusterShape(SdxClusterShape.MEDIUM_DUTY_HA);
        sdxCluster.setEnvName("envir");
        sdxCluster.setAccountId("hortonworks");
        sdxCluster.setTags(Json.silent(new HashMap<>()));

        when(sdxService.getById(clusterId)).thenReturn(sdxCluster);
        PollingConfig pollingConfig = new PollingConfig(10, TimeUnit.MILLISECONDS, 100, TimeUnit.MILLISECONDS);
        doThrow(new PollerStoppedException("Stopped."))
                .when(cloudbreakPoller).pollCreateUntilAvailable(sdxCluster, pollingConfig);

        Assertions.assertThrows(PollerStoppedException.class, () -> underTest.waitCloudbreakClusterCreation(clusterId, pollingConfig));

        verify(sdxStatusService, times(1))
                .setStatusForDatalakeAndNotify(DatalakeStatusEnum.STACK_CREATION_IN_PROGRESS, "Datalake stack creation in progress", sdxCluster);
    }

    @Test
    void waitCloudbreakClusterCreationFailedByFailedStack() {
        long clusterId = CLUSTER_ID.incrementAndGet();
        SdxCluster sdxCluster = generateValidSdxCluster(clusterId);
        PollingConfig pollingConfig = new PollingConfig(10, TimeUnit.MILLISECONDS, 500, TimeUnit.MILLISECONDS);
        doThrow(new UserBreakException("Stack creation failedStack creation failed"))
                .when(cloudbreakPoller).pollCreateUntilAvailable(sdxCluster, pollingConfig);
        when(sdxService.getById(clusterId)).thenReturn(sdxCluster);

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
        when(stackV4Endpoint.get(anyLong(), eq(sdxCluster.getClusterName()), anySet(), anyString())).thenReturn(stackV4Response);
        when(sdxService.getById(clusterId)).thenReturn(sdxCluster);
        PollingConfig pollingConfig = new PollingConfig(10, TimeUnit.MILLISECONDS, 1000, TimeUnit.MILLISECONDS);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datahub:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        underTest.waitCloudbreakClusterCreation(clusterId, pollingConfig);

        verify(sdxStatusService, times(1))
                .setStatusForDatalakeAndNotify(DatalakeStatusEnum.STACK_CREATION_IN_PROGRESS, "Datalake stack creation in progress", sdxCluster);
        verify(sdxStatusService, times(1))
                .setStatusForDatalakeAndNotify(DatalakeStatusEnum.STACK_CREATION_FINISHED, "Stack created for Datalake", sdxCluster);
    }

    @Test
    void startStackDeletionStackNotFound() {
        long clusterId = CLUSTER_ID.incrementAndGet();
        SdxCluster sdxCluster = generateValidSdxCluster(clusterId);
        when(sdxService.getById(clusterId)).thenReturn(sdxCluster);
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.CREATE_FAILED);
        doThrow(new NotFoundException()).when(stackV4Endpoint).deleteInternal(anyLong(), eq(sdxCluster.getClusterName()), eq(Boolean.FALSE),
                nullable(String.class));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datahub:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        underTest.startStackDeletion(clusterId, false);

        verify(stackV4Endpoint).deleteInternal(eq(0L), eq(null), eq(false), nullable(String.class));
    }

    @Test
    void startForcedStackDeletionStackFound() {
        long clusterId = CLUSTER_ID.incrementAndGet();
        SdxCluster sdxCluster = generateValidSdxCluster(clusterId);
        when(sdxService.getById(clusterId)).thenReturn(sdxCluster);
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.CREATE_FAILED);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datahub:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        underTest.startStackDeletion(clusterId, true);

        verify(stackV4Endpoint).deleteInternal(eq(0L), eq(null), eq(true), nullable(String.class));
    }

    @Test
    void startStackDeletionButClientError() {
        long clusterId = CLUSTER_ID.incrementAndGet();
        SdxCluster sdxCluster = generateValidSdxCluster(clusterId);
        when(sdxService.getById(clusterId)).thenReturn(sdxCluster);
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.CREATE_FAILED);
        WebApplicationException webApplicationException = new WebApplicationException();
        doThrow(webApplicationException).when(stackV4Endpoint).deleteInternal(anyLong(), eq(sdxCluster.getClusterName()),
                eq(Boolean.FALSE), nullable(String.class));
        when(webApplicationExceptionMessageExtractor.getErrorMessage(webApplicationException)).thenReturn("web-error");
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datahub:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        RuntimeException actual = Assertions.assertThrows(RuntimeException.class, () -> underTest.startStackDeletion(clusterId, false));
        Assertions.assertEquals("Cannot delete cluster, error happened during the operation: web-error", actual.getMessage());

        verify(stackV4Endpoint).deleteInternal(eq(0L), eq(null), eq(false), nullable(String.class));
    }

    @Test
    void waitCloudbreakClusterDeletionButTimeout() {
        long clusterId = CLUSTER_ID.incrementAndGet();
        SdxCluster sdxCluster = generateValidSdxCluster(clusterId);
        when(sdxService.getById(clusterId)).thenReturn(sdxCluster);
        StackV4Response firstStackV4Response = new StackV4Response();
        firstStackV4Response.setStatus(Status.AVAILABLE);
        when(stackV4Endpoint.get(anyLong(), eq(sdxCluster.getClusterName()), anySet(), anyString())).thenReturn(firstStackV4Response);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datahub:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        PollingConfig pollingConfig = new PollingConfig(10, TimeUnit.MILLISECONDS, 200, TimeUnit.MILLISECONDS);

        Assertions.assertThrows(PollerStoppedException.class, () -> underTest.waitCloudbreakClusterDeletion(clusterId, pollingConfig));
    }

    @Test
    void waitCloudbreakClusterDeletionButFailed() {
        long clusterId = CLUSTER_ID.incrementAndGet();
        SdxCluster sdxCluster = generateValidSdxCluster(clusterId);
        when(sdxService.getById(clusterId)).thenReturn(sdxCluster);
        StackV4Response firstStackV4Response = new StackV4Response();
        firstStackV4Response.setStatus(Status.AVAILABLE);
        StackV4Response secondStackV4Response = new StackV4Response();
        secondStackV4Response.setStatus(Status.DELETE_FAILED);
        when(stackV4Endpoint.get(anyLong(), eq(sdxCluster.getClusterName()), anySet(), anyString())).thenReturn(firstStackV4Response)
                .thenReturn(secondStackV4Response);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datahub:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        PollingConfig pollingConfig = new PollingConfig(10, TimeUnit.MILLISECONDS, 500, TimeUnit.MILLISECONDS);

        Assertions.assertThrows(UserBreakException.class, () -> underTest.waitCloudbreakClusterDeletion(clusterId, pollingConfig));
    }

    @Test
    void waitCloudbreakClusterDeletionSuccessful() {
        long clusterId = CLUSTER_ID.incrementAndGet();
        SdxCluster sdxCluster = generateValidSdxCluster(clusterId);
        when(sdxService.getById(clusterId)).thenReturn(sdxCluster);
        when(stackV4Endpoint.get(anyLong(), eq(sdxCluster.getClusterName()), anySet(), anyString())).thenThrow(new NotFoundException());
        PollingConfig pollingConfig = new PollingConfig(10, TimeUnit.MILLISECONDS, 200, TimeUnit.MILLISECONDS);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datahub:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        underTest.waitCloudbreakClusterDeletion(clusterId, pollingConfig);

        verify(sdxStatusService, times(1))
                .setStatusForDatalakeAndNotify(DatalakeStatusEnum.STACK_DELETED, "Datalake stack deleted", sdxCluster);
    }

    @Test
    void waitCloudbreakClusterDeletionClusterRetryFailedTest() {
        long clusterId = CLUSTER_ID.incrementAndGet();
        SdxCluster sdxCluster = generateValidSdxCluster(clusterId);
        sdxCluster.setClusterName("sdxcluster1");
        when(sdxService.getById(clusterId)).thenReturn(sdxCluster);

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

        when(stackV4Endpoint.get(anyLong(), eq(sdxCluster.getClusterName()), anySet(), anyString()))
                .thenReturn(firstStackV4Response)
                .thenReturn(secondStackV4Response)
                .thenReturn(thirdStackV4Response);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datahub:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        PollingConfig pollingConfig = new PollingConfig(10, TimeUnit.MILLISECONDS, 500, TimeUnit.MILLISECONDS);

        Assertions.assertThrows(UserBreakException.class, () -> underTest.waitCloudbreakClusterDeletion(clusterId, pollingConfig),
                "Data lake deletion failed 'sdxcluster1', delete failed");
        verify(stackV4Endpoint, times(5)).get(anyLong(), eq(sdxCluster.getClusterName()), anySet(), anyString());
    }

    @Test
    void waitCloudbreakClusterDeletionStackRetryFailedTest() {
        long clusterId = CLUSTER_ID.incrementAndGet();
        SdxCluster sdxCluster = generateValidSdxCluster(clusterId);
        sdxCluster.setClusterName("sdxcluster1");
        when(sdxService.getById(clusterId)).thenReturn(sdxCluster);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datahub:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
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

        when(stackV4Endpoint.get(anyLong(), eq(sdxCluster.getClusterName()), anySet(), anyString()))
                .thenReturn(firstStackV4Response)
                .thenReturn(secondStackV4Response)
                .thenThrow(new NotFoundException());

        PollingConfig pollingConfig = new PollingConfig(10, TimeUnit.MILLISECONDS, 1000, TimeUnit.MILLISECONDS);

        underTest.waitCloudbreakClusterDeletion(clusterId, pollingConfig);

        verify(sdxStatusService, times(1))
                .setStatusForDatalakeAndNotify(DatalakeStatusEnum.STACK_DELETED, "Datalake stack deleted", sdxCluster);
        verify(stackV4Endpoint, times(3)).get(anyLong(), eq(sdxCluster.getClusterName()), anySet(), anyString());
    }

    private DetailedEnvironmentResponse getEnvironmentResponse() {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setName("env");
        detailedEnvironmentResponse.setEnvironmentStatus(EnvironmentStatus.AVAILABLE);
        CompactRegionResponse compactRegionResponse = new CompactRegionResponse();
        compactRegionResponse.setNames(Lists.newArrayList("eu-west-1"));
        compactRegionResponse.setDisplayNames(Map.of("eu-west-1", "ireland"));
        detailedEnvironmentResponse.setRegions(compactRegionResponse);
        detailedEnvironmentResponse.setCrn(CrnTestUtil.getEnvironmentCrnBuilder()
                .setResource(UUID.randomUUID().toString())
                .setAccountId(UUID.randomUUID().toString())
                .build().toString());
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        network.setCrn(CrnTestUtil.getNetworkCrnBuilder()
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
        sdxCluster.setAccountId("hortonworks");
        sdxCluster.setTags(Json.silent(new HashMap<>()));
        sdxCluster.setEnvCrn("");
        sdxCluster.setStackRequestToCloudbreak("{}");
        sdxCluster.setCrn(CrnTestUtil.getDatalakeCrnBuilder()
                .setAccountId("asd")
                .setResource("asd")
                .build().toString());
        return sdxCluster;
    }
}
