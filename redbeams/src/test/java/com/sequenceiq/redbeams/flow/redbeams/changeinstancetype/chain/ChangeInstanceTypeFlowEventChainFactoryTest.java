package com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.chain;

import static com.sequenceiq.redbeams.flow.graph.FlowOfflineStateGraphGenerator.FLOW_CONFIGS_PACKAGE;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.CHANGE_INSTANCE_TYPE_START_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.graph.FlowChainConfigGraphGeneratorUtil;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeEvent;

class ChangeInstanceTypeFlowEventChainFactoryTest {

    private static final Long RESOURCE_ID = 1L;

    private static final String TARGET_INSTANCE_TYPE = "db.m5.xlarge";

    private final ChangeInstanceTypeFlowEventChainFactory underTest = new ChangeInstanceTypeFlowEventChainFactory();

    @Test
    void testInitEvent() {
        assertEquals(EventSelectorUtil.selector(ChangeInstanceTypeFlowChainTriggerEvent.class), underTest.initEvent());
    }

    @Test
    void testCreateFlowTriggerEventQueue() {
        ChangeInstanceTypeFlowChainTriggerEvent triggerEvent = new ChangeInstanceTypeFlowChainTriggerEvent(underTest.initEvent(), RESOURCE_ID,
                TARGET_INSTANCE_TYPE);

        FlowTriggerEventQueue eventQueue = underTest.createFlowTriggerEventQueue(triggerEvent);
        List<Selectable> events = List.copyOf(eventQueue.getQueue());

        assertEquals(1, events.size());
        ChangeInstanceTypeEvent startEvent = assertInstanceOf(ChangeInstanceTypeEvent.class, events.getFirst());
        assertEquals(CHANGE_INSTANCE_TYPE_START_EVENT.event(), startEvent.selector());
        assertEquals(RESOURCE_ID, startEvent.getResourceId());
        assertEquals(TARGET_INSTANCE_TYPE, startEvent.getTargetInstanceType());
        assertNull(startEvent.getLiveState());
        assertSame(triggerEvent.accepted(), startEvent.accepted());
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE, eventQueue, "CHANGE_INSTANCE_TYPE");
    }
}
