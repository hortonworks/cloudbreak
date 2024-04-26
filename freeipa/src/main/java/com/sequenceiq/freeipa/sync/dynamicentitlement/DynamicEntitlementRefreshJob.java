package com.sequenceiq.freeipa.sync.dynamicentitlement;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DELETED_ON_PROVIDER_SIDE;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.STOPPED;

import java.util.EnumSet;
import java.util.Optional;

import jakarta.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.entity.Stack;
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
            LOGGER.debug("DynamicEntitlementSetter cannot run, because flow is running for freeipa stack: {}", stackId);
        } else if (!status.isUnschedulableState() && !EnumSet.of(STOPPED, DELETED_ON_PROVIDER_SIDE).contains(status) &&
                config.isDynamicEntitlementEnabled()) {
            LOGGER.debug("DynamicEntitlementSetter will apply watched entitlement changes for stack: {}", stackId);
            ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> dynamicEntitlementRefreshService.changeClusterConfigurationIfEntitlementsChanged(stack));
        } else if (status.isUnschedulableState()) {
            LOGGER.info("DynamicEntitlementSetter job will be unscheduled, stack state is {}", stack.getStackStatus().getStatus());
            jobService.unschedule(context.getJobDetail().getKey());
        }
    }

    private Long getStackId() {
        return Long.valueOf(getLocalId());
    }

}
