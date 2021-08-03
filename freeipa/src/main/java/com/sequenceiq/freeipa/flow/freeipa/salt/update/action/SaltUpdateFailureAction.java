package com.sequenceiq.freeipa.flow.freeipa.salt.update.action;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.OperationAwareAction;
import com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateEvent;
import com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateState;
import com.sequenceiq.freeipa.flow.stack.AbstractStackFailureAction;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.StackFailureContext;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

public class SaltUpdateFailureAction extends AbstractStackFailureAction<SaltUpdateState, SaltUpdateEvent> implements OperationAwareAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltUpdateFailureAction.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private OperationService operationService;

    @Override
    protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
        LOGGER.error("Salt state update failed", payload.getException());
        stackUpdater.updateStackStatus(payload.getResourceId(), DetailedStackStatus.SALT_STATE_UPDATE_FAILED,
                "Salt update failed with: " + payload.getException().getMessage());
        if (isOperationIdSet(variables)) {
            operationService.failOperation(context.getStack().getAccountId(), getOperationId(variables), payload.getException().getMessage());
        }
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(StackFailureContext context) {
        return new StackEvent(SaltUpdateEvent.SALT_UPDATE_FAILURE_HANDLED_EVENT.event(), context.getStack().getId());
    }
}
