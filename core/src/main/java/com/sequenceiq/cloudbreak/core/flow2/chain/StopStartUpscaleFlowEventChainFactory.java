package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncEvent.STACK_SYNC_EVENT;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartus.StopStartUpscaleEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackAndClusterUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackSyncTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StopStartUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class StopStartUpscaleFlowEventChainFactory implements FlowEventChainFactory<StackAndClusterUpscaleTriggerEvent> {
    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private StackUtil stackUtil;

    @Override
    public String initEvent() {
        return FlowChainTriggers.STOPSTART_UPSCALE_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(StackAndClusterUpscaleTriggerEvent event) {
        StackView stackView = stackDtoService.getStackViewById(event.getResourceId());
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        addStackSyncTriggerEvent(event, flowEventChain);
        addClusterScaleTriggerEventIfNeeded(event, stackView, flowEventChain);
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }

    // TODO CB-14929: Is a stack sync actually required?
    private void addStackSyncTriggerEvent(StackAndClusterUpscaleTriggerEvent event, Queue<Selectable> flowEventChain) {
        flowEventChain.add(new StackSyncTriggerEvent(
                STACK_SYNC_EVENT.event(),
                event.getResourceId(),
                false,
                event.accepted())
        );
    }

    private void addClusterScaleTriggerEventIfNeeded(StackAndClusterUpscaleTriggerEvent event, StackView stackView, Queue<Selectable> flowEventChain) {
        Map<String, Integer> hostGroupsWithAdjustment = event.getHostGroupsWithAdjustment();
        boolean stopStartFailureRecoveryEnabled = stackUtil.stopStartScalingFailureRecoveryEnabled(stackView);
        if (hostGroupsWithAdjustment.keySet().size() > 1) {
            throw new BadRequestException("Start stop upscale flow was intended to handle only 1 hostgroup.");
        }
        for (Map.Entry<String, Integer> hostGroupWithAdjustment : hostGroupsWithAdjustment.entrySet()) {
            flowEventChain.add(
                    new StopStartUpscaleTriggerEvent(
                            StopStartUpscaleEvent.STOPSTART_UPSCALE_TRIGGER_EVENT.event(),
                            stackView.getId(),
                            hostGroupWithAdjustment.getKey(),
                            hostGroupWithAdjustment.getValue(),
                            event.getClusterManagerType(),
                            stopStartFailureRecoveryEnabled));
        }
    }
}
