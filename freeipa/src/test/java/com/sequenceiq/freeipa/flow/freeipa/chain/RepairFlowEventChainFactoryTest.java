package com.sequenceiq.freeipa.flow.freeipa.chain;

import static com.sequenceiq.freeipa.flow.graph.FlowOfflineStateGraphGenerator.FLOW_CONFIGS_PACKAGE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.graph.FlowChainConfigGraphGeneratorUtil;
import com.sequenceiq.freeipa.flow.chain.FlowChainTriggers;
import com.sequenceiq.freeipa.flow.chain.RepairFlowEventChainFactory;
import com.sequenceiq.freeipa.flow.freeipa.repair.event.RepairEvent;

class RepairFlowEventChainFactoryTest {

    private static final long STACK_ID = 1L;

    private static final String OPERATION_ID = "operation-id";

    private static final int INSTANCE_COUNT_BY_GROUP = 2;

    private static final List<String> INSTANCE_IDS_TO_REPAIR = List.of("instance1");

    private static final List<String> TERMINATED_INSTANCE_IDS = List.of("instance2");

    private RepairFlowEventChainFactory underTest = new RepairFlowEventChainFactory();

    @Test
    void testRepair() {
        RepairEvent event = new RepairEvent(FlowChainTriggers.REPAIR_TRIGGER_EVENT, STACK_ID,
                OPERATION_ID, INSTANCE_COUNT_BY_GROUP, INSTANCE_IDS_TO_REPAIR, TERMINATED_INSTANCE_IDS);

        FlowTriggerEventQueue flowTriggerEventQueue = underTest.createFlowTriggerEventQueue(event);
        Queue<Selectable> eventQueues = flowTriggerEventQueue.getQueue();

        List<String> triggeredOperations = eventQueues.stream().map(Selectable::selector).collect(Collectors.toList());
        assertEquals(List.of("CHANGE_PRIMARY_GATEWAY_EVENT", "DOWNSCALE_EVENT", "UPSCALE_EVENT", "CHANGE_PRIMARY_GATEWAY_EVENT"), triggeredOperations);
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE_NAME, flowTriggerEventQueue);
    }
}
