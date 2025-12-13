package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.generator.FlowOfflineStateGraphGenerator.FLOW_CONFIGS_PACKAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationService;
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

        assertEquals(3, flowTriggerEventQueue.getQueue().size());
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

        assertEquals(2, flowTriggerEventQueue.getQueue().size());
        assertTrue(flowTriggerEventQueue.getQueue().stream().noneMatch(event -> FlowChainTriggers.FULL_UPSCALE_TRIGGER_EVENT.equals(event.getSelector())));
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE, flowTriggerEventQueue, "ZOOKEEPER_TO_KRAFT_MIGRATION");
    }
}