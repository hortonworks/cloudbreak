package com.sequenceiq.remoteenvironment.service.connector.classiccluster;

import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.remotecluster.client.RemoteClusterServiceClient;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class ClassicClusterListServiceTest {
    @Mock
    private EntitlementService entitlementService;

    @Mock
    private RemoteClusterServiceClient remoteClusterServiceClient;

    @InjectMocks
    private ClassicClusterListService underTest;

    @Test
    void testList() {
        OnPremisesApiProto.Cluster cluster1 = OnPremisesApiProto.Cluster.newBuilder()
                .setClusterCrn("clustercrn1")
                .setName("cluster1")
                .setLastCreateTime(new Date().getTime())
                .setKnoxEnabled(true)
                .setKnoxUrl("knoxurl1")
                .setDatacenterName("datacenter1")
                .setData(OnPremisesApiProto.ClusterData.newBuilder().setProperties(JsonUtil.writeValueAsStringSilent(Map.of("entityStatus", "entityState1"))))
                .build();
        OnPremisesApiProto.Cluster cluster2 = OnPremisesApiProto.Cluster.newBuilder()
                .setClusterCrn("clustercrn2")
                .setName("cluster2")
                .setLastCreateTime(new Date().getTime())
                .setKnoxEnabled(true)
                .setKnoxUrl("knoxurl2")
                .setDatacenterName("datacenter2")
                .setData(OnPremisesApiProto.ClusterData.newBuilder().setProperties(JsonUtil.writeValueAsStringSilent(Map.of("entityStatus", "entityState2"))))
                .build();
        when(remoteClusterServiceClient.listClassicClusters()).thenReturn(List.of(cluster1, cluster2));
        Collection<SimpleRemoteEnvironmentResponse> actual = underTest.list();
        assertEquals(cluster1, actual.stream().filter(resp -> resp.getName().equals(cluster1.getName())).findFirst().get());
        assertEquals(cluster2, actual.stream().filter(resp -> resp.getName().equals(cluster2.getName())).findFirst().get());
    }

    @Test
    void testListDisabledKnox() {
        OnPremisesApiProto.Cluster cluster = OnPremisesApiProto.Cluster.newBuilder()
                .setClusterCrn("clustercrn1")
                .setName("cluster1")
                .setLastCreateTime(new Date().getTime())
                .setKnoxEnabled(false)
                .setKnoxUrl("knoxurl1")
                .setManagerUri("manageruri")
                .setDatacenterName("datacenter1")
                .build();
        when(remoteClusterServiceClient.listClassicClusters()).thenReturn(List.of(cluster));
        Collection<SimpleRemoteEnvironmentResponse> actual = underTest.list();
        Assertions.assertEquals(cluster.getManagerUri(), actual.stream().findFirst().get().getUrl());
    }

    @Test
    void testListEmptyKnoxUrl() {
        OnPremisesApiProto.Cluster cluster = OnPremisesApiProto.Cluster.newBuilder()
                .setClusterCrn("clustercrn1")
                .setName("cluster1")
                .setLastCreateTime(new Date().getTime())
                .setKnoxEnabled(true)
                .setManagerUri("manageruri")
                .setDatacenterName("datacenter1")
                .build();
        when(remoteClusterServiceClient.listClassicClusters()).thenReturn(List.of(cluster));
        Collection<SimpleRemoteEnvironmentResponse> actual = underTest.list();
        Assertions.assertEquals(cluster.getManagerUri(), actual.stream().findFirst().get().getUrl());
    }

    @Test
    void testListAvailableStatus() {
        OnPremisesApiProto.Cluster cluster = OnPremisesApiProto.Cluster.newBuilder()
                .setClusterCrn("clustercrn1")
                .setName("cluster1")
                .setLastCreateTime(new Date().getTime())
                .setKnoxEnabled(true)
                .setManagerUri("manageruri")
                .setDatacenterName("datacenter1")
                .setData(OnPremisesApiProto.ClusterData.newBuilder().setProperties(JsonUtil.writeValueAsStringSilent(Map.of("entityStatus", "GOOD_HEALTH"))))
                .build();
        when(remoteClusterServiceClient.listClassicClusters()).thenReturn(List.of(cluster));
        Collection<SimpleRemoteEnvironmentResponse> actual = underTest.list();
        Assertions.assertEquals("AVAILABLE", actual.stream().findFirst().get().getStatus());
    }

    @Test
    void testListUnknownStatus() {
        OnPremisesApiProto.Cluster cluster = OnPremisesApiProto.Cluster.newBuilder()
                .setClusterCrn("clustercrn1")
                .setName("cluster1")
                .setLastCreateTime(new Date().getTime())
                .setKnoxEnabled(true)
                .setManagerUri("manageruri")
                .setDatacenterName("datacenter1")
                .build();
        when(remoteClusterServiceClient.listClassicClusters()).thenReturn(List.of(cluster));
        Collection<SimpleRemoteEnvironmentResponse> actual = underTest.list();
        Assertions.assertEquals("UNKNOWN", actual.stream().findFirst().get().getStatus());
    }

    private void assertEquals(OnPremisesApiProto.Cluster cluster, SimpleRemoteEnvironmentResponse envResponse) {
        Assertions.assertEquals(cluster.getClusterCrn(), envResponse.getCrn());
        Assertions.assertEquals(cluster.getName(), envResponse.getName());
        Assertions.assertEquals(cluster.getLastCreateTime(), envResponse.getCreated());
        Assertions.assertEquals(cluster.getKnoxUrl(), envResponse.getUrl());
        Assertions.assertEquals(new Json(cluster.getData().getProperties()).getMap().get("entityStatus"), envResponse.getStatus());
        Assertions.assertEquals("PRIVATE_CLOUD", envResponse.getCloudPlatform());
        Assertions.assertEquals("PRIVATE_CLOUD", envResponse.getRegion());
    }
}
