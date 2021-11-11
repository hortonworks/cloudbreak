package com.sequenceiq.cloudbreak.core.flow2.chain;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.flow2.dto.NetworkScaleDetails;
import com.sequenceiq.cloudbreak.core.flow2.event.StackAndClusterUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StackRepairTriggerEvent;
import com.sequenceiq.cloudbreak.service.stack.repair.UnhealthyInstances;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class StackRepairFlowEventChainFactory implements FlowEventChainFactory<StackRepairTriggerEvent> {

    @Override
    public String initEvent() {
        return FlowChainTriggers.STACK_REPAIR_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(StackRepairTriggerEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedDeque<>();
        flowEventChain.add(new StackEvent(FlowChainTriggers.FULL_SYNC_TRIGGER_EVENT, event.getResourceId(), event.accepted()));
        UnhealthyInstances unhealthyInstances = event.getUnhealthyInstances();
        String fullUpscaleTriggerEvent = FlowChainTriggers.FULL_UPSCALE_TRIGGER_EVENT;
        for (String hostGroupName : unhealthyInstances.getHostGroups()) {
            List<String> instances = unhealthyInstances.getInstancesForGroup(hostGroupName);
            flowEventChain.add(
                    new StackAndClusterUpscaleTriggerEvent(fullUpscaleTriggerEvent, event.getResourceId(), hostGroupName,
                            instances.size(), ScalingType.UPSCALE_TOGETHER, NetworkScaleDetails.getEmpty(),
                            new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, (long) instances.size())));
        }
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }

}
