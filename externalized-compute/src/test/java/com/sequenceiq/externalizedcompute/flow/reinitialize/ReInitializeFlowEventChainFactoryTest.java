package com.sequenceiq.externalizedcompute.flow.reinitialize;

import static com.sequenceiq.externalizedcompute.flow.graph.FlowOfflineStateGraphGenerator.FLOW_CONFIGS_PACKAGE;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sequenceiq.externalizedcompute.flow.ExternalizedComputeClusterEvent;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.graph.FlowChainConfigGraphGeneratorUtil;

class ReInitializeFlowEventChainFactoryTest {

    private ReInitializeFlowEventChainFactory underTest;

    @Test
    void testFlowChainEventQueueBuildingAndGenerateGraph() {
        underTest = new ReInitializeFlowEventChainFactory();
        ExternalizedComputeClusterEvent externalizedComputeClusterEvent = new ExternalizedComputeClusterEvent(0L, "actorCrn");

        FlowTriggerEventQueue flowTriggerEventQueue = underTest.createFlowTriggerEventQueue(externalizedComputeClusterEvent);

        Assertions.assertEquals(2, flowTriggerEventQueue.getQueue().size());
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE, flowTriggerEventQueue);
    }

}