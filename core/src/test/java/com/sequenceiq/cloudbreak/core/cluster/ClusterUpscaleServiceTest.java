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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
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
import com.sequenceiq.cloudbreak.message.FlowMessageService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.UpscaleClusterRequest;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ScalingException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;
import com.sequenceiq.cloudbreak.service.stack.RuntimeVersionService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@ExtendWith(MockitoExtension.class)
class ClusterUpscaleServiceTest {

    private static final long STACK_ID = 1L;

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

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private FlowMessageService flowMessageService;

    @Mock
    private RuntimeVersionService runtimeVersionService;

    @BeforeEach
    public void setUp() {
        Cluster cluster = new Cluster();
        cluster.setId(2L);
        when(stackDtoService.getById(eq(STACK_ID))).thenReturn(stackDto);
        inOrder = Mockito.inOrder(parcelService, recipeEngine, clusterStatusService, clusterCommissionService, clusterApi, clusterHostServiceRunner,
                clusterService);
        lenient().when(stackDto.getCluster()).thenReturn(cluster);
        lenient().when(stackDto.getId()).thenReturn(STACK_ID);
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
        Map<String, String> candidates = Map.of("master-1", "privateIp");
        when(clusterHostServiceRunner.collectUpscaleCandidates(any(), isNull(), anyBoolean())).thenReturn(candidates);

        underTest.installServicesOnNewHosts(createRequest(true, true, Map.of("master", Set.of("master-1", "master-2", "master-3")), null, false, false));

        inOrder.verify(parcelService).removeUnusedParcelComponents(stackDto);
        inOrder.verify(recipeEngine, times(1)).executePostClouderaManagerStartRecipesOnTargets(stackDto, Set.of(hostGroup), candidates);
        inOrder.verify(clusterApi, times(1)).upscaleCluster(any());
        inOrder.verify(clusterStatusService, times(1)).getDecommissionedHostsFromCM();
        inOrder.verify(clusterCommissionService, times(0)).recommissionHosts(any());
        inOrder.verify(clusterApi, times(1)).restartAll(false);
        inOrder.verify(clusterHostServiceRunner, times(1)).createCronForUserHomeCreation(eq(stackDto), eq(candidates.keySet()));
    }

