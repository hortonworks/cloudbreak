package com.sequenceiq.cloudbreak.core.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterCommissionService;
import com.sequenceiq.cloudbreak.cluster.api.ClusterStatusService;
import com.sequenceiq.cloudbreak.cluster.model.ParcelOperationStatus;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@ExtendWith(MockitoExtension.class)
class ClusterUpscaleServiceTest {

    @Mock
    private StackDtoService stackDtoService;

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

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private ClusterStatusService clusterStatusService;

    @Mock
    private ClusterCommissionService clusterCommissionService;

    @Mock
    private ClusterHostServiceRunner clusterHostServiceRunner;

    private InOrder inOrder;

    @Mock
    private StackDto stackDto;

    @BeforeEach
    public void setUp() {
        Cluster cluster = new Cluster();
        cluster.setId(2L);
        when(stackDtoService.getById(eq(1L))).thenReturn(stackDto);
        inOrder = Mockito.inOrder(parcelService, recipeEngine, clusterStatusService, clusterCommissionService, clusterApi);
        lenient().when(stackDto.getCluster()).thenReturn(cluster);
        lenient().when(stackDto.getId()).thenReturn(1L);
    }

    @Test
    public void testInstallServicesWithRepairAndServiceRestart() throws CloudbreakException {
        HostGroup hostGroup = newHostGroup(
                "master",
                newInstance(InstanceStatus.SERVICES_HEALTHY),
                newInstance(InstanceStatus.SERVICES_HEALTHY),
                newInstance(InstanceStatus.SERVICES_HEALTHY));
        when(hostGroupService.getByClusterWithRecipes(any())).thenReturn(Set.of(hostGroup));
        when(hostGroupService.getByCluster(any())).thenReturn(Set.of(hostGroup));
        when(parcelService.removeUnusedParcelComponents(stackDto)).thenReturn(new ParcelOperationStatus(Map.of(), Map.of()));

        when(clusterApiConnectors.getConnector(any(StackDto.class))).thenReturn(clusterApi);
        when(clusterApi.clusterStatusService()).thenReturn(clusterStatusService);
        when(clusterStatusService.getDecommissionedHostsFromCM()).thenReturn(List.of());
        when(clusterHostServiceRunner.collectUpscaleCandidates(any(), isNull(), anyBoolean())).thenReturn(Map.of());

        underTest.installServicesOnNewHosts(1L, Set.of("master"), true, true, Map.of("master", Set.of("master-1", "master-2", "master-3")), null);

        inOrder.verify(parcelService).removeUnusedParcelComponents(stackDto);
        inOrder.verify(recipeEngine, times(1)).executePostClouderaManagerStartRecipesOnTargets(stackDto, Set.of(hostGroup), Map.of());
        inOrder.verify(clusterApi, times(1)).upscaleCluster(any());
        inOrder.verify(clusterStatusService, times(1)).getDecommissionedHostsFromCM();
        inOrder.verify(clusterCommissionService, times(0)).recommissionHosts(any());
        inOrder.verify(clusterApi, times(1)).restartAll(false);
    }

    @Test
    public void testInstallServicesWithRepairAndServiceRestartWhenOneHostIsDecommissioned() throws CloudbreakException {
        HostGroup hostGroup = newHostGroup(
                "master",
                newInstance(InstanceStatus.SERVICES_HEALTHY),
                newInstance(InstanceStatus.SERVICES_HEALTHY),
                newInstance(InstanceStatus.SERVICES_HEALTHY));
        when(hostGroupService.getByClusterWithRecipes(any())).thenReturn(Set.of(hostGroup));
        when(hostGroupService.getByCluster(any())).thenReturn(Set.of(hostGroup));
        when(parcelService.removeUnusedParcelComponents(stackDto)).thenReturn(new ParcelOperationStatus(Map.of(), Map.of()));

        when(clusterApiConnectors.getConnector(any(StackDto.class))).thenReturn(clusterApi);
        when(clusterApi.clusterStatusService()).thenReturn(clusterStatusService);
        when(clusterApi.clusterCommissionService()).thenReturn(clusterCommissionService);
        when(clusterStatusService.getDecommissionedHostsFromCM()).thenReturn(List.of("master-2"));
        when(clusterHostServiceRunner.collectUpscaleCandidates(any(), isNull(), anyBoolean())).thenReturn(Map.of());

        underTest.installServicesOnNewHosts(1L, Set.of("master"), true, true, Map.of("master", Set.of("master-1", "master-2", "master-3")), null);

        inOrder.verify(parcelService).removeUnusedParcelComponents(stackDto);
        inOrder.verify(recipeEngine, times(1)).executePostClouderaManagerStartRecipesOnTargets(stackDto, Set.of(hostGroup), Map.of());
        inOrder.verify(clusterApi, times(1)).upscaleCluster(any());
        inOrder.verify(clusterStatusService, times(1)).getDecommissionedHostsFromCM();
        inOrder.verify(clusterCommissionService, times(1)).recommissionHosts(List.of("master-2"));
        inOrder.verify(clusterApi, times(1)).restartAll(false);
    }

