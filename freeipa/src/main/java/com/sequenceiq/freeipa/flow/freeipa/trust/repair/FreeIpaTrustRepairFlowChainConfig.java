package com.sequenceiq.freeipa.flow.freeipa.trust.repair;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.freeipa.flow.chain.FlowChainTriggers;
import com.sequenceiq.freeipa.flow.freeipa.trust.finish.event.FinishTrustSetupEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.repair.event.TrustRepairEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.TrustSetupEvent;

@Component
public class FreeIpaTrustRepairFlowChainConfig implements FlowEventChainFactory<TrustRepairEvent> {

    @Override
    public String initEvent() {
        return FlowChainTriggers.REPAIR_TRUST_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(TrustRepairEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new TrustSetupEvent(event.getResourceId(), event.getOperationId(), event.accepted()));
        flowEventChain.add(new FinishTrustSetupEvent(event.getResourceId(), event.getOperationId()));
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }
}
