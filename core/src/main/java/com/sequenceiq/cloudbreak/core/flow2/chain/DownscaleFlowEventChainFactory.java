package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.DECOMMISSION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.STACK_DOWNSCALE_EVENT;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterAndStackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterScaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackScaleTriggerEvent;
import com.sequenceiq.cloudbreak.domain.Constraint;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.view.StackView;
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
        ClusterScaleTriggerEvent cste;
        if (event.getPrivateIds() == null) {
            cste = new ClusterDownscaleTriggerEvent(DECOMMISSION_EVENT.event(), event.getStackId(), event.getHostGroupName(),
                    event.getAdjustment(), event.accepted());
        } else {
            cste = new ClusterDownscaleTriggerEvent(DECOMMISSION_EVENT.event(), event.getStackId(), event.getHostGroupName(),
                    event.getPrivateIds(), event.accepted());
        }
        flowEventChain.add(cste);
        if (event.getScalingType() == ScalingType.DOWNSCALE_TOGETHER) {
            StackView stackView = stackService.getByIdView(event.getStackId());
            HostGroup hostGroup = hostGroupService.getByClusterIdAndName(stackView.getClusterView().getId(), event.getHostGroupName());
            Constraint hostGroupConstraint = hostGroup.getConstraint();
            String instanceGroupName = Optional.ofNullable(hostGroupConstraint.getInstanceGroup()).map(InstanceGroup::getGroupName).orElse(null);
            StackScaleTriggerEvent sste;
            if (event.getPrivateIds() == null) {
                sste = new StackDownscaleTriggerEvent(STACK_DOWNSCALE_EVENT.event(), event.getStackId(), instanceGroupName, event.getAdjustment());
            } else {
                sste = new StackDownscaleTriggerEvent(STACK_DOWNSCALE_EVENT.event(), event.getStackId(), instanceGroupName, event.getPrivateIds());
            }
            flowEventChain.add(sste);
        }
        return flowEventChain;
    }
}
