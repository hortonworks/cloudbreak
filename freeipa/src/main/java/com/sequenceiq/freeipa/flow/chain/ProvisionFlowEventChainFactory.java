package com.sequenceiq.freeipa.flow.chain;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowEventChainFactory;
import com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionEvent;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent;

@Component
public class ProvisionFlowEventChainFactory implements FlowEventChainFactory<StackEvent> {
    @Override
    public String initEvent() {
        return FlowChainTriggers.PROVISION_TRIGGER_EVENT;
    }

    @Override
    public Queue<Selectable> createFlowTriggerEventQueue(StackEvent event) {

        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new StackEvent(StackProvisionEvent.START_CREATION_EVENT.event(), event.getStackId(), event.accepted()));
        flowEventChain.add(new StackEvent(FreeIpaProvisionEvent.FREEIPA_PROVISION_EVENT.event(), event.getStackId()));
        return flowEventChain;
    }
}
