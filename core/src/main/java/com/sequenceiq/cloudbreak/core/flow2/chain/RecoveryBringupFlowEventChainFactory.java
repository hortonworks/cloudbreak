package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLUSTER_CREATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent.START_CREATION_EVENT;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ProvisionEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ProvisionType;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class RecoveryBringupFlowEventChainFactory implements FlowEventChainFactory<StackEvent> {

    @Override
    public String initEvent() {
        return FlowChainTriggers.RECOVERY_PROVISION_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(StackEvent event) {

        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new ProvisionEvent(START_CREATION_EVENT.event(), event.getResourceId(), ProvisionType.RECOVERY, event.accepted()));
        flowEventChain.add(new ProvisionEvent(CLUSTER_CREATION_EVENT.event(), event.getResourceId(), ProvisionType.RECOVERY));
        return new FlowTriggerEventQueue(getName(), flowEventChain);
    }
}
