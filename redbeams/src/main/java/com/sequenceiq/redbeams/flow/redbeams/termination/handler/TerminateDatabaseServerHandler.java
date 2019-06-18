package com.sequenceiq.redbeams.flow.redbeams.termination.handler;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.termination.event.terminate.TerminateDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.termination.event.terminate.TerminateDatabaseServerSuccess;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class TerminateDatabaseServerHandler implements EventHandler<TerminateDatabaseServerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TerminateDatabaseServerHandler.class);

    @Inject
    private EventBus eventBus;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(TerminateDatabaseServerRequest.class);
    }

    @Override
    public void accept(Event<TerminateDatabaseServerRequest> event) {
        RedbeamsEvent request = event.getData();
        Selectable response = new TerminateDatabaseServerSuccess(request.getResourceId());

        // TODO: Actually terminate database servers
        LOGGER.info("A database server would be terminated here.");

        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
