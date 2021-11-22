package com.sequenceiq.cloudbreak.job;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_RUNNING;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_UNHEALTHY;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.STOPPED;
import static com.sequenceiq.cloudbreak.cloud.model.HostName.hostName;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;
import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import com.gs.collections.impl.factory.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.InternalCrnBuilder;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.status.ExtendedHostStatuses;
import com.sequenceiq.cloudbreak.common.type.HealthCheck;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.cloudbreak.quartz.statuschecker.service.StatusCheckerJobService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterOperationService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.RuntimeVersionService;
import com.sequenceiq.cloudbreak.service.stack.StackInstanceStatusChecker;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.StackViewService;
import com.sequenceiq.cloudbreak.service.stack.flow.InstanceSyncState;
import com.sequenceiq.cloudbreak.service.stack.flow.StackSyncService;
import com.sequenceiq.cloudbreak.service.stack.flow.SyncConfig;
import com.sequenceiq.flow.core.FlowLogService;

import io.opentracing.Tracer;

@DisallowConcurrentExecution
@Component
public class StackStatusCheckerJob extends StatusCheckerJob {

    private static final String DATAHUB_INTERNAL_ACTOR_CRN = new InternalCrnBuilder(Crn.Service.DATAHUB).getInternalCrnForServiceAsString();

    private static final Logger LOGGER = LoggerFactory.getLogger(StackStatusCheckerJob.class);

    @Inject
    private StatusCheckerJobService jobService;

    @Inject
    private StackService stackService;

    @Inject
    private StackViewService stackViewService;

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

    public StackStatusCheckerJob(Tracer tracer) {
        super(tracer, "Stack Status Checker Job");
    }

    @Override
    protected Object getMdcContextObject() {
        return stackViewService.findById(getStackId()).orElseGet(StackView::new);
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        if (flowLogService.isOtherFlowRunning(getStackId())) {
            LOGGER.debug("StackStatusCheckerJob cannot run, because flow is running for stack: {}", getStackId());
            return;
        }
        try {
            measure(() -> {
                Stack stack = stackService.get(getStackId());
                Status stackStatus = stack.getStatus();
                if (Status.getUnschedulableStatuses().contains(stackStatus)) {
                    LOGGER.debug("Stack sync will be unscheduled, stack state is {}", stackStatus);
                    jobService.unschedule(getLocalId());
                } else if (shouldSwitchToLongSyncJob(stackStatus, context)) {
                    LOGGER.debug("Stack sync will be scheduled to long polling, stack state is {}", stackStatus);
                    jobService.unschedule(getLocalId());
                    jobService.scheduleLongIntervalCheck(new StackJobAdapter(stack));
                } else if (null == stackStatus || ignoredStates().contains(stackStatus)) {
                    LOGGER.debug("Stack sync is skipped, stack state is {}", stackStatus);
                } else if (syncableStates().contains(stackStatus)) {
                    ThreadBasedUserCrnProvider.doAs(DATAHUB_INTERNAL_ACTOR_CRN, () -> doSync(stack));
                    switchToShortSyncIfNecessary(context);
                } else {
                    LOGGER.warn("Unhandled stack status, {}", stackStatus);
                }
            }, LOGGER, "Check status took {} ms for stack {}.", getStackId());
        } catch (Exception e) {
            LOGGER.info("Exception during cluster state check.", e);
        }
    }

    private void switchToShortSyncIfNecessary(JobExecutionContext context) {
        if (isLongSyncJob(context)) {
            Stack stack = stackService.get(getStackId());
            Status stackStatus = stack.getStatus();
            if (!longSyncableStates().contains(stackStatus)) {
                jobService.schedule(new StackJobAdapter(stack));
            }
        }
    }

    private boolean shouldSwitchToLongSyncJob(Status stackStatus, JobExecutionContext context) {
        return !isLongSyncJob(context) && longSyncableStates().contains(stackStatus);
    }

    private boolean isLongSyncJob(JobExecutionContext context) {
        return StatusCheckerJobService.LONG_SYNC_JOB_TYPE.equals(context.getMergedJobDataMap().get(StatusCheckerJobService.SYNC_JOB_TYPE));
    }

