package com.sequenceiq.environment.environment.flow.delete.config;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.environment.environment.flow.delete.event.EnvDeleteStateSelectors;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;

@Component
public class EnvDeleteFlowChainFactory implements FlowEventChainFactory<BaseFlowEvent> {

    @Override
    public String initEvent() {
        return "ENV_DELETE_CHAIN_EVENT";
    }

    @Override
    public Queue<Selectable> createFlowTriggerEventQueue(BaseFlowEvent event) {
        Queue<Selectable> flowChainTriggers = new ConcurrentLinkedDeque<>();
        flowChainTriggers.add(new BaseFlowEvent(EnvDeleteStateSelectors.START_FREEIPA_DELETE_EVENT.event(), event.getResourceId(), event.accepted()));
        return flowChainTriggers;
    }
}
