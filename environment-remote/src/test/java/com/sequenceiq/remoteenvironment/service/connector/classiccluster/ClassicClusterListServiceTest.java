package com.sequenceiq.remoteenvironment.service.connector.classiccluster;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class ClassicClusterListServiceTest {

    @InjectMocks
    private ClassicClusterListService underTest;

    @Test
    void testList() {
        OnPremisesApiProto.ClusterData.Builder clusterData = OnPremisesApiProto.ClusterData.newBuilder()
                .setClusterDetails(OnPremisesApiProto.ClusterDetails.newBuilder()
                        .setEntityStatus(OnPremisesApiProto.EntityStatus.Value.NONE));
        OnPremisesApiProto.Cluster cluster1 = OnPremisesApiProto.Cluster.newBuilder()
                .setClusterCrn("clustercrn1")
                .setName("cluster1")
                .setLastCreateTime(new Date().getTime())
                .setKnoxEnabled(true)
                .setKnoxUrl("knoxurl1")
                .setDatacenterName("datacenter1")
                .setData(clusterData)
                .build();
        OnPremisesApiProto.Cluster cluster2 = OnPremisesApiProto.Cluster.newBuilder()
                .setClusterCrn("clustercrn2")
                .setName("cluster2")
                .setLastCreateTime(new Date().getTime())
                .setKnoxEnabled(true)
                .setKnoxUrl("knoxurl2")
                .setDatacenterName("datacenter2")
                .setData(clusterData)
                .build();
        Collection<SimpleRemoteEnvironmentResponse> actual = underTest.list(List.of(cluster1, cluster2));
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
        Collection<SimpleRemoteEnvironmentResponse> actual = underTest.list(List.of(cluster));
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
        Collection<SimpleRemoteEnvironmentResponse> actual = underTest.list(List.of(cluster));
        Assertions.assertEquals(cluster.getManagerUri(), actual.stream().findFirst().get().getUrl());
    }

    @Test
    void testListAvailableStatus() {
        OnPremisesApiProto.ClusterData.Builder clusterData = OnPremisesApiProto.ClusterData.newBuilder()
                .setClusterDetails(OnPremisesApiProto.ClusterDetails.newBuilder()
                        .setEntityStatus(OnPremisesApiProto.EntityStatus.Value.GOOD_HEALTH));
        OnPremisesApiProto.Cluster cluster = OnPremisesApiProto.Cluster.newBuilder()
                .setClusterCrn("clustercrn1")
                .setName("cluster1")
                .setLastCreateTime(new Date().getTime())
                .setKnoxEnabled(true)
                .setManagerUri("manageruri")
                .setDatacenterName("datacenter1")
                .setData(clusterData)
                .build();
        Collection<SimpleRemoteEnvironmentResponse> actual = underTest.list(List.of(cluster));
        Assertions.assertEquals("AVAILABLE", actual.stream().findFirst().get().getStatus());
    }

    @Test
    void testListOtherStatus() {
        OnPremisesApiProto.ClusterData.Builder clusterData = OnPremisesApiProto.ClusterData.newBuilder()
                .setClusterDetails(OnPremisesApiProto.ClusterDetails.newBuilder()
                        .setEntityStatus(OnPremisesApiProto.EntityStatus.Value.CONCERNING_HEALTH));
        OnPremisesApiProto.Cluster cluster = OnPremisesApiProto.Cluster.newBuilder()
                .setClusterCrn("clustercrn1")
                .setName("cluster1")
                .setLastCreateTime(new Date().getTime())
                .setKnoxEnabled(true)
                .setManagerUri("manageruri")
                .setDatacenterName("datacenter1")
                .setData(clusterData)
                .build();
        Collection<SimpleRemoteEnvironmentResponse> actual = underTest.list(List.of(cluster));
        Assertions.assertEquals("CONCERNING_HEALTH", actual.stream().findFirst().get().getStatus());
    }

    private void assertEquals(OnPremisesApiProto.Cluster cluster, SimpleRemoteEnvironmentResponse envResponse) {
        Assertions.assertEquals(cluster.getClusterCrn(), envResponse.getCrn());
        Assertions.assertEquals(cluster.getName(), envResponse.getName());
        Assertions.assertEquals(cluster.getLastCreateTime(), envResponse.getCreated());
        Assertions.assertEquals(cluster.getKnoxUrl(), envResponse.getUrl());
        Assertions.assertEquals(cluster.getData().getClusterDetails().getEntityStatus().toString(), envResponse.getStatus());
        Assertions.assertEquals("PRIVATE_CLOUD", envResponse.getCloudPlatform());
        Assertions.assertEquals("PRIVATE_CLOUD", envResponse.getRegion());
    }
}
