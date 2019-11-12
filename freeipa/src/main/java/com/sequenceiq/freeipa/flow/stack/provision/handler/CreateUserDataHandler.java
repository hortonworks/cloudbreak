package com.sequenceiq.freeipa.flow.stack.provision.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.provision.event.userdata.CreateUserDataFailed;
import com.sequenceiq.freeipa.flow.stack.provision.event.userdata.CreateUserDataRequest;
import com.sequenceiq.freeipa.flow.stack.provision.event.userdata.CreateUserDataSuccess;
import com.sequenceiq.freeipa.service.image.userdata.UserDataService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class CreateUserDataHandler implements EventHandler<CreateUserDataRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateUserDataHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private UserDataService userDataService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(CreateUserDataRequest.class);
    }

    @Override
    public void accept(Event<CreateUserDataRequest> event) {
        StackEvent request = event.getData();
        Selectable response;
        try {
            userDataService.createUserData(request.getResourceId());
            response = new CreateUserDataSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.error("Creating user data has failed", e);
            response = new CreateUserDataFailed(request.getResourceId(), e);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
