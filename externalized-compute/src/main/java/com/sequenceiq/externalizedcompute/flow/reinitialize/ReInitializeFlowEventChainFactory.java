package com.sequenceiq.externalizedcompute.flow.reinitialize;

import static com.sequenceiq.externalizedcompute.flow.chain.ExternalizedComputeClusterFlowChainTriggers.EXTERNALIZED_COMPUTE_CLUSTER_REINIT_TRIGGER_EVENT;
import static com.sequenceiq.externalizedcompute.flow.create.ExternalizedComputeClusterCreateFlowEvent.EXTERNALIZED_COMPUTE_CLUSTER_CREATE_INITIATED_EVENT;
import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteFlowEvent.EXTERNALIZED_COMPUTE_CLUSTER_DELETE_INITIATED_EVENT;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.externalizedcompute.flow.ExternalizedComputeClusterEvent;
import com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteEvent;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class ReInitializeFlowEventChainFactory implements FlowEventChainFactory<ExternalizedComputeClusterEvent> {

    @Override
    public String initEvent() {
        return EXTERNALIZED_COMPUTE_CLUSTER_REINIT_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(ExternalizedComputeClusterEvent event) {
        Long resourceId = event.getResourceId();
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new ExternalizedComputeClusterDeleteEvent(EXTERNALIZED_COMPUTE_CLUSTER_DELETE_INITIATED_EVENT.event(), resourceId,
                event.getActorCrn(), true, true, event.accepted()));
        flowEventChain.add(new ExternalizedComputeClusterEvent(EXTERNALIZED_COMPUTE_CLUSTER_CREATE_INITIATED_EVENT.event(), resourceId,
                        event.getActorCrn()));

        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }
}
