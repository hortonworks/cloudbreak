package com.sequenceiq.cloudbreak.job;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DECOMMISSION_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_RUNNING;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_UNHEALTHY;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.STOPPED;
import static com.sequenceiq.cloudbreak.cloud.model.HostName.hostName;
import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.UNKNOWN;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_FAILED_NODES_SALT_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;
import static java.util.stream.Collectors.toSet;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.status.ExtendedHostStatuses;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.type.HealthCheck;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.metrics.MetricsClient;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.SaltSyncService;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.cloudbreak.quartz.statuschecker.service.StatusCheckerJobService;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterOperationService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.RuntimeVersionService;
import com.sequenceiq.cloudbreak.service.stack.ServiceStatusCheckerLogLocationDecorator;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackInstanceStatusChecker;
import com.sequenceiq.cloudbreak.service.stack.flow.InstanceSyncState;
import com.sequenceiq.cloudbreak.service.stack.flow.StackSyncService;
import com.sequenceiq.cloudbreak.service.stack.flow.SyncConfig;
import com.sequenceiq.cloudbreak.util.Benchmark;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.domain.FlowLog;

@DisallowConcurrentExecution
@Component
public class StackStatusCheckerJob extends StatusCheckerJob {

    public static final EnumSet<Status> SYNCABLE_STATES = EnumSet.of(
            Status.AVAILABLE,
            Status.UPDATE_FAILED,
            Status.ENABLE_SECURITY_FAILED,
            Status.START_FAILED,
            Status.STOPPED,
            Status.STOP_FAILED,
            Status.AMBIGUOUS,
            Status.UNREACHABLE,
            Status.NODE_FAILURE,
            Status.RESTORE_FAILED,
            Status.BACKUP_FAILED,
            Status.BACKUP_FINISHED,
            Status.RESTORE_FINISHED,
            Status.DELETED_ON_PROVIDER_SIDE,
            Status.EXTERNAL_DATABASE_START_FAILED,
            Status.EXTERNAL_DATABASE_START_IN_PROGRESS,
            Status.EXTERNAL_DATABASE_START_FINISHED,
            Status.EXTERNAL_DATABASE_STOP_FAILED,
            Status.EXTERNAL_DATABASE_STOP_IN_PROGRESS,
            Status.EXTERNAL_DATABASE_STOP_FINISHED,
            Status.EXTERNAL_DATABASE_UPGRADE_FINISHED,
            Status.EXTERNAL_DATABASE_UPGRADE_FAILED,
            Status.RECOVERY_FAILED,
            Status.UPGRADE_CCM_FAILED,
            Status.UPGRADE_CCM_FINISHED,
            Status.UPGRADE_CCM_IN_PROGRESS,
            Status.DATAHUB_DISK_UPDATE_VALIDATION_FAILED,
            Status.DATAHUB_DISK_UPDATE_FAILED,
            Status.DATAHUB_DISK_UPDATE_RESIZE_FAILED,
            Status.LOAD_BALANCER_UPDATE_FINISHED,
            Status.LOAD_BALANCER_UPDATE_FAILED
    );

    public static final EnumSet<Status> IGNORED_STATES = EnumSet.of(
            Status.REQUESTED,
            Status.CREATE_IN_PROGRESS,
            Status.UPDATE_IN_PROGRESS,
            Status.UPDATE_REQUESTED,
            Status.STOP_REQUESTED,
            Status.START_REQUESTED,
            Status.STOP_IN_PROGRESS,
            Status.START_IN_PROGRESS,
            Status.WAIT_FOR_SYNC,
            Status.MAINTENANCE_MODE_ENABLED,
            Status.EXTERNAL_DATABASE_CREATION_IN_PROGRESS,
            Status.EXTERNAL_DATABASE_UPGRADE_IN_PROGRESS,
            Status.BACKUP_IN_PROGRESS,
            Status.RESTORE_IN_PROGRESS,
            Status.LOAD_BALANCER_UPDATE_IN_PROGRESS,
            Status.RECOVERY_IN_PROGRESS,
            Status.RECOVERY_REQUESTED,
            Status.DETERMINE_DATALAKE_DATA_SIZES_IN_PROGRESS,
            Status.DATAHUB_DISK_UPDATE_VALIDATION_IN_PROGRESS
    );

    public static final EnumSet<Status> LONG_SYNCABLE_STATES = EnumSet.of(Status.DELETED_ON_PROVIDER_SIDE);

