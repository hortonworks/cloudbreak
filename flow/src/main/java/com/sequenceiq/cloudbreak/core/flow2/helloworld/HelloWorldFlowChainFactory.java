package com.sequenceiq.cloudbreak.core.flow2.helloworld;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowEventChainFactory;
import com.sequenceiq.cloudbreak.reactor.api.event.BaseFlowEvent;

@Component
public class HelloWorldFlowChainFactory implements FlowEventChainFactory<BaseFlowEvent> {
    @Override
    public String initEvent() {
        return "HELLOWORLD_CHAIN_EVENT";
    }

    @Override
    public Queue<Selectable> createFlowTriggerEventQueue(BaseFlowEvent event) {
        Queue<Selectable> flowChainTriggers = new ConcurrentLinkedDeque<>();
        flowChainTriggers.add(new BaseFlowEvent(HelloWorldEvent.START_HELLO_WORLD_EVENT.event(), event.getStackId(), event.accepted()));
        return flowChainTriggers;
    }
}
