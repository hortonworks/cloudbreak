package com.sequenceiq.cloudbreak.core.flow2.chain;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.event.AwsVariantMigrationTriggerEvent;
import com.sequenceiq.cloudbreak.domain.projection.StackIdView;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.view.ClusterView;
import com.sequenceiq.cloudbreak.domain.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.StackViewService;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

public class ClusterRepairFlowEventChainFactoryTest {

    private static final long INSTANCE_GROUP_ID = 1L;

    private static final String FAILED_NODE_FQDN_PRIMARY_GATEWAY = "failedNode-FQDN-primary-gateway";

    private static final String FAILED_NODE_FQDN_SECONDARY_GATEWAY = "failedNode-FQDN-secondary-gateway";

    private static final String FAILED_NODE_FQDN_CORE = "failedNode-FQDN-core";

    private static final long STACK_ID = 1L;

    private static final long CLUSTER_ID = 2L;

    private static final boolean MULTIPLE_GATEWAY = true;

    private static final boolean NOT_MULTIPLE_GATEWAY = false;

    private static final StackIdView ATTACHED_WORKLOAD = mock(StackIdView.class);

    @Mock
    private StackService stackService;

    @Mock
    private StackViewService stackViewService;

    @Mock
    private HostGroupService hostGroupService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private KerberosConfigService kerberosConfigService;

    @Mock
    private InstanceGroupService instanceGroupService;

    @InjectMocks
    private ClusterRepairFlowEventChainFactory underTest;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testRepairSingleGatewayWithNoAttached() {
        Stack stack = getStack(NOT_MULTIPLE_GATEWAY);
        setupStackView();
        when(stackService.findClustersConnectedToDatalakeByDatalakeStackId(STACK_ID)).thenReturn(Set.of());
        setupHostGroup(true);

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(new TriggerEventBuilder(stack).withFailedPrimaryGateway().build());

        List<String> triggeredOperations = eventQueues.getQueue().stream().map(Selectable::selector).collect(Collectors.toList());
        assertEquals(List.of("FLOWCHAIN_INIT_TRIGGER_EVENT",
                "STACK_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"), triggeredOperations);
    }

    @Test
    public void testRepairSingleGatewayWithAttached() {
        Stack stack = getStack(NOT_MULTIPLE_GATEWAY);
        when(stackService.findClustersConnectedToDatalakeByDatalakeStackId(STACK_ID)).thenReturn(Set.of(ATTACHED_WORKLOAD));
        setupStackView();
        setupHostGroup(true);

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(new TriggerEventBuilder(stack).withFailedPrimaryGateway().build());

        List<String> triggeredOperations = eventQueues.getQueue().stream().map(Selectable::selector).collect(Collectors.toList());
        assertEquals(List.of("FLOWCHAIN_INIT_TRIGGER_EVENT",
                "STACK_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"), triggeredOperations);
    }

    @Test
    public void testRepairSingleGatewayMultipleNodes() {
        Stack stack = getStack(NOT_MULTIPLE_GATEWAY);
        setupStackView();
        when(stackService.findClustersConnectedToDatalakeByDatalakeStackId(STACK_ID)).thenReturn(Set.of());

        HostGroup masterHostGroup = setupHostGroup(setupInstanceGroup(InstanceGroupType.GATEWAY));
        when(hostGroupService.findHostGroupInClusterByName(anyLong(), eq("hostGroup-master"))).thenReturn(Optional.of(masterHostGroup));
        HostGroup coreHostGroup = setupHostGroup(setupInstanceGroup(InstanceGroupType.CORE));
        when(hostGroupService.findHostGroupInClusterByName(anyLong(), eq("hostGroup-core"))).thenReturn(Optional.of(coreHostGroup));
        setupPrimaryGateway();

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(
                new TriggerEventBuilder(stack).withFailedPrimaryGateway().withFailedCore().build());

        List<String> triggeredOperations = eventQueues.getQueue().stream().map(Selectable::selector).collect(Collectors.toList());
        assertEquals(List.of("FLOWCHAIN_INIT_TRIGGER_EVENT",
                "STACK_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"),
                triggeredOperations);
    }

