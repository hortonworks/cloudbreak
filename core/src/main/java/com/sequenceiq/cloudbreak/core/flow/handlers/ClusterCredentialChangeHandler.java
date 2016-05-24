package com.sequenceiq.cloudbreak.core.flow.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.AbstractFlowHandler;
import com.sequenceiq.cloudbreak.core.flow.FlowHandler;
import com.sequenceiq.cloudbreak.core.flow.context.ClusterAuthenticationContext;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;

import reactor.bus.Event;

@Component
public class ClusterCredentialChangeHandler extends AbstractFlowHandler<ClusterAuthenticationContext> implements FlowHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCredentialChangeHandler.class);

    @Override
    protected Object execute(Event<ClusterAuthenticationContext> event) throws CloudbreakException {
        LOGGER.info("execute() for phase: {}", event.getKey());
        FlowContext context = getFlowFacade().credentialChange(event.getData());
        LOGGER.info("Authentication change is finished. Context: {}", context);
        return context;
    }
}
