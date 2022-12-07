package com.sequenceiq.cloudbreak.core.flow2.chain;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.ModifyProxyConfigEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.update.userdata.UpdateUserDataEvents;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.modifyproxy.ModifyProxyConfigFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.modifyproxy.ModifyProxyConfigRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UserDataUpdateRequest;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class ModifyProxyConfigFlowEventChainFactory implements FlowEventChainFactory<ModifyProxyConfigFlowChainTriggerEvent> {

    @Override
    public String initEvent() {
        return FlowChainTriggers.MODIFY_PROXY_CONFIG_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(ModifyProxyConfigFlowChainTriggerEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new StackEvent(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), event.getResourceId(), event.accepted()));
        flowEventChain.add(new ModifyProxyConfigRequest(ModifyProxyConfigEvent.MODIFY_PROXY_CONFIG_EVENT.selector(),
                event.getResourceId(), event.getPreviousProxyConfigCrn()));
        flowEventChain.add(UserDataUpdateRequest.builder()
                .withSelector(UpdateUserDataEvents.UPDATE_USERDATA_TRIGGER_EVENT.event())
                .withStackId(event.getResourceId())
                .withModifyProxyConfig(true)
                .build());
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }
}
