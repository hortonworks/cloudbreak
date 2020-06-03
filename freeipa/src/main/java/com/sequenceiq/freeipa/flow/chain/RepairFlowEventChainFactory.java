package com.sequenceiq.freeipa.flow.chain;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleEvent;
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

        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new DownscaleEvent(DownscaleFlowEvent.DOWNSCALE_EVENT.event(),
                event.getResourceId(), event.getInstanceIds(), event.getInstanceCountByGroup(), Boolean.TRUE, event.getOperationId(), event.accepted()));
        flowEventChain.add(new UpscaleEvent(UpscaleFlowEvent.UPSCALE_EVENT.event(),
                event.getResourceId(), event.getInstanceCountByGroup(), Boolean.TRUE, event.getOperationId()));
        return flowEventChain;
    }
}
