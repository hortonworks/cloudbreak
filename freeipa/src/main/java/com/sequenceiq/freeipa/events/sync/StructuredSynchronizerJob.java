package com.sequenceiq.freeipa.events.sync;

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
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.freeipa.CDPFreeipaStructuredSyncEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.CDPDefaultStructuredEventClient;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;

@DisallowConcurrentExecution
@Component
public class StructuredSynchronizerJob extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructuredSynchronizerJob.class);

    @Inject
    private StackService stackService;

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
        Long stackId = getLocalIdAsLong();
        try {
            Stack stack = stackService.getStackById(stackId);
            if (stack == null) {
                LOGGER.debug("Stack not found with id {}, StructuredSynchronizerJob will be unscheduled.", stackId);
                structuredSynchronizerJobService.unschedule(getLocalId());
            } else if (stack.getStackStatus() == null || stack.getStackStatus().getStatus() == null) {
                LOGGER.debug("Stack state is null for stack {}. The event won't be stored!", stackId);
            } else if (unschedulableStates().contains(stack.getStackStatus().getStatus())) {
                LOGGER.debug("StructuredSynchronizerJob will be unscheduled for stack {}, stack state is {}", stackId, stack.getStackStatus().getStatus());
                structuredSynchronizerJobService.unschedule(getLocalId());
            } else {
                LOGGER.debug("StructuredSynchronizerJob is running for stack: '{}'", stackId);
                CDPFreeipaStructuredSyncEvent cdpFreeipaStructuredSyncEvent = structuredSyncEventFactory.createCDPFreeipaStructuredSyncEvent(stackId);
                cdpDefaultStructuredEventClient.sendStructuredEvent(cdpFreeipaStructuredSyncEvent);
            }
        } catch (NotFoundException e) {
            LOGGER.debug("Stack not found with id {}, StructuredSynchronizerJob will be unscheduled.", stackId);
            structuredSynchronizerJobService.unschedule(getLocalId());
        } catch (Exception ex) {
            LOGGER.error("Error happened during CDPFreeipaStructuredSyncEvent generation! The event won't be stored!", ex);
        }
    }

    public static Set<Status> unschedulableStates() {
        return EnumSet.of(
                Status.DELETE_COMPLETED
        );
    }
}
