package com.sequenceiq.freeipa.sync;

import static com.sequenceiq.cloudbreak.util.Benchmark.checkedMeasure;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.metrics.MetricsClient;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.SaltSyncService;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.cloudbreak.quartz.statuschecker.service.StatusCheckerJobService;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@DisallowConcurrentExecution
@Component
public class StackStatusCheckerJob extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackStatusCheckerJob.class);

    private static final String AUTO_SYNC_LOG_PREFIX = ":::Auto sync::: ";

    private static final EnumSet<Status> LONG_SYNCABLE_STATES = EnumSet.of(Status.DELETED_ON_PROVIDER_SIDE, Status.STALE);

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private FreeipaChecker freeipaChecker;

    @Inject
    private ProviderChecker providerChecker;

    @Inject
    private StackService stackService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private AutoSyncConfig autoSyncConfig;

    @Inject
    private FreeipaStatusInfoLogger freeipaStatusInfoLogger;

    @Inject
    private MetricsClient metricsClient;

    @Inject
    private StatusCheckerJobService jobService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private SaltSyncService saltSyncService;

    @Override
    protected Optional<Object> getMdcContextObject() {
        return Optional.ofNullable(stackService.getStackById(getStackId()));
    }

    @Override
    protected void executeJob(JobExecutionContext context) {
        Long stackId = getStackId();
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        try {
            if (flowLogService.isOtherFlowRunning(stackId)) {
                LOGGER.debug("StackStatusCheckerJob cannot run, because flow is running for freeipa stack: {}", stackId);
            } else {
                LOGGER.debug("No flows running, trying to sync freeipa");
                rescheduleIfRequired(stack.getStackStatus().getStatus(), context);
                syncAStack(stack, false);
            }
            if (stack.getStackStatus() != null && stack.getStackStatus().getStatus() != null) {
                metricsClient.processStackStatus(stack.getResourceCrn(), stack.getCloudPlatform(), stack.getStackStatus().getStatus().name(),
                        stack.getStackStatus().getStatus().ordinal(), stackService.computeMonitoringEnabled(stack));
            }
        } catch (InterruptSyncingException e) {
            LOGGER.info("Syncing was interrupted", e);
        }
    }

    private Set<String> getHostsWithSaltFailure(Stack stack) {
        if (autoSyncConfig.isSaltCheckEnabled()) {
            GatewayConfig gatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
            Optional<Set<String>> failedMinions = saltSyncService.checkSaltMinions(gatewayConfig);
            if (failedMinions.isPresent()) {
                LOGGER.debug("Salt minions check failed for: {}", failedMinions.get());
                if (autoSyncConfig.isSaltCheckStatusChangeEnabled()) {
                    return failedMinions.get();
                }
            }
        }
        return Set.of();
    }

    private void rescheduleIfRequired(Status stackStatus, JobExecutionContext context) {
        switchToShortSyncIfNecessary(stackStatus, context);
        switchToLongSyncIfNecessary(stackStatus, context);
    }

    private void switchToShortSyncIfNecessary(Status stackStatus, JobExecutionContext context) {
        if (jobService.isLongSyncJob(context) && !LONG_SYNCABLE_STATES.contains(stackStatus)) {
            LOGGER.info("Switching to short sync as status is {}", stackStatus);
            jobService.unschedule(getLocalId());
            jobService.schedule(getStackId(), StackJobAdapter.class);
        }
    }

    private void switchToLongSyncIfNecessary(Status stackStatus, JobExecutionContext context) {
        if (!jobService.isLongSyncJob(context) && LONG_SYNCABLE_STATES.contains(stackStatus)) {
            LOGGER.info("Switching to long sync as status is {}", stackStatus);
            jobService.unschedule(getLocalId());
            jobService.scheduleLongIntervalCheck(getStackId(), StackJobAdapter.class);
        }
    }

    private Long getStackId() {
        return Long.valueOf(getLocalId());
    }

    public void syncAStack(Stack stack, boolean updateStatusFromFlow) {
        try {
            checkedMeasure(() -> {
                Set<InstanceMetaData> notTerminatedForStack = stack.getAllInstanceMetaDataList().stream()
                        .filter(Predicate.not(InstanceMetaData::isTerminated))
                        .collect(Collectors.toSet());

                Set<InstanceMetaData> unusableInstances = collectAndUpdateUnusableInstances(notTerminatedForStack);

                Set<InstanceMetaData> checkableInstances = Sets.newHashSet(Sets.difference(notTerminatedForStack, unusableInstances));

                int alreadyDeletedCount = notTerminatedForStack.size() - checkableInstances.size();
                if (alreadyDeletedCount > 0) {
                    LOGGER.info(AUTO_SYNC_LOG_PREFIX + "Count of already in deleted on provider side state: {}", alreadyDeletedCount);
                }
                if (!checkableInstances.isEmpty()) {
                    Set<String> hostsWithSaltFailure = getHostsWithSaltFailure(stack);
                    SyncResult syncResult = freeipaChecker.getStatus(stack, checkableInstances, hostsWithSaltFailure);
                    if (DetailedStackStatus.AVAILABLE == syncResult.getStatus()) {
                        for (Map.Entry<InstanceMetaData, DetailedStackStatus> entry : syncResult.getInstanceStatusMap().entrySet()) {
                            updateInstanceStatus(entry.getKey(), entry.getValue());
                        }
                        updateStackStatus(stack, syncResult, null, alreadyDeletedCount, updateStatusFromFlow);
                    } else {
                        List<ProviderSyncResult> results = providerChecker.updateAndGetStatuses(stack, checkableInstances,
                                syncResult.getInstanceStatusMap(), updateStatusFromFlow);
                        if (!results.isEmpty()) {
                            updateStackStatus(stack, syncResult, results, alreadyDeletedCount, updateStatusFromFlow);
                        } else {
                            LOGGER.debug(AUTO_SYNC_LOG_PREFIX + "results is empty, skip update");
                        }
                    }
                } else if (alreadyDeletedCount > 0) {
                    SyncResult syncResult = new SyncResult("FreeIpa is " + DetailedStackStatus.DELETED_ON_PROVIDER_SIDE,
                            DetailedStackStatus.DELETED_ON_PROVIDER_SIDE, null);
                    updateStackStatus(stack, syncResult, null, alreadyDeletedCount, updateStatusFromFlow);
                }
                freeipaStatusInfoLogger.logFreeipaStatus(stack.getId(), checkableInstances);
            }, LOGGER, AUTO_SYNC_LOG_PREFIX + "freeipa stack sync in {}ms");
        } catch (Exception e) {
            LOGGER.info(AUTO_SYNC_LOG_PREFIX + "Error occurred during freeipa sync: {}", e.getMessage(), e);
        }
    }

    private Set<InstanceMetaData> collectAndUpdateUnusableInstances(Set<InstanceMetaData> notTerminatedForStack) {
        Set<InstanceMetaData> unusableInstances = notTerminatedForStack.stream()
                .filter(im -> StringUtils.isAnyBlank(im.getDiscoveryFQDN(), im.getPrivateIp()))
                .collect(Collectors.toSet());
        LOGGER.info(AUTO_SYNC_LOG_PREFIX + "Unusable instances due to missing FQDN or IP: {}", unusableInstances);
        unusableInstances.forEach(im -> {
            if (StringUtils.isBlank(im.getInstanceId())) {
                setStatusIfNotTheSame(im, InstanceStatus.TERMINATED);
            } else {
                setStatusIfNotTheSame(im, InstanceStatus.FAILED);
            }
        });
        return unusableInstances;
    }

    private void updateInstanceStatus(InstanceMetaData instanceMetaData, DetailedStackStatus detailedStackStatus) {
        switch (detailedStackStatus) {
            case AVAILABLE:
                setStatusIfNotTheSame(instanceMetaData, InstanceStatus.CREATED);
                break;
            case UNHEALTHY:
                setStatusIfNotTheSame(instanceMetaData, InstanceStatus.UNHEALTHY);
                break;
            case UNREACHABLE:
                setStatusIfNotTheSame(instanceMetaData, InstanceStatus.UNREACHABLE);
                break;
            default:
                LOGGER.info(AUTO_SYNC_LOG_PREFIX + "the '{}' status is not converted", detailedStackStatus);
        }
    }

    private void updateStackStatus(Stack stack, SyncResult result, List<ProviderSyncResult> providerSyncResults, int alreadyDeletedCount,
            boolean updateStatusFromFlow) {
        DetailedStackStatus status = providerSyncResults == null ? result.getStatus() : getStackStatus(providerSyncResults, result, alreadyDeletedCount,
                stack.getStackStatus());
        if (status != stack.getStackStatus().getDetailedStackStatus()) {
            if (autoSyncConfig.isUpdateStatus()) {
                if (!updateStatusFromFlow && flowLogService.isOtherFlowRunning(stack.getId())) {
                    throw new InterruptSyncingException(AUTO_SYNC_LOG_PREFIX + "interrupt syncing in updateStackStatus, flow is running on freeipa stack " +
                            stack.getName());
                } else {
                    stackUpdater.updateStackStatus(stack, status, result.getMessage());
                }
            } else {
                LOGGER.info(AUTO_SYNC_LOG_PREFIX + "The stack status would be had to update from {} to {}",
                        stack.getStackStatus().getDetailedStackStatus(), status);
            }
        }
    }

    private DetailedStackStatus getStackStatus(List<ProviderSyncResult> providerSyncResults, SyncResult result, int alreadyDeletedCount,
            StackStatus stackStatus) {
        if (providerSyncResults.stream().allMatch(hasStatus(InstanceStatus.DELETED_ON_PROVIDER_SIDE, InstanceStatus.DELETED_BY_PROVIDER))) {
            return DetailedStackStatus.DELETED_ON_PROVIDER_SIDE;
        }
        if (alreadyDeletedCount > 0) {
            return DetailedStackStatus.UNHEALTHY;
        }
        if (providerSyncResults.stream().allMatch(hasStatus(InstanceStatus.STOPPED))) {
            return DetailedStackStatus.STOPPED;
        }
        if (providerSyncResults.stream().allMatch(hasStatus(InstanceStatus.UNKNOWN))) {
            if (isStale(stackStatus)) {
                return DetailedStackStatus.STALE;
            } else {
                return DetailedStackStatus.UNREACHABLE;
            }
        }
        if (providerSyncResults.stream().anyMatch(
                hasStatus(InstanceStatus.DELETED_ON_PROVIDER_SIDE, InstanceStatus.DELETED_BY_PROVIDER, InstanceStatus.STOPPED))) {
            return DetailedStackStatus.UNHEALTHY;
        }
        return result.getStatus();
    }

    private boolean isStale(StackStatus stackStatus) {
        if (DetailedStackStatus.UNREACHABLE == stackStatus.getDetailedStackStatus() && stackStatus.getCreated() != null) {
            long daysInMillis = TimeUnit.DAYS.toMillis(autoSyncConfig.getStaleAfterDays());
            return (System.currentTimeMillis() - stackStatus.getCreated()) > daysInMillis;
        } else if (DetailedStackStatus.STALE == stackStatus.getDetailedStackStatus()) {
            return true;
        }
        return false;
    }

    private Predicate<ProviderSyncResult> hasStatus(InstanceStatus... statuses) {
        return providerSyncResult -> Set.of(statuses).contains(providerSyncResult.getStatus());
    }

    private void setStatusIfNotTheSame(InstanceMetaData instanceMetaData, InstanceStatus newStatus) {
        InstanceStatus oldStatus = instanceMetaData.getInstanceStatus();
        if (oldStatus != newStatus) {
            if (autoSyncConfig.isUpdateStatus()) {
                instanceMetaData.setInstanceStatus(newStatus);
                LOGGER.info(AUTO_SYNC_LOG_PREFIX + "The instance status updated from {} to {}", oldStatus, newStatus);
            } else {
                LOGGER.info(AUTO_SYNC_LOG_PREFIX + "The instance status would be had to update from {} to {}",
                        oldStatus, newStatus);
            }
        }
    }

}
