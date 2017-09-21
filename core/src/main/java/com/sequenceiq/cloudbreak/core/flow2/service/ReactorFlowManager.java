package com.sequenceiq.cloudbreak.core.flow2.service;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.BYOS;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLUSTER_CREATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent.CLUSTER_START_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.sync.ClusterSyncEvent.CLUSTER_SYNC_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_UPSCALE_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd.ClusterCredentialChangeEvent.CLUSTER_CREDENTIALCHANGE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.STACK_DOWNSCALE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.repair.ManualStackRepairTriggerEvent.MANUAL_STACK_REPAIR_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopEvent.STACK_STOP_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncEvent.STACK_SYNC_EVENT;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.cloud.Acceptable;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.controller.CloudbreakApiException;
import com.sequenceiq.cloudbreak.controller.FlowsAlreadyRunningException;
import com.sequenceiq.cloudbreak.core.flow2.Flow2Handler;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.EphemeralClusterEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterAndStackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterCredentialChangeTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterScaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.InstanceTerminationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackAndClusterUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackScaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackSyncTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.EphemeralClusterUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StackRepairTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationEvent;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.repair.UnhealthyInstances;

import reactor.bus.Event;
import reactor.bus.EventBus;

/**
 * Flow manager implementation backed by Reactor.
 * This class is the flow state machine and mediates between the states and reactor events
 */
@Service
public class ReactorFlowManager {

    private static final long WAIT_FOR_ACCEPT = 5L;

    @Inject
    private EventBus reactor;

    @Inject
    private ErrorHandlerAwareFlowEventFactory eventFactory;

    @Inject
    private StackService stackService;

    public void triggerProvisioning(Long stackId) {
        String selector = FlowChainTriggers.FULL_PROVISION_TRIGGER_EVENT;
        notify(selector, new StackEvent(selector, stackId));
    }

    public void triggerStackStart(Long stackId) {
        String selector = FlowChainTriggers.FULL_START_TRIGGER_EVENT;
        StackEvent startTriggerEvent = new StackEvent(selector, stackId);
        notify(selector, startTriggerEvent);
    }

    public void triggerStackStop(Long stackId) {
        String selector = STACK_STOP_EVENT.event();
        notify(selector, new StackEvent(selector, stackId));
    }

    public void triggerStackUpscale(Long stackId, InstanceGroupAdjustmentJson instanceGroupAdjustment, boolean withClusterEvent) {
        String selector = FlowChainTriggers.FULL_UPSCALE_TRIGGER_EVENT;
        StackAndClusterUpscaleTriggerEvent stackAndClusterUpscaleTriggerEvent = new StackAndClusterUpscaleTriggerEvent(selector,
                stackId, instanceGroupAdjustment.getInstanceGroup(), instanceGroupAdjustment.getScalingAdjustment(),
                withClusterEvent ? ScalingType.UPSCALE_TOGETHER : ScalingType.UPSCALE_ONLY_STACK);
        notify(selector, stackAndClusterUpscaleTriggerEvent);
    }

    public void triggerStackDownscale(Long stackId, InstanceGroupAdjustmentJson instanceGroupAdjustment) {
        String selector = STACK_DOWNSCALE_EVENT.event();
        StackScaleTriggerEvent stackScaleTriggerEvent = new StackDownscaleTriggerEvent(selector, stackId, instanceGroupAdjustment.getInstanceGroup(),
                instanceGroupAdjustment.getScalingAdjustment());
        notify(selector, stackScaleTriggerEvent);
    }

    public void triggerStackSync(Long stackId) {
        String selector = STACK_SYNC_EVENT.event();
        notify(selector, new StackSyncTriggerEvent(selector, stackId, true));
    }

    public void triggerStackRemoveInstance(Long stackId, String instanceId) {
        String selector = InstanceTerminationEvent.TERMINATION_EVENT.event();
        InstanceTerminationTriggerEvent event = new InstanceTerminationTriggerEvent(selector, stackId, Collections.singleton(instanceId));
        notify(selector, event);
    }

    public void triggerTermination(Long stackId, Boolean deleteDependencies) {
        Stack stack = stackService.getById(stackId);

        if (BYOS.equals(stack.cloudPlatform())) {
            String selector = FlowChainTriggers.BYOS_CLUSTER_TERMINATION_TRIGGER_EVENT;
            notify(selector, new TerminationEvent(selector, stackId, null, deleteDependencies));
        } else {
            String selector = StackTerminationEvent.TERMINATION_EVENT.event();
            notify(selector, new TerminationEvent(selector, stackId, deleteDependencies));
        }
        cancelRunningFlows(stackId);
    }

    public void triggerForcedTermination(Long stackId, Boolean deleteDependencies) {
        Stack stack = stackService.get(stackId);

        if (BYOS.equals(stack.cloudPlatform())) {
            String selector = FlowChainTriggers.BYOS_CLUSTER_TERMINATION_TRIGGER_EVENT;
            notify(selector, new TerminationEvent(selector, stackId, null, deleteDependencies));
        } else {
            String selector = StackTerminationEvent.FORCE_TERMINATION_EVENT.event();
            notify(selector, new TerminationEvent(selector, stackId, deleteDependencies));
        }
        cancelRunningFlows(stackId);
    }

    public void triggerClusterInstall(Long stackId) {
        String selector = CLUSTER_CREATION_EVENT.event();
        notify(selector, new StackEvent(selector, stackId));
    }