    @Test
    public void testRepairMultipleGatewayWithNoAttached() {
        Stack stack = getStack(MULTIPLE_GATEWAY);
        when(stackService.findClustersConnectedToDatalakeByDatalakeStackId(STACK_ID)).thenReturn(Set.of());
        InstanceGroupView ig = mock(InstanceGroupView.class);
        when(ig.getInstanceGroupType()).thenReturn(InstanceGroupType.GATEWAY);
        when(ig.getNodeCount()).thenReturn(5);
        when(instanceGroupService.findViewByStackId(STACK_ID)).thenReturn(Set.of(ig));
        setupHostGroup(true);
        setupStackView();

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(new TriggerEventBuilder(stack).withFailedPrimaryGateway().build());

        List<String> triggeredOperations = eventQueues.getQueue().stream().map(Selectable::selector).collect(Collectors.toList());
        assertEquals(List.of(
                "FLOWCHAIN_INIT_TRIGGER_EVENT",
                "STACK_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"), triggeredOperations);
    }

    @Test
    public void testRepairMultipleGatewayWithAttached() {
        Stack stack = getStack(MULTIPLE_GATEWAY);
        when(stackService.findClustersConnectedToDatalakeByDatalakeStackId(STACK_ID)).thenReturn(Set.of(ATTACHED_WORKLOAD));
        InstanceGroupView ig = mock(InstanceGroupView.class);
        when(ig.getInstanceGroupType()).thenReturn(InstanceGroupType.GATEWAY);
        when(ig.getNodeCount()).thenReturn(5);
        when(instanceGroupService.findViewByStackId(STACK_ID)).thenReturn(Set.of(ig));
        setupHostGroup(true);
        setupStackView();

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(new TriggerEventBuilder(stack).withFailedPrimaryGateway().build());

        List<String> triggeredOperations = eventQueues.getQueue().stream().map(Selectable::selector).collect(Collectors.toList());
        assertEquals(List.of(
                "FLOWCHAIN_INIT_TRIGGER_EVENT",
                "STACK_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"), triggeredOperations);
    }

    @Test
    public void testRepairCoreNodes() {
        Stack stack = getStack(NOT_MULTIPLE_GATEWAY);
        when(stackService.findClustersConnectedToDatalakeByDatalakeStackId(STACK_ID)).thenReturn(Set.of(ATTACHED_WORKLOAD));
        setupHostGroup(false);
        setupStackView();

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(new TriggerEventBuilder(stack).withFailedCore().build());

        List<String> triggeredOperations = eventQueues.getQueue().stream().map(Selectable::selector).collect(Collectors.toList());
        assertEquals(List.of("FLOWCHAIN_INIT_TRIGGER_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"), triggeredOperations);
    }

    @Test
    public void testRepairNotGatewayInstanceGroup() {
        Stack stack = getStack(NOT_MULTIPLE_GATEWAY);
        when(stackService.findClustersConnectedToDatalakeByDatalakeStackId(STACK_ID)).thenReturn(Set.of());
        setupHostGroup(false);
        setupStackView();

        FlowTriggerEventQueue eventQueues = underTest.createFlowTriggerEventQueue(new TriggerEventBuilder(stack).withFailedCore().build());

        List<String> triggeredOperations = eventQueues.getQueue().stream().map(Selectable::selector).collect(Collectors.toList());
        assertEquals(List.of("FLOWCHAIN_INIT_TRIGGER_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT",
                "FLOWCHAIN_FINALIZE_TRIGGER_EVENT"), triggeredOperations);
    }

    @Test
    public void testAddAwsNativeMigrationIfNeedWhenNotUpgrade() {
        Queue<Selectable> flowTriggers = new ConcurrentLinkedDeque<>();
        String groupName = "groupName";
        boolean upgrade = false;
        String variant = "variant";
        underTest.addAwsNativeEventMigrationIfNeed(flowTriggers, STACK_ID, groupName, upgrade, variant);
        Assertions.assertTrue(flowTriggers.isEmpty());
    }

    @Test
    public void testAddAwsNativeMigrationIfNeedWhenUpgradeButNotAwsNativeVariant() {
        Queue<Selectable> flowTriggers = new ConcurrentLinkedDeque<>();
        String groupName = "groupName";
        boolean upgrade = true;
        String variant = "variant";
        underTest.addAwsNativeEventMigrationIfNeed(flowTriggers, STACK_ID, groupName, upgrade, variant);
        Assertions.assertTrue(flowTriggers.isEmpty());
    }

