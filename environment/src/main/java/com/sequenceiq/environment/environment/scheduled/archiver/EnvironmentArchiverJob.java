package com.sequenceiq.environment.environment.scheduled.archiver;

import java.util.Optional;

import jakarta.inject.Inject;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.MdcQuartzJob;
import com.sequenceiq.cloudbreak.structuredevent.service.db.CDPStructuredEventDBService;
import com.sequenceiq.cloudbreak.util.TimeUtil;
import com.sequenceiq.environment.environment.service.EnvironmentService;

@Component
public class EnvironmentArchiverJob extends MdcQuartzJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentArchiverJob.class);

    @Inject
    private EnvironmentArchiverConfig environmentArchiverConfig;

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private CDPStructuredEventDBService structuredEventService;

    @Inject
    private TimeUtil timeUtil;

    @Override
    protected Optional<MdcContextInfoProvider> getMdcContextConfigProvider() {
        return Optional.empty();
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        try {
            purgeFinalisedEnvironments(environmentArchiverConfig.getRetentionPeriodInDays());
        } catch (TransactionService.TransactionExecutionException e) {
            LOGGER.error("Transaction failed for environment cleanup.", e);
            throw new JobExecutionException(e);
        }
    }

    public void purgeFinalisedEnvironments(int retentionPeriodDays) throws TransactionService.TransactionExecutionException {
        LOGGER.debug("Cleaning finalised environments");
        long timestampsBefore = timeUtil.getTimestampThatDaysBeforeNow(retentionPeriodDays);
        environmentService.getAllForArchive(timestampsBefore).stream().forEach(
                crn -> {
                    Optional<Exception> exception = structuredEventService.deleteStructuredEventByResourceCrn(crn);
                    if (exception.isEmpty()) {
                        environmentService.deleteByResourceCrn(crn);
                    } else {
                        LOGGER.error("Could not completely delete environment events for {} with error {}", crn, exception.get());
                    }
                }
        );
    }
}
