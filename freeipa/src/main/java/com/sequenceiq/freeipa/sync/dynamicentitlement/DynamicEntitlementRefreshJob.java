package com.sequenceiq.freeipa.sync.dynamicentitlement;

import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
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
            ThreadBasedUserCrnProvider.doAs(
                    internalCrnModifier.changeAccountIdInCrnString(regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                            stack.getAccountId()).toString(),
                    () -> dynamicEntitlementRefreshService.changeClusterConfigurationIfEntitlementsChanged(stack));
        }
    }

    private Long getStackId() {
        return Long.valueOf(getLocalId());
    }

}