    @Test
    public void testInstallServicesShouldNotCallServiceRestartWhenRollingUpgradeEnabled() throws CloudbreakException {
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
        Map<String, String> candidates = Map.of("master-1", "privateIp");
        when(clusterHostServiceRunner.collectUpscaleCandidates(any(), isNull(), anyBoolean())).thenReturn(candidates);
        when(runtimeVersionService.getRuntimeVersion(any())).thenReturn(Optional.of("7.3.1"));

        underTest.installServicesOnNewHosts(createRequest(true, true, Map.of("master", Set.of("master-1", "master-2", "master-3")), null, false, true));

        inOrder.verify(parcelService).removeUnusedParcelComponents(stackDto);
        inOrder.verify(recipeEngine, times(1)).executePostClouderaManagerStartRecipesOnTargets(stackDto, Set.of(hostGroup), candidates);
        inOrder.verify(clusterApi, times(1)).upscaleCluster(any());
        inOrder.verify(clusterStatusService, times(1)).getDecommissionedHostsFromCM();
        inOrder.verify(clusterCommissionService, times(0)).recommissionHosts(any());
        inOrder.verify(clusterHostServiceRunner, times(1)).createCronForUserHomeCreation(eq(stackDto), eq(candidates.keySet()));
        verify(clusterApi, times(0)).restartAll(false);
        verify(clusterApi, times(0)).rollingRestartServices();
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
        Map<String, String> candidates = Map.of("master-1", "privateIp");
        when(clusterHostServiceRunner.collectUpscaleCandidates(any(), isNull(), anyBoolean())).thenReturn(candidates);

        underTest.installServicesOnNewHosts(createRequest(true, true, Map.of("master", Set.of("master-1", "master-2", "master-3")), null, false, false));

        inOrder.verify(parcelService).removeUnusedParcelComponents(stackDto);
        inOrder.verify(recipeEngine, times(1)).executePostClouderaManagerStartRecipesOnTargets(stackDto, Set.of(hostGroup), candidates);
        inOrder.verify(clusterApi, times(1)).upscaleCluster(any());
        inOrder.verify(clusterStatusService, times(1)).getDecommissionedHostsFromCM();
        inOrder.verify(clusterCommissionService, times(1)).recommissionHosts(List.of("master-2"));
        inOrder.verify(clusterApi, times(1)).restartAll(false);
        inOrder.verify(clusterHostServiceRunner, times(1)).createCronForUserHomeCreation(eq(stackDto), eq(candidates.keySet()));
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
        Map<String, String> candidates = Map.of("master-1", "privateIp");
        when(clusterHostServiceRunner.collectUpscaleCandidates(any(), isNull(), anyBoolean())).thenReturn(candidates);

        underTest.installServicesOnNewHosts(createRequest(true, true, Map.of("master", Set.of("master-1", "master-2", "master-3")), null, false, false));

        inOrder.verify(parcelService).removeUnusedParcelComponents(stackDto);
        inOrder.verify(recipeEngine, times(1)).executePostClouderaManagerStartRecipesOnTargets(stackDto, Set.of(hostGroup), candidates);
        inOrder.verify(clusterApi, times(1)).upscaleCluster(any());
        inOrder.verify(clusterApi, times(0)).restartAll(false);
        inOrder.verify(clusterCommissionService, times(0)).recommissionHosts(any());
        inOrder.verify(clusterHostServiceRunner, times(1)).createCronForUserHomeCreation(eq(stackDto), eq(candidates.keySet()));
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
        Map<String, String> candidates = Map.of("master-1", "privateIp");
        when(clusterHostServiceRunner.collectUpscaleCandidates(any(), isNull(), anyBoolean())).thenReturn(candidates);

        when(clusterApiConnectors.getConnector(any(StackDto.class))).thenReturn(clusterApi);

        underTest.installServicesOnNewHosts(createRequest(false, false, Map.of("master", Set.of("master-1", "master-2", "master-3")), null, false, false));

        inOrder.verify(parcelService).removeUnusedParcelComponents(stackDto);
        inOrder.verify(recipeEngine, times(1)).executePostClouderaManagerStartRecipesOnTargets(stackDto, Set.of(hostGroup), candidates);
        inOrder.verify(clusterApi, times(1)).upscaleCluster(any());
        inOrder.verify(clusterApi, times(0)).restartAll(false);
        inOrder.verify(clusterStatusService, times(0)).getDecommissionedHostsFromCM();
        inOrder.verify(clusterCommissionService, times(0)).recommissionHosts(any());
        inOrder.verify(clusterHostServiceRunner, times(1)).createCronForUserHomeCreation(eq(stackDto), eq(candidates.keySet()));
    }

    @Test
    public void testInstallServicesShouldThrowExceptionWhenFailedToRemoveUnusedParcels() throws CloudbreakException {
        when(parcelService.removeUnusedParcelComponents(stackDto)).thenReturn(new ParcelOperationStatus(Map.of(), Map.of("parcel", "parcel")));

        CloudbreakException exception = assertThrows(CloudbreakException.class,
                () -> underTest.installServicesOnNewHosts(createRequest(true, true, new HashMap<>(), null, false, false)));

        assertEquals("Failed to remove the following parcels: {parcel=[parcel]}", exception.getMessage());
        verify(parcelService).removeUnusedParcelComponents(stackDto);
    }

