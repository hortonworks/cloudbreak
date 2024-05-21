package com.sequenceiq.freeipa.flow.stack.provision.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.provision.event.userdata.CreateUserDataFailed;
import com.sequenceiq.freeipa.flow.stack.provision.event.userdata.CreateUserDataRequest;
import com.sequenceiq.freeipa.flow.stack.provision.event.userdata.CreateUserDataSuccess;
import com.sequenceiq.freeipa.service.SecurityConfigService;
import com.sequenceiq.freeipa.service.image.userdata.UserDataService;

@Component
public class CreateUserDataHandler extends ExceptionCatcherEventHandler<CreateUserDataRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateUserDataHandler.class);

    @Inject
    private UserDataService userDataService;

    @Inject
    private SecurityConfigService securityConfigService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(CreateUserDataRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<CreateUserDataRequest> event) {
        LOGGER.error("Creating user data has failed with unexpected error", e);
        return new CreateUserDataFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<CreateUserDataRequest> event) {
        StackEvent request = event.getData();
        try {
            securityConfigService.createIfDoesntExists(request.getResourceId());
            userDataService.createUserData(request.getResourceId());
            return new CreateUserDataSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.error("Creating user data has failed", e);
            return new CreateUserDataFailed(request.getResourceId(), e);
        }
    }
}
