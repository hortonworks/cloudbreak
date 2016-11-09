package com.sequenceiq.cloudbreak.core.flow2.stack.repair;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowEventChainFactory;
import com.sequenceiq.cloudbreak.core.flow2.event.StackAndClusterUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.stack.repair.UnhealthyInstances;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
public class StackRepairFlowEventChainFactory implements FlowEventChainFactory<StackRepairTriggerEvent> {
    @Override
    public String initEvent() {
        return FlowChainTriggers.STACK_REPAIR_TRIGGER_EVENT;
    }

    @Override
    public Queue<Selectable> createFlowTriggerEventQueue(StackRepairTriggerEvent event) {
        Queue<Selectable> flowChainTriggers = new ConcurrentLinkedDeque<>();
        flowChainTriggers.add(new StackEvent(FlowChainTriggers.FULL_SYNC_TRIGGER_EVENT, event.getStackId()));
        UnhealthyInstances unhealthyInstances = event.getUnhealthyInstances();
        String fullUpscaleTriggerEvent = FlowChainTriggers.FULL_UPSCALE_TRIGGER_EVENT;
        for (String hostGroupName : unhealthyInstances.getHostGroups()) {
            List<String> instances = unhealthyInstances.getInstancesForGroup(hostGroupName);
            flowChainTriggers.add(
                    new StackAndClusterUpscaleTriggerEvent(fullUpscaleTriggerEvent, event.getStackId(), hostGroupName,
                            instances.size(), ScalingType.UPSCALE_TOGETHER));
        }
        return flowChainTriggers;
    }

}
