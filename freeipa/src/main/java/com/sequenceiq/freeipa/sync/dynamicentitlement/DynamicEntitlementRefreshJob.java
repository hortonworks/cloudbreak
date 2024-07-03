package com.sequenceiq.freeipa.sync.dynamicentitlement;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.image.ImageNotFoundException;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.stack.StackService;

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
    private ImageService imageService;

    @Override
    protected Optional<Object> getMdcContextObject() {
        return Optional.ofNullable(stackService.getStackById(getStackId()));
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) {
        Long stackId = getStackId();
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Status status = stack.getStackStatus().getStatus();
        if (flowLogService.isOtherFlowRunning(stackId)) {
            LOGGER.debug("DynamicEntitlementRefreshJob cannot run, because flow is running for FreeIPA.");
        } else if (status.isAvailable() && config.isDynamicEntitlementEnabled() && isRequiredPackagesInstalled(stack)) {
            LOGGER.debug("DynamicEntitlementRefreshJob will apply watched entitlement changes for FreeIPA");
            ThreadBasedUserCrnProvider.doAs(
                    internalCrnModifier.changeAccountIdInCrnString(regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                            stack.getAccountId()).toString(),
                    () -> dynamicEntitlementRefreshService.changeClusterConfigurationIfEntitlementsChanged(stack));
        } else if (status.isUnschedulableState()) {
            LOGGER.info("DynamicEntitlementRefreshJob job will be unscheduled for FreeIPA, stack state is {}", stack.getStackStatus().getStatus());
            jobService.unschedule(context.getJobDetail().getKey());
        } else {
            logDynamicEntitlementInfo(stack, status);
        }
    }

    private boolean isRequiredPackagesInstalled(Stack stack) {
        try {
            Image image = imageService.getImageForStack(stack);
            if (image != null) {
                Map<String, String> packageVersions = image.getPackageVersions();
                if (packageVersions != null) {
                    boolean requiredPackagesInstalled = packageVersions.containsKey(ImagePackageVersion.CDP_PROMETHEUS.getKey());
                    LOGGER.debug("DynamicEntitlementRefreshJob required packages installed {}", requiredPackagesInstalled);
                    return requiredPackagesInstalled;
                } else {
                    LOGGER.warn("PackageVersions is null in image {}", image.getUuid());
                }
            } else {
                LOGGER.warn("Image not found");
            }
        } catch (ImageNotFoundException e) {
            LOGGER.warn("Image not found: {}", e);
        }
        return false;
    }

    private void logDynamicEntitlementInfo(Stack stack, Status status) {
        Map<String, Boolean> changedEntitlements = new HashMap<>();
        if (config.isDynamicEntitlementEnabled()) {
            changedEntitlements = ThreadBasedUserCrnProvider.doAs(
                    internalCrnModifier.changeAccountIdInCrnString(regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                            stack.getAccountId()).toString(),
                    () -> dynamicEntitlementRefreshService.getChangedWatchedEntitlements(stack));
        }
        LOGGER.debug("DynamicEntitlementRefreshJob cannot run info: stack state is {} for stack {}," +
                        " is DynamicEntitlementRefreshJob enabled: {}, changedEntitlements: {}.",
                status, stack.getResourceCrn(), config.isDynamicEntitlementEnabled(), changedEntitlements);
    }

    private Long getStackId() {
        return Long.valueOf(getLocalId());
    }

}
