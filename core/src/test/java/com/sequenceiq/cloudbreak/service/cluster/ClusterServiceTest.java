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

import java.io.IOException;
import java.util.Collections;
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
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.FlowsAlreadyRunningException;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.hostmetadata.HostMetadataService;
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
    private HostMetadataService hostMetadataService;

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
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceGroupType(InstanceGroupType.CORE);

        HostGroup hostGroup1 = new HostGroup();
        hostGroup1.setName("hostGroup1");
        hostGroup1.setRecoveryMode(RecoveryMode.MANUAL);
        hostGroup1.setInstanceGroup(instanceGroup);

        HostMetadata host1Metadata = new HostMetadata();
        host1Metadata.setHostName("host1Name");
        host1Metadata.setHostGroup(hostGroup1);
        host1Metadata.setHostMetadataState(HostMetadataState.UNHEALTHY);

        HostMetadata host2Metadata = new HostMetadata();
        host2Metadata.setHostName("host2Name.healthy");
        host2Metadata.setHostGroup(hostGroup1);
        host2Metadata.setHostMetadataState(HostMetadataState.HEALTHY);

        Set<HostMetadata> hostMetadata = Set.of(host1Metadata, host2Metadata);
        hostGroup1.setHostMetadata(hostMetadata);

        when(hostGroupService.getByCluster(eq(1L))).thenReturn(Set.of(hostGroup1));
        when(flowLogService.findAllByResourceIdOrderByCreatedDesc(1L)).thenReturn(Collections.emptyList());
        when(stackUpdater.updateStackStatus(1L, DetailedStackStatus.REPAIR_IN_PROGRESS)).thenReturn(stack);
        when(blueprintService.isAmbariBlueprint(any())).thenReturn(Boolean.TRUE);

        underTest.repairCluster(1L, List.of("hostGroup1"), false, false);

        verify(stack, never()).getInstanceMetaDataAsList();
        verify(flowManager).triggerClusterRepairFlow(eq(1L), eq(Map.of("hostGroup1", List.of("host1Name"))), eq(false));
    }

    @Test
    public void repairClusterNodeIdsHappyPath() throws IOException {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceGroupType(InstanceGroupType.CORE);

        HostGroup hostGroup1 = new HostGroup();
        hostGroup1.setName("hostGroup1");
        hostGroup1.setRecoveryMode(RecoveryMode.MANUAL);
        hostGroup1.setInstanceGroup(instanceGroup);

        HostMetadata host1Metadata = new HostMetadata();
        host1Metadata.setHostName("host1Name.healthy");
        host1Metadata.setHostGroup(hostGroup1);
        host1Metadata.setHostMetadataState(HostMetadataState.HEALTHY);

        Set<HostMetadata> hostMetadata = Set.of(host1Metadata);
        hostGroup1.setHostMetadata(hostMetadata);

        when(hostGroupService.getByCluster(eq(1L))).thenReturn(Set.of(hostGroup1));

        InstanceMetaData instance1md = new InstanceMetaData();
        instance1md.setInstanceId("instanceId1");
        instance1md.setDiscoveryFQDN("host1Name.healthy");

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
    public void shouldThrowExceptionWhenNoMetaDataFoundDuringFailureReport() {
        when(stackService.findByCrn(STACK_CRN)).thenReturn(stack);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            underTest.reportHealthChange(STACK_CRN, Set.of("host1"), Set.of());
        });

        assertEquals("No metadata information for the node: host1", exception.getMessage());
    }

    @Test
    public void shouldTriggerRecoveryInAutoRecoveryHostsAndUpdateHostMetaData() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText("{ \"Blueprints\": { \"blueprint_name\": \"MyBlueprint\" } }");
        cluster.setBlueprint(blueprint);

        when(stackService.findByCrn(STACK_CRN)).thenReturn(stack);

        HostMetadata hostMetadata1 = givenHost("host1", "hg1", RecoveryMode.AUTO, HostMetadataState.HEALTHY);
        HostMetadata hostMetadata2 = givenHost("host2", "hg2", RecoveryMode.MANUAL, HostMetadataState.HEALTHY);

        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setDatabaseEngine(DatabaseVendor.POSTGRES);
        when(rdsConfigService.findByClusterIdAndType(CLUSTER_ID, DatabaseType.CLOUDERA_MANAGER)).thenReturn(rdsConfig);
        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
        when(clusterApi.clusterModificationService()).thenReturn(clusterModificationService);
        when(hostMetadataService.findHostsInCluster(CLUSTER_ID)).thenReturn(Set.of(hostMetadata1, hostMetadata2));
        when(cloudbreakMessagesService.getMessage(any(), anyCollection())).thenReturn("auto recovery").thenReturn("failed node");

        underTest.reportHealthChange(STACK_CRN, Set.of("host1", "host2"), Set.of());

        Map<String, List<String>> autoRecoveredNodes = Map.of("hg1", List.of("host1"));
        verify(flowManager).triggerClusterRepairFlow(STACK_ID, autoRecoveredNodes, false);
        verify(cloudbreakMessagesService, times(1)).getMessage("cluster.autorecovery.requested", Collections.singletonList(autoRecoveredNodes));
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(STACK_ID, "RECOVERY", "auto recovery");
        verify(hostMetadataService, times(1)).saveAll(Set.of(hostMetadata1));
        assertEquals(HostMetadataState.WAITING_FOR_REPAIR, hostMetadata1.getHostMetadataState());

        verify(cloudbreakMessagesService, times(1)).getMessage("cluster.failednodes.reported", Collections.singletonList(Set.of("host2")));
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(STACK_ID, "RECOVERY", "failed node");
        verify(hostMetadataService, times(1)).saveAll(Set.of(hostMetadata2));
        assertEquals(HostMetadataState.UNHEALTHY, hostMetadata2.getHostMetadataState());
    }

    @Test
    public void shouldTriggerSync() {
        String hostFQDN = "host2Name.stopped";
        HostMetadata hostMetadata = givenHost(hostFQDN, "hg2", RecoveryMode.MANUAL, HostMetadataState.HEALTHY);
        InstanceMetaData instanceMd = new InstanceMetaData();
        instanceMd.setDiscoveryFQDN(hostFQDN);

        when(stackService.findByCrn(STACK_CRN)).thenReturn(stack);
        when(hostMetadataService.findHostsInCluster(CLUSTER_ID)).thenReturn(Set.of(hostMetadata));
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

        HostMetadata hostMetadata = givenHost("host", "hg", RecoveryMode.AUTO, HostMetadataState.HEALTHY);

        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setDatabaseEngine(DatabaseVendor.POSTGRES);
        when(rdsConfigService.findByClusterIdAndType(CLUSTER_ID, DatabaseType.CLOUDERA_MANAGER)).thenReturn(rdsConfig);
        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
        when(clusterApi.clusterModificationService()).thenReturn(clusterModificationService);
        doThrow(new FlowsAlreadyRunningException("Flow in action")).when(flowManager).triggerClusterRepairFlow(anyLong(), anyMap(), anyBoolean());

        assertThrows(FlowsAlreadyRunningException.class, () -> {
            underTest.reportHealthChange(STACK_CRN, Set.of(hostMetadata.getHostName()), Set.of());
        });

        verifyNoMoreInteractions(cloudbreakMessagesService);
        verifyNoMoreInteractions(cloudbreakEventService);
        verifyNoMoreInteractions(hostMetadataService);
    }

    @Test
    public void shouldRegisterNewHealthyHosts() {
        when(stackService.findByCrn(STACK_CRN)).thenReturn(stack);

        HostMetadata hostMetadata = getHost("host", "hg", RecoveryMode.AUTO, HostMetadataState.UNHEALTHY);

        when(hostMetadataService.findHostsInCluster(CLUSTER_ID)).thenReturn(Set.of(hostMetadata));
        when(cloudbreakMessagesService.getMessage(any(), anyCollection())).thenReturn("recovery detected");

        underTest.reportHealthChange(STACK_CRN, Set.of(), Set.of("host"));

        verify(cloudbreakMessagesService).getMessage("cluster.recoverednodes.reported", Collections.singletonList(Set.of("host")));
        verify(cloudbreakEventService).fireCloudbreakEvent(STACK_ID, "AVAILABLE", "recovery detected");
        verify(hostMetadataService).saveAll(Set.of(hostMetadata));
        assertEquals(HostMetadataState.HEALTHY, hostMetadata.getHostMetadataState());
    }

    private HostMetadata givenHost(String hostName, String hostGroupName, RecoveryMode recoveryMode, HostMetadataState hostMetadataState) {
        HostMetadata hostMetadata = getHost(hostName, hostGroupName, recoveryMode, hostMetadataState);

        when(hostMetadataService.findHostInClusterByName(CLUSTER_ID, hostName)).thenReturn(Optional.of(hostMetadata));

        return hostMetadata;
    }

    private HostMetadata getHost(String hostName, String hostGroupName, RecoveryMode recoveryMode, HostMetadataState hostMetadataState) {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceGroupType(InstanceGroupType.GATEWAY);

        HostGroup hostGroup = new HostGroup();
        hostGroup.setName(hostGroupName);
        hostGroup.setRecoveryMode(recoveryMode);
        hostGroup.setInstanceGroup(instanceGroup);

        HostMetadata hostMetadata = new HostMetadata();
        hostMetadata.setHostName(hostName);
        hostMetadata.setHostGroup(hostGroup);
        hostMetadata.setHostMetadataState(hostMetadataState);
        hostGroup.setHostMetadata(Set.of(hostMetadata));

        return hostMetadata;
    }
}