package com.sequenceiq.datalake.flow.chain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeUpgradeFlowChainStartEvent;
import com.sequenceiq.datalake.flow.graph.FlowOfflineStateGraphGenerator;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.graph.FlowChainConfigGraphGeneratorUtil;

class DatalakeUpgradeFlowEventChainFactoryTest {

    private DatalakeUpgradeFlowEventChainFactory underTest;

    @Test
    void testCreateFlowEventQueue() {
        underTest = new DatalakeUpgradeFlowEventChainFactory();
        DatalakeUpgradeFlowChainStartEvent event = new DatalakeUpgradeFlowChainStartEvent(
                DatalakeUpgradeFlowChainStartEvent.DATALAKE_UPGRADE_FLOW_CHAIN_EVENT, 0L,
                "userId", "imageId", true, "backupLocation", null,
                false, true);

        FlowTriggerEventQueue eventQueue = underTest.createFlowTriggerEventQueue(event);

        assertNotNull(eventQueue);
        assertEquals(2, eventQueue.getQueue().size());
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FlowOfflineStateGraphGenerator.FLOW_CONFIGS_PACKAGE_NAME, eventQueue);
    }
}