    @Test
    public void testAddAwsNativeMigrationIfNeedWhenUpgradeAndAwsNativeVariant() {
        Queue<Selectable> flowTriggers = new ConcurrentLinkedDeque<>();
        String groupName = "groupName";
        boolean upgrade = true;
        String variant = "AWS_NATIVE";
        underTest.addAwsNativeEventMigrationIfNeed(flowTriggers, STACK_ID, groupName, upgrade, variant);
        Assertions.assertFalse(flowTriggers.isEmpty());
        AwsVariantMigrationTriggerEvent actual = (AwsVariantMigrationTriggerEvent) flowTriggers.peek();
        Assertions.assertEquals(groupName, actual.getHostGroupName());
    }

    private void setupHostGroup(boolean gatewayInstanceGroup) {
        HostGroup hostGroup = setupHostGroup(setupInstanceGroup(gatewayInstanceGroup ? InstanceGroupType.GATEWAY : InstanceGroupType.CORE));
        when(hostGroupService.findHostGroupInClusterByName(anyLong(), anyString())).thenReturn(Optional.of(hostGroup));
        setupPrimaryGateway();
    }

    private void setupPrimaryGateway() {
        when(instanceMetaDataService.getPrimaryGatewayDiscoveryFQDNByInstanceGroup(anyLong(), anyLong()))
                .thenReturn(Optional.of(FAILED_NODE_FQDN_PRIMARY_GATEWAY));
    }

    private Stack getStack(boolean multipleGateway) {
        Stack stack = mock(Stack.class);
        when(stack.isMultipleGateway()).thenReturn(multipleGateway);
        Cluster cluster = mock(Cluster.class);
        when(stack.getCluster()).thenReturn(cluster);
        when(stack.getId()).thenReturn(STACK_ID);
        when(cluster.getId()).thenReturn(CLUSTER_ID);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        return stack;
    }

    private void setupStackView() {
        StackView stack = mock(StackView.class);
        ClusterView cluster = mock(ClusterView.class);
        when(stack.getClusterView()).thenReturn(cluster);
        when(stack.getId()).thenReturn(STACK_ID);
        when(cluster.getId()).thenReturn(CLUSTER_ID);
        when(stackViewService.getById(STACK_ID)).thenReturn(stack);
    }

    private HostGroup setupHostGroup(InstanceGroup instanceGroup) {
        HostGroup hostGroup = mock(HostGroup.class);
        when(hostGroup.getName()).thenReturn("hostGroupName");
        when(hostGroup.getInstanceGroup()).thenReturn(instanceGroup);
        return hostGroup;
    }

    private InstanceGroup setupInstanceGroup(InstanceGroupType instanceGroupType) {
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        when(instanceGroup.getId()).thenReturn(INSTANCE_GROUP_ID);
        when(instanceGroup.getInstanceGroupType()).thenReturn(instanceGroupType);
        return instanceGroup;
    }

    private static class TriggerEventBuilder {

        private final Stack stack;

        private final List<String> failedGatewayNodes = new ArrayList<>();

        private final List<String> failedCoreNodes = new ArrayList<>();

        private TriggerEventBuilder(Stack stack) {
            this.stack = stack;
        }

        private TriggerEventBuilder withFailedPrimaryGateway() {
            failedGatewayNodes.add(FAILED_NODE_FQDN_PRIMARY_GATEWAY);
            return this;
        }

        private TriggerEventBuilder withFailedSecondaryGateway() {
            failedGatewayNodes.add(FAILED_NODE_FQDN_SECONDARY_GATEWAY);
            return this;
        }

        private TriggerEventBuilder withFailedCore() {
            failedCoreNodes.add(FAILED_NODE_FQDN_CORE);
            return this;
        }

        private ClusterRepairTriggerEvent build() {
            Map<String, List<String>> failedNodes = new HashMap<>();
            if (!failedGatewayNodes.isEmpty()) {
                failedNodes.put("hostGroup-master", failedGatewayNodes);
            }
            if (!failedCoreNodes.isEmpty()) {
                failedNodes.put("hostGroup-core", failedCoreNodes);
            }
            return new ClusterRepairTriggerEvent(stack.getId(), failedNodes, false);
        }
    }
}
