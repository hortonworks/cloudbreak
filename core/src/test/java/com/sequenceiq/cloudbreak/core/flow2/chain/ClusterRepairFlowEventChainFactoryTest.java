package com.sequenceiq.cloudbreak.core.flow2.chain;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.domain.Constraint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

public class ClusterRepairFlowEventChainFactoryTest {

    private static final long INSTANCE_GROUP_ID = 1L;

    private static final String FAILED_NODE_FQDN_PRIMARY_GATEWAY = "failedNode-FQDN-primary-gateway";

    private static final String FAILED_NODE_FQDN_SECONDARY_GATEWAY = "failedNode-FQDN-secondary-gateway";

    private static final String FAILED_NODE_FQDN_CORE = "failedNode-FQDN-core";

    private static final long STACK_ID = 1L;

    private static final long CLUSTER_ID = 2L;

    private static final Stack ATTACHED_WORKLOAD = mock(Stack.class);

    @Mock
    private StackService stackService;

    @Mock
    private HostGroupService hostGroupService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private ClusterService clusterService;

    @InjectMocks
    private ClusterRepairFlowEventChainFactory underTest;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testRepairSingleGatewayWithNoAttached() {
        Stack stack = getStack();
        when(clusterService.isMultipleGateway(stack)).thenReturn(false);
        when(stackService.findClustersConnectedToDatalakeByDatalakeStackId(STACK_ID)).thenReturn(Set.of());
        setupHostGroup(true);

        Queue<Selectable> eventQueues = underTest.createFlowTriggerEventQueue(new TriggerEventBuilder(stack).withFailedPrimaryGateway().build());

        List<String> triggeredOperations = eventQueues.stream().map(Selectable::selector).collect(Collectors.toList());
        assertEquals(List.of("STACK_DOWNSCALE_TRIGGER_EVENT", "FULL_UPSCALE_TRIGGER_EVENT"), triggeredOperations);
    }

    @Test
    public void testRepairSingleGatewayWithAttached() {
        Stack stack = getStack();
        when(clusterService.isMultipleGateway(stack)).thenReturn(false);
        when(stackService.findClustersConnectedToDatalakeByDatalakeStackId(STACK_ID)).thenReturn(Set.of(ATTACHED_WORKLOAD));
        setupHostGroup(true);

        Queue<Selectable> eventQueues = underTest.createFlowTriggerEventQueue(new TriggerEventBuilder(stack).withFailedPrimaryGateway().build());

        List<String> triggeredOperations = eventQueues.stream().map(Selectable::selector).collect(Collectors.toList());
        assertEquals(List.of("STACK_DOWNSCALE_TRIGGER_EVENT", "FULL_UPSCALE_TRIGGER_EVENT", "EPHEMERAL_CLUSTERS_UPDATE_TRIGGER_EVENT"), triggeredOperations);
    }

    @Test
    public void testRepairSingleGatewayMultipleNodes() {
        Stack stack = getStack();
        when(clusterService.isMultipleGateway(stack)).thenReturn(false);
        when(stackService.findClustersConnectedToDatalakeByDatalakeStackId(STACK_ID)).thenReturn(Set.of());
        setupHostGroup(true);

        Queue<Selectable> eventQueues = underTest.createFlowTriggerEventQueue(
                new TriggerEventBuilder(stack).withFailedPrimaryGateway().withFailedCore().build());

        List<String> triggeredOperations = eventQueues.stream().map(Selectable::selector).collect(Collectors.toList());
        assertEquals(List.of("STACK_DOWNSCALE_TRIGGER_EVENT", "FULL_UPSCALE_TRIGGER_EVENT", "FULL_DOWNSCALE_TRIGGER_EVENT", "FULL_UPSCALE_TRIGGER_EVENT"),
                triggeredOperations);
    }

    @Test
    public void testRepairMultipleGatewayWithNoAttached() {
        Stack stack = getStack();
        when(clusterService.isMultipleGateway(stack)).thenReturn(true);
        when(stackService.findClustersConnectedToDatalakeByDatalakeStackId(STACK_ID)).thenReturn(Set.of());
        setupHostGroup(true);

        Queue<Selectable> eventQueues = underTest.createFlowTriggerEventQueue(new TriggerEventBuilder(stack).withFailedPrimaryGateway().build());

        List<String> triggeredOperations = eventQueues.stream().map(Selectable::selector).collect(Collectors.toList());
        assertEquals(List.of(
                "CHANGE_PRIMARY_GATEWAY_TRIGGER_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT"), triggeredOperations);
    }

