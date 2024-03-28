package com.sequenceiq.externalizedcompute.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.thunderhead.service.liftiepublic.LiftiePublicProto.CommonClusterSpec;
import com.cloudera.thunderhead.service.liftiepublic.LiftiePublicProto.CreateClusterRequest;
import com.cloudera.thunderhead.service.liftiepublic.LiftiePublicProto.CreateClusterResponse;
import com.sequenceiq.cloudbreak.auth.CrnUser;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeCluster;
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
    private CrnUserDetailsService crnUserDetailsService;

    @InjectMocks
    private ExternalizedComputeClusterCreateService externalizedComputeClusterCreateService;

    @Test
    void initiateCreationTest() {
        ReflectionTestUtils.setField(externalizedComputeClusterCreateService, "kubernetesVersion", "1.28");
        ExternalizedComputeCluster externalizedComputeCluster = new ExternalizedComputeCluster();
        externalizedComputeCluster.setName("cluser-name");
        externalizedComputeCluster.setEnvironmentCrn("envcrn");
        Json tags = new Json(Map.of("label1", "value1"));
        externalizedComputeCluster.setTags(tags);
        CreateClusterResponse createClusterResponse = mock(CreateClusterResponse.class);
        when(createClusterResponse.getClusterId()).thenReturn("liftie-1");
        ArgumentCaptor<CreateClusterRequest> commonCreateClusterRequestArgumentCaptor = ArgumentCaptor.forClass(
                CreateClusterRequest.class);
        when(liftieGrpcClient.createCluster(commonCreateClusterRequestArgumentCaptor.capture(), any())).thenReturn(createClusterResponse);
        when(crnUserDetailsService.loadUserByUsername(USER_CRN))
                .thenReturn(new CrnUser("id", USER_CRN, "perdos", "perdos@cloudera.com", "cloudera", "admin"));
        when(externalizedComputeClusterRepository.findById(any())).thenReturn(Optional.of(externalizedComputeCluster));
        DetailedEnvironmentResponse environmentResponse = mock(DetailedEnvironmentResponse.class);
        EnvironmentNetworkResponse networkResponse = mock(EnvironmentNetworkResponse.class);
        when(networkResponse.getLiftieSubnets()).thenReturn(Map.of("subnet1", new CloudSubnet(), "subnet2", new CloudSubnet()));
        when(environmentResponse.getNetwork()).thenReturn(networkResponse);
        when(environmentEndpoint.getByCrn(externalizedComputeCluster.getEnvironmentCrn())).thenReturn(environmentResponse);
        externalizedComputeClusterCreateService.initiateCreation(1L, USER_CRN);

        CreateClusterRequest clusterRequest = commonCreateClusterRequestArgumentCaptor.getValue();
        CommonClusterSpec spec = clusterRequest.getSpec();
        assertEquals("1.28", spec.getKubernetes().getVersion());
        assertThat(spec.getNetwork().getTopology().getSubnetsList().stream().toList()).containsExactlyInAnyOrder("subnet1", "subnet2");
        assertEquals("1.28", spec.getKubernetes().getVersion());
        Assertions.assertTrue(spec.getDeployments().getLogging().getEnabled());
        assertEquals(USER_CRN, clusterRequest.getMetadata().getClusterOwner().getCrn());
        assertEquals("cloudera", clusterRequest.getMetadata().getClusterOwner().getAccountId());
        assertEquals("envcrn", clusterRequest.getMetadata().getEnvironmentCrn());
        assertThat(clusterRequest.getMetadata().getLabels()).containsEntry("label1", "value1");

        ArgumentCaptor<ExternalizedComputeCluster> externalizedComputeClusterArgumentCaptor = ArgumentCaptor.forClass(ExternalizedComputeCluster.class);
        verify(externalizedComputeClusterRepository, times(1)).save(externalizedComputeClusterArgumentCaptor.capture());
        assertEquals("liftie-1", externalizedComputeClusterArgumentCaptor.getValue().getLiftieName());
    }

    @Test
    void initiateCreationShouldNotHappenWhenLiftieNameExists() {
        ReflectionTestUtils.setField(externalizedComputeClusterCreateService, "kubernetesVersion", "1.28");
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