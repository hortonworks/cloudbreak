package com.sequenceiq.datalake.flow.chain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeUpgradePreparationFlowChainStartEvent;
import com.sequenceiq.datalake.flow.datalake.upgrade.preparation.event.DatalakeUpgradePreparationStartEvent;
import com.sequenceiq.datalake.flow.graph.FlowOfflineStateGraphGenerator;
import com.sequenceiq.datalake.service.sdx.SdxTestUtil;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.graph.FlowChainConfigGraphGeneratorUtil;

class DatalakeUpgradePreparationFlowEventChainFactoryTest {

    private DatalakeUpgradePreparationFlowEventChainFactory underTest;

    @Test
    void testCreateFlowEventQueue() {
        underTest = new DatalakeUpgradePreparationFlowEventChainFactory();
        DatalakeUpgradePreparationFlowChainStartEvent event = new DatalakeUpgradePreparationFlowChainStartEvent(
                0L, "userId", "imageId", "backupLocation", null);

        FlowTriggerEventQueue eventQueue = underTest.createFlowTriggerEventQueue(event);

        assertNotNull(eventQueue);
        assertEquals(2, eventQueue.getQueue().size());
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FlowOfflineStateGraphGenerator.FLOW_CONFIGS_PACKAGE_NAME, eventQueue);
    }

    @Test
    void testComponentsAreForwardedAfterBackupValidation() {
        underTest = new DatalakeUpgradePreparationFlowEventChainFactory();
        DatalakeUpgradePreparationFlowChainStartEvent event = new DatalakeUpgradePreparationFlowChainStartEvent(
                0L, "userId", "imageId", "backupLocation", SdxTestUtil.getUpgradeComponents());

        FlowTriggerEventQueue queue = underTest.createFlowTriggerEventQueue(event);

        queue.getQueue().remove();
        DatalakeUpgradePreparationStartEvent preparationEvent = (DatalakeUpgradePreparationStartEvent) queue.getQueue().remove();
        assertSame(event.getComponents(), preparationEvent.getComponents());
        assertEquals(event.getImageId(), preparationEvent.getImageId());
    }

}
