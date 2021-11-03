package com.sequenceiq.cloudbreak.core.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.model.ParcelOperationStatus;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class ClusterUpscaleServiceTest {

    @Mock
    private StackService stackService;

    @Mock
    private HostGroupService hostGroupService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private RecipeEngine recipeEngine;

    @Mock
    private ParcelService parcelService;

    @InjectMocks
    private ClusterUpscaleService underTest;

    @Test
    public void testInstallServicesOnNewHostWithRestart() throws CloudbreakException {
        Stack stack = new Stack();
        stack.setId(1L);
        Cluster cluster = new Cluster();
        cluster.setId(2L);
        stack.setCluster(cluster);
        when(stackService.getByIdWithClusterInTransaction(eq(1L))).thenReturn(stack);
        ClusterApi clusterApi = mock(ClusterApi.class);
        when(clusterApiConnectors.getConnector(any(Stack.class))).thenReturn(clusterApi);
        HostGroup hostGroup = new HostGroup();
        InstanceGroup instanceGroup = new InstanceGroup();
        InstanceMetaData im1 = new InstanceMetaData();
        im1.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        InstanceMetaData im2 = new InstanceMetaData();
        im2.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        InstanceMetaData im3 = new InstanceMetaData();
        im3.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        instanceGroup.setInstanceMetaData(Set.of(im1, im2, im3));
        hostGroup.setInstanceGroup(instanceGroup);
        when(hostGroupService.getByClusterIdAndNameWithRecipes(2L, "master")).thenReturn(hostGroup);
        when(parcelService.removeUnusedParcelComponents(stack)).thenReturn(new ParcelOperationStatus(Collections.emptyMap(), Collections.emptyMap()));

        underTest.installServicesOnNewHosts(1L, "master", true, true);

        verify(clusterApi, times(1)).upscaleCluster(eq(hostGroup), any());
        verify(clusterApi, times(1)).restartAll(false);
        verify(parcelService).removeUnusedParcelComponents(stack);
    }

    @Test
    public void testInstallServicesOnNewHostWithRestartButThereIsAnUnhealthyNode() throws CloudbreakException {
        Stack stack = new Stack();
        stack.setId(1L);
        Cluster cluster = new Cluster();
        cluster.setId(2L);
        stack.setCluster(cluster);
        when(stackService.getByIdWithClusterInTransaction(eq(1L))).thenReturn(stack);
        ClusterApi clusterApi = mock(ClusterApi.class);
        when(clusterApiConnectors.getConnector(any(Stack.class))).thenReturn(clusterApi);
        HostGroup hostGroup = new HostGroup();
        InstanceGroup instanceGroup = new InstanceGroup();
        InstanceMetaData im1 = new InstanceMetaData();
        im1.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        InstanceMetaData im2 = new InstanceMetaData();
        im2.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        InstanceMetaData im3 = new InstanceMetaData();
        im3.setInstanceStatus(InstanceStatus.DELETED_BY_PROVIDER);
        stack.setInstanceGroups(Set.of(instanceGroup));
        instanceGroup.setInstanceMetaData(Set.of(im1, im2, im3));
        hostGroup.setInstanceGroup(instanceGroup);
        when(hostGroupService.getByClusterIdAndNameWithRecipes(2L, "master")).thenReturn(hostGroup);
        when(parcelService.removeUnusedParcelComponents(stack)).thenReturn(new ParcelOperationStatus(Collections.emptyMap(), Collections.emptyMap()));

        underTest.installServicesOnNewHosts(1L, "master", true, true);

        verify(clusterApi, times(1)).upscaleCluster(eq(hostGroup), any());
        verify(clusterApi, times(0)).restartAll(false);
        verify(parcelService).removeUnusedParcelComponents(stack);
    }

    @Test
    public void testInstallServicesShouldThrowExceptionWhenFailedToRemoveUnusedParcels() throws CloudbreakException {
        Stack stack = new Stack();
        stack.setId(1L);
        Cluster cluster = new Cluster();
        cluster.setId(2L);
        stack.setCluster(cluster);
        when(stackService.getByIdWithClusterInTransaction(eq(1L))).thenReturn(stack);
        when(parcelService.removeUnusedParcelComponents(stack)).thenReturn(new ParcelOperationStatus(Collections.emptyMap(), Map.of("parcel", "parcel")));

        CloudbreakException exception = assertThrows(CloudbreakException.class, () -> underTest.installServicesOnNewHosts(1L, "master", true, true));

        assertEquals("Failed to remove the following parcels: {parcel=parcel}", exception.getMessage());
        verify(parcelService).removeUnusedParcelComponents(stack);
    }

}