    @Test
    public void testInstallServicesShouldNotThrowExceptionWhenTargetedUpscaleClusterFailed() throws CloudbreakException {
        when(entitlementService.targetedUpscaleSupported(any())).thenReturn(Boolean.TRUE);
        HostGroup hostGroup = newHostGroup(
                "master",
                newInstance(InstanceStatus.SERVICES_HEALTHY),
                newInstance(InstanceStatus.SERVICES_HEALTHY),
                newInstance(InstanceStatus.SERVICES_HEALTHY));
        when(hostGroupService.getByClusterWithRecipes(any())).thenReturn(Set.of(hostGroup));
        when(hostGroupService.getByCluster(any())).thenReturn(Set.of(hostGroup));
        when(parcelService.removeUnusedParcelComponents(stackDto)).thenReturn(new ParcelOperationStatus(Map.of(), Map.of()));

        Map<String, String> candidates = Map.of("master-1", "privateIp");
        when(clusterHostServiceRunner.collectUpscaleCandidates(any(), isNull(), anyBoolean())).thenReturn(candidates);
        when(clusterApiConnectors.getConnector(any(StackDto.class))).thenReturn(clusterApi);
        when(clusterApi.upscaleCluster(any())).thenThrow(new ScalingException("error", Set.of("instanceId1")));

        underTest.installServicesOnNewHosts(createRequest(false, true, new HashMap<>(), null, false, false));

        inOrder.verify(parcelService).removeUnusedParcelComponents(stackDto);
        inOrder.verify(recipeEngine, times(1)).executePostClouderaManagerStartRecipesOnTargets(stackDto, Set.of(hostGroup), candidates);
        inOrder.verify(clusterApi, times(1)).upscaleCluster(any());
        inOrder.verify(clusterApi, times(0)).restartAll(false);
        inOrder.verify(clusterStatusService, times(0)).getDecommissionedHostsFromCM();
        inOrder.verify(clusterCommissionService, times(0)).recommissionHosts(any());
        inOrder.verify(clusterService, times(1)).updateInstancesToZombieByInstanceIds(eq(STACK_ID), eq(Set.of("instanceId1")));
    }

    @Test
    public void testInstallServicesShouldThrowExceptionWhenNotTargetedUpscaleClusterFailed() throws CloudbreakException {
        HostGroup hostGroup = newHostGroup(
                "master",
                newInstance(InstanceStatus.SERVICES_HEALTHY),
                newInstance(InstanceStatus.SERVICES_HEALTHY),
                newInstance(InstanceStatus.SERVICES_HEALTHY));
        when(hostGroupService.getByClusterWithRecipes(any())).thenReturn(Set.of(hostGroup));
        when(hostGroupService.getByCluster(any())).thenReturn(Set.of(hostGroup));
        when(parcelService.removeUnusedParcelComponents(stackDto)).thenReturn(new ParcelOperationStatus(Map.of(), Map.of()));

        Map<String, String> candidates = Map.of("master-1", "privateIp");
        when(clusterHostServiceRunner.collectUpscaleCandidates(any(), isNull(), anyBoolean())).thenReturn(candidates);
        when(clusterApiConnectors.getConnector(any(StackDto.class))).thenReturn(clusterApi);
        when(clusterApi.upscaleCluster(any())).thenThrow(new ScalingException("error", Set.of("instanceId1")));

        ScalingException exception = assertThrows(ScalingException.class,
                () -> underTest.installServicesOnNewHosts(createRequest(false, true, new HashMap<>(), null, true, false)));

        Assertions.assertThat(exception.getFailedInstanceIds()).containsExactly("instanceId1");
        assertEquals("error", exception.getMessage());
        inOrder.verify(parcelService).removeUnusedParcelComponents(stackDto);
        inOrder.verify(recipeEngine, times(1)).executePostClouderaManagerStartRecipesOnTargets(stackDto, Set.of(hostGroup), candidates);
        inOrder.verify(clusterApi, times(1)).upscaleCluster(any());
        inOrder.verify(clusterApi, times(0)).restartAll(false);
        inOrder.verify(clusterStatusService, times(0)).getDecommissionedHostsFromCM();
        inOrder.verify(clusterCommissionService, times(0)).recommissionHosts(any());
        inOrder.verify(clusterService, times(1)).updateInstancesToOrchestrationFailedByInstanceIds(eq(STACK_ID), eq(Set.of("instanceId1")));
    }

    @Test
    void restartAllShouldCallRegularRestartWhenTheRollingRestartIsNotEnabled() {
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);

        underTest.restartAll(STACK_ID, false);

        verify(clusterApi).restartAll(false);
    }

    @Test
    void restartAllShouldCallRollingRestartWhenTheRollingRestartIsEnabled() {
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);

        underTest.restartAll(STACK_ID, true);

        verify(clusterApi).rollingRestartServices();
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

    private UpscaleClusterRequest createRequest(boolean repair, boolean restartServices, Map<String, Collection<String>> hostGroupsWithHostNames,
            Map<String, Integer> hostGroupWithAdjustment, boolean primaryGatewayChanged, boolean rollingRestartEnabled) {
        return new UpscaleClusterRequest(STACK_ID, Set.of("master"), repair, restartServices, hostGroupsWithHostNames, hostGroupWithAdjustment,
                primaryGatewayChanged, rollingRestartEnabled);
    }

}