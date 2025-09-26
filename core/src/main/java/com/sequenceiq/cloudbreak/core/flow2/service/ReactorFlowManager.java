package com.sequenceiq.cloudbreak.core.flow2.service;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RotateRdsCertificateType.MIGRATE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RotateRdsCertificateType.ROTATE;
import static com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers.MIGRATE_ZOOKEEPER_TO_KRAFT_CHAIN_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.ADD_VOLUMES_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrenew.ClusterCertificateRenewEvent.CLUSTER_CERTIFICATE_REISSUE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.cmsync.CmSyncEvent.CM_SYNC_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.PillarConfigurationUpdateEvent.PILLAR_CONFIG_UPDATE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.restore.DatabaseRestoreEvent.DATABASE_RESTORE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.metrics.datasizes.DetermineDatalakeDataSizesEvent.DETERMINE_DATALAKE_DATA_SIZES_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesEvent.DELETE_VOLUMES_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event.CoreModifySeLinuxStateSelectors.CORE_MODIFY_SELINUX_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLUSTER_CREATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.RotateRdsCertificateEvent.ROTATE_RDS_CERTIFICATE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent.SALT_UPDATE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.services.restart.ClusterServicesRestartEvent.CLUSTER_SERVICES_RESTART_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFlowEvent.SKU_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent.CLUSTER_START_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.sync.ClusterSyncEvent.CLUSTER_SYNC_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.CLUSTER_UPSCALE_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd.ClusterCredentialChangeEvent.CLUSTER_CREDENTIALCHANGE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.CoreVerticalScaleEvent.STACK_VERTICALSCALE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.STACK_DOWNSCALE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.event.StackInstanceMetadataUpdateEvent.STACK_IMDUPDATE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.repair.ManualStackRepairTriggerEvent.MANUAL_STACK_REPAIR_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopEvent.STACK_STOP_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncEvent.STACK_SYNC_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer.StackLoadBalancerUpdateEvent.STACK_LOAD_BALANCER_UPDATE_EVENT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.CertificatesRotationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackAddVolumesRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackDeleteVolumesRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.osupgrade.OrderedOSUpgradeSet;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateType;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesRequest;
import com.sequenceiq.cloudbreak.core.flow2.cluster.java.SetDefaultJavaVersionTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event.CoreModifySeLinuxEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.restart.RestartInstancesWithRdsStartEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.services.restart.event.ClusterServicesRestartTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateStateSelectors;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.dto.NetworkScaleDetails;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterAndStackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterCertificatesRotationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterCredentialChangeTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterCredentialChangeTriggerEvent.Type;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleDetails;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterRecoveryTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterScaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterUpgradeTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.CoreVerticalScalingTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.DatabaseBackupTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.DatabaseRestoreTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.DeleteVolumesTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.DistroXUpgradeTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.MaintenanceModeValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.MultiHostgroupClusterAndStackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.RdsUpgradeChainTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.RefreshEntitlementParamsFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackAndClusterUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackImageUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackLoadBalancerUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackSyncTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.UpgradePreparationChainTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.user.ExternalDatabaseUserFlowStartEvent;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.user.ExternalDatabaseUserOperation;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.user.config.ExternalDatabaseUserEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.clusterproxy.reregister.ClusterProxyReRegistrationEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.event.StackInstanceMetadataUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.event.CoreRootVolumeUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.clusterproxy.ClusterProxyReRegistrationTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.datalakemetrics.datasizes.DetermineDatalakeDataSizesBaseEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.modifyproxy.ModifyProxyConfigFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateTriggerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent.RepairType;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.CmSyncTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.OSUpgradeByUpgradeSetsTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StackRepairTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationType;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.flow.chain.SecretRotationFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.repair.UnhealthyInstances;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.service.FlowCancelService;

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

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

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
        ClusterDownscaleDetails details = new ClusterDownscaleDetails(forced, false, false);
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

        ClusterDownscaleDetails details = new ClusterDownscaleDetails(forced, false, false);
        ClusterAndStackDownscaleTriggerEvent event = new ClusterAndStackDownscaleTriggerEvent(selector, stackId,
                Collections.singletonMap(hostGroup, privateIds), ScalingType.DOWNSCALE_TOGETHER, new Promise<>(), details);
        return reactorNotifier.notify(stackId, selector, event);
    }

    public FlowIdentifier triggerRestartInstances(Long stackId, List<String> instanceIds, boolean rdsRestartRequired) {
        RestartInstancesWithRdsStartEvent restartInstancesWithRdsStartEvent = new RestartInstancesWithRdsStartEvent(stackId, instanceIds, rdsRestartRequired);
        String selector = FlowChainTriggers.RESTART_INSTANCES_WITH_RDS_RESTART_CHAIN_TRIGGER_EVENT;
        return reactorNotifier.notify(stackId, selector, restartInstancesWithRdsStartEvent);
    }

    public FlowIdentifier triggerStackRemoveInstances(Long stackId, Map<String, Set<Long>> instanceIdsByHostgroupMap, boolean forced) {
        String selector = FlowChainTriggers.FULL_DOWNSCALE_MULTIHOSTGROUP_TRIGGER_EVENT;
        ClusterDownscaleDetails details = new ClusterDownscaleDetails(forced, false, false);
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

    public FlowIdentifier triggerModifyProxyConfig(Long stackId, String previousProxyConfigCrn) {
        String selector = FlowChainTriggers.MODIFY_PROXY_CONFIG_CHAIN_TRIGGER_EVENT;
        return reactorNotifier.notify(stackId, selector, new ModifyProxyConfigFlowChainTriggerEvent(stackId, previousProxyConfigCrn));
    }

    public FlowIdentifier triggerClusterReInstall(Long stackId) {
        String selector = FlowChainTriggers.CLUSTER_RESET_CHAIN_TRIGGER_EVENT;
        return reactorNotifier.notify(stackId, selector, new StackEvent(selector, stackId));
    }

    public FlowIdentifier triggerDatalakeClusterUpgrade(Long stackId, String imageId, boolean rollingUpgradeEnabled) {
        String selector = FlowChainTriggers.DATALAKE_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT;
        return reactorNotifier.notify(stackId, selector, new ClusterUpgradeTriggerEvent(selector, stackId, imageId, rollingUpgradeEnabled));
    }

    public FlowIdentifier triggerDistroXUpgrade(Long stackId, ImageChangeDto imageChangeDto, boolean replaceVms, boolean lockComponents, String variant,
            boolean rollingUpgradeEnabled, String runtime) {
        String selector = FlowChainTriggers.DISTROX_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT;
        return reactorNotifier.notify(stackId, selector, new DistroXUpgradeTriggerEvent(selector, stackId, new Promise<>(), imageChangeDto, replaceVms,
                lockComponents, variant, rollingUpgradeEnabled, runtime));
    }

    public FlowIdentifier triggerClusterUpgradePreparation(Long stackId, ImageChangeDto imageChangeDto, String runtimeVersion) {
        String selector = FlowChainTriggers.CLUSTER_UPGRADE_PREPARATION_CHAIN_TRIGGER_EVENT;
        return reactorNotifier.notify(stackId, selector, new UpgradePreparationChainTriggerEvent(selector, stackId, imageChangeDto, runtimeVersion));
    }

    public FlowIdentifier triggerRdsUpgrade(Long stackId, TargetMajorVersion targetVersion, String backupLocation, String backupInstanceProfile) {
        String selector = FlowChainTriggers.UPGRADE_RDS_CHAIN_TRIGGER_EVENT;
        return reactorNotifier.notify(stackId, selector,
                new RdsUpgradeChainTriggerEvent(selector, stackId, targetVersion, backupLocation, backupInstanceProfile));
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

    public FlowIdentifier triggerVerticalScale(Long stackId, StackVerticalScaleV4Request request) {
        String selector = STACK_VERTICALSCALE_EVENT.event();
        CoreVerticalScalingTriggerEvent event = new CoreVerticalScalingTriggerEvent(selector, stackId, request);
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

    public FlowIdentifier triggerClusterServicesRestart(Long stackId, boolean refreshNeeded, boolean rollingRestart, boolean restartStaleServices) {
        String selector = CLUSTER_SERVICES_RESTART_TRIGGER_EVENT.event();
        return reactorNotifier.notify(stackId, selector, new ClusterServicesRestartTriggerEvent(selector, stackId, refreshNeeded, rollingRestart,
                restartStaleServices, false));
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

    public FlowIdentifier triggerOsUpgradeByUpgradeSetsFlow(Long stackId, String triggeredVariant, ImageChangeDto imageChangeDto,
            List<OrderedOSUpgradeSet> upgradeSets) {
        return reactorNotifier.notify(stackId, FlowChainTriggers.OS_UPGRADE_BY_UPGRADE_SETS_TRIGGER_EVENT,
                new OSUpgradeByUpgradeSetsTriggerEvent(stackId, triggeredVariant, imageChangeDto, upgradeSets));
    }

    public void triggerStackRepairFlow(Long stackId, UnhealthyInstances unhealthyInstances) {
        String selector = FlowChainTriggers.STACK_REPAIR_TRIGGER_EVENT;
        reactorNotifier.notify(stackId, selector, new StackRepairTriggerEvent(stackId, unhealthyInstances));
    }

    public FlowIdentifier triggerClusterRepairFlow(Long stackId, Map<String, List<String>> failedNodesMap, RepairType repairType,
            boolean restartServices, String triggeredVariant, boolean upgrade) {
        return reactorNotifier.notify(stackId, FlowChainTriggers.CLUSTER_REPAIR_TRIGGER_EVENT,
                new ClusterRepairTriggerEvent(stackId, failedNodesMap, repairType, restartServices, triggeredVariant, upgrade, false));
    }

    public FlowIdentifier triggerClusterRepairFlow(Long stackId, Map<String, List<String>> failedNodesMap,
            boolean restartServices) {
        return reactorNotifier.notify(stackId, FlowChainTriggers.CLUSTER_REPAIR_TRIGGER_EVENT,
                new ClusterRepairTriggerEvent(stackId, failedNodesMap, RepairType.ALL_AT_ONCE, restartServices, null));
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

    public FlowIdentifier triggerDatalakeDatabaseBackup(Long stackId, String location, String backupId,
            boolean closeConnections, List<String> skipDatabaseNames, int databaseMaxDurationInMin, boolean dryRun) {
        String selector = FlowChainTriggers.DATALAKE_DATABASE_BACKUP_CHAIN_TRIGGER_EVENT;
        return reactorNotifier.notify(stackId, selector, new DatabaseBackupTriggerEvent(selector, stackId,
                location, backupId, closeConnections, skipDatabaseNames, databaseMaxDurationInMin, dryRun));
    }

    public FlowIdentifier triggerDatalakeDatabaseRestore(Long stackId, String location, String backupId, int databaseMaxDurationInMin, boolean dryRun) {
        String selector = DATABASE_RESTORE_EVENT.event();
        DatabaseRestoreTriggerEvent databaseRestoreTriggerEvent =
            new DatabaseRestoreTriggerEvent(selector, stackId, location, backupId, databaseMaxDurationInMin, dryRun);
        return reactorNotifier.notify(stackId, selector, databaseRestoreTriggerEvent);
    }

    public FlowIdentifier triggerAutoTlsCertificatesRotation(Long stackId, CertificatesRotationV4Request certificatesRotationV4Request) {
        String selector = FlowChainTriggers.ROTATE_CLUSTER_CERTIFICATES_CHAIN_TRIGGER_EVENT;
        ClusterCertificatesRotationTriggerEvent clusterCertificatesRotationTriggerEvent = new ClusterCertificatesRotationTriggerEvent(selector, stackId,
                certificatesRotationV4Request.getCertificateRotationType(), certificatesRotationV4Request.getSkipSaltUpdate());
        return reactorNotifier.notify(stackId, selector, clusterCertificatesRotationTriggerEvent);
    }

    public FlowIdentifier triggerStackLoadBalancerUpdate(Long stackId) {
        String selector = STACK_LOAD_BALANCER_UPDATE_EVENT.event();
        StackLoadBalancerUpdateTriggerEvent stackLoadBalancerUpdateTriggerEvent = new StackLoadBalancerUpdateTriggerEvent(selector, stackId);
        return reactorNotifier.notify(stackId, selector, stackLoadBalancerUpdateTriggerEvent);
    }

    public FlowIdentifier triggerClusterProxyConfigReRegistration(Long stackId, boolean skipFullReRegistration, String originalCrn) {
        String selector;
        if (stackService.getById(stackId).getTunnel().useCcmV1()) {
            selector = ClusterProxyReRegistrationEvent.CLUSTER_PROXY_CCMV1_REMAP_EVENT.event();
        } else {
            selector = ClusterProxyReRegistrationEvent.CLUSTER_PROXY_RE_REGISTRATION_EVENT.event();
        }
        return reactorNotifier.notify(stackId, selector, new ClusterProxyReRegistrationTriggerEvent(selector, stackId, skipFullReRegistration, originalCrn));
    }

    public void triggerDetermineDatalakeDataSizes(Long stackId, String operationId) {
        String selector = DETERMINE_DATALAKE_DATA_SIZES_EVENT.event();
        reactorNotifier.notify(stackId, selector, new DetermineDatalakeDataSizesBaseEvent(selector, stackId, operationId));
    }

    public FlowIdentifier triggerSecretRotation(Long stackId, String crn, List<SecretType> secretTypes, RotationFlowExecutionType executionType,
            Map<String, String> additionalProperties) {
        String selector = EventSelectorUtil.selector(SecretRotationFlowChainTriggerEvent.class);
        Acceptable triggerEvent = new SecretRotationFlowChainTriggerEvent(selector, stackId, crn, secretTypes, executionType, additionalProperties);
        return reactorNotifier.notify(stackId, selector, triggerEvent);
    }

    private NetworkScaleDetails getStackNetworkScaleDetails(InstanceGroupAdjustmentV4Request instanceGroupAdjustment) {
        List<String> preferredSubnetIds = new ArrayList<>();
        Set<String> preferredAvailabilityZones = new HashSet<>();
        if (instanceGroupAdjustment.getNetworkScaleRequest() != null) {
            List<String> preferredSubnetIdsFromRequest = instanceGroupAdjustment.getNetworkScaleRequest().getPreferredSubnetIds();
            if (CollectionUtils.isNotEmpty(preferredSubnetIdsFromRequest)) {
                preferredSubnetIds.addAll(preferredSubnetIdsFromRequest);
            }

            Set<String> preferredAvailabilityZonesFromRequest = instanceGroupAdjustment.getNetworkScaleRequest().getPreferredAvailabilityZones();
            if (CollectionUtils.isNotEmpty(preferredAvailabilityZonesFromRequest)) {
                preferredAvailabilityZones.addAll(preferredAvailabilityZonesFromRequest);
            }
        }
        return new NetworkScaleDetails(preferredSubnetIds, preferredAvailabilityZones);
    }

    public FlowIdentifier triggerDeleteVolumes(Long stackId, StackDeleteVolumesRequest deleteRequest) {
        String selector = DELETE_VOLUMES_VALIDATION_EVENT.event();
        DeleteVolumesTriggerEvent event = new DeleteVolumesTriggerEvent(selector, stackId, deleteRequest);
        return reactorNotifier.notify(stackId, selector, event);
    }

    public FlowIdentifier triggerStackUpdateDisks(StackDto stack, DiskUpdateRequest updateRequest) {
        MDCBuilder.buildMdcContext(stack);
        Long stackId = stack.getId();
        String selector = DistroXDiskUpdateStateSelectors.DATAHUB_DISK_UPDATE_VALIDATION_EVENT.selector();
        updateRequest.setGroup(updateRequest.getGroup().toLowerCase(Locale.ROOT));
        LOGGER.info("Datahub Vertical Scale flow triggered for datahub {}", stack.getName());
        DistroXDiskUpdateEvent datahubDiskUpdateTriggerEvent = DistroXDiskUpdateEvent.builder()
                .withResourceId(stackId)
                .withStackId(stackId)
                .withGroup(updateRequest.getGroup())
                .withVolumeType(updateRequest.getVolumeType())
                .withSize(updateRequest.getSize())
                .withDiskType(updateRequest.getDiskType().name())
                .withClusterName(stack.getCluster().getResourceName())
                .withAccountId(stack.getAccountId())
                .withSelector(selector)
                .build();
        LOGGER.debug("Disk Update flow trigger event sent for datahub {}", stack.getName());
        return reactorNotifier.notify(stackId, selector, datahubDiskUpdateTriggerEvent);
    }

    public FlowIdentifier triggerSkuMigration(Long stackId, boolean force) {
        SkuMigrationTriggerEvent event = new SkuMigrationTriggerEvent(SKU_MIGRATION_EVENT.event(), stackId, force);
        return reactorNotifier.notify(stackId, SKU_MIGRATION_EVENT.event(), event);
    }

    public FlowIdentifier triggerRefreshEntitlementParams(Long stackId, String crn, Map<String, Boolean> changedEntitlements, Boolean saltRefreshNeeded) {
        String selector = FlowChainTriggers.REFRESH_ENTITLEMENT_PARAM_CHAIN_TRIGGER_EVENT;
        flowMessageService.fireEventAndLog(stackId, "DYNAMIC_ENTITLEMENT", ResourceEvent.STACK_DYNAMIC_ENTITLEMENT_STARTED);
        Acceptable triggerEvent = new RefreshEntitlementParamsFlowChainTriggerEvent(selector, stackId, crn, changedEntitlements, saltRefreshNeeded);
        return reactorNotifier.notify(stackId, selector, triggerEvent);
    }

    public FlowIdentifier triggerAddVolumes(Long stackId, StackAddVolumesRequest addVolumesRequest) {
        String selector = ADD_VOLUMES_TRIGGER_EVENT.event();
        AddVolumesRequest event = new AddVolumesRequest(selector, stackId, addVolumesRequest.getNumberOfDisks(), addVolumesRequest.getType(),
                addVolumesRequest.getSize(), CloudVolumeUsageType.valueOf(addVolumesRequest.getCloudVolumeUsageType()), addVolumesRequest.getInstanceGroup());
        return reactorNotifier.notify(stackId, selector, event);
    }

    public FlowIdentifier triggerInstanceMetadataUpdate(StackDto stackDto, InstanceMetadataUpdateType updateType) {
        StackInstanceMetadataUpdateTriggerEvent triggerEvent =
                new StackInstanceMetadataUpdateTriggerEvent(STACK_IMDUPDATE_EVENT.event(), stackDto.getId(), updateType);
        return reactorNotifier.notify(stackDto.getId(), STACK_IMDUPDATE_EVENT.event(), triggerEvent);
    }

    public FlowIdentifier triggerRotateRdsCertificate(Long stackId) {
        String selector = ROTATE_RDS_CERTIFICATE_EVENT.event();
        return reactorNotifier.notify(stackId, selector, new RotateRdsCertificateTriggerRequest(selector, stackId, ROTATE));
    }

    public FlowIdentifier triggerMigrateRdsToTls(Long stackId) {
        String selector = ROTATE_RDS_CERTIFICATE_EVENT.event();
        return reactorNotifier.notify(stackId, selector, new RotateRdsCertificateTriggerRequest(selector, stackId, MIGRATE));
    }

    public FlowIdentifier triggerRootVolumeUpdateFlow(Long stackId, Map<String, List<String>> updatedNodesMap, DiskUpdateRequest updateRequest) {
        return reactorNotifier.notify(stackId, FlowChainTriggers.CORE_ROOT_VOLUME_UPDATE_TRIGGER_EVENT,
                new CoreRootVolumeUpdateTriggerEvent(
                        FlowChainTriggers.CORE_ROOT_VOLUME_UPDATE_TRIGGER_EVENT,
                        stackId,
                        updatedNodesMap,
                        updateRequest.getVolumeType(),
                        updateRequest.getSize(),
                        updateRequest.getGroup(),
                        updateRequest.getDiskType().name()
                )
        );
    }

    public FlowIdentifier triggerSetDefaultJavaVersion(Long stackId, String javaVersion, boolean restartServices, boolean restartCM, boolean rollingRestart) {
        String selector = FlowChainTriggers.SET_DEFAULT_JAVA_VERSION_CHAIN_TRIGGER_EVENT;
        return reactorNotifier.notify(stackId, selector,
                new SetDefaultJavaVersionTriggerEvent(selector, stackId, javaVersion, restartServices, restartCM, rollingRestart));
    }

    public FlowIdentifier triggerModifySelinux(Long stackId, SeLinux selinuxMode) {
        String selector = CORE_MODIFY_SELINUX_EVENT.event();
        return reactorNotifier.notify(stackId, selector, new CoreModifySeLinuxEvent(selector, stackId, selinuxMode));
    }

    public FlowIdentifier triggerExternalDatabaseUserOperation(Long stackId, String name, String crn, ExternalDatabaseUserOperation operation,
            DatabaseType dbType, String dbUser) {
        String selector = ExternalDatabaseUserEvent.START_EXTERNAL_DATABASE_USER_OPERATION_EVENT.event();
        return reactorNotifier.notify(stackId, selector, new ExternalDatabaseUserFlowStartEvent(stackId, selector, name, crn, operation, dbType, dbUser));
    }

    public FlowIdentifier triggerZookeeperToKraftMigration(Long stackId) {
        String selector = MIGRATE_ZOOKEEPER_TO_KRAFT_CHAIN_TRIGGER_EVENT;
        return reactorNotifier.notify(stackId, selector, new MigrateZookeeperToKraftFlowChainTriggerEvent(selector, stackId));
    }
}
