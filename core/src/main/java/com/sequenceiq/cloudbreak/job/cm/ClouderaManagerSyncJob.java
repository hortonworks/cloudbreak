package com.sequenceiq.cloudbreak.job.cm;

import java.util.Optional;

import jakarta.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.template.ClusterManagerTemplateSyncService;
import com.sequenceiq.flow.core.FlowLogService;

@DisallowConcurrentExecution
@Component
public class ClouderaManagerSyncJob extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerSyncJob.class);

    @Inject
    private StackDtoService stackService;

    @Inject
    private ClouderaManagerSyncJobService jobService;

    @Inject
    private ClouderaManagerSyncConfig config;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private ClusterManagerTemplateSyncService clusterManagerTemplateSyncService;

    @Override
    protected void executeJob(JobExecutionContext context) {
        Long stackId = getStackId();
        StackDto stack = stackService.getById(getLocalIdAsLong());
        Status status = stack.getStatus();
        if (!config.isClouderaManagerSyncEnabled()) {
            LOGGER.info("CMSyncJob cannot run, because feature is disabled.");
        } else if (flowLogService.isOtherFlowRunning(stackId)) {
            LOGGER.info("CMSyncJob cannot run, because flow is running.");
        } else if (Status.getUnschedulableStatuses().contains(stack.getStatus())) {
            LOGGER.info("CMSyncJob job will be unscheduled, stack state is {}", status);
            jobService.deregister(context.getJobDetail().getKey());
        } else if (status.isAvailable()) {
            LOGGER.info("CMSyncJob will run...");
            ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> clusterManagerTemplateSyncService.sync(stack.getId())
            );
        } else {
            LOGGER.info("CMSyncJob will not run, because stack status is {}.", status);
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