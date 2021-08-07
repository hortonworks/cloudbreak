package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.terminate.config.ExternalDatabaseTerminationEvent.START_EXTERNAL_DATABASE_TERMINATION_EVENT;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationEvent;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class ProperTerminationFlowEventChainFactory implements FlowEventChainFactory<TerminationEvent> {
    @Override
    public String initEvent() {
        return FlowChainTriggers.PROPER_TERMINATION_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(TerminationEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new TerminationEvent(ClusterTerminationEvent.PROPER_TERMINATION_EVENT.event(), event.getResourceId(), event.getTerminationType()));
        flowEventChain.add(new TerminationEvent(START_EXTERNAL_DATABASE_TERMINATION_EVENT.event(), event.getResourceId(), event.getTerminationType()));
        flowEventChain.add(new TerminationEvent(StackTerminationEvent.TERMINATION_EVENT.event(), event.getResourceId(), event.getTerminationType(),
                event.accepted()));
        return new FlowTriggerEventQueue(getName(), flowEventChain);
    }
}
