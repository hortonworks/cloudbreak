package com.sequenceiq.externalizedcompute.flow.reinitialize;

import static com.sequenceiq.externalizedcompute.flow.graph.FlowOfflineStateGraphGenerator.FLOW_CONFIGS_PACKAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import com.sequenceiq.externalizedcompute.flow.ExternalizedComputeClusterEvent;
import com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteEvent;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.graph.FlowChainConfigGraphGeneratorUtil;

class ReInitializeFlowEventChainFactoryTest {

    private ReInitializeFlowEventChainFactory underTest;

    @Test
    void testFlowChainEventQueueBuildingAndGenerateGraph() {
        underTest = new ReInitializeFlowEventChainFactory();
        ExternalizedComputeClusterEvent externalizedComputeClusterEvent = new ExternalizedComputeClusterEvent(0L, "actorCrn");

        FlowTriggerEventQueue flowTriggerEventQueue = underTest.createFlowTriggerEventQueue(externalizedComputeClusterEvent);

        assertEquals(2, flowTriggerEventQueue.getQueue().size());
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE, flowTriggerEventQueue);
    }

    @Test
    void testReinitDeleteEventUsesNoForce() {
        underTest = new ReInitializeFlowEventChainFactory();
        ExternalizedComputeClusterEvent externalizedComputeClusterEvent = new ExternalizedComputeClusterEvent(0L, "actorCrn");

        FlowTriggerEventQueue flowTriggerEventQueue = underTest.createFlowTriggerEventQueue(externalizedComputeClusterEvent);

        ExternalizedComputeClusterDeleteEvent deleteEvent = (ExternalizedComputeClusterDeleteEvent)
                new ArrayList<>(flowTriggerEventQueue.getQueue()).get(0);
        assertFalse(deleteEvent.isForce(), "Reinit should not use force delete — a failed delete must fail the reinit");
    }

}