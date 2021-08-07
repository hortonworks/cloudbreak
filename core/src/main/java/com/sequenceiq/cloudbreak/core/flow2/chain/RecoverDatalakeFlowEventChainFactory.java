package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.recovery.bringup.DatalakeRecoveryBringupEvent.RECOVERY_BRINGUP_EVENT;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterRecoveryTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationType;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.core.chain.finalize.flowevents.FlowChainFinalizePayload;
import com.sequenceiq.flow.core.chain.init.flowevents.FlowChainInitPayload;

@Component
public class RecoverDatalakeFlowEventChainFactory implements FlowEventChainFactory<ClusterRecoveryTriggerEvent> {

    @Override
    public String initEvent() {
        return FlowChainTriggers.CLUSTER_RECOVERY_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(ClusterRecoveryTriggerEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new FlowChainInitPayload(getName(), event.getResourceId(), event.accepted()));
        flowEventChain.add(new TerminationEvent(FlowChainTriggers.RECOVERY_TERMINATION_TRIGGER_EVENT, event.getResourceId(), TerminationType.RECOVERY));
        flowEventChain.add(new ClusterRecoveryTriggerEvent(RECOVERY_BRINGUP_EVENT.event(), event.getResourceId(), event.accepted()));
        flowEventChain.add(new StackEvent(FlowChainTriggers.RECOVERY_PROVISION_TRIGGER_EVENT, event.getResourceId(), event.accepted()));
        flowEventChain.add(new FlowChainFinalizePayload(getName(), event.getResourceId(), event.accepted()));

        return new FlowTriggerEventQueue(getName(), flowEventChain);
    }
}
