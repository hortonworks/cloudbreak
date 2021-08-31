package com.sequenceiq.freeipa.flow.chain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleEvent;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.ChangePrimaryGatewayEvent;
import com.sequenceiq.freeipa.flow.freeipa.repair.event.RepairEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleEvent;

@Component
public class RepairFlowEventChainFactory implements FlowEventChainFactory<RepairEvent> {
    @Override
    public String initEvent() {
        return FlowChainTriggers.REPAIR_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(RepairEvent event) {
        Set<String> terminatedOrRemovedInstanceIdsSet = new HashSet<>(event.getRepairInstanceIds());
        terminatedOrRemovedInstanceIdsSet.addAll(event.getAdditionalTerminatedInstanceIds());
        ArrayList<String> terminatedOrRemovedInstanceIds = new ArrayList<>(terminatedOrRemovedInstanceIdsSet);

        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new ChangePrimaryGatewayEvent(ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_EVENT.event(), event.getResourceId(),
                terminatedOrRemovedInstanceIds, Boolean.FALSE, event.getOperationId(), event.accepted()));
        flowEventChain.add(new DownscaleEvent(DownscaleFlowEvent.DOWNSCALE_EVENT.event(),
                event.getResourceId(), terminatedOrRemovedInstanceIds, event.getInstanceCountByGroup(), true, true, false, event.getOperationId()));
        flowEventChain.add(new UpscaleEvent(UpscaleFlowEvent.UPSCALE_EVENT.event(),
                event.getResourceId(), event.getInstanceCountByGroup(), true, true, false, event.getOperationId()));
        flowEventChain.add(new ChangePrimaryGatewayEvent(ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_EVENT.event(), event.getResourceId(),
                terminatedOrRemovedInstanceIds, true, event.getOperationId()));
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }
}
