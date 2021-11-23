package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncEvent.STACK_SYNC_EVENT;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartdownscale.StopStartDownscaleEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterAndStackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StopStartDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackSyncTriggerEvent;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.view.ClusterView;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class StopStartDownscaleFlowEventChainFactory implements FlowEventChainFactory<ClusterAndStackDownscaleTriggerEvent> {

    @Inject
    private StackService stackService;

    @Inject
    private HostGroupService hostGroupService;

    @Override
    public String initEvent() {
        return FlowChainTriggers.STOPSTART_DOWNSCALE_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(ClusterAndStackDownscaleTriggerEvent event) {

        StackView stackView = stackService.getViewByIdWithoutAuth(event.getResourceId());
        ClusterView clusterView = stackView.getClusterView();
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(clusterView.getId(), event.getHostGroupName())
                .orElseThrow(NotFoundException.notFound("hostgroup", event.getHostGroupName()));



        StopStartDownscaleTriggerEvent te = new StopStartDownscaleTriggerEvent(
                StopStartDownscaleEvent.STOPSTART_DOWNSCALE_TRIGGER_EVENT.event(),
                stackView.getId(),
                hostGroup.getName(),
                event.getAdjustment(),
                // TODO CB-14929: This seems sub-optimal. Will need to lookup the hostnames again in a subsequent operation.
                Sets.newHashSet(event.getPrivateIds()),
                event.isSinglePrimaryGateway(),
                event.isRestartServices(),
                event.getClusterManagerType()
        );

        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();

        // TODO CB-14929: Figure out how this event is supposed to be accepted.
        //  Adding this (stack sync) temporarily, just to make sure the event gets accepted. The flow is otherwise not being accepted.

        // TODO CB-14929: Is a stack sync really required here. What does it do ?
        // TODO CB-14929: Is a stack sync required after a downscale completes.
        addStackSyncTriggerEvent(event, flowEventChain);

        flowEventChain.add(te);

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
