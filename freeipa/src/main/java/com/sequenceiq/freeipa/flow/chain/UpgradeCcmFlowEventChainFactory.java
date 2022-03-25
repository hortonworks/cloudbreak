package com.sequenceiq.freeipa.flow.chain;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.freeipa.flow.stack.update.UpdateUserDataEvents;
import com.sequenceiq.freeipa.flow.stack.update.event.UserDataUpdateRequest;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmFlowChainTriggerEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmTriggerEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector;

@Component
public class UpgradeCcmFlowEventChainFactory implements FlowEventChainFactory<UpgradeCcmFlowChainTriggerEvent> {
    @Override
    public String initEvent() {
        return FlowChainTriggers.UPGRADE_CCM_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(UpgradeCcmFlowChainTriggerEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new UpgradeCcmTriggerEvent(UpgradeCcmStateSelector.UPGRADE_CCM_TRIGGER_EVENT.event(), event.getOperationId(), event.getResourceId(),
                event.accepted())
                .withIsChained(true)
                .withIsFinal(false));
        flowEventChain.add(new UserDataUpdateRequest(UpdateUserDataEvents.UPDATE_USERDATA_TRIGGER_EVENT.event(), event.getResourceId())
                .withOperationId(event.getOperationId())
                .withIsChained(true)
                .withIsFinal(true));
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }
}
