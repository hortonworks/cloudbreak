package com.sequenceiq.flow.component.sleep;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.component.sleep.event.NestedSleepChainTriggerEvent;

public class NestedSleepChainEventFactory implements FlowEventChainFactory<NestedSleepChainTriggerEvent> {

    @Override
    public String initEvent() {
        return NestedSleepChainTriggerEvent.NESTED_SLEEP_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(NestedSleepChainTriggerEvent event) {
        return new FlowTriggerEventQueue(getName(), event, new ConcurrentLinkedQueue<>(event.getSleepChainTriggerEvents()));
    }
}
