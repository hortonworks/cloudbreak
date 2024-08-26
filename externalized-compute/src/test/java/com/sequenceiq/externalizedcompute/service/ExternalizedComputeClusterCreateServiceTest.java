package com.sequenceiq.externalizedcompute.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.liftiepublic.LiftiePublicProto.CreateClusterRequest;
import com.cloudera.thunderhead.service.liftiepublic.LiftiePublicProto.CreateClusterResponse;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.ExternalizedComputeClusterResponse;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeCluster;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeClusterStatusEnum;
import com.sequenceiq.externalizedcompute.repository.ExternalizedComputeClusterRepository;
import com.sequenceiq.liftie.client.LiftieGrpcClient;

@ExtendWith(MockitoExtension.class)
class ExternalizedComputeClusterCreateServiceTest {

    public static final String USER_CRN = "crn:altus:iam:us-west-1:cloudera:user:perdos";

    @Mock
    private LiftieGrpcClient liftieGrpcClient;

    @Mock
    private EnvironmentEndpoint environmentEndpoint;

    @Mock
    private ExternalizedComputeClusterRepository externalizedComputeClusterRepository;

    @Mock
    private ExternalizedComputeClusterStatusService externalizedComputeClusterStatusService;

    @InjectMocks
    private ExternalizedComputeClusterCreateService externalizedComputeClusterCreateService;

    @Test
    void initiateCreationTest() {
        ExternalizedComputeCluster externalizedComputeCluster = new ExternalizedComputeCluster();
        externalizedComputeCluster.setName("cluster-name");
        externalizedComputeCluster.setDefaultCluster(true);
        externalizedComputeCluster.setEnvironmentCrn("envcrn");
        Json tags = new Json(Map.of("label1", "value1"));
        externalizedComputeCluster.setTags(tags);
        CreateClusterResponse createClusterResponse = mock(CreateClusterResponse.class);
        when(createClusterResponse.getClusterId()).thenReturn("liftie-1");
        ArgumentCaptor<CreateClusterRequest> commonCreateClusterRequestArgumentCaptor = ArgumentCaptor.forClass(
                CreateClusterRequest.class);
        when(liftieGrpcClient.createCluster(commonCreateClusterRequestArgumentCaptor.capture(), any())).thenReturn(createClusterResponse);
        when(externalizedComputeClusterRepository.findById(any())).thenReturn(Optional.of(externalizedComputeCluster));
        DetailedEnvironmentResponse environmentResponse = mock(DetailedEnvironmentResponse.class);
        ExternalizedComputeClusterResponse computeClusterResponse = new ExternalizedComputeClusterResponse();
        computeClusterResponse.setWorkerNodeSubnetIds(Set.of("subnetX", "subnetY"));
        when(environmentResponse.getExternalizedComputeCluster()).thenReturn(computeClusterResponse);
        when(environmentEndpoint.getByCrn(externalizedComputeCluster.getEnvironmentCrn())).thenReturn(environmentResponse);
        externalizedComputeClusterCreateService.initiateCreation(1L, USER_CRN);

        CreateClusterRequest clusterRequest = commonCreateClusterRequestArgumentCaptor.getValue();
        assertEquals("envcrn", clusterRequest.getEnvironment());
        assertEquals("cluster-name", clusterRequest.getName());
        assertEquals("Common compute cluster", clusterRequest.getDescription());
        assertThat(clusterRequest.getTagsMap()).containsEntry("label1", "value1");
        assertTrue(clusterRequest.getIsDefault());
        assertThat(clusterRequest.getNetwork().getSubnetsList().stream().toList()).containsExactlyInAnyOrder("subnetX", "subnetY");

        ArgumentCaptor<ExternalizedComputeCluster> externalizedComputeClusterArgumentCaptor = ArgumentCaptor.forClass(ExternalizedComputeCluster.class);
        verify(externalizedComputeClusterRepository, times(1)).save(externalizedComputeClusterArgumentCaptor.capture());
        ExternalizedComputeCluster cluster = externalizedComputeClusterArgumentCaptor.getValue();
        assertEquals("liftie-1", cluster.getLiftieName());
        verify(externalizedComputeClusterStatusService, times(1))
                .setStatus(cluster, ExternalizedComputeClusterStatusEnum.LIFTIE_CLUSTER_CREATION_IN_PROGRESS, "Liftie cluster creation in progress");
    }

