package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.DECOMMISSION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_UPSCALE_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.STACK_DOWNSCALE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncEvent.STACK_SYNC_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent.ADD_INSTANCES_EVENT;

import java.util.Collections;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.apache.commons.collections4.MapUtils;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleDetails;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterScaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackAndClusterUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackScaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackSyncTriggerEvent;
import com.sequenceiq.cloudbreak.domain.view.ClusterView;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class UpscaleFlowEventChainFactory implements FlowEventChainFactory<StackAndClusterUpscaleTriggerEvent> {

    @Inject
    private StackService stackService;

    @Value("${cb.upscale.zombie.auto.cleanup.enabled}")
    private boolean zombieAutoCleanupEnabled;

    @Override
    public String initEvent() {
        return FlowChainTriggers.FULL_UPSCALE_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(StackAndClusterUpscaleTriggerEvent event) {
        StackView stackView = stackService.getViewByIdWithoutAuth(event.getResourceId());
        ClusterView clusterView = stackView.getClusterView();
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        addStackSyncTriggerEvent(event, flowEventChain);
        addStackScaleTriggerEvent(event, flowEventChain);
        addClusterScaleTriggerEventIfNeeded(event, stackView, clusterView, flowEventChain);
        if (zombieAutoCleanupEnabled) {
            addClusterDownscaleTriggerEventForZombieNodes(event, flowEventChain);
            addStackDownscaleTriggerEventForZombieNodes(event, flowEventChain, stackView);
        }
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
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
            flowEventChain.add(
                    new ClusterScaleTriggerEvent(CLUSTER_UPSCALE_TRIGGER_EVENT.event(),
                            stackView.getId(),
                            event.getHostGroupsWithAdjustment(),
                            event.getHostGroupsWithPrivateIds(),
                            event.getHostGroupsWithHostNames(),
                            event.isSingleMasterGateway(),
                            event.isKerberosSecured(),
                            event.isSingleNodeCluster(),
                            event.isRestartServices(),
                            event.getClusterManagerType()
                    )
            );
        }
    }

    private void addStackScaleTriggerEvent(StackAndClusterUpscaleTriggerEvent event, Queue<Selectable> flowEventChain) {
        StackScaleTriggerEvent stackScaleTriggerEvent = new StackScaleTriggerEvent(
                ADD_INSTANCES_EVENT.event(),
                event.getResourceId(),
                event.getHostGroupsWithAdjustment(),
                event.getHostGroupsWithPrivateIds(),
                event.getHostGroupsWithHostNames(),
                event.getNetworkScaleDetails(),
                event.getAdjustmentTypeWithThreshold(),
                event.getTriggeredStackVariant());
        stackScaleTriggerEvent = event.isRepair() ? stackScaleTriggerEvent.setRepair() : stackScaleTriggerEvent;
        flowEventChain.add(stackScaleTriggerEvent);
    }

    private void addClusterDownscaleTriggerEventForZombieNodes(StackAndClusterUpscaleTriggerEvent event, Queue<Selectable> flowEventChain) {
        Set<String> hostGroups = getHostGroups(event);
        if (!event.isRepair() && !hostGroups.isEmpty()) {
            flowEventChain.add(new ClusterDownscaleTriggerEvent(DECOMMISSION_EVENT.event(), event.getResourceId(), hostGroups,
                    event.accepted(), new ClusterDownscaleDetails(false, false, true)));
        }
    }

    private void addStackDownscaleTriggerEventForZombieNodes(StackAndClusterUpscaleTriggerEvent event, Queue<Selectable> flowEventChain, StackView stackView) {
        Set<String> hostGroups = getHostGroups(event);
        if (!event.isRepair() && !hostGroups.isEmpty()) {
            flowEventChain.add(new StackDownscaleTriggerEvent(STACK_DOWNSCALE_EVENT.event(), event.getResourceId(), Collections.emptyMap(),
                    Collections.emptyMap(), Collections.emptyMap(), stackView.getPlatformVariant()));
        }
    }

    private Set<String> getHostGroups(StackAndClusterUpscaleTriggerEvent event) {
        Set<String> hostGroups = new HashSet<>();
        if (MapUtils.isNotEmpty(event.getHostGroupsWithHostNames())) {
            hostGroups.addAll(event.getHostGroupsWithHostNames().keySet());
        } else if (MapUtils.isNotEmpty(event.getHostGroupsWithPrivateIds())) {
            hostGroups.addAll(event.getHostGroupsWithPrivateIds().keySet());
        } else if (MapUtils.isNotEmpty(event.getHostGroupsWithAdjustment())) {
            hostGroups.addAll(event.getHostGroupsWithAdjustment().keySet());
        }
        return hostGroups;
    }
}
