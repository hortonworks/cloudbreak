package com.sequenceiq.cloudbreak.core.flow2.chain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Queue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.metrics.datasizes.DetermineDatalakeDataSizesEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.datalakemetrics.datasizes.DetermineDatalakeDataSizesBaseEvent;

public class DetermineDatalakeDataSizesFlowEventChainFactoryTest {
    private static final Long STACK_ID = 1L;

    private static final String OPERATION_ID = "operation-id";

    @InjectMocks
    private DetermineDatalakeDataSizesFlowEventChainFactory factory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void chainCreationTest() {
        DetermineDatalakeDataSizesBaseEvent event = new DetermineDatalakeDataSizesBaseEvent(
                FlowChainTriggers.DETERMINE_DATALAKE_DATA_SIZES_CHAIN_TRIGGER_EVENT, STACK_ID, OPERATION_ID
        );
        Queue<Selectable> flowQueue = factory.createFlowTriggerEventQueue(event).getQueue();
        assertEquals(2, flowQueue.size());

        checkEventIsSaltUpdate(flowQueue.remove());
        checkEventIsDetermineDatalakeDataSizes(flowQueue.remove());
    }

    private void checkEventIsSaltUpdate(Selectable event) {
        assertEquals(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), event.selector());
        assertTrue(event instanceof StackEvent);
        assertEquals(STACK_ID, event.getResourceId());
    }

    private void checkEventIsDetermineDatalakeDataSizes(Selectable event) {
        assertEquals(DetermineDatalakeDataSizesEvent.DETERMINE_DATALAKE_DATA_SIZES_EVENT.event(), event.selector());
        assertTrue(event instanceof DetermineDatalakeDataSizesBaseEvent);
        assertEquals(STACK_ID, event.getResourceId());
    }
}
