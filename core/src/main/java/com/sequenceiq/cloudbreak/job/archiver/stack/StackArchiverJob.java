package com.sequenceiq.cloudbreak.job.archiver.stack;

import java.util.Optional;

import jakarta.inject.Inject;

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

    @Override
    protected Optional<MdcContextInfoProvider> getMdcContextConfigProvider() {
        return Optional.empty();
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws RuntimeException {
        purgeFinalisedStacks(stackArchiverConfig.getRetentionPeriodInDays());
    }

    public void purgeFinalisedStacks(int retentionPeriodDays) {
        LOGGER.debug("Cleaning finalised stacks");
        long timestampsBefore = timeUtil.getTimestampThatDaysBeforeNow(retentionPeriodDays);
        stackService.getAllForArchive(timestampsBefore).stream().forEach(
                crn -> {
                    LOGGER.debug("Cleaning up stack object structuredEvents with crn {}.", crn);
                    Optional<Exception> exception = structuredEventService.deleteStructuredEventByResourceCrn(crn);
                    if (exception.isEmpty()) {
                        LOGGER.debug("Cleaning up stack object legacy structuredEvents with crn {}.", crn);
                        legacyStructuredEventDBService.deleteEntriesByResourceCrn(crn);
                        LOGGER.debug("Cleaning up stack object with crn {}.", crn);
                        stackService.deleteArchivedByResourceCrn(crn);
                    } else {
                        LOGGER.error("Could not completely delete stack events for {} with error {}", crn, exception.get());
                        throw new RuntimeException(
                                String.format("Failed to archive terminated stack for stack: %s, exception: %s",
                                        crn, exception.get().getMessage()));
                    }
                }
        );
    }
}
