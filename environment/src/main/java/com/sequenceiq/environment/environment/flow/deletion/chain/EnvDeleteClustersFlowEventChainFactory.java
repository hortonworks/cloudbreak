package com.sequenceiq.environment.environment.flow.deletion.chain;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvClustersDeleteStateSelectors.START_DATAHUB_CLUSTERS_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_FREEIPA_DELETE_EVENT;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;

@Component
public class EnvDeleteClustersFlowEventChainFactory implements FlowEventChainFactory<EnvDeleteEvent> {

    @Override
    public String initEvent() {
        return FlowChainTriggers.ENV_DELETE_CLUSTERS_TRIGGER_EVENT;
    }

    @Override
    public Queue<Selectable> createFlowTriggerEventQueue(EnvDeleteEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        //TODO builder
        flowEventChain.add(new EnvDeleteEvent(START_DATAHUB_CLUSTERS_DELETE_EVENT.event(), event.getResourceId(), event.accepted(), event.getResourceName(),
                event.getResourceCrn()));
        flowEventChain.add(new EnvDeleteEvent(START_FREEIPA_DELETE_EVENT.event(), event.getResourceId(), event.getResourceName(), event.getResourceCrn()));
        return flowEventChain;
    }
}