    public static final EnumSet<Status> STATES_FROM_AVAILABLE_ALLOWED = EnumSet.of(
            Status.AMBIGUOUS,
            Status.NODE_FAILURE,
            Status.STOPPED,
            Status.START_FAILED,
            Status.STOP_FAILED,
            Status.UPDATE_FAILED,
            Status.ENABLE_SECURITY_FAILED,
            Status.DATAHUB_DISK_UPDATE_VALIDATION_FAILED,
            Status.DATAHUB_DISK_UPDATE_FAILED,
            Status.DATAHUB_DISK_UPDATE_RESIZE_FAILED);

    private static final EnumSet<InstanceStatus> STATES_FROM_HEALTHY_ALLOWED = EnumSet.of(
            SERVICES_UNHEALTHY,
            SERVICES_RUNNING,
            DECOMMISSION_FAILED,
            FAILED);

    private static final Logger LOGGER = LoggerFactory.getLogger(StackStatusCheckerJob.class);

    @Inject
    private StatusCheckerJobService jobService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private ClusterOperationService clusterOperationService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private StackInstanceStatusChecker stackInstanceStatusChecker;

    @Inject
    private StackSyncService syncService;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private InstanceMetaDataToCloudInstanceConverter cloudInstanceConverter;

    @Inject
    private RuntimeVersionService runtimeVersionService;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private MetricsClient metricsClient;

    @Inject
    private ServiceStatusCheckerLogLocationDecorator serviceStatusCheckerLogLocationDecorator;

    @Inject
    private Clock clock;

    @Inject
    private StackStatusCheckerConfig config;

    @Inject
    private SaltSyncService saltSyncService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Override
    protected Optional<MdcContextInfoProvider> getMdcContextConfigProvider() {
        return Optional.ofNullable(stackDtoService.getStackViewById(getStackId()));
    }

    @Override
    protected void executeJob(JobExecutionContext context) {
        if (shouldSkipStatusCheck()) {
            LOGGER.info("Status check skipped for stack {}", getStackId());
            return;
        }
        try {
            measure(() -> {
                StackDto stack = stackDtoService.getById(getStackId());
                Status stackStatus = stack.getStatus();
                if (Status.getUnschedulableStatuses().contains(stackStatus)) {
                    LOGGER.debug("Stack sync will be unscheduled, stack state is {}", stackStatus);
                    jobService.unschedule(getLocalId());
                } else if (shouldSwitchToLongSyncJob(stackStatus, context)) {
                    LOGGER.debug("Stack sync will be scheduled to long polling, stack state is {}", stackStatus);
                    jobService.unschedule(getLocalId());
                    jobService.scheduleLongIntervalCheck(getStackId(), StackJobAdapter.class);
                } else if (null == stackStatus || IGNORED_STATES.contains(stackStatus)) {
                    LOGGER.debug("Stack sync is skipped, stack state is {}", stackStatus);
                } else if (SYNCABLE_STATES.contains(stackStatus)) {
                    doSync(stack);
                    switchToShortSyncIfNecessary(context, stack);
                } else {
                    LOGGER.warn("Unhandled stack status, {}", stackStatus);
                }
                if (stackStatus != null) {
                    metricsClient.processStackStatus(stack.getResourceCrn(), stack.getCloudPlatform(), stackStatus.name(), stackStatus.ordinal(),
                            stackDtoService.computeMonitoringEnabled(stack));
                }
            }, LOGGER, "Check status took {} ms for stack {}.", getStackId());
        } catch (Exception e) {
            LOGGER.info("Exception during cluster state check.", e);
        }
    }

