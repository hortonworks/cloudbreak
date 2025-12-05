package com.sequenceiq.cloudbreak.job.provider;

import java.util.Optional;

import jakarta.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.core.FlowLogService;

@DisallowConcurrentExecution
@Component
public class ProviderSyncJob extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderSyncJob.class);

    @Inject
    private StackDtoService stackService;

    @Inject
    private ProviderSyncService providerSyncService;

    @Inject
    private ProviderSyncJobService jobService;

    @Inject
    private ProviderSyncConfig config;

    @Inject
    private FlowLogService flowLogService;

    @Override
    protected void executeJob(JobExecutionContext context) {
        Long stackId = getStackId();
        StackDto stack = stackService.getById(getLocalIdAsLong());
        Status status = stack.getStatus();
        if (!config.isProviderSyncEnabled()) {
            LOGGER.info("ProviderSyncJob cannot run, because feature is disabled.");
        } else if (flowLogService.isOtherFlowRunning(stackId)) {
            LOGGER.info("ProviderSyncJob cannot run, because flow is running.");
        } else if (Status.getUnschedulableStatuses().contains(stack.getStatus())) {
            LOGGER.info("ProviderSyncJob job will be unscheduled, stack state is {}", status);
            jobService.deregister(context.getJobDetail().getKey());
        } else if (status.isAvailable()) {
            LOGGER.info("ProviderSyncJob will run...");
            providerSyncService.syncResources(stack);
        } else {
            LOGGER.info("ProviderSyncJob will not run, because stack status is {}.", status);
        }
    }

    @Override
    protected Optional<MdcContextInfoProvider> getMdcContextConfigProvider() {
        return Optional.ofNullable(stackService.getStackViewById(getLocalIdAsLong()));
    }

    private Long getStackId() {
        return Long.valueOf(getLocalId());
    }
}