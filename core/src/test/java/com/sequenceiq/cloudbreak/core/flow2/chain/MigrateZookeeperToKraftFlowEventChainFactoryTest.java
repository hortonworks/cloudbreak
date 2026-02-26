package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers.FULL_UPSCALE_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.START_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationStateSelectors.START_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.generator.FlowOfflineStateGraphGenerator.FLOW_CONFIGS_PACKAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftConfigurationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationService;
import com.sequenceiq.cloudbreak.core.flow2.event.StackAndClusterUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.graph.FlowChainConfigGraphGeneratorUtil;

@ExtendWith(MockitoExtension.class)
class MigrateZookeeperToKraftFlowEventChainFactoryTest {

    @Mock
    private StackService stackService;

    @Mock
    private SkuMigrationService skuMigrationService;

    @InjectMocks
    private MigrateZookeeperToKraftFlowEventChainFactory underTest;

    @Test
    void testCreateFlowTriggerEventQueueWhenMigratingZookeeperToKraft() {
        Stack stack = new Stack();
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("kraft");
        instanceGroup.setInstanceMetaData(Set.of());
        stack.setInstanceGroups(Set.of(instanceGroup));
        when(stackService.getByIdWithLists(0L)).thenReturn(stack);
        when(stackService.getPlatformVariantByStackId(0L)).thenReturn(new CloudPlatformVariant("AWS", "AWS_NATIVE"));
        MigrateZookeeperToKraftFlowChainTriggerEvent triggerEvent = new MigrateZookeeperToKraftFlowChainTriggerEvent(
                FlowChainTriggers.MIGRATE_ZOOKEEPER_TO_KRAFT_CHAIN_TRIGGER_EVENT, 0L);

        FlowTriggerEventQueue flowTriggerEventQueue = underTest.createFlowTriggerEventQueue(triggerEvent);

        Queue<Selectable> queue = flowTriggerEventQueue.getQueue();
        Queue<Selectable> restrainedQueueData = new ConcurrentLinkedQueue<>(queue);
        assertEquals(3, queue.size());
        checkEventIsKraftConfiguration(queue.poll());
        checkEventIsStackAndClusterUpscale(queue.poll());
        checkEventIsKraftMigration(queue.poll());
        flowTriggerEventQueue.getQueue().addAll(restrainedQueueData);

        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE, flowTriggerEventQueue, "ZOOKEEPER_TO_KRAFT_MIGRATION");
    }

    @Test
    void testCreateFlowTriggerEventQueueWhenUpscaleDoesNotRequired() {
        Stack stack = new Stack();
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInitialNodeCount(3);
        instanceGroup.setGroupName("kraft");
        instanceGroup.setInstanceMetaData(Set.of(new InstanceMetaData(), new InstanceMetaData(), new InstanceMetaData()));
        stack.setInstanceGroups(Set.of(instanceGroup));
        when(stackService.getByIdWithLists(0L)).thenReturn(stack);
        MigrateZookeeperToKraftFlowChainTriggerEvent triggerEvent = new MigrateZookeeperToKraftFlowChainTriggerEvent(
                FlowChainTriggers.MIGRATE_ZOOKEEPER_TO_KRAFT_CHAIN_TRIGGER_EVENT, 0L);

        FlowTriggerEventQueue flowTriggerEventQueue = underTest.createFlowTriggerEventQueue(triggerEvent);

        Queue<Selectable> queue = flowTriggerEventQueue.getQueue();
        Queue<Selectable> restrainedQueueData = new ConcurrentLinkedQueue<>(queue);
        assertEquals(2, queue.size());
        checkEventIsKraftConfiguration(queue.poll());
        checkEventIsKraftMigration(queue.poll());
        flowTriggerEventQueue.getQueue().addAll(restrainedQueueData);

        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE, flowTriggerEventQueue, "ZOOKEEPER_TO_KRAFT_MIGRATION");
    }

    private void checkEventIsKraftConfiguration(Selectable event) {
        assertEquals(START_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_VALIDATION_EVENT.selector(), event.selector());
        assertInstanceOf(MigrateZookeeperToKraftConfigurationTriggerEvent.class, event);
        assertEquals(0L, event.getResourceId());
    }

    private void checkEventIsStackAndClusterUpscale(Selectable event) {
        assertEquals(FULL_UPSCALE_TRIGGER_EVENT, event.selector());
        assertInstanceOf(StackAndClusterUpscaleTriggerEvent.class, event);
        assertEquals(0L, event.getResourceId());
        StackAndClusterUpscaleTriggerEvent upscaleEvent = (StackAndClusterUpscaleTriggerEvent) event;
        assertEquals(Map.of("kraft", 3), (upscaleEvent.getHostGroupsWithAdjustment()));
    }

    private void checkEventIsKraftMigration(Selectable event) {
        assertEquals(START_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT.selector(), event.selector());
        assertInstanceOf(MigrateZookeeperToKraftTriggerEvent.class, event);
        assertEquals(0L, event.getResourceId());
    }
}