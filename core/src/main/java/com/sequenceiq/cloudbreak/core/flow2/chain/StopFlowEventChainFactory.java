package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.stop.ClusterStopEvent.CLUSTER_STOP_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopEvent.STACK_STOP_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncEvent.STACK_SYNC_EVENT;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.event.StackSyncTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;

@Component
public class StopFlowEventChainFactory implements FlowEventChainFactory<StackEvent> {
    @Override
    public String initEvent() {
        return FlowChainTriggers.FULL_STOP_TRIGGER_EVENT;
    }

    @Override
    public Queue<Selectable> createFlowTriggerEventQueue(StackEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new StackEvent(CLUSTER_STOP_EVENT.event(), event.getResourceId(), event.accepted()));
        flowEventChain.add(new StackEvent(STACK_STOP_EVENT.event(), event.getResourceId()));
        flowEventChain.add(new StackSyncTriggerEvent(STACK_SYNC_EVENT.event(), event.getResourceId(), true, event.accepted()));
        return flowEventChain;
    }
}
