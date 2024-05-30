package com.sequenceiq.cloudbreak.job.dynamicentitlement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.auth.security.internal.InternalCrnModifier;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.exception.FlowsAlreadyRunningException;
import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.telemetry.DynamicEntitlementRefreshService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@DisallowConcurrentExecution
@Component
public class DynamicEntitlementRefreshJob extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicEntitlementRefreshJob.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private DynamicEntitlementRefreshService dynamicEntitlementRefreshService;

    @Inject
    private DynamicEntitlementRefreshJobService jobService;

    @Inject
    private DynamicEntitlementRefreshConfig config;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private InternalCrnModifier internalCrnModifier;

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        StackDto stack = stackDtoService.getById(getLocalIdAsLong());
        if (stack.getStatus().isAvailable() && anyInstanceStopped(stack)) {
            LOGGER.info("DynamicEntitlementRefreshJob cannot run because there are stopped instances in the stack {}.", stack.getResourceCrn());
            logDynamicEntitlementInfo(stack);
        } else if (stack.getStatus().isAvailable() && config.isDynamicEntitlementEnabled()) {
            LOGGER.debug("DynamicEntitlementRefreshJob will apply watched entitlement changes for stack: {}.", stack.getResourceCrn());
            try {
                ThreadBasedUserCrnProvider.doAs(
                        internalCrnModifier.changeAccountIdInCrnString(regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                                stack.getAccountId()).toString(),
                        () -> dynamicEntitlementRefreshService.changeClusterConfigurationIfEntitlementsChanged(stack));
            } catch (FlowsAlreadyRunningException e) {
                LOGGER.info("DynamicEntitlementRefreshJob cannot run because another flow is running: {}", e.getMessage());
            }
        } else if (Status.getUnschedulableStatuses().contains(stack.getStatus())) {
            LOGGER.info("DynamicEntitlementRefreshJob will be unscheduled, stack state is {} for stack {}.", stack.getStatus(), stack.getResourceCrn());
            jobService.unschedule(context.getJobDetail().getKey());
        } else {
            LOGGER.debug("DynamicEntitlementRefreshJob cannot run because of stack state.");
            logDynamicEntitlementInfo(stack);
        }
    }

    private void logDynamicEntitlementInfo(StackDto stack) {
        Map<String, Boolean> changedEntitlements = new HashMap<>();
        if (config.isDynamicEntitlementEnabled()) {
            changedEntitlements = ThreadBasedUserCrnProvider.doAs(
                    internalCrnModifier.changeAccountIdInCrnString(regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                            stack.getAccountId()).toString(),
                    () -> dynamicEntitlementRefreshService.getChangedWatchedEntitlements(stack));
        }
        LOGGER.info("DynamicEntitlementRefreshJob cannot run info: stack state is {} for stack {}," +
                        " is DynamicEntitlementRefreshJob enabled: {}, changedEntitlements: {}.",
                stack.getStatus(), stack.getResourceCrn(), config.isDynamicEntitlementEnabled(), changedEntitlements);
    }

    private boolean anyInstanceStopped(StackDto stack) {
        return stack.getInstanceGroupDtos().stream()
                .map(InstanceGroupDto::getInstanceMetadataViews)
                .flatMap(List::stream)
                .map(InstanceMetadataView::getInstanceStatus)
                .anyMatch(instanceStatus -> InstanceStatus.STOPPED.equals(instanceStatus));
    }

    private Long getStackId() {
        return Long.valueOf(getLocalId());
    }

    @Override
    protected Optional<MdcContextInfoProvider> getMdcContextConfigProvider() {
        return Optional.ofNullable(stackDtoService.getStackViewById(getLocalIdAsLong()));
    }
}