    private Set<String> getHostsWithSaltFailure(StackDto stackDto) {
        if (config.isSaltCheckEnabled()) {
            GatewayConfig gatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stackDto);
            Optional<Set<String>> failedMinions = saltSyncService.checkSaltMinions(gatewayConfig);
            if (failedMinions.isPresent()) {
                LOGGER.debug("Salt minions check failed for: {}", failedMinions.get());
                if (config.isSaltCheckStatusChangeEnabled()) {
                    return failedMinions.get();
                }
            }
        }
        return Set.of();
    }

    private boolean shouldSkipStatusCheck() {
        if (flowLogService.isOtherFlowRunning(getStackId())) {
            LOGGER.debug("StackStatusCheckerJob cannot run, because flow is running for stack: {}", getStackId());
            return true;
        }
        Optional<FlowLog> lastFlowLog = flowLogService.getLastFlowLogWithEndTime(getStackId());
        if (lastFlowLog.isPresent()) {
            FlowLog flowLog = lastFlowLog.get();
            Instant skipTimeInstant = clock.nowMinus(Duration.ofMinutes(config.getSkipWindow()));
            Instant lastFlowLogEndTimeInstant = Instant.ofEpochMilli(flowLog.getEndTime());
            if (lastFlowLogEndTimeInstant.isAfter(skipTimeInstant)) {
                LOGGER.debug("StackStatusCheckerJob skipped, because the last flow log was finished for stack {}. Skip window is {} minutes. " +
                                "Last flow log endtime in UTC: {}. Skip time in UTC: {}.", getStackId(), config.getSkipWindow(),
                        lastFlowLogEndTimeInstant.atZone(ZoneOffset.UTC), skipTimeInstant.atZone(ZoneOffset.UTC));
                return true;
            }
        }
        return false;
    }

    private void switchToShortSyncIfNecessary(JobExecutionContext context, StackDto stackDto) {
        if (jobService.isLongSyncJob(context)) {
            Status stackStatus = stackDto.getStatus();
            if (!LONG_SYNCABLE_STATES.contains(stackStatus)) {
                jobService.schedule(getStackId(), StackJobAdapter.class);
            }
        }
    }

    private boolean shouldSwitchToLongSyncJob(Status stackStatus, JobExecutionContext context) {
        return !jobService.isLongSyncJob(context) && LONG_SYNCABLE_STATES.contains(stackStatus);
    }

    private void doSync(StackDto stack) {
        Set<String> hostsWithSaltFailure = getHostsWithSaltFailure(stack);
        ClusterApi connector = clusterApiConnectors.getConnector(stack);
        List<InstanceMetadataView> runningInstances = instanceMetaDataService.getAllNotTerminatedInstanceMetadataViewsByStackId(stack.getId());
        try {
            if (isClusterManagerRunning(stack.getStack(), connector)) {
                ExtendedHostStatuses extendedHostStatuses = getExtendedHostStatuses(stack, connector);
                Map<HostName, Set<HealthCheck>> hostStatuses = extendedHostStatuses.getHostsHealth();
                LOGGER.debug("Cluster '{}' state check, host certicates expiring: [{}], cm running, hoststates: {}",
                        stack.getId(), extendedHostStatuses.isAnyCertExpiring(), hostStatuses);
                Map<InstanceMetadataView, Optional<String>> failedInstancesInstanceMetadata =
                        getFailedInstancesInstanceMetadata(stack, extendedHostStatuses, runningInstances);
                updateFailedInstancesWithSaltCheckResult(hostsWithSaltFailure, failedInstancesInstanceMetadata, runningInstances);
                reportHealthAndSyncInstances(stack, runningInstances, failedInstancesInstanceMetadata,
                        getNewHealthyHostNames(extendedHostStatuses, runningInstances, hostsWithSaltFailure), extendedHostStatuses.isAnyCertExpiring(),
                        extendedHostStatuses.getCertExpirationDetails());
            } else {
                syncInstances(stack, runningInstances);
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Error during sync", e);
            syncInstances(stack, runningInstances);
        }
    }

    private void updateFailedInstancesWithSaltCheckResult(Set<String> hostsWithSaltFailure,
            Map<InstanceMetadataView, Optional<String>> failedInstancesInstanceMetadata,
            List<InstanceMetadataView> runningInstances) {
        String saltFailureMessage = messagesService.getMessage(CLUSTER_FAILED_NODES_SALT_FAILURE_EVENT.getMessage());
        hostsWithSaltFailure.forEach(hostWithSaltFailure -> {
            Optional<InstanceMetadataView> instanceMetadataView;
            Optional<String> reasonWithSaltMessage = Optional.of(saltFailureMessage);
            Optional<Map.Entry<InstanceMetadataView, Optional<String>>> entryForHost = failedInstancesInstanceMetadata.entrySet().stream()
                    .filter(entry -> StringUtils.equals(entry.getKey().getDiscoveryFQDN(), hostWithSaltFailure))
                    .findFirst();
            if (entryForHost.isPresent()) {
                Optional<String> reason = entryForHost.get().getValue();
                instanceMetadataView = Optional.ofNullable(entryForHost.get().getKey());
                if (reason.isPresent()) {
                    reasonWithSaltMessage = Optional.of(String.format("%s %s", reason.get(), saltFailureMessage));
                }
            } else {
                instanceMetadataView = runningInstances.stream()
                        .filter(imd -> StringUtils.equals(imd.getDiscoveryFQDN(), hostWithSaltFailure))
                        .findFirst();
            }
            if (instanceMetadataView.isPresent()) {
                failedInstancesInstanceMetadata.put(instanceMetadataView.get(), reasonWithSaltMessage);
            }
        });
    }

    private void reportHealthAndSyncInstances(StackDto stack, Collection<InstanceMetadataView> runningInstances,
            Map<InstanceMetadataView, Optional<String>> failedInstances, Set<String> newHealthyHostNames, boolean hostCertExpiring,
            String certExpirationDetails) {
        Map<String, Optional<String>> newFailedNodeNamesWithReason = failedInstances.entrySet()
                .stream()
                .filter(e -> !Set.of(SERVICES_UNHEALTHY, STOPPED)
                        .contains(e.getKey().getInstanceStatus()))
                .filter(e -> e.getKey().getDiscoveryFQDN() != null)
                .collect(Collectors.toMap(e -> e.getKey().getDiscoveryFQDN(), Map.Entry::getValue));
        runIfFlowNotRunning(
                () -> updateStates(stack, failedInstances.keySet(), newFailedNodeNamesWithReason, newHealthyHostNames, hostCertExpiring, certExpirationDetails));
        syncInstances(stack, runningInstances, failedInstances.keySet(), InstanceSyncState.RUNNING, true);
    }

    private void updateStates(StackDto stack, Collection<InstanceMetadataView> failedInstances, Map<String, Optional<String>> newFailedNodeNamesWithReason,
            Set<String> newHealthyHostNames, boolean hostCertExpiring, String certExpirationDetails) {
        LOGGER.info("Updating status: Failed instances: {} New failed node names: {} New healthy host name: {} Host cert expiring: {}",
                failedInstances.stream().map(s -> s.getDiscoveryFQDN()).collect(toSet()),
                newFailedNodeNamesWithReason.keySet(),
                newHealthyHostNames,
                hostCertExpiring);
        clusterService.updateClusterCertExpirationState(stack.getCluster(), hostCertExpiring, certExpirationDetails);
        if (!failedInstances.isEmpty()) {
            if (stackUtil.stopStartScalingEntitlementEnabled(stack.getStack())) {
                Set<InstanceMetadataView> stoppedInstances = failedInstances.stream().filter(im -> im.getInstanceStatus().equals(STOPPED)).collect(toSet());
                long stoppedInstancesCount = stoppedInstances.size();
                Set<String> computeGroups = getComputeHostGroups(stack.getBlueprint());
                boolean stoppedComputeOnly = stoppedInstances.stream().map(im -> im.getInstanceGroupName()).allMatch(computeGroups::contains);
                if (stoppedInstancesCount > 0 && stoppedComputeOnly && stoppedInstancesCount == failedInstances.size()) {
                    // TODO CB-15146: This may need to change depending on the final form of how we check which operations are to be allowed
                    //  when there are some STOPPED instances
                    clusterService.updateClusterStatusByStackId(stack.getId(), DetailedStackStatus.AVAILABLE);
                } else {
                    LOGGER.debug("WithStopStartEntitlement, putting cluster into NODE_FAILURE. Counts: stoppedInstanceCount={}, failedInstanceCount={}",
                            stoppedInstancesCount, failedInstances.size());
                    clusterService.updateClusterStatusByStackId(stack.getId(), DetailedStackStatus.NODE_FAILURE);
                }
            } else {
                clusterService.updateClusterStatusByStackId(stack.getId(), DetailedStackStatus.NODE_FAILURE);
            }
        } else if (STATES_FROM_AVAILABLE_ALLOWED.contains(stack.getStatus())) {
            clusterService.updateClusterStatusByStackId(stack.getId(), DetailedStackStatus.AVAILABLE);
        }
        clusterOperationService.reportHealthChange(stack.getResourceCrn(), newFailedNodeNamesWithReason, newHealthyHostNames);
    }

    private void runIfFlowNotRunning(Runnable function) {
        if (!flowLogService.isOtherFlowRunning(getStackId())) {
            function.run();
        }
    }

    private boolean isClusterManagerRunning(StackView stack, ClusterApi connector) {
        return !stack.isStopped()
                && !stack.isStackInDeletionOrFailedPhase()
                && isCMRunning(connector);
    }

    private void syncInstances(StackDto stack, Collection<InstanceMetadataView> instanceMetaData) {
        syncInstances(stack, instanceMetaData, instanceMetaData, InstanceSyncState.DELETED_ON_PROVIDER_SIDE, false);
    }

    private void syncInstances(StackDto stack, Collection<InstanceMetadataView> runningInstances,
            Collection<InstanceMetadataView> instanceMetaData, InstanceSyncState defaultState, boolean cmServerRunning) {
        List<CloudInstance> cloudInstances = cloudInstanceConverter.convert(instanceMetaData, stack.getStack());
        List<CloudVmInstanceStatus> instanceStatuses = stackInstanceStatusChecker.queryInstanceStatuses(stack, cloudInstances);
        LOGGER.debug("Cluster '{}' state check on provider, instances: {}", stack.getId(), instanceStatuses);
        SyncConfig syncConfig = new SyncConfig(true, cmServerRunning, isProviderUnreachable(instanceStatuses));
        runIfFlowNotRunning(() -> syncService.autoSync(stack.getStack(), runningInstances, instanceStatuses, defaultState, syncConfig));
    }

    private boolean isProviderUnreachable(List<CloudVmInstanceStatus> instanceStatuses) {
        if (!instanceStatuses.isEmpty()) {
            return instanceStatuses.stream()
                    .map(CloudVmInstanceStatus::getStatus)
                    .allMatch(UNKNOWN::equals);
        }
        return false;
    }

    private boolean isCMRunning(ClusterApi connector) {
        return Benchmark.measureAndWarnIfLong(() -> connector.clusterStatusService().isClusterManagerRunningQuickCheck(),
                LOGGER,
                "Checking Cloudera Manager is running");
    }

    private ExtendedHostStatuses getExtendedHostStatuses(StackDto stack, ClusterApi connector) {
        return Benchmark.measureAndWarnIfLong(() -> connector.clusterStatusService()
                        .getExtendedHostStatuses(runtimeVersionService.getRuntimeVersion(stack.getCluster().getId())),
                LOGGER,
                "Getting extended host statuses");
    }

    private Set<String> getNewHealthyHostNames(ExtendedHostStatuses hostStatuses, Collection<InstanceMetadataView> runningInstances,
            Set<String> hostsWithSaltFailure) {
        Set<String> healthyHosts = hostStatuses.getHostsHealth().keySet().stream()
                .filter(hostStatuses::isHostHealthy)
                .map(HostName::value)
                .filter(host -> !hostsWithSaltFailure.contains(host))
                .collect(toSet());
        Set<String> unhealthyStoredHosts = runningInstances.stream()
                .filter(i -> STATES_FROM_HEALTHY_ALLOWED.contains(i.getInstanceStatus()))
                .filter(i -> i.getDiscoveryFQDN() != null)
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .collect(toSet());
        return Sets.intersection(healthyHosts, unhealthyStoredHosts);
    }

    private Map<InstanceMetadataView, Optional<String>> getFailedInstancesInstanceMetadata(StackDto stack, ExtendedHostStatuses hostStatuses,
            Collection<InstanceMetadataView> runningInstances) {
        Map<String, Optional<String>> failedHosts = hostStatuses.getHostsHealth().entrySet().stream()
                .filter(e -> !hostStatuses.isHostHealthy(e.getKey()))
                .collect(Collectors.toMap(e -> e.getKey().value(), e -> Optional.ofNullable(hostStatuses.statusReasonForHost(e.getKey()))));
        Set<String> noReportHosts = runningInstances.stream()
                .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null)
                .filter(instanceMetaData -> !instanceMetaData.isZombie())
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .filter(discoveryFQDN -> hostStatuses.getHostsHealth().get(hostName(discoveryFQDN)) == null)
                .collect(toSet());
        return serviceStatusCheckerLogLocationDecorator.decorate(runningInstances.stream()
                .filter(i -> failedHosts.containsKey(i.getDiscoveryFQDN()) || noReportHosts.contains(i.getDiscoveryFQDN()))
                .collect(Collectors.toMap(imd -> imd, imd -> getReasonForFailedInstance(failedHosts, imd))), hostStatuses, stack);
    }

    private Optional<String> getReasonForFailedInstance(Map<String, Optional<String>> failedHosts, InstanceMetadataView imd) {
        if (failedHosts.containsKey(imd.getDiscoveryFQDN())) {
            return failedHosts.get(imd.getDiscoveryFQDN());
        }
        return Optional.empty();
    }

    private Set<String> getComputeHostGroups(Blueprint blueprint) {
        String blueprintText = blueprint.getBlueprintJsonText();
        CmTemplateProcessor blueprintProcessor = cmTemplateProcessorFactory.get(blueprintText);
        Versioned blueprintVersion = () -> blueprintProcessor.getVersion().get();
        return blueprintProcessor.getComputeHostGroups(blueprintVersion);
    }

    private Long getStackId() {
        return Long.valueOf(getLocalId());
    }
}
