package com.sequenceiq.cloudbreak.core.flow2.service;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrenew.ClusterCertificateRenewEvent.CLUSTER_CERTIFICATE_REISSUE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.cmsync.CmSyncEvent.CM_SYNC_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.PillarConfigurationUpdateEvent.PILLAR_CONFIG_UPDATE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.restore.DatabaseRestoreEvent.DATABASE_RESTORE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLUSTER_CREATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent.SALT_UPDATE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.services.restart.ClusterServicesRestartEvent.CLUSTER_SERVICES_RESTART_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent.CLUSTER_START_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.sync.ClusterSyncEvent.CLUSTER_SYNC_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_UPSCALE_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd.ClusterCredentialChangeEvent.CLUSTER_CREDENTIALCHANGE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.STACK_DOWNSCALE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.repair.ManualStackRepairTriggerEvent.MANUAL_STACK_REPAIR_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopEvent.STACK_STOP_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncEvent.STACK_SYNC_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer.StackLoadBalancerUpdateEvent.STACK_LOAD_BALANCER_UPDATE_EVENT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.CertificatesRotationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.rotatepassword.RotateSaltPasswordEvent;
import com.sequenceiq.cloudbreak.core.flow2.dto.NetworkScaleDetails;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterAndStackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterCertificatesRotationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterCredentialChangeTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterCredentialChangeTriggerEvent.Type;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleDetails;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterRecoveryTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterScaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterUpgradeTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.DatabaseBackupTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.DatabaseRestoreTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.DistroXUpgradeTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.MaintenanceModeValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.MultiHostgroupClusterAndStackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackAndClusterUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackImageUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackLoadBalancerUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackSyncTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.clusterproxy.reregister.ClusterProxyReRegistrationEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.CmSyncTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StackRepairTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationType;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.repair.UnhealthyInstances;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.service.FlowCancelService;

import reactor.rx.Promise;

/**
 * Flow manager implementation backed by Reactor.
 * This class is the flow state machine and mediates between the states and reactor events
 */
