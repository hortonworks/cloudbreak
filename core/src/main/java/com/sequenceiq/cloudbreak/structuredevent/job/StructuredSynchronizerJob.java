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
import com.sequenceiq.cloudbreak.quartz.TracedQuartzJob;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.LegacyDefaultStructuredEventClient;
import com.sequenceiq.cloudbreak.structuredevent.StructuredSyncEventFactory;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;

import io.opentracing.Tracer;

@DisallowConcurrentExecution
@Component
public class StructuredSynchronizerJob extends TracedQuartzJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructuredSynchronizerJob.class);

    private String localId;

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
        return stackService.getById(getStackId());
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        try {
            Stack stack = stackService.get(getStackId());
            if (stack == null) {
                LOGGER.debug("Stack not found with id {}, StructuredSynchronizerJob will be unscheduled.", getStackId());
                syncJobService.unschedule(localId);
            } else if (stack.getStatus() == null) {
                LOGGER.debug("Stack state is null for stack {}. The event won't be stored!", getStackId());
            } else if (unshedulableStates().contains(stack.getStatus())) {
                LOGGER.debug("StructuredSynchronizerJob will be unscheduled for stack {}, stack state is {}", getStackId(), stack.getStatus());
                syncJobService.unschedule(localId);
            } else {
                LOGGER.debug("StructuredSynchronizerJob is running for stack: '{}'", getStackId());
                StructuredSyncEvent structuredEvent = structuredSyncEventFactory.createStructuredSyncEvent(getStackId());
                legacyDefaultStructuredEventClient.sendStructuredEvent(structuredEvent);
            }
        } catch (NotFoundException ex) {
            LOGGER.debug("Stack not found with id {}, StructuredSynchronizerJob will be unscheduled.", getStackId());
            syncJobService.unschedule(localId);
        } catch (Exception ex) {
            LOGGER.error("Error happened during StructuredSyncEvent generation! The event won't be stored!", ex);
        }
    }

    Set<Status> unshedulableStates() {
        return EnumSet.of(
                Status.DELETE_COMPLETED
        );
    }

    private Long getStackId() {
        return Long.valueOf(localId);
    }

    public String getLocalId() {
        return localId;
    }

    public void setLocalId(String localId) {
        this.localId = localId;
    }
}
