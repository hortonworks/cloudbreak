package com.sequenceiq.freeipa.flow.chain;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.event.ModifyProxyFlowChainTriggerEvent;
import com.sequenceiq.freeipa.flow.stack.update.UpdateUserDataEvents;
import com.sequenceiq.freeipa.flow.stack.update.event.UserDataUpdateRequest;

@Component
public class ModifyProxyConfigFlowEventChainFactory implements FlowEventChainFactory<ModifyProxyFlowChainTriggerEvent> {

    @Override
    public String initEvent() {
        return FlowChainTriggers.MODIFY_PROXY_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(ModifyProxyFlowChainTriggerEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new UserDataUpdateRequest(UpdateUserDataEvents.UPDATE_USERDATA_TRIGGER_EVENT.event(), event.getResourceId(), event.accepted())
                .withOperationId(event.getOperationId())
                .withIsChained(true)
                .withIsFinal(true));
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }
}
