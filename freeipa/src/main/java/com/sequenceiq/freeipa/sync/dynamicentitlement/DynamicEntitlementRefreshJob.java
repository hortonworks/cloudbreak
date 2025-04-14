package com.sequenceiq.freeipa.sync.dynamicentitlement;

import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.AvailabilityChecker;

@DisallowConcurrentExecution
@Component
public class DynamicEntitlementRefreshJob extends StatusCheckerJob {

    static final String FLOW_CHAIN_ID = "flowChainId";

    static final String ERROR_COUNT = "errorCount";

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicEntitlementRefreshJob.class);

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
    private AvailabilityChecker availabilityChecker;

    @Override
    protected Optional<Object> getMdcContextObject() {
        return Optional.ofNullable(stackService.getStackById(getStackId()));
    }

    @Override
    protected void executeJob(JobExecutionContext context) {
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
            dynamicEntitlementRefreshService.getChangedWatchedEntitlementsAndStoreNewFromUms(stack);
        } else {
            LOGGER.info("DynamicEntitlementRefreshJob will apply watched entitlement changes for FreeIPA");
            FlowIdentifier flowIdentifier = dynamicEntitlementRefreshService.changeClusterConfigurationIfEntitlementsChanged(stack);
            if (flowIdentifier != null && flowIdentifier.getPollableId() != null) {
                rescheduleIfPreviousFlowChainFailed(stack, context.getJobDetail(), flowIdentifier.getPollableId());
            }
        }
    }

    private void rescheduleIfPreviousFlowChainFailed(Stack stack, JobDetail jobDetail, String flowChainId) {
        if (flowChainId != null) {
            int errorCountFromJob = getErrorCountFromJob(jobDetail);
            String flowChainIdFromJob = jobDetail.getJobDataMap().getString(FLOW_CHAIN_ID);
            boolean previousFlowFailed = dynamicEntitlementRefreshService.previousFlowFailed(stack, flowChainIdFromJob);
            int errorCount = calculateNewErrorCount(errorCountFromJob, previousFlowFailed);
            addNewParametersToJobDetail(jobDetail, flowChainId, errorCount);
            jobService.reScheduleWithBackoff(stack.getId(), jobDetail, errorCount);
        }
    }

    private int calculateNewErrorCount(int errorCountFromJob, boolean previousFlowFailed) {
        if (previousFlowFailed) {
            return errorCountFromJob + 1;
        }
        return 0;
    }

    private void addNewParametersToJobDetail(JobDetail jobDetail, String flowChainId, int errorCount) {
        jobDetail.getJobDataMap().put(FLOW_CHAIN_ID, flowChainId);
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

    private Long getStackId() {
        return Long.valueOf(getLocalId());
    }

}