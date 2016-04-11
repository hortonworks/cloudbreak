package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult;
import com.sequenceiq.cloudbreak.core.flow2.SelectableEvent;

@Component("StackTerminationFailureAction")
public class StackTerminationFailureAction extends AbstractStackTerminationAction<TerminateStackResult> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackTerminationFailureAction.class);
    @Inject
    private StackTerminationService stackTerminationService;

    public StackTerminationFailureAction() {
        super(TerminateStackResult.class);
    }

    @Override
    protected void doExecute(StackTerminationContext context, TerminateStackResult payload, Map<Object, Object> variables) {
        stackTerminationService.handleStackTerminationError(context, payload, variables);
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(StackTerminationContext context) {
        return new SelectableEvent(StackTerminationEvent.STACK_TERMINATION_FAIL_HANDLED_EVENT.stringRepresentation());
    }
}