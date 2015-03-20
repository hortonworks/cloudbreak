package com.sequenceiq.cloudbreak.core.flow.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.AbstractFlowHandler;
import com.sequenceiq.cloudbreak.core.flow.FlowHandler;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.TerminationContext;

import reactor.event.Event;

@Service
public class StackTerminationHandler extends AbstractFlowHandler<TerminationContext> implements FlowHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackTerminationHandler.class);

    @Override
    protected Object execute(Event<TerminationContext> event) throws CloudbreakException {
        LOGGER.info("execute() for phase: {}", event.getKey());
        TerminationContext context = (TerminationContext) getFlowFacade().terminateStack(event.getData());
        LOGGER.info("Cluster terminated. Context: {}", context);
        return context;
    }

    @Override
    protected void handleErrorFlow(Throwable throwable, Object data) {
        Event<TerminationContext> event = (Event<TerminationContext>) data;
        TerminationContext terminationContext = event.getData();
        LOGGER.info("execute() for phase: {}", event.getKey());
        try {
            FlowContext context = getFlowFacade().handleStackTerminationFailure(terminationContext);
            LOGGER.info("Stack termination failure is handled. Context: {}", context);
        } catch (CloudbreakException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    protected Object assemblePayload(Object serviceResult) {
        LOGGER.info("assemblePayload() for phase: {}", serviceResult);
        return serviceResult;
    }
}
