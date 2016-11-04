package com.sequenceiq.cloudbreak.core.flow2.chain;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterAndStackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterScaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackScaleTriggerEvent;
import com.sequenceiq.cloudbreak.domain.Constraint;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class DownscaleFlowEventChainFactory implements FlowEventChainFactory<ClusterAndStackDownscaleTriggerEvent> {
    @Inject
    private StackService stackService;

    @Inject
    private HostGroupService hostGroupService;

    @Override
    public String initEvent() {
        return FlowChainTriggers.FULL_DOWNSCALE_TRIGGER_EVENT;
    }

    @Override
    public Queue<Selectable> createFlowTriggerEventQueue(ClusterAndStackDownscaleTriggerEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new ClusterScaleTriggerEvent(FlowTriggers.CLUSTER_DOWNSCALE_TRIGGER_EVENT, event.getStackId(), event.getHostGroupName(),
                event.getAdjustment(), event.accepted()));
        if (event.getScalingType() == ScalingType.DOWNSCALE_TOGETHER) {
            Stack stack = stackService.getById(event.getStackId());
            HostGroup hostGroup = hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), event.getHostGroupName());
            Constraint hostGroupConstraint = hostGroup.getConstraint();
            String instanceGroupName = Optional.ofNullable(hostGroupConstraint.getInstanceGroup()).map(InstanceGroup::getGroupName).orElse(null);
            flowEventChain.add(new StackScaleTriggerEvent(FlowTriggers.STACK_DOWNSCALE_TRIGGER_EVENT, event.getStackId(), instanceGroupName,
                    event.getAdjustment()));
        }
        return flowEventChain;
    }
}
