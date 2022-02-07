package com.sequenceiq.cloudbreak.structuredevent.job;

import java.util.EnumSet;
import java.util.Set;

import javax.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.LegacyDefaultStructuredEventClient;
import com.sequenceiq.cloudbreak.structuredevent.StructuredSyncEventFactory;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;

import io.opentracing.Tracer;

@DisallowConcurrentExecution
@Component
public class StructuredSynchronizerJob extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructuredSynchronizerJob.class);

    @Inject
    private StackService stackService;

    @Inject
    private StructuredSynchronizerJobService syncJobService;

    @Inject
    private StructuredSyncEventFactory structuredSyncEventFactory;

    @Inject
    private LegacyDefaultStructuredEventClient legacyDefaultStructuredEventClient;

    public StructuredSynchronizerJob(Tracer tracer) {
        super(tracer, "Structured Synchronizer Job");
    }

    @Override
    protected Object getMdcContextObject() {
        return stackService.getById(getLocalIdAsLong());
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        Long stackId = getLocalIdAsLong();
        try {
            Stack stack = stackService.get(stackId);
            if (stack == null) {
                LOGGER.debug("Stack not found with id {}, StructuredSynchronizerJob will be unscheduled.", stackId);
                syncJobService.unschedule(getLocalId());
            } else if (stack.getStatus() == null) {
                LOGGER.debug("Stack state is null for stack {}. The event won't be stored!", stackId);
            } else if (unschedulableStates().contains(stack.getStatus())) {
                LOGGER.debug("StructuredSynchronizerJob will be unscheduled for stack {}, stack state is {}", stackId, stack.getStatus());
                syncJobService.unschedule(getLocalId());
            } else {
                LOGGER.debug("StructuredSynchronizerJob is running for stack: '{}'", stackId);
                StructuredSyncEvent structuredEvent = structuredSyncEventFactory.createStructuredSyncEvent(stackId);
                legacyDefaultStructuredEventClient.sendStructuredEvent(structuredEvent);
            }
        } catch (NotFoundException ex) {
            LOGGER.debug("Stack not found with id {}, StructuredSynchronizerJob will be unscheduled.", stackId);
            syncJobService.unschedule(getLocalId());
        } catch (Exception ex) {
            LOGGER.error("Error happened during StructuredSyncEvent generation! The event won't be stored!", ex);
        }
    }

    Set<Status> unschedulableStates() {
        return EnumSet.of(
                Status.DELETE_COMPLETED
        );
    }
}
