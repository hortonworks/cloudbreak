package com.sequenceiq.cloudbreak.core.flow2.chain;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationEvent;

@Component
public class BYOSTerminationFlowEventChainFactory implements FlowEventChainFactory<TerminationEvent> {
    @Override
    public String initEvent() {
        return FlowChainTriggers.BYOS_CLUSTER_TERMINATION_TRIGGER_EVENT;
    }

    @Override
    public Queue<Selectable> createFlowTriggerEventQueue(TerminationEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new StackEvent(ClusterTerminationEvent.TERMINATION_EVENT.event(), event.getStackId(), null));
        flowEventChain.add(new TerminationEvent(StackTerminationEvent.TERMINATION_EVENT.event(), event.getStackId(), null, event.getDeleteDependencies()));
        return flowEventChain;
    }
}
