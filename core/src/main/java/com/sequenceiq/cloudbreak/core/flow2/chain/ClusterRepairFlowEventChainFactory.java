package com.sequenceiq.cloudbreak.core.flow2.chain;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterAndStackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackAndClusterUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent;

@Component
public class ClusterRepairFlowEventChainFactory implements FlowEventChainFactory<ClusterRepairTriggerEvent> {

    @Override
    public String initEvent() {
        return FlowChainTriggers.CLUSTER_REPAIR_TRIGGER_EVENT;
    }

    @Override
    public Queue<Selectable> createFlowTriggerEventQueue(ClusterRepairTriggerEvent event) {
        Queue<Selectable> flowChainTriggers = new ConcurrentLinkedDeque<>();
        Map<String, List<String>> failedNodesMap = event.getFailedNodesMap();
        for (Map.Entry<String, List<String>> failedNodes : failedNodesMap.entrySet()) {
            flowChainTriggers.add(new ClusterAndStackDownscaleTriggerEvent(FlowChainTriggers.FULL_DOWNSCALE_TRIGGER_EVENT, event.getStackId(),
                    failedNodes.getKey(), new HashSet<>(failedNodes.getValue()), ScalingType.DOWNSCALE_TOGETHER, event.accepted()));
            flowChainTriggers.add(new StackAndClusterUpscaleTriggerEvent(FlowChainTriggers.FULL_UPSCALE_TRIGGER_EVENT, event.getStackId(),
                    failedNodes.getKey(), failedNodes.getValue().size(), ScalingType.UPSCALE_TOGETHER));
        }
        return flowChainTriggers;
    }
}
