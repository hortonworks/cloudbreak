package com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.RotateSaltPasswordType;
import com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.event.RotateSaltPasswordFailureResponse;
import com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.event.RotateSaltPasswordRequest;
import com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.event.RotateSaltPasswordSuccessResponse;
import com.sequenceiq.freeipa.orchestrator.RotateSaltPasswordService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class RotateSaltPasswordHandler extends ExceptionCatcherEventHandler<RotateSaltPasswordRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RotateSaltPasswordHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private RotateSaltPasswordService rotateSaltPasswordService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RotateSaltPasswordRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RotateSaltPasswordRequest> event) {
        LOGGER.warn("Fallback to default failure event for exception", e);
        return new RotateSaltPasswordFailureResponse(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<RotateSaltPasswordRequest> event) {
        Long stackId = event.getData().getResourceId();
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        RotateSaltPasswordType rotateSaltPasswordType = event.getData().getType();
        LOGGER.info("Rotating salt password with type {}", rotateSaltPasswordType);
        try {
            switch (rotateSaltPasswordType) {
                case SALT_BOOTSTRAP_ENDPOINT:
                    rotateSaltPasswordService.rotateSaltPassword(stack);
                    break;
                case FALLBACK:
                    rotateSaltPasswordService.rotateSaltPasswordFallback(stack);
                    break;
                default:
                    throw new IllegalStateException(String.format("RotateSaltPasswordType %s is not handled", rotateSaltPasswordType));
            }
            return new RotateSaltPasswordSuccessResponse(stackId);
        } catch (Exception e) {
            LOGGER.warn("Failed to rotate salt password with type {}", rotateSaltPasswordType, e);
            return new RotateSaltPasswordFailureResponse(stackId, e);
        }
    }
}
