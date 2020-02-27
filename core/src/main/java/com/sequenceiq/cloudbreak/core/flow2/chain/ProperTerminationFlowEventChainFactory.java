package com.sequenceiq.cloudbreak.core.flow2.chain;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationEvent;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;

@Component
public class ProperTerminationFlowEventChainFactory implements FlowEventChainFactory<TerminationEvent> {
    @Override
    public String initEvent() {
        return FlowChainTriggers.PROPER_TERMINATION_TRIGGER_EVENT;
    }

    @Override
    public Queue<Selectable> createFlowTriggerEventQueue(TerminationEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new TerminationEvent(ClusterTerminationEvent.PROPER_TERMINATION_EVENT.event(), event.getResourceId(), event.getForced()));
        flowEventChain.add(new TerminationEvent(StackTerminationEvent.TERMINATION_EVENT.event(), event.getResourceId(), event.getForced()));
        return flowEventChain;
    }
}
