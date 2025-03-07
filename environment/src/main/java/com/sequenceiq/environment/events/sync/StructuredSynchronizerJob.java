package com.sequenceiq.environment.events.sync;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.CDPEnvironmentStructuredSyncEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.CDPDefaultStructuredEventClient;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.service.EnvironmentService;

@DisallowConcurrentExecution
@Component
public class StructuredSynchronizerJob extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructuredSynchronizerJob.class);

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private StructuredSynchronizerJobService structuredSynchronizerJobService;

    @Inject
    private StructuredSyncEventFactory structuredSyncEventFactory;

    @Inject
    private CDPDefaultStructuredEventClient cdpDefaultStructuredEventClient;

    @Override
    protected Optional<MdcContextInfoProvider> getMdcContextConfigProvider() {
        return Optional.empty();
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) {
        Long id = getLocalIdAsLong();
        try {
            Environment environment = environmentService.findEnvironmentByIdOrThrow(id);
            if (environment == null) {
                LOGGER.debug("Environment not found with id {}, StructuredSynchronizerJob will be unscheduled.", id);
                structuredSynchronizerJobService.unschedule(getLocalId());
            } else if (environment.getStatus() == null) {
                LOGGER.debug("Environment status is null for environment {}. The event won't be stored!", id);
            } else if (unschedulableStates().contains(environment.getStatus())) {
                LOGGER.debug("StructuredSynchronizerJob will be unscheduled for environment {}, environment status is {}", id, environment.getStatus());
                structuredSynchronizerJobService.unschedule(getLocalId());
            } else {
                LOGGER.debug("StructuredSynchronizerJob is running for environment: '{}'", id);
                CDPEnvironmentStructuredSyncEvent cdpEnvironmentStructuredSyncEvent = structuredSyncEventFactory.createCDPEnvironmentStructuredSyncEvent(id);
                cdpDefaultStructuredEventClient.sendStructuredEvent(cdpEnvironmentStructuredSyncEvent);
            }
        } catch (NotFoundException e) {
            LOGGER.debug("Environment not found with id {}, StructuredSynchronizerJob will be unscheduled.", id);
            structuredSynchronizerJobService.unschedule(getLocalId());
        } catch (Exception ex) {
            LOGGER.error("Error happened during CDPEnvironmentStructuredSyncEvent generation! The event won't be stored!", ex);
        }
    }

    public static Set<EnvironmentStatus> unschedulableStates() {
        return EnumSet.of(
                EnvironmentStatus.ARCHIVED
        );
    }
}
