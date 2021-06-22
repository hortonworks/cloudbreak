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
import java.util.Map.Entry;
import java.util.Set;

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
import com.sequenceiq.cloudbreak.common.type.ClusterManagerState;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerState.ClusterManagerStatus;
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
                if (unshedulableStates().contains(stack.getStatus())) {
                    LOGGER.debug("Stack sync will be unscheduled, stack state is {}", stack.getStatus());
                    jobService.unschedule(getLocalId());
                } else if (null == stack.getStatus() || ignoredStates().contains(stack.getStatus())) {
                    LOGGER.debug("Stack sync is skipped, stack state is {}", stack.getStatus());
                } else if (syncableStates().contains(stack.getStatus())) {
                    ThreadBasedUserCrnProvider.doAs(DATAHUB_INTERNAL_ACTOR_CRN, () -> doSync(stack));
                } else {
                    LOGGER.warn("Unhandled stack status, {}", stack.getStatus());
                }
            }, LOGGER, "Check status took {} ms for stack {}.", getStackId());
        } catch (Exception e) {
            LOGGER.info("Exception during cluster state check.", e);
        }
    }

    @VisibleForTesting
    Set<Status> unshedulableStates() {
        return EnumSet.of(
                Status.CREATE_FAILED,
                Status.PRE_DELETE_IN_PROGRESS,
                Status.DELETE_IN_PROGRESS,
                Status.DELETE_FAILED,
                Status.DELETE_COMPLETED,
                Status.DELETED_ON_PROVIDER_SIDE,
                Status.EXTERNAL_DATABASE_CREATION_FAILED,
                Status.EXTERNAL_DATABASE_DELETION_IN_PROGRESS,
                Status.EXTERNAL_DATABASE_DELETION_FINISHED,
                Status.EXTERNAL_DATABASE_DELETION_FAILED,
                Status.LOAD_BALANCER_UPDATE_FINISHED,
                Status.LOAD_BALANCER_UPDATE_FAILED
        );
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
                Status.LOAD_BALANCER_UPDATE_IN_PROGRESS
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
                Status.RESTORE_FAILED,
                Status.BACKUP_FAILED,
                Status.BACKUP_FINISHED,
                Status.RESTORE_FINISHED,
                Status.EXTERNAL_DATABASE_START_FAILED,
                Status.EXTERNAL_DATABASE_START_IN_PROGRESS,
                Status.EXTERNAL_DATABASE_START_FINISHED,
                Status.EXTERNAL_DATABASE_STOP_FAILED,
                Status.EXTERNAL_DATABASE_STOP_IN_PROGRESS,
                Status.EXTERNAL_DATABASE_STOP_FINISHED
        );
    }

    private void doSync(Stack stack) {
        ClusterApi connector = clusterApiConnectors.getConnector(stack);
        Set<InstanceMetaData> runningInstances = instanceMetaDataService.findNotTerminatedForStack(stack.getId());
        try {
            if (isClusterManagerRunning(stack, connector)) {
                ExtendedHostStatuses extendedHostStatuses = connector.clusterStatusService().getExtendedHostStatuses();
                Map<HostName, ClusterManagerState> hostStatuses = extendedHostStatuses.getHostHealth();
                LOGGER.debug("Cluster '{}' state check, host certicates expiring: [{}], cm running, hoststates: {}",
                        stack.getId(), extendedHostStatuses.isHostCertExpiring(), hostStatuses);
                reportHealthAndSyncInstances(stack, runningInstances, getFailedInstancesInstanceMetadata(hostStatuses, runningInstances),
                        getNewHealthyHostNames(hostStatuses, runningInstances), extendedHostStatuses.isHostCertExpiring());
            } else {
                syncInstances(stack, runningInstances, false);
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Error during sync", e);
            syncInstances(stack, runningInstances, false);
        }
    }

    private void reportHealthAndSyncInstances(Stack stack, Collection<InstanceMetaData> runningInstances, Collection<InstanceMetaData> failedInstances,
            Set<String> newHealtyHostNames, boolean hostCertExpiring) {
        Set<String> newFailedNodeNames = failedInstances.stream()
                .filter(i -> !Set.of(SERVICES_UNHEALTHY, STOPPED).contains(i.getInstanceStatus()))
                .map(InstanceMetaData::getDiscoveryFQDN)
                .collect(toSet());
        ifFlowNotRunning(() -> updateStates(stack, failedInstances, newFailedNodeNames, newHealtyHostNames, hostCertExpiring));
        syncInstances(stack, runningInstances, failedInstances, InstanceSyncState.RUNNING, true);
    }

    private void updateStates(Stack stack, Collection<InstanceMetaData> failedInstances, Set<String> newFailedNodeNames, Set<String> newHealtyHostNames,
            boolean hostCertExpiring) {
        LOGGER.info("Updating status: Failed instances: {} New failed node names: {} New healthy host name: {} Host cert expiring: {}",
                failedInstances, newFailedNodeNames, newHealtyHostNames, hostCertExpiring);
        clusterService.updateClusterCertExpirationState(stack.getCluster(), hostCertExpiring);
        clusterOperationService.reportHealthChange(stack.getResourceCrn(), newFailedNodeNames, newHealtyHostNames);
        if (!failedInstances.isEmpty()) {
            clusterService.updateClusterStatusByStackId(stack.getId(), Status.AMBIGUOUS);
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

    private Set<String> getNewHealthyHostNames(Map<HostName, ClusterManagerState> hostStatuses, Set<InstanceMetaData> runningInstances) {
        Set<String> healthyHosts = hostStatuses.entrySet().stream()
                .filter(e -> e.getValue().getClusterManagerStatus() == ClusterManagerStatus.HEALTHY)
                .map(Entry::getKey)
                .map(HostName::value)
                .collect(toSet());
        Set<String> unhealthyStoredHosts = runningInstances.stream()
                .filter(i -> i.getInstanceStatus() == SERVICES_UNHEALTHY || i.getInstanceStatus() == SERVICES_RUNNING)
                .map(InstanceMetaData::getDiscoveryFQDN)
                .collect(toSet());
        return Sets.intersect(healthyHosts, unhealthyStoredHosts);
    }

    private Set<InstanceMetaData> getFailedInstancesInstanceMetadata(Map<HostName, ClusterManagerState> hostStatuses,
            Set<InstanceMetaData> runningInstances) {
        Set<String> failedHosts = hostStatuses.entrySet().stream()
                .filter(e -> e.getValue().getClusterManagerStatus() == ClusterManagerStatus.UNHEALTHY)
                .map(Entry::getKey)
                .map(HostName::value)
                .collect(toSet());
        Set<String> noReportHosts = runningInstances.stream()
                .filter(i -> hostStatuses.get(hostName(i.getDiscoveryFQDN())) == null)
                .map(InstanceMetaData::getDiscoveryFQDN)
                .collect(toSet());
        return runningInstances.stream()
                .filter(i -> failedHosts.contains(i.getDiscoveryFQDN()) || noReportHosts.contains(i.getDiscoveryFQDN()))
                .collect(toSet());
    }

    private Long getStackId() {
        return Long.valueOf(getLocalId());
    }
}
