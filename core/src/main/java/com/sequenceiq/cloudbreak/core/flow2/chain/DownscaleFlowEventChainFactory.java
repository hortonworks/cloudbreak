package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.DECOMMISSION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.STACK_DOWNSCALE_EVENT;

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
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class DownscaleFlowEventChainFactory implements FlowEventChainFactory<ClusterAndStackDownscaleTriggerEvent> {
    @Inject
    private StackService stackService;

    @Override
    public String initEvent() {
        return FlowChainTriggers.FULL_DOWNSCALE_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(ClusterAndStackDownscaleTriggerEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        ClusterScaleTriggerEvent cste = new ClusterDownscaleTriggerEvent(DECOMMISSION_EVENT.event(), event.getResourceId(), event.getHostGroupsWithAdjustment(),
                event.getHostGroupsWithPrivateIds(), event.getHostGroupsWithHostNames(), event.accepted(), event.getDetails());
        flowEventChain.add(cste);
        if (event.getScalingType() == ScalingType.DOWNSCALE_TOGETHER) {
            StackView stackView = stackService.getViewByIdWithoutAuth(event.getResourceId());
            StackScaleTriggerEvent sste = new StackDownscaleTriggerEvent(STACK_DOWNSCALE_EVENT.event(), event.getResourceId(),
                    event.getHostGroupsWithAdjustment(), event.getHostGroupsWithPrivateIds(), event.getHostGroupsWithHostNames(),
                    stackView.getPlatformVariant());
            if (event.getDetails() != null && event.getDetails().isRepair()) {
                sste.setRepair();
            }
            flowEventChain.add(sste);
        }
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }
}