    @Test
    void initiateCreationWithoutWorkerNodeSubnetIdsTest() {
        ExternalizedComputeCluster externalizedComputeCluster = new ExternalizedComputeCluster();
        externalizedComputeCluster.setName("cluster-name");
        externalizedComputeCluster.setDefaultCluster(true);
        externalizedComputeCluster.setEnvironmentCrn("envcrn");
        Json tags = new Json(Map.of("label1", "value1"));
        externalizedComputeCluster.setTags(tags);
        CreateClusterResponse createClusterResponse = mock(CreateClusterResponse.class);
        when(createClusterResponse.getClusterId()).thenReturn("liftie-1");
        ArgumentCaptor<CreateClusterRequest> commonCreateClusterRequestArgumentCaptor = ArgumentCaptor.forClass(
                CreateClusterRequest.class);
        when(liftieGrpcClient.createCluster(commonCreateClusterRequestArgumentCaptor.capture(), any())).thenReturn(createClusterResponse);
        when(externalizedComputeClusterRepository.findById(any())).thenReturn(Optional.of(externalizedComputeCluster));
        DetailedEnvironmentResponse environmentResponse = mock(DetailedEnvironmentResponse.class);
        EnvironmentNetworkResponse networkResponse = mock(EnvironmentNetworkResponse.class);
        when(networkResponse.getLiftieSubnets()).thenReturn(Map.of("subnet1", new CloudSubnet(), "subnet2", new CloudSubnet()));
        when(environmentResponse.getNetwork()).thenReturn(networkResponse);
        when(environmentResponse.getExternalizedComputeCluster()).thenReturn(new ExternalizedComputeClusterResponse());
        when(environmentEndpoint.getByCrn(externalizedComputeCluster.getEnvironmentCrn())).thenReturn(environmentResponse);
        externalizedComputeClusterCreateService.initiateCreation(1L, USER_CRN);

        CreateClusterRequest clusterRequest = commonCreateClusterRequestArgumentCaptor.getValue();
        assertEquals("envcrn", clusterRequest.getEnvironment());
        assertEquals("cluster-name", clusterRequest.getName());
        assertEquals("Common compute cluster", clusterRequest.getDescription());
        assertThat(clusterRequest.getTagsMap()).containsEntry("label1", "value1");
        assertTrue(clusterRequest.getIsDefault());
        assertThat(clusterRequest.getNetwork().getSubnetsList().stream().toList()).containsExactlyInAnyOrder("subnet1", "subnet2");

        ArgumentCaptor<ExternalizedComputeCluster> externalizedComputeClusterArgumentCaptor = ArgumentCaptor.forClass(ExternalizedComputeCluster.class);
        verify(externalizedComputeClusterRepository, times(1)).save(externalizedComputeClusterArgumentCaptor.capture());
        ExternalizedComputeCluster cluster = externalizedComputeClusterArgumentCaptor.getValue();
        assertEquals("liftie-1", cluster.getLiftieName());
        verify(externalizedComputeClusterStatusService, times(1))
                .setStatus(cluster, ExternalizedComputeClusterStatusEnum.LIFTIE_CLUSTER_CREATION_IN_PROGRESS, "Liftie cluster creation in progress");
    }

    @Test
    void initiateCreationShouldNotHappenWhenLiftieNameExists() {
        ExternalizedComputeCluster externalizedComputeCluster = new ExternalizedComputeCluster();
        externalizedComputeCluster.setLiftieName("liftie-cluster-name");
        externalizedComputeCluster.setName("cluser-name");
        externalizedComputeCluster.setEnvironmentCrn("envcrn");
        Json tags = new Json(Map.of("label1", "value1"));
        externalizedComputeCluster.setTags(tags);
        when(externalizedComputeClusterRepository.findById(any())).thenReturn(Optional.of(externalizedComputeCluster));
        externalizedComputeClusterCreateService.initiateCreation(1L, USER_CRN);
        verify(liftieGrpcClient, times(0)).createCluster(any(), any());
    }
}