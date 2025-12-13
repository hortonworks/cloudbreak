package com.sequenceiq.freeipa.flow.chain;

import static com.sequenceiq.freeipa.flow.graph.FlowOfflineStateGraphGenerator.FLOW_CONFIGS_PACKAGE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.graph.FlowChainConfigGraphGeneratorUtil;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmFlowChainTriggerEvent;

class UpgradeCcmFlowEventChainFactoryTest {

    private UpgradeCcmFlowEventChainFactory underTest;

    @Test
    void createFlowTriggerEventQueue() {
        underTest = new UpgradeCcmFlowEventChainFactory();
        UpgradeCcmFlowChainTriggerEvent event = new UpgradeCcmFlowChainTriggerEvent("selector", "resourceId", 0L, Tunnel.CCM);

        FlowTriggerEventQueue flowTriggerEventQueue = underTest.createFlowTriggerEventQueue(event);

        assertNotNull(flowTriggerEventQueue);
        assertEquals(2, flowTriggerEventQueue.getQueue().size());
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE_NAME, flowTriggerEventQueue);
    }
}