package com.sequenceiq.freeipa.flow.freeipa.chain;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.freeipa.flow.chain.FlowChainTriggers;
import com.sequenceiq.freeipa.flow.chain.RepairFlowEventChainFactory;
import com.sequenceiq.freeipa.flow.freeipa.repair.event.RepairEvent;

public class RepairFlowEventChainFactoryTest {

    private static final long STACK_ID = 1L;

    private static final String OPERATION_ID = "operation-id";

    private static final int INSTANCE_COUNT_BY_GROUP = 2;

    private static final List<String> INSTANCE_IDS_TO_REPAIR = List.of("instance1");

    private static final List<String> TERMINATED_INSTANCE_IDS = List.of("instance2");

    @InjectMocks
    private RepairFlowEventChainFactory underTest;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testRepair() {
        RepairEvent event = new RepairEvent(FlowChainTriggers.REPAIR_TRIGGER_EVENT, STACK_ID,
                OPERATION_ID, INSTANCE_COUNT_BY_GROUP, INSTANCE_IDS_TO_REPAIR, TERMINATED_INSTANCE_IDS);

        Queue<Selectable> eventQueues = underTest.createFlowTriggerEventQueue(event);

        List<String> triggeredOperations = eventQueues.stream().map(Selectable::selector).collect(Collectors.toList());
        assertEquals(List.of("DOWNSCALE_EVENT", "UPSCALE_EVENT"), triggeredOperations);
    }
}
