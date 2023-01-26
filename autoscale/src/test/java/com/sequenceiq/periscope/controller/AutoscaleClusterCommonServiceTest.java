package com.sequenceiq.periscope.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.model.NameOrCrn;
import com.sequenceiq.periscope.monitor.handler.CloudbreakCommunicator;
import com.sequenceiq.periscope.service.AutoscaleRestRequestThreadLocalService;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.NotFoundException;

@ExtendWith(MockitoExtension.class)
public class AutoscaleClusterCommonServiceTest {

    private static final String TEST_CLUSTER_NAME = "testcluster";

    private static final String TEST_CLUSTER_CRN = "crn:cdp:datahub:us-west-1:autoscale:cluster:f7563fc1-e8ff-486a-9260-4e54ccabbaa0";

    @InjectMocks
    private AutoScaleClusterCommonService underTest;

    @Mock
    private ClusterService clusterService;

    @Mock
    private CloudbreakCommunicator cloudbreakCommunicator;

    @Mock
    private AutoscaleRestRequestThreadLocalService restRequestThreadLocalService;

    private String tenant = "testTenant";

    @BeforeEach
    public void setup() {
        when(restRequestThreadLocalService.getCloudbreakTenant()).thenReturn(tenant);
    }

    @Test
    public void testGetClusterByCRNWhenPresentInDB() {
        when(clusterService.findOneByStackCrnAndTenant(TEST_CLUSTER_CRN, tenant)).thenReturn(getACluster());

        underTest.getClusterByCrnOrName(NameOrCrn.ofCrn(TEST_CLUSTER_CRN));
        verify(cloudbreakCommunicator, never()).getAutoscaleClusterByCrn(TEST_CLUSTER_CRN);
        verify(clusterService, never()).create(any(AutoscaleStackV4Response.class));
    }

    @Test
    public void testGetClusterByNameWhenPresentInDB() {
        when(clusterService.findOneByStackNameAndTenant(TEST_CLUSTER_NAME, tenant)).thenReturn(getACluster());

        underTest.getClusterByCrnOrName(NameOrCrn.ofName(TEST_CLUSTER_NAME));
        verify(cloudbreakCommunicator, never()).getAutoscaleClusterByName(TEST_CLUSTER_NAME, tenant);
        verify(clusterService, never()).create(any(AutoscaleStackV4Response.class));
    }

    @Test
    public void testGetClusterByCRNWhenNotPresentInDBThenCBSyncByCrn() {
        AutoscaleStackV4Response autoscaleStackV4Response = mock(AutoscaleStackV4Response.class);
        when(clusterService.findOneByStackCrnAndTenant(TEST_CLUSTER_CRN, tenant)).thenReturn(Optional.empty());
        when(cloudbreakCommunicator.getAutoscaleClusterByCrn(TEST_CLUSTER_CRN)).thenReturn(autoscaleStackV4Response);
        when(autoscaleStackV4Response.getStackType()).thenReturn(StackType.WORKLOAD);
        when(clusterService.create(autoscaleStackV4Response)).thenReturn(getACluster().get());

        Cluster cluster = underTest.getClusterByCrnOrName(NameOrCrn.ofCrn(TEST_CLUSTER_CRN));
        assertEquals(TEST_CLUSTER_NAME, cluster.getStackName(), "Cluster Name should match");
        assertEquals(TEST_CLUSTER_CRN, cluster.getStackCrn(), "Cluster CRN should match");
    }

    @Test
    public void testGetClusterByNameWhenNotPresentInDBThenCBSyncByName() {
        AutoscaleStackV4Response autoscaleStackV4Response = mock(AutoscaleStackV4Response.class);
        when(clusterService.findOneByStackNameAndTenant(TEST_CLUSTER_NAME, tenant)).thenReturn(Optional.empty());
        when(cloudbreakCommunicator.getAutoscaleClusterByName(TEST_CLUSTER_NAME, tenant)).thenReturn(autoscaleStackV4Response);
        when(autoscaleStackV4Response.getStackType()).thenReturn(StackType.WORKLOAD);
        when(clusterService.create(autoscaleStackV4Response)).thenReturn(getACluster().get());

        Cluster cluster = underTest.getClusterByCrnOrName(NameOrCrn.ofName(TEST_CLUSTER_NAME));
        assertEquals(TEST_CLUSTER_NAME, cluster.getStackName(), "Cluster Name should match");
        assertEquals(TEST_CLUSTER_CRN, cluster.getStackCrn(), "Cluster CRN should match");
    }

    @Test
    public void testGetClusterByNameWhenNotFound() {
        when(clusterService.findOneByStackNameAndTenant(TEST_CLUSTER_NAME, tenant)).thenReturn(Optional.empty());
        when(cloudbreakCommunicator.getAutoscaleClusterByName(TEST_CLUSTER_NAME, tenant)).thenThrow(NotFoundException.class);

        Assertions.assertThrows(NotFoundException.class,
                () -> underTest.getClusterByCrnOrName(NameOrCrn.ofName(TEST_CLUSTER_NAME)));
    }

    @Test
    public void testGetClusterByCrnWhenNotFound() {
        when(clusterService.findOneByStackCrnAndTenant(TEST_CLUSTER_CRN, tenant)).thenReturn(Optional.empty());
        when(cloudbreakCommunicator.getAutoscaleClusterByCrn(TEST_CLUSTER_CRN)).thenThrow(NotFoundException.class);

        Assertions.assertThrows(NotFoundException.class,
                () -> underTest.getClusterByCrnOrName(NameOrCrn.ofCrn(TEST_CLUSTER_CRN)));
    }

    private Optional<Cluster> getACluster() {
        Cluster cluster = new Cluster();
        cluster.setStackCrn(TEST_CLUSTER_CRN);
        cluster.setStackName(TEST_CLUSTER_NAME);
        cluster.setCloudPlatform("AWS");
        cluster.setTunnel(Tunnel.CLUSTER_PROXY);
        cluster.setStackType(StackType.WORKLOAD);
        return Optional.of(cluster);
    }
}
