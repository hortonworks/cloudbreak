package com.sequenceiq.freeipa.flow.stack.update.handler;

import static com.sequenceiq.freeipa.flow.stack.update.UpdateUserDataEvents.UPDATE_USERDATA_FAILED_EVENT;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.update.event.UserDataUpdateFailed;
import com.sequenceiq.freeipa.flow.stack.update.event.UserDataUpdateRequest;
import com.sequenceiq.freeipa.flow.stack.update.event.UserDataUpdateSuccess;
import com.sequenceiq.freeipa.service.image.userdata.UserDataService;

import reactor.bus.Event;

@Component
public class UpdateUserDataHandler extends ExceptionCatcherEventHandler<UserDataUpdateRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateUserDataHandler.class);

    @Inject
    private UserDataService userDataService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UserDataUpdateRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UserDataUpdateRequest> event) {
        LOGGER.error("Updating user data in the stack's image entity has failed", e);
        return new UserDataUpdateFailed(UPDATE_USERDATA_FAILED_EVENT.event(), resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UserDataUpdateRequest> event) {
        StackEvent request = event.getData();
        try {
            LOGGER.info("Updating userData in the stack's current used image entity...");
            userDataService.createUserData(request.getResourceId());
            return new UserDataUpdateSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.error("Updating user data in the stack's image entity has failed", e);
            return new UserDataUpdateFailed(UPDATE_USERDATA_FAILED_EVENT.event(), request.getResourceId(), e);
        }
    }
}
