package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

@Component("StackTerminationFinishedAction")
public class StackTerminationFinishedAction extends AbstractStackTerminationAction<TerminateStackResult> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackTerminationFinishedAction.class);

    @Inject
    private StackTerminationService stackTerminationService;

    public StackTerminationFinishedAction() {
        super(TerminateStackResult.class);
    }

    @Override
    protected void doExecute(StackTerminationContext context, TerminateStackResult payload, Map<Object, Object> variables) {
        Boolean deleteDependencies = Boolean.valueOf(String.valueOf(variables.get("DELETEDEPENDENCIES")));
        stackTerminationService.finishStackTermination(context, payload, deleteDependencies == null ? false : deleteDependencies);
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(StackTerminationContext context) {
        return new StackEvent(StackTerminationEvent.TERMINATION_FINALIZED_EVENT.stringRepresentation(), context.getStack().getId());
    }
}
