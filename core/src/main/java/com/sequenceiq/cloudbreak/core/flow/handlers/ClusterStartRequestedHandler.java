package com.sequenceiq.cloudbreak.core.flow.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.AbstractFlowHandler;
import com.sequenceiq.cloudbreak.core.flow.FlowHandler;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;

import reactor.event.Event;

@Component
public class ClusterStartRequestedHandler extends AbstractFlowHandler<StackStatusUpdateContext> implements FlowHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStartRequestedHandler.class);

    @Override
    protected Object execute(Event<StackStatusUpdateContext> event) throws CloudbreakException {
        LOGGER.info("execute() for phase: {}", event.getKey());
        FlowContext context = getFlowFacade().startClusterRequested(event.getData());
        LOGGER.info("Cluster start requested handled. Context: {}", context);
        return context;
    }
}
