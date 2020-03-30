package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_UPSCALE_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncEvent.STACK_SYNC_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.ADD_INSTANCES_EVENT;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterScaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackAndClusterUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackScaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackSyncTriggerEvent;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.view.ClusterView;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;

@Component
public class UpscaleFlowEventChainFactory implements FlowEventChainFactory<StackAndClusterUpscaleTriggerEvent> {
    @Inject
    private StackService stackService;

    @Inject
    private HostGroupService hostGroupService;

    @Override
    public String initEvent() {
        return FlowChainTriggers.FULL_UPSCALE_TRIGGER_EVENT;
    }

    @Override
    public Queue<Selectable> createFlowTriggerEventQueue(StackAndClusterUpscaleTriggerEvent event) {
        StackView stackView = stackService.getViewByIdWithoutAuth(event.getResourceId());
        ClusterView clusterView = stackView.getClusterView();
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        addStackSyncTriggerEvent(event, flowEventChain);
        addStackScaleTriggerEvent(event, flowEventChain);
        addClusterScaleTriggerEventIfNeeded(event, stackView, clusterView, flowEventChain);
        return flowEventChain;
    }

    private void addStackSyncTriggerEvent(StackAndClusterUpscaleTriggerEvent event, Queue<Selectable> flowEventChain) {
        flowEventChain.add(new StackSyncTriggerEvent(
                STACK_SYNC_EVENT.event(),
                event.getResourceId(),
                false,
                event.accepted())
        );
    }

    private void addClusterScaleTriggerEventIfNeeded(StackAndClusterUpscaleTriggerEvent event, StackView stackView, ClusterView clusterView,
            Queue<Selectable> flowEventChain) {
        if (ScalingType.isClusterUpScale(event.getScalingType()) && clusterView != null) {
            HostGroup hostGroup = hostGroupService.getByClusterIdAndName(clusterView.getId(), event.getInstanceGroup())
                    .orElseThrow(NotFoundException.notFound("hostgroup", event.getInstanceGroup()));
            flowEventChain.add(
                    new ClusterScaleTriggerEvent(CLUSTER_UPSCALE_TRIGGER_EVENT.event(),
                            stackView.getId(),
                            hostGroup.getName(),
                            event.getAdjustment(),
                            Sets.newHashSet(event.getHostNames()),
                            event.isSingleMasterGateway(),
                            event.isKerberosSecured(),
                            event.isSingleNodeCluster(),
                            event.getClusterManagerType()
                    )
            );
        }
    }

    private void addStackScaleTriggerEvent(StackAndClusterUpscaleTriggerEvent event, Queue<Selectable> flowEventChain) {
        StackScaleTriggerEvent stackScaleTriggerEvent = new StackScaleTriggerEvent(
                ADD_INSTANCES_EVENT.event(),
                event.getResourceId(),
                event.getInstanceGroup(),
                event.getAdjustment(),
                event.getHostNames());
        stackScaleTriggerEvent = event.isRepair() ? stackScaleTriggerEvent.setRepair() : stackScaleTriggerEvent;
        flowEventChain.add(stackScaleTriggerEvent);
    }
}