    public void triggerEphemeralUpdate(Long stackId) {
        String selector = EphemeralClusterEvent.EPHEMERAL_CLUSTER_UPDATE_TRIGGER_EVENT.event();
        notify(selector, new EphemeralClusterUpdateTriggerEvent(selector, stackId));
    }

    public void triggerClusterReInstall(Long stackId) {
        String selector = FlowChainTriggers.CLUSTER_RESET_CHAIN_TRIGGER_EVENT;
        notify(selector, new StackEvent(selector, stackId));
    }

    public void triggerClusterUpgrade(Long stackId) {
        String selector = FlowChainTriggers.CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT;
        reactor.notify(selector, eventFactory.createEventWithErrHandler(new StackEvent(selector, stackId)));
    }

    public void triggerClusterCredentialReplace(Long stackId, String userName, String password) {
        String selector = CLUSTER_CREDENTIALCHANGE_EVENT.event();
        ClusterCredentialChangeTriggerEvent event = ClusterCredentialChangeTriggerEvent.replaceUserEvent(selector, stackId, userName, password);
        notify(selector, event);
    }

    public void triggerClusterCredentialUpdate(Long stackId, String password) {
        String selector = CLUSTER_CREDENTIALCHANGE_EVENT.event();
        ClusterCredentialChangeTriggerEvent event = ClusterCredentialChangeTriggerEvent.changePasswordEvent(selector, stackId, password);
        notify(selector, event);
    }

    public void triggerClusterUpscale(Long stackId, HostGroupAdjustmentJson hostGroupAdjustment) {
        String selector = CLUSTER_UPSCALE_TRIGGER_EVENT.event();
        ClusterScaleTriggerEvent event = new ClusterScaleTriggerEvent(selector, stackId,
                hostGroupAdjustment.getHostGroup(), hostGroupAdjustment.getScalingAdjustment());
        notify(selector, event);
    }

    public void triggerClusterDownscale(Long stackId, HostGroupAdjustmentJson hostGroupAdjustment) {
        String selector = FlowChainTriggers.FULL_DOWNSCALE_TRIGGER_EVENT;
        ScalingType scalingType = hostGroupAdjustment.getWithStackUpdate() ? ScalingType.DOWNSCALE_TOGETHER : ScalingType.DOWNSCALE_ONLY_CLUSTER;
        ClusterAndStackDownscaleTriggerEvent event = new ClusterAndStackDownscaleTriggerEvent(selector, stackId,
                hostGroupAdjustment.getHostGroup(), hostGroupAdjustment.getScalingAdjustment(), scalingType);
        notify(selector, event);
    }

    public void triggerClusterStart(Long stackId) {
        String selector = CLUSTER_START_EVENT.event();
        notify(selector, new StackEvent(selector, stackId));
    }

    public void triggerClusterStop(Long stackId) {
        String selector = FlowChainTriggers.FULL_STOP_TRIGGER_EVENT;
        notify(selector, new StackEvent(selector, stackId));
    }

    public void triggerClusterSync(Long stackId) {
        String selector = CLUSTER_SYNC_EVENT.event();
        notify(selector, new StackEvent(selector, stackId));
    }

    public void triggerFullSync(Long stackId) {
        String selector = FlowChainTriggers.FULL_SYNC_TRIGGER_EVENT;
        notify(selector, new StackEvent(selector, stackId));
    }

    public void triggerClusterTermination(Long stackId) {
        Stack stack = stackService.get(stackId);
        if (BYOS.equals(stack.cloudPlatform())) {
            String selector = FlowChainTriggers.BYOS_CLUSTER_TERMINATION_TRIGGER_EVENT;
            notify(selector, new StackEvent(selector, stackId, null));
        } else {
            String selector = ClusterTerminationEvent.TERMINATION_EVENT.event();
            notify(selector, new StackEvent(selector, stackId));
        }
        cancelRunningFlows(stackId);
    }

    public void triggerManualRepairFlow(Long stackId) {
        String selector = MANUAL_STACK_REPAIR_TRIGGER_EVENT.event();
        notify(selector, new StackEvent(selector, stackId));
    }

    public void triggerStackRepairFlow(Long stackId, UnhealthyInstances unhealthyInstances) {
        String selector = FlowChainTriggers.STACK_REPAIR_TRIGGER_EVENT;
        notify(selector, new StackRepairTriggerEvent(stackId, unhealthyInstances));
    }

    public void triggerClusterRepairFlow(Long stackId, Map<String, List<String>> failedNodesMap, boolean removeOnly) {
        notify(FlowChainTriggers.CLUSTER_REPAIR_TRIGGER_EVENT, new ClusterRepairTriggerEvent(stackId, failedNodesMap, removeOnly));
    }

    public void cancelRunningFlows(Long stackId) {
        StackEvent cancelEvent = new StackEvent(Flow2Handler.FLOW_CANCEL, stackId);
        reactor.notify(Flow2Handler.FLOW_CANCEL, eventFactory.createEventWithErrHandler(cancelEvent));
    }

    private void notify(String selector, Acceptable acceptable) {
        Event<Acceptable> event = eventFactory.createEventWithErrHandler(acceptable);
        reactor.notify(selector, event);
        try {
            Boolean accepted = true;
            if (event.getData().accepted() != null) {
                accepted = event.getData().accepted().await(WAIT_FOR_ACCEPT, TimeUnit.SECONDS);
            }
            if (accepted == null || !accepted) {
                throw new FlowsAlreadyRunningException(String.format("Stack %d has flows under operation, request not allowed.", event.getData().getStackId()));
            }
        } catch (InterruptedException e) {
            throw new CloudbreakApiException(e.getMessage());
        }
    }
}
