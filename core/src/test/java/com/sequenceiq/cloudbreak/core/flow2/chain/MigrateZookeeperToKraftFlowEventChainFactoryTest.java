package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.generator.FlowOfflineStateGraphGenerator.FLOW_CONFIGS_PACKAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationService;
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
        when(stackService.getPlatformVariantByStackId(0L)).thenReturn(new CloudPlatformVariant("AWS", "AWS_NATIVE"));
        MigrateZookeeperToKraftFlowChainTriggerEvent triggerEvent = new MigrateZookeeperToKraftFlowChainTriggerEvent(
                FlowChainTriggers.MIGRATE_ZOOKEEPER_TO_KRAFT_CHAIN_TRIGGER_EVENT, 0L);

        FlowTriggerEventQueue flowTriggerEventQueue = underTest.createFlowTriggerEventQueue(triggerEvent);

        assertEquals(3, flowTriggerEventQueue.getQueue().size());
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE, flowTriggerEventQueue, "ZOOKEEPER_TO_KRAFT_MIGRATION");
    }
}