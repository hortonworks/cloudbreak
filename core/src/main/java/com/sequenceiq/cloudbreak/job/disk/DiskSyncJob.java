package com.sequenceiq.cloudbreak.job.disk;

import java.util.Optional;

import jakarta.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskSyncMode;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.core.FlowLogService;

@DisallowConcurrentExecution
@Component
public class DiskSyncJob extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiskSyncJob.class);

    @Inject
    private StackDtoService stackService;

    @Inject
    private DiskSyncConfig config;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private DiskSyncJobService jobService;

    @Inject
    private DiskSyncService diskSyncService;

    @Override
    protected void executeJob(JobExecutionContext context) throws JobExecutionException {
        Long stackId = getStackId();
        if (!config.isDiskSyncEnabled()) {
            LOGGER.info("DiskSyncJob cannot run, because feature is disabled.");
            return;
        }
        StackDto stack = stackService.getById(getLocalIdAsLong());
        Status status = stack.getStatus();
        if (flowLogService.isOtherFlowRunning(stackId)) {
            LOGGER.info("DiskSyncJob cannot run, because flow is running.");
        } else if (Status.getUnschedulableStatuses().contains(stack.getStatus())) {
            LOGGER.info("DiskSyncJob job will be unscheduled, stack state is {}", status);
            jobService.deregister(context.getJobDetail().getKey());
        } else if (status.isAvailable()) {
            LOGGER.info("DiskSyncJob will run...");
            diskSyncService.syncResources(stack, DiskSyncMode.DRY_RUN);
        } else {
            LOGGER.info("DiskSyncJob will not run, because stack status is {}.", status);
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
