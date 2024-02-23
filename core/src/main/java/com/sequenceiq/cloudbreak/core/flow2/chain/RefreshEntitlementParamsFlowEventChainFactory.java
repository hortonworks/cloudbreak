package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent.SALT_UPDATE_EVENT;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.event.RefreshEntitlementParamsFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.RefreshEntitlementParamsTriggerEvent;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

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
            flowEventChain.add(new StackEvent(SALT_UPDATE_EVENT.event(), event.getResourceId(), event.accepted()));
        }
        flowEventChain.add(RefreshEntitlementParamsTriggerEvent.fromChainTrigger(event));
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }
}
