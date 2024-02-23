package com.sequenceiq.cloudbreak.job.dynamicentitlement;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETED_ON_PROVIDER_SIDE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOPPED;

import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.telemetry.DynamicEntitlementRefreshService;

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

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        StackDto stack = stackDtoService.getById(getLocalIdAsLong());
        if (!Sets.union(Status.getUnschedulableStatuses(), Set.of(STOPPED, DELETED_ON_PROVIDER_SIDE)).contains(stack.getStatus()) &&
                config.isDynamicEntitlementEnabled()) {
            dynamicEntitlementRefreshService.changeClusterConfigurationIfEntitlementsChanged(stack);
        } else if (Status.getUnschedulableStatuses().contains(stack.getStatus())) {
            LOGGER.info("DynamicEntitlementSetter job will be unscheduled, stack state is {}", stack.getStatus());
            jobService.unschedule(context.getJobDetail().getKey());
        }
    }

    private Long getStackId() {
        return Long.valueOf(getLocalId());
    }

    @Override
    protected Optional<MdcContextInfoProvider> getMdcContextConfigProvider() {
        return Optional.ofNullable(stackDtoService.getStackViewById(getLocalIdAsLong()));
    }
}
