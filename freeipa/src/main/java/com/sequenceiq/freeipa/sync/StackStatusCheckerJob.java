package com.sequenceiq.freeipa.sync;

import static com.sequenceiq.cloudbreak.util.Benchmark.checkedMeasure;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.security.InternalCrnBuilder;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.logger.MdcContext;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;
import com.sequenceiq.statuschecker.job.StatusCheckerJob;

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
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private AutoSyncConfig autoSyncConfig;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        Long stackId = getStackId();
        Stack stack = stackService.getStackById(stackId);
        prepareMdcContextWithStack(stack);
        if (flowLogService.isOtherFlowRunning(stackId)) {
            LOGGER.debug("StackStatusCheckerJob cannot run, because flow is running for stack: {}", stackId);
            return;
        }
        syncAStack(stack);
        MDCBuilder.cleanupMdc();
    }

    private Long getStackId() {
        return Long.valueOf(getLocalId());
    }

    private void prepareMdcContextWithStack(Stack stack) {
        MdcContext.builder()
                .resourceCrn(stack.getResourceCrn())
                .resourceName(stack.getName())
                .resourceType("STACK")
                .environmentCrn(stack.getEnvironmentCrn())
                .buildMdc();
    }

    private void syncAStack(Stack stack) {
        try {
            checkedMeasure(() -> {
                Crn internalCrnForService = new InternalCrnBuilder(Crn.Service.IAM).getInternalCrnForService();
                ThreadBasedUserCrnProvider.doAs(internalCrnForService.toString(), () -> {
                    Set<InstanceMetaData> notTerminatedForStack = instanceMetaDataService.findNotTerminatedForStack(stack.getId());
                    Set<InstanceMetaData> checkableInstances = notTerminatedForStack.stream().filter(i -> !i.isDeletedOnProvider())
                            .collect(Collectors.toSet());

                    int alreadyDeletedCount = notTerminatedForStack.size() - checkableInstances.size();
                    if (alreadyDeletedCount > 0) {
                        LOGGER.info(":::Auto sync::: Count of already in deleted on provider side state: {}", alreadyDeletedCount);
                    }
                    if (!checkableInstances.isEmpty()) {
                        SyncResult syncResult = freeipaChecker.getStatus(stack, checkableInstances);
                        List<ProviderSyncResult> results = providerChecker.updateAndGetStatuses(stack, checkableInstances);
                        updateStackStatus(stack, syncResult, results);
                    }
                });
                return null;
            }, LOGGER, ":::Auto sync::: freeipa stack sync in {}ms");
        } catch (Exception e) {
            LOGGER.info(":::Auto sync::: Error occurred during freeipa sync: {}", e.getMessage(), e);
        }
    }

    private void updateStackStatus(Stack stack, SyncResult result, List<ProviderSyncResult> providerSyncResults) {
        DetailedStackStatus status = getStackStatus(providerSyncResults, result);
        if (status != stack.getStackStatus().getDetailedStackStatus()) {
            if (autoSyncConfig.isUpdateStatus()) {
                stackUpdater.updateStackStatus(stack, status, result.getMessage());
            } else {
                LOGGER.info(":::Auto sync::: The stack status would be had to update from {} to {}",
                        stack.getStackStatus().getDetailedStackStatus(), status);
            }
        }
    }

    private DetailedStackStatus getStackStatus(List<ProviderSyncResult> providerSyncResults, SyncResult result) {
        if (result.getStatus() == DetailedStackStatus.PROVISIONED) {
            return DetailedStackStatus.PROVISIONED;
        }
        if (providerSyncResults.stream().allMatch(hasStatus(InstanceStatus.STOPPED))) {
            return DetailedStackStatus.STOPPED;
        }
        if (providerSyncResults.stream().anyMatch(hasStatus(InstanceStatus.STOPPED))) {
            return DetailedStackStatus.PROVISIONED;
        }
        if (providerSyncResults.stream().allMatch(hasStatus(InstanceStatus.DELETED_ON_PROVIDER_SIDE, InstanceStatus.DELETED_BY_PROVIDER))) {
            return DetailedStackStatus.DELETED_ON_PROVIDER_SIDE;
        }
        if (providerSyncResults.stream().anyMatch(hasStatus(InstanceStatus.DELETED_ON_PROVIDER_SIDE, InstanceStatus.DELETED_BY_PROVIDER))) {
            return DetailedStackStatus.PROVISIONED;
        }
        return result.getStatus();
    }

    private Predicate<ProviderSyncResult> hasStatus(InstanceStatus... statuses) {
        return providerSyncResult -> Set.of(statuses).contains(providerSyncResult.getStatus());
    }

}
