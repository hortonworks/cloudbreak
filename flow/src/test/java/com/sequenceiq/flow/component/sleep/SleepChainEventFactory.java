package com.sequenceiq.flow.component.sleep;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.component.sleep.event.SleepChainTriggerEvent;
import com.sequenceiq.flow.component.sleep.event.SleepStartEvent;

public class SleepChainEventFactory implements FlowEventChainFactory<SleepChainTriggerEvent> {

    public static final String SLEEP_CHAIN_TRIGGER_EVENT = "SLEEP_CHAIN_TRIGGER_EVENT";

    @Override
    public String initEvent() {
        return SLEEP_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(SleepChainTriggerEvent event) {
        return new FlowTriggerEventQueue(getName(), event, new ConcurrentLinkedQueue<>(event.getSleepConfigs()
                .stream()
                .map(config -> new SleepStartEvent(event.getResourceId(), config.getSleepTime(), config.getFailUntil(), event.accepted()))
                .collect(Collectors.toList())));
    }
}
