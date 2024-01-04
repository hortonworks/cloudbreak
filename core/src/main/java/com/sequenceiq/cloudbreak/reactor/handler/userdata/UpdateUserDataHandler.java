package com.sequenceiq.cloudbreak.reactor.handler.userdata;

import static com.sequenceiq.cloudbreak.core.flow2.stack.update.userdata.UpdateUserDataEvents.UPDATE_USERDATA_FAILED_EVENT;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UserDataUpdateFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UserDataUpdateRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UserDataUpdateSuccess;
import com.sequenceiq.cloudbreak.service.image.userdata.UserDataService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

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
        UserDataUpdateRequest request = event.getData();
        try {
            LOGGER.info("Updating userData in the stack's current used image component...");
            if (request.getOldTunnel() != null) {
                LOGGER.info("Updating userdata for CCM upgrade");
                userDataService.updateJumpgateFlagOnly(request.getResourceId());
            }
            if (request.isModifyProxyConfig()) {
                LOGGER.info("Updating userdata for proxy config modification");
                userDataService.updateProxyConfig(request.getResourceId());
            }
            return new UserDataUpdateSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.error("Updating user data in the stack's image component has failed", e);
            return new UserDataUpdateFailed(UPDATE_USERDATA_FAILED_EVENT.event(), request.getResourceId(), e);
        }
    }
}
