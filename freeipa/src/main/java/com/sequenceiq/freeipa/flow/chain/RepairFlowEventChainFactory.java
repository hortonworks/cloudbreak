package com.sequenceiq.freeipa.flow.chain;

import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
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
    public Queue<Selectable> createFlowTriggerEventQueue(RepairEvent event) {
        Set<String> terminatedOrRemovedInstanceIdsSet = new HashSet<>(event.getRepairInstanceIds());
        terminatedOrRemovedInstanceIdsSet.addAll(event.getAdditionalTerminatedInstanceIds());
        List<String> terminatedOrRemovedInstanceIds = terminatedOrRemovedInstanceIdsSet.stream().collect(Collectors.toList());


        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new ChangePrimaryGatewayEvent(ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_EVENT.event(), event.getResourceId(),
                terminatedOrRemovedInstanceIds, Boolean.FALSE, event.getOperationId(), event.accepted()));
        flowEventChain.add(new DownscaleEvent(DownscaleFlowEvent.DOWNSCALE_EVENT.event(),
                event.getResourceId(), terminatedOrRemovedInstanceIds, event.getInstanceCountByGroup(), Boolean.TRUE, event.getOperationId()));
        flowEventChain.add(new UpscaleEvent(UpscaleFlowEvent.UPSCALE_EVENT.event(),
                event.getResourceId(), event.getInstanceCountByGroup(), Boolean.TRUE, event.getOperationId()));
        flowEventChain.add(new ChangePrimaryGatewayEvent(ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_EVENT.event(), event.getResourceId(),
                terminatedOrRemovedInstanceIds, Boolean.TRUE, event.getOperationId()));
        return flowEventChain;
    }
}
