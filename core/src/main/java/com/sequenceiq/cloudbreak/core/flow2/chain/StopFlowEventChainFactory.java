package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.stop.ClusterStopEvent.CLUSTER_STOP_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.externaldatabase.stop.config.ExternalDatabaseStopEvent.EXTERNAL_DATABASE_COMMENCE_STOP_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopEvent.STACK_STOP_EVENT;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class StopFlowEventChainFactory implements FlowEventChainFactory<StackEvent> {
    @Override
    public String initEvent() {
        return FlowChainTriggers.FULL_STOP_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(StackEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new StackEvent(CLUSTER_STOP_EVENT.event(), event.getResourceId(), event.accepted()));
        flowEventChain.add(new StackEvent(EXTERNAL_DATABASE_COMMENCE_STOP_EVENT.event(), event.getResourceId()));
        flowEventChain.add(new StackEvent(STACK_STOP_EVENT.event(), event.getResourceId()));
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }
}
