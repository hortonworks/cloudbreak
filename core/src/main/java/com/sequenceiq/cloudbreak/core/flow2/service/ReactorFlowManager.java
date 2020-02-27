package com.sequenceiq.cloudbreak.core.flow2.service;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrenew.ClusterCertificateRenewEvent.CLUSTER_CERTIFICATE_REISSUE_EVENT;
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
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.EphemeralClusterEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterAndStackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterCredentialChangeTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleDetails;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterScaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.MaintenanceModeValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.MultiHostgroupClusterAndStackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackAndClusterUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackImageUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackSyncTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.EphemeralClusterUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StackRepairTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationEvent;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.repair.UnhealthyInstances;

import reactor.rx.Promise;

/**
 * Flow manager implementation backed by Reactor.
 * This class is the flow state machine and mediates between the states and reactor events
 */
@Service
public class ReactorFlowManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReactorFlowManager.class);

    @Inject
    private StackService stackService;

    @Inject
    private TerminationTriggerService terminationTriggerService;

    @Inject
    private ReactorNotifier reactorNotifier;

    @Inject
    private FlowCancelService flowCancelService;

    @Inject
    private AsyncTaskExecutor intermediateBuilderExecutor;

    public void triggerProvisioning(Long stackId) {
        String selector = FlowChainTriggers.FULL_PROVISION_TRIGGER_EVENT;
        reactorNotifier.notify(stackId, selector, new StackEvent(selector, stackId));
    }

    public void triggerStackStart(Long stackId) {
        String selector = FlowChainTriggers.FULL_START_TRIGGER_EVENT;
        Acceptable startTriggerEvent = new StackEvent(selector, stackId);
        reactorNotifier.notify(stackId, selector, startTriggerEvent);
    }

    public void triggerStackStop(Long stackId) {
        String selector = STACK_STOP_EVENT.event();
        reactorNotifier.notify(stackId, selector, new StackEvent(selector, stackId));
    }

    public void triggerStackUpscale(Long stackId, InstanceGroupAdjustmentV4Request instanceGroupAdjustment, boolean withClusterEvent) {
        String selector = FlowChainTriggers.FULL_UPSCALE_TRIGGER_EVENT;
        Acceptable stackAndClusterUpscaleTriggerEvent = new StackAndClusterUpscaleTriggerEvent(selector,
                stackId, instanceGroupAdjustment.getInstanceGroup(), instanceGroupAdjustment.getScalingAdjustment(),
                withClusterEvent ? ScalingType.UPSCALE_TOGETHER : ScalingType.UPSCALE_ONLY_STACK);
        reactorNotifier.notify(stackId, selector, stackAndClusterUpscaleTriggerEvent);
    }

    public void triggerStackDownscale(Long stackId, InstanceGroupAdjustmentV4Request instanceGroupAdjustment) {
        String selector = STACK_DOWNSCALE_EVENT.event();
        Acceptable stackScaleTriggerEvent = new StackDownscaleTriggerEvent(selector, stackId, instanceGroupAdjustment.getInstanceGroup(),
                instanceGroupAdjustment.getScalingAdjustment());
        reactorNotifier.notify(stackId, selector, stackScaleTriggerEvent);
    }

    public void triggerStackSync(Long stackId) {
        String selector = STACK_SYNC_EVENT.event();
        reactorNotifier.notify(stackId, selector, new StackSyncTriggerEvent(selector, stackId, true));
    }

    public void triggerStackRemoveInstance(Long stackId, String hostGroup, Long privateId) {
        triggerStackRemoveInstance(stackId, hostGroup, privateId, false);
    }

    public void triggerStackRemoveInstance(Long stackId, String hostGroup, Long privateId, boolean forced) {
        String selector = FlowChainTriggers.FULL_DOWNSCALE_TRIGGER_EVENT;
        ClusterDownscaleDetails details = new ClusterDownscaleDetails(forced, false);
        ClusterAndStackDownscaleTriggerEvent event = new ClusterAndStackDownscaleTriggerEvent(selector, stackId, hostGroup, Collections.singleton(privateId),
                ScalingType.DOWNSCALE_TOGETHER, new Promise<>(), details);
        reactorNotifier.notify(stackId, selector, event);
    }

    public void triggerStackRemoveInstances(Long stackId, Map<String, Set<Long>> instanceIdsByHostgroupMap, boolean forced) {
        String selector = FlowChainTriggers.FULL_DOWNSCALE_MULTIHOSTGROUP_TRIGGER_EVENT;
        ClusterDownscaleDetails details = new ClusterDownscaleDetails(forced, false);
        MultiHostgroupClusterAndStackDownscaleTriggerEvent event = new MultiHostgroupClusterAndStackDownscaleTriggerEvent(selector, stackId,
                instanceIdsByHostgroupMap, details, ScalingType.DOWNSCALE_TOGETHER, new Promise<>());
        reactorNotifier.notify(stackId, selector, event);
    }

    public void triggerTermination(Long stackId, Boolean forced) {
        String selector = StackTerminationEvent.TERMINATION_EVENT.event();
        reactorNotifier.notify(stackId, selector, new TerminationEvent(selector, stackId, forced));
        flowCancelService.cancelRunningFlows(stackId);
    }

    public void triggerClusterInstall(Long stackId) {
        String selector = CLUSTER_CREATION_EVENT.event();
        reactorNotifier.notify(stackId, selector, new StackEvent(selector, stackId));
    }

    public void triggerEphemeralUpdate(Long stackId) {
        String selector = EphemeralClusterEvent.EPHEMERAL_CLUSTER_UPDATE_TRIGGER_EVENT.event();
        reactorNotifier.notify(stackId, selector, new EphemeralClusterUpdateTriggerEvent(selector, stackId));
    }

    public void triggerClusterReInstall(Long stackId) {
        String selector = FlowChainTriggers.CLUSTER_RESET_CHAIN_TRIGGER_EVENT;
        reactorNotifier.notify(stackId, selector, new StackEvent(selector, stackId));
    }

    public void triggerClusterCredentialReplace(Long stackId, String userName, String password) {
        String selector = CLUSTER_CREDENTIALCHANGE_EVENT.event();
        ClusterCredentialChangeTriggerEvent event = ClusterCredentialChangeTriggerEvent.replaceUserEvent(selector, stackId, userName, password);
        reactorNotifier.notify(stackId, selector, event);
    }

    public void triggerClusterCredentialUpdate(Long stackId, String password) {
        String selector = CLUSTER_CREDENTIALCHANGE_EVENT.event();
        ClusterCredentialChangeTriggerEvent event = ClusterCredentialChangeTriggerEvent.changePasswordEvent(selector, stackId, password);
        reactorNotifier.notify(stackId, selector, event);
    }

    public void triggerClusterUpscale(Long stackId, HostGroupAdjustmentV4Request hostGroupAdjustment) {
        String selector = CLUSTER_UPSCALE_TRIGGER_EVENT.event();
        Acceptable event = new ClusterScaleTriggerEvent(selector, stackId,
                hostGroupAdjustment.getHostGroup(), hostGroupAdjustment.getScalingAdjustment());
        reactorNotifier.notify(stackId, selector, event);
    }

    public void triggerClusterDownscale(Long stackId, HostGroupAdjustmentV4Request hostGroupAdjustment) {
        String selector = FlowChainTriggers.FULL_DOWNSCALE_TRIGGER_EVENT;
        ScalingType scalingType = hostGroupAdjustment.getWithStackUpdate() ? ScalingType.DOWNSCALE_TOGETHER : ScalingType.DOWNSCALE_ONLY_CLUSTER;
        Acceptable event = new ClusterAndStackDownscaleTriggerEvent(selector, stackId,
                hostGroupAdjustment.getHostGroup(), hostGroupAdjustment.getScalingAdjustment(), scalingType);
        reactorNotifier.notify(stackId, selector, event);
    }

    public void triggerClusterStart(Long stackId) {
        String selector = CLUSTER_START_EVENT.event();
        reactorNotifier.notify(stackId, selector, new StackEvent(selector, stackId));
    }

    public void triggerClusterStop(Long stackId) {
        String selector = FlowChainTriggers.FULL_STOP_TRIGGER_EVENT;
        reactorNotifier.notify(stackId, selector, new StackEvent(selector, stackId));
    }

    public void triggerClusterSync(Long stackId) {
        String selector = CLUSTER_SYNC_EVENT.event();
        reactorNotifier.notify(stackId, selector, new StackEvent(selector, stackId));
    }

    public void triggerClusterSyncWithoutCheck(Long stackId) {
        String selector = CLUSTER_SYNC_EVENT.event();
        reactorNotifier.notifyWithoutCheck(stackId, selector, new StackEvent(selector, stackId));
    }

    public void triggerFullSync(Long stackId) {
        String selector = FlowChainTriggers.FULL_SYNC_TRIGGER_EVENT;
        reactorNotifier.notify(stackId, selector, new StackEvent(selector, stackId));
    }

    public void triggerFullSyncWithoutCheck(Long stackId) {
        String selector = FlowChainTriggers.FULL_SYNC_TRIGGER_EVENT;
        reactorNotifier.notifyWithoutCheck(stackId, selector, new StackEvent(selector, stackId));
    }

    public void triggerManualRepairFlow(Long stackId) {
        String selector = MANUAL_STACK_REPAIR_TRIGGER_EVENT.event();
        reactorNotifier.notify(stackId, selector, new StackEvent(selector, stackId));
    }

    public void triggerStackRepairFlow(Long stackId, UnhealthyInstances unhealthyInstances) {
        String selector = FlowChainTriggers.STACK_REPAIR_TRIGGER_EVENT;
        reactorNotifier.notify(stackId, selector, new StackRepairTriggerEvent(stackId, unhealthyInstances));
    }

    public void triggerClusterRepairFlow(Long stackId, Map<String, List<String>> failedNodesMap, boolean removeOnly) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        reactorNotifier.notify(stackId, FlowChainTriggers.CLUSTER_REPAIR_TRIGGER_EVENT, new ClusterRepairTriggerEvent(stack, failedNodesMap, removeOnly));
    }

    public void triggerStackImageUpdate(Long stackId, String newImageId, String imageCatalogName, String imageCatalogUrl) {
        String selector = FlowChainTriggers.STACK_IMAGE_UPDATE_TRIGGER_EVENT;
        reactorNotifier.notify(stackId, selector, new StackImageUpdateTriggerEvent(selector, stackId, newImageId, imageCatalogName, imageCatalogUrl));
    }

    public void triggerMaintenanceModeValidationFlow(Long stackId) {
        String selector = FlowChainTriggers.CLUSTER_MAINTENANCE_MODE_VALIDATION_TRIGGER_EVENT;
        reactorNotifier.notify(stackId, selector, new MaintenanceModeValidationTriggerEvent(selector, stackId));
    }

    public void triggerClusterCertificationRenewal(Long stackId) {
        String selector = CLUSTER_CERTIFICATE_REISSUE_EVENT.event();
        reactorNotifier.notify(stackId, selector, new StackEvent(selector, stackId));
    }

    public void triggerClusterTermination(Stack stack, boolean forced, String userCrn) {
        intermediateBuilderExecutor.submit(() -> {
            MDCBuilder.buildMdcContext(stack);
            ThreadBasedUserCrnProvider.doAs(userCrn, () -> {
                LOGGER.debug("Async termination flow trigger for stack: '{}', forced: '{}'", stack.getName(), forced);
                long startedAt = System.currentTimeMillis();
                terminationTriggerService.triggerTermination(stack, forced);
                LOGGER.debug("Async termination flow trigger for stack: '{}' took '{}' ms", stack.getName(), System.currentTimeMillis() - startedAt);
            });
            MDCBuilder.cleanupMdc();
        });
    }
}
