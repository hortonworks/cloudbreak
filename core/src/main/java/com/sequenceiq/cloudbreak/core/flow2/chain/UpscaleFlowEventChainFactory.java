package com.sequenceiq.cloudbreak.core.flow2.chain;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterScaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackAndClusterUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackScaleTriggerEvent;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class UpscaleFlowEventChainFactory implements FlowEventChainFactory<StackAndClusterUpscaleTriggerEvent> {
    @Inject
    private StackService stackService;
    @Inject
    private HostGroupService hostGroupService;

    @Override
    public String initEvent() {
        return FlowTriggers.FULL_UPSCALE_TRIGGER_EVENT;
    }

    @Override
    public Queue<Selectable> createFlowTriggerEventQueue(StackAndClusterUpscaleTriggerEvent event) {
        Stack stack = stackService.getById(event.getStackId());
        Cluster cluster = stack.getCluster();
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new StackEvent(FlowTriggers.STACK_SYNC_TRIGGER_EVENT, event.getStackId()));
        flowEventChain.add(new StackScaleTriggerEvent(FlowTriggers.STACK_UPSCALE_TRIGGER_EVENT, event.getStackId(), event.getInstanceGroup(),
                event.getAdjustment()));
        if (ScalingType.isClusterUpScale(event.getScalingType()) && cluster != null) {
            HostGroup hostGroup = hostGroupService.getByClusterIdAndInstanceGroupName(cluster.getId(), event.getInstanceGroup());
            flowEventChain.add(new ClusterScaleTriggerEvent(FlowTriggers.CLUSTER_UPSCALE_TRIGGER_EVENT, stack.getId(),
                    hostGroup.getName(), event.getAdjustment()));
        }
        return flowEventChain;
    }
}
