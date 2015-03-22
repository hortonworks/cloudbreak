package com.sequenceiq.cloudbreak.core.flow.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.AbstractFlowHandler;
import com.sequenceiq.cloudbreak.core.flow.FlowHandler;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackScalingContext;

import reactor.event.Event;

@Component
public class StackDownscaleHandler extends AbstractFlowHandler<StackScalingContext> implements FlowHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackDownscaleHandler.class);

    @Override
    protected Object execute(Event<StackScalingContext> event) throws CloudbreakException {
        LOGGER.info("execute() for phase: {}", event.getKey());
        FlowContext context = getFlowFacade().downscaleStack(event.getData());
        LOGGER.info("Upscale of stack is finished. Context: {}", context);
        return context;
    }

    @Override
    protected Object handleErrorFlow(Throwable throwable, Object data) {
        Event<StackScalingContext> event = (Event<StackScalingContext>) data;
        StackScalingContext scalingContext = event.getData();
        LOGGER.info("execute() for phase: {}", event.getKey());
        try {
            FlowContext context = getFlowFacade().handleStackScalingFailure(scalingContext);
            LOGGER.info("Stack downscaling failure is handled. Context: {}", context);
        } catch (CloudbreakException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    protected Object assemblePayload(Object serviceResult) {
        return serviceResult;
    }
}