    private Set<Status> longSyncableStates() {
        return EnumSet.of(Status.DELETED_ON_PROVIDER_SIDE);
    }

    @VisibleForTesting
    Set<Status> ignoredStates() {
        return EnumSet.of(
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
                Status.BACKUP_IN_PROGRESS,
                Status.RESTORE_IN_PROGRESS,
                Status.LOAD_BALANCER_UPDATE_IN_PROGRESS,
                Status.RECOVERY_IN_PROGRESS,
                Status.RECOVERY_REQUESTED
        );
    }

    @VisibleForTesting
    Set<Status> syncableStates() {
        return EnumSet.of(
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
                Status.RECOVERY_FAILED
        );
    }

    private void doSync(Stack stack) {
        ClusterApi connector = clusterApiConnectors.getConnector(stack);
        Set<InstanceMetaData> runningInstances = instanceMetaDataService.findNotTerminatedForStack(stack.getId());
        try {
            if (isClusterManagerRunning(stack, connector)) {
                ExtendedHostStatuses extendedHostStatuses = connector.clusterStatusService().getExtendedHostStatuses(
                        runtimeVersionService.getRuntimeVersion(stack.getCluster().getId()));
                Map<HostName, Set<HealthCheck>> hostStatuses = extendedHostStatuses.getHostsHealth();
                LOGGER.debug("Cluster '{}' state check, host certicates expiring: [{}], cm running, hoststates: {}",
                        stack.getId(), extendedHostStatuses.isAnyCertExpiring(), hostStatuses);
                reportHealthAndSyncInstances(stack, runningInstances, getFailedInstancesInstanceMetadata(extendedHostStatuses, runningInstances),
                        getNewHealthyHostNames(extendedHostStatuses, runningInstances), extendedHostStatuses.isAnyCertExpiring());
            } else {
                syncInstances(stack, runningInstances, false);
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Error during sync", e);
            syncInstances(stack, runningInstances, false);
        }
    }

    private void reportHealthAndSyncInstances(Stack stack, Collection<InstanceMetaData> runningInstances,
            Map<InstanceMetaData, Optional<String>> failedInstances, Set<String> newHealthyHostNames, boolean hostCertExpiring) {
        Map<String, Optional<String>> newFailedNodeNamesWithReason = failedInstances.entrySet()
                .stream()
                .filter(e -> !Set.of(SERVICES_UNHEALTHY, STOPPED)
                        .contains(e.getKey().getInstanceStatus()))
                .filter(e -> e.getKey().getDiscoveryFQDN() != null)
                .collect(Collectors.toMap(e -> e.getKey().getDiscoveryFQDN(), Map.Entry::getValue));
        ifFlowNotRunning(() -> updateStates(stack, failedInstances.keySet(), newFailedNodeNamesWithReason, newHealthyHostNames, hostCertExpiring));
        syncInstances(stack, runningInstances, failedInstances.keySet(), InstanceSyncState.RUNNING, true);
    }

    private void updateStates(Stack stack, Collection<InstanceMetaData> failedInstances, Map<String, Optional<String>> newFailedNodeNamesWithReason,
            Set<String> newHealthyHostNames, boolean hostCertExpiring) {
        LOGGER.info("Updating status: Failed instances: {} New failed node names: {} New healthy host name: {} Host cert expiring: {}",
                failedInstances, newFailedNodeNamesWithReason.keySet(), newHealthyHostNames, hostCertExpiring);
        clusterService.updateClusterCertExpirationState(stack.getCluster(), hostCertExpiring);
        clusterOperationService.reportHealthChange(stack.getResourceCrn(), newFailedNodeNamesWithReason, newHealthyHostNames);
        if (!failedInstances.isEmpty()) {
            clusterService.updateClusterStatusByStackId(stack.getId(), Status.NODE_FAILURE);
        } else if (statesFromAvailableAllowed().contains(stack.getCluster().getStatus())) {
            clusterService.updateClusterStatusByStackId(stack.getId(), Status.AVAILABLE);
        }
    }

    private void ifFlowNotRunning(Runnable function) {
        if (flowLogService.isOtherFlowRunning(getStackId())) {
            return;
        }
        function.run();
    }

    private EnumSet<Status> statesFromAvailableAllowed() {
        return EnumSet.of(
                Status.AMBIGUOUS,
                Status.UNREACHABLE,
                Status.NODE_FAILURE,
                Status.STOPPED,
                Status.START_FAILED,
                Status.STOP_FAILED,
                Status.UPDATE_FAILED,
                Status.ENABLE_SECURITY_FAILED);
    }

    private boolean isClusterManagerRunning(Stack stack, ClusterApi connector) {
        return !stack.isStopped()
                && !stack.isStackInDeletionOrFailedPhase()
                && isCMRunning(connector);
    }

    private void syncInstances(Stack stack, Collection<InstanceMetaData> instanceMetaData, boolean cmServerRunning) {
        syncInstances(stack, instanceMetaData, instanceMetaData, InstanceSyncState.DELETED_ON_PROVIDER_SIDE, cmServerRunning);
    }

    private void syncInstances(Stack stack, Collection<InstanceMetaData> runningInstances,
            Collection<InstanceMetaData> instanceMetaData, InstanceSyncState defaultState, boolean cmServerRunning) {
        List<CloudInstance> cloudInstances = cloudInstanceConverter.convert(instanceMetaData, stack.getEnvironmentCrn(), stack.getStackAuthentication());
        List<CloudVmInstanceStatus> instanceStatuses = stackInstanceStatusChecker.queryInstanceStatuses(stack, cloudInstances);
        LOGGER.debug("Cluster '{}' state check on provider, instances: {}", stack.getId(), instanceStatuses);
        SyncConfig syncConfig = new SyncConfig(true, cmServerRunning);
        ifFlowNotRunning(() -> syncService.autoSync(stack, runningInstances, instanceStatuses, defaultState, syncConfig));
    }

    private boolean isCMRunning(ClusterApi connector) {
        return connector.clusterStatusService().isClusterManagerRunningQuickCheck();
    }

    private Set<String> getNewHealthyHostNames(ExtendedHostStatuses hostStatuses, Set<InstanceMetaData> runningInstances) {
        Set<String> healthyHosts = hostStatuses.getHostsHealth().keySet().stream()
                .filter(hostStatuses::isHostHealthy)
                .map(HostName::value)
                .collect(toSet());
        Set<String> unhealthyStoredHosts = runningInstances.stream()
                .filter(i -> i.getInstanceStatus() == SERVICES_UNHEALTHY || i.getInstanceStatus() == SERVICES_RUNNING)
                .filter(i -> i.getDiscoveryFQDN() != null)
                .map(InstanceMetaData::getDiscoveryFQDN)
                .collect(toSet());
        return Sets.intersect(healthyHosts, unhealthyStoredHosts);
    }

    private Map<InstanceMetaData, Optional<String>> getFailedInstancesInstanceMetadata(ExtendedHostStatuses hostStatuses,
            Set<InstanceMetaData> runningInstances) {
        Map<String, Optional<String>> failedHosts = hostStatuses.getHostsHealth().entrySet().stream()
                .filter(e -> !hostStatuses.isHostHealthy(e.getKey()))
                .collect(Collectors.toMap(e -> e.getKey().value(), e -> Optional.ofNullable(hostStatuses.statusReasonForHost(e.getKey()))));
        Set<String> noReportHosts = runningInstances.stream()
                .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null)
                .map(InstanceMetaData::getDiscoveryFQDN)
                .filter(discoveryFQDN -> hostStatuses.getHostsHealth().get(hostName(discoveryFQDN)) == null)
                .collect(toSet());
        return runningInstances.stream()
                .filter(i -> failedHosts.containsKey(i.getDiscoveryFQDN()) || noReportHosts.contains(i.getDiscoveryFQDN()))
                .collect(Collectors.toMap(imd -> imd, imd -> getReasonForFailedInstance(failedHosts, imd)));
    }

    private Optional<String> getReasonForFailedInstance(Map<String, Optional<String>> failedHosts, InstanceMetaData imd) {
        if (failedHosts.containsKey(imd.getDiscoveryFQDN())) {
            return failedHosts.get(imd.getDiscoveryFQDN());
        }
        return Optional.empty();
    }

    private Long getStackId() {
        return Long.valueOf(getLocalId());
    }
}
