package com.sequenceiq.freeipa.flow.stack.update.handler;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static com.sequenceiq.freeipa.flow.stack.update.UpdateUserDataEvents.UPDATE_USERDATA_FAILED_EVENT;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.stack.update.event.UserDataUpdateFailed;
import com.sequenceiq.freeipa.flow.stack.update.event.UserDataUpdateRequest;
import com.sequenceiq.freeipa.flow.stack.update.event.UserDataUpdateSuccess;
import com.sequenceiq.freeipa.service.image.userdata.UserDataService;

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
        return new UserDataUpdateFailed(UPDATE_USERDATA_FAILED_EVENT.event(), resourceId, e, ERROR);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UserDataUpdateRequest> event) {
        UserDataUpdateRequest request = event.getData();
        try {
            LOGGER.info("Updating userData in the stack's current used image entity...");
            if (request.getOldTunnel() != null) {
                LOGGER.info("Updating userdata for CCM upgrade");
                switch (request.getOldTunnel()) {
                    case CCM:
                        LOGGER.debug("Regenerating user data from request payload.");
                        userDataService.regenerateUserDataForCcmUpgrade(request.getResourceId());
                        break;
                    case CCMV2:
                        LOGGER.debug("Updating Jumpgate flag only.");
                        userDataService.updateJumpgateFlagOnly(request.getResourceId());
                        break;
                    default:
                        throw new IllegalStateException(String.format("Upgrade from %s is not implemented", request.getOldTunnel()));
                }
            }
            if (request.isModifyProxyConfig()) {
                LOGGER.info("Updating userdata for proxy modification");
                userDataService.updateProxyConfig(request.getResourceId());
            }
            return new UserDataUpdateSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.error("Updating user data in the stack's image entity has failed", e);
            return new UserDataUpdateFailed(UPDATE_USERDATA_FAILED_EVENT.event(), request.getResourceId(), e, ERROR);
        }
    }
}