@Service
public class ReactorFlowManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReactorFlowManager.class);

    @Inject
    private TerminationTriggerService terminationTriggerService;

    @Inject
    private ReactorNotifier reactorNotifier;

    @Inject
    private FlowCancelService flowCancelService;

    @Inject
    private AsyncTaskExecutor intermediateBuilderExecutor;

    @Inject
    private StackService stackService;

    public FlowIdentifier triggerProvisioning(Long stackId) {
        String selector = FlowChainTriggers.FULL_PROVISION_TRIGGER_EVENT;
        return reactorNotifier.notify(stackId, selector, new StackEvent(selector, stackId));
    }

    public FlowIdentifier triggerStackStart(Long stackId) {
        String selector = FlowChainTriggers.FULL_START_TRIGGER_EVENT;
        Acceptable startTriggerEvent = new StackEvent(selector, stackId);
        return reactorNotifier.notify(stackId, selector, startTriggerEvent);
    }

    public FlowIdentifier triggerStackStop(Long stackId) {
        String selector = STACK_STOP_EVENT.event();
        return reactorNotifier.notify(stackId, selector, new StackEvent(selector, stackId));
    }

    public FlowIdentifier triggerStopStartStackUpscale(Long stackId, InstanceGroupAdjustmentV4Request instanceGroupAdjustment, boolean withClusterEvent) {
        LOGGER.debug("FlowManager trigger for stopstart-upscale");
        String selector = FlowChainTriggers.STOPSTART_UPSCALE_CHAIN_TRIGGER_EVENT;
        AdjustmentTypeWithThreshold adjustmentTypeWithThreshold = new AdjustmentTypeWithThreshold(instanceGroupAdjustment.getAdjustmentType(),
                instanceGroupAdjustment.getThreshold());
        CloudPlatformVariant cloudPlatformVariant = stackService.getPlatformVariantByStackId(stackId);
        Acceptable stackAndClusterUpscaleTriggerEvent = new StackAndClusterUpscaleTriggerEvent(selector,
                stackId, Collections.singletonMap(instanceGroupAdjustment.getInstanceGroup(), instanceGroupAdjustment.getScalingAdjustment()),
                withClusterEvent ? ScalingType.UPSCALE_TOGETHER : ScalingType.UPSCALE_ONLY_STACK,
                getStackNetworkScaleDetails(instanceGroupAdjustment), adjustmentTypeWithThreshold, cloudPlatformVariant.getVariant().value());
        return reactorNotifier.notify(stackId, selector, stackAndClusterUpscaleTriggerEvent);
    }

    public FlowIdentifier triggerStackUpscale(Long stackId, InstanceGroupAdjustmentV4Request instanceGroupAdjustment, boolean withClusterEvent) {
        LOGGER.info("FlowManager trigger for upscale");
        String selector = FlowChainTriggers.FULL_UPSCALE_TRIGGER_EVENT;
        AdjustmentTypeWithThreshold adjustmentTypeWithThreshold = new AdjustmentTypeWithThreshold(instanceGroupAdjustment.getAdjustmentType(),
                instanceGroupAdjustment.getThreshold());
        CloudPlatformVariant cloudPlatformVariant = stackService.getPlatformVariantByStackId(stackId);
        Acceptable stackAndClusterUpscaleTriggerEvent = new StackAndClusterUpscaleTriggerEvent(selector,
                stackId, Collections.singletonMap(instanceGroupAdjustment.getInstanceGroup(), instanceGroupAdjustment.getScalingAdjustment()),
                withClusterEvent ? ScalingType.UPSCALE_TOGETHER : ScalingType.UPSCALE_ONLY_STACK,
                getStackNetworkScaleDetails(instanceGroupAdjustment), adjustmentTypeWithThreshold, cloudPlatformVariant.getVariant().value());
        LOGGER.info("Triggering stack upscale with {} adjustment, {} adjustment type, {} threshold",
                instanceGroupAdjustment.getScalingAdjustment(), adjustmentTypeWithThreshold.getAdjustmentType(), adjustmentTypeWithThreshold.getThreshold());
        return reactorNotifier.notify(stackId, selector, stackAndClusterUpscaleTriggerEvent);
    }

    public FlowIdentifier triggerStackDownscale(Long stackId, InstanceGroupAdjustmentV4Request instanceGroupAdjustment) {
        String selector = STACK_DOWNSCALE_EVENT.event();
        CloudPlatformVariant cloudPlatformVariant = stackService.getPlatformVariantByStackId(stackId);
        Acceptable stackScaleTriggerEvent = new StackDownscaleTriggerEvent(selector, stackId,
                Collections.singletonMap(instanceGroupAdjustment.getInstanceGroup(), instanceGroupAdjustment.getScalingAdjustment()),
                cloudPlatformVariant.getVariant().value());
        return reactorNotifier.notify(stackId, selector, stackScaleTriggerEvent);
    }

    public FlowIdentifier triggerStackSync(Long stackId) {
        String selector = STACK_SYNC_EVENT.event();
        return reactorNotifier.notify(stackId, selector, new StackSyncTriggerEvent(selector, stackId, true));
    }

    public void triggerStackRemoveInstance(Long stackId, String hostGroup, Long privateId) {
        triggerStackRemoveInstance(stackId, hostGroup, privateId, false);
    }

    public FlowIdentifier triggerStackRemoveInstance(Long stackId, String hostGroup, Long privateId, boolean forced) {
        String selector = FlowChainTriggers.FULL_DOWNSCALE_TRIGGER_EVENT;
        ClusterDownscaleDetails details = new ClusterDownscaleDetails(forced, false);
        ClusterAndStackDownscaleTriggerEvent event = new ClusterAndStackDownscaleTriggerEvent(selector, stackId, Collections.singletonMap(hostGroup, 1),
                Collections.singletonMap(hostGroup, Collections.singleton(privateId)), ScalingType.DOWNSCALE_TOGETHER,
                new Promise<>(), details);
        return reactorNotifier.notify(stackId, selector, event);
    }

    public FlowIdentifier triggerStopStartStackDownscale(Long stackId, Map<String, Set<Long>> instanceIdsByHostgroupMap, boolean forced) {
        // TODO CB-14929: stop-start is not meant for multiple hostGroups - set up a different API for this.
        LOGGER.debug("triggerStopStartStackDownscale with instanceIdsByHostgroupMap={}", instanceIdsByHostgroupMap);
        if (instanceIdsByHostgroupMap.size() != 1) {
            throw new RuntimeException("Expected instancesIdsToHostGroupMap to contain exactly 1 host group. Found" + instanceIdsByHostgroupMap.size());
        }

        Map.Entry<String, Set<Long>> entry = instanceIdsByHostgroupMap.entrySet().iterator().next();
        String hostGroup = entry.getKey();
        Set<Long> privateIds = entry.getValue();
        LOGGER.debug("ids to remove(stop). size:{}, ids:{}", privateIds.size(), privateIds);

        String selector = FlowChainTriggers.STOPSTART_DOWNSCALE_CHAIN_TRIGGER_EVENT;

        ClusterDownscaleDetails details = new ClusterDownscaleDetails(forced, false);
        ClusterAndStackDownscaleTriggerEvent event = new ClusterAndStackDownscaleTriggerEvent(selector, stackId,
                Collections.singletonMap(hostGroup, privateIds), ScalingType.DOWNSCALE_TOGETHER,  new Promise<>(), details);
        return reactorNotifier.notify(stackId, selector, event);
    }

    public FlowIdentifier triggerStackRemoveInstances(Long stackId, Map<String, Set<Long>> instanceIdsByHostgroupMap, boolean forced) {
        String selector = FlowChainTriggers.FULL_DOWNSCALE_MULTIHOSTGROUP_TRIGGER_EVENT;
        ClusterDownscaleDetails details = new ClusterDownscaleDetails(forced, false);
        MultiHostgroupClusterAndStackDownscaleTriggerEvent event = new MultiHostgroupClusterAndStackDownscaleTriggerEvent(selector, stackId,
                instanceIdsByHostgroupMap, details, ScalingType.DOWNSCALE_TOGETHER, new Promise<>());
        return reactorNotifier.notify(stackId, selector, event);
    }

    public void triggerTermination(Long stackId) {
        String selector = StackTerminationEvent.TERMINATION_EVENT.event();
        reactorNotifier.notify(stackId, selector, new TerminationEvent(selector, stackId, TerminationType.REGULAR));
        flowCancelService.cancelRunningFlows(stackId);
    }

    public FlowIdentifier triggerClusterInstall(Long stackId) {
        String selector = CLUSTER_CREATION_EVENT.event();
        return reactorNotifier.notify(stackId, selector, new StackEvent(selector, stackId));
    }

    public FlowIdentifier triggerRotateSaltPassword(Long stackId) {
        String selector = RotateSaltPasswordEvent.ROTATE_SALT_PASSWORD_EVENT.event();
        return reactorNotifier.notify(stackId, selector, new StackEvent(selector, stackId));
    }

    public FlowIdentifier triggerClusterReInstall(Long stackId) {
        String selector = FlowChainTriggers.CLUSTER_RESET_CHAIN_TRIGGER_EVENT;
        return reactorNotifier.notify(stackId, selector, new StackEvent(selector, stackId));
    }

    public FlowIdentifier triggerDatalakeClusterUpgrade(Long stackId, String imageId) {
        String selector = FlowChainTriggers.DATALAKE_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT;
        return reactorNotifier.notify(stackId, selector, new ClusterUpgradeTriggerEvent(selector, stackId, imageId));
    }

    public FlowIdentifier triggerDistroXUpgrade(Long stackId, ImageChangeDto imageChangeDto, boolean replaceVms, boolean lockComponents, String variant) {
        String selector = FlowChainTriggers.DISTROX_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT;
        return reactorNotifier.notify(stackId, selector, new DistroXUpgradeTriggerEvent(selector, stackId, imageChangeDto, replaceVms, lockComponents,
                variant));
    }

    public FlowIdentifier triggerDatalakeClusterRecovery(Long stackId) {
        String selector = FlowChainTriggers.CLUSTER_RECOVERY_CHAIN_TRIGGER_EVENT;
        return reactorNotifier.notify(stackId, selector, new ClusterRecoveryTriggerEvent(selector, stackId));
    }

    public FlowIdentifier triggerClusterCredentialReplace(Long stackId, String userName, String password) {
        String selector = CLUSTER_CREDENTIALCHANGE_EVENT.event();
        ClusterCredentialChangeTriggerEvent event = new ClusterCredentialChangeTriggerEvent(selector, stackId, userName, password, Type.REPLACE);
        return reactorNotifier.notify(stackId, selector, event);
    }

    public FlowIdentifier triggerClusterCredentialUpdate(Long stackId, String password) {
        String selector = CLUSTER_CREDENTIALCHANGE_EVENT.event();
        ClusterCredentialChangeTriggerEvent event = new ClusterCredentialChangeTriggerEvent(selector, stackId, null, password, Type.UPDATE);
        return reactorNotifier.notify(stackId, selector, event);
    }

    public FlowIdentifier triggerClusterUpscale(Long stackId, HostGroupAdjustmentV4Request hostGroupAdjustment) {
        String selector = CLUSTER_UPSCALE_TRIGGER_EVENT.event();
        Acceptable event = new ClusterScaleTriggerEvent(selector, stackId,
                Collections.singletonMap(hostGroupAdjustment.getHostGroup(), hostGroupAdjustment.getScalingAdjustment()), null, null);
        return reactorNotifier.notify(stackId, selector, event);
    }

    public FlowIdentifier triggerClusterDownscale(Long stackId, HostGroupAdjustmentV4Request hostGroupAdjustment) {
        String selector = FlowChainTriggers.FULL_DOWNSCALE_TRIGGER_EVENT;
        ScalingType scalingType = hostGroupAdjustment.getWithStackUpdate() ? ScalingType.DOWNSCALE_TOGETHER : ScalingType.DOWNSCALE_ONLY_CLUSTER;
        Acceptable event = new ClusterAndStackDownscaleTriggerEvent(selector, stackId, Collections.singletonMap(hostGroupAdjustment.getHostGroup(),
                hostGroupAdjustment.getScalingAdjustment()), scalingType);
        return reactorNotifier.notify(stackId, selector, event);
    }

    public FlowIdentifier triggerClusterStart(Long stackId) {
        String selector = CLUSTER_START_EVENT.event();
        return reactorNotifier.notify(stackId, selector, new StackEvent(selector, stackId));
    }

    public FlowIdentifier triggerClusterServicesRestart(Long stackId) {
        String selector = CLUSTER_SERVICES_RESTART_TRIGGER_EVENT.event();
        return reactorNotifier.notify(stackId, selector, new StackEvent(selector, stackId));
    }

    public FlowIdentifier triggerClusterStop(Long stackId) {
        String selector = FlowChainTriggers.FULL_STOP_TRIGGER_EVENT;
        return reactorNotifier.notify(stackId, selector, new StackEvent(selector, stackId));
    }

    public FlowIdentifier triggerClusterSync(Long stackId) {
        String selector = CLUSTER_SYNC_EVENT.event();
        return reactorNotifier.notify(stackId, selector, new StackEvent(selector, stackId));
    }

    public void triggerClusterSyncWithoutCheck(Long stackId) {
        String selector = CLUSTER_SYNC_EVENT.event();
        reactorNotifier.notifyWithoutCheck(stackId, selector, new StackEvent(selector, stackId));
    }

    public FlowIdentifier triggerFullSync(Long stackId) {
        String selector = FlowChainTriggers.FULL_SYNC_TRIGGER_EVENT;
        return reactorNotifier.notify(stackId, selector, new StackEvent(selector, stackId));
    }

    public FlowIdentifier triggerSyncComponentVersionsFromCm(Long stackId, Set<String> candidateImageUuids) {
        String selector = CM_SYNC_TRIGGER_EVENT.event();
        return reactorNotifier.notify(stackId, selector, new CmSyncTriggerEvent(stackId, candidateImageUuids));
    }

    public void triggerFullSyncWithoutCheck(Long stackId) {
        String selector = FlowChainTriggers.FULL_SYNC_TRIGGER_EVENT;
        reactorNotifier.notifyWithoutCheck(stackId, selector, new StackEvent(selector, stackId));
    }

    public FlowIdentifier triggerManualRepairFlow(Long stackId) {
        String selector = MANUAL_STACK_REPAIR_TRIGGER_EVENT.event();
        return reactorNotifier.notify(stackId, selector, new StackEvent(selector, stackId));
    }

    public void triggerStackRepairFlow(Long stackId, UnhealthyInstances unhealthyInstances) {
        String selector = FlowChainTriggers.STACK_REPAIR_TRIGGER_EVENT;
        reactorNotifier.notify(stackId, selector, new StackRepairTriggerEvent(stackId, unhealthyInstances));
    }

    public FlowIdentifier triggerClusterRepairFlow(Long stackId, Map<String, List<String>> failedNodesMap, boolean oneNodeFromEachHostGroupAtOnce,
            boolean restartServices) {
        return reactorNotifier.notify(stackId, FlowChainTriggers.CLUSTER_REPAIR_TRIGGER_EVENT,
                new ClusterRepairTriggerEvent(stackId, failedNodesMap, oneNodeFromEachHostGroupAtOnce, restartServices));
    }

    public FlowIdentifier triggerClusterRepairFlow(Long stackId, Map<String, List<String>> failedNodesMap,
            boolean restartServices) {
        return reactorNotifier.notify(stackId, FlowChainTriggers.CLUSTER_REPAIR_TRIGGER_EVENT,
                new ClusterRepairTriggerEvent(stackId, failedNodesMap, restartServices));
    }

    public FlowIdentifier triggerStackImageUpdate(ImageChangeDto imageChangeDto) {
        String selector = FlowChainTriggers.STACK_IMAGE_UPDATE_TRIGGER_EVENT;
        return reactorNotifier.notify(imageChangeDto.getStackId(), selector, new StackImageUpdateTriggerEvent(selector, imageChangeDto));
    }

    public FlowIdentifier triggerMaintenanceModeValidationFlow(Long stackId) {
        String selector = FlowChainTriggers.CLUSTER_MAINTENANCE_MODE_VALIDATION_TRIGGER_EVENT;
        return reactorNotifier.notify(stackId, selector, new MaintenanceModeValidationTriggerEvent(selector, stackId));
    }

    public FlowIdentifier triggerClusterCertificationRenewal(Long stackId) {
        String selector = CLUSTER_CERTIFICATE_REISSUE_EVENT.event();
        return reactorNotifier.notify(stackId, selector, new StackEvent(selector, stackId));
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

    public FlowIdentifier triggerSaltUpdate(Long stackId) {
        String selector = SALT_UPDATE_EVENT.event();
        return reactorNotifier.notify(stackId, selector, new StackEvent(selector, stackId));
    }

    public FlowIdentifier triggerPillarConfigurationUpdate(Long stackId) {
        String selector = PILLAR_CONFIG_UPDATE_EVENT.event();
        return reactorNotifier.notify(stackId, selector, new StackEvent(selector, stackId));
    }

    public FlowIdentifier triggerDatalakeDatabaseBackup(Long stackId, String location, String backupId, boolean closeConnections) {
        String selector = FlowChainTriggers.DATALAKE_DATABASE_BACKUP_CHAIN_TRIGGER_EVENT;
        return reactorNotifier.notify(stackId, selector, new DatabaseBackupTriggerEvent(selector, stackId, location, backupId, closeConnections));
    }

    public FlowIdentifier triggerDatalakeDatabaseRestore(Long stackId, String location, String backupId) {
        String selector = DATABASE_RESTORE_EVENT.event();
        return reactorNotifier.notify(stackId, selector, new DatabaseRestoreTriggerEvent(selector, stackId, location, backupId));
    }

    public FlowIdentifier triggerAutoTlsCertificatesRotation(Long stackId, CertificatesRotationV4Request certificatesRotationV4Request) {
        String selector = FlowChainTriggers.ROTATE_CLUSTER_CERTIFICATES_CHAIN_TRIGGER_EVENT;
        ClusterCertificatesRotationTriggerEvent clusterCertificatesRotationTriggerEvent = new ClusterCertificatesRotationTriggerEvent(selector, stackId,
                certificatesRotationV4Request.getRotateCertificatesType());
        return reactorNotifier.notify(stackId, selector, clusterCertificatesRotationTriggerEvent);
    }

    public FlowIdentifier triggerStackLoadBalancerUpdate(Long stackId) {
        String selector = STACK_LOAD_BALANCER_UPDATE_EVENT.event();
        StackLoadBalancerUpdateTriggerEvent stackLoadBalancerUpdateTriggerEvent = new StackLoadBalancerUpdateTriggerEvent(selector, stackId);
        return reactorNotifier.notify(stackId, selector, stackLoadBalancerUpdateTriggerEvent);
    }

    public FlowIdentifier triggerClusterProxyConfigReRegistration(Long stackId) {
        String selector = ClusterProxyReRegistrationEvent.CLUSTER_PROXY_RE_REGISTRATION_EVENT.event();
        return reactorNotifier.notify(stackId, selector, new StackEvent(selector, stackId));
    }

    private NetworkScaleDetails getStackNetworkScaleDetails(InstanceGroupAdjustmentV4Request instanceGroupAdjustment) {
        List<String> preferredSubnetIds = new ArrayList<>();
        if (instanceGroupAdjustment.getNetworkScaleRequest() != null) {
            List<String> preferredSubnetIdsFromRequest = instanceGroupAdjustment.getNetworkScaleRequest().getPreferredSubnetIds();
            if (CollectionUtils.isNotEmpty(preferredSubnetIdsFromRequest)) {
                preferredSubnetIds.addAll(preferredSubnetIdsFromRequest);
            }
        }
        return new NetworkScaleDetails(preferredSubnetIds);
    }
}
