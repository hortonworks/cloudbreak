package com.sequenceiq.flow.core.helloworld.config;

import static com.sequenceiq.flow.core.helloworld.config.HelloWorldEvent.HELLOWORLD_TRIGGER_EVENT;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;

@Component
public class HelloWorldFlowChainFactory implements FlowEventChainFactory<BaseFlowEvent> {
    @Override
    public String initEvent() {
        return "HELLOWORLD_CHAIN_EVENT";
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(BaseFlowEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedDeque<>();
        flowEventChain.add(new BaseFlowEvent(HELLOWORLD_TRIGGER_EVENT.event(), event.getResourceId(), event.getResourceCrn(), event.accepted()));
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }
}
