package com.sequenceiq.cloudbreak.core.flow2.chain;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.trustedrealm.UpdateTrustedRealmChainTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.trustedrealm.UpdateTrustedRealmTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.SaltUpdateTriggerEvent;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.core.chain.finalize.flowevents.FlowChainFinalizePayload;
import com.sequenceiq.flow.core.chain.init.flowevents.FlowChainInitPayload;

@Component
public class UpdateTrustedRealmFlowEventChainFactory implements FlowEventChainFactory<UpdateTrustedRealmChainTriggerEvent> {

    @Override
    public String initEvent() {
        return FlowChainTriggers.UPDATE_TRUSTED_REALM_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(UpdateTrustedRealmChainTriggerEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new FlowChainInitPayload(getName(), event.getResourceId(), event.accepted()));
        // Salt update is only needed when adding/updating a trusted realm, not when removing one.
        // During removal, the realm configuration is already gone so re-running Salt would be unnecessary and could fail.
        if (event.isSaltUpdateRequired() && !event.isRemove()) {
            flowEventChain.add(new SaltUpdateTriggerEvent(event.getResourceId(), event.accepted(), false));
        }
        flowEventChain.add(UpdateTrustedRealmTriggerEvent.fromChainTrigger(event));
        flowEventChain.add(new FlowChainFinalizePayload(getName(), event.getResourceId(), event.accepted()));
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }
}
