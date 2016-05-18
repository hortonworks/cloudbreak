package com.sequenceiq.cloudbreak.core.flow.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.AbstractFlowHandler;
import com.sequenceiq.cloudbreak.core.flow.FlowHandler;
import com.sequenceiq.cloudbreak.core.flow.context.StackScalingContext;

import reactor.bus.Event;

@Component
public class UpscaleMetadataCollectHandler extends AbstractFlowHandler<StackScalingContext> implements FlowHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpscaleMetadataCollectHandler.class);

    @Override
    protected Object execute(Event<StackScalingContext> event) throws CloudbreakException {
        LOGGER.info("execute() for phase: {}", event.getKey());
        StackScalingContext stackStatusUpdateContext = (StackScalingContext) getFlowFacade().collectMetadata(event.getData());
        LOGGER.info("Metadata set up. Context: {}", stackStatusUpdateContext);
        return stackStatusUpdateContext;
    }
}
