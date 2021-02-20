package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.DECOMMISSION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.STACK_DOWNSCALE_EVENT;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterAndStackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterScaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackScaleTriggerEvent;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

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
    public FlowTriggerEventQueue createFlowTriggerEventQueue(ClusterAndStackDownscaleTriggerEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        ClusterScaleTriggerEvent cste;
        cste = event.getPrivateIds() == null
                ? new ClusterDownscaleTriggerEvent(DECOMMISSION_EVENT.event(), event.getResourceId(), event.getHostGroupName(), event.getAdjustment(),
                event.accepted(), event.getDetails())
                : new ClusterDownscaleTriggerEvent(DECOMMISSION_EVENT.event(), event.getResourceId(), event.getHostGroupName(), event.getPrivateIds(),
                event.accepted(), event.getDetails());
        flowEventChain.add(cste);
        if (event.getScalingType() == ScalingType.DOWNSCALE_TOGETHER) {
            StackView stackView = stackService.getViewByIdWithoutAuth(event.getResourceId());
            HostGroup hostGroup = hostGroupService.findHostGroupInClusterByName(stackView.getClusterView().getId(), event.getHostGroupName())
                    .orElseThrow(NotFoundException.notFound("hostgroup", event.getHostGroupName()));
            String instanceGroupName = Optional.ofNullable(hostGroup.getInstanceGroup()).map(InstanceGroup::getGroupName).orElse(null);
            StackScaleTriggerEvent sste;
            sste = event.getPrivateIds() == null
                    ? new StackDownscaleTriggerEvent(STACK_DOWNSCALE_EVENT.event(), event.getResourceId(), instanceGroupName, event.getAdjustment())
                    : new StackDownscaleTriggerEvent(STACK_DOWNSCALE_EVENT.event(), event.getResourceId(), instanceGroupName, event.getPrivateIds());
            flowEventChain.add(sste);
        }
        return new FlowTriggerEventQueue(getName(), flowEventChain);
    }
}
