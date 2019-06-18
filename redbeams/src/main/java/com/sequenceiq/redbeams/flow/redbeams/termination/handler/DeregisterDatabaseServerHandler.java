package com.sequenceiq.redbeams.flow.redbeams.termination.handler;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.termination.event.deregister.DeregisterDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.termination.event.deregister.DeregisterDatabaseServerSuccess;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class DeregisterDatabaseServerHandler implements EventHandler<DeregisterDatabaseServerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeregisterDatabaseServerHandler.class);

    @Inject
    private EventBus eventBus;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DeregisterDatabaseServerRequest.class);
    }

    @Override
    public void accept(Event<DeregisterDatabaseServerRequest> event) {
        RedbeamsEvent request = event.getData();
        Selectable response = new DeregisterDatabaseServerSuccess(request.getResourceId());

        // TODO: Actually deregister database server
        LOGGER.info("A database server would be deregistered here.");

        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
