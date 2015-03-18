package com.sequenceiq.cloudbreak.core.flow.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.AbstractFlowHandler;
import com.sequenceiq.cloudbreak.core.flow.FlowHandler;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.UpdateInstancesContext;
import com.sequenceiq.cloudbreak.core.flow.service.FlowFacade;

import reactor.event.Event;

@Component
public class StackDownscaleHandler extends AbstractFlowHandler<UpdateInstancesContext> implements FlowHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackDownscaleHandler.class);

    @Autowired
    private FlowFacade flowFacade;

    @Override
    protected Object execute(Event<UpdateInstancesContext> event) throws CloudbreakException {
        LOGGER.info("execute() for phase: {}", event.getKey());
        FlowContext context = flowFacade.downscaleStack(event.getData());
        LOGGER.info("Upscale of stack finished. Context: {}", context);
        return context;
    }

    @Override
    protected void handleErrorFlow(Throwable throwable, Object data) {

    }

    @Override
    protected Object assemblePayload(Object serviceResult) {
        return serviceResult;
    }
}
