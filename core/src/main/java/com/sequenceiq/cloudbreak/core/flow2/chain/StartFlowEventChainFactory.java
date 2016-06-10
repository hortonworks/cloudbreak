package com.sequenceiq.cloudbreak.core.flow2.chain;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

@Component
public class StartFlowEventChainFactory implements FlowEventChainFactory<StackEvent> {

    @Override
    public String initEvent() {
        return FlowTriggers.FULL_START_TRIGGER_EVENT;
    }

    @Override
    public Queue<Selectable> createFlowTriggerEventQueue(StackEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new StackEvent(FlowTriggers.STACK_START_TRIGGER_EVENT, event.getStackId()));
        flowEventChain.add(new StackEvent(FlowTriggers.CLUSTER_START_TRIGGER_EVENT, event.getStackId()));
        return flowEventChain;
    }
}
