package com.sequenceiq.cloudbreak.core.flow.handlers;

import static com.sequenceiq.cloudbreak.domain.ScalingType.isStackDownScale;

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
        StackScalingContext stackScalingContext = event.getData();
        if (isStackDownScale(stackScalingContext.getScalingType())) {
            FlowContext context = getFlowFacade().downscaleStack(stackScalingContext);
            LOGGER.info("Upscale of stack is finished. Context: {}", context);
            return context;
        }
        return stackScalingContext;
    }

    @Override
    protected Object handleErrorFlow(Throwable throwable, StackScalingContext handlerContext) throws Exception {
        LOGGER.info("Stack downscaling failure is handled. Context: {}", handlerContext);
        handlerContext.setErrorReason(throwable.getMessage());
        return getFlowFacade().handleStackScalingFailure(handlerContext);
    }
}
