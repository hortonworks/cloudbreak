package com.sequenceiq.freeipa.flow.freeipa.trust.repair.config;

import static com.sequenceiq.freeipa.flow.chain.FlowChainTriggers.TRUST_REPAIR_TRIGGER_EVENT;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.freeipa.flow.freeipa.trust.repair.event.FreeIpaTrustRepairEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.event.FreeIpaTrustSetupFinishEvent;

@Component
public class FreeIpaTrustRepairFlowChainConfig implements FlowEventChainFactory<FreeIpaTrustRepairEvent> {

    @Override
    public String initEvent() {
        return TRUST_REPAIR_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(FreeIpaTrustRepairEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new FreeIpaTrustSetupEvent(event.getResourceId(), event.getOperationId(), event.accepted()));
        flowEventChain.add(new FreeIpaTrustSetupFinishEvent(event.getResourceId(), event.getOperationId()));
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }
}
