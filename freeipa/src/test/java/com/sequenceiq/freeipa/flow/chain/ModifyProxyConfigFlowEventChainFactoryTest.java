package com.sequenceiq.freeipa.flow.chain;

import static com.sequenceiq.freeipa.flow.graph.FlowOfflineStateGraphGenerator.FLOW_CONFIGS_PACKAGE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.graph.FlowChainConfigGraphGeneratorUtil;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.event.ModifyProxyFlowChainTriggerEvent;

class ModifyProxyConfigFlowEventChainFactoryTest {

    private ModifyProxyConfigFlowEventChainFactory underTest;

    @Test
    void createFlowTriggerEventQueue() {
        underTest = new ModifyProxyConfigFlowEventChainFactory();
        ModifyProxyFlowChainTriggerEvent event = new ModifyProxyFlowChainTriggerEvent("selector", 0L, "resourceid", "previousproxyconfigcrn");

        FlowTriggerEventQueue flowTriggerEventQueue = underTest.createFlowTriggerEventQueue(event);

        assertNotNull(flowTriggerEventQueue);
        assertEquals(3, flowTriggerEventQueue.getQueue().size());
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE_NAME, flowTriggerEventQueue);
    }
}