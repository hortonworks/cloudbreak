package com.sequenceiq.cloudbreak.core.flow2.chain;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import org.springframework.stereotype.Component;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent.CLUSTER_START_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartEvent.STACK_START_EVENT;

@Component
public class StartFlowEventChainFactory implements FlowEventChainFactory<StackEvent> {

    @Override
    public String initEvent() {
        return FlowChainTriggers.FULL_START_TRIGGER_EVENT;
    }

    @Override
    public Queue<Selectable> createFlowTriggerEventQueue(StackEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new StackEvent(STACK_START_EVENT.event(), event.getStackId(), event.accepted()));
        flowEventChain.add(new StackEvent(CLUSTER_START_EVENT.event(), event.getStackId()));
        return flowEventChain;
    }
}
