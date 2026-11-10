package com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.chain;

import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.CHANGE_INSTANCE_TYPE_START_EVENT;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeEvent;

@Component
public class ChangeInstanceTypeFlowEventChainFactory implements FlowEventChainFactory<ChangeInstanceTypeFlowChainTriggerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeInstanceTypeFlowEventChainFactory.class);

    @Override
    public String initEvent() {
        return EventSelectorUtil.selector(ChangeInstanceTypeFlowChainTriggerEvent.class);
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(ChangeInstanceTypeFlowChainTriggerEvent event) {
        LOGGER.debug("Creating flow trigger event queue for changing the database server instance type with event {}", event);
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new ChangeInstanceTypeEvent(CHANGE_INSTANCE_TYPE_START_EVENT.event(), event.getResourceId(), event.accepted(),
                event.getTargetInstanceType(), null));
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }
}
