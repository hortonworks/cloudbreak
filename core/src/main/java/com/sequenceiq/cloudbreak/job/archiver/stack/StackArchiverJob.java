package com.sequenceiq.cloudbreak.job.archiver.stack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.MdcQuartzJob;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.db.LegacyStructuredEventDBService;
import com.sequenceiq.cloudbreak.structuredevent.service.db.CDPStructuredEventDBService;
import com.sequenceiq.cloudbreak.util.TimeUtil;

@Component
@DisallowConcurrentExecution
public class StackArchiverJob extends MdcQuartzJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackArchiverJob.class);

    @Inject
    private StackArchiverConfig stackArchiverConfig;

    @Inject
    private StackService stackService;

    @Inject
    private CDPStructuredEventDBService structuredEventService;

    @Inject
    private LegacyStructuredEventDBService legacyStructuredEventDBService;

    @Inject
    private TimeUtil timeUtil;

    @Inject
    private StackArchiverJobService stackArchiverJobService;

    @Override
    protected Optional<MdcContextInfoProvider> getMdcContextConfigProvider() {
        return Optional.empty();
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws RuntimeException {
        try {
            purgeFinalizedStacks(stackArchiverConfig.getRetentionPeriodInDays());
        } finally {
            LOGGER.debug("Stack archiver job execution finished, reschedule job.");
            stackArchiverJobService.reschedule();
        }
    }

    public void purgeFinalizedStacks(int retentionPeriodDays) {
        int limitForStack = stackArchiverConfig.getLimitForStack();
        LOGGER.debug("Cleaning finalised stacks, limit: {}, retention period days: {}", limitForStack, retentionPeriodDays);
        Map<String, String> failedDeletions = new HashMap<>();
        long timestampsBefore = timeUtil.getTimestampThatDaysBeforeNow(retentionPeriodDays);
        stackService.getAllForArchive(timestampsBefore, limitForStack).forEach(
                crn -> {
                    try {
                        purgeFinalizedStack(crn, failedDeletions);
                    } catch (Exception e) {
                        LOGGER.error("Could not completely delete stack events for {}.", crn, e);
                        failedDeletions.put(crn, e.getMessage());
                    }
                }
        );
        if (!failedDeletions.isEmpty()) {
            throw new RuntimeException(String.format("Failed to purge finalzied stacks: %s", failedDeletions));
        }
    }

    private void purgeFinalizedStack(String crn, Map<String, String> failedDeletions) {
        LOGGER.debug("Cleaning up stack object structuredEvents with crn {}.", crn);
        long start = System.currentTimeMillis();
        structuredEventService.deleteStructuredEventByResourceCrn(crn);
        LOGGER.debug("Cleaning up stack object legacy structuredEvents with crn {}.", crn);
        legacyStructuredEventDBService.deleteEntriesByResourceCrn(crn);
        LOGGER.debug("Cleaning up stack object with crn {} took {} ms.", crn, System.currentTimeMillis() - start);
        stackService.deleteArchivedByResourceCrn(crn);
        LOGGER.debug("Cleaning up stack finished with crn {}.", crn);
    }
}
