package com.sequenceiq.redbeams.flow.redbeams.provision.handler;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate.AllocateDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate.AllocateDatabaseServerSuccess;
import com.sequenceiq.redbeams.flow.stack.RedbeamsEvent;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class AllocateDatabaseHandler implements EventHandler<AllocateDatabaseServerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllocateDatabaseHandler.class);

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

        // TODO: Actually allocate databases

        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
