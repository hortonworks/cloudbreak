package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.CreateUserDataFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.CreateUserDataRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.CreateUserDataSuccess;
import com.sequenceiq.cloudbreak.service.idbroker.IdBrokerService;
import com.sequenceiq.cloudbreak.service.image.userdata.UserDataService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

@Component
public class CreateUserDataHandler implements EventHandler<CreateUserDataRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateUserDataHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private UserDataService userDataService;

    @Inject
    private IdBrokerService idBrokerService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(CreateUserDataRequest.class);
    }

    @Override
    public void accept(Event<CreateUserDataRequest> event) {
        StackEvent request = event.getData();
        Selectable response;
        try {
            idBrokerService.generateIdBrokerSignKey(request.getResourceId());
            userDataService.createUserData(request.getResourceId());
            response = new CreateUserDataSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.error("Creating user data has failed", e);
            response = new CreateUserDataFailed(request.getResourceId(), e);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
