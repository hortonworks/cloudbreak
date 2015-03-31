package com.sequenceiq.cloudbreak.core.flow.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.AbstractFlowHandler;
import com.sequenceiq.cloudbreak.core.flow.FlowHandler;
import com.sequenceiq.cloudbreak.core.flow.context.DefaultFlowContext;

import reactor.event.Event;

@Service
public class StackTerminationHandler extends AbstractFlowHandler<DefaultFlowContext> implements FlowHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackTerminationHandler.class);

    @Override
    protected Object execute(Event<DefaultFlowContext> event) throws CloudbreakException {
        LOGGER.info("execute() for phase: {}", event.getKey());
        DefaultFlowContext context = (DefaultFlowContext) getFlowFacade().terminateStack(event.getData());
        LOGGER.info("Cluster terminated. Context: {}", context);
        return context;
    }

    @Override
    protected Object handleErrorFlow(Throwable throwable, DefaultFlowContext data) throws Exception {
        LOGGER.info("execute() for phase: {}", getClass());
        data.setErrorReason(throwable.getMessage());
        return getFlowFacade().handleStackTerminationFailure(data);
    }
}
