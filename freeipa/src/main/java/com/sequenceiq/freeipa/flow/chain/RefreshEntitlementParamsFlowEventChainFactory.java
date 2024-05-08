package com.sequenceiq.freeipa.flow.chain;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateTriggerEvent;
import com.sequenceiq.freeipa.flow.stack.dynamicentitlement.RefreshEntitlementParamsFlowChainTriggerEvent;
import com.sequenceiq.freeipa.flow.stack.dynamicentitlement.RefreshEntitlementParamsTriggerEvent;

@Component
public class RefreshEntitlementParamsFlowEventChainFactory implements FlowEventChainFactory<RefreshEntitlementParamsFlowChainTriggerEvent> {

    @Override
    public String initEvent() {
        return FlowChainTriggers.REFRESH_ENTITLEMENT_PARAM_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(RefreshEntitlementParamsFlowChainTriggerEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        if (event.getSaltRefreshNeeded()) {
            flowEventChain.add(new SaltUpdateTriggerEvent(event.getResourceId(),
                    event.accepted(), true, false, event.getOperationId()));
        }
        flowEventChain.add(RefreshEntitlementParamsTriggerEvent.fromChainTrigger(event, true, true));
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }
}
