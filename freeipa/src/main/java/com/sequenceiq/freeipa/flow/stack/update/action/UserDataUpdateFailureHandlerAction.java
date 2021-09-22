package com.sequenceiq.freeipa.flow.stack.update.action;

import static com.sequenceiq.freeipa.flow.stack.update.UpdateUserDataEvents.UPDATE_USERDATA_FAILURE_HANDLED_EVENT;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.flow.OperationAwareAction;
import com.sequenceiq.freeipa.flow.stack.AbstractStackFailureAction;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.StackFailureContext;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.image.change.ImageChangeState;
import com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents;
import com.sequenceiq.freeipa.service.operation.OperationService;

class UserDataUpdateFailureHandlerAction extends AbstractStackFailureAction<ImageChangeState, ImageChangeEvents> implements OperationAwareAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserDataUpdateFailureHandlerAction.class);

    @Inject
    private OperationService operationService;

    @Override
    protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) throws Exception {
        LOGGER.error("UserData change failed", payload.getException());
        if (isOperationIdSet(variables)) {
            operationService.failOperation(context.getStack().getAccountId(), getOperationId(variables), payload.getException().getMessage());
        }
        sendEvent(context, new StackEvent(UPDATE_USERDATA_FAILURE_HANDLED_EVENT.event(), context.getStack().getId()));
    }
}
