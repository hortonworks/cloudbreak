package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncEvent.STACK_SYNC_EVENT;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartds.StopStartDownscaleEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterAndStackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackSyncTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StopStartDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class StopStartDownscaleFlowEventChainFactory implements FlowEventChainFactory<ClusterAndStackDownscaleTriggerEvent> {

    @Inject
    private StackService stackService;

    @Override
    public String initEvent() {
        return FlowChainTriggers.STOPSTART_DOWNSCALE_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(ClusterAndStackDownscaleTriggerEvent event) {

        StackView stackView = stackService.getViewByIdWithoutAuth(event.getResourceId());
        Map<String, Set<Long>> hostGroupsWithPrivateIds = event.getHostGroupsWithPrivateIds();

        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();

        // TODO CB-14929: Is a stack sync really required here. What does it do ? (As of now it also serves to accept the event)
        addStackSyncTriggerEvent(event, flowEventChain);

        if (hostGroupsWithPrivateIds.keySet().size() > 1) {
            throw new BadRequestException("Start stop downscale flow was intended to handle only 1 hostgroup.");
        }
        for (Map.Entry<String, Set<Long>> hostGroupWithPrivateIds : hostGroupsWithPrivateIds.entrySet()) {
            StopStartDownscaleTriggerEvent te = new StopStartDownscaleTriggerEvent(
                    StopStartDownscaleEvent.STOPSTART_DOWNSCALE_TRIGGER_EVENT.event(),
                    stackView.getId(),
                    hostGroupWithPrivateIds.getKey(),
                    hostGroupWithPrivateIds.getValue()
            );
            flowEventChain.add(te);
        }

        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }

    private void addStackSyncTriggerEvent(ClusterAndStackDownscaleTriggerEvent event, Queue<Selectable> flowEventChain) {
        flowEventChain.add(new StackSyncTriggerEvent(
                STACK_SYNC_EVENT.event(),
                event.getResourceId(),
                false,
                event.accepted())
        );
    }
}
