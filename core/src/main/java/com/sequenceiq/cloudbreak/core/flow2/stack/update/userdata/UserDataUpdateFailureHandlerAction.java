package com.sequenceiq.cloudbreak.core.flow2.stack.update.userdata;

import static com.sequenceiq.cloudbreak.core.flow2.stack.update.userdata.UpdateUserDataEvents.UPDATE_USERDATA_FAILURE_HANDLED_EVENT;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

class UserDataUpdateFailureHandlerAction extends AbstractStackFailureAction<UpdateUserDataState, UpdateUserDataEvents> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserDataUpdateFailureHandlerAction.class);

    @Override
    protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) throws Exception {
        LOGGER.error("UserData change failed", payload.getException());
        sendEvent(context, new StackEvent(UPDATE_USERDATA_FAILURE_HANDLED_EVENT.event(), context.getStackView().getId()));
    }
}
