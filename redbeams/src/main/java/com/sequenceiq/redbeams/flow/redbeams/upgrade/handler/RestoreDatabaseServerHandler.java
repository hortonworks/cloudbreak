package com.sequenceiq.redbeams.flow.redbeams.upgrade.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.RedbeamsUpgradeFailedEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.RestoreDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.RestoreDatabaseServerSuccess;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@Component
public class RestoreDatabaseServerHandler extends ExceptionCatcherEventHandler<RestoreDatabaseServerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestoreDatabaseServerHandler.class);

    @Inject
    private DBStackService dbStackService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RestoreDatabaseServerRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RestoreDatabaseServerRequest> event) {
        RedbeamsUpgradeFailedEvent failure = new RedbeamsUpgradeFailedEvent(resourceId, e);
        LOGGER.warn("Error restoring the database server:", e);
        return failure;
    }

    @Override
    protected Selectable doAccept(HandlerEvent<RestoreDatabaseServerRequest> handlerEvent) {
        Event<RestoreDatabaseServerRequest> event = handlerEvent.getEvent();
        LOGGER.debug("Received event: {}", event);
        RestoreDatabaseServerRequest request = event.getData();
        DBStack dbStack = dbStackService.getById(request.getResourceId());
        Selectable response;
        try {
            // TODO add the restore code

            response = new RestoreDatabaseServerSuccess(request.getResourceId());
            LOGGER.debug("Successfully restored the database server {}", dbStack);
        } catch (Exception e) {
            response = new RedbeamsUpgradeFailedEvent(request.getResourceId(), e);
            LOGGER.warn("Error restoring the database server {}:", dbStack, e);
        }

        return response;
    }

}
