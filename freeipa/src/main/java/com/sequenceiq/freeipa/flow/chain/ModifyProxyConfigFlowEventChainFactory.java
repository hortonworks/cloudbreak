package com.sequenceiq.freeipa.flow.chain;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateTriggerEvent;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.event.ModifyProxyConfigTriggerEvent;
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
        flowEventChain.add(new SaltUpdateTriggerEvent(event.getResourceId(), event.accepted(), true, false, event.getOperationId()));
        flowEventChain.add(new ModifyProxyConfigTriggerEvent(event.getResourceId(), event.accepted(), true, false, event.getOperationId()));
        flowEventChain.add(
                UserDataUpdateRequest.builder()
                        .withSelector(UpdateUserDataEvents.UPDATE_USERDATA_TRIGGER_EVENT.event())
                        .withStackId(event.getResourceId())
                        .withModifyProxyConfig(true)
                        .withAccepted(event.accepted())
                        .withOperationId(event.getOperationId())
                        .withChained(true)
                        .withFinalFlow(true)
                        .build());
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }
}
