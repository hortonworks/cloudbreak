package com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.DiskSyncEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.request.DiskSyncFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.request.DiskSyncHandlerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.request.DiskSyncProcessFinishedEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.job.disk.DiskSyncService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class DiskSyncHandler extends ExceptionCatcherEventHandler<DiskSyncHandlerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiskSyncHandler.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private DiskSyncService diskSyncService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DiskSyncHandlerEvent.class);
    }

    @Override
    public Selectable doAccept(HandlerEvent<DiskSyncHandlerEvent> handlerEvent) {
        DiskSyncHandlerEvent payload = handlerEvent.getData();
        Long stackId = payload.getResourceId();
        LOGGER.debug("Starting disk metadata sync for stack {}", stackId);
        try {
            StackDto stackDto = stackDtoService.getById(stackId);
            diskSyncService.syncResources(stackDto, payload.getDiskSyncMode());
            LOGGER.info("Successfully synchronized disk metadata for stack {}", stackId);
            return new DiskSyncProcessFinishedEvent(stackId);
        } catch (Exception e) {
            LOGGER.warn("Failed to synchronize disk metadata for stack {}", stackId, e);
            return new DiskSyncFailedEvent(DiskSyncEvent.FAILURE_EVENT.event(), stackId, e);
        }
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DiskSyncHandlerEvent> event) {
        return new DiskSyncFailedEvent(DiskSyncEvent.FAILURE_EVENT.event(), resourceId, e);
    }
}
