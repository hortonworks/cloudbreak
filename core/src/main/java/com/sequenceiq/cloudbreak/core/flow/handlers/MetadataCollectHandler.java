package com.sequenceiq.cloudbreak.core.flow.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.AbstractFlowHandler;
import com.sequenceiq.cloudbreak.core.flow.FlowHandler;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;

import reactor.bus.Event;

@Component
public class MetadataCollectHandler extends AbstractFlowHandler<StackStatusUpdateContext> implements FlowHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataCollectHandler.class);

    @Override
    protected Object execute(Event<StackStatusUpdateContext> event) throws CloudbreakException {
        LOGGER.info("execute() for phase: {}", event.getKey());
        StackStatusUpdateContext stackStatusUpdateContext = (StackStatusUpdateContext) getFlowFacade().collectMetadata(event.getData());
        LOGGER.info("Metadata set up. Context: {}", stackStatusUpdateContext);
        return stackStatusUpdateContext;
    }
}

