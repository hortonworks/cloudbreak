package com.sequenceiq.cloudbreak.job.dynamicentitlement;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.WORKLOAD;

import java.util.List;
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
import com.sequenceiq.cloudbreak.util.StackUtil;
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
    private StackUtil stackUtil;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private InternalCrnModifier internalCrnModifier;

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        StackDto stack = stackDtoService.getById(getLocalIdAsLong());
        if (stackUtil.stopStartScalingEntitlementEnabled(stack.getStack()) && WORKLOAD.equals(stack.getType())) {
            LOGGER.info("DynamicEntitlementRefreshJob cannot run because stopstartscaling is enabled for stack {}.", stack.getResourceCrn());
            jobService.unschedule(context.getJobDetail().getKey());
        } else if (stack.getStatus().isAvailable() && anyInstanceStopped(stack)) {
            LOGGER.info("DynamicEntitlementRefreshJob cannot run because there are stopped instances in the stack {}.", stack.getResourceCrn());
        } else if (stack.getStatus().isAvailable() && config.isDynamicEntitlementEnabled()) {
            LOGGER.debug("DynamicEntitlementRefreshJob will apply watched entitlement changes for stack: {}.", stack.getResourceCrn());
            try {
                ThreadBasedUserCrnProvider.doAs(
                        internalCrnModifier.changeAccountIdInCrnString(regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                                stack.getAccountId()).toString(),
                        () -> dynamicEntitlementRefreshService.changeClusterConfigurationIfEntitlementsChanged(stack));
            } catch (FlowsAlreadyRunningException e) {
                LOGGER.info("DynamicEntitlementRefreshJob failed to run because another flow is running: {}", e.getMessage());
            }
        } else if (Status.getUnschedulableStatuses().contains(stack.getStatus())) {
            LOGGER.info("DynamicEntitlementRefreshJob will be unscheduled, stack state is {} for stack {}.", stack.getStatus(), stack.getResourceCrn());
            jobService.unschedule(context.getJobDetail().getKey());
        } else {
            LOGGER.info("DynamicEntitlementRefreshJob cannot run, stack state is {} for stack {}, is DynamicEntitlementRefreshJob enabled: {}.",
                    stack.getStatus(), stack.getResourceCrn(), config.isDynamicEntitlementEnabled());
        }
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
