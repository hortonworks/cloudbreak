package com.sequenceiq.cloudbreak.service.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.FlowsAlreadyRunningException;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.StateStatus;

@ExtendWith(MockitoExtension.class)
public class ClusterServiceTest {

    private static final long STACK_ID = 1;

    private static final String STACK_CRN = "STACK_CRN";

    private static final long CLUSTER_ID = 1;

    @Mock
    private StackService stackService;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private HostGroupService hostGroupService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private ResourceService resourceService;

    @Mock
    private ReactorFlowManager flowManager;

    @Mock
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Mock
    private CloudbreakEventService cloudbreakEventService;

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @InjectMocks
    private ClusterService underTest;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private ClusterModificationService clusterModificationService;

    private Cluster cluster;

    private Stack stack;

    @Spy
    private ResourceAttributeUtil resourceAttributeUtil;

    @BeforeEach
    public void setUp() throws TransactionExecutionException {
        cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        cluster.setRdsConfigs(Set.of());
        stack = spy(new Stack());
        stack.setId(STACK_ID);
        stack.setResourceCrn(STACK_CRN);
        stack.setCluster(cluster);
        stack.setPlatformVariant("AWS");
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(Status.AVAILABLE);
        stack.setStackStatus(stackStatus);
        cluster.setStack(stack);

        lenient().doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get()).when(transactionService).required(any());
    }

    @Test
    public void repairClusterHostGroupsHappyPath() {

        HostGroup hostGroup1 = new HostGroup();
        hostGroup1.setName("hostGroup1");
        hostGroup1.setRecoveryMode(RecoveryMode.MANUAL);
        InstanceMetaData host1 = getHost("host1", hostGroup1.getName(), InstanceStatus.SERVICES_UNHEALTHY, InstanceGroupType.CORE);
        hostGroup1.setInstanceGroup(host1.getInstanceGroup());

        when(hostGroupService.getByCluster(eq(1L))).thenReturn(Set.of(hostGroup1));
        when(flowLogService.findAllByResourceIdOrderByCreatedDesc(1L)).thenReturn(Collections.emptyList());
        when(stackUpdater.updateStackStatus(1L, DetailedStackStatus.REPAIR_IN_PROGRESS)).thenReturn(stack);
        when(blueprintService.isAmbariBlueprint(any())).thenReturn(Boolean.TRUE);

        underTest.repairCluster(1L, List.of("hostGroup1"), false, false);

        verify(stack, never()).getInstanceMetaDataAsList();
        verify(flowManager).triggerClusterRepairFlow(eq(1L), eq(Map.of("hostGroup1", List.of("host1"))), eq(false));
    }

    @Test
    public void repairClusterNodeIdsHappyPath() {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceGroupType(InstanceGroupType.CORE);

        HostGroup hostGroup1 = new HostGroup();
        hostGroup1.setName("hostGroup1");
        hostGroup1.setRecoveryMode(RecoveryMode.MANUAL);
        hostGroup1.setInstanceGroup(instanceGroup);

        when(hostGroupService.getByCluster(eq(1L))).thenReturn(Set.of(hostGroup1));

        InstanceMetaData instance1md = new InstanceMetaData();
        instance1md.setInstanceId("instanceId1");
        instance1md.setDiscoveryFQDN("host1Name.healthy");
        instance1md.setInstanceGroup(instanceGroup);
        instanceGroup.setInstanceMetaData(Collections.singleton(instance1md));

        List<InstanceMetaData> instanceMetaData = List.of(instance1md);

        when(stack.getInstanceMetaDataAsList()).thenReturn(instanceMetaData);

        Resource volumeSet = new Resource();
        VolumeSetAttributes attributes = new VolumeSetAttributes("eu-west-1", Boolean.TRUE, "", List.of(), 100, "standard");
        attributes.setDeleteOnTermination(null);
        volumeSet.setAttributes(new Json(attributes));
        volumeSet.setInstanceId("instanceId1");
        volumeSet.setResourceType(ResourceType.AWS_VOLUMESET);
        stack.setResources(Set.of(volumeSet));
        FlowLog flowLog = new FlowLog();
        flowLog.setStateStatus(StateStatus.SUCCESSFUL);
        when(flowLogService.findAllByResourceIdOrderByCreatedDesc(1L)).thenReturn(List.of(flowLog));
        when(stackUpdater.updateStackStatus(1L, DetailedStackStatus.REPAIR_IN_PROGRESS)).thenReturn(stack);
        when(blueprintService.isAmbariBlueprint(any())).thenReturn(Boolean.TRUE);

        underTest.repairCluster(1L, List.of("instanceId1"), false, false, false);
        verify(stack).getInstanceMetaDataAsList();
        verify(stack).getDiskResources();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Resource>> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(resourceService).saveAll(saveCaptor.capture());
        assertFalse(resourceAttributeUtil.getTypedAttributes(saveCaptor.getValue().get(0), VolumeSetAttributes.class).get().getDeleteOnTermination());
        verify(flowManager).triggerClusterRepairFlow(eq(1L), eq(Map.of("hostGroup1", List.of("host1Name.healthy"))), eq(false));
    }

    @Test
    public void shouldNotUpdateStackStateWhenHasPendingFlow() {
        FlowLog flowLog = new FlowLog();
        flowLog.setStateStatus(StateStatus.PENDING);
        when(flowLogService.findAllByResourceIdOrderByCreatedDesc(1L)).thenReturn(List.of(flowLog));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            underTest.repairCluster(1L, List.of("instanceId1"), false, false, false);
        });
        assertEquals("Repair cannot be performed, because there is already an active flow.", exception.getMessage());

        verifyZeroInteractions(stackUpdater);
    }

    @Test
    public void shouldThrowExceptionWhenNewHealthyAndFailedNodeAreTheSame() {
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            underTest.reportHealthChange(STACK_CRN, Set.of("host"), Set.of("host"));
        });

        assertEquals("Failed nodes [host] and healthy nodes [host] should not have common items.", exception.getMessage());
    }

    @Test
    public void shouldTriggerRecoveryInAutoRecoveryHostsAndUpdateHostMetaData() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText("{ \"Blueprints\": { \"blueprint_name\": \"MyBlueprint\" } }");
        cluster.setBlueprint(blueprint);

        when(stackService.findByCrn(STACK_CRN)).thenReturn(stack);

        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setDatabaseEngine(DatabaseVendor.POSTGRES);
        when(rdsConfigService.findByClusterIdAndType(CLUSTER_ID, DatabaseType.CLOUDERA_MANAGER)).thenReturn(rdsConfig);
        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
        when(clusterApi.clusterModificationService()).thenReturn(clusterModificationService);
        when(cloudbreakMessagesService.getMessage(any(), anyCollection())).thenReturn("auto recovery").thenReturn("failed node");

        InstanceMetaData host1 = getHost("host1", "master", InstanceStatus.SERVICES_HEALTHY, InstanceGroupType.GATEWAY);
        when(instanceMetaDataService.findHostInStack(eq(stack.getId()), eq("host1"))).thenReturn(Optional.of(host1));

        InstanceMetaData host2 = getHost("host2", "group2", InstanceStatus.SERVICES_HEALTHY, InstanceGroupType.GATEWAY);
        when(instanceMetaDataService.findHostInStack(eq(stack.getId()), eq("host2"))).thenReturn(Optional.of(host2));

        when(hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), host1.getInstanceGroup().getGroupName()))
                .thenReturn(Optional.of(getHostGroup(host1, RecoveryMode.AUTO)));
        when(hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), host2.getInstanceGroup().getGroupName()))
                .thenReturn(Optional.of(getHostGroup(host2, RecoveryMode.MANUAL)));

        when(instanceMetaDataService.findNotTerminatedForStack(eq(stack.getId()))).thenReturn(new HashSet<>(Arrays.asList(host1, host2)));

        underTest.reportHealthChange(STACK_CRN, Set.of("host1", "host2"), Set.of());

        Map<String, List<String>> autoRecoveredNodes = Map.of("master", List.of("host1"));
        verify(flowManager).triggerClusterRepairFlow(STACK_ID, autoRecoveredNodes, false);
        verify(cloudbreakMessagesService, times(1)).getMessage("cluster.autorecovery.requested", Collections.singletonList(autoRecoveredNodes));
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(STACK_ID, "RECOVERY", "auto recovery");

        verify(cloudbreakMessagesService, times(1)).getMessage("cluster.failednodes.reported", Collections.singletonList(Set.of("host2")));
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(STACK_ID, "RECOVERY", "failed node");
    }

    @Test
    public void shouldTriggerSync() {
        String hostFQDN = "host2Name.stopped";
        InstanceMetaData instanceMd = getHost(hostFQDN, "master", InstanceStatus.SERVICES_HEALTHY, InstanceGroupType.GATEWAY);
        when(hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), instanceMd.getInstanceGroup().getGroupName()))
                .thenReturn(Optional.of(getHostGroup(instanceMd, RecoveryMode.MANUAL)));
        when(instanceMetaDataService.findHostInStack(eq(stack.getId()), eq(hostFQDN))).thenReturn(Optional.of(instanceMd));

        when(stackService.findByCrn(STACK_CRN)).thenReturn(stack);
        when(cloudbreakMessagesService.getMessage(any(), anyCollection())).thenReturn("failed node");
        when(instanceMetaDataService.getPrimaryGatewayInstanceMetadata(anyLong())).thenReturn(Optional.of(instanceMd));
        doNothing().when(flowManager).triggerStackSync(anyLong());

        underTest.reportHealthChange(STACK_CRN, Set.of(hostFQDN), Set.of());

        verify(flowManager, times(1)).triggerStackSync(eq(stack.getId()));
    }

    @Test
    public void shouldNotUpdateHostMetaDataWhenRecoveryTriggerFails() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText("{ \"Blueprints\": { \"blueprint_name\": \"MyBlueprint\" } }");
        cluster.setBlueprint(blueprint);

        when(stackService.findByCrn(STACK_CRN)).thenReturn(stack);
        InstanceMetaData host1 = getHost("host1", "master", InstanceStatus.SERVICES_HEALTHY, InstanceGroupType.GATEWAY);
        when(instanceMetaDataService.findHostInStack(eq(stack.getId()), eq("host1"))).thenReturn(Optional.of(host1));
        when(hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), host1.getInstanceGroup().getGroupName()))
                .thenReturn(Optional.of(getHostGroup(host1, RecoveryMode.AUTO)));

        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setDatabaseEngine(DatabaseVendor.POSTGRES);
        when(rdsConfigService.findByClusterIdAndType(CLUSTER_ID, DatabaseType.CLOUDERA_MANAGER)).thenReturn(rdsConfig);
        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
        when(clusterApi.clusterModificationService()).thenReturn(clusterModificationService);
        doThrow(new FlowsAlreadyRunningException("Flow in action")).when(flowManager).triggerClusterRepairFlow(anyLong(), anyMap(), anyBoolean());

        assertThrows(FlowsAlreadyRunningException.class, () -> {
            underTest.reportHealthChange(STACK_CRN, Set.of("host1"), Set.of());
        });

        verifyNoMoreInteractions(cloudbreakMessagesService);
        verifyNoMoreInteractions(cloudbreakEventService);
    }

    @Test
    public void shouldRegisterNewHealthyHosts() {
        when(stackService.findByCrn(STACK_CRN)).thenReturn(stack);

        InstanceMetaData host1 = getHost("host1", "master", InstanceStatus.SERVICES_UNHEALTHY, InstanceGroupType.GATEWAY);

        when(instanceMetaDataService.findNotTerminatedForStack(eq(stack.getId()))).thenReturn(new HashSet<>(Arrays.asList(host1)));

        when(cloudbreakMessagesService.getMessage(any(), anyCollection())).thenReturn("recovery detected");

        underTest.reportHealthChange(STACK_CRN, Set.of(), Set.of("host1"));

        verify(cloudbreakMessagesService).getMessage("cluster.recoverednodes.reported", Collections.singletonList(Set.of("host1")));
        verify(cloudbreakEventService).fireCloudbreakEvent(STACK_ID, "AVAILABLE", "recovery detected");
        assertEquals(InstanceStatus.SERVICES_HEALTHY, host1.getInstanceStatus());
    }

    private InstanceMetaData getHost(String hostName, String groupName, InstanceStatus instanceStatus, InstanceGroupType instanceGroupType) {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceGroupType(instanceGroupType);
        instanceGroup.setGroupName(groupName);

        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN(hostName);
        instanceMetaData.setInstanceGroup(instanceGroup);
        instanceMetaData.setInstanceStatus(instanceStatus);
        instanceGroup.setInstanceMetaData(Collections.singleton(instanceMetaData));

        return instanceMetaData;
    }

    private HostGroup getHostGroup(InstanceMetaData instanceMetaData, RecoveryMode recoveryMode) {
        HostGroup hostGroup = new HostGroup();
        hostGroup.setName(instanceMetaData.getInstanceGroupName());
        hostGroup.setRecoveryMode(recoveryMode);
        return hostGroup;
    }
}