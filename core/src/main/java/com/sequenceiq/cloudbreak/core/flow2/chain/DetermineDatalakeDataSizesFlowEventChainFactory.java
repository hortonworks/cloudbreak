package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.metrics.datasizes.DetermineDatalakeDataSizesEvent.DETERMINE_DATALAKE_DATA_SIZES_EVENT;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.datalakemetrics.datasizes.DetermineDatalakeDataSizesBaseEvent;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class DetermineDatalakeDataSizesFlowEventChainFactory implements FlowEventChainFactory<DetermineDatalakeDataSizesBaseEvent> {
    @Override
    public String initEvent() {
        return FlowChainTriggers.DETERMINE_DATALAKE_DATA_SIZES_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(DetermineDatalakeDataSizesBaseEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
//        flowEventChain.add(new StackEvent(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), event.getResourceId(), event.accepted()));
        flowEventChain.add(new DetermineDatalakeDataSizesBaseEvent(
                DETERMINE_DATALAKE_DATA_SIZES_EVENT.event(), event.getResourceId(), event.getOperationId(), event.accepted()
        ));
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }
}
