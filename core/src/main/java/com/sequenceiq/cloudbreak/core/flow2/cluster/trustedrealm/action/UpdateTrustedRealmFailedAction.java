package com.sequenceiq.cloudbreak.core.flow2.cluster.trustedrealm.action;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.trustedrealm.UpdateTrustedRealmEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.trustedrealm.UpdateTrustedRealmState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.trustedrealm.UpdateTrustedRealmStatusService;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

@Component("UPDATE_TRUSTED_REALM_FAILED_STATE")
public class UpdateTrustedRealmFailedAction extends AbstractStackFailureAction<UpdateTrustedRealmState, UpdateTrustedRealmEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateTrustedRealmFailedAction.class);

    @Inject
    private UpdateTrustedRealmStatusService statusService;

    @Override
    protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
        LOGGER.error("Error during update trusted realm for stack {}", context.getStackId(), payload.getException());
        statusService.failed(context.getStackId(), payload.getException());
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(StackFailureContext context) {
        return new StackEvent(UpdateTrustedRealmEvent.UPDATE_TRUSTED_REALM_FAIL_HANDLED_EVENT.event(), context.getStackId());
    }
}

