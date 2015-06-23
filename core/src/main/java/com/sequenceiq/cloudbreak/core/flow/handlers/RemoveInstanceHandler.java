package com.sequenceiq.cloudbreak.core.flow.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.AbstractFlowHandler;
import com.sequenceiq.cloudbreak.core.flow.FlowHandler;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackInstanceUpdateContext;

import reactor.bus.Event;

@Component
public class RemoveInstanceHandler extends AbstractFlowHandler<StackInstanceUpdateContext> implements FlowHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoveInstanceHandler.class);

    @Override
    protected Object execute(Event<StackInstanceUpdateContext> event) throws CloudbreakException {
        LOGGER.info("execute() for phase: {}", event.getKey());
        FlowContext context = getFlowFacade().removeInstance(event.getData());
        LOGGER.info("Instance was removed from stack. Context: {}", context);
        return context;
    }

    @Override
    protected Object handleErrorFlow(Throwable throwable, StackInstanceUpdateContext data) throws Exception {
        LOGGER.info("handleErrorFlow() for phase: {}", getClass());
        data.setErrorReason(throwable.getMessage());
        return getFlowFacade().handleStackScalingFailure(data);
    }
}
