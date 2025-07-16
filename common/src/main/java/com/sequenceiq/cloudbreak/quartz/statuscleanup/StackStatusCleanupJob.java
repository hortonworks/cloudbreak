package com.sequenceiq.cloudbreak.quartz.statuscleanup;

import java.util.Optional;

import jakarta.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.MdcQuartzJob;
import com.sequenceiq.cloudbreak.util.TimeUtil;

@Component
@DisallowConcurrentExecution
public class StackStatusCleanupJob extends MdcQuartzJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackStatusCleanupJob.class);

    @Inject
    private StackStatusCleanupConfig stackStatusCleanUpConfig;

    @Inject
    private Optional<StackStatusCleanupService> stackStatusCleanupService;

    @Inject
    private TimeUtil timeUtil;

    @Inject
    private StackStatusCleanupJobService stackStatusCleanUpJobService;

    @Override
    protected Optional<MdcContextInfoProvider> getMdcContextConfigProvider() {
        return Optional.empty();
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) {
        try {
            cleanupOldStackStatusEntries(stackStatusCleanUpConfig.getRetentionPeriodInDays());
        } finally {
            LOGGER.debug("Stack status cleanup job execution finished, reschedule job.");
            stackStatusCleanUpJobService.reschedule();
        }
    }

    public void cleanupOldStackStatusEntries(int retentionPeriodDays) {
        int limit = stackStatusCleanUpConfig.getLimit();
        LOGGER.debug("Cleaning old stack statuses, limit: {}, retention period days: {}", limit, retentionPeriodDays);
        long timestampBefore = timeUtil.getTimestampThatDaysBeforeNow(retentionPeriodDays);
        stackStatusCleanupService.ifPresent(statusCleanupService -> statusCleanupService.cleanupByTimestamp(limit, timestampBefore));
    }
}
