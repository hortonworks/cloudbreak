package com.sequenceiq.redbeams.flow.redbeams.provision.handler;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate.AllocateDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate.AllocateDatabaseServerSuccess;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class AllocateDatabaseServerHandler implements EventHandler<AllocateDatabaseServerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllocateDatabaseServerHandler.class);

    @Inject
    private EventBus eventBus;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(AllocateDatabaseServerRequest.class);
    }

    @Override
    public void accept(Event<AllocateDatabaseServerRequest> event) {
        RedbeamsEvent request = event.getData();
        Selectable response = new AllocateDatabaseServerSuccess(request.getResourceId());

        // TODO: Actually allocate database servers
        LOGGER.info("A database server would be allocated here.");

        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
