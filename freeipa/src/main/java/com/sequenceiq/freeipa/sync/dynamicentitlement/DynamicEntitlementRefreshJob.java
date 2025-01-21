package com.sequenceiq.freeipa.sync.dynamicentitlement;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.auth.security.internal.InternalCrnModifier;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.AvailabilityChecker;

@DisallowConcurrentExecution
@Component
public class DynamicEntitlementRefreshJob extends StatusCheckerJob {

    static final String OPERATION_ID = "operationId";

    static final String ERROR_COUNT = "errorCount";

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicEntitlementRefreshJob.class);

    private static final int ATTEMPT_NUMBER = 100;

    private static final int SLEEP_TIME = 10;

    @Inject
    private StackService stackService;

    @Inject
    private DynamicEntitlementRefreshService dynamicEntitlementRefreshService;

    @Inject
    private DynamicEntitlementRefreshJobService jobService;

    @Inject
    private DynamicEntitlementRefreshConfig config;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private InternalCrnModifier internalCrnModifier;

    @Inject
    private AvailabilityChecker availabilityChecker;

    @Override
    protected Optional<Object> getMdcContextObject() {
        return Optional.ofNullable(stackService.getStackById(getStackId()));
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) {
        Long stackId = getStackId();
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Status status = stack.getStackStatus().getStatus();
        if (!config.isDynamicEntitlementEnabled()) {
            LOGGER.info("DynamicEntitlementRefreshJob cannot run, because feature is disabled.");
        } else if (flowLogService.isOtherFlowRunning(stackId)) {
            LOGGER.info("DynamicEntitlementRefreshJob cannot run, because flow is running for FreeIPA.");
        } else if (status.isUnschedulableState()) {
            LOGGER.info("DynamicEntitlementRefreshJob job will be unscheduled for FreeIPA, stack state is {}", status);
            jobService.unschedule(context.getJobDetail().getKey());
        } else if (!availabilityChecker.isRequiredPackagesInstalled(stack, Set.of(ImagePackageVersion.CDP_PROMETHEUS.getKey()))) {
            LOGGER.info("DynamicEntitlementRefreshJob cannot run, because required package (cdp-prometheus) is missing, probably its an old image.");
        } else if (!status.isAvailable()) {
            LOGGER.info("DynamicEntitlementRefreshJob store new watched entitlements without triggering related flow, because status is {}", status);
            ThreadBasedUserCrnProvider.doAs(
                    internalCrnModifier.changeAccountIdInCrnString(regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                            stack.getAccountId()).toString(),
                    () -> dynamicEntitlementRefreshService.getChangedWatchedEntitlementsAndStoreNewFromUms(stack));
        } else {
            LOGGER.info("DynamicEntitlementRefreshJob will apply watched entitlement changes for FreeIPA");
            String operationId = ThreadBasedUserCrnProvider.doAs(
                    internalCrnModifier.changeAccountIdInCrnString(regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                            stack.getAccountId()).toString(),
                    () -> dynamicEntitlementRefreshService.changeClusterConfigurationIfEntitlementsChanged(stack));
            rescheduleIfPreviousFlowChainFailed(stack, context.getJobDetail(), operationId);
        }
    }

    private void rescheduleIfPreviousFlowChainFailed(Stack stack, JobDetail jobDetail, String operationId) {
        if (operationId != null) {
            int errorCountFromJob = getErrorCountFromJob(jobDetail);
            String operationIdFromJob = jobDetail.getJobDataMap().getString(OPERATION_ID);
            boolean previousOperationFailed = dynamicEntitlementRefreshService.previousOperationFailed(stack, operationIdFromJob);
            int errorCount = calculateNewErrorCount(errorCountFromJob, previousOperationFailed);
            addNewParametersToJobDetail(jobDetail, operationId, errorCount);
            jobService.reScheduleWithBackoff(stack.getId(), jobDetail, errorCount);
        }
    }

    private int calculateNewErrorCount(int errorCountFromJob, boolean previousOperationFailed) {
        if (previousOperationFailed) {
            return errorCountFromJob + 1;
        }
        return 0;
    }

    private void addNewParametersToJobDetail(JobDetail jobDetail, String operationId, int errorCount) {
        jobDetail.getJobDataMap().put(OPERATION_ID, operationId);
        jobDetail.getJobDataMap().putAsString(ERROR_COUNT, errorCount);
    }

    private int getErrorCountFromJob(JobDetail jobDetail) {
        int result = 0;
        String errorCount = jobDetail.getJobDataMap().getString(ERROR_COUNT);
        if (errorCount != null) {
            try {
                result = Integer.parseInt(errorCount);
            } catch (NumberFormatException e) {
                result = 0;
            }
        }
        return result;
    }

    private void logDynamicEntitlementInfo(Stack stack, Status status) {
        Map<String, Boolean> changedEntitlements = new HashMap<>();
        if (config.isDynamicEntitlementEnabled()) {
            changedEntitlements = ThreadBasedUserCrnProvider.doAs(
                    internalCrnModifier.changeAccountIdInCrnString(regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                            stack.getAccountId()).toString(),
                    () -> dynamicEntitlementRefreshService.getChangedWatchedEntitlementsAndStoreNewFromUms(stack));
        }
        LOGGER.debug("DynamicEntitlementRefreshJob cannot run info: stack state is {} for stack {}," +
                        " is DynamicEntitlementRefreshJob enabled: {}, changedEntitlements: {}.",
                status, stack.getResourceCrn(), config.isDynamicEntitlementEnabled(), changedEntitlements);
    }

    private Long getStackId() {
        return Long.valueOf(getLocalId());
    }

}
