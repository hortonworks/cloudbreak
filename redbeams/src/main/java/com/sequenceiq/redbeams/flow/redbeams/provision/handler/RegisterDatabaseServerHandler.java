package com.sequenceiq.redbeams.flow.redbeams.provision.handler;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.redbeams.flow.redbeams.provision.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.register.RegisterDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.register.RegisterDatabaseServerSuccess;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class RegisterDatabaseServerHandler implements EventHandler<RegisterDatabaseServerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterDatabaseServerHandler.class);

    @Inject
    private EventBus eventBus;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RegisterDatabaseServerRequest.class);
    }

    @Override
    public void accept(Event<RegisterDatabaseServerRequest> event) {
        RedbeamsEvent request = event.getData();
        Selectable response = new RegisterDatabaseServerSuccess(request.getResourceId());

        // TODO: Actually allocate databases
        LOGGER.info("A database would be registered here.");

        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
