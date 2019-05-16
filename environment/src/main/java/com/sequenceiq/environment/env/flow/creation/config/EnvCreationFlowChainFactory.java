package com.sequenceiq.environment.env.flow.creation.config;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.environment.env.flow.creation.event.EnvCreationStateSelectors;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;

@Component
public class EnvCreationFlowChainFactory implements FlowEventChainFactory<BaseFlowEvent> {

    @Override
    public String initEvent() {
        return "ENV_CREATION_CHAIN_EVENT";
    }

    @Override
    public Queue<Selectable> createFlowTriggerEventQueue(BaseFlowEvent event) {
        Queue<Selectable> flowChainTriggers = new ConcurrentLinkedDeque<>();
        flowChainTriggers.add(new BaseFlowEvent(EnvCreationStateSelectors.START_NETWORK_CREATION_EVENT.event(), event.getResourceId(), event.accepted()));
        return flowChainTriggers;
    }
}
