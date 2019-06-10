package com.sequenceiq.flow.core.helloworld.config;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;

@Component
public class HelloWorldFlowChainFactory implements FlowEventChainFactory<BaseFlowEvent> {
    @Override
    public String initEvent() {
        return "HELLOWORLD_CHAIN_EVENT";
    }

    @Override
    public Queue<Selectable> createFlowTriggerEventQueue(BaseFlowEvent event) {
        Queue<Selectable> flowChainTriggers = new ConcurrentLinkedDeque<>();
        flowChainTriggers.add(new BaseFlowEvent(HelloWorldEvent.HELLOWORLD_TRIGGER_EVENT.event(), event.getResourceId(), event.accepted()));
        return flowChainTriggers;
    }
}
