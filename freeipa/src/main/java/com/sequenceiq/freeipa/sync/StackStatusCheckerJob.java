package com.sequenceiq.freeipa.sync;

import static com.sequenceiq.cloudbreak.util.Benchmark.checkedMeasure;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

import io.opentracing.Tracer;

@DisallowConcurrentExecution
@Component
public class StackStatusCheckerJob extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackStatusCheckerJob.class);

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
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private FreeipaStatusInfoLogger freeipaStatusInfoLogger;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Value("${freeipa.autosync.update.status:true}")
    private boolean updateStatus;

    public StackStatusCheckerJob(Tracer tracer) {
        super(tracer, "Stack Status Checker Job");
    }

    @Override
    protected Object getMdcContextObject() {
        return stackService.getStackById(getStackId());
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        Long stackId = getStackId();
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        try {
            if (flowLogService.isOtherFlowRunning(stackId)) {
                LOGGER.debug("StackStatusCheckerJob cannot run, because flow is running for freeipa stack: {}", stackId);
            } else {
                LOGGER.debug("No flows running, trying to sync freeipa");
                syncAStack(stack, false);
            }
        } catch (InterruptSyncingException e) {
            LOGGER.info("Syncing was interrupted", e);
        }
    }

    private Long getStackId() {
        return Long.valueOf(getLocalId());
    }

    public void syncAStack(Stack stack, boolean updateStatusFromFlow) {
        try {
            checkedMeasure(() -> {
                ThreadBasedUserCrnProvider.doAsInternalActor(
                        regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                        () -> {
                    // Exclude terminated but include deleted
                    Set<InstanceMetaData> notTerminatedForStack = stack.getAllInstanceMetaDataList().stream()
                            .filter(Predicate.not(InstanceMetaData::isTerminated))
                            .collect(Collectors.toSet());
                    Set<InstanceMetaData> checkableInstances = notTerminatedForStack.stream()
                            .filter(Predicate.not(InstanceMetaData::isDeletedOnProvider))
                            .collect(Collectors.toSet());

                    int alreadyDeletedCount = notTerminatedForStack.size() - checkableInstances.size();
                    if (alreadyDeletedCount > 0) {
                        LOGGER.info(":::Auto sync::: Count of already in deleted on provider side state: {}", alreadyDeletedCount);
                    }
                    if (!checkableInstances.isEmpty()) {
                        SyncResult syncResult = freeipaChecker.getStatus(stack, checkableInstances);
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
                                LOGGER.debug("results is empty, skip update");
                            }
                        }
                    } else if (alreadyDeletedCount > 0) {
                        SyncResult syncResult =  new SyncResult("FreeIpa is " + DetailedStackStatus.DELETED_ON_PROVIDER_SIDE,
                                DetailedStackStatus.DELETED_ON_PROVIDER_SIDE, null);
                        updateStackStatus(stack, syncResult, null, alreadyDeletedCount, updateStatusFromFlow);
                    }
                    freeipaStatusInfoLogger.logFreeipaStatus(stack.getId(), checkableInstances);
                });
                return null;
            }, LOGGER, ":::Auto sync::: freeipa stack sync in {}ms");
        } catch (Exception e) {
            LOGGER.info(":::Auto sync::: Error occurred during freeipa sync: {}", e.getMessage(), e);
        }
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
                LOGGER.info(":::Auto sync::: the '{}' status is not converted", detailedStackStatus);
        }
    }

    private void updateStackStatus(Stack stack, SyncResult result, List<ProviderSyncResult> providerSyncResults, int alreadyDeletedCount,
            boolean updateStatusFromFlow) {
        DetailedStackStatus status = providerSyncResults == null ? result.getStatus() : getStackStatus(providerSyncResults, result, alreadyDeletedCount);
        if (status != stack.getStackStatus().getDetailedStackStatus()) {
            if (autoSyncConfig.isUpdateStatus()) {
                if (!updateStatusFromFlow && flowLogService.isOtherFlowRunning(stack.getId())) {
                    throw new InterruptSyncingException(":::Auto sync::: interrupt syncing in updateStackStatus, flow is running on freeipa stack " +
                            stack.getName());
                } else {
                    stackUpdater.updateStackStatus(stack, status, result.getMessage());
                }
            } else {
                LOGGER.info(":::Auto sync::: The stack status would be had to update from {} to {}",
                        stack.getStackStatus().getDetailedStackStatus(), status);
            }
        }
    }

    private DetailedStackStatus getStackStatus(List<ProviderSyncResult> providerSyncResults, SyncResult result, int alreadyDeletedCount) {
        if (providerSyncResults.stream().allMatch(hasStatus(InstanceStatus.DELETED_ON_PROVIDER_SIDE, InstanceStatus.DELETED_BY_PROVIDER))) {
            return DetailedStackStatus.DELETED_ON_PROVIDER_SIDE;
        }
        if (alreadyDeletedCount > 0) {
            return DetailedStackStatus.UNHEALTHY;
        }
        if (providerSyncResults.stream().allMatch(hasStatus(InstanceStatus.STOPPED))) {
            return DetailedStackStatus.STOPPED;
        }
        if (providerSyncResults.stream().anyMatch(
                hasStatus(InstanceStatus.DELETED_ON_PROVIDER_SIDE, InstanceStatus.DELETED_BY_PROVIDER, InstanceStatus.STOPPED))) {
            return DetailedStackStatus.UNHEALTHY;
        }
        return result.getStatus();
    }

    private Predicate<ProviderSyncResult> hasStatus(InstanceStatus... statuses) {
        return providerSyncResult -> Set.of(statuses).contains(providerSyncResult.getStatus());
    }

    private void setStatusIfNotTheSame(InstanceMetaData instanceMetaData, InstanceStatus newStatus) {
        InstanceStatus oldStatus = instanceMetaData.getInstanceStatus();
        if (oldStatus != newStatus) {
            if (updateStatus) {
                instanceMetaData.setInstanceStatus(newStatus);
                LOGGER.info(":::Auto sync::: The instance status updated from {} to {}", oldStatus, newStatus);
            } else {
                LOGGER.info(":::Auto sync::: The instance status would be had to update from {} to {}",
                        oldStatus, newStatus);
            }
        }
    }

}