    @Test
    public void testRepairMultipleGatewayWithAttached() {
        Stack stack = getStack();
        when(clusterService.isMultipleGateway(stack)).thenReturn(true);
        when(stackService.findClustersConnectedToDatalakeByDatalakeStackId(STACK_ID)).thenReturn(Set.of(ATTACHED_WORKLOAD));
        setupHostGroup(true);

        Queue<Selectable> eventQueues = underTest.createFlowTriggerEventQueue(new TriggerEventBuilder(stack).withFailedPrimaryGateway().build());

        List<String> triggeredOperations = eventQueues.stream().map(Selectable::selector).collect(Collectors.toList());
        assertEquals(List.of(
                "CHANGE_PRIMARY_GATEWAY_TRIGGER_EVENT",
                "FULL_DOWNSCALE_TRIGGER_EVENT",
                "FULL_UPSCALE_TRIGGER_EVENT",
                "EPHEMERAL_CLUSTERS_UPDATE_TRIGGER_EVENT"), triggeredOperations);
    }

    @Test
    public void testRepairCoreNodes() {
        Stack stack = getStack();
        when(clusterService.isMultipleGateway(stack)).thenReturn(false);
        when(stackService.findClustersConnectedToDatalakeByDatalakeStackId(STACK_ID)).thenReturn(Set.of(ATTACHED_WORKLOAD));
        setupHostGroup(true);

        Queue<Selectable> eventQueues = underTest.createFlowTriggerEventQueue(new TriggerEventBuilder(stack).withFailedCore().build());

        List<String> triggeredOperations = eventQueues.stream().map(Selectable::selector).collect(Collectors.toList());
        assertEquals(List.of("FULL_DOWNSCALE_TRIGGER_EVENT", "FULL_UPSCALE_TRIGGER_EVENT"), triggeredOperations);
    }

    @Test
    public void testRepairNotGatewayInstanceGroup() {
        Stack stack = getStack();
        when(clusterService.isMultipleGateway(stack)).thenReturn(false);
        when(stackService.findClustersConnectedToDatalakeByDatalakeStackId(STACK_ID)).thenReturn(Set.of());
        setupHostGroup(false);

        Queue<Selectable> eventQueues = underTest.createFlowTriggerEventQueue(new TriggerEventBuilder(stack).withFailedCore().build());

        List<String> triggeredOperations = eventQueues.stream().map(Selectable::selector).collect(Collectors.toList());
        assertEquals(List.of("FULL_DOWNSCALE_TRIGGER_EVENT", "FULL_UPSCALE_TRIGGER_EVENT"), triggeredOperations);
    }

    private void setupHostGroup(boolean gatewayInstanceGroup) {
        HostGroup hostGroup = setupHostGroup(setupInstanceGroup(gatewayInstanceGroup ? InstanceGroupType.GATEWAY : InstanceGroupType.CORE));
        when(hostGroupService.findHostGroupInClusterByName(anyLong(), anyString())).thenReturn(Optional.of(hostGroup));
        setupPrimaryGateway();
    }

    private void setupPrimaryGateway() {
        List<InstanceMetaData> primaryGatewayIm = getPrimaryGatewayByInstanceGroup();
        when(instanceMetaDataService.getPrimaryGatewayByInstanceGroup(anyLong(), anyLong())).thenReturn(primaryGatewayIm);
    }

    private Stack getStack() {
        Stack stack = mock(Stack.class);
        Cluster cluster = mock(Cluster.class);
        when(stack.getCluster()).thenReturn(cluster);
        when(stack.getId()).thenReturn(STACK_ID);
        when(cluster.getId()).thenReturn(CLUSTER_ID);
        return stack;
    }

    private List<InstanceMetaData> getPrimaryGatewayByInstanceGroup() {
        InstanceMetaData primaryGatewayIm = mock(InstanceMetaData.class);
        when(primaryGatewayIm.getDiscoveryFQDN()).thenReturn(FAILED_NODE_FQDN_PRIMARY_GATEWAY);
        when(primaryGatewayIm.getInstanceMetadataType()).thenReturn(InstanceMetadataType.GATEWAY_PRIMARY);
        return List.of(primaryGatewayIm);
    }

    private HostGroup setupHostGroup(InstanceGroup instanceGroup) {
        HostGroup hostGroup = mock(HostGroup.class);
        when(hostGroup.getName()).thenReturn("hostGroupName");
        Constraint constraint = mock(Constraint.class);
        when(constraint.getInstanceGroup()).thenReturn(instanceGroup);
        when(hostGroup.getConstraint()).thenReturn(constraint);
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

        private final List<String> failedHostnames = new ArrayList<>();

        private TriggerEventBuilder(Stack stack) {
            this.stack = stack;
        }

        private TriggerEventBuilder withFailedPrimaryGateway() {
            failedHostnames.add(FAILED_NODE_FQDN_PRIMARY_GATEWAY);
            return this;
        }

        private TriggerEventBuilder withFailedSecondaryGateway() {
            failedHostnames.add(FAILED_NODE_FQDN_PRIMARY_GATEWAY);
            return this;
        }

        private TriggerEventBuilder withFailedCore() {
            failedHostnames.add(FAILED_NODE_FQDN_CORE);
            return this;
        }

        private ClusterRepairTriggerEvent build() {
            Map<String, List<String>> failedNodes = Map.of("hostGroup-master", failedHostnames);
            return new ClusterRepairTriggerEvent(stack, failedNodes, false);
        }
    }
}