    @Test
    public void testInstallServicesWithRepairAndServiceRestartWhenOneHostIsUnhealthy() throws CloudbreakException {
        HostGroup hostGroup = newHostGroup(
                "master",
                newInstance(InstanceStatus.SERVICES_HEALTHY),
                newInstance(InstanceStatus.SERVICES_HEALTHY),
                newInstance(InstanceStatus.DELETED_BY_PROVIDER));
        when(stackDto.getAllAvailableInstances()).thenReturn(hostGroup.getInstanceGroup().getAllAvailableInstanceMetadata());
        when(hostGroupService.getByClusterWithRecipes(any())).thenReturn(Set.of(hostGroup));
        when(hostGroupService.getByCluster(any())).thenReturn(Set.of(hostGroup));
        when(parcelService.removeUnusedParcelComponents(stackDto)).thenReturn(new ParcelOperationStatus(Map.of(), Map.of()));

        when(clusterApiConnectors.getConnector(any(StackDto.class))).thenReturn(clusterApi);
        when(clusterApi.clusterStatusService()).thenReturn(clusterStatusService);
        when(clusterStatusService.getDecommissionedHostsFromCM()).thenReturn(List.of());
        when(clusterHostServiceRunner.collectUpscaleCandidates(any(), isNull(), anyBoolean())).thenReturn(Map.of());

        underTest.installServicesOnNewHosts(1L, Set.of("master"), true, true, Map.of("master", Set.of("master-1", "master-2", "master-3")), null);

        inOrder.verify(parcelService).removeUnusedParcelComponents(stackDto);
        inOrder.verify(recipeEngine, times(1)).executePostClouderaManagerStartRecipesOnTargets(stackDto, Set.of(hostGroup), Map.of());
        inOrder.verify(clusterApi, times(1)).upscaleCluster(any());
        inOrder.verify(clusterApi, times(0)).restartAll(false);
        inOrder.verify(clusterCommissionService, times(0)).recommissionHosts(any());
    }

    @Test
    public void testInstallServicesWithoutRepairAndServiceRestart() throws CloudbreakException {
        HostGroup hostGroup = newHostGroup(
                "master",
                newInstance(InstanceStatus.SERVICES_HEALTHY),
                newInstance(InstanceStatus.SERVICES_HEALTHY),
                newInstance(InstanceStatus.DELETED_BY_PROVIDER));
        when(hostGroupService.getByClusterWithRecipes(any())).thenReturn(Set.of(hostGroup));
        when(hostGroupService.getByCluster(any())).thenReturn(Set.of(hostGroup));
        when(parcelService.removeUnusedParcelComponents(stackDto)).thenReturn(new ParcelOperationStatus(Map.of(), Map.of()));
        when(clusterHostServiceRunner.collectUpscaleCandidates(any(), isNull(), anyBoolean())).thenReturn(Map.of());

        when(clusterApiConnectors.getConnector(any(StackDto.class))).thenReturn(clusterApi);

        underTest.installServicesOnNewHosts(1L, Set.of("master"), false, false, Map.of("master", Set.of("master-1", "master-2", "master-3")), null);

        inOrder.verify(parcelService).removeUnusedParcelComponents(stackDto);
        inOrder.verify(recipeEngine, times(1)).executePostClouderaManagerStartRecipesOnTargets(stackDto, Set.of(hostGroup), Map.of());
        inOrder.verify(clusterApi, times(1)).upscaleCluster(any());
        inOrder.verify(clusterApi, times(0)).restartAll(false);
        inOrder.verify(clusterStatusService, times(0)).getDecommissionedHostsFromCM();
        inOrder.verify(clusterCommissionService, times(0)).recommissionHosts(any());
    }

    @Test
    public void testInstallServicesShouldThrowExceptionWhenFailedToRemoveUnusedParcels() throws CloudbreakException {
        when(parcelService.removeUnusedParcelComponents(stackDto)).thenReturn(new ParcelOperationStatus(Map.of(), Map.of("parcel", "parcel")));

        CloudbreakException exception = assertThrows(CloudbreakException.class,
                () -> underTest.installServicesOnNewHosts(1L, Set.of("master"), true, true, new HashMap<>(), null));

        assertEquals("Failed to remove the following parcels: {parcel=[parcel]}", exception.getMessage());
        verify(parcelService).removeUnusedParcelComponents(stackDto);
    }

    private HostGroup newHostGroup(String name, InstanceMetaData... instances) {
        HostGroup hostGroup = new HostGroup();
        hostGroup.setName(name);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceMetaData(Set.of(instances));
        hostGroup.setInstanceGroup(instanceGroup);
        return hostGroup;
    }

    private InstanceMetaData newInstance(InstanceStatus status) {
        InstanceMetaData instance = new InstanceMetaData();
        instance.setInstanceStatus(status);
        return instance;
    }

}