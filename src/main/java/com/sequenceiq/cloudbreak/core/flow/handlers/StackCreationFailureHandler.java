package com.sequenceiq.cloudbreak.core.flow.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.AbstractFlowHandler;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.core.flow.service.StackFacade;

import reactor.event.Event;

@Service
public class StackCreationFailureHandler extends AbstractFlowHandler<FlowContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackCreationFailureHandler.class);

    @Autowired
    private StackFacade stackFacade;

    @Override
    protected Object execute(Event<FlowContext> event) throws CloudbreakException {
        LOGGER.info("execute() for phase: {}", event.getKey());
        FlowContext context = stackFacade.stackCreationError(event.getData());
        LOGGER.info("Stack creation failure  handled. Context: {}", context);
        return context;
    }
}
