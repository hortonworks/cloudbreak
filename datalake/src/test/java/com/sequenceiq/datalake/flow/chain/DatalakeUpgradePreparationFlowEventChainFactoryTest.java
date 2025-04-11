package com.sequenceiq.datalake.flow.chain;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeUpgradePreparationFlowChainStartEvent;
import com.sequenceiq.datalake.flow.graph.FlowOfflineStateGraphGenerator;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.graph.FlowChainConfigGraphGeneratorUtil;

class DatalakeUpgradePreparationFlowEventChainFactoryTest {

    private DatalakeUpgradePreparationFlowEventChainFactory underTest;

    @Test
    void testCreateFlowEventQueue() {
        underTest = new DatalakeUpgradePreparationFlowEventChainFactory();
        DatalakeUpgradePreparationFlowChainStartEvent event = new DatalakeUpgradePreparationFlowChainStartEvent(
                0L, "userId", "imageId", "backupLocation");

        FlowTriggerEventQueue eventQueue = underTest.createFlowTriggerEventQueue(event);

        Assertions.assertNotNull(eventQueue);
        Assertions.assertEquals(2, eventQueue.getQueue().size());
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FlowOfflineStateGraphGenerator.FLOW_CONFIGS_PACKAGE_NAME, eventQueue);
    }

}